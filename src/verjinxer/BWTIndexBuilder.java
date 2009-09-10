package verjinxer;

import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.sequenceanalysis.BWTIndex;
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
      final Alphabet alphabet = suffixDLL.getAlphabet();
      final byte[] sequences = suffixDLL.getSequence().array();
      
      int[]  c = new int[256];
      byte[] e = new byte[suffixDLL.capacity()];
      byte[] l = new byte[suffixDLL.capacity()];
      int[] el = new int[suffixDLL.capacity()];
      
      {// build e and l, count characters in c
         suffixDLL.resetToBegin();
         int indexPos = 0;
         int sequencePos;
         if (suffixDLL.getCurrentPosition() != -1) {
            sequencePos = suffixDLL.getCurrentPosition();
            e[indexPos] = sequences[sequencePos];
            int chi = sequences[sequencePos] +128;
            c[chi]++; // count characters
            l[indexPos] = sequencePos > 0 ? sequences[sequencePos - 1]
                  : sequences[sequences.length - 1];
            indexPos++;
            while (suffixDLL.hasNextUp()) {
               suffixDLL.nextUp();
               sequencePos = suffixDLL.getCurrentPosition();
               e[indexPos] = sequences[sequencePos];
               chi = sequences[sequencePos] +128;
               c[chi]++; // count characters
               l[indexPos] = sequencePos > 0 ? sequences[sequencePos - 1]
                     : sequences[sequences.length - 1];
               indexPos++;
            }
         }
      }// END build e and l, count characters in c

      {// build c
         int tmp = c[0];
         c[0] = 0;
         for (int i = 1; i < c.length; i++) {
            final int tmp2 = c[i];
            c[i] = c[i - 1] + tmp;
            tmp = tmp2;
         }
      }// END build c

      {// build el
         int[] counter = new int[c.length];
         for (int i = 0; i < l.length; i++) {
            final byte character = l[i];
            final int chi = character+128;
            final int indexPos = c[chi];
            final int charCounter = counter[chi];
            el[indexPos + charCounter] = i;
            counter[chi]++;
         }
      }// END build el
      
      return new BWTIndex(c,e,el);
   }

}
