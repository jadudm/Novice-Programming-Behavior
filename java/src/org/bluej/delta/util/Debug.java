package org.bluej.delta.util;


/**
 * Handles debug output.
 * 
 * @author Poul Henriksen
 *
 */
public class Debug 
{
    private static boolean enabled;

    /**
     * Reports an exception to stderr if debugging is enabled.
     * 
     */
    public static void reportException(Throwable t) {
        if(enabled) {
            t.printStackTrace();
        }
    }

    /**
     * Enables or disables debug output.
     */
    public static void setEnabled(boolean b)
    {
        enabled = b;
    }

    public static void println(Object string)
    {
        if(enabled) {
            System.err.println(string);
        }
    }
}
