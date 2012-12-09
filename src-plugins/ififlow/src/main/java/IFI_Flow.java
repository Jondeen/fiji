/*Manual tracking v2.0, 15/06/05
 Fabrice P Cordelières, fabrice.cordelieres at curie.u-psud.fr
 New features:
 2D centring correction added
 Directionality check added
 Previous track files may be reloaded
 3D features added (retrieve z coordinates, quantification and 3D representation as VRML file)
 */

import adapter.Image5DAdapter;
import adapter.ImageAdapter;
import ij.*;
import ij.gui.*;
import ij.io.*;
import ij.measure.*;
import ij.plugin.filter.Duplicater;
import ij.plugin.frame.*;
import ij.process.*;
import ij.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.StringTokenizer;


public class IFI_Flow extends PlugInFrame implements ActionListener, ItemListener, MouseListener {

    //Calibration related variables---------------------------------------------
    double calxy = 0.129; //This value may be changed to meet your camera caracteristics
    double calz = 0.3; //This value may be changed to meet your piezo/Z-stepper caracteristics
    double calt = 2; //This value may be changed to meet your timelapse settings
    int cent = 5; //Default side size for the square where the center is searched
    int dotsize = 5; // Drawing parameter: default dot size
    double linewidth = 1; // Drawing parameter: default line width
    int fontsize = 12; // Drawing parameter: default font size
    Color[] col = {Color.blue, Color.green, Color.red, Color.cyan, Color.magenta, Color.yellow, Color.white}; //Values for color in the drawing options
    //Universal variables-------------------------------------------------------
    int i;
    int j;
    int k;
    int l;
    int m;
    int n;
    String txt;
    //Interface related variables-----------------------------------------------
    static Frame instance;
    Font bold = new Font("", 3, 12);
    Panel panel;
    //Tracking
    Button butSelSource;
    Button butSelCompleted;
    Button butStart;
    //Image related variables---------------------------------------------------
    ImagePlus img;
    String imgtitle;
    int Width;
    int Height;
    int Depth;
    int Slice;
    String SliceTitle;
    ImageCanvas canvas;
    ImagePlus ip;
    ImageStack stack;
    ImageWindow win;
    StackConverter sc;
    Duplicater dp;
    //Tracking related variables------------------------------------------------
    boolean islistening = false; //True as long as the user is tracking
    int[] xRoi; //Defines the ROI to be shown using the 'Show path' option - x coordinates
    int[] yRoi; //Defines the ROI to be shown using the 'Show path' option - y coordinates
    Roi roi; //ROI
    int Nbtrack = 1; // Number of tracks
    int NbPoint = 1; // Number of tracked points in the current track
    int ox; //x coordinate of the current tracked point
    int oy; //y coordinate of the current tracked point
    int PixVal; //intensity of the current tracked point
    int prevx; //x coordinate of the previous tracked point
    int prevy; //y coordinate of the previous tracked point
    double Distance; //Distance between (ox,oy) and (prevx, prevy)
    double Velocity; //Distance/calt
    int pprevx; //x coordinate of the antepenultimate tracked point
    int pprevy; //y coordinate of the antepenultimate tracked point
    boolean trackZ; // whether Z or time is tracked
    //Centring correction related variables--------------------------------------
    String commentCorr; //Stores the tracked point coordinates and the corrected point coordinates
    //Reference related variables-----------------------------------------------
    boolean islisteningRef = false; // True when the add reference button has been clicked.
    boolean RefSet = false; // True if a reference has already been set
    int DirIndex = 1; //1 for anterograde movement, -1 for retrograde movement
    int refx = 0; // x coordinate of the reference pixel
    int refy = 0; // y coordinate of the reference pixel
    Roi roiRef; // Circular region drawn around the reference
    //Dialog boxes--------------------------------------------------------------
    GenericDialog gd;
    GenericDialog gd1;
    GenericDialog gd2;
    GenericDialog VRMLgd;
    OpenDialog od;
    SaveDialog sd;
    String FileName; // Filename with extension
    String File; // Filename without extension
    String dir; // Directory
    //Results tables------------------------------------------------------------
    ResultsTable rt; //2D results table
    ResultsTable rtmp; // Temporary results table
    String[] head = {"Track n°", "Slice n°", "X", "Y", "Distance", "Velocity", "Pixel Value"}; //2D results table's headings
    ResultsTable rt3D; //3D results table
    //Load Previous Track File related variables--------------------------------
    BufferedReader in; //Input file
    String line; //Input line from the input file
    StringTokenizer Token; //used to separate tab delimited values in the imported file
    //Retrieve z coordinates dialog box & variables-----------------------------
    String[] CentringArray = {"No centring correction", "Barycentre in signal box", "Max intensity in signal box"}; //List of options in the centring correction choicelist
    int Centring; //3D centring option n°
    int sglBoxx; //Width of the signal box
    int sglBoxy; //Height of the signal box
    int sglBoxz; //Depth of the signal box
    int bkgdBoxx; //Width of the background box
    int bkgdBoxy; //Height of the background box
    int bkgdBoxz; //Depth of the background box
    String[] QuantificationArray = {"No background correction", "Bkgd box centred on sgl box", "Bkgd box on top left", "Bkgd box on top right", "Bkgd box on bottom left", "Bkgd box on bottom right"}; //List of options in the quantification settings choicelist
    int Quantification; //3D quantification option n°
    boolean DoQuantification; //True if the Do quantification checkbox is checked
    boolean DoBleachCorr; //True if the Do bleaching correction checkbox is checked
    boolean DoVRML; //True if the Export 3D+t data as a VRML file checkbox is checked
    //3D centring correction related variables----------------------------------
    int tmpx; //Temporary x value
    int tmpy; //Temporary y value
    int tmpz; //Temporary z value
    int tmpttl; //Temporary sum of all pixels' values in the signal box
    int tmppixval; //Intensity of the current pixel
    //Quantification related variables------------------------------------------
    int limsx1; //Left limit of the signal box
    int limsx2; //Right limit of the signal box
    int limsy1; //Upper limit of the signal box
    int limsy2; //Lower limit of the signal box
    int limsz1; //Top limit of the signal box
    int limsz2; //Bottom limit of the signal box
    double sizeSgl; //Number of voxel in the signal box
    int limbx1; //Left limit of the background box
    int limbx2; //Right limit of the background box
    int limby1; //Upper limit of the background box
    int limby2; //Lower limit of the background box
    int limbz1; //Top limit of the background box
    int limbz2; //Bottom limit of the background box
    double sizeBkgd; //Number of voxel in the background box
    double Qsgl; //Summed intensities of the voxels inside the signal box
    double Qbkgd; //Summed intensities of the voxels inside the background box
    double Qttl; //Summed intensities of the whole pixels in the stack at current time
    double Qttl0; //Summed intensities of the whole pixels in the stack at the first time of the current track
    double QSglBkgdCorr; //QSgl background corrected
    double QSglBkgdBleachCorr; //QSgl background and bleaching corrected
    //VRML export related variables---------------------------------------------
    String[] StaticArray = {"None", "Trajectories", "Objects"}; //List of options in the static view choicelist
    String[] DynamicArray = {"None", "Objects", "Objects & Static Trajectories", "Objects & Progressive Trajectories"}; //List of options in the dynamic view choicelist
    String Static; //Designation of the static view selected, to be added to the destination filename
    String Dynamic; //Designation of the dynamic view selected, to be added to the destination filename
    boolean StaticView; //True if a static view has to be generated
    boolean StaticViewObj; //True if a static view of the objects has to be generated
    boolean StaticViewTraj; //True if a static view of the trajectories has to be generated
    boolean DynamicView; //True if a dynamic view has to be generated
    boolean DynamicViewStaticTraj; //True if a dynamic view of the objects overlayed to a static view of trajectories has to be generated
    boolean DynamicViewDynamicTraj; //True if a dynamic view of the objects overlayed to a dynamic view of trajectories has to be generated
    String dirVRMLstat; //Path to save the static VRML view
    OutputStreamWriter oswStat; //Output file for VRML static view
    String dirVRMLdynam; //Path to save the dynamic VRML view
    OutputStreamWriter oswDynam; //Output file for VRML dynamic view
    String[] vrmlCol = {"0 0 1", "0 1 0", "1 0 0", "0.5 1 1", "1 0.5 1", "1 1 0", "1 1 1"}; //Values for colors in the VRML file
    int x; //Variable to store x coordinate read from the 3D results table
    int y; //Variable to store y coordinate read from the 3D results table
    int z; //Variable to store z coordinate read from the 3D results table
    int xOld; //Variable to store previous x coordinate read from the 3D results table
    int yOld; //Variable to store previous y coordinate read from the 3D results table
    int zOld; //Variable to store previous z coordinate read from the 3D results table
    int[][] VRMLarray; //1st dimension: line n° from the 3D results table; 2nd dimension: 0-Tag (track n°/color); 1-time; 2-x, 3-y; 4-z
    int vrmlCount; //Number of tracks modulo 6: will define the color applied to the track
    double DistOfView; //Distance between the object and the camera in the VRML view
    double minTime; //Minimum timepoint where a track is started
    double maxTime; //Maximum timepoint where a track is ended
    int countBefore; //Difference between the current track startpoint and minTime
    int countAfter;//Difference between the current track endpoint and maxTime
    int countTtl; //Difference between countBefore and countAfter
    String key; //Defines the animation's keyframes
    int Tag; //Track number
    int TagOld; //Previous track number
    String point; //Stores the xyz coordinates (modified by calibration) of the current point from the current track
    int pointNb; //Number of points in the current track
    String pointKey; //Stores the xyz coordinates (modified by calibration) of each point from the current track
    String lastPoint; //Stores the xyz coordinates (modified by calibration) of the last point from the current track
    protected ImageAdapter adapter;

