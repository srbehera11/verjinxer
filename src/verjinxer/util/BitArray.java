
package verjinxer.util;

import java.io.IOException;

/**
 * This class implements a simple BitArray.
 * Its initial size cannot be changed.
 * Note: Unless you have reason to use this (for efficient I/O),
 * you probably rather want to use the standard library's BitSet.
 * @author Sven Rahmann
 */
public class BitArray {

   final int[] bits; // package access for ArrayFile
   
   /** number of bits */
   public final int size;
   
   /**
    * Create a new BitArray with space for 'size' bits.
    * Initially, all bits are zero.
    * It is an error to access bits outside the range 0 .. size-1.
    * @param size  size of the BitArray (in bits)
    */
   public BitArray(final int size) {
      bits = new int[(int)((size+31L)/32L) + 1];
      this.size = size;
      bits[bits.length-1] = size; // save size in last int
   }
   
   private BitArray(final int[] bits) {
      this.bits = bits;
      final int bl = bits.length-1;
      size = bits[bl];
      assert(bl*32 <= size): String.format("bl: %d%nsize:%d", bl, size);
      assert(size < (bl+1)*32);
   }

   /**
    * @return number of 1-bits in this BitArray
    */
   public int cardinality() {
      int c=0;
      for(int b: bits) c+=Integer.bitCount(b);
      return c - Integer.bitCount(bits[bits.length-1]); // subtract last as it contains size
   }

   /** set the i-th bit in the BitArray to value 'val'. 
    *  More precisely, set it to val's lowest bit.
    * @param i   position to change
    * @param val bit value to set at position i
    */
   public void set(final int i, final int val) {
      if(i<0 || i>=size) throw new IndexOutOfBoundsException(String.format("%d outside [0,%d[", i, size));
      final int idx = i/32;
      final int mod = i%32;
      if ((val&1)==1) bits[idx] |=  (1<<mod);
      else            bits[idx] &= ~(1<<mod);
   }

   /** set the i-th bit in the BitArray to value 'bval', given as a boolean.
    * @param i    position to change
    * @param bval bit value to set at position i (as a boolean)
    */
   public void set(final int i, final boolean bval) {
      if(i<0 || i>=size) throw new IndexOutOfBoundsException(String.format("%d outside [0,%d[", i, size));
      final int idx = i/32;
      final int mod = i%32;
      if (bval) bits[idx] |=  (1<<mod);
      else      bits[idx] &= ~(1<<mod);
   }

   /**
    * get the i-th bit in the BitArray.
    * @param i  position to read
    * @return   bit value at position i
    */
   public int get(final int i) {
      if(i<0 || i>=size) throw new IndexOutOfBoundsException(String.format("%d outside [0,%d[", i, size));
      final int idx = i/32;
      final int mod = i%32;
      return ( (bits[idx] & (1<<mod)) !=0 )? 1 : 0;
   }
   
   /**
    * set all bits to zero
    */
   public void clear() {
      for(int i=0; i<bits.length-1; i++) bits[i]=0;
   }

   /**
    * write the BitArray to an ArrayFile
    * @param f the ArrayFile
    * @throws java.io.IOException if any IO error occurs
    */
   public void writeTo(final ArrayFile f) throws IOException {
      f.writeArray(bits);
   }
   
   public static BitArray readFrom(final ArrayFile f) throws IOException {
      final int[] b = f.readArray((int[])null);
      return new BitArray(b);
   }

   /**
    * 
    * @param from
    * @param to
    * @return
    * @author Markus Kemmerling
    */
   public int getBits(final int from, final int to) {
      //TODO use bit manipulation direct on underlying structure without #get() 
      final int diff = to - from;
      int i = 0;
      for (int j = 0; j <= diff; j++) {
         if (get(j+from) == 1) {
            i |= 1 << j;
         }
      }
      return i;
   }
}
