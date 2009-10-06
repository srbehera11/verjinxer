package verjinxer.util;

public class RankCalculator {
   private static int[][] smallrank = new int[65536][16]; // new int[1<<16][16]
   
   /**
    * Returns the number of times the given bit (0 or 1) appears in smallblock[0,...,prefix].
    * @param bit
    *           The bit to count.
    * @param smallblock
    *           The bitString as integer in which the bit is counted.
    * @param prefix
    *           The end of the substring of smallblock (inclusive) in which the bit is counted.
    * @return
    */
   public static int rank(int bit, int smallblock, int prefix) {
      final int half1 = smallblock & 65535;
      final int half2 = smallblock >> 16;
      final int t = prefix > 15 ? 15 : prefix;
      final int u = prefix - t;
      
      final int rank1 = smallrank[half1][t] + smallrank[half2][u];
      if(bit == 0) {
         return prefix - rank1;
      } else {
         return rank1; 
      }
   }
   
   private static int simpleRank1(int smallblock, int prefix) {
      //TODO calculate the initialisation code for smallrank
      int counter = 0;
      for(int i = 0; i <= prefix; i++) {
         final int bit = smallblock & (1<<i);
         if (bit == 1) {
            counter++;
         }
      }
      return counter;
   }

}
