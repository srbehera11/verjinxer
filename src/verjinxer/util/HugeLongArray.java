/*
 */
package verjinxer.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

/**
 * This class provides huge (&lt; 2 Giga Elements) arrays of basic type long. Since Java limits
 * array indices to int's, we need to use several primitive arrays to emulate a huge array. This
 * causes some overhead and hence loss of efficiency, but appears to be the only method to create
 * arrays with more than 2^31 ~ 2 billion elements. This particular class provides huge long
 * arrays.
 * 
 * @author Sven Rahmann
 */
public class HugeLongArray {

   /** number of bytes per element */
   private final static int BYTESPERELEMENT = Long.SIZE/8;

   /** number of elements in the array */
   public final long length;
   
   final private static int BITS = 30;
   final private static int BINSIZE = 1 << BITS;
   final private static int BITMASK_HIGH = (-1) << BITS;
   final private static int BITMASK_LOW = ~BITMASK_HIGH;

   /** internal array of arrays */
   private final long[][] arrays;

   private final int bins;
   
   /**
    * creates a new HugeLongArray
    * 
    * @param size
    *           number of elements in the array, up to (2^63 -1)
    */
   public HugeLongArray(final long length) {
      this.length = length;
      bins = (int)((length-1) >> BITS) + 1; // last bin is potentially empty (array of length 0)
      arrays = new long[bins][];
      for (int i = 0; i < bins - 1; ++i) {
         arrays[i] = new long[BINSIZE];
      }
      if (bins > 0) {
         arrays[bins - 1] = new long[(int)(length & BITMASK_LOW)];
         assert arrays[bins-1].length > 0;
      }
   }

   /**
    * Creates a new HugeTypeArray as an exact copy of an existing one
    * 
    * @param other
    *           the existing array
    */
   public HugeLongArray(final HugeLongArray other) {
      length = other.length;
      bins = other.bins;
      arrays = new long[bins][];
      for (int i = 0; i < bins; i++) {
         System.arraycopy(other.arrays[i], 0, arrays[i], 0, other.arrays[i].length);
      }
   }

   /**
    * Returns the value of the specified element
    * 
    * @param i
    *           element number
    * @return value of element i
    */
   public final long get(final long i) {
      return arrays[(int)(i >> BITS)][(int)(i & BITMASK_LOW)];
   }

   /**
    * set element i to value
    * 
    * @param i
    *           number of element to set
    * @param value
    *           new value of element i
    */
   public final void set(final long i, final long value) {
      arrays[(int)(i >> BITS)][(int)(i & BITMASK_LOW)] = value;
   }

   /**
    * fill the whole array with one value
    * 
    * @param value
    *           value to fill the array with
    */
   public final void fill(final long value) {
      for (int i = 0; i < bins; i++) {
         Arrays.fill(arrays[i], value);
      }
   }

   @Override
   public boolean equals(Object other) {
      if (!(other instanceof HugeLongArray)) {
         return false;
      }
      HugeLongArray o = (HugeLongArray) other;
      if (length != o.length) {
         return false;
      }
      if (bins != o.bins) {
         return false;
      }
      for (int i = 0; i < o.bins; i++) {
         if (!Arrays.equals(arrays[i], o.arrays[i])) {
            return false;
         }
      }
      return true;
   }

   @Override
   public int hashCode() {
      int hash = 5;
      hash = 53 * hash + (int) (this.length ^ (this.length >>> 32));
      hash = 53 * hash + this.bins;
      for (long i = 0; i < length; i++) {
         hash = 53 * hash + (int) get(i);
      }
      return hash;
   }

   /**
    * finds a location (not necessarily the first or last) of the given key in (a subrange of ) the
    * array, IF THE ARRAY IS SORTED. This takes O(log |length|) time.
    * 
    * @param key
    *           the key to search for.
    * @param fromIndex
    * @param toIndex
    * @return index i such that get(i)==key, if it exists. Otherwise, -(i+1) for a hypothetical
    *         index i at which the key should be inserted to maintain a sorted array.
    */
   public long binarySearch(final long key, final long fromIndex, final long toIndex) {
      long low = fromIndex;
      long high = toIndex - 1;

      while (low <= high) {
         long mid = (low + high) >>> 1;
         final long midVal = get(mid);
         // int cmp = midVal.compareTo(key);

         if (midVal < key) {
            low = mid + 1;
         } else if (midVal > key) {
            high = mid - 1;
         } else {
            return mid; // key found
         }
      }
      return -(low + 1); // key not found.
   }

