package verjinxer.sequenceanalysis.alignment;

import java.util.Arrays;

import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.sequenceanalysis.alignment.IAligner.Direction;
import verjinxer.sequenceanalysis.alignment.IAligner.MatrixPosition;

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
   private Scores scores = new Scores(); // initialization with default scores;
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
    * Sets the scores to use for insertion, deletion, mismatch and match.
    * 
    * @param scores
    *           Scores to use to build the alignment.
    */
   public void setScores(Scores scores) {
      this.scores = scores;
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
    * @param alphabet
    *           The alphabet used for the two sequences.
    * @return
    */
   public SemiglobalAligner.SemiglobalAlignmentResult semiglobalAlign(final byte[] s1, final int start1, final int end1, final byte[] s2, final int start2, final int end2, Alphabet alphabet) {
      /*            s2 (column:1..n)
            --------------->
           |
        s1 | (row:1..m)
           |
           V
      */
      
      final int m = end1 - start1;
      final int n = end2 - start2;
   
      // the DP table row x column
      IAligner.Entry[][] table = new IAligner.Entry[m + 1][n + 1];
   
      beginLocation.initMatrix(table, scores);
      
      int row, column;
   
      // calculate alignment (using unit costs)
      Direction bt;
      int score;
      for (row = 1; row < table.length; ++row) {
         for (column = 1; column < table[0].length; ++column) {
            // look diagonal
            bt = Direction.DIAG;
            // only symbols can match, two matching wildcards are treated as mismatch.
            score = table[row - 1][column - 1].score
                  + ((s1[start1 + row - 1] == s2[start2 + column - 1] && alphabet.isSymbol(s1[start1
                        + row - 1])) ? scores.getMatchScore() : scores.getMismatchScore());
            // look up
            int tmp = table[row - 1][column].score + scores.getDeletionScore();
            if (tmp > score) {
               bt = Direction.UP;
               score = tmp;
            }
            // look left
            tmp = table[row][column - 1].score + scores.getInsertionScore();
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

      // both must be greater or equal than 0, cause one of the sequences can have length 0
      assert bestRow >= 0 && bestRow < table.length : String.format("bestRow: %s%n max is %s", bestRow, m);
      assert bestColumn >= 0 && bestColumn < table[bestRow].length : String.format("bestColumn: %s%n max is %s", bestColumn, n);
   
      // now track back
      byte[] alignment1 = new byte[m + n];
      byte[] alignment2 = new byte[m + n];
   
      int p1 = alignment1.length-1;
      int p2 = alignment2.length-1;
   
      row = m;
      column = n;
   
      assert bestRow == table.length - 1 || bestColumn == table[0].length - 1;
   
      // first, walk from the lower right corner to the
      // position where we found the maximum score
      if (table.length - 1 == bestRow) { // we are in the last row
         while (column > bestColumn) {
            alignment1[p1--] = IAligner.GAP;
            alignment2[p2--] = s2[start2 + --column];
         }
      } else { // we are in the last column
         while (row > bestRow) {
            alignment1[p1--] = s1[start1 + --row];
            alignment2[p2--] = IAligner.GAP;
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
            --row;
            --column;
            if (s1[start1 + row] != s2[start2 + column] || !alphabet.isSymbol(s1[start1 + row])) {
               errors++;
            }
            alignment1[p1--] = s1[start1 + row];
            alignment2[p2--] = s2[start2 + column];
         } else if (direction == Direction.LEFT) {
            errors++;
            alignment1[p1--] = IAligner.GAP;
            alignment2[p2--] = s2[start2 + --column];
         } else if (direction == Direction.UP) {
            alignment1[p1--] = s1[start1 + --row];
            alignment2[p2--] = IAligner.GAP;
            errors++;
         }
      }
   
      // compute the length of the actual alignment (ignoring ends)
      int length = rlen - p1;
   
      IAligner.MatrixPosition beginPosition = new IAligner.MatrixPosition(row, column);
   
      while (column > 0) {
         alignment1[p1--] = IAligner.GAP;
         alignment2[p2--] = s2[start2 + --column];
      }
      while (row > 0) {
         alignment1[p1--] = s1[start1 + --row];
         alignment2[p2--] = IAligner.GAP;
      }
      assert row == 0 && column == 0;
      
      //cut unused fields in alignments
      alignment1 = Arrays.copyOfRange(alignment1, p1+1, alignment1.length);
      alignment2 = Arrays.copyOfRange(alignment2, p2+1, alignment2.length);
      
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
   public SemiglobalAligner.SemiglobalAlignmentResult semiglobalAlign(final byte[] s1,
         final byte[] s2, Alphabet alphabet) {
      return semiglobalAlign(s1, 0, s1.length, s2, 0, s2.length, alphabet);
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
