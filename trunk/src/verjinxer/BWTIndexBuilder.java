package verjinxer;

import java.nio.BufferUnderflowException;
import java.nio.IntBuffer;

import verjinxer.sequenceanalysis.BWTIndex;
import verjinxer.sequenceanalysis.Sequences;
import verjinxer.sequenceanalysis.SuffixXorDLL;

/**
 * Class responsible for build a BWT-Index
 * @author Markus Kemmerling
 */
public class BWTIndexBuilder {

   /**
    * Creates the needed array c, where for each byte is first position in e is stored, from the
    * given array, where for each byte is number is stored. The given array will be overridden.
    * 
    * @param c
    *           Number of each bytes in sequence.
    */
   private static void buildC(int[] c) {
      int tmp = c[0];
      c[0] = 0;
      for (int i = 1; i < c.length; i++) {
         final int tmp2 = c[i];
         c[i] = c[i - 1] + tmp;
         tmp = tmp2;
      }
   }

   /**
    * Creates the needed array el, where for a character 'b' at position 'i' in array e the position
    * (in array e) where to find the next character regarding the associated text/sequence is
    * stored.
    * 
    * @param c
    *           First position in array e where to find a byte 'b'.
    * @param l
    *           BWT transformation of the associated text/sequence.
    */
   private static int[] buildEL(final int[] c, final byte[] l) {
      int[] el = new int[l.length];

//      long starttime = System.currentTimeMillis();

      int[] counter = new int[c.length];
      for (int i = 0; i < l.length; i++) {
         final byte character = l[i];
         final int chi = character + 128;
         final int indexPos = c[chi];
         final int charCounter = counter[chi];
         el[indexPos + charCounter] = i;
         counter[chi]++;
      }
//      TODO check which version is faster - for the small sequences in the test cases there is no time difference
//      long endtime = System.currentTimeMillis();
//      System.out.printf("!!! Time needed for building el with FIRST method: %d !!!%n", endtime-starttime);
//      
//      starttime = System.currentTimeMillis();
//      counter = Arrays.copyOf(c, c.length);
//      for (int i = 0; i < l.length; i++) {
//         final byte character = l[i];
//         final int chi = character + 128;
//         final int indexPos = counter[chi];
//         el[indexPos] = i;
//         counter[chi]++;
//      }
//      endtime = System.currentTimeMillis();
//      System.out.printf("!!! Time needed for building el with SECOND method: %d !!!%n", endtime-starttime);
      
      return el;
   }

   /**
    * Builds a BWT-Index from the give Burrows-Wheeler-Transformation for the sequence/text associated with the Transformation.
    * @param bwt
    *          Burrows-Wheeler-Transformation to build the index from.
    * @return The BWT-Index.
    */
   public static BWTIndex build(final byte[] bwt, final int[] bwt2text) {
      int[] c = new int[256];
      int[] histo = new int[256];
      int[] el;
      
      for(int i = 0; i < bwt.length; i++) {
         final byte b = bwt[i];
         final int bi = b + 128;
         histo[bi]++;
      }
      c[0]=0;
      for (int i = 1; i < 256; i++) {
         c[i] = c[i-1] + histo[i-1];
      }
      
      el = buildEL(c, bwt);
      return new BWTIndex(c, el, bwt2text);
   }

}