   /**
    * finds a location (not necessarily the first or last) of the given key in (a subrange of ) the
    * array, IF THE ARRAY IS SORTED. This takes O(log |length|) time.
    * 
    * @param key
    *           the key to search for.
    * @return index i such that get(i)==key, if it exists. Otherwise, -(i+1) for a hypothetical
    *         index i at which the key should be inserted to maintain a sorted array.
    */
   public long binarySearch(final long key) {
      return binarySearch(key, 0, length);
   }

   /** sorts the elements of this array by size */
   public void sort() {
      sort(0, length);
   }

   // ====================== SORT ====================================
   /**
    * Sorts the specified sub-array of longs into ascending order.
    * 
    * @param off
    * @param len
    */
   public void sort(final long off, final long len) {
      // Insertion sort on smallest arrays
      if (len < 7) {
         for (long i = off; i < len + off; i++) {
            for (long j = i; j > off && get(j - 1) > get(j); j--) {
               swap(j, j - 1);
            }
         }
         return;
      }

      // Choose a partition element, v
      long m = off + (len >> 1); // Small arrays, middle element
      if (len > 7) {
         long l = off;
         long n = off + len - 1;
         if (len > 40) { // Big arrays, pseudomedian of 9
            long s = len / 8;
            l = med3(l, l + s, l + 2 * s);
            m = med3(m - s, m, m + s);
            n = med3(n - 2 * s, n - s, n);
         }
         m = med3(l, m, n); // Mid-size, med of 3
      }
      final long v = get(m);

      // Establish Invariant: v* (<v)* (>v)* v*
      long a = off, b = a, c = off + len - 1, d = c;
      while (true) {
         while (b <= c && get(b) <= v) {
            if (get(b) == v) {
               swap(a++, b);
            }
            b++;
         }
         while (c >= b && get(c) >= v) {
            if (get(c) == v) {
               swap(c, d--);
            }
            c--;
         }
         if (b > c) {
            break;
         }
         swap(b++, c--);
      }
      // Swap partition elements back to middle
      long s, n = off + len;
      s = Math.min(a - off, b - a);
      vecswap(off, b - s, s);
      s = Math.min(d - c, n - d - 1);
      vecswap(b, n - s, s);

      // Recursively sort non-partition-elements
      if ((s = b - a) > 1) {
         sort(off, s);
      }
      if ((s = d - c) > 1) {
         sort(n - s, s);
      }
   }

   /**
    * Swap the element at position i with the element at position j
    * 
    * @param i
    * @param j
    */
   public void swap(final long i, final long j) {
      final long t = get(i);
      set(i, get(j));
      set(j, t);
   }

   /**
    * Swaps the elements at i .. (i+n-1) with the elements at j .. (j+n-1). The ranges may not
    * overlap!
    * 
    * @param i
    * @param j
    * @param n
    */
   private void vecswap(long i, long j, final long n) {
      for (long t = 0; t < n; t++, i++, j++)
         swap(i, j);
   }

   /**
    * Returns the index of the median of values at the given three indices.
    */
   private long med3(final long a, final long b, final long c) {
      return (get(a) < get(b) ? (get(b) < get(c) ? b : get(a) < get(c) ? c : a)
            : (get(b) > get(c) ? b : get(a) > get(c) ? c : a));
   }

   // ======================== COPY ================================
   /**
    * @return an exact copy of this array
    */
   public HugeLongArray copy() {
      return new HugeLongArray(this);
   }

   /**
    * creates a new array with 'to' - 'from' elements by copying the range [from .. to-1] of this
    * array. To copy the whole array, call the more efficient 'copy' method.
    * 
    * @param from
    *           index where copying starts
    * @param to
    *           index before which copying stops.
    * @return a copy of a range of this array
    */
   public HugeLongArray copyRange(final long from, final long to) {
      final long newsize = to - from;
      final HugeLongArray c = new HugeLongArray(newsize);
      for (long i = 0; i < newsize; i++) {
         c.set(i, get(i + from));
      }
      return c;
   }

