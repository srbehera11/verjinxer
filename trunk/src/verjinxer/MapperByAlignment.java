package verjinxer;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

import com.spinn3r.log5j.Logger;

import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.sequenceanalysis.alignment.Scores;
import verjinxer.sequenceanalysis.alignment.Aligner;
import verjinxer.sequenceanalysis.alignment.AlignmentResult;
import verjinxer.sequenceanalysis.alignment.beginlocations.TopEdge;
import verjinxer.sequenceanalysis.alignment.endlocations.BottomEdgeLeftmost;
import verjinxer.util.ArrayUtils;
import verjinxer.util.BitArray;
import verjinxer.util.FileTypes;

/** Maps reads. */
public class MapperByAlignment {
   
   private static final Logger log = Globals.getLogger();

   /** keep track of best hits' error */
   private int[] seqbesterror;
   
   /** number of hits with best error */
   private int[] seqbesthits;
   
   /** number of all hits */
   private int[] seqallhits;
   
   private final byte[] rcj;
   
   /** number of sequences */
   private final int tm;

   private final byte[] tall; // the whole text

   private final PrintWriter allout;

   // private long tn = 0; // length of t

   private final ArrayList<String> indices;

   private final long longestsequence;

   final BitArray tselect;
   
   final BitArray trepeat;
   
   final BitArray tmapped;
   
   final Project tProject;
   
   private Alphabet alphabet;
   
   /** sequence separator positions in text t */
   private final long[] tssp;

   private Globals g;

   public MapperByAlignment(Globals g, long longestsequence, int tm, Alphabet alphabet, PrintWriter allout, BitArray tselect,
         BitArray trepeat, BitArray tmapped, byte[] tall, long[] tssp, Project tProject, ArrayList<String> indices) {

      this.g = g;
      this.longestsequence = longestsequence;
      rcj = new byte[(int) longestsequence + 1]; // +1 for separator
      this.tm = tm;
      seqbesterror = new int[tm]; // keep track of best hits' error
      seqbesthits = new int[tm]; // number of hits with best error
      seqallhits = new int[tm]; // number of all hits
      this.alphabet = alphabet;
      this.allout = allout;
      this.tselect = tselect;
      this.trepeat = trepeat;
      this.tmapped = tmapped;
      this.tall = tall;
      this.tssp = tssp;
      this.tProject = tProject;
      this.indices = indices;
   }

   public final void mapByAlignmentAtOnce(final int trim, final double errorlevel,
         final boolean revcomp) {
      // String iname = null;

      int inum = indices.size();
      // itext = new byte[(int)longestindexlen];

      Arrays.fill(seqbesterror, (int) longestsequence + 2);

      File[] iname = new File[inum];
      Project[] iprj = new Project[inum];
      byte[][] itext = new byte[inum][];
      int[] ilength = new int[inum];

      for (int idx = 0; idx < inum; idx++) {
         // load idx-th q-gram index
         iname[idx] = new File(indices.get(idx));
         try {
            iprj[idx] = Project.createFromFile(iname[idx]);
         } catch (IOException ex) {
            log.error("could not read project file %s: %s", iname[idx], ex);
            g.terminate(1);
         }
         log.info("map: processing index '%s', reading .seq", iname[idx]);
         int as = iprj[idx].getIntProperty("LargestSymbol") + 1;
         assert alphabet.largestSymbol() == as;
         itext[idx] = g.slurpByteArray(new File(iname[idx].getAbsolutePath() + FileTypes.SEQ), 0, -1, null); // overwrite
         ilength[idx] = iprj[idx].getIntProperty("Length");
         assert itext[idx].length == ilength[idx];
      }
      for (int j = 0; j < tm; j++) {
         if (tselect.get(j) == 0 || trepeat.get(j) == 1)
            continue; // skip if not selected, or if repeat
         final int jstart = (j == 0 ? 0 : (int) (tssp[j - 1] + 1)); // first character
         final int jstop = (int) (tssp[j] + 1); // one behind last character = [separatorpos. +1]
         final int jlength = jstop - jstart; // length including separator
         for (int idx = 0; idx < inum; idx++) {
            log.info("  aligning seq %d against idx %d %s", j, idx, iname[idx]);
            doTheAlignment(idx, itext[idx], tall, jstart, jlength - 1, j, 1, trim, errorlevel);
            if (revcomp && trepeat.get(j) == 0) {
               ArrayUtils.revcompArray(tall, jstart, jstop - 1, (byte) 4, rcj);
               rcj[jlength - 1] = -1;
               doTheAlignment(idx, itext[idx], rcj, 0, jlength - 1, j, -1, trim, errorlevel);
            }
         }
         if (seqallhits[j] > 0)
            tmapped.set(j, true);
         if (tmapped.get(j) == 1 || trepeat.get(j) == 1)
            tselect.set(j, false);
      } // end for j
      g.dumpIntArray(tProject.makeFile(FileTypes.MAPBESTERROR), seqbesterror);
      g.dumpIntArray(tProject.makeFile(FileTypes.MAPBESTHITS), seqbesthits);
      g.dumpIntArray(tProject.makeFile(FileTypes.MAPALLHITS), seqallhits);
   }

   private final int doTheAlignment(final int currenti, final byte[] itext, final byte[] txt,
         final int jstart, final int jlength, final int currentj, final int currentdir,
         final int trim, final double errorlevel) {
      final int len = jlength - 2 * trim;
      final int tol = (int) Math.ceil(jlength * errorlevel);
      log.debug(" tol=%d", tol);
      
      Aligner aligner = new Aligner();
      aligner.setBeginLocations(new TopEdge());
      aligner.setEndLocations(new BottomEdgeLeftmost());
      aligner.setScores(new Scores(-1, -1, 0, -1));

      assert alphabet.isEndOfLine(itext[itext.length-1]);
      //because of 'end of line' value do not consider last value of itext for alignment
      AlignmentResult result = aligner.align(txt, jstart + trim, jstart + trim
            + len, itext, 0, itext.length-1, alphabet);

      int endpos, enddelta;
      // if (result.getErrors() <= tol) { //TODO use tol and handle else case
      if (result.getEndPosition().column > 0) {
         endpos = result.getEndPosition().column - 1;
      } else {
         endpos = result.getEndPosition().column;
      }
      enddelta = result.getErrors();
      // }

      if (endpos >= 0) {
         endpos += trim;
         seqallhits[currentj]++;
         if (enddelta < seqbesterror[currentj]) {
            seqbesterror[currentj] = enddelta;
            seqbesthits[currentj] = 1;
         } else if (enddelta == seqbesterror[currentj]) {
            seqbesthits[currentj]++;
         }
         allout.printf("%d %d  %d %d %d%n", currentj, currentdir, currenti, endpos, enddelta);
      }
      return endpos;
   }
}
