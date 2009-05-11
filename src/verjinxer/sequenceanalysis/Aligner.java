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

}