    public IFI_Flow() {
        super("IFIFlow");
        instance = this;

        try {
            Class.forName("i5d.gui.Image5DWindow");
            adapter = new Image5DAdapter();
        } catch (ClassNotFoundException e) {
            adapter = new ImageAdapter();
        }
        
        ImagePlus ipl = WindowManager.getCurrentImage();
        
        ImageProcessor ipr = ipl.getProcessor();
        
        ipr.setThreshold(200, 255, ImageProcessor.BLACK_AND_WHITE_LUT);
        

        //Interface setup ------------------------------------------------------
        panel = new Panel();
        panel.setLayout(new GridLayout(0, 3));
        panel.setBackground(SystemColor.control);


        //---------------------------------Tracking
        panel.add(new Label());
        Label title = new Label();
        title.setText("IFI Workflow α:");
        title.setFont(bold);
        panel.add(title);
        panel.add(new Label());

        butSelSource = new Button("Select Source Folder");
        butSelSource.addActionListener(this);
        panel.add(butSelSource);

        butSelCompleted = new Button("Select Completed Folder");
        butSelCompleted.addActionListener(this);
        panel.add(butSelCompleted);

        butStart = new Button("Start flow");
        butStart.addActionListener(this);
        panel.add(butStart);


        add(panel, BorderLayout.CENTER);
        pack();
        show();
        IJ.showProgress(2, 1);
        rt = new ResultsTable();

    }

