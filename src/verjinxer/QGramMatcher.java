package verjinxer;

import static verjinxer.sequenceanalysis.BisulfiteQGramCoder.NUCLEOTIDE_A;
import static verjinxer.sequenceanalysis.BisulfiteQGramCoder.NUCLEOTIDE_C;
import static verjinxer.sequenceanalysis.BisulfiteQGramCoder.NUCLEOTIDE_G;
import static verjinxer.sequenceanalysis.BisulfiteQGramCoder.NUCLEOTIDE_T;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

import com.spinn3r.log5j.Logger;

import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.sequenceanalysis.BisulfiteQGramCoder;
import verjinxer.sequenceanalysis.InvalidSymbolException;
import verjinxer.sequenceanalysis.QGramCoder;
import verjinxer.sequenceanalysis.QGramFilter;
import verjinxer.sequenceanalysis.QGramIndex;
import verjinxer.util.BitArray;
import verjinxer.util.ProjectInfo;
import verjinxer.util.TicToc;

public class QGramMatcher {
   private static final Logger log = Globals.log;
   final Globals g;

   /** minimum match length; guaranteed to be at least q */
   final int minlen;

   /** q-gram length */
   final int q;

   /** the alphabet map */
   final Alphabet alphabet;

   /** the query sequence text (coded) */
   final byte[] t;

   /** sequence separator positions in text t */
   final private long[] tssp;

   /** Positions of all q-grams */
   final QGramIndex qgramindex;

   /** the indexed text (coded) */
   final byte[] s;

   /** sequence separator positions in indexed sequence s */
   final long[] ssp;

   /** number of sequences in s */
   final int sm;

   /** stride width of the q-gram index */
   final int stride;

   /** maximum number of allowed matches */
   final int maxseqmatches;

   final MatchReporter matchReporter;
   final QGramCoder qgramcoder;
   final BitArray toomanyhits;
   final QGramFilter qgramfilter;

   /** whether the index is for bisulfite sequences */
   final boolean bisulfiteIndex;

   /** whether C matches C, even if not before G */
   final boolean c_matches_c; //

   // TODO make some of these final
   /** number of active matches */
   int active;

   /** starting positions of active matches in s */
   int[] activepos;

   /** match lengths for active matches */
   int[] activelen;

   // / int[] activediag;
   int[] newpos; // starting positions of new matches in s // TODO rename to currentpos
   int[] newlen; // match lengths for new matches
   // int[] newdiag;
   int seqstart = 0; // starting pos of current sequence in t

   /**
    * Creates a new instance of QgramMatcher
    * 
    * @param gl
    *           the Globals structure
    * @param args
    *           the command line arguments
    * @param toomanyhits
    *           may be null
    */
   public QGramMatcher(Globals g, String dt, String ds, String toomanyhitsfilename,
         int maxseqmatches, int minseqmatches, int minlen, final QGramCoder qgramcoder,
         final QGramFilter qgramfilter, final PrintWriter out, final boolean sorted,
         final boolean selfcmp, final boolean c_matches_c,
         ProjectInfo project) throws IOException {
      this.g = g;
      this.bisulfiteIndex = project.isBisulfiteIndex();
      this.qgramfilter = qgramfilter;
      this.qgramcoder = qgramcoder;
      this.q = qgramcoder.q;
      this.c_matches_c = c_matches_c;
      this.stride = project.getStride();

      if (c_matches_c && !bisulfiteIndex) throw new UnsupportedOperationException("c_matches_c for non-bisulfite index not supported");
      alphabet = g.readAlphabet(ds + FileNameExtensions.alphabet);

      // final BitSet thefilter = coder.createFilter(opt.get("F")); // empty filter if null
      if (minlen < q) {
         log.warn("qmatch: increasing minimum match length to q=%d!", q);
         minlen = q;
      }
      this.minlen = minlen;

      if (minseqmatches < 1) {
         log.warn("qmatch: increasing minimum match number to 1!");
         minseqmatches = 1;
      }
      this.maxseqmatches = maxseqmatches;

      /* private void openFiles(String dt, String ds, String outname, boolean external) { */
      // Read text, text-ssp, seq, qbck, ssp into arrays;
      // read sequence descriptions;
      // memory-map or read qpos.
      TicToc ttimer = new TicToc();
      final String tfile = dt + FileNameExtensions.seq;
      final String tsspfile = dt + FileNameExtensions.ssp;
      final String seqfile = ds + FileNameExtensions.seq;
      final String sspfile = ds + FileNameExtensions.ssp;
      System.gc();
      t = g.slurpByteArray(tfile);
      tssp = g.slurpLongArray(tsspfile);
      final ArrayList<String> tdesc = g.slurpTextFile(dt + FileNameExtensions.desc, tssp.length);
      assert tdesc.size() == tssp.length;

      final ArrayList<String> sdesc;

      if (dt.equals(ds)) {
         s = t;
         ssp = tssp;
         sm = tssp.length;
         sdesc = tdesc;
      } else {
         s = g.slurpByteArray(seqfile);
         ssp = g.slurpLongArray(sspfile);
         sm = ssp.length;
         sdesc = g.slurpTextFile(ds + FileNameExtensions.desc, sm);
         assert sdesc.size() == sm;
      }

      if (sorted) {
         matchReporter = new SortedMatchReporter(ssp, minseqmatches, minlen, tdesc, sdesc, out);
      } else {
         matchReporter = new GlobalMatchReporter(ssp, minseqmatches, maxseqmatches, selfcmp, out);
      }

      qgramindex = new QGramIndex(project);
      log.info("qmatch: mapping and reading files took %.1f sec", ttimer.tocs());

      if (toomanyhitsfilename != null) {
         toomanyhits = g.slurpBitArray(toomanyhitsfilename);
      } else {
         toomanyhits = new BitArray(tssp.length); // if -t not given, start with a clean filter
      }
   }

