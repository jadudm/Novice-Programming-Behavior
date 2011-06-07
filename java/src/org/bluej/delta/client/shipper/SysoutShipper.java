package org.bluej.delta.client.shipper;


/**
 * Shipper used for debuggging. Prints everything to System.out.
 * 
 * @author Poul Henriksen
 *
 */
public class SysoutShipper implements Shipper
{

    /**
     * Address is not used for this logger.
     */
    public void initialise(String address)
    {
        System.out.println("Sysout shipper initialised with:" + address);
    }

    public void ship(Packet p)
    {
        System.out.println(p.toString());
    }    
}
