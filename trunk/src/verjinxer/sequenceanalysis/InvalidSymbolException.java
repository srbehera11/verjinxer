/*
 * InvalidSymbolException.java
 *
 * Created on December 12, 2006, 5:39 AM
 *
 */

package verjinxer.sequenceanalysis;

/**
 *
 * @author Sven Rahmann
 */
public class InvalidSymbolException extends java.lang.Exception {
  private static final long serialVersionUID = 3502727305855185638L;

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
