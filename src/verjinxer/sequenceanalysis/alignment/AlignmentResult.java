package verjinxer.sequenceanalysis.alignment;

import verjinxer.sequenceanalysis.alignment.Aligner.MatrixPosition;

/**
 * 
 * @author Markus Kemmerling
 */
public class AlignmentResult {
   private byte[] sequence1, sequence2;

   private int length, errors;
   private MatrixPosition begin, end;

   public AlignmentResult() {
      // all attributes are initialized by default
   }

   public AlignmentResult(byte[] sequence1, byte[] sequence2, MatrixPosition begin,
         MatrixPosition end, int length, int errors) {
      setAllAttributes(sequence1, sequence2, begin, end, length, errors);
   }

   public void setAllAttributes(byte[] sequence1, byte[] sequence2, MatrixPosition begin,
         MatrixPosition end, int length, int errors) {
      this.sequence1 = sequence1;
      this.sequence2 = sequence2;
      this.begin = begin;
      this.length = length;
      this.errors = errors;
      this.end = end;
   }

   public byte[] getSequence1() {
      return sequence1;
   }

   public byte[] getSequence2() {
      return sequence2;
   }

   public MatrixPosition getBeginPosition() {
      return begin;
   }

   public MatrixPosition getEndPosition() {
      return end;
   }

   public int getLength() {
      return length;
   }

   public int getErrors() {
      return errors;
   }

   @Override
   public String toString() {
      return printAsBytes();
   }

   /**
    * Prints the contrast of the two sequences considered as bytes.
    * 
    * @return A visualization of the alignment
    */
   public String printAsBytes() {
      StringBuilder sb = new StringBuilder(sequence1.length * 3 + 4);
      for (byte b : sequence1) {
         if (b == Aligner.GAP) {
            sb.append(' ');
         } else {
            sb.append(b);
         }
      }

      sb.append('\n');

      int beginIndex = Math.max(begin.row, begin.column);

      for (int i = 0; i < sequence1.length; i++) {
         if (i >= beginIndex && i < beginIndex + length) {
            if (sequence1[i] == sequence2[i]) {
               sb.append('|');
            } else {
               sb.append('x');
            }
         } else {
            sb.append(' ');
         }
      }

      sb.append('\n');

      for (byte b : sequence2) {
         if (b == Aligner.GAP) {
            sb.append(' ');
         } else {
            sb.append(b);
         }
      }

      return sb.toString();
   }

   /**
    * Prints the contrast of the two sequences considered as characters.
    * 
    * @return A visualization of the alignment
    */
   public String printAsChars() {
      StringBuilder sb = new StringBuilder(sequence1.length * 3 + 4);
      for (byte b : sequence1) {
         if (b == Aligner.GAP) {
            sb.append(' ');
         } else {
            sb.append((char) b);
         }
      }

      sb.append('\n');

      int beginIndex = Math.max(begin.row, begin.column);

      for (int i = 0; i < sequence1.length; i++) {
         if (i >= beginIndex && i < beginIndex + length) {
            if (sequence1[i] == sequence2[i]) {
               sb.append('|');
            } else {
               sb.append('x');
            }
         } else {
            sb.append(' ');
         }
      }

      sb.append('\n');

      for (byte b : sequence2) {
         if (b == Aligner.GAP) {
            sb.append(' ');
         } else {
            sb.append((char) b);
         }
      }

      return sb.toString();
   }
}