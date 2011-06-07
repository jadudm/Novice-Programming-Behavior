package org.bluej.delta.client.invocation;

import org.bluej.delta.client.EventData;
import org.bluej.delta.client.DeltaMain;

import bluej.extensions.event.InvocationEvent;
import bluej.extensions.event.InvocationListener;

/**
 * The invocation logger logs all invocation events recieved from BlueJ and
 * constructs InvocationDatas and ships them of when they are done.
 * 
 * @author Poul Henriksen
 * 
 */
public class InvocationLogger implements InvocationListener
{    
    private DeltaMain main;

    public InvocationLogger(DeltaMain main)
    {
        this.main = main;
    }

    public void invocationFinished(InvocationEvent event)
    {
        EventData eventData = new InvocationData(event);
        main.ship(eventData);
    }
}
