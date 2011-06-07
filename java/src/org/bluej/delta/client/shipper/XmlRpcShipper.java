package org.bluej.delta.client.shipper;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.apache.xmlrpc.AsyncCallback;
import org.apache.xmlrpc.XmlRpcClient;
import org.apache.xmlrpc.XmlRpcClientLite;
import org.bluej.delta.util.Debug;
import org.bluej.delta.util.Pair;
import org.bluej.delta.util.Util;


/**
 * Sends the data through xml-rpc.
 * 
 * 
 * @author Poul Henriksen
 *
 */
public class XmlRpcShipper
    implements Shipper
{

    /** What types should be used in the database. */
    private final static Hashtable TYPE_MAP = new Hashtable();
    
    static {
        TYPE_MAP.put(String.class, "text");
        TYPE_MAP.put(Integer.class, "integer");
        TYPE_MAP.put(int.class, "integer");
        TYPE_MAP.put(Double.class, "double");
        TYPE_MAP.put(double.class, "double");
        TYPE_MAP.put(Boolean.class, "boolean");
        TYPE_MAP.put(boolean.class, "boolean");
     //   TYPE_MAP.put(String[].class, "text"); 
    }
    

    // Names of the RPC method names.
    private static final String XML_RPC_INSERT = "insert"; // Insert without authentication.
    private static final String XML_RPC_INSERT_SECURE = "insert.secure"; // Insert with authentication.
    private static final String XML_RPC_INIT = "init";  // Call used for authentication. Returns a random number.

    /** The XML-RPC client */
    private XmlRpcClient client;
    
    /**
     * A callback from the RPC that does nothing more than report errors.
     */
    private AsyncCallback insertCallBack = new AsyncCallback() {
        public void handleError(Exception exception, URL url, String method)
        {
            Debug.reportException(exception);
        }

        public void handleResult(Object result, URL url, String method)
        {
            Debug.println("Send packet succesfully without authentication.");
            
        }
    };
    
    /**
     * A callback from the RPC that does nothing more than report errors.
     */
    private AsyncCallback secureInsertCallBack = new AsyncCallback() {
        public void handleError(Exception exception, URL url, String method)
        {
            Debug.reportException(exception);
        }

        public void handleResult(Object result, URL url, String method)
        {
            Debug.println("Send packet succesfully with authentication.");
            
        }
    };

    /**
     * Initialises the connectiong to the given address. The string should be in the URL format.
     * 
     * @see java.net.URL
     */
    public void initialise(String address)
    {
        try {
            client = new XmlRpcClientLite(address);
            //TODO: test the connection ?
        }
        catch (MalformedURLException e) {
            Debug.reportException(e);
        }
    }

    /**
     * Sends a packet.
     */
    public void ship(final Packet p)
    {
        Debug.println("Sending with XmlRpcShipper");

				final Vector v = createXmlRpcData(p);
        // System.out.println("### random: " );
        // printBytes(random);
        // System.out.println("### MD5'ed: ");
        // System.out.println(encrypted);

				// INSECURE INSERT
				// These three lines are all that are needed for the insecure insert.
				// Comment them out if you want to run with a 'more secure' insert.
				// By 'more secure', I mean 'panacea of security,' not 'real security'.
				Debug.println("Sending packet without authentication: ");
				Debug.println(p);
				client.executeAsync(XML_RPC_INSERT, v, insertCallBack);

				// SECURE INSERT
				// this block comment implements a 'more secure' insert, where
				// 'more secure' implies that the server and the client 
				// have some kind of shared secret. In particular, some kind of 
				// shared 'password' or similar. However, this is not "secure"
				// in any real sense, and as such I've turned it off for now.
				
        // The protocol is like this: 
        // 1. Call INIT without any parameters.
        // 2. If, INIT returns successfull, it will return a random number R.
        // 3. If not, it will throw an error.
        // Read respective descriptions of these two cases below.
				/*
        client.executeAsync(XML_RPC_INIT, new Vector(), new AsyncCallback() {
            public void handleError(Exception exception, URL url, String method)
            {
                Debug.reportException(exception);
                Debug.println("Did not authenticate. Trying non authenticating server.");

                // We got an error!
                // It might be because it is not an authenticating server, let's try without the authentication.    
                // The INSERT expects the following parameters:
                // Field names (Vector of Strings)
                // Field types (Vector of Strings)
                // Data (map from field names (Strings) to the values (types in TYPE_MAP))
                final Vector v = createXmlRpcData(p);       

                Debug.println("Sending packet without authentication: ");
                Debug.println(p);
                client.executeAsync(XML_RPC_INSERT, v, insertCallBack);
            }

            public void handleResult(Object result, URL url, String method)
            {
                Debug.println("XML_RPC_INIT result recieved");
                // We got a random number back!
                // We will now encode the random number like this: XOR random
                // number with the key and calculate the MD5.
                // The INSERT_SECURE expects the following paramters: 
                // random number (byte array)
                // encoded number (byte array)
                // the rest of the data as in INSERT
                String keyString = "12345"; //Only use ASCII characters to avoid problems.

                byte[] key = null;
                try {
                    key = keyString.getBytes("UTF-8");
                }
                catch (UnsupportedEncodingException e) {
                    Debug.reportException(e);
                    return;
                }

                byte[] random = (byte[]) result;
                //TODO for testing the weird bug where '+' becomes a ' '
                //Will be base 64 encoded to: [120, 83, 66, 97, 43, 57, 115, 69, 105, 78, 54, 53, 70, 98, 67, 76, 77, 47, 118, 52, 75, 65, 61, 61]
                //random = new byte[] {-59, 32, 90, -5, -37, 4, -120, -34, -71, 21, -80, -117, 51, -5, -8, 40};
             
                byte[] xored = Util.xor(key, random);
                String encrypted = Util.stringMd5(xored);
                
               
                final Vector v = createXmlRpcData(p);
                v.insertElementAt(encrypted, 0); // add second parameter
                v.insertElementAt(random, 0); // add first paramter
                // System.out.println("### random: " );
                // printBytes(random);
                // System.out.println("### MD5'ed: ");
                // System.out.println(encrypted);
                Debug.println("Sending packet with authentication: ");
                Debug.println(p);
                client.executeAsync(XML_RPC_INSERT_SECURE, v, secureInsertCallBack);
            }
						
       

            private void printBytes(byte[] encrypted)
            {
                for (int i = 0; i < encrypted.length; i++) {
                    byte b = encrypted[i];
                    System.out.print(b + " ");
                }
                System.out.println();
            }
        }); */
    }
    
    /**
     * Returns a vector with the location id and vectors of fields, types, and data.
     * 
     */
    private Vector createXmlRpcData(Packet p)
    {
        Vector params = new Vector();        
        Vector fields = new Vector();
        Vector types = new Vector();
        Hashtable data = new Hashtable();
        
        for (Iterator iter = p.getData().iterator(); iter.hasNext();) {
            Pair pair = (Pair) iter.next();
            String key = pair.getKey();
            fields.add(key);
            Object value = pair.getValue();
            String type = getType(pair);
            if(type == null) {// We should NEVER accept nulls. It is the client responsibility to assign a meaningful value in this case.
                Debug.reportException(new IllegalArgumentException("Type was null for the key: " + key + ". Using string as the type."));
                type = (String) TYPE_MAP.get(String.class);
            }
            types.add(type);
            
            if (value == null) { // We should NEVER accept nulls. It is the client responsibility to assign a meaningful value in this case.
                Debug.reportException(new IllegalArgumentException("Value was null for the key: " + key + ". Using empty string as the value."));                
                data.put(key, ""); // xml-rpc can't handle null types
            }
            /*else  if (value instanceof String[]) {
                data.put(key, Util.stringArray2String((String[]) value));
            }*/
            else {
                data.put(key, pair.getValue());
            }            
        }
        params.add(p.getName());
        params.add(fields);
        params.add(types);
        params.add(data);
        
        return params;
    }

    /**
     * Tries to figure out the type of the value in a pair. It will default to
     * the String type if it can't figure it out.
     * 
     * @throws IllegalArgumentException If it cannot figure out the type.
     * 
     */
    private String getType(Pair pair)
    {
        Object value = pair.getValue();
        if (value == null) {
            return null;
        }
        else {
            Class cls = value.getClass();
            String type = (String) TYPE_MAP.get(cls);
            if (type == null) {
                Debug.reportException(new IllegalArgumentException("Could not map type: " + value.getClass() + " (key:"
                        + pair.getKey() + "). " + " Using string as the type."));
                type = (String) TYPE_MAP.get(String.class);
            }

            return type;
        }
    }
}
