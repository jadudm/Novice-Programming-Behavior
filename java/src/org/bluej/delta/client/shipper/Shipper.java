package org.bluej.delta.client.shipper;


/**
 * Interface for all shippers.
 * <p>
 * <b> 
 * IMPORTANT: All implementations must have a public default constructor!
 * </b>
 * @author Poul Henriksen
 *
 */
public interface Shipper
{
    /**
     * Initialise the shipper with the address picked up from the
     * delta.properties file.
     * 
     */
    public void initialise(String address);

    /**
     * Packet to ship.
     */
    public void ship(Packet p);
}