    /**
     * Moves to a given position in either time or z in the given image,
     * depending on what is tracked.
     */
    protected void moveTo(ImagePlus img, int position) {
        if (trackZ) {
            adapter.setSlice(img, position);
        } else {
            adapter.setFrame(img, position);
        }
    }

    /**
     * Gets either the current slice or the current frame, depending on what is
     * tracked.
     */
    protected int getPosition(ImagePlus img) {
        if (trackZ) {
            return adapter.getSlice(img);
        } else {
            return adapter.getFrame(img);
        }
    }

    /**
     * Gets the number of slices or frames available in the image, depending on
     * whit is tracked.
     */
    protected int getMaxPosition(ImagePlus img) {
        if (trackZ) {
            return img.getNSlices();
        } else {
            return img.getNFrames();
        }
    }

    public void itemStateChanged(ItemEvent e) {
    }
    
    public void actionPerformed(ActionEvent e) {
        // Button Add Track pressed---------------------------------------------
        if (e.getSource() == butSelSource) {
            if (islistening) {
                IJ.showMessage("This operation can't be completed:\na track is already being followed...");
                return;
            }
            HideParam();
            img = WindowManager.getCurrentImage();
            imgtitle = img.getTitle();
            if (imgtitle.indexOf(".") != -1) {
                imgtitle = imgtitle.substring(0, imgtitle.indexOf("."));
            }
            
            IJ.setTool(7);

            // assume time tracking when more than one frame is present
            trackZ = img.getNSlices() > 1 && img.getNFrames() == 1;

            xRoi = new int[getMaxPosition(img)];
            yRoi = new int[getMaxPosition(img)];

            if (img == null) {
                IJ.showMessage("Error", "Man,\n" + "You're in deep troubles:\n" + "no opened stack...");
                return;
            }

            win = img.getWindow();
            canvas = win.getCanvas();
            moveTo(img, 1);

            NbPoint = 1;
            IJ.showProgress(2, 1);
            canvas.addMouseListener(this);
            islistening = true;
            return;
        }

        // Button Delete last point pressed-------------------------------------
        if (e.getSource() == butStart) {
            gd = new GenericDialog("Delete last point");
            gd.addMessage("Are you sure you want to \n" + "delete last point ?");
            gd.showDialog();
            if (gd.wasCanceled()) {
                return;
            }

            //Create a temporary ResultTable and copy only the non deleted data
            rtmp = new ResultsTable();
            for (i = 0; i < (rt.getCounter()); i++) {
                rtmp.incrementCounter();
                for (j = 0; j < 7; j++) {
                    rtmp.addValue(j, rt.getValue(j, i));
                }
            }

            rt.reset();

            //Copy data back to original table except last point

            for (i = 0; i < head.length; i++) {
                rt.setHeading(i, head[i]);
            }

            for (i = 0; i < ((rtmp.getCounter()) - 1); i++) {
                rt.incrementCounter();
                for (j = 0; j < 7; j++) {
                    rt.addValue(j, rtmp.getValue(j, i));
                }
            }
            rt.show("Results from " + imgtitle + " in " );

            //Manage case where the deleted point is the last of a serie
            if (islistening == false) {
                Nbtrack--;
                canvas.addMouseListener(this);
                islistening = true;
            }

            prevx = (int) rt.getValue(2, rt.getCounter() - 1);
            prevy = (int) rt.getValue(3, rt.getCounter() - 1);
            moveTo(img, ((int) rt.getValue(1, rt.getCounter() - 1)) + 1);
            IJ.showStatus("Last Point Deleted !");
        }

        // Button End Track pressed---------------------------------------------
        if (e.getSource() == butSelCompleted) {
            Nbtrack++;
            canvas.removeMouseListener(this);
            islistening = false;
            IJ.showStatus("Tracking is over");
            IJ.showProgress(2, 1);
            // return;
        }



    }

