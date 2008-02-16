/*
 * Strings.java
 *
 * Created on February 4, 2007, 2:37 PM
 *
 */

package verjinxer.util;

/**
 * utility class with String utilities
 * @author Sven Rahmann
 */
public class StringUtils {

   private StringUtils() {
   }
   
   /** returns a string created by joining all args 
    *  and placing the string joinseq between them 
    * @param joinseq string to be placed between any two args
    * @param args    array of strings to be joined
    * @return the joined string
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
    * @return the joined string
    */
   public static final String join(final String[] args) {
      return join(" ",args);
   }

   /**
    * create a string out of an integer array, joining the elements by a given string
    * @param joinseq  the string to be placed betwee two array elements
    * @param a        the array
    * @param offset   position in the array where to start building the string
    * @param length   number of elements in the array to include in the resulting string
    * @return the joined string
    */
   public static final String join(final String joinseq, final int[] a, int offset, int length) {
    if(length==0) return("");
    StringBuilder sb = new StringBuilder(Integer.toString(a[offset]));
    for(int i=1; i<length; i++)  sb.append(joinseq).append(a[offset+i]);
    return sb.toString();
  }
 
   /**
    * create a string out of an byte array, joining the elements by a given string
    * @param joinseq  the string to be placed betwee two array elements
    * @param a        the array
    * @param offset   position in the array where to start building the string
    * @param length   number of elements in the array to include in the resulting string
    * @return the joined string
    */
  public static final String join(final String joinseq, final byte[] a, int offset, int length) {
    if(length==0) return("");
    StringBuilder sb = new StringBuilder(Integer.toString(a[offset]));
    for(int i=1; i<length; i++)  sb.append(joinseq).append(a[offset+i]);
    return sb.toString();
  }

 
}
