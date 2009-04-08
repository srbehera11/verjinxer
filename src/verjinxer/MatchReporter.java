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
   private long[] ssp;

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

class GlobalMatchReporter implements MatchReporter {
   /** list of unsorted matches */
   private final ArrayList<GlobalMatch> globalmatches;

   private final int minseqmatches;
   private final int maxseqmatches;
   private final PrintWriter out;
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
      globalmatches.ensureCapacity(maxseqmatches < 127 ? maxseqmatches + 1 : 128);
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

   private int seqindex(final int p) {
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
