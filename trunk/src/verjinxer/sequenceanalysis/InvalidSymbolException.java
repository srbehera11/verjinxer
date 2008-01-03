/*
 * InvalidSymbolException.java
 *
 * Created on December 12, 2006, 5:39 AM
 *
 */

package rahmann.sequenceanalysis;

/**
 *
 * @author Sven Rahmann
 */
public class InvalidSymbolException extends java.lang.Exception
{
   
   /**
    * Creates a new instance of <code>InvalidSymbolException</code> without detail message.
    */
   public InvalidSymbolException()
   {
      super();
   }
   
   
   /**
    * Constructs an instance of <code>InvalidSymbolException</code> with the specified detail message.
    * @param msg the detail message.
    */
   public InvalidSymbolException(String msg)
   {
      super(msg);
   }
}