   public void tooManyHits(String filename) {
      log.info("qmatch: too many hits for %d/%d sequences (%.2f%%)", toomanyhits.cardinality(),
            tssp.length, toomanyhits.cardinality() * 100.0 / tssp.length);
      g.dumpBitArray(filename, toomanyhits);
   }

   /**
    * 
    * @param thefilter
    */
   public void match() {
      // Walk through t:
      // (A) Initialization
      TicToc timer = new TicToc();

      final int maxactive = qgramindex.getMaximumBucketSize();
      activepos = new int[maxactive];
      activelen = new int[maxactive];

      // / activediag = new int[maxactive];
      newpos = new int[maxactive];
      newlen = new int[maxactive];
      // / newdiag = new int[maxactive];
      // / currentpos = new int[maxactive];

      // (B) Walking ...
      final int tn = t.length;
      final int slicefreq = 5;
      final int slicesize = 1 + (slicefreq * tn / 100);
      int nextslice = 0;
      int percentdone = 0;
      int symremaining = 0;

      seqstart = 0;
      int seqnum = 0; // number of current sequence in t
      int tp = 0; // current position in t
      int seqmatches = 0; // number of matches in current target sequence
      while (tp < tn) {

         // (1) Determine next valid position p in t with potential match
         if (symremaining < minlen) { // next invalid is possibly at tp+symremaining
            // TODO < q
            tp += symremaining;
            symremaining = 0;
            for (; tp < tn && (!alphabet.isSymbol(t[tp])); tp++) {
               if (alphabet.isSeparator(t[tp])) {
                  assert tp == tssp[seqnum];
                  matchReporter.write(seqnum);
                  matchReporter.clear();
                  seqnum++;
                  seqstart = tp + 1;
                  matchReporter.setSequenceStart(seqstart);
                  seqmatches = 0;
               }
            }
            if (tp >= tn)
               break;
            if (toomanyhits.get(seqnum) == 1) {
               symremaining = 0;
               tp = (int) tssp[seqnum];
               continue;
            }
            int i; // next valid symbol is now at tp, count number of valid symbols
            for (i = tp; i < tn && alphabet.isSymbol(t[i]); i++) {
            }
            symremaining = i - tp;
            if (symremaining < minlen)
               continue; // / < q
         }
         assert alphabet.isSymbol(t[tp]);
         assert symremaining >= minlen; // TODO >= q
         log.debug("  position %d (in seq. %d, starting at %d): %d symbols", tp, seqnum, seqstart,
               symremaining);

         // (2) initialize qcode and active q-grams
         active = 0; // number of active q-grams
         int qcode = qgramcoder.code(t, tp);
         assert qcode >= 0;
         seqmatches += updateActiveIntervals(tp, qcode, maxseqmatches - seqmatches,
               qgramfilter.isFiltered(qcode));
         if (seqmatches > maxseqmatches) {
            symremaining = 0;
            tp = (int) tssp[seqnum];
            toomanyhits.set(seqnum, true);
         }

         // (3) repeatedly process current position p
         while (symremaining >= minlen) { // / >= q
            // (3a) Status
            while (tp >= nextslice) {
               log.info("  %2d%% done, %.1f sec, pos %d/%d, seq %d/%d", percentdone, timer.tocs(),
                     tp, tn - 1, seqnum, tssp.length - 1);
               percentdone += slicefreq;
               nextslice += slicesize;
            }
            // (3b) update q-gram
            tp++;
            symremaining--;
            if (symremaining >= minlen) {
               qcode = qgramcoder.codeUpdate(qcode, t[tp + q - 1]);
               assert qcode >= 0;
               seqmatches += updateActiveIntervals(tp, qcode, maxseqmatches - seqmatches,
                     qgramfilter.isFiltered(qcode));
               if (seqmatches > maxseqmatches) {
                  symremaining = 0;
                  tp = (int) tssp[seqnum];
                  toomanyhits.set(seqnum, true);
               }
            }
         } // end (3) while loop

         // (4) done with this block of positions. Go to next.
      }
      assert seqnum == tssp.length && tp == tn;
   }

