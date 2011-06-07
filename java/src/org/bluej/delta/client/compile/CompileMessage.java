package org.bluej.delta.client.compile;


/**
 * A message from the compiler. Either an error or a warning.
 * @author Poul Henriksen
 *
 */
public class CompileMessage
{
    private String type;
    private String message;
    private int lineNumber;

    public static final String WARNING = "WARNING";
    public static final String ERROR = "ERROR";
    
    public CompileMessage(String type, String message, int lineNumber) {
        this.type = type;
        this.message = message;
        this.lineNumber = lineNumber;
    }

    public int getLineNumber()
    {
        return lineNumber;
    }

    public String getMessage()
    {
        return message;
    }

    public String getType()
    {
        return type;
    }
}
