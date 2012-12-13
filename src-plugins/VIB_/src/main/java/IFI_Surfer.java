
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
public class IFI_Surfer implements PlugIn {
    
    private static final String rx = "(\\d{1,4})\\-(\\d{1,4})(?:\\+(\\d{1,4})\\-(\\d{1,4}))?:(\\d{1,3})";
    Pattern pattern;

    static String p = "C:\\Speedwork\\index.txt.txt";
    static String iDir = "C:\\Speedwork\\In\\";
    static String oDir = "C:\\Speedwork\\Out\\";
    
    
    static File[] i;
    static File[] o;

    /**
     * @param args the command line arguments
     */
    public void run(String str) {
                pattern = Pattern.compile(rx);
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

    private void processSlide(String line) {
       Matcher matcher = pattern.matcher(line);
       matcher.find();
    }
}
