
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;

import ij.gui.GenericDialog;
import ij.gui.Line;
import ij.gui.Roi;

import ij.plugin.PlugIn;
import ij.plugin.filter.Rotator;

import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import java.awt.Rectangle;

import java.util.ArrayList;
import java.util.Iterator;
import java.lang.Math;

/**
 * Select two images with a Line ROI in each, and rotate/translate/scale one to
 * the other. Stacks are not explicitly supported, but a macro can easily use
 * this plugin for the purpose by iterating over all slices.
 */
public class Align_Image_North implements PlugIn {

    public void run(String arg) {

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

        GenericDialog gd = new GenericDialog("Align Images");
        String current = WindowManager.getCurrentImage().getTitle();
        gd.addChoice("source", titles, titles[0]);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }

        ImagePlus source = WindowManager.getImage(ids[gd.getNextChoiceIndex()]);
        Line sr = (Line) source.getRoi();
        AngleLine line1 = new AngleLine(sr.x1, sr.y1, sr.x2, sr.y2);
        source.unlock();
        IJ.run("Select All");
        IJ.run("Rotate...", "angle=" + Math.toDegrees(line1.angle));
        Roi roi = source.getRoi();
        Rectangle r = roi.getBounds();
        int x_ = r.width > source.getWidth()? r.width: source.getWidth();
        int y_ = r.height > source.getHeight()? r.height: source.getHeight();
        IJ.run("Canvas Size...", "width=" + x_ + " height=" + y_ + " position=Center zero");


        ImageProcessor result = source.getProcessor();
        result.rotate(Math.toDegrees(line1.angle));
        source.draw();
    }

    /**
     * Align an image to another image given line selections in each.
     */
    public static ImageProcessor align(ImageProcessor source, AngleLine line1) {
        ImageProcessor result = new FloatProcessor(0, 0);
        source.rotate(Math.toDegrees(line1.angle));
        result.insert(source, 0, 0);
        return result;
    }

    protected static abstract class Interpolator {

        ImageProcessor ip;
        int w, h;

        public Interpolator(ImageProcessor ip) {
            this.ip = ip;
            w = ip.getWidth();
            h = ip.getHeight();
        }

        public abstract float get(float x, float y);
    }

    protected static class BilinearInterpolator extends Interpolator {

        public BilinearInterpolator(ImageProcessor ip) {
            super(ip);
        }

        public float get(float x, float y) {
            int i = (int) x;
            int j = (int) y;
            float fx = x - i;
            float fy = y - j;
            float v00 = ip.getPixelValue(i, j);
            float v01 = ip.getPixelValue(i + 1, j);
            float v10 = ip.getPixelValue(i, j + 1);
            float v11 = ip.getPixelValue(i + 1, j + 1);
            return (1 - fx) * (1 - fy) * v00 + fx * (1 - fy) * v01
                    + (1 - fx) * fy * v10 + fx * fy * v11;
        }
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