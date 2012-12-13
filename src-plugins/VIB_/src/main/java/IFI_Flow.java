
import ij.gui.*;
import ij.*;
import ij.process.*;
import ij.plugin.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.Iterator;

public class IFI_Flow implements PlugIn, MouseWheelListener {
    String filename;
    ImagePlus source;

    public void run(String arg) {
        filename = IJ.getImage().getOriginalFileInfo().fileName;
        IJ.showMessage(filename);
        
        IJ.run("Stack to RGB");
        IJ.run("Rename...", "title=Working");
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
            IJ.showMessage("Need at least 1 image with a LineROI in it.");
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
        new Versatile_Wand().doWand(source, p.getPolygon().xpoints[0], p.getPolygon().ypoints[0]);
        IJ.run("Colors...", "foreground=white background=white selection=yellow");
        IJ.run("Crop");
        IJ.setTool("brush");
        IJ.register(IFI_Flow.class);
        source.getWindow().getCanvas().addMouseWheelListener(this);
        new CustomDialog("Look!").show();

    }

    public static ImageProcessor align(ImageProcessor source, AngleLine line1) {
        ImageProcessor result = new FloatProcessor(0, 0);
        source.rotate(Math.toDegrees(line1.angle));
        result.insert(source, 0, 0);
        return result;
    }

    public void mouseClicked(MouseEvent e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void mousePressed(MouseEvent e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void mouseReleased(MouseEvent e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void mouseEntered(MouseEvent e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void mouseExited(MouseEvent e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    protected static class AngleLine extends ij.gui.Line {

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
        }
        return brushSize;
    }

    /**
     * Change the brush size by the given length increment (in pixel units). A
     * lower limit of 1 pixel is preserved. Returns the value finally accepted
     * for brush size.
     */
    static public int setBrushSize(int inc) {
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

    public void mouseWheelMoved(MouseWheelEvent e) {
        final int rotation = e.getWheelRotation();
        final int sign = rotation > 0 ? 1 : -1;
        // resize brush for AreaList/AreaTree painting
        synchronized (this) {
            setBrushSize((int) (10 * rotation ));
        } // the getWheelRotation provides the sign
        int extra = (int) (10);
        if (extra < 2) {
            extra = 2;
        }
        extra += 4; // for good measure
        IJ.getImage().draw();
    }

    private class CustomDialog extends WaitForUserDialog {

        GridBagConstraints c = new GridBagConstraints();
        Roi VIN, IN, N;
        String lastRoi = "";
        Checkbox isIndex = new Checkbox("Is this an index image?");
        Checkbox hasSeptumBubbles = new Checkbox("Are there obvious bubbles in septum area?");
        Checkbox hasTissueBubbles = new Checkbox("Are there obvious bubbles in tissue area?");
        Checkbox hasFibers = new Checkbox("Are there occurrances of fibers?");
        Checkbox hasLesions = new Checkbox("Does the tissue have obvious lesions?");
        Checkbox hasArtifacts = new Checkbox("Are other artifact clearly visible in tissue?");
        CheckboxGroup ROISelector = new CheckboxGroup();
        Checkbox ViableInfarctedNecrotic = new Checkbox(null, ROISelector, true);
        Checkbox InfarctedNecrotic = new Checkbox(null, ROISelector, false);
        Checkbox Necrotic = new Checkbox(null, ROISelector, false);
        Button finished = new Button("Done");

        public CustomDialog(String message) {
            this("User Action Required", message);
        }

        public CustomDialog(String title, String message) {
            super(title, message);
            remove(getComponentCount() - 1);
            remove(getComponentCount() - 1);

            VIN = source.getRoi();
            lastRoi = "VIN";

            c.insets = new Insets(10, 10, 10, 10);
            fill(GridBagConstraints.WEST, 0, 0, 3, 1, isIndex);
            fill(GridBagConstraints.WEST, 0, 1, 3, 1, hasSeptumBubbles);
            fill(GridBagConstraints.WEST, 0, 2, 3, 1, hasTissueBubbles);
            fill(GridBagConstraints.WEST, 0, 3, 3, 1, hasFibers);
            fill(GridBagConstraints.WEST, 0, 4, 3, 1, hasLesions);
            fill(GridBagConstraints.WEST, 0, 5, 3, 1, hasArtifacts);
            fill(GridBagConstraints.CENTER, 0, 6, 1, 0.5, new Label("All"));
            fill(GridBagConstraints.CENTER, 1, 6, 1, 0.5, new Label("Area at risk"));
            fill(GridBagConstraints.CENTER, 2, 6, 1, 0.5, new Label("Necrotic"));
            fill(GridBagConstraints.CENTER, 0, 7, 1, 0.5, ViableInfarctedNecrotic);
            fill(GridBagConstraints.CENTER, 1, 7, 1, 0.5, InfarctedNecrotic);
            fill(GridBagConstraints.CENTER, 2, 7, 1, 0.5, Necrotic);
            fill(GridBagConstraints.EAST, 0, 8, 3, 1, finished);
            
            
            ViableInfarctedNecrotic.addItemListener(ROISel);
            InfarctedNecrotic.addItemListener(ROISel);
            Necrotic.addItemListener(ROISel);
            finished.addActionListener(ButSel);
            pack();
        }
        
        private void fill(int anchor, int gridx, int gridy, int gridwidth, double weightx, Component comp) {
            c.anchor = anchor;
            c.gridx = gridx;
            c.gridy = gridy;
            c.gridwidth = gridwidth;
            c.weightx = weightx;
            add(comp,c);
        }
        
        ItemListener ROISel = new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if ("VIN".equals(lastRoi)) {
                    VIN = source.getRoi();
                } else if ("IN".equals(lastRoi)) {
                    IN = source.getRoi();
                } else if ("N".equals(lastRoi)) {
                    N = source.getRoi();
                }
                
                if (ViableInfarctedNecrotic.getState()) {
                    source.setRoi(VIN);
                    lastRoi = "VIN";
                }
                
                if (InfarctedNecrotic.getState()) {
                    source.setRoi(IN);
                    lastRoi = "IN";
                }
                
                if (Necrotic.getState()) {
                    source.setRoi(N);
                    lastRoi = "N";
                }
            }
        };
        
        ActionListener ButSel = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                IJ.showMessage("YO");
            }
        
        };
    }
}
