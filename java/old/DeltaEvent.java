import java.util.Date;

import bluej.extensions.*;
import bluej.extensions.event.*; 

/**
 * A DeltaEvent is a wrapper around CompileEvent, with a time-stamp added.
 * 
 * @author Matt Jadud
 */
class DeltaEvent {
  private CompileEvent event;
  private long time;
  
  public CompileEvent getEvent() {
    return event;
  }
  
  public long getTime() {
    return time;
  }
  
  public DeltaEvent(CompileEvent e) {
    event = e;
    time = System.currentTimeMillis();
  }
}

