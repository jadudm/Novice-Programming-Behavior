package org.bluej.delta.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.Random;

import org.bluej.delta.client.compile.CompilationLogger;
import org.bluej.delta.client.invocation.InvocationLogger;
import org.bluej.delta.client.shipper.Packet;
import org.bluej.delta.client.shipper.Shipper;
import org.bluej.delta.client.shipper.ShippingQueue;
import org.bluej.delta.util.Debug;
import org.bluej.delta.util.Pair;

import bluej.extensions.BProject;
import bluej.extensions.BlueJ;
import bluej.extensions.Extension;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.extensions.event.PackageEvent;
import bluej.extensions.event.PackageListener;

/**
 * An extension for logging BlueJ events.
 * 
 * <p>
 * 
 * If new loggers are created, you should register them in the method:
 * registerListeners(BlueJ bluej).
 * 
 * @author Poul Henriksen
 * 
 */
public class DeltaMain extends Extension implements PackageListener
{
    private static final String PROPERTIES_LOCATION = "extensions/delta.properties";
    
    /**
     * File where the project id is stored.
     */
    private static final String PROJECT_PROP_FILE = "projectid.bjlog";
    public static String VERSION = "20081219";
    private String hostname;
    private String ipAddress;

    private Properties config;
    private ShippingQueue shippingQueue;
    private BlueJ bluej;
    private int sessionId = (int) (System.currentTimeMillis() / 1000);
    private File configFile;
    private Hashtable projectIdMap = new Hashtable();
    private Hashtable pkg2PrjMap = new Hashtable();

    private String userName ;

    private String userHome;

    /**
     * Register the loggers to listen for BlueJ events.
     * 
     * @param bluej
     */
    private void registerListeners(BlueJ bluej)
    {
        bluej.addCompileListener(new CompilationLogger(this));
        bluej.addInvocationListener(new InvocationLogger(this));
        Debug.println("Listeners registered");
    }

    /**
     * Ship the data. Adds some general info to the packet before sending it.
     * Loggers should call this method when a set of data has been collected and
     * is ready to be send to the server.
     * 
     */
    public void ship(EventData eventData)
    {
        Debug.println("Shipping...");
        Packet packet = new Packet(userName);

        packet.setName(config.getProperty("location") + "_" + eventData.getName());
        
        packet.add(new Pair("DELTA_VERSION", VERSION));
        packet.add(new Pair("BJ_EXT_VERSION", VERSION_MAJOR + "." + VERSION_MINOR));
        
     
        packet.add(new Pair("SYSUSER",userName));
        packet.add(new Pair("HOME", userHome));
        packet.add(new Pair("OSNAME", System.getProperty("os.name")));
        packet.add(new Pair("OSVER", System.getProperty("os.version")));
        packet.add(new Pair("OSARCH", System.getProperty("os.arch")));
        packet.add(new Pair("IPADDR", ipAddress));
        packet.add(new Pair("HOSTNAME", hostname));        

        packet.add(new Pair("LOCATION_ID", config.getProperty("location")));        
        packet.add(new Pair("PROJECT_ID", getProjectId(getProjectDir(eventData.getPackageDir()))));
        packet.add(new Pair("SESSION_ID", sessionId));
        
        String prjPath = getProjectDir(eventData.getPackageDir()).getPath();
        packet.add(new Pair("PROJECT_PATH", prjPath));  
        String pkgPath = eventData.getPackageDir().getPath();
        packet.add(new Pair("PACKAGE_PATH", pkgPath));    

        packet.add(new Pair("DELTA_NAME", eventData.getName()));
        packet.add(new Pair("DELTA_SEQ_NUMBER", eventData.getSeqNumber()));
        packet.add(new Pair("DELTA_START_TIME", eventData.getStartTime()));
        packet.add(new Pair("DELTA_END_TIME", eventData.getEndTime()));
        for (Iterator iter = eventData.iterator(); iter.hasNext();) {
            Pair pair = (Pair) iter.next();
            
            packet.add(pair);
        }

        shippingQueue.add(packet);
        Debug.println("Shipped data");
    }


    private File getProjectDir(File packageDir)
    {
       return (File) pkg2PrjMap .get(packageDir);
    }

    /**
     * Tries to load the ID for this project, or generate a new ID if none can be found.
     * 
     * @return A semi-unique string identifying this project.
     */
    private String getProjectId(File projectDir)
    {
        String projectId  = null;
        try {
            Properties projectProperties = new Properties();
            File projectIdFile = new File(projectDir, PROJECT_PROP_FILE);
            projectId = (String) projectIdMap.get(projectDir);
            if(projectId == null && projectIdFile.canRead()) {    
                loadProperties(projectProperties, projectIdFile);
                projectId  = projectProperties.getProperty("PROJECT_ID");
            }
            if (projectId == null) {       
                String time = Long.toString(System.currentTimeMillis());
                String random = Integer.toString(new Random().nextInt(1000));
                projectId = time + random;
                projectProperties.put("PROJECT_ID", projectId);
                try {
                    storeProperties(projectProperties, projectIdFile);
                }
                catch (IOException e) {
                    Debug.reportException(e);
                    projectId = "NOT_STORED" + projectId;
                }
            }
            projectIdMap.put(projectDir, projectId);
            return projectId;
        }
        catch (Exception e1) {
            Debug.reportException(e1);
        }
        return "ERROR";       
       
    }