   /**
    * Same as match(), except that all bisulfite-simulated q-grams of the query are searched in the
    * q-gram index.
    */
   public void bisulfiteMatch() {
      assert !bisulfiteIndex;
//      log.info("bisulfiteMatch");
      // Walk through t:
      // (A) Initialization
      TicToc timer = new TicToc();

      BisulfiteQGramCoder bicoder = (BisulfiteQGramCoder) qgramcoder;
      final int maxactive = qgramindex.getMaximumBucketSize();
      activepos = new int[maxactive];
      activelen = new int[maxactive];

      newpos = new int[maxactive];
      newlen = new int[maxactive];

      // (B) Walking ...
      final int tn = t.length;
      final int slicefreq = 5;
      final int slicesize = 1 + (slicefreq * tn / 100);
      int nextslice = 0;
      int percentdone = 0;
      int symremaining = 0;

      seqstart = 0;
      int seqnum = 0; // number of current sequence in t
      int tp = 0; // current position in t
      int seqmatches = 0; // number of matches in current target sequence

      outer: while (tp < tn) {

         // (1) Determine next valid position p in t with potential match
         while (symremaining < minlen) { // next invalid is possibly at tp+symremaining
            // TODO < q
            tp += symremaining;
            symremaining = 0;
            for (; tp < tn && (!alphabet.isSymbol(t[tp])); tp++) {
               if (alphabet.isSeparator(t[tp])) {
                  assert tp == tssp[seqnum];
                  matchReporter.write(seqnum);
                  matchReporter.clear();
                  seqnum++;
                  seqstart = tp + 1;
                  matchReporter.setSequenceStart(seqstart);
                  seqmatches = 0;
               }
            }
            if (tp >= tn)
               break outer;
            if (toomanyhits.get(seqnum) == 1) {
               symremaining = 0;
               tp = (int) tssp[seqnum];
               continue;
            }
            // next valid symbol is now at tp, count number of remaining valid symbols
            int i;
            for (i = tp; i < tn && alphabet.isSymbol(t[i]); i++) {
            }
            symremaining = i - tp;
         }
         assert alphabet.isSymbol(t[tp]);
         assert symremaining >= minlen; // TODO >= q
         log.debug("  position %d (in seq. %d, starting at %d): %d symbols", tp, seqnum, seqstart,
               symremaining);

         // (2) initialize qcode and active q-grams
         active = 0; // number of active q-grams
         int[] qcodesForward = bicoder.bisulfiteQCodes(t, tp, true);
         int[] qcodesReverse = bicoder.bisulfiteQCodes(t, tp, false);

         // TODO copying the arrays is totally unnecessary
         int[] qcodes = new int[qcodesForward.length + qcodesReverse.length];
         System.arraycopy(qcodesForward, 0, qcodes, 0, qcodesForward.length);
         System.arraycopy(qcodesReverse, 0, qcodes, qcodesForward.length, qcodesReverse.length);

         seqmatches += updateActiveIntervalsBisulfite(tp, qcodes, maxseqmatches - seqmatches);

         // /////////assert qcode >= 0;
         // //////seqmatches += updateActiveIntervals(tp, qcode, maxseqmatches - seqmatches,
         // /////// qgramfilter.isFiltered(qcode));
         if (seqmatches > maxseqmatches) {
            symremaining = 0;
            tp = (int) tssp[seqnum];
            toomanyhits.set(seqnum, true);
         }

         // (3) repeatedly process current position p
         while (symremaining >= minlen) { // / >= q
            // (3a) Status
            while (tp >= nextslice) {
               log.info("  %2d%% done, %.1f sec, pos %d/%d, seq %d/%d", percentdone, timer.tocs(),
                     tp, tn - 1, seqnum, tssp.length - 1);
               percentdone += slicefreq;
               nextslice += slicesize;
            }
            // (3b) update q-gram
            tp++;
            symremaining--;
            if (symremaining >= minlen) {
               qcodesForward = bicoder.bisulfiteQCodes(t, tp, true);
               qcodesReverse = bicoder.bisulfiteQCodes(t, tp, false);

               // TODO copying the arrays is totally unnecessary
               qcodes = new int[qcodesForward.length + qcodesReverse.length];
               System.arraycopy(qcodesForward, 0, qcodes, 0, qcodesForward.length);
               System.arraycopy(qcodesReverse, 0, qcodes, qcodesForward.length, qcodesReverse.length);

               seqmatches += updateActiveIntervalsBisulfite(tp, qcodes, maxseqmatches - seqmatches);
               // /////// qcode = bicoder.codeUpdate(qcode, t[tp + q - 1]);
               // ///////// assert qcode >= 0;
               // //////// seqmatches += updateActiveIntervals(tp, qcode, maxseqmatches -
               // seqmatches,
               // //////// qgramfilter.isFiltered(qcode));
               if (seqmatches > maxseqmatches) {
                  symremaining = 0;
                  tp = (int) tssp[seqnum];
                  toomanyhits.set(seqnum, true);
               }
            }
         } // end (3) while loop

         // (4) done with this block of positions. Go to next.
      }
      assert seqnum == tssp.length && tp == tn;
   }


