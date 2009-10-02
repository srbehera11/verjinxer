package verjinxer.util;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Markus Kemmerling
 */
public class WaveletTreeTest {
   
   private static final byte[][] sequences = {
      {0,1,0,0,1,2,1,2,0,2,1},
      {0,1,0,0,1,2,4,4,3,4,2,4}
   };
   
   private static final byte[][] alphabet = {
      {0,2},
      {0,4}
   };
   
   private static WaveletTree[] trees;
   

   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
      trees = new WaveletTree[sequences.length];
      byte[] sequence;
      for (int i = 0; i < sequences.length; i++) {
         sequence = Arrays.copyOf(sequences[i], sequences[i].length);
         trees[i] = new WaveletTree(sequence, alphabet[i][0], alphabet[i][1]);
      }
   }

   @AfterClass
   public static void tearDownAfterClass() throws Exception {
   }

   @Test
   public void testGetCharacter() {
      for(int i = 0; i < sequences.length; i++) {
         for(int j = 0; j < sequences[i].length; j++) {
            if (i == 0 && j == 7 ) {
               System.out.println("");
            }
            assertEquals(String.format("Error for i=%d, j=%d", i,j), sequences[i][j], trees[i].getCharacter(j));
         }
      }
   }

   @Test
   public void testRank() {
      for(int i = 0; i < sequences.length; i++) {
         for(int j = 1; j <= sequences[i].length; j++) {
            for (byte b = alphabet[i][0]; b <= alphabet[i][1]; b++ ) {
//               if (i ==1 && j == 7 && b == 2) {
//                  System.out.println("");
//               }
               
               int rank = rank(b, j, sequences[i]);
               assertEquals(String.format("Error for i=%d, j=%d, b=%d", i,j,b), rank, trees[i].rank(b, j));
            }
         }
      }
   }

   /**
    * TODO
    * @param character
    * @param prefix
    * @param sequence
    * @return
    */
   private static int rank(byte character, int prefix, byte[] sequence) {
      int counter = 0;
      for(int i = 0; i < prefix && i < sequence.length; i++) {
         if (sequence[i] == character) {
            counter++;
         }
      }
      return counter;
   }
}
