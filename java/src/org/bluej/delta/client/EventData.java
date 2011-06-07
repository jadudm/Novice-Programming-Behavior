package org.bluej.delta.client;

import java.io.File;
import java.util.Iterator;

/**
 * An EventData is a collection of information about some events in BlueJ. An
 * EventData can either map directly to one BlueJ-event or be composed of
 * several BlueJ events.
 * 
 * By inheriting this class, the logs will get a sequence number so the sequence
 * of events can be reconstructed.
 * 
 * @author Poul Henriksen
 */
public abstract class EventData
{
    private static int idCount = 0; // ID that can be used to reconstruct the
                                    // sequence in which events took place.
    private int id = idCount++;

    /**
     * The ID for this event. IDs are assigned sequentially in the order at
     * which the events are created.
     * 
     */
    public int getSeqNumber()
    {
        return id;
    }

    /**
     * A string used to identify the name of this EventData.
     * 
     */
    public abstract String getName();

    /**
     * Returns all the data gathered in this Log.
     * 
     * @return An iterator that returns key/value pairs of the type Pair.
     * @see org.bluej.delta.util.Pair
     */
    public abstract Iterator iterator();

    /**
     * Returns the time at which this event was created.
     * 
     * @return Time in ms.
     */
    public abstract int getStartTime();

    /**
     * Return the time at which this event finished.
     * 
     * @return Time in ms
     */
    public abstract int getEndTime();
    
    /**
     * Attempt to get the package directory that this EventData is related to.
     * Returning null means that it was not possible to determine.
     */
    public abstract File getPackageDir();
}
