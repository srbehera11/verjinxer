package verjinxer.sequenceanalysis.alignment;

import java.util.Arrays;

import verjinxer.sequenceanalysis.alignment.IAligner.Direction;
import verjinxer.sequenceanalysis.alignment.IAligner.MatrixPosition;
import verjinxer.util.ArrayUtils;

/**
 * This is a flexible Aligner. Depending on the begin and end locations ( {@link #beginLocation},
 * {@link #endLocation}), different kinds of alignment are calculated.
 * 
 * @author Markus Kemmerling
 */
public class SemiglobalAligner {

   
   public static class SemiglobalAlignmentResult {
      private byte[] sequence1, sequence2;

      private int length, errors;
      private IAligner.MatrixPosition begin, end;

      public SemiglobalAlignmentResult() {
         // all attributes are initialized by default
      }

      public SemiglobalAlignmentResult(byte[] sequence1, byte[] sequence2, MatrixPosition begin,
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

      public int getBegin() {
         return Math.max(begin.row, begin.column); // TODO ?
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
            if (b == IAligner.GAP) {
               sb.append(' ');
            } else {
               sb.append(b);
            }
         }

         sb.append('\n');

         for (int i = 0; i < sequence1.length; i++) {
            if (i >= getBegin() && i < getBegin() + length) {
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

      /**
       * Prints the contrast of the two sequences considered as characters.
       * 
       * @return A visualization of the alignment
       */
      public String printAsChars() {
         StringBuilder sb = new StringBuilder(sequence1.length * 3 + 4);
         for (byte b : sequence1) {
            if (b == IAligner.GAP) {
               sb.append(' ');
            } else {
               sb.append((char) b);
            }
         }

         sb.append('\n');

         for (int i = 0; i < sequence1.length; i++) {
            if (i >= getBegin() && i < getBegin() + length) {
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
               sb.append((char) b);
            }
         }

         return sb.toString();
      }
   }

   private BeginLocations beginLocation;
   private EndLocations endLocation;
   private boolean debug = false; 
   
   /**
    * @param beginLocation
    *           Where to start in the table with the alignment.
    */
   public void setBeginLocations(BeginLocations beginLocation) {
      this.beginLocation = beginLocation;
   }

   /**
    * @param endLocation
    *           Where to end in the table with the alignment.
    */
   public void setEndLocations(EndLocations endLocation) {
      this.endLocation = endLocation;
   }

   /**
    * Invoke this method before calculating the alignment to print the alignment table.
    */
   public void debug() {
      debug = true;
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
         // s1 is empty //TODO number errors depend on strategies
         byte[] alignment2 = Arrays.copyOfRange(s2, start2, end2);
         byte[] alignment1 = new byte[alignment2.length];
         Arrays.fill(alignment1, IAligner.GAP);
         return new SemiglobalAligner.SemiglobalAlignmentResult(alignment1, alignment2,
               new IAligner.MatrixPosition(0, alignment2.length), new IAligner.MatrixPosition(0,
                     alignment2.length), 0, 0);
      } else if (n == 0) {
         // s2 is empty //TODO number errors depend on strategies
         byte[] alignment1 = Arrays.copyOfRange(s1, start1, end1);
         byte[] alignment2 = new byte[alignment1.length];
         Arrays.fill(alignment2, IAligner.GAP);
         return new SemiglobalAligner.SemiglobalAlignmentResult(alignment1, alignment2,
               new IAligner.MatrixPosition(alignment1.length, 0), new IAligner.MatrixPosition(
                     alignment1.length, 0), 0, 0);
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
      final IAligner.MatrixPosition endPosition = endLocation.getEndPosition(table);

      final int bestRow = endPosition.row;
      final int bestColumn = endPosition.column;

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
   
      IAligner.MatrixPosition beginPosition = new IAligner.MatrixPosition(row, column);
   
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
      
      if (debug) {
         printDebug(table, null);
      }
   
      return new SemiglobalAligner.SemiglobalAlignmentResult(alignment1, alignment2, beginPosition,
            endPosition, length, errors);
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
   
   private static void printDebug(IAligner.Entry[][] table, SemiglobalAligner.SemiglobalAlignmentResult result) {
      for (int i = 0; i < table.length; i++) {
         for(IAligner.Entry entry: table[i]) {
            System.out.print(entry.score + ":");
            if (entry.backtrack != null) {
               switch (entry.backtrack) {
               case DIAG: System.out.print("D"); break;
               case LEFT: System.out.print("L"); break;
               case UP: System.out.print("U"); break;
               }
            }
            System.out.print("\t");
         }
         System.out.println();
      }
   }
}