   enum BisulfiteState { GA, CT, UNKNOWN }

   /**
    * Compares sequences s and t, allowing bisulfite replacements.
    * 
    * @param sp
    *           start index in s
    * @param tp
    *           start index in t
    * @return length of match
    */
   private int bisulfiteMatchLength(byte[] s, int sp, byte[] t, int tp) {
      BisulfiteState state = BisulfiteState.UNKNOWN;
      int offset = 0;
      while (true) {
         if (!alphabet.isSymbol(s[sp + offset]))
            break;

         // What follows is some ugly logic to find out what type
         // of match this is. That is, whether we should allow C -> T or
         // G -> A replacements.
         // For C->T, the rules are:
         // If there's a C->T replacement, we must only allow those.
         // If there's a C not preceding a G that has not been replaced
         // by a T, then we must not allow C->T replacements.

         if (s[sp + offset] == NUCLEOTIDE_G && t[tp + offset] == NUCLEOTIDE_A) {
            if (state == BisulfiteState.CT)
               break;
            state = BisulfiteState.GA;
         } else if (offset > 0 && s[sp + offset - 1] != NUCLEOTIDE_C
               && t[tp + offset - 1] != NUCLEOTIDE_C && s[sp + offset] == NUCLEOTIDE_G
               && t[tp + offset] == NUCLEOTIDE_G) {
            // G on both without preceding C
            if (state == BisulfiteState.GA)
               break;
            state = BisulfiteState.CT;
         } else if (s[sp + offset] == NUCLEOTIDE_C && t[tp + offset] == NUCLEOTIDE_T) {
            if (state == BisulfiteState.GA)
               break;
            state = BisulfiteState.CT;
         } else if (sp + offset + 1 < s.length && tp + offset + 1 < t.length
               && s[sp + offset + 1] != NUCLEOTIDE_G &&
               /* t[tp+offset+1] != NUCLEOTIDE_G && */
               s[sp + offset] == NUCLEOTIDE_C && t[tp + offset] == NUCLEOTIDE_C) {
            if (state == BisulfiteState.CT)
               break;
            state = BisulfiteState.GA;
         } else {
            if (s[sp + offset] != t[tp + offset])
               break;
         }
         offset++;
      }
      assert offset >= q;
      return offset;
   }

   /**
    * Compares sequences s and t, allowing bisulfite replacements. Allows that a C matches a C, even
    * if not before G (and that a G matches G even if not after C).
    * 
    * @param sp
    *           start index in s
    * @param tp
    *           start index in t
    * @return length of match
    */
   private int bisulfiteMatchLengthCmC(byte[] s, int sp, byte[] t, int tp) {
         System.out.println("bisulfiteMatchLengthCmC. tp=" + tp + ". sp="+sp);
         try {
            System.out.println("s[sp..sp+q]="+alphabet.preimage(s, sp, q));
            System.out.println("t[tp..tp+q]="+alphabet.preimage(t, tp, q));
         } catch (InvalidSymbolException e) {
            e.printStackTrace();
         }
      int offset = 0;

      while (alphabet.isSymbol(s[sp + offset]) && s[sp + offset] == t[tp + offset])
         offset++;

      // the first mismatch tells us what type of match this is
      byte s_char = s[sp + offset];
      byte t_char = t[tp + offset];
      if (s_char == NUCLEOTIDE_C && t_char == NUCLEOTIDE_T) {
         // we have C -> T replacements
         offset++;
         while (alphabet.isSymbol(s[sp + offset])
               && (s[sp + offset] == t[tp + offset] || s[sp + offset] == NUCLEOTIDE_C
                     && t[tp + offset] == NUCLEOTIDE_T))
            offset++;
      } else if (s_char == NUCLEOTIDE_G && t_char == NUCLEOTIDE_A) {
         // we have G -> A replacements
         offset++;
         while (alphabet.isSymbol(s[sp + offset])
               && (s[sp + offset] == t[tp + offset] || s[sp + offset] == NUCLEOTIDE_G
                     && t[tp + offset] == NUCLEOTIDE_A))
            offset++;
      }
      assert offset >= q;
      return offset;
   }

