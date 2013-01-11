
import ij.*;
import ij.gui.*;
import ij.plugin.*;
import ij.plugin.frame.RoiManager;
import ij.process.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import sun.misc.Regexp;

public class IIF_Flow implements PlugIn, MouseWheelListener {

    String WinRoot = "C:/Speedwork";
    String LinRoot = "/home/ogaard";
    String outDir = "/Out";
    String roiSaveFormat = outDir + "/roi/%04d%s.roi";
    String jpgSaveFormat = outDir + "/jpg/%04d.jpg";
    String dataSaveFormat = outDir + "/data/%04d.dat";
    String inFormat = "/Done/seq%04d.jp2";
    
    int imgID;
    String seriesID;
    String filename;
    ImagePlus source;

    public void run(String arg) {
        File tPath = new File(toWindowsSeparator(WinRoot));
        if (tPath.exists()) {
            roiSaveFormat = toWindowsSeparator(WinRoot + roiSaveFormat);
            jpgSaveFormat = toWindowsSeparator(WinRoot + jpgSaveFormat);
            dataSaveFormat = toWindowsSeparator(WinRoot + dataSaveFormat);
            inFormat = toWindowsSeparator(WinRoot + inFormat);
        } else {
            roiSaveFormat = LinRoot + roiSaveFormat;
            jpgSaveFormat = LinRoot + jpgSaveFormat;
            dataSaveFormat = LinRoot + dataSaveFormat;
            inFormat = LinRoot + inFormat;            
        }
        imgID = Integer.parseInt(arg.split("\\|")[0]);
        seriesID = arg.split("\\|")[1];
        IJ.run("Bio-Formats Windowless Importer", "open=" + String.format(inFormat, imgID));
        ImagePlus tImage = IJ.getImage();
        filename = tImage.getOriginalFileInfo().fileName;
        IJ.run("Stack to RGB");
        tImage.close();
        IJ.setTool("arrow");
        new WaitForUserDialog("Action Required", "Use the arrowtool to set direction towards right ventricle.").show();
        // Find all images that have a LineRoi in them
        int[] ids = WindowManager.getIDList();

        if (null == ids) {
            return; // no images open
        }

        ArrayList all = new ArrayList();

        for (int i = 0; i < ids.length; i++) {
            ImagePlus imp = WindowManager.getImage(ids[i]);
            Roi roi = imp.getRoi();
            int type = imp.getType();
            if (null != roi && roi instanceof Line) {
                all.add(imp);
            }
        }

        if (all.size() == 0) {
            IJ.showMessage("Need at least 1 image with a LineROI in it. // Index image assumed.");

            try {
                File nFile = new File(String.format(dataSaveFormat, imgID));
                FileWriter outFile;
                outFile = new FileWriter(nFile);
                PrintWriter out = new PrintWriter(outFile);
                out.println("Series ID: " + seriesID);
                out.println("Is this an index image?: " + "Yes");
                out.close();
            } catch (IOException ex) {
                Logger.getLogger(IIF_Flow.class.getName()).log(Level.SEVERE, null, ex);
            }

            IJ.run(source, "Close All", "");

            return;
        }

        // create choice arrays
        String[] titles = new String[all.size()];
        int k = 0;
        for (Iterator it = all.iterator(); it.hasNext();) {
            titles[k++] = ((ImagePlus) it.next()).getTitle();
        }

        if (all.size() > 1) {
            GenericDialog gd = new GenericDialog("Align Image");
            String current = WindowManager.getCurrentImage().getTitle();
            gd.addChoice("source", titles, titles[0]);
            gd.showDialog();
            if (gd.wasCanceled()) {
                return;
            }
            source = WindowManager.getImage(ids[gd.getNextChoiceIndex()]);
        } else {
            source = WindowManager.getCurrentImage();
        }

        Arrow sr = (Arrow) source.getRoi();
        AngleLine line1 = new AngleLine(sr.x1, sr.y1, sr.x2, sr.y2);
        source.unlock();
        IJ.run("Select All");
        IJ.run("Rotate...", "angle=" + Math.toDegrees(line1.angle));
        Roi roi = source.getRoi();
        Rectangle r = roi.getBounds();
        int x_ = r.width > source.getWidth() ? r.width : source.getWidth();
        int y_ = r.height > source.getHeight() ? r.height : source.getHeight();

        IJ.run("Colors...", "foreground=white background=white selection=yellow");
        IJ.run("Canvas Size...", "width=" + x_ + " height=" + y_ + " position=Center zero");


        ImageProcessor result = source.getProcessor();
        result.rotate(Math.toDegrees(line1.angle));
        source.draw();
        IJ.setTool("point");
        new WaitForUserDialog("Action Required", "Use point tool to target slice area.").show();
        PointRoi p = (PointRoi) source.getRoi();
        IJ.run("Colors...", "foreground=black background=black selection=yellow");
        new IIF_Wand().doWand(source, p.getPolygon().xpoints[0], p.getPolygon().ypoints[0]);
        IJ.run("Colors...", "foreground=white background=white selection=yellow");
        IJ.run("Crop");
        IJ.setTool("brush");
        IJ.register(IIF_Flow.class);
        source.getWindow().getCanvas().addMouseWheelListener(this);
        source.getWindow().maximize();
        new CustomDialog("Look!").show();

    }

