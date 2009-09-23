package verjinxer;

import java.nio.BufferUnderflowException;
import java.nio.IntBuffer;

import verjinxer.sequenceanalysis.Sequences;
import verjinxer.sequenceanalysis.SuffixXorDLL;
import verjinxer.util.MathUtils;

/**
 * Class responsible for building a Burrows-Wheeler-Transformation.
 * 
 * @author Markus Kemmerling
 */
public class BWTBuilder {
   
   public static int calculateBaseIndex( int length ) {
      final int power = (int)Math.ceil(MathUtils.log2(MathUtils.log2(length)));
      return 1<<power;
   }

   /**
    * Creates a BWT of the given sequence with help of its suffix array.
    * @param suffixArray
    *           The suffix array of the sequence.
    * @param sequence
    *           Sequence to build the BWT for.
    * @return The BWT of the sequence.
    */
   public static BWT build(IntBuffer suffixArray, Sequences sequence) {
      final byte[] seq = sequence.array();
      byte[] bwt = new byte[suffixArray.capacity()];
      
      final int value = calculateBaseIndex(seq.length);
      final int bitmask = value -1;
      final int numberIndex = (int)Math.ceil((double)seq.length/(double)value);
      int[] sampledSuffixArray = new int[numberIndex];

      suffixArray.rewind();
      int bwtPos = 0;
      int samplePos = 0;
      int sequencePos;
      while (true) {
         try {
            sequencePos = suffixArray.get();
         } catch (BufferUnderflowException ex) {
            break;
         }
         sequencePos = sequencePos > 0 ? sequencePos - 1 : seq.length - 1; // go one step left
         bwt[bwtPos] = seq[sequencePos];
         if( (bwtPos&bitmask) == 0 ) {
            sampledSuffixArray[samplePos] = sequencePos;
            assert samplePos * value == bwtPos;
            samplePos++;
         }
         bwtPos++;
         
      }
      assert samplePos == sampledSuffixArray.length;
      return new BWT(bwt,sampledSuffixArray);

   }

   /**
    * Creates a BWT from the given suffix list.
    * @param suffixDLL
    *           The suffix list to create the BWT from.
    * @return The BWT.
    */
   public static BWT build(SuffixXorDLL suffixDLL) {
      final byte[] seq = suffixDLL.getSequence().array();

      byte[] bwt = new byte[suffixDLL.capacity()];
      
      final int value = calculateBaseIndex(seq.length);
      final int bitmask = value -1;
      final int numberIndex = (int)Math.ceil((double)seq.length/(double)value);
      int[] sampledSuffixArray = new int[numberIndex];

      suffixDLL.resetToBegin();
      int bwtPos = 0;
      int samplePos = 0;
      int sequencePos;
      if (suffixDLL.getCurrentPosition() != -1) {
         sequencePos = suffixDLL.getCurrentPosition();
         sequencePos = sequencePos > 0 ? sequencePos - 1 : seq.length - 1; // go one step left
         bwt[bwtPos] = seq[sequencePos];
         if( (bwtPos&bitmask) == 0 ) {
            sampledSuffixArray[samplePos] = sequencePos;
            assert samplePos * value == bwtPos;
            samplePos++;
         }
         bwtPos++;
         while (suffixDLL.hasNextUp()) {
            suffixDLL.nextUp();
            sequencePos = suffixDLL.getCurrentPosition();
            sequencePos = sequencePos > 0 ? sequencePos - 1 : seq.length - 1; // go one step left
            bwt[bwtPos] = seq[sequencePos];
            if( (bwtPos&bitmask) == 0 ) {
               sampledSuffixArray[samplePos] = sequencePos;
               assert samplePos * value == bwtPos;
               samplePos++;
            }
            bwtPos++;
         }
      }

      return new BWT(bwt,sampledSuffixArray);
   }
   
   public static class BWT{
      public final byte[] bwt;
      public final int[] sampledSuffixArray;
      
      private BWT(final byte[] bwt, final int[] sampledSuffixArray) {
         this.bwt = bwt;
         this.sampledSuffixArray = sampledSuffixArray;
      }
   }

}