   /**
    * @param tp
    * @param qcode
    * @param maxmatches
    *           stop reporting new matches if this limit is reached
    * @param filtered
    * @return number of matches reported
    */
   private int updateActiveIntervalsBisulfite(final int tp, final int[] qcodes, final int maxmatches) {
//      System.out.println("updateActiveIntervalsBisulfite. tp = "+tp);
      int matches = 0;
      // decrease length of active matches, as long as they stay >= q TODO >= minlen?
      int ai; // index into the array of active matches
      for (ai = 0; ai < active; ai++) {
         activepos[ai]++;
         if (activelen[ai] > q)
            activelen[ai]--;
         else
            activelen[ai] = 0;
      }

      // collect all qgram positions of all unfiltered qcodes into newpos, reallocating
      // newpos if necessary
      int newactive = 0;
      for (int qcode : qcodes) {
         if (!qgramfilter.isFiltered(qcode)) {
            newactive += qgramindex.getBucketSize(qcode);
         }
      }
      if (newpos.length < newactive) {
         int newSize = Math.max(newactive, Math.max(10000, newpos.length * 2));
         newpos = new int[newSize];
         newlen = new int[newSize];
      }
      assert newpos.length >= newactive;
      int pos = 0;
      for (int qcode : qcodes) {
         if (qgramfilter.isFiltered(qcode)) {
            continue;
         }
         qgramindex.getQGramPositions(qcode, newpos, pos);
         pos += qgramindex.getBucketSize(qcode);
      }

      // TODO n-way merge would be more efficient
      Arrays.sort(newpos, 0, newactive);

      // iterate over all new matches
      ai = 0;
      for (int ni = 0; ni < newactive; ni++) {
         while (ai < active && activelen[ai] < q)
            ai++;

         if (c_matches_c) {
            // we must skip those matches that are only active because
            // of the 'C matches C' rule. They don't have q-grams
            // in common with the query anymore
            while (ai < active && newpos[ni] > activepos[ai])
               ai++;
         }
         // make sure that newly found q-grams overlap the old ones (unless c_matches_c)
         assert ai == active || newpos[ni] <= activepos[ai] : String.format(
               "tp=%d, ai/active=%d/%d, ni=%d, newpos=%d, activepos=%d, activelen=%d", tp, ai,
               active, ni, newpos[ni], activepos[ai], activelen[ai]);
         assert ai <= active;
         if (ai == active || newpos[ni] < activepos[ai]) {
            // this is a new match:
            // determine newlen[ni] by comparing s[sp...] with t[tp...]
            int sp;
            int offset;
            /*if (!bisulfiteQueries) {
               sp = newpos[ni] + q;
               offset = q;
               while (s[sp] == t[tp + offset] && alphabet.isSymbol(s[sp])) {
                  sp++;
                  offset++;
               }
               sp -= offset; // go back to start of match
            } else {*/
            sp = newpos[ni];
            offset = c_matches_c ? bisulfiteMatchLengthCmC(t, tp, s, sp)
                  : bisulfiteMatchLength(t, tp, s, sp);
//            }
            newlen[ni] = offset;

            // maximal match (tp, sp, offset), i.e. ((seqnum,tp-seqstart), (i,sss), offset)
            if (offset >= minlen) {
               matchReporter.add(sp, tp, offset);
               ++matches;
               if (matches > maxmatches)
                  break;
            }
         } else { // this is an old (continuing) match
            newlen[ni] = activelen[ai];
            ai++;
         }
      }

      // TODO put this note somewhere else

      // There are always two buffers for match positions:
      // - activepos contains the currently active matches.
      // - newpos contains the matches of the next round.
      //
      // One buffer is not enough since the computation needs to be able to look at both.
      // When newpos has been updated after a round and contains the now active
      // positions, references are simply swapped: activepos becomes newpos and vice-versa.
      // In this way, newpos and activepos never have to be re-allocated.
      //
      // The same holds for the match length arrays activelen and newlen.

      // swap activepos <-> newpos and activelen <-> newlen
      int[] tmp;

      tmp = activepos;
      activepos = newpos;
      newpos = tmp;

      tmp = activelen;
      activelen = newlen;
      newlen = tmp;

      active = newactive;
      return matches;

   }

