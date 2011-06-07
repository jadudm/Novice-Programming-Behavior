package org.bluej.delta.client.compile;

import java.io.File;
import java.util.Hashtable;

import org.bluej.delta.client.DeltaMain;

import bluej.extensions.event.CompileEvent;
import bluej.extensions.event.CompileListener;

/**
 * The compilation logger logs all compile events recieved from BlueJ and
 * constructs CompileDatas and ships them of when they are done.
 * 
 * @author Poul Henriksen
 * 
 */
public class CompilationLogger
    implements CompileListener
{
    private DeltaMain main;

    /** Map from files to CompileDatas */
    private Hashtable compileData = new Hashtable();

    public CompilationLogger(DeltaMain main)
    {
        this.main = main;
    }

    public void compileStarted(CompileEvent event)
    {
        File[] files = event.getFiles();

        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            CompileData data = new CompileData(event, file);
            compileData.put(file, data);
        }
    }

    public void compileError(CompileEvent event)
    {
        File[] files = event.getFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            CompileData data = (CompileData) compileData.get(file);
            data.addMessage(event);
        }
    }

    public void compileWarning(CompileEvent event)
    {
        File[] files = event.getFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            CompileData data = (CompileData) compileData.get(file);
            data.addMessage(event);
        }
    }

    public void compileSucceeded(CompileEvent event)
    {
        end(event);
    }

    public void compileFailed(CompileEvent event)
    {
        end(event);
    }

    /**
     * Ends the logging of the files contained in the CompileEvent.
     */
    private void end(CompileEvent event)
    {
        File[] files = event.getFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            CompileData data = (CompileData) compileData.remove(file);
            data.end(event);
            main.ship(data);
        }
    }
}
