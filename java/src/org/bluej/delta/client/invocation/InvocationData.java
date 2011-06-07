package org.bluej.delta.client.invocation;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.bluej.delta.client.EventData;
import org.bluej.delta.util.Debug;
import org.bluej.delta.util.Pair;
import org.bluej.delta.util.Util;

import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.extensions.event.InvocationEvent;

/**
 * Logs information about an invocation in BlueJ. An invocation is when a user
 * invokes a method or a constructor.
 * 
 * @author Poul Henriksen
 * 
 */
public class InvocationData extends EventData
{
    private int startTime;
    private int endTime;

    private List keysAndValues = new LinkedList();
    private File packageDir;
    
    public InvocationData(InvocationEvent event)
    {
        startTime = endTime = Util.getTime();
        try {
            keysAndValues.add(new Pair("PACKAGE", event.getPackage().getName()));
        }
        catch (ProjectNotOpenException e) {
            Debug.reportException(e);
            keysAndValues.add(new Pair("PACKAGE", "PROJECT_NOT_OPEN_EXCEPTION"));
        }
        catch (PackageNotFoundException e) {
            Debug.reportException(e);
            keysAndValues.add(new Pair("PACKAGE", "PACKAGE_NOT_FOUND_EXCEPTION"));
        }
        keysAndValues.add(new Pair("CLASS_NAME", event.getClassName()));
        String objectName = event.getObjectName();
        if (objectName == null) {
            objectName = "";
        }
        keysAndValues.add(new Pair("OBJECT_NAME", objectName));
        String methodName = event.getMethodName();
        if (methodName == null) {
            methodName = "";
        };
        keysAndValues.add(new Pair("METHOD_NAME", methodName));
        keysAndValues.add(new Pair("PARAMETER_TYPES", Util.stringArray2String(Util.classesToStrings(event
                .getSignature()))));
        /**
         * Crap! getSignature returns null for the types if they are of any of
         * the simple types. That sucks a bit. We still get useful parameter
         * values though, so it is not catastrophic. BlueJ 2.1.3 and below only.
         * Fixed in later BlueJs.
         */

        String[] params = event.getParameters();
        if (params == null) {
            params = new String[]{};
        }
        keysAndValues.add(new Pair("PARAMETERS", Util.stringArray2String(params)));

        String result = "";
        Object resultObj = event.getResult();
        if(resultObj != null) {
            result = resultObj.toString();
        }
        keysAndValues.add(new Pair("RESULT", result));
        // TODO: The static result type is nowhere? Yep, that is right, no way to get it with the current extension API.

        // Hmpf. Ugly constants.
        String invocationStatus = "NO_STATUS";
        switch(event.getInvocationStatus()) {
            case InvocationEvent.UNKNOWN_EXIT :
                invocationStatus = "UNKNOWN_EXIT";
                break;
            case InvocationEvent.EXCEPTION_EXIT :
                invocationStatus = "EXCEPTION_EXIT";
                break;
            case InvocationEvent.FORCED_EXIT :
                invocationStatus = "FORCED__EXIT";
                break;
            case InvocationEvent.NORMAL_EXIT :
                invocationStatus = "NORMAL_EXIT";
                break;
            case InvocationEvent.TERMINATED_EXIT :
                invocationStatus = "TERMINATED_EXIT";
                break;
        }
        keysAndValues.add(new Pair("INVOCATION_STATUS", invocationStatus));        

        try {
            packageDir = event.getPackage().getDir();
        }
        catch (ProjectNotOpenException e1) {
            Debug.reportException(e1);
        }
        catch (PackageNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * The time at which the invocation started.
     * 
     * @return Time in ms.
     */
    public int getStartTime()
    {
        return startTime;
    }

    /**
     * The time at which the invocation finished.
     * 
     * @return Time in ms.
     */
    public int getEndTime()
    {
        return endTime;
    }

    /**
     * Return a string identifying this EventData.
     * 
     * @return Returns the string: "InvocationData"
     */
    public String getName()
    {
        return "InvocationData";
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

    public File getPackageDir()
    {
        return packageDir;
    }

}
