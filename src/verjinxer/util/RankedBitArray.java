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
      
      //calculate rank1, rank0 can be obtained by rank = pos - rank1
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
    * TODO
    * @param from
    * @param to
    * @return
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
