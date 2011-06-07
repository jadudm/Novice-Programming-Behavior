package org.bluej.delta.client.shipper;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.bluej.delta.util.Pair;
import org.bluej.delta.util.Util;


/**
 * A packet to be shipped. A packet contains a list of key/value pairs.
 * 
 * @author Poul Henriksen
 *
 */
public class Packet
{
    private List data = new LinkedList();
    private String name = "NO_NAME_GIVEN";
    private String userName;    
    
    /**
     * TODO: add packet type here as well instead of in a separate method.

     * Creates a new packet for the given user. The username string will be
     * removed from every pair that is marked as containing the username.
     * 
     * @param userName Username to be filtered out of pairs.
     */
    public Packet(String userName) {
        this.userName = userName;
    }
   
    public void add(Pair pair) {
        Object value = pair.getValue();
        if(pair.containsUserName() && value instanceof String) {
            String filtered = Util.scrambleUserName((String) value, userName);
            data.add(new Pair(pair.getKey(), filtered));
        } else {
            data.add(pair);
        }
    }
    
    /**
     * Returns a list of Pairs.
     * 
     */
    public List getData() {
        return data;
    }


    /**
     * A name identifying the type of this packet.
     */    
    public void setName(String loc)
    {
        this.name = loc;
    }


    /**
     * A name identifying the type of this packet.
     */    
    public String getName()
    {
        return name;
    }
    
    
    /**
     * Print out all the pairs in this packet.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        for (Iterator iter = data.iterator(); iter.hasNext();) {
            Pair pair = (Pair) iter.next();
            buf.append(pair.getKey() + " : " );
            Object value = pair.getValue();
            if (value instanceof String[]) {
                buf.append(Util.stringArray2String((String[])value));
            } 
            else {
                buf.append(value);
            }
            buf.append(System.getProperty("line.separator"));
        }
        return buf.toString();
    }
}
