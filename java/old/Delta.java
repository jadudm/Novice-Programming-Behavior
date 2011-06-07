import java.net.*;
import java.util.Date;
import java.util.*;
import java.io.*;
import bluej.extensions.*;
import bluej.extensions.event.*; 


// Compilation
// javac -classpath "bluej/bluejext.jar:." Delta.java

//JAR
// jar cmf manifest.txt Delta.jar *.class 

//INSTALL
// cp Delta.jar ~/.bluej/extensions/

/**
 * The Delta extension main class. It listens for compile-events and wraps those
 * into DeltaEvent, and puts them into a a DeltaGatherer which represents one
 * compilation. Once the DeltaGathere has all the information needed, it is
 * posted to with the DeltaPost.
 * 
 * @author Matt Jadud
 */
public class Delta extends Extension implements CompileListener {

    // Per-site variable
    private String        location = "KENT";
    private String        deltaversion = "20041003";

    // Member variables
    private String        postUrl;
    private long          startTime, endTime;
    private DeltaGatherer farmer;
    private DeltaPost     mailman;
    private int           clientIndex;
    private Map           perFileHash;
    private String        ipAddress;
    private String        hostname;

    /**
     * When this method is called, the extension may start its work.
     */
    public void startup (BlueJ bluej) {
	/**
	 * The clientIndex is reset to zero every time BlueJ is 
	 * started up. In theory, this gives us a contiguous
	 * programming session. However, a student could open
	 * other projects, and I'm not confident that this
	 * variable would be reset. Therefore, I should
	 * probably be listening to Project events as well...
	 */
	clientIndex = 0;
	
	/**
	 * As well as a client index, I need a per-file index,
	 * so that I can (easily) track the edit history on
	 * a particular file within a session. 
	 * I could do this in other ways (post-capture),
	 * but it gets tedious really quickly.
	 */
	perFileHash = new HashMap();
	
	// Grab the client addresses for reporting purposes.
	try {
	    InetAddress addr = InetAddress.getLocalHost();
	    String ipAddr = addr.getHostAddress();
	    hostname = addr.getHostName();
	    ipAddress = ipAddr;
	} catch (UnknownHostException e) { } 

	// And lastly, register the extension as a 
	// compile listener with BlueJ
	bluej.addCompileListener(this);
    }

    public boolean  isCompatible () { return true; }
    public String   getVersion () { return (deltaversion); }
    public String   getName() { return("Delta Gatherer"); }
    public String   getDescription () {
	return ("Captures information about programming behavior."
		+ "\nQuestions? Drop a note to mcj4@kent.ac.uk");
    }

    public URL getURL () {
	try { return new URL("mailto:mcj4@kent.ac.uk");
	} catch (Exception e) {
	    // This is a quiet extension
	    // System.out.println ("Delta: getURL: Exception=" + e.getMessage());
	    return null; }
    }
    
    
    ///////    ///////    ///////    ///////    ///////
    ///////    ///////    ///////    ///////    ///////

    private void updatePerFileCount(CompileEvent ce) {
	try {
	    java.io.File[] files = ce.getFiles();
	    
	    for(int i = 0; i < files.length;  i++) {
		String  temp_name = files[i].getName();
		Integer temp_val  = (Integer) perFileHash.get((Object)temp_name);
		if (temp_val != null) {
		    Integer tmp = (Integer) perFileHash.get((Object)temp_name);
		    perFileHash.put((Object)temp_name,
				    new Integer(1 + tmp.intValue()));
		} else {
		    perFileHash.put((Object)temp_name, new Integer(0));
		}
	    }
	} catch (Exception e) { }
    }
	

    /**
     * I have been led to believe that there will only ever
     * be one compileStarted() event, regardless of how 
     * many files are being compiled at a given moment.
     */
    public void compileStarted(CompileEvent ce) {
	/**
	 * The DeltaGatherer is an object that manages
	 * storing a list of compilation events; 
	 * I want a new object initialized (an empty list)
	 * every time compilation is restarted.
	 */ 
	startTime = System.currentTimeMillis();
	farmer = new DeltaGatherer(ce);
    }


    /** 
     * Errors and warnings are 'transients'; this is because they 
     * are never the end of the process, but something that we can
     * capture en-route to either a compileFailed or compileSucceeded
     * event (which indicates the end of compilation.)
     */
    public void compileError(CompileEvent ce) {
	farmer.addTransient(ce); }
    public void compileWarning(CompileEvent ce) {
	farmer.addTransient(ce); }
    
    /**
     * I'll end up with one compileFailed or compileSucceeded
     * event per file. Because either of these events could
     * result in the last event of a compile sequence, I've left
     * it to the farmer to decide if things are done.
     */
    public void compileFailed(CompileEvent ce) {
	updatePerFileCount(ce);
	if (farmer.end(ce)) shipIt(); }
    
    public void compileSucceeded(CompileEvent ce) {
	updatePerFileCount(ce);
	if(farmer.end(ce)) shipIt(); }


    private void shipIt() {
	endTime = System.currentTimeMillis();
	
	mailman = new DeltaPost();
	mailman.add("DELTAVERSION", deltaversion);
	mailman.add("SYSUSER",  System.getProperty("user.name"));
	mailman.add("HOME",     System.getProperty("user.home"));
	mailman.add("BJUSER",   "bluej-reported-username");
	mailman.add("OSNAME",   System.getProperty("os.name"));
	mailman.add("OSVER",    System.getProperty("os.version"));
	mailman.add("OSARCH",   System.getProperty("os.arch"));
	mailman.add("CSTART",   startTime);
	mailman.add("CEND",     endTime);
	mailman.add("CDUR",     endTime - startTime);
	mailman.add("IPADDR",   ipAddress);
	mailman.add("HOSTNAME",   hostname);
	mailman.add("LOCATION", location);

	mailman.add("CLIENTINDEX", clientIndex);

	mailman.add(farmer, perFileHash);

	//UPDATE THE CLIENT INDEX
	/**
	 * Ug. State. Every time a compilation takes place, I want
	 * the local client to note that fact, and increment
	 * it's own compilation counter. I don't know where
	 * to put this that's more hidden/automatic or 
	 * less fragile.
	 */
	clientIndex = clientIndex + 1;
	
	mailman.deliver();
    } 

} 


