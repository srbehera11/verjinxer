package verjinxer.sequenceanalysis.alignment;

import java.util.Arrays;

import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.sequenceanalysis.alignment.IAligner.Direction;
import verjinxer.util.ArrayUtils;

/**
 * 
 * @author Marcel Martin
 * 
 */
public class Aligner {
   public static class ForwardAlignmentResult {
      private final byte[] sequence1, sequence2;

      private final int lengthOnReference;

      private final int errors;

      public ForwardAlignmentResult(byte[] sequence1, byte[] sequence2, int errors,
            int lengthOnReference) {
         this.sequence1 = sequence1;
         this.sequence2 = sequence2;
         this.errors = errors;
         this.lengthOnReference = lengthOnReference;
         // TODO assert lengthOnReference == sequence1.length && lengthOnReference ==
         // sequence2.length;
      }

      public byte[] getSequence1() {
         return sequence1;
      }

      public byte[] getSequence2() {
         return sequence2;
      }

      public int getLengthOnReference() {
         return lengthOnReference;
      }

      public int getErrors() {
         return errors;
      }
      
      /**
       * Prints the contrast of the two sequences considered as bytes.
       * 
       * @return A visualization of the alignment
       */
      public String printAsBytes() {
         StringBuilder s1 = new StringBuilder(lengthOnReference * 6 + 2);
         StringBuilder s2 = new StringBuilder(lengthOnReference * 2);
         StringBuilder m = new StringBuilder(lengthOnReference * 2 + 1);

         // lengthOnReference is related to the origin reference and not to the sequence stored
         // here where gaps may be inserted.
         // Therefore i counts the normal values in sequence2 and gapCounter the gaps.
         int gapCounter = 0;
         for (int i = 0; i < lengthOnReference;) { // i is incremented in last if statement
            if (sequence1[i + gapCounter] == IAligner.GAP) {
               s1.append(' ');
            } else {
               s1.append(sequence1[i + gapCounter]);
            }

            if (sequence1[i + gapCounter] == sequence2[i + gapCounter]) {
               m.append('|');
            } else {
               m.append('x');
            }

            if (sequence2[i + gapCounter] == IAligner.GAP) {
               s2.append(' ');
               gapCounter++;
            } else {
               s2.append(sequence2[i + gapCounter]);
               i++;
            }
         }

         s1.append('\n');
         m.append('\n');

         s1.append(m);
         s1.append(s2);

         return s1.toString();
      }
      
      /**
       * Prints the contrast of the two sequences considered as characters.
       * 
       * @return A visualization of the alignment
       */
      public String printAsChars() {
         StringBuilder s1 = new StringBuilder(lengthOnReference * 6 + 2);
         StringBuilder s2 = new StringBuilder(lengthOnReference * 2);
         StringBuilder m = new StringBuilder(lengthOnReference * 2 + 1);

         // lengthOnReference is related to the origin reference and not to the sequence stored
         // here where gaps may be inserted.
         // Therefore i counts the normal values in sequence2 and gapCounter the gaps.
         int gapCounter = 0;
         for (int i = 0; i < lengthOnReference;) { // i is incremented in last if statement
            if (sequence1[i + gapCounter] == IAligner.GAP) {
               s1.append(' ');
            } else {
               s1.append((char) sequence1[i + gapCounter]);
            }

            if (sequence1[i + gapCounter] == sequence2[i + gapCounter]) {
               m.append('|');
            } else {
               m.append('x');
            }

            if (sequence2[i + gapCounter] == IAligner.GAP) {
               s2.append(' ');
               gapCounter++;
            } else {
               s2.append((char) sequence2[i + gapCounter]);
               i++;
            }
         }

         s1.append('\n');
         m.append('\n');

         s1.append(m);
         s1.append(s2);

         return s1.toString();
      }
   }
   
   /**
    * Uses a banded alignment.
    * 
    * @param s1
    * @param s2
    * @param e
    * @return
    */
   public static ForwardAlignmentResult forwardAlign(byte[] s1, byte[] s2, int e) {
      return forwardAlign(s1, s2, e, false);
   }