   /**
    * @param tp
    * @param qcode
    * @param maxmatches
    *           stop reporting new matches if this limit is reached
    * @param filtered
    * @return number of matches reported
    */
   private int updateActiveIntervals(final int tp, final int qcode, final int maxmatches,
         final boolean filtered) {
      int matches = 0;
      // decrease length of active matches, as long as they stay >= q TODO >= minlen?
      int ai; // index into the array of active matches
      for (ai = 0; ai < active; ai++) {
         activepos[ai]++;
         if (activelen[ai] > q)
            activelen[ai]--;
         else
            activelen[ai] = 0;
      }

      // If this q-gram is filtered, discard matches that are too short from
      // activepos and activelen, and return.
      if (filtered) {
         int ni = 0;
         for (ai = 0; ai < active; ai++) {
            if (activelen[ai] < q)
               continue;
            assert ni <= ai;
            activepos[ni] = activepos[ai];
            activelen[ni] = activelen[ai];
            ni++;
         }
         active = ni;
         return 0;
      }
      // if (tp % 1000 == 0) g.logmsg("  findactive. tp=%d, newactive=%d", tp, newactive
      // /*coder.qGramString(qcode,amap), lrmmrow, r*/);

      // this q-gram is not filtered!

      qgramindex.getQGramPositions(qcode, newpos);
      final int newactive = qgramindex.getBucketSize(qcode); // number of new active q-grams

      // iterate over all new matches
      ai = 0;
      for (int ni = 0; ni < newactive; ni++) {
         while (ai < active && activelen[ai] < q)
            ai++;

         if (c_matches_c) {
            // we must skip those matches that are only active because
            // of the 'C matches C' rule. They don't have q-grams
            // in common with the query anymore
            while (ai < active && newpos[ni] > activepos[ai])
               ai++;
         }
         // make sure that newly found q-grams overlap the old ones (unless c_matches_c)
         assert ai == active || newpos[ni] <= activepos[ai] : String.format(
               "tp=%d, ai/active=%d/%d, ni=%d, newpos=%d, activepos=%d, activelen=%d", tp, ai,
               active, ni, newpos[ni], activepos[ai], activelen[ai]);
         assert ai <= active;
         if (ai == active || newpos[ni] < activepos[ai]) {
            // this is a new match:
            // determine newlen[ni] by comparing s[sp...] with t[tp...]
            int sp;
            int offset;
            if (!bisulfiteIndex) {
               sp = newpos[ni] + q;
               offset = q;
               while (s[sp] == t[tp + offset] && alphabet.isSymbol(s[sp])) {
                  sp++;
                  offset++;
               }
               sp -= offset; // go back to start of match
            } else {
               sp = newpos[ni];
               offset = c_matches_c ? bisulfiteMatchLengthCmC(s, sp, t, tp)
                     : bisulfiteMatchLength(s, sp, t, tp);
            }
            newlen[ni] = offset;

            // maximal match (tp, sp, offset), i.e. ((seqnum,tp-seqstart), (i,sss), offset)
            if (offset >= minlen) {
               matchReporter.add(sp, tp, offset);
               ++matches;
               if (matches > maxmatches)
                  break;
            }
         } else { // this is an old (continuing) match
            newlen[ni] = activelen[ai];
            ai++;
         }
      }

      // TODO put this note somewhere else

      // There are always two buffers for match positions:
      // - activepos contains the currently active matches.
      // - newpos contains the matches of the next round.
      //
      // One buffer is not enough since the computation needs to be able to look at both.
      // When newpos has been updated after a round and contains the now active
      // positions, references are simply swapped: activepos becomes newpos and vice-versa.
      // In this way, newpos and activepos never have to be re-allocated.
      //
      // The same holds for the match length arrays activelen and newlen.

      // swap activepos <-> newpos and activelen <-> newlen
      int[] tmp;

      tmp = activepos;
      activepos = newpos;
      newpos = tmp;

      tmp = activelen;
      activelen = newlen;
      newlen = tmp;

      active = newactive;
      return matches;
   }

