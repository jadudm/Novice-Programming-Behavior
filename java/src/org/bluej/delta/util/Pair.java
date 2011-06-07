package org.bluej.delta.util;

/**
 * A pair represents a key and value pair. The keys have to be Strings and the
 * values can only be of certain types. A pair must ALWAYS have a value - null
 * is not accepted and will throw an IllegalArgumentException.
 * 
 * @author Poul Henriksen
 */
public class Pair
{
    private String key;
    private Object value;
    private boolean containsUserName;

    /**
     * 
     * @throws IllegalArgumentException if key or value is null
     */
    public Pair(String key, String value)
    {
        checkForNullParameters(key, value);
        this.key = key;
        this.value = value;
        this.containsUserName = true;
    }

    /**
     * @throws IllegalArgumentException
     *             if key or value is null
     */
    public Pair(String key, byte[] value)
    {
        checkForNullParameters(key, value);
        this.key = key;
        this.value = value;
    }

    /**
     * @throws IllegalArgumentException
     *             if key or value is null
     */
    public Pair(String key, double value)
    {
        checkForNullParameters(key, "");
        this.key = key;
        this.value = new Double(value);
    }

    /**
     * @throws IllegalArgumentException
     *             if key or value is null
     */
    public Pair(String key, Double value)
    {
        checkForNullParameters(key, value);
        this.key = key;
        this.value = value;
    }

    /**
     * @throws IllegalArgumentException
     *             if key or value is null
     */
    public Pair(String key, int value)
    {
        checkForNullParameters(key, "");
        this.key = key;
        this.value = new Integer(value);
    }

    /**
     * @throws IllegalArgumentException
     *             if key or value is null
     */
    public Pair(String key, Integer value)
    {
        checkForNullParameters(key, value);
        this.key = key;
        this.value = value;
    }

    /**
     * @throws IllegalArgumentException
     *             if key or value is null
     */
    public Pair(String key, boolean b)
    {
        checkForNullParameters(key, "");
        this.key = key;
        this.value = new Boolean(b);
    }

    /**
     * @throws IllegalArgumentException
     *             if key or value is null
     */
    public Pair(String key, Boolean value)
    {
        checkForNullParameters(key, value);
        this.key = key;
        this.value = value;
    }

    /**
     * Will never return null
     */
    public String getKey()
    {
        return key;
    }

    /**
     * Will never return null
     */
    public Object getValue()
    {
        return value;
    }

    public String toString()
    {
        return key + " : " + value;
    }

    /**
     * @throws IllegalArgumentException
     *             if key or value is null
     */
    private void checkForNullParameters(Object key, Object value)
    {
        if (key == null) {
            throw new IllegalArgumentException("Key must not be null.");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value must not be null. (key: " + key + ")");
        }
    }

    public boolean containsUserName()
    {
        return containsUserName;
    }
}