   /**
    * 
    * Uses a banded alignment.
    * 
    * @param s1
    * @param s2
    * @param e
    * @param debug
    *           Whether to print the alignment table.
    * @return
    */
   public static ForwardAlignmentResult forwardAlign(byte[] s1, byte[] s2, int e, boolean debug) {
      /* 
                s2 (n, j)
            --------------->
           |
        s1 | (m, i)
           |
           V
      */

      int m = s1.length;
      int n = s2.length;
      assert e >= 0;

      if (n < m - e) {
         return null;
      }
      n = Math.min(n, m + e);

      // the following code makes sure that e is at most n.
      // if that is the case, then there are no savings
      // using a sparse DP table (in fact, there is some overhead), but
      // that happens so rarely in our application that it's currently not
      // worth optimizing for.

      // e_adjusted == 0 means that e wasn't adjusted.
      // Otherwise, it contains the old value of e.
      int e_adjusted = 0;
      if (e > m) {
         e_adjusted = e;
         e = m;
      }
      // assert n >= e;

      // Since the DP table is stored column-wise, the indices are exchanged: column first, then
      // row.
      // We use a banded alignment to save space. That is, only those 2*e+1 consecutive entries of
      // each column are stored
      // that are really necessary.
      // Where you would access DP[i][j] in a full DP table, you now have to access
      // columns[j][i-j+e].
      IAligner.CostEntry[][] columns = new IAligner.CostEntry[n + 1][2 * e + 1];

      for (int i = 0; i < columns.length; ++i) {
         for (int j = 0; j < columns[i].length; ++j) {
            columns[i][j] = new IAligner.CostEntry();
         }
      }

      int i, j, d;

      assert columns[0].length == 2 * e + 1; // TODO remove after porting

      // fill that part of the first column that is used
      for (d = e; d < columns[0].length; ++d) {
         columns[0][d].cost = d - e;
         columns[0][d].backtrack = Direction.UP;
      }

      // fill
      for (j = 0; j <= Math.min(e, n); ++j) {
         columns[j][e - j].cost = j;
         columns[j][e - j].backtrack = Direction.LEFT;
      }

      // calculate alignment
      // outer loop goes over columns
      IAligner.CostEntry[] cur_column;
      IAligner.CostEntry[] prev_column = columns[0];
      for (j = 1; j < n + 1; ++j) {
         cur_column = columns[j];

         for (d = Math.max(e - j + 1, 0); d < Math.min(2 * e, m - j + e) + 1; ++d) {
            Direction bt = Direction.DIAG;
            // cost function needs previous and next characters
            // byte p1 = 'x'; // TODO choose a better 'neutral' character
            // byte p2 = 'x';
            // byte n1 = 'x';
            // byte n2 = 'x';
            // if (d + j - e - 2 >= 0)
            // p1 = s1[d + j - e - 2];
            // if (j - 2 >= 0)
            // p2 = s2[j - 2];
            // if (d + j - e < m)
            // n1 = s1[d + j - e];
            // if (j < n)
            // n2 = s2[j];
            byte c1 = s1[d + j - e - 1];
            byte c2 = s2[j - 1];
            int cost = prev_column[d].cost;

            // switch (costtype) {
            // case REGULAR:
            cost += (c1 != c2) ? IAligner.COSTS.MISMATCH : 0;
            // break;
            // case BISULFITE_CT_CMC:
            // cost += cost_bisulfite_CT_cmc(c1, c2);
            // break;
            // case BISULFITE_GA_CMC:
            // cost += cost_bisulfite_GA_cmc(c1, c2);
            // break;
            // case BISULFITE_CT:
            // cost += cost_bisulfite_CT(c1, c2, n1, n2);
            // break;
            // case BISULFITE_GA:
            // cost += cost_bisulfite_GA(c1, c2, p1, p2);
            // break;
            // }

            // we can only have a deletion (UP in backtrack table) if we are not at the top
            if (d != 0) {
               int tmp = cur_column[d - 1].cost + IAligner.COSTS.INDEL;
               // avoid aligning "CG" to "C-G"
               // if (costtype > REGULAR && c2 == 'C' && c1 == 'G' && p1 == 'C' && n2 == 'G') {
               // tmp += 1;
               // }
               if (tmp < cost) {
                  bt = Direction.UP;
                  cost = tmp;
               }
            }
            // we can only have an insertion (LEFT in backtrack table) if we are not at the bottom
            if (d != 2 * e) {
               int tmp = prev_column[d + 1].cost + IAligner.COSTS.INDEL;

               // avoid aligning "C-G" to "CG"
               // if (costtype > REGULAR && c1 == 'C' && c2 == 'G' && p2 == 'C' && n1 == 'G') {
               // tmp += 1;
               // }
               if (tmp < cost) {
                  bt = Direction.LEFT;
                  cost = tmp;
               }
            }
            cur_column[d].cost = cost;
            cur_column[d].backtrack = bt;
         }
         // Abort if it's not possible anymore to find an alignment (reduces runtime by more than
         // 25%)
         /* TODO
         if min(cur_column) > e {
            free(columns);
            return None;
         }*/
         prev_column = cur_column;
      }

      // find position with lowest cost in last row
      assert m - e <= n;

      // TODO last_column!

      /*
       * The Alignment ends at the bottom edge. There, the rightmost entry is taken.
       * The base for the decision to take the rightmost entry is illustrated by the following example:
       * 
       * query: Hallo
       * reference: Hella
       * 
       * By taken the rightmost entry at the bottom, we get this alignment that we intuitive expected:
       * Hallo
       * |x||x
       * Hella
       * 
       * If we would take the leftmost entry at the bottom edge, we would get that less intuitive alignment:
       * Hallo
       * |x||x
       * Hell-
       */

      int best = m + n + 1;
      int best_j = -1;
      for (j = Math.max(0, m - e); j < Math.min(m + e + 1, n + 1); ++j) {
         if (columns[j][m - j + e].cost <= best) { // <= is necessary to take the rightmost entry 
            best_j = j;
            best = columns[j][m - j + e].cost;
         }
      }
      int errors = best;

      if (errors > e || (e_adjusted > 0 && errors > e_adjusted)) {
         return null;
      }

      // now track back (from "lower right" corner)
      // now track back
      byte[] alignment1 = new byte[m + n + 4]; // TODO
      byte[] alignment2 = new byte[m + n + 4]; // TODO

      int p1 = 0;
      int p2 = 0;

      i = m;
      j = best_j;

      d = i - j + e;

      // we build reverse sequences while backtracking and
      // reverse them afterwards.

      while (i > 0 || j > 0) {
         assert i >= 0 && j >= 0;
         assert 0 <= d && d <= 2 * e;
         Direction direction = columns[j][d].backtrack;
         if (direction == Direction.DIAG) {
            alignment1[p1++] = s1[--i];
            alignment2[p2++] = s2[--j];
         } else if (direction == Direction.LEFT) {
            alignment1[p1++] = IAligner.GAP;
            alignment2[p2++] = s2[--j];
         } else if (direction == Direction.UP) {
            alignment1[p1++] = s1[--i];
            alignment2[p2++] = IAligner.GAP;
         } else {
            assert false;
         }
         d = i - j + e;
      }

      assert i == 0 && j == 0 && d == e;
      // assert(len(r1) == len(r2));
      // assert error_check == errors

      // reverse result
      ArrayUtils.reverseArray(alignment1, p1);
      ArrayUtils.reverseArray(alignment2, p2);

      // return (r1, r2, begin, length, errors)
      // PyObject* o = Py_BuildValue("ssii", alignment1, alignment2, errors, best_j);

      if (debug) {
         printDebugForwardAligner(columns, e, m, n);
      }
      return new ForwardAlignmentResult(alignment1, alignment2, errors, best_j);
   }
   
