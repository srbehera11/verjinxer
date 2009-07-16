package verjinxer.sequenceanalysis.alignment;

import java.util.Arrays;

import verjinxer.sequenceanalysis.alignment.EndLocations.MatrixPosition;
import verjinxer.sequenceanalysis.alignment.IAligner.Direction;
import verjinxer.util.ArrayUtils;

/**
 * @author Markus Kemmerling
 */
public class SemiglobalAligner {

   
   public static class SemiglobalAlignmentResult {
      private byte[] sequence1, sequence2;
   
      private int begin, length, errors;
   
      public SemiglobalAlignmentResult() {
         // all attributes are initialized by default
      }
   
      public SemiglobalAlignmentResult(byte[] sequence1, byte[] sequence2, int begin, int length,
            int errors) {
         setAllAttributes(sequence1, sequence2, begin, length, errors);
      }
   
      public void setAllAttributes(byte[] sequence1, byte[] sequence2, int begin, int length,
            int errors) {
         this.sequence1 = sequence1;
         this.sequence2 = sequence2;
         this.begin = begin;
         this.length = length;
         this.errors = errors;
      }
   
      public byte[] getSequence1() {
         return sequence1;
      }
   
      public byte[] getSequence2() {
         return sequence2;
      }
   
      public int getBegin() {
         return begin;
      }
   
      public int getLength() {
         return length;
      }
   
      public int getErrors() {
         return errors;
      }
   
      @Override
      public String toString() {
         StringBuilder sb = new StringBuilder(sequence1.length * 3 + 4);
         for (byte b : sequence1) {
            if (b == IAligner.GAP) {
               sb.append(' ');
            } else {
               sb.append(b);
            }
         }
   
         sb.append('\n');
   
         for (int i = 0; i < sequence1.length; i++) {
            if (i >= begin && i < begin + length) {
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
            if (b == IAligner.GAP) {
               sb.append(' ');
            } else {
               sb.append(b);
            }
         }
   
         return sb.toString();
      }
   }

   private BeginLocations beginLocation;
   private EndLocations endLocation;
   
   public void setBeginLocations(BeginLocations beginLocation) {
      this.beginLocation = beginLocation;
   }
   
   public void setEndLocations(EndLocations endLocation) {
      this.endLocation = endLocation;
   }

