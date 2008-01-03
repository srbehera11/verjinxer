/*
 * FastaFormatException.java
 *
 * Created on December 15, 2006, 8:23 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package rahmann.sequenceanalysis;

/**
 *
 * @author Sven Rahmann
 */
public class FastaFormatException extends java.lang.Exception
{
   
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
