package verjinxer;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Collect matches and write them to a file.
 * 
 * @author Marcel Martin
 * 
 */
interface MatchReporter {
   /** Writes all collected matches. */
   public void write(int seqnum);

   /** Appends a match to the list of matches. */
   public void add(int sstart, final long tstart, int matchlength);

   /**
    * Matches are reported relative to this position within the query sequence t. Call this every
    * time you advance to the next query sequence.
    */
   public void setSequenceStart(long seqstart);

   /**
    * Clears the match list.
    */
   public void clear();
}

class SortedMatchReporter implements MatchReporter {
   /** list of sorted matches */
   private final ArrayList<ArrayList<Match>> matches;
   private final PrintWriter out;
   
   /** sequence separator positions in indexed sequence s */
   private final long[] ssp;

   /** minimum number of matches for output */
   private final int minseqmatches;
   private long seqstart;

   /** sequence descriptions of t (queries) */
   private final ArrayList<String> tdesc;

   /** description of sequences in indexed sequence s */
   private final ArrayList<String> sdesc;
   private final int minlen;

   public SortedMatchReporter(long[] ssp, int minseqmatches, int minlen, ArrayList<String> tdesc,
         ArrayList<String> sdesc, PrintWriter out) {
      this.ssp = ssp;
      this.minseqmatches = minseqmatches;
      this.tdesc = tdesc;
      this.sdesc = sdesc;
      this.out = out;
      this.minlen = minlen;
      matches = new ArrayList<ArrayList<Match>>();
      matches.ensureCapacity(ssp.length);
      for (int i = 0; i < ssp.length; i++)
         matches.add(i, new ArrayList<Match>(32));
   }

   /**
    * Adds a match to the list.
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
      matches.get(i).add(new Match(ttt, sss, matchlength));
   }

   private int seqindex(final int p) {
      int si = Arrays.binarySearch(ssp, p);
      if (si >= 0)
         return si; // return the index of the ssp position
      return (-si - 1); // we are in a sequence, return the index of the following ssp position
   }

   public void write(int seqnum) {
      ArrayList<Match> mi = null;
      long total = 0;
      int mseq = 0;
      int ms;
      out.printf(">%d:'''%s'''%n", seqnum, tdesc.get(seqnum));
      for (int i = 0; i < ssp.length; i++) {
         mi = matches.get(i);
         if (mi.size() == 0)
            continue;
         ms = 0;
         for (Match mm : mi)
            ms += mm.len;
         if (ms >= minseqmatches * minlen) {
            total += mi.size();
            mseq++;
            out.printf("@%d:'''%s'''%n", i, sdesc.get(i));
            for (Match mm : mi)
               out.printf(". %d %d %d %d%n", mm.tpos, mm.spos, mm.len, (long) mm.spos - mm.tpos);
         }
         // mi.clear(); // clear match list
      }
      out.printf("<%d: %d %d%n%n", seqnum, mseq, total);
   }

   public void clear() {
      for (int i = 0; i < matches.size(); i++) {
         matches.get(i).clear();
      }
   }

   /** simple structure for sorted matches, per index sequence */
   private class Match {
      final long tpos;
      final int spos;
      final int len;

      public Match(final long tpos, final int spos, final int len) {
         this.tpos = tpos;
         this.spos = spos;
         this.len = len;
      }
   }

   @Override
   public void setSequenceStart(long seqstart) {
      this.seqstart = seqstart;
   }
}

class RunGlobalMatchReporter extends GlobalMatchReporter {

   private int[] queryRunToPos; // 't'
   private int[] indexRunToPos; // 's'

   // TODO don't use int[] arrays as parameter, but IntBuffers instead
   public RunGlobalMatchReporter(long[] ssp, int minseqmatches, int maxseqmatches, boolean selfcmp,
         PrintWriter out, int[] queryRunToPos, int[] indexRunToPos) {
      super(ssp, minseqmatches, maxseqmatches, selfcmp, out);
      this.queryRunToPos = queryRunToPos;
      this.indexRunToPos = indexRunToPos;
   }

   @Override
   public void add(int sRunStart, long tRunStart, int runMatchLength) {
      // convert run-based indices to regular indices
      int sStart = indexRunToPos[sRunStart];
      
      // TODO not 64-bit ready!
      int tStart = queryRunToPos[(int)tRunStart];
      assert (int)(tRunStart + runMatchLength) == tRunStart;
      
      // compute actual matchLength
      // TODO what do we report when the matchLengths differ?
      // TODO not 64-bit ready!
      int matchLength = java.lang.Math.min(indexRunToPos[sRunStart + runMatchLength] - sStart, queryRunToPos[(int)(tRunStart + runMatchLength)] - tStart);
      
//      int i = seqindex(sStart);
//      int ttt = tStart - seqstart;
//      int sss = sStart - (i == 0 ? 0 : (int) ssp[i - 1] + 1);
//      if (!selfcmp || sStart > tStart)
//         globalmatches.add(new GlobalMatch(ttt, i, sss, matchLength));
    
      
      super.add(sStart, tStart, matchLength);
   }
}