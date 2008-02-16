/*
 * TicToc.java
 *
 * Created on December 11, 2006, 11:55 PM
 *
 */

package verjinxer.util;

/**
 *
 * @author Sven Rahmann
 */
public final class TicToc
{
   private long t;
   
   /** Creates a new instance of a TicToc timer, remembering system time */
   public TicToc()
   {
      t = System.nanoTime();
   }
   
   /** Remembers current system time */
   public void tic()
   {
      t = System.nanoTime();
   }
   
   /** 
    * @return  seconds since last tic() as double
    */
   public double tocs() {
    return ((System.nanoTime()-t)/1000000000.0);      
  }

   /** 
    * @return nanoseconds since last tic()
    */
   public long toc()
   {
      return System.nanoTime()-t;
   }
   
   /**
    * @return  milliseconds since last tic()
    */
   public long tocMilliSeconds()
   {
      return java.lang.Math.round((System.nanoTime()-t)/1000000.0);      
   }
   
   /**
    * @return  integer seconds since last tic()
    */
   public long tocSeconds()
   {
      return java.lang.Math.round((System.nanoTime()-t)/1000000000.0);
   }

}
