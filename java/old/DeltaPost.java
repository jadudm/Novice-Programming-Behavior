import java.net.*; 
import java.io.*; 
import java.util.*;
import bluej.extensions.*;
import bluej.extensions.event.*; 
import java.net.URLEncoder;

/**
 * This sends information with http-postings. It can send basic key/value pairs
 * or it can ssend a DeltaGatherer.
 * 
 * @author Matt Jadud
 */
public class DeltaPost {
  
  private String          msg = "";
  private BlueJ           context;
  private ArrayList       pointers;
  private boolean         first = true;
  private Map             localHash;
  
  // The old pointer.
  //"http://myrtle.kent.ac.uk/~mcj4/031009-url-pointer";
  
  private void debug(String msg) {
    if (false) {
      System.err.println(msg);
    }
  }
  
  
  public DeltaPost () {
    pointers  = new ArrayList();
    pointers.add(new String("http://www.cs.kent.ac.uk/~mcj4/kent-pointer-20041003"));
    //pointers.add(new String("http://www.cs-ed.org/delta/iu-pointer-20041003"));
    //pointers.add(new String("http://127.0.0.1/~mcj4/test-pointer"));
  }
  
  private URLConnection initConnection(String pointer) {
    
    URLConnection con;
    URL url;
    
    try { 
      url = new URL(getPostURL(pointer)); 
      
      con = url.openConnection(); 
      
      //Init variables 
      // 20031210 MCJ WARNING
      // Currently, I don't know if these go before or after
      // creating the connection.
      con.setDoInput(true); 
      con.setDoOutput(true); 
      con.setUseCaches(false); 
      
    } catch (Throwable except) { return null; }
    
    return con;
  }
  
  private String getPostURL(String pointer) {
    String input = "";
    try {
      URL pointerURL = new URL(pointer);
      URLConnection con = pointerURL.openConnection();
      
      BufferedReader in = 
        new BufferedReader(new InputStreamReader(con.getInputStream()));
      input = in.readLine();
      in.close();
    } catch (Exception e) { }
    
    debug("PostURL: " + input);
    return input;
  }
  
  /**
   * Add content to be shipped off.
   * 20040105 MCJ Now, everything is Base64 encoded... less
   * worry when shipping it over the wire. I hope.
   */
  public void add(String tag, String val) {
    try {
      String encVal = URLEncoder.encode(val, "UTF-8");

      if (first) {
        msg = msg + tag + "=" +  encode(encVal.getBytes());
        //URLEncoder.encode(val, "UTF-8"); 
      } else {
        msg = msg + "&" + tag + "=" + encode(encVal.getBytes());
        //URLEncoder.encode(val, "UTF-8"); 
      }
    } catch (Exception e) { }
    
    first = false;
  }
  
  public void add(String tag, long val) {
    try { 
      if (first) {
        msg = msg + tag + "=" + val;
        //URLEncoder.encode("" + val, "UTF-8");     
      } else {
        msg = msg + "&" + tag + "=" + val;
        //URLEncoder.encode("" + val, "UTF-8");
      }
    } catch (Exception e) { }
    
    first = false;
  }
  
  private String fileToString(File fobj) {
    String gatherer = "";
    int ch; 
    
    try {
      FileReader fr = new FileReader(fobj);
      // -1??? For the love of all things holy...
      while ((ch = fr.read()) != -1) {
        gatherer = gatherer + Character.toString((char) ch); }
    } catch (Exception e) { }
    
    // No URLEncoding, because add() will take care of it later.
    return gatherer;
  }
  
  
  
  private void possibleAddFileUpdate(String filename, Map perFileHash) {
    
    Iterator iter = perFileHash.keySet().iterator();
    while(iter.hasNext()) {
      Object key = iter.next();
      Integer tmpInt = (Integer) perFileHash.get(key);
      
      if(((String) key).equals(filename)) {
        add("THISFILE_COUNT", tmpInt.intValue());
        add("THISFILE_FILENAME", filename);
      } }
  }
  
  private void addTransient(String prefix, CompileEvent ce, long time, Map pfh) {
    try { 
      java.io.File[] files = ce.getFiles();
      add("EVENT_TYPE", prefix);
      for(int i = 0; i < files.length; i++) {
        possibleAddFileUpdate(files[i].getName(), pfh);
        add(prefix + "_FILENAME", files[i].getCanonicalPath());
        add(prefix + "_TIME", time);
        add(prefix + "_MSG", ce.getErrorMessage());
        add(prefix + "_LINE", ce.getErrorLineNumber());
        add(prefix + "_FILE", fileToString(files[i]));
      } } catch (Exception e) { } } 
  
  private void addFinal(String prefix, CompileEvent ce, long time, Map pfh) {
    try {
      java.io.File[] files = ce.getFiles();
      
      if(prefix != "FAILURE") {
        add("EVENT_TYPE", prefix);
        for(int i = 0; i < files.length; i++) {
          possibleAddFileUpdate(files[i].getName(), pfh);
          add(prefix + "_FILENAME", files[i].getCanonicalPath());
          add(prefix + "_TIME", time);
          add(prefix + "_FILE", fileToString(files[i]));
        } }
    } catch (Exception e) { } } 
  
  private void addStart(String prefix, CompileEvent ce, long time, Map pfh) {
    try { 
      java.io.File[] files = ce.getFiles();
      
      for(int i = 0; i < files.length; i++) {
        possibleAddFileUpdate(files[i].getName(), pfh);
        add(prefix + "_FILENAME", files[i].getCanonicalPath());
        add(prefix + "_TIME", time);
      } } catch (Exception e) { } } 
  
