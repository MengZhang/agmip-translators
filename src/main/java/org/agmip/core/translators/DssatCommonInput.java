package org.agmip.core.translators;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.agmip.core.types.AdvancedHashMap;
import org.agmip.core.types.TranslatorInput;

/**
 * DSSAT Experiment Data I/O API Class
 *
 * @author Meng Zhang
 * @version 1.0
 */
public abstract class DssatCommonInput implements TranslatorInput {

    protected String[] flg = new String[3];
    protected String defValR = "-99.0";
    protected String defValC = "";
    protected String defValI = "-99";
    protected String defValD = "20110101";
    protected String jsonKey = "unknown";

    /**
     * DSSAT Data Output method for Controller using
     * 
     * @param m  The holder for BufferReader objects for all files
     * @return result data holder object
     */
    protected abstract AdvancedHashMap readFile(HashMap m) throws IOException;
    
   /**
     * DSSAT XFile Data input method
     * 
     * @param arg0  file name
     * @return result data holder object
     */
    @Override
    public AdvancedHashMap readFile(String arg0) {

        AdvancedHashMap ret = new AdvancedHashMap();
        String filePath = arg0;

        try {
            // read file by file
            ret = readFile(getBufferReader(filePath));

        } catch (Exception e) {
            System.out.println(e.toString());
        }
        
        return ret;
    }
    
    /**
     * Set reading flgs for reading lines
     * 
     * @param line  the string of reading line
     */
    protected void judgeContentType(String line) {
        // Section Title line
        if (line.startsWith("*")) {

            setTitleFlgs(line);

        } // Data title line
        else if (line.startsWith("@")) {

            flg[1] = line.substring(1).trim().toLowerCase();
            flg[2] = "";

        } // Comment line
        else if (line.startsWith("!")) {

            flg[2] = "comment";

        } // Data line
        else if (!line.trim().equals("")) {

            flg[2] = "data";

        } // Continued blank line
        else if (flg[2].equals("blank")) {

            flg[1] = "";
            flg[2] = "blank";

        } else {

            flg[0] = "";
            flg[1] = "";
            flg[2] = "blank";
        }
    }

    /**
     * Set reading flgs for title lines
     * 
     * @param line  the string of reading line
     */
    protected abstract void setTitleFlgs(String line);

    /**
     * Translate data str from "yyddd" to "yyyymmdd"
     *
     * @param str date string with format of "yyddd"
     * @return result date string with format of "yyyymmdd"
     */
    protected String translateDateStr(String str) {

        return translateDateStr(str, "0");
    }

    /**
     * Translate data str from "yyddd" to "yyyymmdd" plus days you want
     *
     * @param startDate date string with format of "yyydd"
     * @param strDays the number of days need to be added on
     * @return result date string with format of "yyyymmdd"
     */
    protected String translateDateStr(String startDate, String strDays) {

        // Initial Calendar object
        Calendar cal = Calendar.getInstance();
        int days;
        int year;
        if (startDate.length() > 5 || startDate.length() < 4) {
            //throw new Exception("");
            return "-99"; //defValD;
        }
        try {
            startDate = String.format("%05d", Integer.parseInt(startDate));
            days = Double.valueOf(strDays).intValue();
            // Set date with input value
            year = Integer.parseInt(startDate.substring(0, 2));
            year += year <= 30 ? 2000 : 1900; // TODO Need confirm that which year is the begining of DSSAT 
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.DAY_OF_YEAR, Integer.parseInt(startDate.substring(2)));
            cal.add(Calendar.DATE, days);
            // translatet to yyddd format
            return String.format("%1$04d%2$02d%3$02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
        } catch (Exception e) {
            // if tranlate failed, then use default value for date
            // sbError.append("! Waring: There is a invalid date [").append(startDate).append("]");
            return "-99"; //formatDateStr(defValD);
        }

    }

