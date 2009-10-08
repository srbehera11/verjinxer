package verjinxer.util;

/**
 * BitArray for that rank queries can be computed.
 * 
 * @author Markus Kemmerling
 */
public class RankedBitArray extends BitArray {
   
   /**
    * Stores answers to rank1 queries for each log2-th position.
    */
   private int[] superblockrank;
   
   /**
    * Stores answers to rank1 queries within a superblock for each log-th position.
    */
   private short[] blockrank;
   
   /**
    * Length of a superblock.
    */
   private final int log;
   
   /**
    * Length of a block.
    */
   private final int log2;
   
   /**
    * Whether this bit array was changed after the last invocation of {@link #preRankCalculation()}.
    */
   boolean changed = true;

   public RankedBitArray(int size) {
      super(size);
      log = (int)MathUtils.log2(this.size);
      log2 = log*log;
   }
   
   /**
    * Invoked before a rank query after this bit array has changed.
    * Calculates the the lookup table {@link #superblockrank} and {@link #blockrank} needed to answer the rank query.  
    */
   private void preRankCalculation() {
      if(size < 2) {
         return;
      }
      
      superblockrank = new int[this.size/log2 + 1];
      blockrank = new short[this.size/log + 1];

      if (superblockrank.length > 0) {
         superblockrank[0] = 0;
      }
      if (blockrank.length > 0) {
         blockrank[0] = 0;
      }
      
      for(int j = 1; j < blockrank.length; j++) {
         final int smallblock = getBits(log*(j-1), log*j);
         final byte smallrank = RankCalculator.rank(1, smallblock, log);
         
         if(j % log  == 0) {
            assert j/log < superblockrank.length : String.format("superblockrank.length: %d, j/log: %d, j: %d, log: %d, size: %d", superblockrank.length, j/log, j, log, size);
            superblockrank[j/log] = superblockrank[(j/log)-1] + blockrank[j-1] + smallrank;
            blockrank[j] = 0;
         } else {
            blockrank[j] = (short) (blockrank[j-1] + smallrank);
         }
      }
      
      changed = false;
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
      if(size < 2) {
         if (size == 0 || pos == 0) {
            return 0;
         } else {
            if (bit == 0) {
               return 1 - this.bits[0];
            } else {
               return this.bits[0];
            }
         }
      }
      
      if (changed) {
         preRankCalculation();
      }
      
      //calculate rank1, rank0 can be obtained by rank0 = pos - rank1
      final int smallblock = getBits((pos/log)*log, pos);
      final int numberOnes = superblockrank[pos/log2] + blockrank[pos/log] + RankCalculator.rank(1, smallblock, pos - (pos/log)*log);
      
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

   @Override
   public void clear() {
      super.clear();
      changed = true;
   }

   @Override
   public void set(int i, boolean bval) {
      super.set(i, bval);
      changed = true;
   }

   @Override
   public void set(int i, int val) {
      super.set(i, val);
      changed = true;
   }
   
   

}