    // Click on image-----------------------------------------------------------
    public void mouseReleased(MouseEvent m) {
        if (!islisteningRef) {
            IJ.showProgress(getPosition(img), getMaxPosition(img));
            if (trackZ) {
                IJ.showStatus("Tracking slice " + adapter.getSlice(img) + " of " + img.getNSlices());
            } else {
                IJ.showStatus("Tracking frame " + adapter.getFrame(img) + " of " + img.getNFrames());
            }
            if (Nbtrack == 1 && NbPoint == 1) {
                for (i = 0; i < head.length; i++) {
                    rt.setHeading(i, head[i]);
                }
            }
        }

        img.killRoi();
        ox = canvas.offScreenX(x);
        oy = canvas.offScreenY(y);

        if (islisteningRef) {
            canvas.removeMouseListener(this);
            islistening = false;
            islisteningRef = false;
            refx = ox;
            refy = oy;
            IJ.showStatus("Reference set to (" + refx + "," + refy + ")");
            RefSet = true;
            dotsize = (int) Tools.parseDouble("1");
            roiRef = new OvalRoi(refx - dotsize, refy - dotsize, 2 * dotsize, 2 * dotsize);
            img.setRoi(roiRef);
            return;
        }

        xRoi[NbPoint - 1] = ox;
        yRoi[NbPoint - 1] = oy;


        if (NbPoint == 1) {
            Distance = -1;
            Velocity = -1;
        } else {
            Distance = calxy * Math.sqrt(Math.pow((ox - prevx), 2) + Math.pow((oy - prevy), 2));
            Velocity = Distance / calt;
        }

        PixVal = img.getProcessor().getPixel(ox, oy);

        rt.incrementCounter();
        double[] doub = {Nbtrack, (img.getCurrentSlice()), ox, oy, Distance, Velocity, PixVal};
        for (i = 0; i < doub.length; i++) {
            rt.addValue(i, doub[i]);
        }
        rt.show("Results from " + imgtitle + " in ");

        if (getPosition(img) < getMaxPosition(img)) {
            NbPoint++;
            moveTo(img, getPosition(img) + 1);
            if (Distance != 0) {
                pprevx = prevx;
                pprevy = prevy;
            }
            prevx = ox;
            prevy = oy;
            roi = new PolygonRoi(xRoi, yRoi, NbPoint - 1, Roi.POLYLINE);
        } else {
            Nbtrack++;
            img.setRoi(roi);
            canvas.removeMouseListener(this);
            islistening = false;
            IJ.showStatus("Tracking is over");
            // return;
        }




    }

    public void mousePressed(MouseEvent m) {
    }

    public void mouseExited(MouseEvent m) {
    }

    public void mouseClicked(MouseEvent m) {
    }

    public void mouseEntered(MouseEvent m) {
    }

    void HideParam() {
        pack();
        show();
    }

    void ShowParam() {
        pack();
        show();
    }

    void Center2D() {
        int lim = (int) ((Tools.parseDouble("0")) / 2);
        int pixval = img.getProcessor().getPixel(ox, oy);
        double xb = 0;
        double yb = 0;
        double sum = 0;
        commentCorr = "(" + ox + "," + oy + ") > (";
        for (i = ox - lim; i < ox + lim + 1; i++) {
            for (j = oy - lim; j < oy + lim + 1; j++) {
                if (img.getProcessor().getPixel(i, j) > pixval && true) {
                    ox = i;
                    oy = j;
                    pixval = img.getProcessor().getPixel(ox, oy);
                }
                if (img.getProcessor().getPixel(i, j) < pixval && true) {
                    ox = i;
                    oy = j;
                    pixval = img.getProcessor().getPixel(ox, oy);
                }
                xb = xb + i * img.getProcessor().getPixel(i, j);
                yb = yb + j * img.getProcessor().getPixel(i, j);
                sum = sum + img.getProcessor().getPixel(i, j);
            }
        }
        commentCorr = commentCorr + ox + "," + oy + ")";
    }

    void Directionnality() {
        if (!RefSet) {
            IJ.showMessage("!!! Warning !!!", " No reference set:\nClick on 'Add reference' first !!!");
            return;
        }

        if (NbPoint == 2) {
            DirIndex = 1;
            pprevx = refx;
            pprevy = refy;
        }

        if (NbPoint > 1) {

            double angle1 = roi.getAngle(pprevx, pprevy, prevx, prevy);
            double angle2 = roi.getAngle(prevx, prevy, ox, oy);
            double angle = Math.abs(180 - Math.abs(angle1 - angle2));
            if (angle > 180.0) {
                angle = 360.0 - angle;
            }
            if (angle < 90) {
                DirIndex = -DirIndex;
            }

            Distance = Distance * DirIndex;
            Velocity = Velocity * DirIndex;
        }

    }

    void Dots() {

        dotsize = (int) Tools.parseDouble("7");
        j = 0;
        int nbtrackold = 1;
        for (i = 0; i < (rt.getCounter()); i++) {
            int nbtrack = (int) rt.getValue(0, i);
            int nbslices = (int) rt.getValue(1, i);
            int cx = (int) rt.getValue(2, i);
            int cy = (int) rt.getValue(3, i);
            if ((nbtrack != nbtrackold)) {
                j++;
            }
            if (j > 6) {
                j = 0;
            }
            ImageProcessor ip2 = stack.getProcessor(nbslices);
            ip2.setColor(col[j]);
            ip2.setLineWidth(dotsize);
            ip2.drawDot(cx, cy);
            nbtrackold = nbtrack;
        }

    }