    /**
     * Divide the data in the line into a map
     *
     * @param line The string of line read from data file
     * @param formats The defination of lenght for each data field (String itemName : Integer length)
     * @return the map contains divided data with keys from original string
     */
    protected AdvancedHashMap readLine(String line, LinkedHashMap<String, Integer> formats) {

        AdvancedHashMap ret = new AdvancedHashMap();
        int length;

        for (String key : formats.keySet()) {
            length = (Integer) formats.get(key);
            if (length <= line.length()) {
                ret.put(key, line.substring(0, length));
                line = line.substring(length);
            } else {
                ret.put(key, line);
                line = "";
            }
        }

        return ret;
    }

    /**
     * Get exname with normal format
     *
     * @return exname
     */
    protected String getExName() {

        // TODO
        String ret = "";

        return ret;
    }

    /**
     * Check if input is a valid value
     *
     * @return check result
     */
    protected boolean checkValidValue(String value) {
        if (value.trim().equals(defValC) || value.equals(defValI) || value.equals(defValR)) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Get BufferReader for each type of file
     *
     * @param filePath the full path of the input file
     * @return result the holder of BufferReader for different type of files
     * @throws FileNotFoundException
     * @throws IOException
     */
    protected HashMap getBufferReader(String filePath) throws FileNotFoundException, IOException {

        HashMap result = new HashMap();
        InputStream in;
        HashMap mapW = new HashMap();
        HashMap mapS = new HashMap();
        String[] tmp = filePath.split("[\\/]");

        // If input File is ZIP file
        if (filePath.toUpperCase().endsWith(".ZIP")) {

            ZipEntry entry;
            in = new ZipInputStream(new FileInputStream(filePath));

            while ((entry = ((ZipInputStream) in).getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    if (entry.getName().matches(".+\\.\\w{2}[Xx]")) {
                        result.put("X", getBuf(in, (int) entry.getSize()));
                    } else if (entry.getName().toUpperCase().endsWith(".WTH")) {
                        mapW.put(entry.getName().toUpperCase(), getBuf(in, (int) entry.getSize()));
                    } else if (entry.getName().toUpperCase().endsWith(".SOL")) {
                        mapS.put(entry.getName().toUpperCase(), getBuf(in, (int) entry.getSize()));
                    } else if (entry.getName().matches(".+\\.\\w{2}[Aa]")) {
                        result.put("A", getBuf(in, (int) entry.getSize()));
                    } else if (entry.getName().matches(".+\\.\\w{2}[Tt]")) {
                        result.put("T", getBuf(in, (int) entry.getSize()));
                    }
                }
            }
        } // If input File is not ZIP file
        else {
            in = new FileInputStream(filePath);
            if (filePath.matches(".+\\.\\w{2}[Xx]")) {
                result.put("X", new BufferedReader(new InputStreamReader(in)));
            } else if (filePath.toUpperCase().endsWith(".WTH")) {
                mapW.put(filePath, new BufferedReader(new InputStreamReader(in)));
            } else if (filePath.toUpperCase().endsWith(".SOL")) {
                mapS.put(filePath, new BufferedReader(new InputStreamReader(in)));
            } else if (filePath.matches(".+\\.\\w{2}[Aa]")) {
                result.put("A", new BufferedReader(new InputStreamReader(in)));
            } else if (filePath.matches(".+\\.\\w{2}[Tt]")) {
                result.put("T", new BufferedReader(new InputStreamReader(in)));
            }
        }

        result.put("W", mapW);
        result.put("S", mapS);
        result.put("Z", tmp[tmp.length - 1]);

        return result;
    }

    /**
     * Get BufferReader object from Zip entry
     *
     * @param in The input stream of zip file
     * @param size The entry size
     * @return result The BufferReader object for current entry
     * @throws IOException
     */
    private BufferedReader getBuf(InputStream in, int size) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        char[] buf = new char[size];
        br.read(buf);
        return new BufferedReader(new CharArrayReader(buf));
    }
}
