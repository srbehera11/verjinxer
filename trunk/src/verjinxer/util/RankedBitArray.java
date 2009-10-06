package verjinxer.util;

/**
 * BitArray for that rank queries can be computed.
 * 
 * @author Markus Kemmerling
 */
public class RankedBitArray extends BitArray {

   public RankedBitArray(int size) {
      super(size);
   }
   
   /**
    * Returns the number of times the given bit (0 or 1) appears in the prefix of this BitArray B[0,...,pos-1];
    * @param bit
    *           Bit for that the number of occurrence is returned.
    * @param pos
    *           End of the prefix (exclusive) within the occurrence are returned.
    * @return Number of times the given bit appears within the positions 0 (inclusive) to pos (exclusive) of this BitArray.
    */
   public int rank(int bit, int pos) {
      //TODO use precomputed lookup tables
      
      //calculate rank1, rank0 can be obtained by rank0 = pos - rank1
      int numberOnes = 0;
      for(int i = 0; i < pos; i++) {
         if (get(i) == 1) {
            numberOnes++;
         }
      }
      
      if (bit == 1) {
         return numberOnes;
      } else {
         return pos - numberOnes;
      }
   }

   /**
    * Returns a subsequence of this BitArray as integer. The subsequence begins at the specified beginIndex and extends to the character at index endIndex - 1.
    * The bit at position 'from' stands at the lowest position in the returned integer 'i'(bitmask 1), the bit at position 'from+1'
    * stands at the second position in 'i' (bitmask 2) and so on.
    * Note that you can request maximal 32 bits. If you request more, only the first 32 are returned.
    * @param from
    *          The beginning index, inclusive.
    * @param to
    *          The ending index, exclusive. 
    * @return The specified subsequence.
    */
   public int getBits(final int from, final int to) {
      //TODO use bit manipulation direct on underlying structure without #get() 
      int diff = to - from;
      if (diff > 32) {
         diff = 32;
      }
      int i = 0;
      for (int j = 0; j < diff; j++) {
         if (get(j+from) == 1) {
            i |= 1 << j;
         }
      }
      return i;
   }

}
