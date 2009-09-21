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
    * Builds a BWT-Index from a suffix list.
    * 
    * @param suffixDLL
    *           Suffix list from that the index shall be build.
    * @return The BWT-Index.
    */
   public static BWTIndex build(SuffixXorDLL suffixDLL) {
      final byte[] seq = suffixDLL.getSequence().array();
      
      int[]  c = new int[256];
      byte[] l = new byte[suffixDLL.capacity()];
      int[] el;
      
      {// build e and l, count characters in c
         suffixDLL.resetToBegin();
         int indexPos = 0;
         int sequencePos;
         if (suffixDLL.getCurrentPosition() != -1) {
            sequencePos = suffixDLL.getCurrentPosition();
            int chi = seq[sequencePos] +128;
            c[chi]++; // count characters
            l[indexPos] = sequencePos > 0 ? seq[sequencePos - 1]
                  : seq[seq.length - 1];
            indexPos++;
            while (suffixDLL.hasNextUp()) {
               suffixDLL.nextUp();
               sequencePos = suffixDLL.getCurrentPosition();
               chi = seq[sequencePos] +128;
               c[chi]++; // count characters
               l[indexPos] = sequencePos > 0 ? seq[sequencePos - 1]
                     : seq[seq.length - 1];
               indexPos++;
            }
         }
      }// END build e and l, count characters in c

      buildC(c);

      el = buildEL(c, l);
      
      return new BWTIndex(c, el);
   }

   /**
    * Builds a BWT-Index from a suffix array and the associated sequence/text.
    * 
    * @param pos
    *           Suffix array from that the index shall be build.
    * @param sequences
    *           Sequence/Text from that the index shall be build.
    * @return The BWT-Index.
    */
   public static BWTIndex build(IntBuffer pos, Sequences sequences) {
      final byte[] seq = sequences.array();
      
      int[]  c = new int[256];
      byte[] l = new byte[pos.capacity()];
      int[] el;
      
      {// build e and l, count characters in c
         pos.rewind();
         int indexPos = 0;
         int sequencePos;
            while (true) {
               try {
                  sequencePos = pos.get();
               } catch (BufferUnderflowException ex) {
                  break;
               }
               final int chi = seq[sequencePos] + 128;
               c[chi]++; // count characters
               l[indexPos] = sequencePos > 0 ? seq[sequencePos - 1]
                     : seq[seq.length - 1];
               indexPos++;
            }
      }// END build e and l, count characters in c

      buildC(c);

      el = buildEL(c, l);
      
      return new BWTIndex(c,el);
   }

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
   public static BWTIndex build(final byte[] bwt) {
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
      return new BWTIndex(c, el);
   }

}