    void ProLines() {

        linewidth = Tools.parseDouble("3");
        j = 0;
        k = 1;
        int cxold = 0;
        int cyold = 0;
        int nbtrackold = 1;

        for (i = 0; i < (rt.getCounter()); i++) {
            int nbtrack = (int) rt.getValue(0, i);
            int nbslices = (int) rt.getValue(1, i);
            int cx = (int) rt.getValue(2, i);
            int cy = (int) rt.getValue(3, i);
            int lim = img.getStackSize() + 1;
            if ((nbtrack != nbtrackold)) {
                j++;
                k = 1;
            }
            for (int n2 = nbtrack; n2 < (rt.getCounter()); n2++) {
                if ((int) (rt.getValue(0, n2)) == nbtrack) {
                    lim = (int) rt.getValue(1, n2);
                }
            }

            if (j > 6) {
                j = 0;
            }
            for (int m2 = nbslices; m2 < lim + 1; m2++) {
                if (k == 1) {
                    cxold = cx;
                    cyold = cy;
                }

                ImageProcessor ip2 = stack.getProcessor(m2);
                ip2.setColor(col[j]);
                ip2.setLineWidth((int) linewidth);
                ip2.drawLine(cxold, cyold, cx, cy);
                nbtrackold = nbtrack;
                k++;
            }
            cxold = cx;
            cyold = cy;
        }
    }

    void Center3D() {
        tmpx = 0;
        tmpy = 0;
        tmpz = 0;
        PixVal = 0;
        tmpttl = 0;
        Qttl = 0;


        limsx1 = x - sglBoxx;
        if (limsx1 < 0) {
            limsx1 = 0;
        }
        limsx2 = x + sglBoxx;
        if (limsx2 > Width - 1) {
            limsx2 = Width - 1;
        }

        limsy1 = y - sglBoxy;
        if (limsy1 < 0) {
            limsy1 = 0;
        }
        limsy2 = y + sglBoxy;
        if (limsy2 > Height - 1) {
            limsy2 = Height - 1;
        }

        limsz1 = z - sglBoxz;
        if (limsz1 < 1) {
            limsz1 = 1;
        }
        limsz2 = z + sglBoxz;
        if (limsz2 > ip.getNSlices()) {
            limsz2 = ip.getNSlices();
        }



        for (l = limsz1; l < limsz2 + 1; l++) {
            moveTo(ip, l);
            for (m = limsy1; m < limsy2 + 1; m++) {
                for (n = limsx1; n < limsx2 + 1; n++) {
                    tmppixval = ip.getProcessor().getPixel(n, m);

                    //Case centring option is barycenter
                    if (Centring == 1) {
                        tmpx = tmpx + n * tmppixval;
                        tmpy = tmpy + m * tmppixval;
                        tmpz = tmpz + l * tmppixval;
                        tmpttl = tmpttl + tmppixval;
                    }

                    //Case centring option is max intensity
                    if (Centring == 2) {
                        if (tmppixval > PixVal) {
                            PixVal = tmppixval;
                            tmpx = n;
                            tmpy = m;
                            tmpz = l;
                            tmpttl = 1;
                        }
                    }
                }
            }
        }
        x = (int) tmpx / tmpttl;
        y = (int) tmpy / tmpttl;
        z = (int) tmpz / tmpttl;
    }

