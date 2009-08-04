package verjinxer.sequenceanalysis.alignment;

import verjinxer.sequenceanalysis.alignment.Aligner.MatrixPosition;

/**
 * @author Markus Kemmerling
 */
public class AlignmentResult {
   /** The sequences that where aligned enlarged with gaps if necessary */
   private byte[] sequence1, sequence2;

   /** Length of the alignment */
   private int length;
   
   /** Errors within the alignment */
   private int errors;
   
   /** Position in the alignment table where the alignment starts */
   private MatrixPosition begin;
   
   /** Position in the alignment table where the alignment ends */
   private MatrixPosition end;

   /**
    * Creates a new instance of AlignmentResult and initializes all attributes with default values.
    */
   public AlignmentResult() {
   }

   /**
    * Creates a new instance of AlignmentResult and initializes all attributes with the given
    * values.
    * 
    * @param sequence1
    *           First sequence enlarged with gaps if necessary.
    * @param sequence2
    *           Second sequence enlarged with gaps if necessary.
    * @param begin
    *           Position in the alignment table where the alignment starts.
    * @param end
    *           Position in the alignment table where the alignment ends.
    * @param length
    *           Length of the alignment.
    * @param errors
    *           Errors within the alignment.
    */
   public AlignmentResult(byte[] sequence1, byte[] sequence2, MatrixPosition begin,
         MatrixPosition end, int length, int errors) {
      setAllAttributes(sequence1, sequence2, begin, end, length, errors);
   }

   /**
    * Sets all attributes to the given values.
    * 
    * @param sequence1
    *           First sequence enlarged with gaps if necessary.
    * @param sequence2
    *           Second sequence enlarged with gaps if necessary.
    * @param begin
    *           Position in the alignment table where the alignment starts.
    * @param end
    *           Position in the alignment table where the alignment ends.
    * @param length
    *           Length of the alignment.
    * @param errors
    *           Errors within the alignment.
    */
   public void setAllAttributes(byte[] sequence1, byte[] sequence2, MatrixPosition begin,
         MatrixPosition end, int length, int errors) {
      this.sequence1 = sequence1;
      this.sequence2 = sequence2;
      this.begin = begin;
      this.length = length;
      this.errors = errors;
      this.end = end;
   }

   /**
    * @return First sequence enlarged with gaps if necessary.
    */
   public byte[] getSequence1() {
      return sequence1;
   }

   /**
    * @return Second sequence enlarged with gaps if necessary.
    */
   public byte[] getSequence2() {
      return sequence2;
   }

   /**
    * @return Position in the alignment table where the alignment starts.
    */
   public MatrixPosition getBeginPosition() {
      return begin;
   }

   /**
    * @return Position in the alignment table where the alignment ends.
    */
   public MatrixPosition getEndPosition() {
      return end;
   }

   /**
    * @return Length of the alignment.
    */
   public int getLength() {
      return length;
   }

   /**
    * @return Errors within the alignment.
    */
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