import java.util.Random;

import verjinxer.util.RankCalculator;
import verjinxer.util.RankedBitArray;


public class RankTimer {

   public static void main (String args[]) {
//         testRankedBitArray();
//         testRank();
      testRank2(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
   }
   
   private static void testRank2(int pos, int number) {
      Random rand = new Random();
      final long startTime = System.currentTimeMillis();
      for(int i = 0; i < number; i++) {
         final int integer = rand.nextInt();
         final int bit = (int) (rand.nextDouble() + 0.5);
         RankCalculator.rank(bit, integer, pos);
      }
      System.out.println(System.currentTimeMillis() - startTime);
   }

   private static void testRankedBitArray() {
      Random rand = new Random();
      final int size = 50000000;
      RankedBitArray rba = new RankedBitArray(size);
      
      for(int i = 0; i < size; i++) {
         rba.set(i, rand.nextBoolean());
      }
      
      final long startTime = System.currentTimeMillis();
      for(int i = 0; i < 100000; i++) {
         final int pos = rand.nextInt(size);
         final int bit = (int) (rand.nextDouble() + 0.5);
         rba.rank(bit, pos);
      }
      
      System.out.println(System.currentTimeMillis() - startTime);
   }
   
   private static void testRank() {
      Random rand = new Random();
      final long startTime = System.currentTimeMillis();
      for(int i = 0; i < 10000; i++) {
         final int integer = rand.nextInt();
         final int pos = rand.nextInt(33);
         final int bit = (int) (rand.nextDouble() + 0.5);
         RankCalculator.rank(bit, integer, pos);
      }
      System.out.println(System.currentTimeMillis() - startTime);
   }
}