    private static ImageProcessor align(ImageProcessor source, AngleLine line1) {
        ImageProcessor result = new FloatProcessor(0, 0);
        source.rotate(Math.toDegrees(line1.angle));
        result.insert(source, 0, 0);
        return result;
    }

    private class AngleLine extends ij.gui.Line {

        public double angle = 0;

        public AngleLine(int ox1, int oy1, int ox2, int oy2) {
            super(ox1, oy1, ox2, oy2);
            angle = Math.atan2((float) (oy1 - oy2), (float) (ox2 - ox1)) - Math.PI / 2;
        }
    }

    /**
     * Hacks on the ij.gui.Toolbar to get the proper value, and defaults to 15
     * if the value is absurd.
     */
    static public int getBrushSize() {
        int brushSize = 15;
        try {
            java.lang.reflect.Field f = Toolbar.class.getDeclaredField("brushSize");
            f.setAccessible(true);
            brushSize = ((Integer) f.get(Toolbar.getInstance())).intValue();
            if (brushSize < 1) {
                brushSize = 15;
            }
        } catch (Exception e) {
            IJ.log(e.getMessage());
        }
        return brushSize;
    }

    /**
     * Change the brush size by the given length increment (in pixel units). A
     * lower limit of 1 pixel is preserved. Returns the value finally accepted
     * for brush size.
     */
    static private int setBrushSize(int inc) {
        int brushSize = 15;
        try {
            java.lang.reflect.Field f = Toolbar.class.getDeclaredField("brushSize");
            f.setAccessible(true);
            brushSize = ((Integer) f.get(Toolbar.getInstance())).intValue();
            if (brushSize + inc < 1) {
                brushSize = 1;
            } else {
                brushSize += inc;
            }
            f.setInt(Toolbar.getInstance(), brushSize);
        } catch (Exception e) {
        }
        return brushSize;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        final int rotation = e.getWheelRotation();
        final int sign = rotation > 0 ? 1 : -1;
        // resize brush for AreaList/AreaTree painting
        synchronized (this) {
            setBrushSize((int) (10 * rotation));
        } // the getWheelRotation provides the sign
        int extra = (int) (10);
        if (extra < 2) {
            extra = 2;
        }
        extra += 4; // for good measure
        IJ.getImage().draw();
    }

    private class CustomDialog extends WaitForUserDialog implements ActionListener, ItemListener {

