package verjinxer.sequenceanalysis;

import java.util.Arrays;

import verjinxer.util.ArrayUtils;

/**
 * 
 * @author Marcel Martin
 * 
 */
public class Aligner {
   /** direction constants for traceback table */
   private enum Direction {
      LEFT, UP, DIAG
   }

   private static class COSTS {
      public static int INDEL = 1;
      public static int MISMATCH = 1;
   }

   private static final byte GAP = -1; // TODO

   /** DP table entry */
   private class Entry {
      public int score;
      public Direction backtrack;
   }

   private static class CostEntry {
      public int cost;
      public Direction backtrack;
   }

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
   }

   /**
    * 
    * Uses a banded alignment.
    * 
    * @param s1
    * @param s2
    * @param e
    * @return
    */
   public static ForwardAlignmentResult forwardAlign(byte[] s1, byte[] s2, int e) {
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
      CostEntry[][] columns = new CostEntry[n + 1][2 * e + 1];

      for (int i = 0; i < columns.length; ++i) {
         for (int j = 0; j < columns[i].length; ++j) {
            columns[i][j] = new CostEntry();
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
      CostEntry[] cur_column;
      CostEntry[] prev_column = columns[0];
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
            cost += (c1 != c2) ? COSTS.MISMATCH : 0;
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
               int tmp = cur_column[d - 1].cost + COSTS.INDEL;
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
               int tmp = prev_column[d + 1].cost + COSTS.INDEL;

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

      // TODO actually, shouldn't we take the entry closest to the diagonal?

      int best = m + n + 1;
      int best_j = -1;
      for (j = Math.max(0, m - e); j < Math.min(m + e + 1, n + 1); ++j) {
         if (columns[j][m - j + e].cost < best) {
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
            alignment1[p1++] = GAP;
            alignment2[p2++] = s2[--j];
         } else if (direction == Direction.UP) {
            alignment1[p1++] = s1[--i];
            alignment2[p2++] = GAP;
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

      return new ForwardAlignmentResult(alignment1, alignment2, errors, best_j);
   }

   public static void printAlignment(ForwardAlignmentResult alignment, Alphabet alphabet) {

   }

   /**
    * Computes an end-gap free alignment. Also called free-shift alignment or semiglobal alignment.
    * 
    * @param s1
    * @param s2
    * @param m
    * @param n
    */
   void semiglobalAlign(byte[] s1, byte[] s2, int m, int n) {
      // byte[] s1;
      // byte[] s2;
      // int m, n;

      /*            s2 (n, j)
            --------------->
           |
        s1 | (m, i)
           |
           V
      */

      // the DP table is stored column-wise
      Entry[][] columns;
      columns = new Entry[n + 1][m + 1];

      int i, j;

      for (i = 0; i < columns.length; ++i) {
         for (j = 0; j < columns[i].length; ++j) {
            columns[i][j] = new Entry();
         }
      }

      for (i = 0; i <= m; ++i) {
         columns[0][i].score = 0;
         columns[0][i].backtrack = Direction.UP; // TODO never read
      }
      for (j = 0; j <= n; ++j) {
         columns[j][0].score = 0;
         columns[j][0].backtrack = Direction.LEFT; // TODO never read
      }

      // calculate alignment (using unit costs)
      // outer loop goes over columns
      Entry[] cur_column;
      Entry[] prev_column = columns[0];
      for (j = 1; j <= n; ++j) {
         cur_column = columns[j];
         for (i = 1; i <= m; ++i) {
            Direction bt = Direction.DIAG;
            int score = prev_column[i - 1].score + ((s1[i - 1] == s2[j - 1]) ? 1 : -1);
            int tmp = cur_column[i - 1].score - 1;
            if (tmp > score) {
               bt = Direction.UP;
               score = tmp;
            }
            tmp = prev_column[i].score - 1;
            if (tmp > score) {
               bt = Direction.LEFT;
               score = tmp;
            }
            cur_column[i].score = score;
            cur_column[i].backtrack = bt;
         }
         prev_column = cur_column;
      }

      // find position with highest score in last column or last row
      int best = -1, best_j = -1;
      for (j = 0; j <= n; ++j) {
         if (columns[j][m].score >= best) {
            best_j = j;
            best = columns[j][m].score;
         }
      }
      assert best_j != -1;
      int best_i = -1;
      best = -1;
      Entry[] last_column = columns[n];
      for (i = 0; i <= m; ++i) {
         if (last_column[i].score >= best) {
            best_i = i;
            best = last_column[i].score;
         }
      }
      assert best_i != -1;
      // last_row = [ (columns[j][m], j) for j in xrange(n+1) ]
      // row_best, best_j = max(last_row)

      if (columns[best_j][m].score > columns[n][best_i].score) {
         best_i = m;
         best = columns[best_j][m].score;
      } else {
         best_j = n;
         best = columns[n][best_i].score;
      }

      // now track back
      byte[] alignment1 = new byte[m + n + 4];
      byte[] alignment2 = new byte[m + n + 4];

      int p1 = 0;
      int p2 = 0;

      i = m;
      j = n;

      // first, walk from the lower right corner to the
      // position where we found the maximum score
      if (i == best_i) { // we are in the last row
         while (j > best_j) {
            alignment1[p1++] = GAP;
            alignment2[p2++] = s2[--j];
         }
      } else { // we are in the last column
         while (i > best_i) {
            alignment1[p1++] = s1[--i];
            alignment2[p2++] = GAP;
         }
      }
      int rlen = p1;

      int errors = 0;

      // now, the actual backtracking.
      // we build reverse sequences while backtracking and
      // reverse them afterwards.
      while (i > 0 && j > 0) { // columns[j][i] > 0
         // int score = columns[j][i].score;
         Direction direction = columns[j][i].backtrack;
         if (direction == Direction.DIAG) {
            if (s1[--i] != s2[--j])
               errors++;
            alignment1[p1++] = s1[i];
            alignment2[p2++] = s2[j];
         } else if (direction == Direction.LEFT) {
            errors++;
            alignment1[p1++] = GAP;
            alignment2[p2++] = s2[--j];
         } else if (direction == Direction.UP) {
            alignment1[p1++] = s1[--i];
            alignment1[p2++] = GAP;
            errors++;
         }
      }

      // compute the length of the actual alignment (ignoring ends)
      int length = p1 - rlen;

      int begin = i;
      if (j > i)
         begin = j;
      // continue from where we are to the upper left corner
      /*
      while (i > 0 && j > 0) {
         *p1++ = s1[--i];
         *p2++ = s2[--j];
      }
      */
      while (j > 0) {
         alignment1[p1++] = GAP;
         alignment2[p2++] = s2[--j];
      }
      while (i > 0) {
         alignment1[p1++] = s1[--i];
         alignment2[p2++] = GAP;
      }
      assert i == 0 && j == 0;

      // print "best score:", columns[best_j][best_i]
      // print "length:", length
      // print "errors:", errors
      assert columns[best_j][best_i].score == length - 2 * errors;

      // reverse result
      ArrayUtils.reverseArray(alignment1, p1);
      ArrayUtils.reverseArray(alignment2, p2);
      // *p1 = '\0';
      // *p2 = '\0';

      // PyObject* o = Py_BuildValue("ssiii", alignment1, alignment2, begin, length, errors);
      // return o;
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
      final int tol = giventol < len ? giventol : len;
      int bestpos = -1;
      int bestd = 2 * (len + 1); // as good as infinity
      final int as = asize;

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
