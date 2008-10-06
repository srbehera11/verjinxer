package verjinxer;

import static verjinxer.Globals.extalph;
import static verjinxer.Globals.extdesc;
import static verjinxer.Globals.extqbck;
import static verjinxer.Globals.extqpos;
import static verjinxer.Globals.extseq;
import static verjinxer.Globals.extssp;
import static verjinxer.sequenceanalysis.BisulfiteQGramCoder.NUCLEOTIDE_A;
import static verjinxer.sequenceanalysis.BisulfiteQGramCoder.NUCLEOTIDE_C;
import static verjinxer.sequenceanalysis.BisulfiteQGramCoder.NUCLEOTIDE_G;
import static verjinxer.sequenceanalysis.BisulfiteQGramCoder.NUCLEOTIDE_T;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import verjinxer.sequenceanalysis.AlphabetMap;
import verjinxer.sequenceanalysis.QGramCoder;
import verjinxer.sequenceanalysis.QGramFilter;
import verjinxer.sequenceanalysis.QGramIndex;
import verjinxer.util.BitArray;
import verjinxer.util.TicToc;

public class QgramMatcher {
   final Globals g;

   final boolean sorted;

   /** minimum number of matches for output */
   final int minseqmatches;

   /** comparing text against itself? */
   final boolean selfcmp;

   /** min. match length */
   final int minlen;

   /** q-gram length */
   final int q;

   /** alphabet size */
   final int asize;

   /** the alphabet map */
   final AlphabetMap amap;

   /** the query sequence text (coded) */
   final byte[] t;

   /** sequence separator positions in text t */
   final long[] tssp;

   /** sequence descriptions of t (queries) */
   final ArrayList<String> tdesc;

   /** Positions of all q-grams */
   final QGramIndex qgramindex;

   final PrintWriter out;

   /** the indexed text (coded) */
   final byte[] s;

   /** sequence separator positions in indexed sequence s */
   final long[] ssp;

   /** number of sequences in s */
   final int sm;

   /** stride width of the q-gram index */
   final int stride;

   /** description of sequences in indexed sequence s */
   final ArrayList<String> sdesc;

   /** maximum number of allowed matches */
   final int maxseqmatches;

   /** list of sorted matches */
   final ArrayList<ArrayList<Match>> matches;

   /** list of unsorted matches */
   final ArrayList<GlobalMatch> globalmatches;

   final BitArray toomanyhits;

   final boolean bisulfite; // whether the index is for bisulfite sequences
   final boolean c_matches_c; // whether C matches C, even if not before G

   // TODO make some of these final
   int active; // number of active matches
   int[] activepos; // starting positions of active matches in s
   int[] activelen; // match lengths for active matches
   int[] activediag;

   int[] currentpos; // starting positions of new matches in s

   int[] newlen; // match lengths for new matches
   int[] newpos;
   int[] newdiag;