        GridBagConstraints c = new GridBagConstraints();
        int VIN = 0;
        int IN = 1;
        int N = 2;
        int[] VIN_IN_N = {VIN, IN, N};
        int[] VIN_IN = {VIN, IN};
        boolean newRoi = false;
        Checkbox isIndex = new Checkbox("Is this an index image?", false);
        Checkbox hasSeptumBubbles = new Checkbox("Are there obvious bubbles in septum area?", false);
        Checkbox hasTissueBubbles = new Checkbox("Are there obvious bubbles in tissue area?", false);
        Checkbox hasFibers = new Checkbox("Are there occurrances of fibers?", false);
        Checkbox hasLesions = new Checkbox("Does the tissue have obvious lesions?", false);
        Checkbox hasArtifacts = new Checkbox("Are other artifact clearly visible in tissue?", false);
        Checkbox isMangled = new Checkbox("Is the sample damaged beyond analytic value?", false);
        Checkbox shouldBeExcluded = new Checkbox("To be excluded based on other factors?", false);
        CheckboxGroup ROISelector = new CheckboxGroup();
        Checkbox ViableInfarctedNecrotic = new Checkbox(null, ROISelector, true);
        Checkbox InfarctedNecrotic = new Checkbox(null, ROISelector, false);
        Checkbox Necrotic = new Checkbox(null, ROISelector, false);
        Button finished = new Button("Done");
        RoiManager rm = new RoiManager();
        String lastRoi;

        public CustomDialog(String message) {
            this("User Action Required", message);
        }

        public CustomDialog(String title, String message) {
            super(title, message);
            remove(getComponentCount() - 1);
            remove(getComponentCount() - 1);
            lastRoi = "VIN";
            source.getRoi().setName(lastRoi);
            rm.addRoi(source.getRoi());

            rm.select(0);

            c.insets = new Insets(10, 10, 10, 10);
            fill(GridBagConstraints.WEST, 0, 0, 3, 1, isIndex);
            fill(GridBagConstraints.WEST, 0, 1, 3, 1, hasSeptumBubbles);
            fill(GridBagConstraints.WEST, 0, 2, 3, 1, hasTissueBubbles);
            fill(GridBagConstraints.WEST, 0, 3, 3, 1, hasFibers);
            fill(GridBagConstraints.WEST, 0, 4, 3, 1, hasLesions);
            fill(GridBagConstraints.WEST, 0, 5, 3, 1, hasArtifacts);
            fill(GridBagConstraints.WEST, 0, 6, 3, 1, isMangled);
            fill(GridBagConstraints.WEST, 0, 7, 3, 1, shouldBeExcluded);
            fill(GridBagConstraints.CENTER, 0, 8, 1, 0.5, new Label("All"));
            fill(GridBagConstraints.CENTER, 1, 8, 1, 0.5, new Label("Area at risk"));
            fill(GridBagConstraints.CENTER, 2, 8, 1, 0.5, new Label("Necrotic"));
            fill(GridBagConstraints.CENTER, 0, 9, 1, 0.5, ViableInfarctedNecrotic);
            fill(GridBagConstraints.CENTER, 1, 9, 1, 0.5, InfarctedNecrotic);
            fill(GridBagConstraints.CENTER, 2, 9, 1, 0.5, Necrotic);
            fill(GridBagConstraints.EAST, 0, 10, 3, 1, finished);

            ViableInfarctedNecrotic.addItemListener(this);
            InfarctedNecrotic.addItemListener(this);
            Necrotic.addItemListener(this);
            finished.addActionListener(this);
            pack();
        }

        private void fill(int anchor, int gridx, int gridy, int gridwidth, double weightx, Component comp) {
            c.anchor = anchor;
            c.gridx = gridx;
            c.gridy = gridy;
            c.gridwidth = gridwidth;
            c.weightx = weightx;
            add(comp, c);
        }

