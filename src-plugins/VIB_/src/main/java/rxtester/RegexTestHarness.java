/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package rxtester;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Jonas Daniel
 */
public class RegexTestHarness {

    InputStreamReader isr;
    BufferedReader reader;
    private String rx = "";
    private String s = "";
    Pattern pattern;
    Matcher matcher;

    public RegexTestHarness() {
        this("", "");
    }

    public RegexTestHarness(String nRx, String nS) {

        isr = new InputStreamReader(System.in);
        reader = new BufferedReader(isr);

        rx = nRx;
        s = nS;
    }

    public void run() {
        
        String trx = rx;
        String ts = s;

            try {
                if (trx.equals("")) {
                    System.out.println("%nEnter your regex: ");
                    trx = reader.readLine();
                }
                if (ts.equals("")) {
                    System.out.println("%nEnter your string to search: ");
                    ts = reader.readLine();
                }
            } catch (IOException e) {
                System.out.println("%nIO error.");
            }
            
        pattern = Pattern.compile(trx);
        matcher = pattern.matcher(ts);
        
        boolean found = false;
        while (matcher.find()) {
            System.out.printf("I found the text"
                    + " \"%s\" starting at "
                    + "index %d and ending at index %d.%n",
                    matcher.group(1),
                    matcher.start(),
                    matcher.end());
            found = true;
        }
        if (!found) {
            System.out.printf("No match found.%n");
        }
    }

    public String getRx() {
        return rx;
    }

    public void setRx(String rx) {
        this.rx = rx;
    }

    public String getS() {
        return s;
    }

    public void setS(String s) {
        this.s = s;
    }
}
