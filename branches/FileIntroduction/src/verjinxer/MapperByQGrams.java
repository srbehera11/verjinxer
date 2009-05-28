package verjinxer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.sequenceanalysis.QGramCoder;
import verjinxer.sequenceanalysis.QGramFilter;
import verjinxer.util.BitArray;
import verjinxer.util.TicToc;

import com.spinn3r.log5j.Logger;

public class MapperByQGrams {
   
   private static final Logger log = Globals.getLogger();

   
   /** current index name */
   // private final String iname = null;
   
   /** current index project data */
   // private final ProjectInfo iproject;
   
   /** current index text length */
   // private final int ilength;
   
   /** current index text */
   private final byte[] itext;
  
   /**length of longest [q]pos*/
   final long longestpos;
   
   final long longestindexlen;
   
   // private final int enddelta = -1;

   /** current index name */
//   private String iname = null;
   private final byte[] rcj;
   private final int[] Dcol;
   /**sequence separator positions in text t*/
   private final long[] tssp;

   private final ArrayList<String> indices;
   
   /** current qbck array */
//   private final int[] iqbck;

   /** current qpos array */
   private final int[] iqpos;

   /** current qgram filter */
//   private QGramFilter ifilter = null;
//   private byte[] qgram = null;

   /** E-value table */
//   private double[] etable = null;


   private final long longestsequence;

   private final Alphabet alphabet;

   private final ArrayList<String> tdesc;
   
   /** number of sequences */
   private final int tm;

   private final Globals g;

   public MapperByQGrams(Globals g, long longestsequence, Alphabet alphabet, ArrayList<String> tdesc, long[] tssp, int tm, ArrayList<String> indices, long longestpos, long longestindexlen) {
      this.longestsequence = longestsequence;
      this.longestpos = longestpos;
      this.longestindexlen = longestindexlen;
      rcj = new byte[(int) longestsequence + 1]; // +1 for separator
      Dcol = new int[(int) longestsequence + 1]; // +1 for separator
      this.alphabet = alphabet;
      this.tdesc = tdesc;
      this.tm = tm;
      this.g = g;
      this.tssp = tssp;
      this.indices = indices;
      iqpos = new int[(int) longestpos];
      itext = new byte[(int) longestindexlen];
   }