   /**
    * Compares sequences s and t, allowing bisulfite replacements. Allows that a C matches a C, even
    * if not before G (and that a G matches G even if not after C).
    * 
    * @param sp
    *           start index in s
    * @param tp
    *           start index in t
    * @param ret
    *           array in which the tuple (sp, tp, length) for the match will be stored
    * @return length of match
    * 
    * 
    *         TODO this function is only used within updateActiveIntervals_strided
    * 
    *         TODO seqstart should not be needed here
    */
   private int bisulfiteMatchLengthCmC(int sstart, int tstart, int[] ret) {
      assert ret.length == 3;
      int sstop = sstart;
      int tstop = tstart;

      // try {
      // System.out.format("t: %d. s: %d%n", tstart, sstart);
      // System.out.println("t: "+amap.preimage(t, tstart, Math.min(t.length-tstart, 100)));
      // System.out.println("s: "+amap.preimage(s, sstart, Math.min(s.length-sstart, 100)));
      // } catch (InvalidSymbolException e) {
      //
      // }
      // idea:
      // [sstart, sstop) and [tstart, tstop) are intervals, which we try to extend
      // to the left and to the right

      // TODO the match type could often be determined from the q-gram itself!

      while (alphabet.isSymbol(s[sstop]) && s[sstop] == t[tstop]) {
         sstop++;
         tstop++;
      }

      // the first mismatch tells us what type of match this is
      final byte nucleotide_s;
      final byte nucleotide_t;
      if (s[sstop] == NUCLEOTIDE_C && t[tstop] == NUCLEOTIDE_T) {
         // we have C -> T replacements
         nucleotide_s = NUCLEOTIDE_C;
         nucleotide_t = NUCLEOTIDE_T;
         sstop++;
         tstop++;
      } else if (s[sstop] == NUCLEOTIDE_G && t[tstop] == NUCLEOTIDE_A) {
         // we have G -> A replacements
         nucleotide_s = NUCLEOTIDE_G;
         nucleotide_t = NUCLEOTIDE_A;
         sstop++;
         tstop++;
      } else {
         // replacement type unknown: searching backwards may give us
         // the desired information
         assert sstop - sstart >= q : "sstop=" + sstop + ". sstart=" + sstart + "(difference: "
               + (sstop - sstart) + ") q=" + q;

         if (stride > 1) {
            while (sstart > 0 && tstart > seqstart && alphabet.isSymbol(s[sstart - 1])
                  && s[sstart - 1] == t[tstart - 1]) {
               sstart--;
               tstart--;
            }
            if (sstart == 0 || tstart == seqstart || !alphabet.isSymbol(s[sstart - 1])) {
               assert seqstart == 0 || !alphabet.isSymbol(t[seqstart - 1]);
               ret[0] = sstart;
               ret[1] = tstart;
               ret[2] = sstop - sstart;
               assert sstop - sstart == tstop - tstart;
               return ret[2];
            }
            if (s[sstart - 1] == NUCLEOTIDE_C && t[tstart - 1] == NUCLEOTIDE_T) {
               // we have C -> T replacements
               nucleotide_s = NUCLEOTIDE_C;
               nucleotide_t = NUCLEOTIDE_T;
            } else if (s[sstart - 1] == NUCLEOTIDE_G && t[tstart - 1] == NUCLEOTIDE_A) {
               // we have G -> A replacements
               nucleotide_s = NUCLEOTIDE_G;
               nucleotide_t = NUCLEOTIDE_A;
            } else {
               // replacement type still unknown
               ret[0] = sstart;
               ret[1] = tstart;
               ret[2] = sstop - sstart;
               assert tstop - tstart == sstop - sstart;
               return ret[2];
            }
         } else {
            // replacement type is unknown and searching backwards is not applicable
            // think about this: assert sstart == 0 || !amap.isSymbol(s[sstart-1]) || s[sstart-1] ==
            // t[tstart-1] || s[sstart-1] == NUCLEOTIDE_C && t[tp-1] == NUCLEOTIDE_T;
            ret[0] = sstart;
            ret[1] = tstart;
            ret[2] = sstop - sstart;
            return ret[2];
         }
      }

      // replacement type is known here

      assert tstop <= t.length && tstart >= seqstart;

      // search further to the right ...
      while (alphabet.isSymbol(s[sstop])
            && (s[sstop] == t[tstop] || (s[sstop] == nucleotide_s && t[tstop] == nucleotide_t))) {
         sstop++;
         tstop++;
      }

      // ... and possibly to the left
      // TODO it may make sense to do this even if stride==1
      if (stride > 1) {
         while (sstart > 0
               && tstart > seqstart
               && alphabet.isSymbol(s[sstart - 1])
               && (s[sstart - 1] == t[tstart - 1] || s[sstart - 1] == nucleotide_s
                     && t[tstart - 1] == nucleotide_t)) {
            sstart--;
            tstart--;
         }
      }

      ret[0] = sstart;
      ret[1] = tstart;
      assert sstop - sstart == tstop - tstart;
      ret[2] = sstop - sstart;
      return ret[2];
   }

   /**
    * 
    * @param sp
    * @param tp
    * @param ret
    *           TODO only used within updateActiveIntervals_strided
    */
   private void regularMatchLength(int sp, int tp, int[] ret) {
      int len = q;
      while (s[sp + len] == t[tp + len] && alphabet.isSymbol(s[sp])) {
         len++;
      }
      if (stride > 1) {
         assert false;
         // FIXME !!!!!!!!
      }
      ret[0] = sp;
      ret[1] = tp;
      ret[2] = len;
   }

