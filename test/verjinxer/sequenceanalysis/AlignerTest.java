package verjinxer.sequenceanalysis;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import verjinxer.sequenceanalysis.alignment.Aligner;
import verjinxer.sequenceanalysis.alignment.BottomEdgeLeftmost;
import verjinxer.sequenceanalysis.alignment.Scores;
import verjinxer.sequenceanalysis.alignment.SemiglobalAligner;
import verjinxer.sequenceanalysis.alignment.TopEdge;
import verjinxer.sequenceanalysis.alignment.Aligner.AlignmentResult;
import verjinxer.sequenceanalysis.alignment.SemiglobalAligner.SemiglobalAlignmentResult;

/**
 * This test cases are to verify that the old align() method of Aligner may be replaced by the
 * variable aligner.
 * 
 * @author kemmer
 */
public class AlignerTest {

   @Test
   public void testAlign1() {
      byte[] txt = { 1, 2, 2, 2 };
      int start = 0;
      int len = txt.length;
      byte[] index = { 0, 1, 2, 2, 1, 2, 2 };
      int bstart = 0;
      int bend = index.length;
      int giventol = 2;
      int[] storage = new int[txt.length];
      int asize = 4;

      AlignmentResult refResult = align(txt, start, len, index, bstart, bend, giventol, storage,
            asize);

      final int pos = refResult.getBestpos(); // pos in index
      final int delta = refResult.getEnddelta(); // errors

      SemiglobalAligner aligner = new SemiglobalAligner();
      aligner.setBeginLocations(new TopEdge());
      aligner.setEndLocations(new BottomEdgeLeftmost());
      aligner.setScores(new Scores(-1, -1, 0, -1));

      SemiglobalAlignmentResult result = aligner.semiglobalAlign(txt, start, len, index, bstart,
            bend);

      assertEquals(pos, bstart + result.getEndPosition().column - 1);
      assertEquals(delta, result.getErrors());
   }

   @Test
   public void testAlign2() {
      byte[] txt = new byte[12];
      int start = 0;
      int len = txt.length;
      byte[] index = new byte[34];
      int bstart = 0;
      int bend = index.length;
      int giventol = 2;
      int[] storage = new int[txt.length];
      int asize = 4;

      final int seed = 0;
      Random rand = new Random(seed);

      SemiglobalAligner aligner = new SemiglobalAligner();
      aligner.setBeginLocations(new TopEdge());
      aligner.setEndLocations(new BottomEdgeLeftmost());
      aligner.setScores(new Scores(-1, -1, 0, -1));

      for (int i = 0; i < 100; i++) {
//         System.out.println("i: " + i);
         for (int j = 0; j < txt.length; j++) {
            txt[j] = (byte) rand.nextInt(asize);
         }
         for (int j = 0; j < index.length; j++) {
            index[j] = (byte) rand.nextInt(asize);
         }
         start = rand.nextInt(txt.length / 2);
         len = rand.nextInt(txt.length - start);
         bstart = rand.nextInt(index.length / 2);
         bend = Math.min(index.length / 2 + 1 + rand.nextInt(index.length / 2), index.length);
         giventol = 2 + rand.nextInt(txt.length / 2);

         AlignmentResult refResult = align(txt, start, len, index, bstart, bend, giventol, storage,
               asize);

         final int pos = refResult.getBestpos(); // pos in index
         final int delta = refResult.getEnddelta(); // errors

         SemiglobalAlignmentResult result = aligner.semiglobalAlign(txt, start, start + len, index,
               bstart, bend);

         if (pos != -1) {
//            System.out.println("txt: " + Arrays.toString(txt));
//            System.out.println("start: " + start);
//            System.out.println("len: " + len);
//            System.out.println("index: " + Arrays.toString(index));
//            System.out.println("bstart: " + bstart);
//            System.out.println("bend: " + bend);
            if (result.getEndPosition().column > 0) {
               assertEquals(pos, bstart + result.getEndPosition().column - 1);
            } else {
               assertEquals(pos, bstart + result.getEndPosition().column);
            }
            assertEquals(delta, result.getErrors());
            assertTrue(result.getErrors() <= giventol);
         } else {
            assertTrue(result.getErrors() > giventol);
         }
      }
   }

   /**************************** Reference Method *******************************/
   /**************************** cp from Aligner  *******************************/

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
      // Simplified, for each position in index a forward alignment is calculated with txt als query
      // and
      // the rest of index as reference. Thereby the maximum error rate is the tol.
      // In other words, this method calculates an alignment from the top edge to the bottom edge.
      // index
      // +---------
      // t | The table is calculated column for column.
      // x | When calculated the i-th column, the results of column (i-1)
      // t | is stored in storage. When calculating position (j,i) and you want to look
      // | left, you must look in storage[j]. The calculated result of position (j,i) is
      // stored immediately in storage[j]. The old value is remembered in dul cause it
      // is needed when calculating position (j+1,i) for looking diagonal.
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

}