    void quantify() {
        Qsgl = 0;
        sizeSgl = 0;
        Qbkgd = 0;
        sizeBkgd = 0;
        Qttl = 0;


        limsx1 = x - sglBoxx;
        if (limsx1 < 0) {
            limsx1 = 0;
        }
        limsx2 = x + sglBoxx;
        if (limsx2 > Width - 1) {
            limsx2 = Width - 1;
        }

        limsy1 = y - sglBoxy;
        if (limsy1 < 0) {
            limsy1 = 0;
        }
        limsy2 = y + sglBoxy;
        if (limsy2 > Height - 1) {
            limsy2 = Height - 1;
        }

        limsz1 = z - sglBoxz;
        if (limsz1 < 1) {
            limsz1 = 1;
        }
        limsz2 = z + sglBoxz;
        if (limsz2 > getMaxPosition(ip)) {
            limsz2 = getMaxPosition(ip);
        }

        if (Quantification == 1) {
            limbx1 = x - bkgdBoxx;
            limbx2 = x + bkgdBoxx;
            limby1 = y - bkgdBoxy;
            limby2 = y + bkgdBoxy;
        }

        if (Quantification == 2) {
            limbx1 = 0;
            limbx2 = bkgdBoxx * 2;
            limby1 = 0;
            limby2 = bkgdBoxy * 2;
        }

        if (Quantification == 3) {
            limbx1 = Width - 2 * bkgdBoxx;
            limbx2 = Width;
            limby1 = 0;
            limby2 = bkgdBoxy * 2;
        }

        if (Quantification == 4) {
            limbx1 = 0;
            limbx2 = bkgdBoxx * 2;
            limby1 = Height - 2 * bkgdBoxy;
            limby2 = Height;
        }
        if (Quantification == 5) {
            limbx1 = Width - 2 * bkgdBoxx;
            limbx2 = Width;
            limby1 = Height - 2 * bkgdBoxy;
            limby2 = Height;
        }

        if (limbx1 < 0) {
            limbx1 = 0;
        }
        if (limbx2 > Width - 1) {
            limbx2 = Width - 1;
        }
        if (limby1 < 0) {
            limby1 = 0;
        }
        if (limby2 > Height - 1) {
            limby2 = Height - 1;
        }
        limbz1 = z - bkgdBoxz;
        if (limbz1 < 1) {
            limbz1 = 1;
        }
        limbz2 = z + bkgdBoxz;
        if (limbz2 > getMaxPosition(ip)) {
            limbz2 = getMaxPosition(ip);
        }

        for (l = limsz1; l < limsz2 + 1; l++) {
            moveTo(ip, l);
            for (m = limsy1; m < limsy2 + 1; m++) {
                for (n = limsx1; n < limsx2 + 1; n++) {
                    Qsgl = Qsgl + ip.getProcessor().getPixel(n, m);
                    sizeSgl++;
                }
            }
        }

        if (Quantification > 0) {
            for (l = limbz1; l < limbz2 + 1; l++) {
                moveTo(ip, l);
                for (m = limby1; m < limby2 + 1; m++) {
                    for (n = limbx1; n < limbx2 + 1; n++) {
                        Qbkgd = Qbkgd + ip.getProcessor().getPixel(n, m);
                        sizeBkgd++;
                    }
                }
            }
        } else {
            sizeBkgd = 1; //Prevents division by zero
        }

        QSglBkgdCorr = Qsgl - Qbkgd * sizeSgl / sizeBkgd;

        if (DoBleachCorr) {
            for (l = 1; l < ip.getStackSize() + 1; l++) {
                ip.setSlice(l);
                for (m = 0; m < Height; m++) {
                    for (n = 0; n < Width; n++) {
                        Qttl = Qttl + ip.getProcessor().getPixel(n, m);
                    }
                }
            }
        }
    }

