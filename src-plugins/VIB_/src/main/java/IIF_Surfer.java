
import ij.IJ;
import ij.plugin.PlugIn;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import rxtester.RegexTestHarness;


/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Jonas Daniel
 */
public class IIF_Surfer implements PlugIn {

    private static final String rx = "(\\d{1,4})\\-(\\d{1,4})(?:\\+(\\d{1,4})\\-(\\d{1,4}))?:(\\d{1,3})";
    Pattern pattern;
    String WinRoot = "C:/Speedwork";
    String LinRoot = "/home/ogaard";
    String p = "/index.txt";
    String iDir = "/Done/seq%04d.jp2";
    String dataSaveFormat = "/Out/data/%04d.dat";

    /**
     * @param args the command line arguments
     */
    public void run(String str) {
        pattern = Pattern.compile(rx);
        File tPath = new File(toWindowsSeparator(WinRoot));
        if (tPath.exists()) {
            p = toWindowsSeparator(WinRoot + p);
            iDir = toWindowsSeparator(WinRoot + iDir);
            dataSaveFormat = toWindowsSeparator(WinRoot + dataSaveFormat);
        } else {
            p = LinRoot + p;
            iDir = LinRoot + iDir;
            dataSaveFormat = LinRoot + dataSaveFormat;
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader(p));
            String line;
            while ((line = reader.readLine()) != null) {
                processSlide(line);
            }
        } catch (IOException x) {
            x.printStackTrace();
        }
        // TODO code application logic here
    }

    public void processSlide(String line) {
        Matcher matcher = pattern.matcher(line);
        matcher.find();
        if (matcher.groupCount() > 1) {
            String id;
            int n = 0;
            int[] samples;
            samples = new int[20];
            for (int i = Integer.parseInt(matcher.group(1)); i <= Integer.parseInt(matcher.group(2)); samples[n++] = i++) {
            }
            if (matcher.group(4) != null && matcher.group(5) != null) {
                for (int i = Integer.parseInt(matcher.group(3)); i <= Integer.parseInt(matcher.group(4)); samples[n++] = i++) {
                }
            }
            id = matcher.group(5);
            for (int i = 0; samples[i] > 0; i++) {
                File tPath = new File(String.format(dataSaveFormat, samples[i]));
                if (!tPath.exists()) {
                    IJ.runPlugIn("IIF_Flow", Integer.toString(samples[i]) + "|" + id);
                }
            }
        }
    }

    private String toWindowsSeparator(String linPath) {
        return linPath.replace("/", "\\");
    }
}
