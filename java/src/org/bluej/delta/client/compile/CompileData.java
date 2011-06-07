package org.bluej.delta.client.compile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.bluej.delta.client.EventData;
import org.bluej.delta.util.Debug;
import org.bluej.delta.util.Pair;
import org.bluej.delta.util.Util;

import bluej.extensions.event.CompileEvent;


/**
 * Information about a compilation of one file.
 * 
 * @author Poul Henriksen
 */
public class CompileData extends EventData
{
    /** Number of times the compiler has been invoked. */
    private static int totalCompiles; // TODO: Is this per project? or perBlueJ? Should be per project. It is per BlueJ!

    /** Counts how many times each file has been compiled */
    private static HashMap perFileCompileCount = new HashMap();

    /** When the first event for this CompileData was fired. */
    private int startTime;

    /** When the last event for this CompileData was fired. */
    private int endTime;

    /** The file in this compilation */
    private File file;

    /** Compiler message (warnings and errors) */
    private CompileMessage message = new CompileMessage("", "", -1);;
    
    /** All the data in one list of key/value pairs. */
    private List keysAndValues = new LinkedList();

    /** Whether the compilation finished successfully. */
    private boolean compileSuccessful = false;

    /** Directory for the package that the file in this CompileData belongs to. */
    private File packageDir;

    /**
     * Creates a new log for the compile event.
     */
    public CompileData(CompileEvent ce, File file)
    {
        if (ce.getEvent() != CompileEvent.COMPILE_START_EVENT) {
            throw new IllegalArgumentException("Initial CompileEvent must be of type COMPILE_START_EVENT (val="
                    + CompileEvent.COMPILE_START_EVENT + "). It was: " + ce.getEvent());
        }
        startTime = Util.getTime();
        totalCompiles++;
        this.packageDir = file.getParentFile();
        this.file = file;
    }

    /**
     * Adds a CompileEvent to this compile log.
     * 
     * @param ce The compile event. Should be of type COMPILE_WARNING_EVENT or
     *            COMPILE_ERROR_EVENT.
     */
    public void addMessage(CompileEvent ce)
    {
        String type;
        if (ce.getEvent() == CompileEvent.COMPILE_WARNING_EVENT) {
            type = CompileMessage.WARNING;
        }
        else if (ce.getEvent() == CompileEvent.COMPILE_ERROR_EVENT) {
            type = CompileMessage.ERROR;;
        }
        else {
            throw new IllegalArgumentException(
                    "Initial CompileEvent must be of type COMPILE_WARNING_EVENT or COMPILE_ERROR_EVENT. It was: "
                            + ce.getEvent());
        }

        message = new CompileMessage(type, ce.getErrorMessage(), ce.getErrorLineNumber());
    }

    /**
     * Signals the ending of an CompileData. One CompileData can contain compile information
     * for only one file. BlueJ (2.1.3), will always generate one
     * COMPILE_DONE_EVENT or COMPILE_FAILED_EVENT per file.
     * 
     * @param ce The compile event. Should be of type COMPILE_DONE_EVENT or
     *            COMPILE_FAILED_EVENT.
     */
    public void end(CompileEvent ce)
    {
        if (ce.getEvent() != CompileEvent.COMPILE_DONE_EVENT && ce.getEvent() != CompileEvent.COMPILE_FAILED_EVENT) {
            throw new IllegalArgumentException(
                    "Ending CompileEvent must be of type COMPILE_DONE_EVENT or COMPILE_FAILED_EVENT. It was: "
                            + ce.getEvent());
        }
        compileSuccessful = (ce.getEvent() == CompileEvent.COMPILE_DONE_EVENT);
        endTime = Util.getTime();
        
        String name = file.getName();
        Integer value = (Integer) perFileCompileCount.get((Object) name);
        if (value != null) {
            Integer currentValue = (Integer) perFileCompileCount.get((Object) name);
            perFileCompileCount.put((Object) name, new Integer(1 + currentValue.intValue()));
        }
        else {
            perFileCompileCount.put((Object) name, new Integer(1));
        }
       
        createList();
    }

    /**
     * Return a string identifying this EventData.
     *  
     * @return Returns the string: "CompileData"
     */
    public String getName()
    {
        return "CompileData";
    }
    
    /**
     * The time at which the compilation started..
     * 
     * @return Time in ms
     */
    public int getStartTime()
    {
        return startTime;
    }

    /**
     * The time at which the compilation finished.
     * 
     * @return Time in ms.
     */
    public int getEndTime()
    {
        return endTime;
    }

    /**
     * Returns all the information gathered for this compile in the form of
     * key/value pairs.
     * 
     * @return An iterator that returns key/value pairs of the type Pair.
     * @see org.bluej.delta.util.Pair
     */
    public Iterator iterator()
    {
        return keysAndValues.listIterator();
    }

    /**
     * Creates a list of all the information gathered for this compilation.
     * 
     * @throws IOException if it can not read the source files.
     * @see #iterator()
     */
    private void createList()
    {
        try {
            keysAndValues.add(new Pair("FILE_PATH", file.getCanonicalPath()));
        }
        catch (IOException e) {
            Debug.reportException(e);
        }
        keysAndValues.add(new Pair("FILE_NAME", file.getName()));
        keysAndValues.add(new Pair("FILE_CONTENTS", Util.getFileContents(file)));
        keysAndValues.add(new Pair("FILE_ENCODING", Util.getDefaultPlatformEncoding()));

        keysAndValues.add(new Pair("COMPILE_SUCCESSFUL", compileSuccessful));
        
        keysAndValues.add(new Pair("MSG_TYPE", message.getType()));
        keysAndValues.add(new Pair("MSG_MESSAGE", message.getMessage()));
        keysAndValues.add(new Pair("MSG_LINE_NUMBER", message.getLineNumber()));

        keysAndValues.add(new Pair("COMPILES_PER_FILE", (Integer) perFileCompileCount.get(file.getName())));
        keysAndValues.add(new Pair("TOTAL_COMPILES", totalCompiles));

    }

    public File getPackageDir()
    {
        return packageDir;
    }
}