    /**
     * Loads the properties for this project. If the extension was loaded from a
     * project's extensions dir, it will look in that dir for the config file.
     * If it was not loaded from a project, it will first look in
     * <USER_HOME>/.bluej/extensions and failing that it will look in
     * <BLUEJ_LIB>/extensions
     * 
     */
    private void loadProperties(Properties properties, File file)
    {
       
        try {
            FileInputStream fis = new FileInputStream(file);
            properties.load(fis);
        }
        catch (FileNotFoundException e1) {
            Debug.reportException(e1);
        }
        catch (IOException e) {
            Debug.reportException(e);
        }
    }
    
    private void storeProperties(Properties properties, File file )
        throws IOException
    {
        FileOutputStream fos = new FileOutputStream(file);
        properties.store(fos, "Properties for BlueJ logging extension.");
    }

    private File getConfigFile(BProject[] openProjects)
    {
        File configFile = null;
        if (openProjects.length > 0) {
            // The extension was loaded from the project dir, so that is where
            // we search for the properties. We search through all the projects
            // and use the first properties file found. If several projects are
            // open at this point, we just use the first one we find. That means
            // that you can only log to ONE location even if you open several
            // project with different configurations! That should be fine...
            for (int i = 0; i < openProjects.length; i++) {
                BProject project = openProjects[i];
                try {
                    File prjDir = project.getDir();
                    File candidateFile = new File(prjDir, PROPERTIES_LOCATION);
                    if (candidateFile.exists()) {
                        configFile = candidateFile;
                    }
                }
                catch (ProjectNotOpenException e) {
                    Debug.reportException(e);
                }
            }
        }
        else {
            // the extension was loaded at BlueJ launch, before we have any
            // projects open, so we search for the properties in the BlueJ and
            // user dir.
            File userDir = bluej.getUserConfigDir();
            File userCandidate = new File(userDir, PROPERTIES_LOCATION);
            if (userCandidate.exists()) {
                configFile = userCandidate;
            }
            else {
                File bluejLibDir = bluej.getSystemLibDir();
                File bluejCandidate = new File(bluejLibDir, PROPERTIES_LOCATION);
                if (bluejCandidate.exists()) {
                    configFile = bluejCandidate;
                }
            }
        }
        return configFile;
    }

    private boolean createShippingQueue()
    {
        Shipper shipper = null;
        String className = config.getProperty("server.type");
        String serverAddress = config.getProperty("server.address");
        try {
            shipper = (Shipper) Class.forName(className).newInstance();            
            shipper.initialise(serverAddress);
            shippingQueue = new ShippingQueue(shipper);
            return true;
        }
        catch (InstantiationException e) {
            Debug.reportException(e);
        }
        catch (IllegalAccessException e) {
            Debug.reportException(e);
        }
        catch (ClassNotFoundException e) {
            Debug.reportException(e);
        }        
        return false;
    }

    // ================================================================
    //
    // B L U E J     E X T E N S I O N      S T U F F
    //
    // ================================================================

    /**
     * When this method is called, the extension may start its work.
     */
    public void startup(BlueJ bluej)
    {
        Debug.println("Starting Delta extension");
        this.bluej = bluej;
        bluej.addPackageListener(this);
        config = new Properties();
        BProject[] openProjects = bluej.getOpenProjects();
        configFile = getConfigFile(openProjects);
        loadProperties(config, configFile);
        Debug.setEnabled(config.getProperty("debug", "false").equalsIgnoreCase("true"));
 
        userHome = System.getProperty("user.home");
        userName = System.getProperty("user.name");
        
        // Grab the client addresses for reporting purposes.
        try {
            InetAddress addr = InetAddress.getLocalHost();
            String ipAddr = addr.getHostAddress();
            hostname = addr.getHostName();
            ipAddress = ipAddr;
        }
        catch (UnknownHostException e) {
            Debug.reportException(e);
        }

        if(createShippingQueue()) {
            registerListeners(bluej);
        }
    }

    public boolean isCompatible()
    {
        return true;
    }

    public String getVersion()
    {
        return (VERSION);
    }

    public String getName()
    {
        return ("EventData Gatherer");
    }

    public String getDescription()
    {
				Debug.println("A description was requested.");
        return ("Captures information about programming behavior." + "\nQuestions? Drop a note to jadudm@gmail.com");
    }

    public URL getURL()
    {
        try {
            return new URL("mailto:jadudm@gmail.com");
        }
        catch (Exception e) {
            Debug.reportException(e);
            return null;
        }
    }

    public void packageClosing(PackageEvent event)
    {
        //Do nothing. 
    }

    public void packageOpened(PackageEvent event)
    {
        try {
            pkg2PrjMap.put(event.getPackage().getDir(), event.getPackage().getProject().getDir());
        }
        catch (ProjectNotOpenException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (PackageNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    

}