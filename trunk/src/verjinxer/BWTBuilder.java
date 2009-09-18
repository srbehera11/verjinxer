package verjinxer;

import java.nio.BufferUnderflowException;
import java.nio.IntBuffer;

import verjinxer.sequenceanalysis.Sequences;
import verjinxer.sequenceanalysis.SuffixXorDLL;

/**
 * Class responsible for building a Burrows-Wheeler-Transformation.
 * 
 * @author Markus Kemmerling
 */
public class BWTBuilder {

   /**
    * Creates a BWT of the given sequence with help of its suffix array.
    * @param suffixArray
    *           The suffix array of the sequence.
    * @param sequence
    *           Sequence to build the BWT for.
    * @return The BWT of the sequence.
    */
   public static byte[] build(IntBuffer suffixArray, Sequences sequence) {
      final byte[] seq = sequence.array();
      byte[] bwt = new byte[suffixArray.capacity()];

      suffixArray.rewind();
      int indexPos = 0;
      int sequencePos;
      while (true) {
         try {
            sequencePos = suffixArray.get();
         } catch (BufferUnderflowException ex) {
            break;
         }
         bwt[indexPos] = sequencePos > 0 ? seq[sequencePos - 1] : seq[seq.length - 1];
         indexPos++;
      }

      return bwt;

   }

   /**
    * Creates a BWT from the given suffix list.
    * @param suffixDLL
    *           The suffix list to create the BWT from.
    * @return The BWT.
    */
   public static byte[] build(SuffixXorDLL suffixDLL) {
      final byte[] seq = suffixDLL.getSequence().array();

      byte[] bwt = new byte[suffixDLL.capacity()];

      suffixDLL.resetToBegin();
      int indexPos = 0;
      int sequencePos;
      if (suffixDLL.getCurrentPosition() != -1) {
         sequencePos = suffixDLL.getCurrentPosition();
         bwt[indexPos] = sequencePos > 0 ? seq[sequencePos - 1] : seq[seq.length - 1];
         indexPos++;
         while (suffixDLL.hasNextUp()) {
            suffixDLL.nextUp();
            sequencePos = suffixDLL.getCurrentPosition();
            bwt[indexPos] = sequencePos > 0 ? seq[sequencePos - 1] : seq[seq.length - 1];
            indexPos++;
         }
      }

      return bwt;
   }

}