   private static void printDebugForwardAligner(IAligner.CostEntry[][] table, final int e, final int m, final int n) {
   // Where you would access DP[i][j] in a full DP table, you now have to access
      // columns[j][i-j+e].
      
      for (int i = 0; i <= m; i++) {
         for (int j = 0; j <= n; j++) {
            if (i-j+e >= table[j].length) break;
            if (i - j + e >= 0) {
               IAligner.CostEntry entry = table[j][i - j + e];
               System.out.print(entry.cost + ":");
               if (entry.backtrack != null) {
                  switch (entry.backtrack) {
                  case DIAG:
                     System.out.print("D");
                     break;
                  case LEFT:
                     System.out.print("L");
                     break;
                  case UP:
                     System.out.print("U");
                     break;
                  }
               }
            }
            System.out.print("\t");
         }
         System.out.println();
      }
      
//      System.out.println();
//      
//      for (int i = 0; i < table.length; i++) {
//         for (IAligner.CostEntry entry : table[i]) {
//            System.out.print(entry.cost + ":");
//            if (entry.backtrack != null) {
//               switch (entry.backtrack) {
//               case DIAG: System.out.print("D"); break;
//               case LEFT: System.out.print("L"); break;
//               case UP: System.out.print("U"); break;
//               }
//            }
//            System.out.print("\t");
//         }
//         System.out.println();
//      }
   }

   public static void printAlignment(ForwardAlignmentResult alignment, Alphabet alphabet) {

   }

   // TODO perhaps remove static
   public static class AlignmentResult {
      private final int enddelta, bestpos;

      AlignmentResult(int enddelta, int bestpos) {
         this.enddelta = enddelta;
         this.bestpos = bestpos;
      }

