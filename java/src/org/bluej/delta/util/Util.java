package org.bluej.delta.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Vector;


/**
 * Some utility methods.
 * 
 * @author Poul Henriksen
 *
 */
public class Util
{
    private static long timeOffset = System.currentTimeMillis();    

    /** Used for encoding */
    private static MessageDigest md ;
    
    static {
        try {
            md = MessageDigest.getInstance("MD5");
        }
        catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    /**
     * Get the default encoding for this platform.
     * 
     * @see java.io.OutputStreamWriter.#getEncoding()
     */
    public static String getDefaultPlatformEncoding()
    {
        OutputStream out = new ByteArrayOutputStream();
        return (new OutputStreamWriter(out)).getEncoding();
    }

    /**
     * Get the line separator on this platform.
     */
    public static String getNewline()
    {
        return System.getProperty("line.separator");
    }
    
    /**
     * Read the contents of the file and return it in a string. The string will
     * be encoded with the default platform encoding.
     * <p>
     * We could translate it into UTF8 or ISO8859 or??? Maybe we should leave
     * that decision to the shipper.
     * 
     * @return A String in the default platform encoding.
     */
    public static String getFileContents(File file)
    {
        StringBuffer gatherer = new StringBuffer();
        int ch;

        FileReader fr;
        try {
            fr = new FileReader(file);

            BufferedReader bf = new BufferedReader(fr);
            String line;
            while ((line = bf.readLine()) != null) {
                gatherer.append(line);
                gatherer.append(Util.getNewline()); // add the line break that
                                                    // was removed by readLine()
            }
            bf.close();
        }
        catch (FileNotFoundException e) {
            Debug.reportException(e);
        }
        catch (IOException e) {
            Debug.reportException(e);
        }
        
        return gatherer.toString();
    }

    /**
     * Takes an array of classes and returns an array with the class names of
     * those classes. Will never return null.
     */
    public static String[] classesToStrings(Class[] classes)
    {
        String[] names = new String[classes.length];
        for (int i = 0; i < classes.length; i++) {
            Class cls = classes[i];
            // Currently there is a bug in the extensions that returns null for
            // all primitve types.
            if (cls != null) {
                names[i] = cls.getName();
            }
            else {
                names[i] = "CLASS_WAS_NULL";
            }
        }
        return names;
    }
    
    /**
     * Returns the time since this class was loaded.
     * @return time in ms
     */
    public static int getTime() {
        return (int) ((System.currentTimeMillis() - timeOffset));
    }

    public static String stringArray2String(String[] strings)
    {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < strings.length; i++) {
            buf.append(strings[i] + ", ");                    
        }
        if(strings.length > 0) {
            //remove last ", "
            buf.delete(buf.length() - 2,buf.length());
        }
        return buf.toString();
    }
    
    public static Vector stringArray2Vector(String[]  strings)
    {
        Vector stringArray = new Vector(strings.length);
        for (int i = 0; i < strings.length; i++) {
            String string = strings[i];
            stringArray.add(string);
        }
        return stringArray;
    } 
    
    public static byte[] xor(byte[] key, byte[] random)
    {
        byte[] smaller = key;
        byte[] larger = random;
        if (smaller.length > larger.length) {
            smaller = random;
            larger = key;
        }
        byte[] xored = new byte[larger.length];
        for (int i = 0; i < xored.length; i++) {
            xored[i] = (byte) (larger[i] ^ smaller[i % smaller.length]);
        }
        return xored;
    }
    
    /**
     * Replaces every occurence of the userName in the given string with a scrambled version of the username.
     */
    public static String scrambleUserName(String str, String userName) {
        String scrambled = scrambleString(userName);
        return replaceString(str, userName, scrambled);
    }
    
    private static String scrambleString(String userName)
    {        
        try {
            byte[] md5user = MessageDigest.getInstance("MD5").digest(userName.getBytes());
            StringBuffer str = new StringBuffer();
            for (int i = 0; i < md5user.length; i++) {
                byte b = md5user[i];
                str.append(Integer.toHexString(128+(int) b));
            }
            return str.toString();
        }
        catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        //Should not happen.
        return userName;
    }

    /**
     * Replace first occurence of a string.
     * 
     * @param string String wherin the replacement should happen.
     * @param org The string to be removed
     * @param replace The string to be inserted
     * @return The string with replacements
     */
    public static String replaceString(String string, String org, String replace)
    {
        
        //TODO: make it a while instead of if to replace all occurencces
        int start = string.indexOf(org);
        if(start <= -1) {
            return string;
        }
        int end = start + org.length();
        string = string.substring(0,start) + replace + string.substring(end);
        return string;
    }
    
    /**
     * Calculate the md5 sum of the given byte array and return it as a string of hexadecimal values.
     * 
     * @param bytes Array of bytes
     * @return MD5 string
     */
    public static String stringMd5(byte[] bytes)
    {
        md.reset();
        byte[] md5Bytes = md.digest(bytes);                
        StringBuffer hexString = new StringBuffer();
        for (int i=0;i<md5Bytes.length;i++) {
             String hex=Integer.toHexString(0xff & md5Bytes[i]);
             if(hex.length()==1) hexString.append('0');
             hexString.append(hex);
        }                
        return hexString.toString();
    }

}
