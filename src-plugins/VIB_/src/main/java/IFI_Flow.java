
import ij.gui.*;
import ij.*;
import ij.process.*;
import ij.plugin.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.Iterator;

public class IFI_Flow implements PlugIn, MouseWheelListener {

    public void run(String arg) {
        //IJ.run("Stack to RGB");
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
        ImagePlus source;
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
            setBrushSize((int) (10 * sign * rotation));
        } // the getWheelRotation provides the sign
        int extra = (int) (10);
        if (extra < 2) {
            extra = 2;
        }
        extra += 4; // for good measure
        IJ.getImage().draw();
    }
}