      public int getEnddelta() {
         return enddelta;
      }

      public int getBestpos() {
         return bestpos;
      }
   }

   /**
    * align text with parallelogram block #b. Requires blocksize, blow, itext[], asize
    * 
    * @param txt
    *           the text array
    * @param start
    *           start position in the text
    * @param len
    *           length of the text (part) to align with a block
    * @param bstart
    *           where to start alignming in the index
    * @param bend
    *           where to end aligning in the index (exclusive)
    * @param giventol
    *           absolute error tolerance
    * @param storage
    *           work storage area, must have length >=len
    * @return position where leftmost best match ends in index, -1 if none.
    */
   public final static AlignmentResult align(final byte[] txt, final int start, final int len,
         final byte[] index, final int bstart, final int bend, final int giventol,
         final int[] storage, int asize) {
      // tries to find an appearance of txt in index.
      // Simplified, for each position in index a forward alignment is calculated with txt als query and
      // the rest of index as reference. Thereby the maximum error rate is the tol.
      // In other words, this method calculates an alignment from the top edge to the bottom edge.
      //       index
      //    +---------
      //  t |                The table is calculated column for column.
      //  x |                When calculated the i-th column, the results of column (i-1)
      //  t |                is stored in storage. When calculating position (j,i) and you want to look
      //    |                left, you must look in storage[j]. The calculated result of position (j,i) is
      //                     stored immediately in storage[j]. The old value is remembered in dul cause it 
      //                     is needed when calculating position (j+1,i) for looking diagonal.
      // 
      final int tol = giventol < len ? giventol : len;
      int bestpos = -1; // position in index where best alignment of txt exists
      int bestd = 2 * (len + 1); // as good as infinity - number of errors for best alignment
      final int as = asize; // assumption: each element of index is between 0 and as (exclusiv)

      for (int k = 0; k < tol; k++)
         storage[k] = k + 1;
      int lei = tol - 1; // initial last essential index
      // int newlei;
      int dup, dul, diag, dmin;
      byte tk;
      for (int c = bstart; c < bend; c++) {
         dup = dul = dmin = 0;
         // newlei = lei+1; if(newlei>=len) newlei=len-1;
         for (int k = 0; k <= lei; k++) {
            tk = txt[start + k];
            dmin = 1 + ((dup < storage[k]) ? dup : storage[k]); // left or up
            diag = dul + ((index[c] != tk || tk < 0 || tk >= as) ? 1 : 0);
            if (diag < dmin)
               dmin = diag;
            dul = storage[k];
            storage[k] = dup = dmin;
         }
         // lei+1 could go diagonally
         if (lei + 1 < len) {
            final int k = ++lei;
            tk = txt[start + k];
            dmin = 1 + dup;
            diag = dul + ((index[c] != tk || tk < 0 || tk >= as) ? 1 : 0);
            if (diag < dmin)
               dmin = diag;
            storage[k] = dup = dmin;
         }
         // dmin now contains storage[lei]
         if (dmin > tol) {
            while (lei >= 0 && storage[lei] > tol)
               lei--;
            dmin = (lei >= 0 ? storage[lei] : 0);
         } else {
            while (dmin < tol && lei < len - 1) {
               storage[++lei] = ++dmin;
            }
         }
         // lei=newlei;
         assert (lei == -1 && dmin == 0) || dmin == storage[lei];
         if (lei == len - 1 && dmin < bestd) {
            bestd = dmin;
            bestpos = c;
         }
      }
      return new AlignmentResult(bestd, bestpos);
   }

   /**
    * FULLY align text with parallelogram block #b. Requires blocksize, blow, itext[], asize
    * 
    * @param txt
    *           the text array
    * @param start
    *           start position in the text
    * @param len
    *           length of the text (part) to align with a block
    * @param bstart
    *           where to start alignming in the index
    * @param bend
    *           where to end aligning in the index (exclusive)
    * @param giventol
    *           absolute error tolerance
    * @param storage
    *           work storage area, must have length >=len
    * @return position where leftmost best match ends in index, -1 if none.
    */
   final static AlignmentResult fullalign(final byte[] txt, final byte[] itext, final int start,
         final int len, final int bstart, final int bend, final int giventol, final int[] storage,
         final int asize) {
      // final int tol = giventol<len? giventol : len;
      int bestpos = -1;
      int bestd = 2 * (len + 1); // as good as infinity
      final byte[] index = itext;
      final int as = asize;

      for (int k = 0; k < len; k++)
         storage[k] = k + 1;
      int dup, dul, diag, dmin;
      byte tk;
      for (int c = bstart; c < bend; c++) {
         dup = dul = dmin = 0;
         for (int k = 0; k < len; k++) {
            tk = txt[start + k];
            dmin = 1 + ((dup < storage[k]) ? dup : storage[k]); // left or up
            diag = dul + ((index[c] != tk || tk < 0 || tk >= as) ? 1 : 0);
            if (diag < dmin)
               dmin = diag;
            dul = storage[k];
            storage[k] = dup = dmin;
         }
         // dmin now contains storage[len-1]
         if (dmin < bestd) {
            bestd = dmin;
            bestpos = c;
         }
      }
      return new AlignmentResult(bestd, bestpos);
   }

