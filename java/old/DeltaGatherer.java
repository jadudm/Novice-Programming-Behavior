import java.util.Date;
import java.util.*;
import java.io.File;

import bluej.extensions.*;
import bluej.extensions.event.*; 

/**
 * Represent information about one compilation.This can contain several DeltaEvents.
 * 
 * @author Matt Jadud
 */
public class DeltaGatherer {
  
  private long startTime, endTime;
  private int numFiles = 0;
  
  private List list;
  private ListIterator listIter;
  
  // Constructor
  // Allocates a new, synchronized ArrayList.
  public DeltaGatherer(CompileEvent ce) {
    list = Collections.synchronizedList(new ArrayList());
    startTime = System.currentTimeMillis();
    numFiles = ce.getFiles().length;
  }
  
  private boolean is_zero(int n) { if (0 == n) { return true; } return false;  }
  
  /**
   * If I get any compilation warnings or errors
   * along the way, I want to hang on to them, and 
   * ship them off later.
   */
  public void addTransient(CompileEvent ce) {
    list.add(new DeltaEvent(ce)); }
  
  /** 
   * The end() method is called when a successful or unsuccessful
   * compile finishes on a file. This gets called once per file;
   * therefore, when BlueJ has finished compiling the last file,
   * I want to report TRUE, so the events can be processed and 
   * shipped off to the server.
   */
  public boolean end(CompileEvent ce) {
    list.add(new DeltaEvent(ce));
    
    //One file down...
    numFiles--;
    
    //Any files to go? If we're done, return TRUE.
    if(is_zero(numFiles)) {
      //Create the list iterator before we go.
      listIter = list.listIterator();
      return true;  
    }
    
    return false;
  }
  
  public boolean hasNext() {
    return listIter.hasNext();
  }
  
  public DeltaEvent next() {
    return (DeltaEvent)listIter.next();
  }
  
}