   /**
    * Computes an end-gap free alignment. Also called free-shift alignment or semiglobal alignment.
    * The alignment is only computed for specified ranges within the given sequences. Particular,
    * the alignment is computed of s1[start1:end1] and s2[start2:end2] (a[x:y] denotes the subarray
    * of an array a with initial index x (inclusive) and final index y (exclusive)).
    * 
    * @param s1
    *           first sequence.
    * @param start1
    *           first index in s1 to compute the alignment from.
    * @param end1
    *           final index in s1 to compute the alignment from, exclusive. (This index may lie
    *           outside the array.)
    * @param s2
    *           second sequence.
    * @param start2
    *           first index in s2 to compute the alignment from.
    * @param end2
    *           final index in s2 to compute the alignment from, exclusive. (This index may lie
    *           outside the array.)
    * @return
    */
   public SemiglobalAligner.SemiglobalAlignmentResult semiglobalAlign(final byte[] s1, final int start1, final int end1, final byte[] s2, final int start2, final int end2) {
      /*            s2 (column:1..n)
            --------------->
           |
        s1 | (row:1..m)
           |
           V
      */
      
      final int m = end1 - start1;
      final int n = end2 - start2;
      
      if (m == 0) {
         // s1 is empty
         byte[] alignment2 = Arrays.copyOfRange(s2, start2, end2);
         byte[] alignment1 = new byte[alignment2.length];
         Arrays.fill(alignment1, IAligner.GAP);
         return new SemiglobalAligner.SemiglobalAlignmentResult(alignment1, alignment2, alignment2.length, 0, 0);
      } else if (n == 0) {
         // s2 is empty
         byte[] alignment1 = Arrays.copyOfRange(s1, start1, end1);
         byte[] alignment2 = new byte[alignment1.length];
         Arrays.fill(alignment2, IAligner.GAP);
         return new SemiglobalAligner.SemiglobalAlignmentResult(alignment1, alignment2, alignment1.length, 0, 0);
      }
   
      // the DP table row x column
      IAligner.Entry[][] table = new IAligner.Entry[m + 1][n + 1];
   
      beginLocation.initMatrix(table);
      
      int row, column;
   
      // calculate alignment (using unit costs)
      Direction bt;
      int score;
      for (row = 1; row < table.length; ++row) {
         for (column = 1; column < table[0].length; ++column) {
            // look diagonal
            bt = Direction.DIAG;
            score = table[row - 1][column - 1].score
                  + ((s1[start1 + row - 1] == s2[start2 + column - 1]) ? IAligner.SCORE_MATCH
                        : IAligner.SCORE_MISMATCH);
            // look up
            int tmp = table[row - 1][column].score + IAligner.SCORE_DELETION;
            if (tmp > score) {
               bt = Direction.UP;
               score = tmp;
            }
            // look left
            tmp = table[row][column - 1].score + IAligner.SCORE_INSERTION;
            if (tmp > score) {
               bt = Direction.LEFT;
               score = tmp;
            }
            table[row][column].score = score;
            table[row][column].backtrack = bt;
         }
      }
   
      // find position where to start trace back
      final MatrixPosition bestPosition = endLocation.getEndPosition(table);

      final int bestRow = bestPosition.row;
      final int bestColumn = bestPosition.column;

      // both must be greater than 0, cause the case that one of the sequences has length 0 is
      // caught at the beginning
      assert bestRow > 0 && bestRow < table.length : String.format("bestRow: %s%n max is %s", bestRow, m);
      assert bestColumn > 0 && bestColumn < table[bestRow].length : String.format("bestColumn: %s%n max is %s", bestColumn, n);
   
      // now track back
      byte[] alignment1 = new byte[m + n];
      byte[] alignment2 = new byte[m + n];
   
      int p1 = 0;
      int p2 = 0;
   
      row = m;
      column = n;
   
      assert bestRow == table.length - 1 || bestColumn == table[0].length - 1;
   
      // first, walk from the lower right corner to the
      // position where we found the maximum score
      if (table.length - 1 == bestRow) { // we are in the last row
         while (column > bestColumn) {
            alignment1[p1++] = IAligner.GAP;
            alignment2[p2++] = s2[start2 + --column];
         }
      } else { // we are in the last column
         while (row > bestRow) {
            alignment1[p1++] = s1[start1 + --row];
            alignment2[p2++] = IAligner.GAP;
         }
      }
      int rlen = p1;
   
      int errors = 0;
   
      // now, the actual backtracking.
      // we build reverse sequences while backtracking and
      // reverse them afterwards.
      Direction direction;
      while (!beginLocation.isValid(row, column)) { // while not a valid starting position
         direction = table[row][column].backtrack;
         if (direction == Direction.DIAG) {
            if (s1[start1 + --row] != s2[start2 + --column])
               errors++;
            alignment1[p1++] = s1[start1 + row];
            alignment2[p2++] = s2[start2 + column];
         } else if (direction == Direction.LEFT) {
            errors++;
            alignment1[p1++] = IAligner.GAP;
            alignment2[p2++] = s2[start2 + --column];
         } else if (direction == Direction.UP) {
            alignment1[p1++] = s1[start1 + --row];
            alignment2[p2++] = IAligner.GAP;
            errors++;
         }
      }
   
      // compute the length of the actual alignment (ignoring ends)
      int length = p1 - rlen;
   
      int begin = row;
      if (column > row)
         begin = column;
   
      while (column > 0) {
         alignment1[p1++] = IAligner.GAP;
         alignment2[p2++] = s2[start2 + --column];
      }
      while (row > 0) {
         alignment1[p1++] = s1[start1 + --row];
         alignment2[p2++] = IAligner.GAP;
      }
      assert row == 0 && column == 0;
      assert table[bestRow][bestColumn].score == length - 2 * errors;
   
      // reverse result
      ArrayUtils.reverseArray(alignment1, p1);
      ArrayUtils.reverseArray(alignment2, p2);
      
      //cut unused fields in alignments
      alignment1 = Arrays.copyOf(alignment1, p1);
      alignment2 = Arrays.copyOf(alignment2, p2);
   
      return new SemiglobalAligner.SemiglobalAlignmentResult(alignment1, alignment2, begin, length, errors);
   }

   /**
    * Computes an end-gap free alignment. Also called free-shift alignment or semiglobal alignment.
    * 
    * @param s1
    * @param s2
    * @author Markus Kemmerling
    */
   public SemiglobalAligner.SemiglobalAlignmentResult semiglobalAlign(final byte[] s1, final byte[] s2) {
      return semiglobalAlign(s1, 0, s1.length, s2, 0, s2.length);
   }
   
   
}
