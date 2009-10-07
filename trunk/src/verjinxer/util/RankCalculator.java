package verjinxer.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Class providing methods to calculate rank queries for arbitrary bit strings.
 * 
 * @author Markus Kemmerling
 */
public class RankCalculator {
//   private static byte[][] smallrank;// = new byte[65536][16]; // new int[1<<16][16]
//
//   static {
//      smallrank =calculateSmallrank();
//   }

   /**
    * Provides convenient access to the smallrank table.
    * 
    * @param smallblock
    *           The bit string in which the ones are counted.
    * @param prefix
    *           The ones are counted in 'smallblock' from position 0 to 'prefix' (exclusive)
    * @return Number of times the one bit exists in smallblock[0,...,prefix-1]
    */
   private static byte smallrank(int smallblock, int prefix) {
      if (prefix < 1) {
         return 0;
      } else {
         //return smallrank[smallblock][prefix - 1];
         return simpleRank1(smallblock, prefix-1);
      }
   }

   /**
    * Returns the number of times the given bit (0 or 1) appears in smallblock[0,...,prefix-1].
    * 
    * @param bit
    *           The bit to count.
    * @param smallblock
    *           The bitString as integer in which the bit is counted.
    * @param prefix
    *           The end of the substring of smallblock (exclusive) in which the bit is counted.
    * @return
    */
   public static int rank(int bit, int smallblock, int prefix) {
      final int half1 = smallblock & 65535;
      final int half2 = smallblock >> 16;
      final int t = prefix > 16 ? 16 : prefix;
      final int u = prefix - t;

      final int rank1 = smallrank(half1, t) + smallrank(half2, u);
      if (bit == 0) {
         return prefix - rank1;
      } else {
         return rank1;
      }
   }

   /**
    * Counts the ones in smallblock[0,...,prefix-1] by walking through it.
    * 
    * @param smallblock
    *           Bit string as integer in which the ones are counted.
    * @param prefix
    *           The end of the substring of smallblock (exclusive) in which the bit is counted.
    * @return Number of times the one bit exists in smallblock[0,...,prefix-1]
    */
   private static byte simpleRank1(int smallblock, int prefix) {
      byte counter = 0;
      for (int i = 0; i <= prefix; i++) {
         final int bit = smallblock & (1 << i);
         if (bit != 0) {
            counter++;
         }
      }
      return counter;
   }

   /**
    * Calculates the smallrank table and stores it to disc in data/smallrank.dat
    * 
    * @param args
    * @throws IOException
    */
   public static void main(String args[]) throws IOException {
      byte[][] smallrank = calculateSmallrank();
      writeSmallrank("data/smallrank.dat", smallrank);
   }

   /**
    * Calculates the smallrank table from scratch.
    * 
    * @return The smallrank table.
    */
   private static byte[][] calculateSmallrank() {
      byte[][] smallrank = new byte[65536][16]; // new int[1<<16][16]

      for (int i = 0; i < smallrank.length; i++) {
         for (int j = 0; j < smallrank[i].length; j++) {
            smallrank[i][j] = simpleRank1(i, j);
         }
      }
      return smallrank;
   }

   /**
    * Writes the given 2 dimensional byte array to disc
    * 
    * @param filename
    *           Where to store the array.
    * @param smallrank
    *           The array to store.
    * @throws IOException
    */
   private static void writeSmallrank(String filename, byte[][] smallrank) throws IOException {

      FileOutputStream fos = new FileOutputStream(filename);
      ObjectOutputStream oos = new ObjectOutputStream(fos);

      oos.writeObject(smallrank);

      oos.close();
   }

   /**
    * Reads the smallrank table from disc.
    * 
    * @param filename
    *           File where the table was stored by using {@link #writeSmallrank(String, byte[][])}.
    * @return The smallrank table.
    * @throws IOException
    * @throws ClassNotFoundException
    */
   private static byte[][] readSmallrank(String filename) throws IOException,
         ClassNotFoundException {
      FileInputStream fis = new FileInputStream(filename);
      ObjectInputStream ois = new ObjectInputStream(fis);

      byte[][] smallrank = (byte[][]) ois.readObject();

      ois.close();

      return smallrank;
   }

}