   /** starting pos of current sequence in t */
   int seqstart = 0;

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
   public QgramMatcher(Globals g, String dt, String ds, String toomanyhitsfilename,
         int maxseqmatches, int minseqmatches, int minlen, int maxactive, int stride,
         final QGramCoder qgramcoder, final QGramFilter qgramfilter, final PrintWriter out,
         final boolean sorted, final boolean external, final boolean selfcmp,
         final boolean bisulfite, final boolean c_matches_c) throws IOException {
      this.g = g;
      this.selfcmp = selfcmp;
      this.bisulfite = bisulfite;
      this.asize = qgramcoder.asize;
      this.q = qgramcoder.q;
      this.out = out;
      this.c_matches_c = c_matches_c;
      this.stride = stride;
      this.sorted = sorted;
      amap = g.readAlphabetMap(ds + extalph);

      // final BitSet thefilter = coder.createFilter(opt.get("F")); // empty filter if null
      if (minlen < q) {
         g.warnmsg("qmatch: increasing minimum match length to q=%d!%n", q);
         minlen = q;
      }
      this.minlen = minlen;

      if (minseqmatches < 1) {
         g.warnmsg("qmatch: increasing minimum match number to 1!%n");
         minseqmatches = 1;
      }
      this.minseqmatches = minseqmatches;
      this.maxseqmatches = maxseqmatches;
      if (sorted) {
         matches = new ArrayList<ArrayList<Match>>();
         globalmatches = null;
      } else {
         globalmatches = new ArrayList<GlobalMatch>(maxseqmatches < 127 ? maxseqmatches + 1 : 128);
         matches = null;
      }

      /**
       * variables written to in the following t tn tssp tm tdesc s ssp sm sdesc
       * 
       * out
       */
      /*
       * private void openFiles(String dt, String ds, String outname, boolean external) {
       */// Read text, text-ssp, seq, qbck, ssp into arrays;
      // read sequence descriptions;
      // memory-map or read qpos.
      TicToc ttimer = new TicToc();
      final String tfile = dt + extseq;
      final String tsspfile = dt + extssp;
      final String seqfile = ds + extseq;
      final String qbckfile = ds + extqbck;
      final String sspfile = ds + extssp;
      final String qposfile = ds + extqpos;
      System.gc();
      t = g.slurpByteArray(tfile);
      tssp = g.slurpLongArray(tsspfile);
      tdesc = g.slurpTextFile(dt + extdesc, tssp.length);
      assert (tdesc.size() == tssp.length);

      if (dt.equals(ds)) {
         s = t;
         ssp = tssp;
         sm = tssp.length;
         sdesc = tdesc;
      } else {
         s = g.slurpByteArray(seqfile);
         ssp = g.slurpLongArray(sspfile);
         sm = ssp.length;
         sdesc = g.slurpTextFile(ds + extdesc, sm);
         assert (sdesc.size() == sm);
      }

      qgramindex = new QGramIndex(g, qposfile, qbckfile, maxactive, stride);
      g.logmsg("qmatch: mapping and reading files took %.1f sec%n", ttimer.tocs());

      // toomanyhits = new BitArray toomanyhits;
      if (toomanyhitsfilename != null) {
         toomanyhits = g.slurpBitArray(toomanyhitsfilename);
      } else {
         toomanyhits = new BitArray(tssp.length); // if -t not given, start with a clean filter
      }
   }

   public void tooManyHits(String filename) {
      g.logmsg("qmatch: too many hits for %d/%d sequences (%.2f%%)%n", toomanyhits.cardinality(),
            tssp.length, toomanyhits.cardinality() * 100.0 / tssp.length);
      g.dumpBitArray(filename, toomanyhits);
   }

