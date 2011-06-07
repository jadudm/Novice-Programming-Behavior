import org.bluej.delta.client.shipper.Packet;
import org.bluej.delta.client.shipper.XmlRpcShipper;
import org.bluej.delta.util.Debug;
import org.bluej.delta.util.Pair;


public class FindStupidBug
{
    public static void main(String[] args)
    {
        Debug.setEnabled(true);
        Debug.println("BLABAL");
        XmlRpcShipper shipper = new XmlRpcShipper();
        shipper.initialise("http://bluej.org:9000/servlets/rpcdb/authdb.ss");
        System.out.println("Shipping 1");
        Packet packet = new Packet("polle");
        packet.setName("TESTPACKET");
        packet.add(new Pair("POLLEKEY", "POLLEVALUE"));
        shipper.ship(packet);
        System.out.println("Shipping 2");
       
    }
}