  public void add(DeltaGatherer farmer, Map pfh) {
    DeltaEvent de;
    CompileEvent ce;
    long time;
    
    //The farmer wraps a list of CompileEvents;
    // hence the fairly transparent use of more()
    // and next(); I'm just marching down a linked list,
    // really.
    while (farmer.hasNext()) {
      //Grab the event.
      de = farmer.next();
      ce = de.getEvent();
      time = de.getTime();
      
      switch ( ce.getEvent() ) {
        
        case CompileEvent.COMPILE_START_EVENT : 
          addStart("START", ce, time, pfh);         
          break;
          
        case CompileEvent.COMPILE_WARNING_EVENT :
          addTransient("WARN", ce, time, pfh);      
          break;
          
        case CompileEvent.COMPILE_ERROR_EVENT : 
          addTransient("ERROR", ce, time, pfh);     
          break;
          
        case CompileEvent.COMPILE_DONE_EVENT : 
          addFinal("SUCCESS", ce, time,pfh);       
          break;
          
        case CompileEvent.COMPILE_FAILED_EVENT : 
          addFinal("FAILURE", ce, time,pfh);       
          break;
      } //end switch
      
    } //end while over farmer elements
  }
  
  
  
  /**
   * deliver() should only be called inside an asynch thread.
   * This way anything that bungs up with the HTTP post can
   * die in it's own time without affecting BlueJ. As least,
   * this is the running theory.
   */ 
  
  public void deliver() {
    
    final ListIterator iter = pointers.listIterator();
    
    /**
     * For each pointer URL, I want
     * to spawn a thread to do a post.
     */
    while(iter.hasNext()) {
      new Thread () {
        public void run() {
          postThread((String) iter.next());
        }
      }.start();
    }
    
  } 
  
  
  public void postThread(final String pointer) {
    
    Thread t = new Thread() {
      public void run() {
        try {
          URLConnection con = null;
          con = initConnection(pointer);
          if (con != null) {
            msg = msg + "\n";
            
            //System.err.println("Talking to: " + pointer);
            
            //System.err.println("Message:\n" + msg);
            
            con.setRequestProperty("CONTENT_LENGTH", "" + msg.length()); 
            OutputStream os = con.getOutputStream(); 
            
            OutputStreamWriter osw = 
              new OutputStreamWriter(os); 
            
            osw.write(msg); 
            osw.flush(); 
            osw.close(); 
            
            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String thisLine;
            while ((thisLine = br.readLine()) != null) { 
              debug("Response: " + thisLine);
            } 
          } else {
            debug("Cannot post to " + pointer);
          }
          
        } catch (Exception e) {  }
      }
    };
    
    try {
      t.start();
      t.join(20000);
      if (t.isAlive()) { t.destroy(); }
    } catch (Exception e) { }
  }
  
  
  
  public static String encode(byte[] raw){
    StringBuffer encoded = new StringBuffer();
    for(int i=0; i<raw.length; i +=3){
      encoded.append(encodeBlock(raw, i));
    }
    return encoded.toString();
  }
  
  
  protected static char[] encodeBlock(byte[] raw, int offset) {
    int block = 0;
    int slack = raw.length - offset - 1;
    int end   = (slack >= 2) ? 2 : slack;
    for( int i=0; i<=end; i++){
      byte b = raw[offset + i];
      int neuter = (b<0) ? b+256 : b;
      block += neuter << (8*(2-i));
    }
    char[] base64 = new char[4];
    for( int i=0; i<4; i++){
      int sixbit = (block >>> (6*(3-i))) & 0x3f;
      base64[i] = getChar(sixbit);
    }
    if( slack < 1 )
      base64[2] = '=';
    if( slack < 2)
      base64[3] = '=';
    
    return base64;
  }
  
  protected static char getChar(int sixBit) {
    if( sixBit >= 0  && sixBit <= 25)
      return (char)('A' + sixBit);
    if( sixBit >= 26 && sixBit <= 51)
      return (char)('a' + (sixBit-26) );
    if( sixBit >= 52 && sixBit <= 61)
      return (char)('0' + (sixBit-52) );
    if( sixBit == 62 ) return '+';
    if( sixBit == 63 ) return '/';
    return '?';
  }
  
  public static byte[] decode(String base64) {
    int pad = 0;
    for( int i = base64.length() -1; base64.charAt(i) == '='; i--)
      pad++;
    int length = base64.length() * 6/8 - pad;
    byte[] raw = new byte[length];
    int rawIndex = 0;
    for(int i=0; i<base64.length(); i +=4){
      int block = (getValue(base64.charAt(i)) << 18)
        +(getValue(base64.charAt(i+1)) << 12)
        +(getValue(base64.charAt(i+2)) << 6)
        +(getValue(base64.charAt(i+3)));
      for( int j=0; j<3 && rawIndex + j < raw.length; j++)
        raw[rawIndex + j] = (byte)((block >> (8*(2-j))) & 0xff);
      rawIndex += 3;
    }
    return raw;
  }
  
  protected static int getValue(char c){
    if( c >= 'A' && c <= 'Z' )
      return c-'A';
    if( c >= 'a' && c <= 'z' )
      return c-'a' + 26;
    if( c >= '0' && c <= '9')
      return c-'0' + 52;
    if( c == '+' ) return 62;
    if( c == '/' ) return 63;
    if( c == '=' ) return 0;
    return -1;
  }
  
  
  
}
