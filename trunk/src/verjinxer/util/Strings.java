/*
 * Strings.java
 *
 * Created on February 4, 2007, 2:37 PM
 *
 */

package rahmann.util;

/**
 *
 * @author Sven Rahmann
 */
public class Strings {
   
   /** returns a string created by joining all args 
    *  and placing the string joinseq between them 
    * @param joinseq string to be placed between any two args
    * @param args    array of strings to be joined
    */
   public static final String join(final String joinseq, final String[] args)
   {
      if(args==null || args.length==0) return("");
      StringBuilder sb = new StringBuilder(args[0]);
      for(int i=1; i<args.length; i++)  sb.append(joinseq).append(args[i]);
      return sb.toString();
   }

   /** returns a string created by joining all args with spaces " "
    * @param args   array of strings to be joined
    */
   public static final String join(final String[] args) {
      return join(" ",args);
   }

  public static final String join(final String joinseq, final int[] a, int offset, int length) {
    if(length==0) return("");
    StringBuilder sb = new StringBuilder(Integer.toString(a[offset]));
    for(int i=1; i<length; i++)  sb.append(joinseq).append(a[offset+i]);
    return sb.toString();
  }
 
  public static final String join(final String joinseq, final byte[] a, int offset, int length) {
    if(length==0) return("");
    StringBuilder sb = new StringBuilder(Integer.toString(a[offset]));
    for(int i=1; i<length; i++)  sb.append(joinseq).append(a[offset+i]);
    return sb.toString();
  }
 
}
