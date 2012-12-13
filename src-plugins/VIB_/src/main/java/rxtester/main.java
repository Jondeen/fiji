/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package rxtester;

import java.awt.Button;
import java.awt.Component;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.JFileChooser;

/**
 *
 * @author Jonas Daniel
 */
public class main {
    
    private static final String rx = "(\\d{1,4})\\-(\\d{1,4})(?:\\+(\\d{1,4})\\-(\\d{1,4}))?:(\\d{1,3})";

    static String p = "R:\\index.txt.txt";
    static String i = "C:\\Speedwork\\In\\";
    static String o = "C:\\Speedwork\\Out\\";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        RegexTestHarness rex = new RegexTestHarness();
        rex.setRx(rx);
        
        try {
            BufferedReader reader = new BufferedReader(new FileReader(p));
            String line;
            while ((line = reader.readLine()) != null) {
                rex.setS(line);
                rex.run();
            }
        } catch (IOException x) {
            x.printStackTrace();
        }
        // TODO code application logic here
    }
}
