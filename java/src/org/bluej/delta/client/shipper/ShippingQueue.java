package org.bluej.delta.client.shipper;

import java.util.LinkedList;

import org.bluej.delta.util.Debug;


/**
 * A queue of items to be shipped.
 * 
 * @author Poul Henriksen
 *
 */
public class ShippingQueue extends Thread
{
    private Shipper shipper;
    private LinkedList queue = new LinkedList();
    private boolean stop;
    
    public ShippingQueue(Shipper shipper) {
        this.shipper = shipper;
        this.start();
    }
    
    /**
     * Adds a packet to the queue that will be sent as soon as possible.
     */
    public void add(Packet p) {        
        synchronized(queue) {
            queue.add(p);
            queue.notifyAll();
        }
    }
  
    
    /**
     * The thread that sends the items from the queue.
     */
    public void run() {
        stop = false;
        while(! stop) {
            synchronized(queue) {
                if(queue.isEmpty()) {
                    try {
                        queue.wait();
                    }
                    catch (InterruptedException e) {
                        Debug.reportException(e);
                    }
                }
                Packet p = (Packet) queue.getFirst();
                queue.removeFirst();
                shipper.ship(p);
            }
        }
    }
    
    /**
     * Stop sending from this queue.
     */
    public void end() {        
        stop = true;
        synchronized(queue) {
            queue.notifyAll();
        }
    }
}
