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
      
  boolean     sorted      = false;

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
  
  /** the target sequence text (coded) */
   final byte[] t;
  
  /** sequence separator positions in text t */
   final long[] tssp;
  
  // int tm; number of sequences in t
  
  /** sequence descriptions of t */
   final ArrayList<String> tdesc; 
  
  /** Positions of all q-grams */
   final QGramIndex qgramindex;

   final PrintWriter out;
  
   /** the index text (coded) */
   final byte[] s;
  
   /** sequence separator positions in index s */
   final long[] ssp;
   
   /** number of sequences in s */
   final int sm;
   
   /** number of sequences in s */
   final ArrayList<String> sdesc;
   
   /** maximum number of allowed matches */
   final int maxseqmatches;
   
   /** list for sorted matches */
   final ArrayList<ArrayList<Match>> matches; 
   
   /** list for unsorted matches */
   final ArrayList<GlobalMatch> globalmatches; 
   
   final BitArray toomanyhits;
   
   final boolean bisulfite; // whether the index is for bisulfite sequences
   final boolean c_matches_c; // whether C matches C, even if not before G

  int     active    = 0;            // number of active matches
  int[]   activepos = null;         // starting positions of active matches in s
  int[]   newpos    = null;         // starting positions of new matches in s
  int[]   activelen = null;         // match lengths for active matches
  int[]   newlen = null;         // match lengths for new matches
  int     seqmatches= 0;            // number of matches in current target sequence