   // ======================== toString ================================
   /**
    * @return string representation of the array, consisting of an opening square bracket '[', the
    *         first ten values, an information about the total number of elements, the last ten
    *         values, and a closing square bracket ']'. If the array contains at most 20 values, the
    *         string returned is contains the whole array, as in util.Arrays.toString.
    */
   @Override
   public String toString() {
      if (length == 0) {
         return "[]";
      }
      if (length <= 20) {
         return (Arrays.toString(arrays[0]));
      }
      StringBuilder sb = new StringBuilder(1024);
      sb.append("[");
      for (int i = 0; i < 9; i++) {
         sb.append(arrays[0][i]);
         sb.append(", ");
      }
      sb.append(String.format("...(%d numbers total)..., ", length));
      for (long i = 0; i < 8; i++) {
         sb.append(get(length - 10 + i));
         sb.append(", ");
      }
      sb.append(get(length - 1));
      sb.append("]");
      return sb.toString();
   }

   // ================= read from and write to file =========================================

   private static final long BUFSIZE = 1024 * 1024;
   private final ByteBuffer _bb = ByteBuffer.allocateDirect((int) BUFSIZE).order(
         ByteOrder.nativeOrder());

   /**
    * Reads a part of a file on disk into a part of this HugeLongArray. If the size of this array
    * is smaller than (start+len), a runtime exception occurs. We read 'len' items from the given
    * file position if the given position is &ge;= 0. If the given position is negative, we read
    * from the current file position. If nItems is negative, we read till the end.
    * 
    * @param fname
    *           file name
    * @param start
    *           position in this array at which to start filling it
    * @param nItems
    *           number of entries to read. If negative, read the whole (remaining) file.
    * @param fpos
    *           file index (in 'long' units) at which to start reading
    * @throws java.io.IOException
    *            if any I/O error occurs
    */
   public final void read(final String fname, long start, long nItems, final long fpos)
         throws IOException {
      final FileChannel channel = new FileInputStream(fname).getChannel();
      if (fpos >= 0)
         channel.position(BYTESPERELEMENT * fpos);
      final LongBuffer ib = _bb.asLongBuffer(); // type depends on 'a'
      if (nItems < 0)
         nItems = (channel.size() - channel.position()) / BYTESPERELEMENT;
      while (nItems > 0) {
         int bytestoread = (int) ((nItems * BYTESPERELEMENT < BUFSIZE) ? nItems * BYTESPERELEMENT
               : BUFSIZE);
         final int itemstoread = bytestoread / BYTESPERELEMENT;
         _bb.position(0).limit(bytestoread);
         while ((bytestoread -= channel.read(_bb)) > 0) {
         }
         ib.position(0).limit(itemstoread);
         // move ib into appropriate component array
         final int movi = (int) (start / BINSIZE);
         final int movj = (int) (start % BINSIZE);
         final int movs = arrays[movi].length - movj;
         final int tomove = (itemstoread > movs) ? movs : itemstoread;
         ib.get(arrays[movi], movj, tomove);
         if (tomove < itemstoread)
            ib.get(arrays[movi + 1], 0, itemstoread - tomove);
         start += itemstoread;
         nItems -= itemstoread;
      }
      channel.close();
   }

   /**
    * factory function for constructing a HugeLongArray from a binary file
    * 
    * @param fname
    *           the file name
    * @return a HugeLongArray with the file's contents
    * @throws java.io.IOException
    */
   public static final HugeLongArray fromFile(final String fname) throws IOException {
      final FileChannel channel = new FileInputStream(fname).getChannel();
      final long flen = channel.size();
      if (flen % BYTESPERELEMENT != 0)
         throw new IOException("File size not compatible with HugeLongArray");
      final HugeLongArray a = new HugeLongArray(flen / BYTESPERELEMENT);
      a.read(fname, 0, flen / BYTESPERELEMENT, 0);
      return a;
   }
}
