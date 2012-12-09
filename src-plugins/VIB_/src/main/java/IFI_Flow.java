
import ij.gui.*;
import ij.*;
import ij.process.*;
import ij.plugin.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;

public class IFI_Flow implements PlugIn {

    public void run(String arg) {
        //IJ.run("Stack to RGB");
        IJ.run("Rename...","title=Working");
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

        GenericDialog gd = new GenericDialog("Align Image");
        String current = WindowManager.getCurrentImage().getTitle();
        gd.addChoice("source", titles, titles[0]);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }

        ImagePlus source = WindowManager.getImage(ids[gd.getNextChoiceIndex()]);
        Arrow sr = (Arrow) source.getRoi();
        AngleLine line1 = new AngleLine(sr.x1,sr.y1,sr.x2,sr.y2);
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
        Versatile_IFIWand wand = new Versatile_IFIWand();
        wand.doWand(source, line1.x2, line1.y2);
    }

    public static ImageProcessor align(ImageProcessor source, AngleLine line1) {
        ImageProcessor result = new FloatProcessor(0, 0);
        source.rotate(Math.toDegrees(line1.angle));
        result.insert(source, 0, 0);
        return result;
    }

    protected static class AngleLine extends ij.gui.Line {

        public double angle = 0;

        public AngleLine(int ox1, int oy1, int ox2, int oy2) {
            super(ox1, oy1, ox2, oy2);
            angle = Math.atan2((float) (oy1 - oy2), (float) (ox2 - ox1)) - Math.PI / 2;
            IJ.showMessage("" + Math.toDegrees(angle));
        }
    }

}