   /** A query that has been aligned to a reference. */
   public static class AlignedQuery {
      private final int start, stop, errors;

      public AlignedQuery(int start, int stop, int errors) {
         this.start = start;
         this.stop = stop;
         this.errors = errors;
      }

      /** Start position of the query relative to the beginning of the reference */
      public int getStart() {
         return start;
      }

      /** Stop position of the query relative to the beginning of the reference */
      public int getStop() {
         return stop;
      }

      /** Number of errors of the alignment */
      public int getErrors() {
         return errors;
      }
   }

   /**
    * Aligns a query to a reference sequence, given an interval on query and reference that matches
    * exactly (seed). Starting from the exact match, the match is first extended to the right and
    * then to the left.
    * 
    * @param query
    *           The entire query sequence.
    * @param reference
    *           The entire reference sequence.
    * @param queryPosition
    *           Position of the exact match on the query sequence.
    * @param referencePosition
    *           Position of the exact match on the reference sequence.
    * @param length
    *           Length of the exact match.
    * @param maximumErrorRate
    *           Allow at most query.length * maximumErrorRate errors during the alignment.
    * @return If there were too many errors, null is returned. Otherwise, an AlignedQuery is
    *         returned.
    */
   public static AlignedQuery alignMatchToReference(final byte[] query, final byte[] reference,
         final int queryPosition, final int referencePosition, final int length,
         final double maximumErrorRate) {
      int refseqpos = referencePosition;

      int maximumNumberOfErrors = (int) (query.length * maximumErrorRate);

      byte[] endQuery = Arrays.copyOfRange(query, queryPosition + length, query.length);
      byte[] endReference = Arrays.copyOfRange(reference, refseqpos + length, reference.length);

      // if you want to convert a byte[] to a String for debugging, use alphabet.preimage(array)

      Aligner.ForwardAlignmentResult alignedEnd = Aligner.forwardAlign(endQuery, endReference,
            maximumNumberOfErrors);

      if (alignedEnd == null) {
         return null;
      } else {
         // System.out.println("alignedEnd: errors " + alignedEnd.getErrors() + ". LengthonRef " +
         // alignedEnd.getLengthOnReference());
         // try {
         // System.out.println("")
         // } catch (InvalidSymbolException e) {
         // e.printStackTrace();
         // }
      }

      byte[] reversedFrontQuery = Arrays.copyOf(query, queryPosition);
      ArrayUtils.reverseArray(reversedFrontQuery, -1);
      byte[] reversedFrontReference = Arrays.copyOf(reference, referencePosition);
      ArrayUtils.reverseArray(reversedFrontReference, -1);

      Aligner.ForwardAlignmentResult alignedFront = Aligner.forwardAlign(reversedFrontQuery,
            reversedFrontReference, maximumNumberOfErrors - alignedEnd.getErrors());

      if (alignedFront == null) {
         return null;
      }
      assert alignedFront.getErrors() + alignedEnd.getErrors() <= maximumNumberOfErrors;

      int start = referencePosition - alignedFront.getLengthOnReference();
      int stop = referencePosition + length + alignedEnd.getLengthOnReference();
      int errors = alignedFront.getErrors() + alignedEnd.getErrors();

      return new AlignedQuery(start, stop, errors);

      // TODO some string twiddling would be necessary here to compute the actual alignment
      // TODO if an actual alignment is not needed, we could use a faster version of forwardAlign
   }

   /**
    * 
    * @param query
    * @param reference
    * @param qualities
    *           quality values of the query
    * @param knownGoodStart
    * @param knownGoodStop
    *           interval known to match exactly
    */
   public static void alignWithQualities(final byte[] query, final byte[] reference,
         final byte[] qualities, final int knownGoodStart, final int knowGoodStop) {
      // TODO
   }
}
