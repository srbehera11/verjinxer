/*
 * FastaFormatException.java
 *
 * Created on December 15, 2006, 8:23 AM
 *
 */

package verjinxer.sequenceanalysis;

/**
 *
 * @author Sven Rahmann
 */
public class FastaFormatException extends java.lang.Exception {
  private static final long serialVersionUID = -7927288270360792495L;

  /**
    * Creates a new instance of <code>FastaFormatException</code> without detail message.
    */
   public FastaFormatException()
   {
      super();
   }
   
   
   /**
    * Constructs an instance of <code>FastaFormatException</code> with the specified detail message.
    * @param msg the detail message.
    */
   public FastaFormatException(String msg)
   {
      super(msg);
   }
}