   /**
    * New method for updating the active intervals when the index is strided. Uses a slightly
    * different algorithm. Don't use, yet.
    * 
    * Given the next q-code of the query sequence, this function updates the currently active
    * intervals (activepos, activelen, activediag).
    * 
    * 
    * writes to: activepos, activelen, active newpos, newlen TODO if stride=1 then currentpos and
    * newpos could be the same
    * 
    * @param tp
    * @param qcode
    * @param maxmatches
    *           stop reporting new matches if this limit is reached
    * @param filtered
    * @return number of matches reported
    */
   private final int updateActiveIntervals_strided(final int tp, final int qcode,
         final int maxmatches, final boolean filtered) {

      // TODO FIXME XXX
      // Note
      // If you really want to use this, you must declare the following three variables
      // not within this method, but as object variables. they are just here to make the code compile.

      int[] currentpos = new int[0];
      int[] newdiag = new int[0];
      int[] activediag = new int[0];

      if (filtered)
         return 0;

      // The aim of this function is to avoid reporting overlapping matches as much as possible.
      //
      // activepos and activelen contain the starting positions and lengths of the active
      // intervals. Active means that these intervals have been recognized as potential
      // matches (potential in the sense that they may be too short to be reported).
      // As soon as a new interval is discovered which is long enough, it is reported.
      //
      // First, we obtain the positions of the q-gram corresponding to the given qcode
      // from the q-gram index.

      int matches = 0;
      qgramindex.getQGramPositions(qcode, currentpos);
      final int currentactive = qgramindex.getBucketSize(qcode); // number of new active q-grams

      if (false && currentactive > 0) {
         System.out.printf("currentpos:");
         for (int mm = 0; mm < currentactive; ++mm) {
            System.out.printf(" %d", currentpos[mm]);
         }
         System.out.println();
      }
      int ai = 0; // index into the array of active matches
      int ci = 0; // index into array of current q-gram matches
      int ni = 0; // index into the array of new active matches (we re-use newpos for that)

      // temporary. needed for getting results out of the matchLength functions
      // declared here so we can re-use it (and avoid reallocating memory)
      int[] match = { 0, 0, 0 };

      // loop over all new q-gram positions and construct the new array of active intervals
      // (overwriting newpos)

      // the following loop is similar to merging two sorted lists

      while (ci < currentactive || ai < active) {
         // which diagonal comes first?

         if (ai == active || (ci < currentactive && currentpos[ci] - tp < activediag[ai])) {
            assert ci < currentactive;
            // new match, determine its length
            if (bisulfiteIndex) {
               // FIXME cmc is ignored
               bisulfiteMatchLengthCmC(currentpos[ci], tp, match);
            } else {
               regularMatchLength(currentpos[ci], tp, match);
            }
            final int sstart = match[0];
            final int tstart = match[1];
            final int len = match[2];

            // System.out.printf("matchleng: sstart=%d tstart=%d len=%d%n", sstart, tstart, len);
            if (len >= minlen) {
               matchReporter.add(sstart, tstart, len);
               ++matches;
               // reportMatch(currentpos[ci], tp, len - (currentpos[ci] - sstart));
               if (matches > maxmatches)
                  break;
               // there should not be a q-gram that overlaps the beginning of an active match
               // and that leads to a match
               // assert ai == active || newpos[ni] < activepos[ai] - q;
            }
            assert sstart - tstart == currentpos[ci] - tp;
            // save this as a new active match
            // we do not use the starting position that is reported,
            // but the position of the q-gram
            // newpos[ni] = currentpos[ci];
            // newlen[ni] = len - (currentpos[ci] - sstart);
            // newdiag[ni] = currentpos[ci] - tp;
            newpos[ni] = sstart;
            newlen[ni] = len;
            newdiag[ni] = sstart - tstart;
            assert newlen[ni] <= len; // TODO remove this
            ++ni;
            ++ci;
         } else if (ci == currentactive || currentpos[ci] - tp > activediag[ai]) {
            assert ai < active;
            // copy active match if there is a chance it could still be hit
            if (activepos[ai] - activediag[ai] + activelen[ai] >= tp + 1 + q) {
               newpos[ni] = activepos[ai];
               newlen[ni] = activelen[ai];
               newdiag[ni] = activediag[ai];
               ++ni;
            }
            ++ai;
         } else {
            // same diagonal
            assert ci < currentactive && ai < active;
            assert currentpos[ci] - tp == activediag[ai];

            if (currentpos[ci] + q > activepos[ai] + activelen[ai]) {
               // new match
               assert false;
            } else {
               // this q-gram is within an active match. report nothing, only copy the active match
               newpos[ni] = activepos[ai];
               newlen[ni] = activelen[ai];
               newdiag[ni] = activediag[ai];
               ++ni;
               ++ai;
               ++ci;
            }
         }
      }

      // System.out.printf("active at tp=%d --- ", tp);
      // for (int m = 0; m < ni; ++m) {
      // System.out.format("len=%d %d %d (dg %d) ", newlen[m], newpos[m], newpos[m] - newdiag[m],
      // newdiag[m]);
      // }
      // System.out.println();

      // TODO put this note somewhere else

      // There are always two buffers for match positions:
      // - activepos contains the currently active matches.
      // - newpos contains the matches of the next round.
      //
      // One buffer is not enough since the computation needs to be able to look at both.
      // When newpos has been updated after a round and contains the now active
      // positions, references are simply swapped: activepos becomes newpos and vice-versa.
      // In this way, newpos and activepos never have to be re-allocated.
      //
      // The same holds for the match length arrays activelen and newlen.

      // swap activepos <-> newpos and activelen <-> newlen
      int[] tmp;
      tmp = activepos;
      activepos = newpos;
      newpos = tmp;
      tmp = activelen;
      activelen = newlen;
      newlen = tmp;
      tmp = activediag;
      activediag = newdiag;
      newdiag = tmp;
      active = ni;
      return matches; // if (seqmatches > maxseqmatches) throw new TooManyHitsException();
   }
}