   /**
    * 
    * @param thefilter
    */
   public void match(QGramCoder coder, final QGramFilter thefilter) {
      // Walk through t:
      // (A) Initialization
      TicToc timer = new TicToc();

      final int maxactive = qgramindex.getMaximumBucketSize();
      activepos = new int[maxactive];
      activediag = new int[maxactive];
      activelen = new int[maxactive];

      newpos = new int[maxactive];
      newlen = new int[maxactive];
      newdiag = new int[maxactive];

      currentpos = new int[maxactive];
      if (sorted) {
         matches.ensureCapacity(sm);
         for (int i = 0; i < sm; i++)
            matches.add(i, new ArrayList<Match>(32));
      } else {
         globalmatches.ensureCapacity(maxseqmatches < 127 ? maxseqmatches + 1 : 128);
      }

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
         if (symremaining < q) { // next invalid is possibly at tp+symremaining
            tp += symremaining;
            symremaining = 0;
            for (; tp < tn && (!amap.isSymbol(t[tp])); tp++) {
               if (amap.isSeparator(t[tp])) {
                  assert tp == tssp[seqnum];
                  if (sorted)
                     writeAndClearMatches(seqnum);
                  else
                     writeAndClearGlobalMatches(seqnum);
                  seqnum++;
                  seqstart = tp + 1;
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
            for (i = tp; i < tn && amap.isSymbol(t[i]); i++) { }
            symremaining = i - tp;
            if (symremaining < q)
               continue;
         }
         assert (amap.isSymbol(t[tp]));
         assert (symremaining >= q);
         // g.logmsg(" position %d (in seq. %d, starting at %d): %d symbols%n", p, seqnum, seqstart,
         // symremaining);

         // (2) initialize qcode and active q-grams
         active = 0; // number of active q-grams
         int qcode = coder.code(t, tp);
         assert qcode >= 0;
         seqmatches += updateActiveIntervals(tp, qcode, maxseqmatches - seqmatches,
               thefilter.isFiltered(qcode));
         if (seqmatches > maxseqmatches) {
            symremaining = 0;
            tp = (int) tssp[seqnum];
            toomanyhits.set(seqnum, true);
         }

         // (3) repeatedly process current position p
         while (symremaining >= q) {
            // (3a) Status
            while (tp >= nextslice) {
               g.logmsg("  %2d%% done, %.1f sec, pos %d/%d, seq %d/%d%n", percentdone,
                     timer.tocs(), tp, tn - 1, seqnum, tssp.length - 1);
               percentdone += slicefreq;
               nextslice += slicesize;
            }
            // (3b) update q-gram
            tp++;
            symremaining--;
            if (symremaining >= q) {
               qcode = coder.codeUpdate(qcode, t[tp + q - 1]);
               assert (qcode >= 0);
               seqmatches += updateActiveIntervals(tp, qcode, maxseqmatches - seqmatches,
                     thefilter.isFiltered(qcode));
               if (seqmatches > maxseqmatches) {
                  symremaining = 0;
                  tp = (int) tssp[seqnum];
                  toomanyhits.set(seqnum, true);
               }
            }
         } // end (3) while loop

         // (4) done with this block of positions. Go to next.
      }
      assert (seqnum == tssp.length && tp == tn);
   }

   private void regularMatchLength(int sp, int tp, int[] ret) {
      int len = q;
      while (s[sp + len] == t[tp + len] && amap.isSymbol(s[sp])) {
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
    * Compares sequences s and t, allowing bisulfite replacements.
    * 
    * @param sp
    *           start index in s
    * @param tp
    *           start index in t
    * @return length of match
    * 
    * @deprecated this is too slow and if it's used then partially unmodified reads cannot be found
    *             TODO is that really true?
    */
   private int[] bisulfiteMatchLength(int sp, int tp) {
      int ga = 2; // 0: false, 1: true, 2: maybe/unknown
      int ct = 2;

      int offset = 0;
      while (true) {
         if (!amap.isSymbol(s[sp + offset]))
            break;

         // What follows is some ugly logic to find out what type
         // of match this is. That is, whether we should allow C -> T or
         // G -> A replacements.
         // For C->T, the rules are:
         // If there's a C->T replacement, we must only allow those.
         // If there's a C not preceding a G that has not been replaced
         // by a T, then we must not allow C->T replacements.

         if (s[sp + offset] == NUCLEOTIDE_G && t[tp + offset] == NUCLEOTIDE_A) {
            if (ct == 1 || ga == 0)
               break;
            else
               ga = 1; // must have G->A
         } else if (offset > 0 && s[sp + offset - 1] != NUCLEOTIDE_C
               && t[tp + offset - 1] != NUCLEOTIDE_C && s[sp + offset] == NUCLEOTIDE_G
               && t[tp + offset] == NUCLEOTIDE_G) {
            if (ga == 1)
               break;
            else
               ga = 0; // not G->A
         }

         else if (s[sp + offset] == NUCLEOTIDE_C && t[tp + offset] == NUCLEOTIDE_T) {
            if (ct == 0 || ga == 1)
               break;
            else
               ct = 1; // must have C->T
         } else if (sp + offset + 1 < s.length && tp + offset + 1 < t.length
               && s[sp + offset + 1] != NUCLEOTIDE_G &&
               /* t[tp+offset+1] != NUCLEOTIDE_G && */
               s[sp + offset] == NUCLEOTIDE_C && t[tp + offset] == NUCLEOTIDE_C) {
            if (ct == 1)
               break;
            else
               ct = 0; // not C->T
         } else {
            if (s[sp + offset] != t[tp + offset])
               break;
         }
         offset++;
      }
      assert offset >= q;
      return new int[] { offset, 0 };
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

      while (amap.isSymbol(s[sstop]) && s[sstop] == t[tstop]) {
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
            while (sstart > 0 && tstart > seqstart && amap.isSymbol(s[sstart - 1])
                  && s[sstart - 1] == t[tstart - 1]) {
               sstart--;
               tstart--;
            }
            if (sstart == 0 || tstart == seqstart || !amap.isSymbol(s[sstart - 1])) {
               assert seqstart == 0 || !amap.isSymbol(t[seqstart - 1]);
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
      while (amap.isSymbol(s[sstop])
            && (s[sstop] == t[tstop] || (s[sstop] == nucleotide_s && t[tstop] == nucleotide_t))) {
         sstop++;
         tstop++;
      }

      // ... and possibly to the left
      // TODO it may make sense to do this even if stride==1
      if (stride > 1) {
         while (sstart > 0
               && tstart > seqstart
               && amap.isSymbol(s[sstart - 1])
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
   private final int updateActiveIntervals(final int tp, final int qcode, final int maxmatches,
         final boolean filtered) {

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
            if (bisulfite) {
               // FIXME cmc is ignored
               bisulfiteMatchLengthCmC(currentpos[ci], tp, match);
            } else {
               regularMatchLength(currentpos[ci], tp, match);
            }
            final int sstart = match[0];
            final int tstart = match[1];
            final int len = match[2];

//            System.out.printf("matchleng: sstart=%d tstart=%d len=%d%n", sstart, tstart, len);
            if (len >= minlen) {
               reportMatch(sstart, tstart, len);
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

//      System.out.printf("active at tp=%d --- ", tp);
//      for (int m = 0; m < ni; ++m) {
//         System.out.format("len=%d %d %d (dg %d) ", newlen[m], newpos[m], newpos[m] - newdiag[m],
//               newdiag[m]);
//      }
//      System.out.println();

      
      // }
      // // decrease length of active matches, as long as they stay >= q TODO >= minlen?
      // int ai;
      // for (ai=0; ai<active; ai++) {
      // activepos[ai]++;
      // if (activelen[ai]>q) activelen[ai]--;
      // else activelen[ai]=0;
      // }
      //    
      // // If this q-gram is filtered, discard matches that are too short from
      // // activepos and activelen, and return.
      // if (filtered) {
      // int ni = 0;
      // for(ai = 0; ai<active; ai++) {
      // if (activelen[ai]<q) continue;
      // assert(ni<=ai);
      // activepos[ni] = activepos[ai];
      // activelen[ni] = activelen[ai];
      // ni++;
      // }
      // active=ni;
      // return;
      // }
      // // this q-gram is not filtered!
      //
      // qgramindex.getQGramPositions(qcode, newpos);
      // final int newactive = qgramindex.getBucketSize(qcode); // number of new active q-grams
      //
      // // iterate over all new matches
      // ai=0;
      // int[] match = {0, 0, 0};
      // for (int ni=0; ni<newactive; ni++) {
      // while (ai<active && activelen[ai]<q) ai++;
      //      
      // if (c_matches_c) {
      // // we must skip those matches that are only active because
      // // of the 'C matches C' rule. They don't have q-grams
      // // in common with the query anymore
      // while (ai < active && newpos[ni] > activepos[ai])
      // ai++;
      // }
      // // make sure that newly found q-grams overlap the old ones (unless c_matches_c)
      // assert ai==active || newpos[ni]<=activepos[ai]
      // : String.format("tp=%d, ai/active=%d/%d, ni=%d, newpos=%d, activepos=%d, activelen=%d",
      // tp,ai,active,ni,newpos[ni],activepos[ai], activelen[ai]);
      // assert ai <= active;
      // if (ai==active || newpos[ni] < activepos[ai]) {
      // // this is a new match:
      // // determine newlen[ni] by comparing s[sp...] with t[tp...]
      // int sstart;
      // int tstart;
      // int matchlength;
      // if (!bisulfite) {
      // // FIXME take stride into account! (search backwards)
      // sstart = newpos[ni] + q;
      // matchlength = q;
      // while (s[sstart]==t[tp+matchlength] && amap.isSymbol(s[sstart])) {
      // sstart++;
      // matchlength++;
      // }
      // sstart -= matchlength; // go back to start of match
      // assert sstart == newpos[ni];
      // tstart = tp;
      // } else {
      // sstart = newpos[ni];
      // // int [] match = c_matches_c ? bisulfiteMatchLengthCmC(matchstart, tp) :
      // bisulfiteMatchLength(matchstart, tp);
      // bisulfiteMatchLengthCmC(sstart, tp, match); // : bisulfiteMatchLength(matchstart, tp);
      // sstart = match[0];
      // tstart = match[1];
      // matchlength = match[2];
      // }
      // newlen[ni] = matchlength;
      // newpos[ni] = sstart;
      // // maximal match (tp, sp, offset), i.e. ((seqnum,tp-seqstart), (i,sss), offset)
      // if (matchlength >= minlen) {
      // reportMatch(sstart, tstart, matchlength);
      // }
      // } else { // this is an old (continuing) match
      // newlen[ni] = activelen[ai];
      // ai++;
      // }
      // if (seqmatches > maxseqmatches) break;
      // }

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
   private void reportMatch(int sstart, final int tstart, int matchlength) {
      int i = seqindex(sstart);
      int ttt = tstart - seqstart;
      int sss = sstart - (i == 0 ? 0 : (int) ssp[i - 1] + 1);
      if (sorted) {
         matches.get(i).add(new Match(ttt, sss, matchlength));
      } else {
         if (!selfcmp || sstart > tstart)
            globalmatches.add(new GlobalMatch(ttt, i, sss, matchlength));
      }
      // System.out.format("reportMatch. i: %d. sss: %d. ttt: %d. matchlen: %d%n", i, sss, ttt,
      // matchlength);
   }

   private int seqindex(final int p) {
      int si = java.util.Arrays.binarySearch(ssp, p);
      if (si >= 0)
         return si; // return the index of the ssp position
      return (-si - 1); // we are in a sequence, return the index of the following ssp position
   }

   private void writeAndClearMatches(int seqnum) {
      ArrayList<Match> mi = null;
      long total = 0;
      int mseq = 0;
      int ms;
      out.printf(">%d:'''%s'''%n", seqnum, tdesc.get(seqnum));
      for (int i = 0; i < sm; i++) { // sm is global := number of sequences in index!
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
         mi.clear(); // clear match list
      }
      out.printf("<%d: %d %d%n%n", seqnum, mseq, total);
   }

   /**
    * writes the list of matches in current target sequence against whole index. clears list of
    * matches.
    */
   private void writeAndClearGlobalMatches(int seqnum) {
      if (globalmatches.size() == 0)
         return;
      if (globalmatches.size() < minseqmatches) {
         globalmatches.clear();
         return;
      }
      if (globalmatches.size() > maxseqmatches) {
         // g.logmsg("qmatch: Sequence %d has too many (>=%d/%d) matches, skipping output%n",
         // seqnum, globalmatches.size(), maxseqmatches);
         globalmatches.clear();
      } else {
         for (GlobalMatch gm : globalmatches) {
            out.printf("%d %d %d %d %d %d%n", seqnum, gm.tpos, gm.sseqnum, gm.spos, gm.len,
                  (long) gm.spos - gm.tpos);
            // (sequence number, sequence position, index sequence number, index sequence position,
            // length, diagonal)
         }
         globalmatches.clear();
      }
   }

   /** simple structure for sorted matches, per index sequence */
   private class Match {
      final int tpos;
      final int spos;
      final int len;

      public Match(final int tpos, final int spos, final int len) {
         this.tpos = tpos;
         this.spos = spos;
         this.len = len;
      }
   }

   /** simple structure for unsorted (global) matches */
   private class GlobalMatch {
      final int tpos;
      final int sseqnum;
      final int spos;
      final int len;

      public GlobalMatch(final int tpos, final int sseqnum, final int spos, final int len) {
         this.tpos = tpos;
         this.sseqnum = sseqnum;
         this.spos = spos;
         this.len = len;
      }
   }
}

// /** exception thrown if too many hits occur */
// class TooManyHitsException extends Exception {
// private static final long serialVersionUID = -1841832699464945659L;
// }