   /**
    * Proceed index-by-index, then sequence-by-sequence. Count the number of common q-grams of each
    * sequence and each index block.
    */
   public void mapByQGram(final int blocksize) {
      int oldq = -1;

      int inum = indices.size();

//      final int blow = (int) Math.floor(-(longestsequence + 1.0) / blocksize);
//      assert blow < 0;
//      final int bhi = (int) Math.floor((double) longestindexlen / blocksize);
//      assert bhi >= 0;
//      seqbesterror = new int[tm]; // keep track of best hits' error
//      seqbesthits = new int[tm]; // number of hits with best error
//      seqallhits = new int[tm]; // number of all hits
//      Arrays.fill(seqbesterror, (int) longestsequence + 2);
//      log.info("map: allocating %d parallelogram counters, width=%d", bhi - blow + 1, blocksize);
//      final int[] bcounter = new int[bhi - blow + 1];
//      QGramCoder coder = null;
//
//      for (int idx = 0; idx < inum; idx++) {
//         // load idx-th q-gram index
//         String iname = indices.get(idx);
//         try {
//            iproject = ProjectInfo.createFromFile(iname);
//         } catch (IOException ex) {
//            log.error("could not read project file: %s", ex);
//            g.terminate(1);
//         }
//         int iq = iproject.getIntProperty("q");
//         log.info("map: processing index '%s', q=%d, filter=%s...", iname, iq, filterstring);
//         int as = iproject.getIntProperty("qAlphabetSize");
//         assert asize == as;
//         if (iq != oldq) {
//            coder = new QGramCoder(iq, asize);
//            qgram = new byte[iq];
//            etable = computeEValues(longestsequence, iq, totalindexlen, blocksize,
//                  1 / (asize - 1.0));
//            oldq = iq;
//         }
//         iqbck = g.slurpIntArray(iname + FileNameExtensions.qbuckets, iqbck); // overwrite if
//         // possible
//         iqpos = g.slurpIntArray(iname + FileNameExtensions.qpositions, iqpos); // overwrite
//         itext = g.slurpByteArray(iname + FileNameExtensions.seq, 0, -1, itext); // overwrite
//         ilength = iproject.getIntProperty("Length");
//         ifilter = new QGramFilter(coder.q, coder.asize, filterstring);
//
//         for (int j = 0; j < tm; j++) {
//            if (tselect.get(j) == 0 || trepeat.get(j) == 1)
//               continue; // skip if not selected, or if repeat
//            int jstart = (j == 0 ? 0 : (int) (tssp[j - 1] + 1)); // first character
//            int jstop = (int) (tssp[j] + 1); // one behind last character = [separatorpos. +1]
//            final int jlength = jstop - jstart; // length including separator
//            processQGramBlocks(idx, coder, tall, jstart, jlength, j, 1, clip, bcounter, blocksize,
//                  blow);
//            if (revcomp && trepeat.get(j) == 0) {
//               ArrayUtils.revcompArray(tall, jstart, jstop - 1, (byte) 4, rcj);
//               rcj[jlength - 1] = -1;
//               // log.info("+: %s",Strings.join("",tall,jstart,jlength));
//               // log.info("-: %s",Strings.join("",rcj,0,jlength));
//               processQGramBlocks(idx, coder, rcj, 0, jlength, j, -1, clip, bcounter, blocksize,
//                     blow);
//            }
//            if (seqallhits[j] > 0)
//               tmapped.set(j, true);
//         } // end for j
//         allout.flush();
//         g.dumpIntArray(String.format("%s%s.%d.mapbesterror", g.outdir, tname, idx), seqbesterror);
//         g.dumpIntArray(String.format("%s%s.%d.mapbesthits", g.outdir, tname, idx), seqbesthits);
//         g.dumpIntArray(String.format("%s%s.%d.mapallhits", g.outdir, tname, idx), seqallhits);
//      } // end for i
//      // at the very end, de-select all mapped sequences and all repeats!
//      for (int j = 0; j < tm; j++)
//         if (tmapped.get(j) == 1 || trepeat.get(j) == 1)
//            tselect.set(j, false);
//
//   }
//
//   /**
//    * compute the q-gram block counts for txt[jstart..jstart+jlength-1].
//    */
//   final void processQGramBlocks(final int currenti, final QGramCoder coder, final byte[] txt,
//         final int jstart, final int jlength, final int currentj, final int currentdir,
//         final int trim, final int[] bcounter, final int blocksize, final int blow) {
//
//      final int q = coder.q;
//      int qcode = -1;
//      int success;
//      final int maxqgrams = jlength - 1 - q + 1 - 2 * trim;
//      final int overlap = (int) (Math.ceil(jlength * errorlevel));
//      int subtract = (int) (Math.ceil(jlength * errorlevel * q));
//      if (q * seqbesterror[currentj] < subtract)
//         subtract = q * seqbesterror[currentj];
//      final int bthresh = maxqgrams - subtract;
//      // 1. block counters are zero at this point.
//      // 2. iterate through all q-grams of txt[0..jlength-1], increase block counters, count
//      // high-scoring blocks
//      final int idelta = -jstart - q + 1;
//      final int jstop = jstart + jlength - 1 - trim; // points to behind last character to examine
//      for (int i = jstart + trim; i < jstop; i++) {
//         if (qcode >= 0) // attempt simple update
//         {
//            qcode = coder.codeUpdate(qcode, txt[i]);
//            if (qcode >= 0 && !ifilter.isFiltered(qcode))
//               incrementBCounters(i + idelta, qcode, overlap, bcounter, blocksize, blow);
//            continue;
//         }
//         // No previous qcode available, scan ahead for at least q new bytes
//         for (success = 0; success < q && i < jstop; i++) {
//            final byte next = txt[i];
//            if (next < 0 || next >= asize)
//               success = 0;
//            else
//               qgram[success++] = next;
//         }
//         i--; // already incremented i beyond read position
//         if (success == q) {
//            qcode = coder.code(qgram);
//            assert qcode >= 0;
//            if (!ifilter.isFiltered(qcode))
//               incrementBCounters(i + idelta, qcode, overlap, bcounter, blocksize, blow);
//         }
//      } // end for i
//
//      // 3. check high-scoring blocks in processQGramBlocks()
//      int blockhits = 0, currenthits = 0;
//      for (int b = 0; b < bcounter.length; b++)
//         if (bcounter[b] >= bthresh)
//            blockhits++;
//      if (blockhits >= repeatthreshold) { // many block hits, this IS a repeat, probably
//         log.info("map: sequence %d hits %d blocks, probably a repeat", currentj, blockhits);
//         trepeat.set(currentj, true);
//      } else { // this IS NOT a repeat, probably
//         for (int b = 0; b < bcounter.length; b++) {
//            if (bcounter[b] >= bthresh) {
//               final int len = jlength - 1 - 2 * trim;
//               final int bstart = (b + blow > 0) ? (b + blow) * blocksize : 0;
//               int bend = bstart + blocksize + jlength + overlap + 1;
//               if (bend > ilength)
//                  bend = ilength;
//               Aligner.AlignmentResult ar = Aligner.align(txt, jstart + trim, len, itext, bstart,
//                     bend, overlap, Dcol, asize);
//               int endpos, enddelta;
//               endpos = ar.getBestpos();
//               enddelta = ar.getEnddelta();
//               if (endpos >= 0) {
//                  endpos += trim;
//                  seqallhits[currentj]++;
//                  currenthits++;
//                  if (enddelta < seqbesterror[currentj]) {
//                     seqbesterror[currentj] = enddelta;
//                     seqbesthits[currentj] = 1;
//                  } else if (enddelta == seqbesterror[currentj]) {
//                     seqbesthits[currentj]++;
//                  }
//                  // log.info("%d%+d: [%s] %d %d", currentj, currentdir,
//                  // Strings.join("",itext,endpos+1-enddelta-jlength+1, jlength-1+enddelta), endpos,
//                  // enddelta);
//                  // log.info("%d%+d: [%s] %d %d", currentj, currentdir,
//                  // Strings.join("",itext,endpos+1-jlength+1, jlength-1), endpos, enddelta);
//                  // log.info(" > [%s]", Strings.join("",txt,jstart,jlength));
//                  allout.printf("%d %d  %d %d %d   %d %d %d%n", currentj, currentdir, currenti,
//                        endpos, enddelta, bthresh, bcounter[b], maxqgrams);
//               }
//            }
//         }
//      } // end else (this is not a repeat)
//      Arrays.fill(bcounter, 0);
//      if (blockhits > 0)
//         log.info(
//               " seq %d%c: %d/%d hits/blocks this time; besterror=%d w. %d best (%d total) hits",
//               currentj, currentdir > 0 ? '+' : '-', currenthits, blockhits,
//               seqbesterror[currentj], seqbesthits[currentj], seqallhits[currentj]);
//   }
//
//   final void incrementBCounters(final int p, final int qcode, final int overlap,
//         final int[] bcounter, final int blocksize, final int blow) {
//      final int bckend = iqbck[qcode + 1];
//      final int[] pos = iqpos;
//      int d, b;
//      for (int r = iqbck[qcode]; r < bckend; r++) {
//         d = pos[r] - p;
//         b = d / blocksize - blow;
//         ++bcounter[b];
//         if ((d % blocksize < overlap) && (b > 0))
//            ++bcounter[b - 1];
//      }
   }

}
