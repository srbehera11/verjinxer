/*
 * Created on 13. April 2007, 17:19
 *
 */


package verjinxer.util;

/**
 * This utility class contains static utility functions for arrays.
 * @author Sven Rahmann
 */
public class ArrayUtils {

   private ArrayUtils() {
   }
   
   /** Allocate and return an int[] of the requested size N, if possible.
    * Otherwise, successively attempt to allocate an int[] of N/2, N/3, N/4, ...
    * until we succeed, or give up (in which case null is returned).
    * @param intsRequired  number of desired elements in the array
    * @return the allocated array, or null if not possible.
    */
   public static int[] getIntSlice(final int intsRequired) {
      int attempt = intsRequired;
      int oldattempt = attempt+1;
      for (int t=1; attempt<oldattempt && t<=100; t++) {
         oldattempt = attempt;
         System.gc();
         try {
            int[] slice = new int[attempt];  
            return slice; // in iteration t, it has 1/t times requested size
         } catch (OutOfMemoryError e) {
           attempt = (int) Math.ceil((double)attempt * t/(t+1.0) * 1.01);
         }
      }
      return null; // after 100 tries, give up!
   }
   
  
   /**
    * @return size of largest allocatable byte array
    * @deprecated
    */
  public static final int largestAllocatable() {
    return largestAllocatable(0);
  }
  
  
   /**
    * @param lmax  desired number of bytes to be allocated
    * @return lmax, or size of largest allocatable byte array
    *  if an array of size lmax cannot be allocated.
    * @deprecated
    */
  public static final int largestAllocatable(long lmax) {
    int min = 0;           // lower bound
    int max = 0x7fffffff;  // upper bound
    if (lmax>0) max = (int)lmax;
    if (canWeAllocate(max)) return (max/8)*8;
    while (max-min > 1024*16) {
      int half = (max-min)/2+min;
      if (canWeAllocate(half)) {
        // yes, go up with the lower bound
        min = half+1;
      } else {
        // no, go down with upper bound
        max = half-1;
      }
    }
    System.gc();
    return (min/8)*8;
  }
  
  /**
   * try to allocate a certain number of bytes.
   * If this is possible, return true; otherwise false.
   * The memory is immediately freed again.
   * @param size  number of bytes to attempt to allocate
   * @return true if 'size' bytes can be allocated, false otherwise.
   */
  @SuppressWarnings("unused")
  public static final boolean canWeAllocate(int size) {
    System.gc();
    try {
      byte[] test = new byte[size];
      test = null;
      return true; 
    } catch (OutOfMemoryError e) {}
    return false;
  }
  
  
  /** reverses an array or a prefix of an array in place
   *@param a  the array
   *@param len the number of elements to reverse (a[0..len-1] is reversed);
   * if len&lt;0 or len&gt;a.length, then the whole array is reversed.
   * @return the same array a, (partially) reversed in-place.
   */
  public static int[] reverseArray(final int[] a, int len) {
    if(len<0) len=a.length;
    int tmp;
    final int half=len/2;
    for(int i=0; i<half; i++) {
      tmp=a[i];
      a[i]=a[len-1-i];
      a[len-1-i]=tmp;
    }
    return a;
  }
  
  /** reverses an array or a prefix of an array in place
   *@param a  the array
   *@param len the number of elements to reverse (a[0..len-1] is reversed);
   * if len&lt;0 or len&gt;a.length, then the whole array is reversed.
   * @return the same array a, (partially) reversed in-place.
   */
  public static byte[] reverseArray(final byte[] a, int len) {
    if(len<0) len=a.length;
    byte tmp;
    final int half=len/2;
    for(int i=0; i<half; i++) {
      tmp=a[i];
      a[i]=a[len-1-i];
      a[len-1-i]=tmp;
    }
    return a;
  }
  
  /** reverse-complements part of an array
   *@param a  the array
   *@param start  where to start reverse-complementing the array,
   *@param stop where to stop reverse-complementing the array,
   *  in fact a[start..stop-1] is reverse-complemented.
   * If stop&lt;0 or stop&gt;a.length, then the we stop at the end of 'a'.
   *@param compl  in the reversed array, each element x is replaced by compl-x
   *@param b  if not null, copy the reverse complement of a to b[0..stop-start-1]. 
   *          Otherwise, reverse-complement in place.
   *@return the array a or b containing the reverse complement
   */
  public static byte[] revcompArray(final byte[] a, final int start, int stop, final byte compl, final byte[] b) {
    if(stop<0 || stop>a.length) stop=a.length;
    byte tmp;
    final int len=stop-start;
    if (b!=null) {
      assert(b.length>=len);
      for(int i=0; i<len; i++) {
        b[i]=a[stop-1-i];
        if (b[i]>=0 && b[i]<compl) b[i] = (byte)(compl-1-b[i]);
      }
      return b;
    } else { // in place:
      final int half=(start+stop)/2;
      for(int i=start; i<half; i++) {
        tmp=a[i];
        a[i]=a[stop+start-1-i];
        a[stop+start-1-i]=tmp;
      }
      for(int i=start; i<stop; i++) {
        if (a[i]>=0 && a[i]<compl) a[i] = (byte)(compl-1-a[i]);
      }
      return a;
    }
  }
  
  /** compute the maximum nonnegative element of an integer array.
   * @param array  the array
   * @return the maximum element of the given array.
   *   If the array is empty or contains only negative elements, 0 is returned.
   */
  public static int maximumElement(int[] array) {
    int maximum = 0;
    for (int a: array) if (a>maximum) maximum=a;
    return maximum;
  }

  
}
