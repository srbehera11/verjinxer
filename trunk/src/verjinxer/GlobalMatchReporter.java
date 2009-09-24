package verjinxer;

import java.io.PrintWriter;
import java.util.ArrayList;

public class GlobalMatchReporter implements MatchReporter {
   /** list of unsorted matches */
   private final ArrayList<GlobalMatch> globalmatches;

   private final int minseqmatches;
   private final int maxseqmatches;
   private final PrintWriter out;
   
   /** sequence separator positions in indexed sequence s */
   private final long[] ssp;

   /** comparing text against itself? */
   final boolean selfcmp;

   private long seqstart;

   public GlobalMatchReporter(long[] ssp, int minseqmatches, int maxseqmatches, boolean selfcmp,
         PrintWriter out) {
      this.ssp = ssp;
      this.minseqmatches = minseqmatches;
      this.maxseqmatches = maxseqmatches;
      this.selfcmp = selfcmp;
      this.out = out;
      globalmatches = new ArrayList<GlobalMatch>(maxseqmatches < 127 ? maxseqmatches + 1 : 128);
   }

   /**
    * Reports a match by adding it to the matches or globalmatches list.
    * 
    * @param sstart
    *           start of match in s
    * @param tstart
    *           start of match in t
    * @param matchlength
    *           length of match
    */
   public void add(int sstart, final long tstart, int matchlength) {
      int i = seqindex(sstart);
      long ttt = tstart - seqstart;
      int sss = sstart - (i == 0 ? 0 : (int) ssp[i - 1] + 1);
      if (!selfcmp || sstart > tstart)
         globalmatches.add(new GlobalMatch(ttt, i, sss, matchlength));
   }

   protected int seqindex(final int p) {
      int si = java.util.Arrays.binarySearch(ssp, p);
      if (si >= 0)
         return si; // return the index of the ssp position
      return (-si - 1); // we are in a sequence, return the index of the following ssp position
   }

   public void clear() {
      globalmatches.clear();
   }

   /** write the list of matches in current target sequence against whole index */
   public void write(int seqnum) {
      if (globalmatches.size() == 0)
         return;
      if (globalmatches.size() < minseqmatches) {
         globalmatches.clear();
         return;
      }
      if (globalmatches.size() > maxseqmatches) {
         // log.debug("qmatch: Sequence %d has too many (>=%d/%d) matches, skipping output", seqnum,
         // globalmatches.size(), maxseqmatches);
         globalmatches.clear();
         return;
      }
      for (GlobalMatch gm : globalmatches) {
         out.printf("%d %d %d %d %d %d%n", seqnum, gm.tpos, gm.sseqnum, gm.spos, gm.len,
               (long) gm.spos - gm.tpos);
         // (sequence number, sequence position, index sequence number, index sequence position,
         // length, diagonal)
      }
      globalmatches.clear();
   }

   /** simple structure for unsorted (global) matches */
   private class GlobalMatch {
      final long tpos;
      final int sseqnum;
      final int spos;
      final int len;

      public GlobalMatch(final long tpos, final int sseqnum, final int spos, final int len) {
         this.tpos = tpos;
         this.sseqnum = sseqnum;
         this.spos = spos;
         this.len = len;
      }
   }

   @Override
   public void setSequenceStart(long seqstart) {
      this.seqstart = seqstart;
   }
}