    void DoVRML() {
        try {
            if (StaticView) {
                oswStat = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(dirVRMLstat)));
            }
            if (DynamicView) {
                oswDynam = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(dirVRMLdynam)));
            }


            vrmlCount = -1;
            TagOld = 0;

            DistOfView = Math.max(Math.sqrt(Math.pow(Width * calxy / 2, 2) + Math.pow(Depth * calz / 2, 2)), Math.sqrt(Math.pow(Height * calxy / 2, 2) + Math.pow(Depth * calz / 2, 2)));
            DistOfView = Math.max(Math.sqrt(Math.pow(Width * calxy / 2, 2) + Math.pow(Height * calxy / 2, 2)), DistOfView);

            if (DynamicView) {
                oswDynam.write("#VRML V2.0 utf8\n");
                oswDynam.write("\n");
                oswDynam.write("Viewpoint{\n");
                oswDynam.write("position " + (Width * calxy / 2) + " " + (Height * calxy / 2) + " " + (-3 * DistOfView) + "\n");
                oswDynam.write("orientation 1 0 0 3.1415926535897932384626433832795\n");
                oswDynam.write("description \"XY view\"}\n");

                oswDynam.write("Viewpoint{\n");
                oswDynam.write("position " + (Width * calxy / 2) + " " + 3 * DistOfView + " " + (Depth * calz / 2) + "\n");
                oswDynam.write("orientation -1 0 0 1.5707963267948966192313216916398\n");
                oswDynam.write("description \"XZ view\"}\n");

                oswDynam.write("Viewpoint{");
                oswDynam.write("position " + 3 * DistOfView + " " + (Height * calxy / 2) + " " + (Depth * calz / 2) + "\n");
                oswDynam.write("orientation 0.57735026918962576450914878050196 0.57735026918962576450914878050196 0.57735026918962576450914878050196 2.0943951023931954923084289221863\n");
                oswDynam.write("description \"YZ view\"}\n");

                oswDynam.write("Transform{\n");
                oswDynam.write("translation " + (Width * calxy / 2) + " " + (Height * calxy / 2) + " " + (ip.getStackSize() * calz / 2) + "\n");
                oswDynam.write("children[\n");
                oswDynam.write("Shape{\n");
                oswDynam.write("appearance Appearance{\n");
                oswDynam.write("material Material{diffuseColor 1 1 1\n");
                oswDynam.write("transparency 0.75}}\n");
                oswDynam.write("geometry Box{size " + (Width * calxy) + " " + (Height * calxy) + " " + (ip.getStackSize() * calz) + "}}]}\n");
                oswDynam.write("\n");
            }


            if (StaticView) {
                oswStat.write("#VRML V2.0 utf8\n");
                oswStat.write("\n");
                oswStat.write("Viewpoint{\n");
                oswStat.write("position " + (Width * calxy / 2) + " " + (Height * calxy / 2) + " " + (-3 * DistOfView) + "\n");
                oswStat.write("orientation 1 0 0 3.1415926535897932384626433832795\n");
                oswStat.write("description \"XY view\"}\n");

                oswStat.write("Viewpoint{\n");
                oswStat.write("position " + (Width * calxy / 2) + " " + 3 * DistOfView + " " + (Depth * calz / 2) + "\n");
                oswStat.write("orientation -1 0 0 1.5707963267948966192313216916398\n");
                oswStat.write("description \"XZ view\"}\n");

                oswStat.write("Viewpoint{");
                oswStat.write("position " + 3 * DistOfView + " " + (Height * calxy / 2) + " " + (Depth * calz / 2) + "\n");
                oswStat.write("orientation 0.57735026918962576450914878050196 0.57735026918962576450914878050196 0.57735026918962576450914878050196 2.0943951023931954923084289221863\n");
                oswStat.write("description \"YZ view\"}\n");

                oswStat.write("Transform{\n");
                oswStat.write("translation " + (Width * calxy / 2) + " " + (Height * calxy / 2) + " " + (ip.getStackSize() * calz / 2) + "\n");
                oswStat.write("children[\n");
                oswStat.write("Shape{\n");
                oswStat.write("appearance Appearance{\n");
                oswStat.write("material Material{diffuseColor 1 1 1\n");
                oswStat.write("transparency 0.75}}\n");
                oswStat.write("geometry Box{size " + (Width * calxy) + " " + (Height * calxy) + " " + (ip.getStackSize() * calz) + "}}]}\n");
                oswStat.write("\n");
            }

            key = "key [\n";
            for (double tmp = minTime; tmp < maxTime + calt; tmp = tmp + calt) {
                key = key + " " + ((tmp - minTime) / (maxTime - minTime));
                countTtl++;
            }

            key = key + "]\n";

            for (i = 0; i < rt.getCounter(); i++) {
                Tag = (int) VRMLarray[i][0];
                if (Tag != TagOld) {
                    vrmlCount++;
                    point = "";
                    pointNb = 0;
                    pointKey = "";
                    lastPoint = "";
                    countBefore = 0;
                    countAfter = 0;


                    if (vrmlCount > 6) {
                        vrmlCount = 0;
                    }
                    if (DynamicView) {
                        oswDynam.write("DEF TRACK" + VRMLarray[i][0] + " Transform{\n");
                        oswDynam.write("children[\n");
                        oswDynam.write("Shape{\n");
                        oswDynam.write("appearance Appearance{\n");
                        oswDynam.write("material Material{diffuseColor " + vrmlCol[vrmlCount] + "}}\n");
                        oswDynam.write("geometry Sphere {radius " + (Tools.parseDouble("9") * calxy) + "}}]}\n");
                        oswDynam.write("\n");
                        oswDynam.write("DEF TRACK" + VRMLarray[i][0] + "_clock TimeSensor{\n");
                        oswDynam.write("cycleInterval 5\n");
                        oswDynam.write("loop TRUE\n");
                        oswDynam.write("stopTime -1}\n");
                        oswDynam.write("\n");
                        oswDynam.write("DEF TRACK" + VRMLarray[i][0] + "_positions PositionInterpolator{\n");
                        oswDynam.write(key + "\n");
                        oswDynam.write("keyValue [\n");
                    }
                    for (j = 0; j < (VRMLarray[i][1]) / calt; j++) {
                        if (DynamicView) {
                            oswDynam.write(calxy * VRMLarray[i][2] + " " + calxy * VRMLarray[i][3] + " " + calz * VRMLarray[i][4] + ",\n");
                        }
                        point = point + calxy * VRMLarray[i][2] + " " + calxy * VRMLarray[i][3] + " " + calz * VRMLarray[i][4] + ", ";
                        //----
                        pointKey = pointKey + point;
                        for (k = pointNb; k < countTtl; k++) {
                            pointKey = pointKey + calxy * VRMLarray[i][2] + " " + calxy * VRMLarray[i][3] + " " + calz * VRMLarray[i][4] + ", ";
                        }
                        pointKey = pointKey + "\n";
                        //----
                        pointNb++;
                        countBefore++;
                    }

                }

                if (StaticView && StaticViewObj) {
                    oswStat.write("Transform{\n");
                    oswStat.write("translation " + calxy * VRMLarray[i][2] + " " + calxy * VRMLarray[i][3] + " " + calz * VRMLarray[i][4] + ",\n");
                    oswStat.write("children[\n");
                    oswStat.write("Shape{\n");
                    oswStat.write("appearance Appearance{\n");
                    oswStat.write("material Material{diffuseColor " + vrmlCol[vrmlCount] + "}}\n");
                    oswStat.write("geometry Sphere {radius " + (Tools.parseDouble("6") * calxy) + "}}]\n}");
                    oswStat.write("\n");
                }

                if (DynamicView) {
                    oswDynam.write(calxy * VRMLarray[i][2] + " " + calxy * VRMLarray[i][3] + " " + calz * VRMLarray[i][4] + ",\n");
                }
                point = point + calxy * VRMLarray[i][2] + " " + calxy * VRMLarray[i][3] + " " + calz * VRMLarray[i][4] + ", ";
                //----
                pointKey = pointKey + point;
                for (k = pointNb + 1; k < countTtl; k++) {
                    pointKey = pointKey + calxy * VRMLarray[i][2] + " " + calxy * VRMLarray[i][3] + " " + calz * VRMLarray[i][4] + ", ";
                }
                pointKey = pointKey + "\n";
                //----
                countAfter++;
                pointNb++;

                if ((i != rt.getCounter() - 1 && VRMLarray[i][0] != VRMLarray[i + 1][0]) || i == rt.getCounter() - 1) {
                    for (k = 0; k < countTtl; k++) {
                        lastPoint = lastPoint + calxy * VRMLarray[i][2] + " " + calxy * VRMLarray[i][3] + " " + calz * VRMLarray[i][4] + ", ";
                    }

                    if (DynamicView) {
                        for (j = 0; j < (countTtl - (countBefore + countAfter)); j++) {
                            oswDynam.write(calxy * VRMLarray[i][2] + " " + calxy * VRMLarray[i][3] + " " + calz * VRMLarray[i][4] + ",\n");
                            pointKey = pointKey + lastPoint + "\n";
                        }
                        oswDynam.write("]}\n");
                        oswDynam.write("\n");
                        oswDynam.write("ROUTE TRACK" + VRMLarray[i][0] + "_clock.fraction_changed TO TRACK" + VRMLarray[i][0] + "_positions.set_fraction\n");
                        oswDynam.write("ROUTE TRACK" + VRMLarray[i][0] + "_positions.value_changed TO TRACK" + VRMLarray[i][0] + ".translation\n");
                        oswDynam.write("\n");
                        oswDynam.write("DEF TRACK" + VRMLarray[i][0] + "_scale PositionInterpolator{\n");
                        oswDynam.write(key + "\n");
                        oswDynam.write("keyValue [\n");
                        for (j = 0; j < countBefore; j++) {
                            oswDynam.write("0 0 0,\n");
                        }
                        for (j = 0; j < countAfter; j++) {
                            oswDynam.write("1 1 1" + ",\n");
                        }
                        for (j = 0; j < (countTtl - (countBefore + countAfter)); j++) {
                            oswDynam.write("0 0 0,\n");
                        }
                        oswDynam.write("]}\n");
                        oswDynam.write("\n");
                        oswDynam.write("ROUTE TRACK" + VRMLarray[i][0] + "_clock.fraction_changed TO TRACK" + VRMLarray[i][0] + "_scale.set_fraction\n");
                        oswDynam.write("ROUTE TRACK" + VRMLarray[i][0] + "_scale.value_changed TO TRACK" + VRMLarray[i][0] + ".scale\n");
                        oswDynam.write("\n");
                        if (DynamicViewStaticTraj || DynamicViewDynamicTraj) {
                            oswDynam.write("Shape{\n");
                            oswDynam.write("geometry IndexedLineSet{\n");
                            oswDynam.write("coord DEF TRAJ" + VRMLarray[i][0] + " Coordinate{\n");
                            if (DynamicViewStaticTraj) {
                                oswDynam.write("point[\n" + point + "]");
                            }
                            oswDynam.write("}\ncoordIndex[");
                        }
                    }

                    if (StaticView && StaticViewTraj) {
                        oswStat.write("Shape{\n");
                        oswStat.write("geometry IndexedLineSet{\n");
                        oswStat.write("coord Coordinate{\n");
                        oswStat.write("point[\n");

                        oswStat.write(point + "]}\n");
                        oswStat.write("coordIndex[");
                    }



                    for (j = 0; j < countTtl; j++) {
                        if (DynamicView && DynamicViewDynamicTraj) {
                            oswDynam.write(j + " ");
                        }
                        if (DynamicView && DynamicViewStaticTraj && j < pointNb) {
                            oswDynam.write(j + " ");
                        }
                        if (StaticView && StaticViewTraj && j < pointNb) {
                            oswStat.write(j + " ");
                        }
                    }

                    if (DynamicView && (DynamicViewStaticTraj || DynamicViewDynamicTraj)) {
                        oswDynam.write("-1]\n");
                        oswDynam.write("color Color{color[" + vrmlCol[vrmlCount] + "]}\n");
                        oswDynam.write("colorPerVertex FALSE}}\n");
                        oswDynam.write("\n");
                    }

                    if (DynamicView && DynamicViewDynamicTraj) {
                        oswDynam.write("DEF TRAJ" + VRMLarray[i][0] + "_coord CoordinateInterpolator{\n");
                        oswDynam.write(key);
                        oswDynam.write("keyValue[\n" + pointKey + "]}\n");
                        oswDynam.write("ROUTE TRACK" + VRMLarray[i][0] + "_clock.fraction_changed TO TRAJ" + VRMLarray[i][0] + "_coord.set_fraction\n");
                        oswDynam.write("ROUTE TRAJ" + VRMLarray[i][0] + "_coord.value_changed TO TRAJ" + VRMLarray[i][0] + ".point\n\n");

                    }

                    if (StaticView && StaticViewTraj) {
                        oswStat.write("-1]\n");
                        oswStat.write("color Color{color[" + vrmlCol[vrmlCount] + "]}\n");
                        oswStat.write("colorPerVertex FALSE}}\n");
                        oswStat.write("\n");
                    }

                }
                TagOld = Tag;
            }
            oswStat.close();
            oswDynam.close();


        } catch (IOException e) {
            IJ.error("Error writing VRML file");
        }

    }
}
