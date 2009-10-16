import java.util.Random;

import verjinxer.util.RankCalculator;
import verjinxer.util.RankedBitArray;


public class RankeTimer {

   public static void main (String args[]) {
         Random rand = new Random();
         final int size = 10000000;
         RankedBitArray rba = new RankedBitArray(size);
         
         for(int i = 0; i < size; i++) {
            rba.set(i, rand.nextBoolean());
         }
         
         final long startTime = System.currentTimeMillis();
         for(int i = 0; i < 10000; i++) {
            final int pos = rand.nextInt(size);
            final int bit = (int) (rand.nextDouble() + 0.5);
            rba.rank(bit, pos);
         }
         
         System.out.println(System.currentTimeMillis() - startTime);

//         Random rand = new Random();
//         final long startTime = System.currentTimeMillis();
//         for(int i = 0; i < 10000; i++) {
//            final int integer = rand.nextInt();
//            final int pos = rand.nextInt(33);
//            final int bit = (int) (rand.nextDouble() + 0.5);
//            RankCalculator.rank(bit, integer, pos);
//         }
//         System.out.println(System.currentTimeMillis() - startTime);
   }
}