        public void itemStateChanged(ItemEvent e) {
            Roi t;
            if (newRoi) {
                if (source.getRoi() != null) {
                    t = source.getRoi();
                    t.setName(lastRoi);
                    rm.addRoi(t);
                }
                newRoi = false;
            } else {
                rm.runCommand("Update");
            }

            if (ViableInfarctedNecrotic.getState()) {
                lastRoi = "VIN";
            }
            if (InfarctedNecrotic.getState()) {
                lastRoi = "IN";
            }
            if (Necrotic.getState()) {
                lastRoi = "N";
            }

            IJ.run(source, "Select None", "");

            if (rm.getCount() >= 2) {
                rm.select(IN);
                rm.setSelectedIndexes(VIN_IN);
                rm.runCommand("AND");
                rm.runCommand("Update");
            }

            if (rm.getCount() == 3) {
                rm.select(N);
                rm.setSelectedIndexes(VIN_IN_N);
                rm.runCommand("AND");
                rm.runCommand("Update");
            }

            IJ.run(source, "Select None", "");

            if (ViableInfarctedNecrotic.getState()) {
                rm.select(VIN);
            }

            if (InfarctedNecrotic.getState()) {
                if (rm.getCount() == 1) {
                    newRoi = true;
                } else {
                    rm.select(IN);
                }
            }

            if (Necrotic.getState()) {
                if (rm.getCount() == 3) {
                    rm.select(N);
                } else if (rm.getCount() == 2) {
                    newRoi = true;
                } else {
                    IJ.showMessage("Please set Area at Risk before selecting necrotic regions. Defaulting to top selection (VIN).");
                    Necrotic.setState(false);
                    ViableInfarctedNecrotic.setState(true);
                    rm.select(VIN);
                }

            }
        }

        ;

        public void actionPerformed(ActionEvent e) {
            try {
                if (rm.getCount() > 0) {
                    rm.select(VIN);
                    IJ.saveAs(source, "Selection", String.format(roiSaveFormat, imgID, "VIN"));
                }
                if (rm.getCount() > 1) {
                    rm.select(IN);
                    IJ.saveAs(source, "Selection", String.format(roiSaveFormat, imgID, "IN"));
                }
                if (rm.getCount() > 2) {
                    rm.select(N);
                    IJ.saveAs(source, "Selection", String.format(roiSaveFormat, imgID, "N"));
                }
            } catch (Exception ex) {
                IJ.showMessage(ex.getMessage());
            }

            dispose();
            try {
                File nFile = new File(String.format(dataSaveFormat, imgID));
                FileWriter outFile = new FileWriter(nFile);
                PrintWriter out = new PrintWriter(outFile);
                out.println("Series ID: " + seriesID);
                out.println(isIndex.getLabel() + ": " + (isIndex.getState() ? "Yes" : "No"));
                out.println(hasSeptumBubbles.getLabel() + ": " + (hasSeptumBubbles.getState() ? "Yes" : "No"));
                out.println(hasTissueBubbles.getLabel() + ": " + (hasTissueBubbles.getState() ? "Yes" : "No"));
                out.println(hasFibers.getLabel() + ": " + (hasFibers.getState() ? "Yes" : "No"));
                out.println(hasLesions.getLabel() + ": " + (hasLesions.getState() ? "Yes" : "No"));
                out.println(hasArtifacts.getLabel() + ": " + (hasArtifacts.getState() ? "Yes" : "No"));
                out.println(isMangled.getLabel() + ": " + (isMangled.getState() ? "Yes" : "No"));
                out.println(shouldBeExcluded.getLabel() + ": " + (shouldBeExcluded.getState() ? "Yes" : "No"));
                out.close();
            } catch (FileNotFoundException ex) {
                IJ.showMessage(ex.getMessage());
                Logger.getLogger(IIF_Flow.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                IJ.showMessage(ex.getMessage());
                Logger.getLogger(IIF_Flow.class.getName()).log(Level.SEVERE, null, ex);
            }

            rm.runCommand("Select All");
            rm.runCommand("Delete");
            rm.close();
            IJ.saveAs(source, "Jpeg", String.format(jpgSaveFormat, imgID));
            IJ.run(source, "Close All", "");
            close();
        }
    ;

    }
    
    private String toWindowsSeparator(String linPath) {
        return linPath.replace("/", "\\");
    }
}