// int         tp          = 0;      // current position in t
   int seqstart = 0;      // starting pos of current sequence in t
   int seqnum = 0;      // number of current sequence in t
  
   /** Creates a new instance of QgramMatcherSubcommand 
    * @param gl the Globals structure
   * @param args the command line arguments
    * @param toomanyhits may be null
   */
   public QgramMatcher(
         Globals g,
         String dt, 
         String ds, 
         String toomanyhitsfilename,
         int maxseqmatches, 
         int minseqmatches, 
         int minlen,
         int maxactive,
         final QGramCoder qgramcoder,
         final QGramFilter qgramfilter,
         final PrintWriter out,
         final boolean sorted, 
         final boolean external,
         final boolean selfcmp, 
         final boolean bisulfite,
         final boolean c_matches_c
         ) 
   {
     this.g = g;
     this.selfcmp = selfcmp;
     this.bisulfite = bisulfite;
     this.asize = qgramcoder.asize;
     this.q = qgramcoder.q;
     this.out = out;
     this.c_matches_c = c_matches_c;
    
    amap  = g.readAlphabetMap(ds+extalph);
    
    //final BitSet thefilter = coder.createFilter(opt.get("F")); // empty filter if null
    if (minlen<q) {
      g.warnmsg("qmatch: increasing minimum match length to q=%d!%n",q);
      minlen=q;
    }
     this.minlen = minlen;
     
    if (minseqmatches<1) {
      g.warnmsg("qmatch: increasing minimum match number to 1!%n");
      minseqmatches=1;
    }  
     this.minseqmatches = minseqmatches;
     this.maxseqmatches = maxseqmatches;
     if (sorted) {
        matches = new ArrayList<ArrayList<Match>>();
        globalmatches = null;
      } else {
        globalmatches = new ArrayList<GlobalMatch>(maxseqmatches<127? maxseqmatches+1: 128);
        matches = null;
    }

    if (c_matches_c) g.logmsg("qmatch: C matches C, even if no G follows%n");
    else g.logmsg("qmatch: C matches C only before G%n");

  /**
      * variables written to in the following
   * t
   * tn
   * tssp
   * tm
   * tdesc
   * s
   * ssp
   * sm
   * sdesc
   * 
   * out
   */
/*     private void openFiles(String dt, String ds, String outname, boolean external) {
 */      // Read text, text-ssp, seq, qbck, ssp into arrays;  
    // read sequence descriptions;
    // memory-map or read qpos.
    TicToc ttimer = new TicToc();
    final String tfile    = dt+extseq;
    final String tsspfile = dt+extssp;
    final String seqfile  = ds+extseq;
    final String qbckfile = ds+extqbck;
    final String sspfile  = ds+extssp;
    final String qposfile = ds+extqpos;
    System.gc();
    t    = g.slurpByteArray(tfile);
    tssp = g.slurpLongArray(tsspfile);
    tdesc= g.slurpTextFile(dt+extdesc, tssp.length);
    assert(tdesc.size()==tssp.length);

    if (dt.equals(ds)) {
      s=t; ssp=tssp; sm=tssp.length; sdesc=tdesc;
    } else {
      s    = g.slurpByteArray(seqfile);
      ssp  = g.slurpLongArray(sspfile);
      sm   = ssp.length;
      sdesc= g.slurpTextFile(ds+extdesc, sm);
      assert(sdesc.size()==sm);
    }
    
    qgramindex = new QGramIndex(g, qposfile, qbckfile, external);
    
    g.logmsg("qmatch: mapping and reading files took %.1f sec%n", ttimer.tocs());
  
    //toomanyhits = new  BitArray toomanyhits;
     if (toomanyhitsfilename != null) {
        toomanyhits = g.slurpBitArray(toomanyhitsfilename);
     } else {
        toomanyhits = new BitArray(tssp.length); // if -t not given, start with a clean filter
      }
    }
   
   public void tooManyHits(String filename) {
      g.logmsg("qmatch: too many hits for %d/%d sequences (%.2f%%)%n", 
            toomanyhits.cardinality(), tssp.length, toomanyhits.cardinality()*100.0/tssp.length);
        g.dumpBitArray(filename, toomanyhits);
  }

  /**
   * 
   * @param maxactive
   * @param thefilter
   * @param toomanyhits gets modified
   * @param c_matches_c whether C always matches C, even if not before G
   */
   public void match(QGramCoder coder, int maxactive, final QGramFilter thefilter) {
    // Walk through t:
    // (A) Initialization
    TicToc timer = new TicToc();
    
    activepos = new int[maxactive];  active=0;
    newpos    = new int[maxactive];
    activelen = new int[maxactive];
    newlen = new int[maxactive];
    if (sorted) {
       matches.ensureCapacity(sm);
       for (int i=0; i<sm; i++) 
          matches.add(i, new ArrayList<Match>(32));
    } else {
       globalmatches.ensureCapacity(maxseqmatches<127? maxseqmatches+1: 128);
    }
    
    // (B) Walking ...
    int tn = t.length;
    final int slicefreq = 5;
    final int slicesize = 1+(slicefreq*tn/100);
    int nextslice = 0;
    int percentdone = 0;
    int symremaining = 0;
    int qcode;

    seqstart = 0;
    seqnum = 0;
    int tp = 0; // current position in t
    seqmatches = 0;
    while (tp < tn) {
      
      // (1) Determine next valid position p in t with potential match
      if (symremaining<minlen) {   // next invalid is possibly at tp+symremaining
        tp += symremaining;
        symremaining = 0;
        for ( ; tp<tn && (!amap.isSymbol(t[tp])); tp++) {
          if (amap.isSeparator(t[tp])) {
            assert(tp==tssp[seqnum]);
            if (sorted) writeMatches();
            else writeGlobalMatches();
            seqnum++;
            seqstart = tp+1; 
            seqmatches = 0;
          }
        }
        if (tp>=tn) break;
        if (toomanyhits.get(seqnum)==1) {
          symremaining=0;
          tp=(int)tssp[seqnum];
          continue;
        }
        int i; // next valid symbol is now at p, count number of valid symbols
        for (i=tp; i<tn && amap.isSymbol(t[i]); i++) {}
        symremaining = i-tp;
        if (symremaining < minlen) continue;
      }
      assert(amap.isSymbol(t[tp]));
      assert(symremaining >= minlen);
      // g.logmsg("  position %d (in seq. %d, starting at %d): %d symbols%n", p, seqnum, seqstart, symremaining);
      
      // (2) initialize qcode and active q-grams
      active = 0;  // number of active q-grams
      qcode = coder.code(t, tp);
      assert(qcode>=0);
      try {
        findactive(tp, qcode, thefilter.getBoolean(qcode)); // updates active, activepos, lenforact
      } catch (TooManyHitsException ex) {
          symremaining=0; 
          tp = (int)tssp[seqnum]; 
          toomanyhits.set(seqnum, true);
      }
      
      // (3) repeatedly process current position p
      while (symremaining >=minlen) {
        // (3a) Status
        while(tp>=nextslice) {
          g.logmsg("  %2d%% done, %.1f sec, pos %d/%d, seq %d/%d%n",  percentdone, timer.tocs(), tp, tn-1, seqnum, tssp.length-1);
          percentdone += slicefreq;
          nextslice += slicesize;
        }
        // (3b) update q-gram
        tp++; symremaining--;
        if (symremaining>=minlen) {
          qcode = coder.codeUpdate(qcode, t[tp+q-1]);
          assert(qcode>=0);
          try {
            findactive(tp, qcode, thefilter.getBoolean(qcode));
          } catch (TooManyHitsException ex) {
            symremaining=0;
            tp = (int)tssp[seqnum];
            toomanyhits.set(seqnum, true);
          }
        }
      } // end (3) while loop
      
      // (4) done with this block of positions. Go to next.
    }
    assert(seqnum==tssp.length && tp==tn);    
  }

  /**
   * Compares sequences s and t, allowing bisulfite replacements.
   * @param sp start index in s
   * @param tp start index in t
   * @return length of match
   */
  private int bisulfiteMatchLength(int sp, int tp) {
     int ga = 2; // 0: false, 1: true, 2: maybe/unknown
     int ct = 2;
     
     int offset = 0;
     while (true) {
        if (!amap.isSymbol(s[sp+offset])) break;
        
        // What follows is some ugly logic to find out what type
        // of match this is. That is, whether we should allow C -> T or
        // G -> A replacements.
        // For C->T, the rules are:
        // If there's a C->T replacement, we must only allow those.
        // If there's a C not preceding a G that has not been replaced
        // by a T, then we must not allow C->T replacements.
        
        if (s[sp+offset] == NUCLEOTIDE_G && t[tp+offset] == NUCLEOTIDE_A) {
           if (ct == 1 || ga == 0) break;
           else ga = 1; // must have G->A
        }
        else if (offset > 0 && 
                s[sp+offset-1] != NUCLEOTIDE_C &&
                t[tp+offset-1] != NUCLEOTIDE_C &&
                s[sp+offset] == NUCLEOTIDE_G &&
                t[tp+offset] == NUCLEOTIDE_G) {
           if (ga == 1) break;
           else ga = 0; // not G->A
        }

        else if (s[sp+offset] == NUCLEOTIDE_C && t[tp+offset] == NUCLEOTIDE_T) {
           if (ct == 0 || ga == 1) break;
           else ct = 1; // must have C->T
        }
        else if (sp+offset+1 < s.length && tp+offset+1 < t.length &&
                s[sp+offset+1] != NUCLEOTIDE_G &&
                /*t[tp+offset+1] != NUCLEOTIDE_G &&*/
                s[sp+offset] == NUCLEOTIDE_C &&
                t[tp+offset] == NUCLEOTIDE_C) {
           if (ct == 1) break;
           else ct = 0; // not C->T
        } else {
           if (s[sp+offset] != t[tp+offset]) break;
        }
        offset++;
     }
     assert offset >= q;
     return offset;
  }

  /**
   * Compares sequences s and t, allowing bisulfite replacements.
   * Allows that a C matches a C, even if not before G (and that a
   * G matches G even if not after C).
   * @param sp start index in s
   * @param tp start index in t
   * @return length of match
   */
  private int bisulfiteMatchLengthCmC(int sp, int tp) {
     int type = 0; // 0: unknown. 1: C->T, 2: G->A
     
     int offset = 0;
     
     //while (!amap.isSymbol(s[sp+offset]) && s[sp+offset] == t[tp+offset]) offset++;
     while (true) {
        if (!amap.isSymbol(s[sp+offset])) break;
        
        // What follows is some ugly logic to find out what type
        // of match this is. That is, whether we should allow C -> T or
        // G -> A replacements.
        // For C->T, the rules are:
        // If there's a C->T replacement, we must only allow those.
        
        byte s_char = s[sp+offset];
        byte t_char = t[tp+offset];
        if (s_char == t_char || (type == 1 && s_char == NUCLEOTIDE_C && t_char == NUCLEOTIDE_T) || (type == 2 && s_char == NUCLEOTIDE_G && t_char == NUCLEOTIDE_A)) {
           offset++;
           continue;
        }
        if (type != 0) break;
        if (s_char == NUCLEOTIDE_C && t_char == NUCLEOTIDE_T)
           type = 1;
        else if (s_char == NUCLEOTIDE_G && t_char == NUCLEOTIDE_A)
           type = 2;
        else break;
        offset++;
     }
     assert offset >= q;
     return offset;
  }

  /*
   * writes to:
   * active
   * lenforact
   * activepos
   * 
   * lenfornew
   * 
   * does not write to
   * qbck
   * 
   * 
   */
  private final void findactive(final int tp, final int qcode, final boolean filtered) 
  throws TooManyHitsException {
    //g.logmsg("  spos=%d, qcode=%d (%s),  row=%d.  rank=%d%n", sp, qcode, coder.qGramString(qcode,amap), lrmmrow, r);
    
    // decrease length of active matches, as long as they stay >= q
    int ai;
    for (ai=0; ai<active; ai++) { 
      activepos[ai]++;  
      if (activelen[ai]>q) activelen[ai]--;
      else activelen[ai]=0; 
    }
    
    // If this q-gram is filtered, discard matches that are too short from
    // activepos and activelen, and return.
    if (filtered) {
      int ni = 0;
      for(ai = 0; ai<active; ai++) {
        if (activelen[ai]<q) continue;
        assert(ni<=ai);
        activepos[ni] = activepos[ai];
        activelen[ni] = activelen[ai];
        ni++;
      }
      active=ni;
      return;
    }
    //if (tp % 1000 == 0) g.logmsg("  findactive. tp=%d, newactive=%d%n", tp, newactive /*coder.qGramString(qcode,amap), lrmmrow, r*/);

    // this q-gram is not filtered!

    qgramindex.getQGramPositions(qcode, newpos);
    final int newactive = qgramindex.bucketSize(qcode); // number of new active q-grams
    
    // iterate over all new matches 
    ai=0;
    for (int ni=0; ni<newactive; ni++) {
      while (ai<active && activelen[ai]<q) ai++;
      
      // make sure that newly found q-grams overlap the old ones (unless c_matches_c)
      assert(c_matches_c || (ai==active || newpos[ni]<=activepos[ai]))
        : String.format("tp=%d, ai/active=%d/%d, ni=%d, newpos=%d, activepos=%d, activelen=%d",
          tp,ai,active,ni,newpos[ni],activepos[ai], activelen[ai]);
      if (ai>=active || newpos[ni]!=activepos[ai]) { 
        // this is a new match:
        // determine newlen[ni] by comparing s[sp...] with t[tp...]
        int sp;
        int offset;
        if (!bisulfite) {
          sp = newpos[ni] + q;
          offset = q;
          while (s[sp]==t[tp+offset] && amap.isSymbol(s[sp])) {
            sp++;
            offset++;
          }
          sp -= offset; // go back to start of match
        } else {
          sp = newpos[ni];
          offset = c_matches_c ? bisulfiteMatchLengthCmC(sp, tp) : bisulfiteMatchLength(sp, tp);
        }
        newlen[ni] = offset;
        // maximal match (tp, sp, offset), i.e. ((seqnum,tp-seqstart), (i,sss), offset)
        if (offset>=minlen) { // report match
          int i = seqindex(newpos[ni]);
          int ttt = tp - seqstart;
          int sss = sp - (i==0? 0 : (int)ssp[i-1]+1);
          if (sorted) { 
            matches.get(i).add(new Match(ttt, sss, offset)); 
          } else { 
            if (!selfcmp || sp>tp) globalmatches.add(new GlobalMatch(ttt, i, sss, offset));
          }
          seqmatches++;
        }
      } else { // this is an old (continuing) match
        newlen[ni] = activelen[ai];
        ai++;
      }
      if (seqmatches > maxseqmatches) break;
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
    
    // swap activepos <-> newpos  and  lenforact <-> lenfornew
    int[] tmp;
    tmp = activepos;  activepos = newpos;    newpos    = tmp;
    tmp = activelen;  activelen = newlen; newlen = tmp;
    active = newactive;
    if (seqmatches > maxseqmatches) throw new TooManyHitsException();
  }
  
   
  private int seqindex(final int p) {
    int si = java.util.Arrays.binarySearch(ssp, p);
    if (si>=0) return si; // return the index of the ssp position
    return(-si-1);        // we are in a sequence, return the index of the following ssp position
  }

  
  private void writeMatches() {
    ArrayList<Match> mi=null;
    long total = 0;
    int  mseq  = 0;
    int  ms;
    out.printf(">%d:'''%s'''%n", seqnum, tdesc.get(seqnum));
    for(int i=0; i<sm; i++) {  // sm is global := number of sequences in index!
      mi = matches.get(i);
      if (mi.size()==0) continue;
      ms = 0;
      for(Match mm : mi)  ms+=mm.len;
      if (ms>=minseqmatches*minlen) {
        total += mi.size();
        mseq++;
        out.printf("@%d:'''%s'''%n",i,sdesc.get(i));
        for (Match mm : mi)
          out.printf(". %d %d %d %d%n", mm.tpos, mm.spos, mm.len, (long)mm.spos-mm.tpos);
      }
      mi.clear(); // clear match list
    }
    out.printf("<%d: %d %d%n%n", seqnum, mseq, total);
  }
  
  /** write the list of matches in current target sequence against whole index */
  private void writeGlobalMatches() {
    if (globalmatches.size()==0) return;
    if (globalmatches.size()<minseqmatches) { globalmatches.clear(); return; }
    if (globalmatches.size()>maxseqmatches) {
      //g.logmsg("qmatch: Sequence %d has too many (>=%d/%d) matches, skipping output%n", seqnum, globalmatches.size(), maxseqmatches);
      globalmatches.clear();
      return;
    }    
    for(GlobalMatch gm : globalmatches) {
      out.printf("%d %d %d %d %d %d%n", seqnum, gm.tpos, gm.sseqnum, gm.spos, gm.len, (long)gm.spos-gm.tpos); 
      // (sequence number, sequence position, index sequence number, index sequence position, length, diagonal)      
    }
    globalmatches.clear();
  }
  
  
   /** simple structure for sorted matches, per index sequence */
   private class Match {
     final int tpos;
     final int spos;
     final int len;
     public Match(final int tpos, final int spos, final int len) {
       this.tpos=tpos; this.spos=spos; this.len=len;
     }
   }

    /** simple structure for unsorted (global) matches */
   private class GlobalMatch {
     final int tpos;
     final int sseqnum;
     final int spos;
     final int len;
     public GlobalMatch(final int tpos, final int sseqnum, final int spos, final int len) {
       this.tpos=tpos; this.sseqnum=sseqnum; this.spos=spos; this.len=len;
     }
   }
}

 /** exception thrown if too many hits occur */
class TooManyHitsException extends Exception {
  private static final long serialVersionUID = -1841832699464945659L;
}
