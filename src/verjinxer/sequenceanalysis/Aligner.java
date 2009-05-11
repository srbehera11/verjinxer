package verjinxer.sequenceanalysis;

/**
 * 
 * @author Marcel Martin
 * 
 */
public class Aligner {
   /** direction constants for backtrack table */
   private enum DIR {
      LEFT, UP, DIAG
   }

   private static final byte GAP = -1; // TODO

   /** DP table entry */
   private class Entry {
      public int score;
      public DIR backtrack;
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
      int i, j; // i:

      for (i = 0; i <= m; ++i) {
         columns[0][i].score = 0;
         columns[0][i].backtrack = DIR.UP; // TODO never read
      }
      for (j = 0; j <= n; ++j) {
         columns[j][0].score = 0;
         columns[j][0].backtrack = DIR.LEFT; // TODO never read
      }

      // calculate alignment (using unit costs)
      // outer loop goes over columns
      Entry[] cur_column;
      Entry[] prev_column = columns[0];
      for (j = 1; j <= n; ++j) {
         cur_column = columns[j];
         for (i = 1; i <= m; ++i) {
            DIR bt = DIR.DIAG;
            int score = prev_column[i - 1].score + ((s1[i - 1] == s2[j - 1]) ? 1 : -1);
            int tmp = cur_column[i - 1].score - 1;
            if (tmp > score) {
               bt = DIR.UP;
               score = tmp;
            }
            tmp = prev_column[i].score - 1;
            if (tmp > score) {
               bt = DIR.LEFT;
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
         DIR direction = columns[j][i].backtrack;
         if (direction == DIR.DIAG) {
            if (s1[--i] != s2[--j])
               errors++;
            alignment1[p1++] = s1[i];
            alignment2[p2++] = s2[j];
         } else if (direction == DIR.LEFT) {
            errors++;
            alignment1[p1++] = GAP;
            alignment2[p2++] = s2[--j];
         } else if (direction == DIR.UP) {
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
      reverseArray(alignment1, p1);
      reverseArray(alignment2, p2);
      // *p1 = '\0';
      // *p2 = '\0';

      // PyObject* o = Py_BuildValue("ssiii", alignment1, alignment2, begin, length, errors);
      // return o;
   }

   /** Reverses array[0..length-1] in place */
   private void reverseArray(byte[] array, int length) {
      for (int i = 0; i < length / 2; ++i) {
         byte tmp = array[i];
         array[i] = array[length - 1 - i];
         array[length - 1 - i] = tmp;
      }
   }

   // TODO remove static
   public static class AlignmentResult {
      int enddelta, bestpos;
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
   public final static  AlignmentResult align(final byte[] txt, final int start, final int len, final byte[] index,
         final int bstart, final int bend, final int giventol, final int[] storage, int asize) {
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
   final static AlignmentResult fullalign(final byte[] txt, final byte[] itext, final int start, final int len, final int bstart,
         final int bend, final int giventol, final int[] storage, final int asize) {
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
   
   /**
    * 
    * @param query
    * @param reference
    * @param qualities quality values of the query
    * @param knownGoodStart
    * @param knownGoodStop interval known to match exactly
    */
   public static void alignWithQualities(final byte[] query, final byte[] reference, final byte[] qualities, final int knownGoodStart, final int knowGoodStop) {
      // TODO
   }
}
