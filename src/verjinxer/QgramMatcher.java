/*
 * QgramMatcher.java
 * Created on 25. April 2007, 12:32
 *
 * Sample usage:
 *
 * To map a 454 sequencing run ('run1') against human chromosome 1:
 * ... qmatch -l 25 -M 15 -f 2:0 -t #  run1  c01
 * We expect a perfect read of length 100.  This should give rise to at least one perfect match of length >= 25.
 * We expect a unique location on the genome. This should give rise to at most 15 hits of length >= 25.
 * We do not allow hits to start a degenerate q-grams that consist  of only 2 different nucleotides.
 * Running hits can extend beyond these, however.
 * We keep track of the sequences that have already been identified as repeats (ie, have too many hits). 
 *
 */

package verjinxer;

import java.io.*;
import java.nio.*;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Properties;
import verjinxer.sequenceanalysis.*;
import verjinxer.util.*;
import static verjinxer.Globals.*;


/**
 *
 * @author Sven Rahmann
 */
public class QgramMatcher {
      
  private Globals g;
  
  /** Creates a new instance of QgramMatcher 
   * @param gl the Globals structure
   */
  public QgramMatcher(Globals gl) {
    g = gl;
  }
  
  /**
   * print help on usage
   */
  public void help() {
    g.logmsg("Usage:%n  %s qmatch  [options]  [<sequences>]  <index>%n", programname);
    g.logmsg("Reports all maximal matches at least as long as a given length >=q,%n");
    g.logmsg("in human-readable output format. Writes .matches or .sorted-matches.%n");
    g.logmsg("Options:%n");
    g.logmsg("  -l, --length <len>   minimum match length [q of q-gram-index]%n");
    g.logmsg("  -s, --sort           write matches sorted per index sequence%n");
    g.logmsg("  -m, --min    <min>   show matches only if >=min (per index seq if sorted)%n");
    g.logmsg("  -M, --max    <max>   stop considering sequences at max+1 matches%n");
    g.logmsg("  -f, --filter <c:d>   apply q-gram filter <complexity:delta>%n");
    g.logmsg("  --self               compare index against itself%n");
    g.logmsg("  -t, --tmh <filename> name of too-many-hits-filter file (use #)%n");
    g.logmsg("  -o, --out <filename> specify output file (use # for stdout)%n");
    g.logmsg("  -x, --external       save memory at the cost of lower speed%n");
  }
  
  /** if run independently, call main
   *@param args (ignored)
   */
  public static void main(String[] args) {
    new QgramMatcher(new Globals()).run(args);
  }

  
  /* Variables */
  boolean     external    = false;
  boolean     sorted      = false;
  int         minmatch    = 1;      // min number of matches for output
  boolean     selfcmp     = false;  // comparing text against itself?
  int         minlen      = 0;      // min. match length
  int         q           = 0;      // q-gram length
  int         asize       = 0;      // alphabet size
  QGramCoder  coder       = null;   // the q-gram coder
  AlphabetMap amap        = null;   // the alphabet map
  
  byte[]      t           = null;   // the target sequence text (coded)
  int         tn          = 0;      // length of t
  int[]       tssp        = null;   // sequence separator positions in text t
  int         tm          = 0;      // number of sequences in t
  ArrayList<String> tdesc = null;   // sequence descriptions of t
  
  int         tp          = 0;      // current position in t
  int         seqstart    = 0;      // starting pos of current sequence in t
  int         seqnum      = 0;      // number of current sequence in t
  PrintWriter out         = null;        
  
  byte[]      s           = null;   // the index text (coded)
  int[]       qbck        = null;   // bucket boundaries
  int[]       ssp         = null;   // sequence separator positions in index s
  int         sm          = 0;      // number of sequences in s
  ArrayList<String> sdesc = null;   // sequence descriptions of s
  IntBuffer   qpos        = null;   // q-gram positions as buffer
  int[]       qposa       = null;   // q-gram positions as array
  
  int     active    = 0;            // number of active matches
  int[]   activepos = null;         // starting positions of active matches in s
  int[]   newpos    = null;         // starting positions of new matches in s
  int[]   lenforact = null;         // match lengths for active matches
  int[]   lenfornew = null;         // match lengths for new matches
  int     seqmatches= 0;            // number of matches in current target sequence
  int     maxseqmatches = -1;       // maximum number of allowed matches
  
  ArrayList<ArrayList<Match>> matches = null; // list for sorted matches
  ArrayList<GlobMatch>   globmatches  = null; // list for unsorted matches
 
  /**
   * @param args the command line arguments
   * @return zero on successful completion, nonzero value otherwise.
   */
  public int run(String[] args) {
    TicToc totalTimer = new TicToc();
    g.cmdname = "qmatch";
    // t: text to find
    // s: sequence in which to search
    String tname, sname, dt, ds;
    
    Options opt = new Options
        ("l=length:,s=sort=sorted,o=out=output:,x=external,m=min=minmatch=minmatches:,M=max:,f=filter:,t=tmh:,self");
    try {
      args = opt.parse(args);
    } catch (IllegalOptionException ex) {
      g.terminate("qmatch: "+ex.toString());
    }
    
    selfcmp = opt.isGiven("self");
    if (args.length<1 || (args.length==1 && !selfcmp)) {
      help(); g.terminate("qmatch: need both <sequences> and <index>, or --self and <index>!");  
    } 
    if (args.length==1) {
      assert(selfcmp);
      tname = args[0];
      sname = tname;
    } else { 
      assert(args.length>=2);
      if (selfcmp) g.logmsg("qmatch: using --self with indices will suppress symmetric matches%n");
      tname = args[0];
      sname = args[1];
      if (selfcmp && !tname.equals(sname)) g.warnmsg("qmatch: using --self, but %s != %s%n", tname,sname);
    }
    dt = g.dir + tname;
    ds = g.dir + sname;
    g.startplog(ds+extlog);
    
    // Read project data and determine asize, q; read alphabet map
    Properties prj = g.readProject(ds+extprj);
    try {
      asize = Integer.parseInt(prj.getProperty("qAlphabetSize"));
      q = Integer.parseInt(prj.getProperty("q"));
    } catch (NumberFormatException ex) {
      g.warnmsg("qmatch: q-grams for index '%s' not found. (Re-create the q-gram index!)%n", ds);
      g.terminate(1);
    }
    coder = new QGramCoder(q,asize);
    amap  = g.readAlphabetMap(ds+extalph);
    
    // Prepare the q-gram filter from -f option
    final BitSet thefilter = coder.createFilter(opt.get("f")); // empty filter if null

    // Prepare the sequence filter
    maxseqmatches = Integer.MAX_VALUE;
    if (opt.isGiven("M")) maxseqmatches = Integer.parseInt(opt.get("M"));
    
    BitSet toomanyhits = null;
    if (opt.isGiven("t")) {
      if (opt.get("t").startsWith("#")) {
        toomanyhits = g.readFilter(dt+".toomanyhits-filter", tm);
      } else {
        toomanyhits = g.readFilter(g.dir + opt.get("t"), tm);
      }
    } else { // -t not given ==> de-select all (clean start)
      toomanyhits = new BitSet(tm);
    }
    
    // Determine option values
    external  = (opt.isGiven("x"));
    sorted = (opt.isGiven("s"));
    minlen = (opt.isGiven("l")? Integer.parseInt(opt.get("l")) : q);
    if (minlen<q) {
      g.warnmsg("qmatch: increasing minimum match length to q=%d!%n",q);
      minlen=q;
    }
    minmatch = (opt.isGiven("m")? Integer.parseInt(opt.get("m")) : 1);
    if (minmatch<1) {
      g.warnmsg("qmatch: increasing minimum match number to 1!%n");
      minmatch=1;
    }  
    
    String outname = String.format("%s-%s-%dx%d",tname, sname, minmatch, minlen);
    if (opt.isGiven("o")) {
      if (outname.length()==0 || outname.startsWith("#"))  outname = null;
      else outname=opt.get("o");
    }
    if (outname!=null)  outname = g.outdir + outname + (sorted? ".sorted-matches" : ".matches");
    
    // end of option parsing

    openFiles(dt, ds, outname);
    int maxactive = Integer.parseInt(prj.getProperty("qbckMax"));
    match(maxactive, thefilter, toomanyhits);
    out.close();
    g.logmsg("qmatch: too many hits for %d/%d sequences (%.2f%%)%n", 
        toomanyhits.cardinality(),tm, 100.0*(double)toomanyhits.cardinality()/tm);
    g.writeFilter(dt+".toomanyhits-filter", toomanyhits);
    g.logmsg("qmatch: done; total time was %.1f sec%n", totalTimer.tocs());
    g.stopplog();

    return 0;
  } // end run()

  /**
   * 
   * @param maxactive
   * @param thefilter
   * @param toomanyhits gets modified
   */
  private void match(int maxactive, final BitSet thefilter, BitSet toomanyhits) {
    // Walk through t:
    // (A) Initialization
    TicToc timer = new TicToc();
    final int slicefreq = 5;
    final int slicesize = 1+(slicefreq*tn/100);
    int nextslice = 0;
    int percentdone = 0;
    int symremaining = 0;
    int qcode;
    
    activepos = new int[maxactive];  active=0;
    newpos    = new int[maxactive];
    lenforact = new int[maxactive];
    lenfornew = new int[maxactive];
    if (sorted) {
      matches = new ArrayList<ArrayList<Match>>(sm);
      for (int i=0; i<sm; i++) matches.add(i, new ArrayList<Match>(32));
    } else {
      globmatches = new ArrayList<GlobMatch>(maxseqmatches<127? maxseqmatches+1: 128);
    }
    
    
    // (B) Walking ...
    seqstart = 0;
    seqnum = 0;
    tp = 0;
    seqmatches = 0;
    while (tp < tn) {
      
      // (1) Determine next valid position p in t with potential match
      if (symremaining<minlen) {   // next invalid is possibly at tp+symremaining
        tp += symremaining; symremaining=0;
        for (; tp<tn && (!amap.isSymbol(t[tp]));  tp++) {
          if (amap.isSeparator(t[tp])) {
            assert(tp==tssp[seqnum]);
            if (sorted) writeMatches(); else writeGlobMatches();
            seqnum++;  seqstart = tp+1;  seqmatches = 0;
          }
        }
        if (tp>=tn) break;
        if (toomanyhits.get(seqnum)) {
          symremaining=0;
          tp=tssp[seqnum];
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
      qcode = coder.code(t,tp);
      assert(qcode>=0);
      try {
        findactive(qcode, thefilter.get(qcode)); // updates active, activepos, lenforact
      } catch (TooManyHitsException ex) {
          symremaining=0; tp = tssp[seqnum]; toomanyhits.set(seqnum);
      }
      
      // (3) repeatedly process current position p
      while (symremaining >=minlen) {
        // (3a) Status
        while(tp>=nextslice) {
          g.logmsg("  %2d%% done, %.1f sec, pos %d/%d, seq %d/%d%n",  percentdone, timer.tocs(), tp, tn-1, seqnum, tm-1);
          percentdone += slicefreq;  nextslice += slicesize;
        }
        // (3b) update q-gram
        tp++; symremaining--;
        if (symremaining>=minlen) {
          qcode = coder.codeUpdate(qcode, t[tp+q-1]);
          assert(qcode>=0);
          try {
            findactive(qcode, thefilter.get(qcode));
          } catch (TooManyHitsException ex) {
            symremaining=0; tp = tssp[seqnum]; toomanyhits.set(seqnum);
          }
        }
      } // end (3) while loop
      
      // (4) done with this block of positions. Go to next.
    }
    assert(seqnum==tm && tp==tn);    
  }

  /**
   * global variables written to in this method:
   * t
   * tn
   * tssp
   * tm
   * tdesc
   * s
   * ssp
   * sm
   * sdesc
   * qbck
   * qpos bzw. qposa
   * 
   * out
   */
  private void openFiles(String dt, String ds, String outname) {
    // Read text, text-ssp, seq, qbck, ssp into arrays;  
    // read sequence descripitions;
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
    tn   = t.length;
    tssp = g.slurpIntArray(tsspfile);
    tm   = tssp.length;
    tdesc= g.slurpTextFile(dt+extdesc, tm);
    assert(tdesc.size()==tm);
    if (dt.equals(ds)) {
      s=t; ssp=tssp; sm=tm; sdesc=tdesc;
    } else {
      s    = g.slurpByteArray(seqfile);
      ssp  = g.slurpIntArray(sspfile);
      sm   = ssp.length;
      sdesc= g.slurpTextFile(ds+extdesc, sm);
      assert(sdesc.size()==sm);
    }
    qbck = g.slurpIntArray(qbckfile);
    if (external) qpos  = g.mapRIntArray(qposfile);
    else          qposa = g.slurpIntArray(qposfile);
    g.logmsg("qmatch: mapping and reading files took %.1f sec%n", ttimer.tocs());


    // start output
    out = new PrintWriter(System.out);
    if (outname!=null) {
      try {
        out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(outname),32*1024), false);
      } catch (FileNotFoundException ex) {
        g.terminate("qmatch: could not create output file. Stop.");
      }
    }
    g.logmsg("qmatch: will write results to %s%n", (outname!=null? "'"+outname+"'" : "stdout"));
  }
  
  
  
  private final void findactive(final int qcode, final boolean filtered) 
  throws TooManyHitsException {
    final int r = qbck[qcode];
    final int newactive = qbck[qcode+1] - r;
    int ai, ni, offset;
    
    //g.logmsg("  spos=%d, qcode=%d (%s),  row=%d.  rank=%d%n", sp, qcode, coder.qGramString(qcode,amap), lrmmrow, r);
    
    // decrease the length of the active matches, as long as they stay >= q
    for(ai=0; ai<active; ai++) { 
      activepos[ai]++;  
      if (lenforact[ai]>q) lenforact[ai]--; else lenforact[ai]=0; 
    }
    
    if (filtered)
    {
      for(ni=0, ai=0; ai<active; ai++) {
        if (lenforact[ai]<q) continue;
        assert(ni<=ai);
        activepos[ni] = activepos[ai];
        lenforact[ni] = lenforact[ai];
        ni++;
      }
      active=ni;
      return;
    }
    
    // this q-gram is not filtered!
    // copy starting positions of current matches positions into 'newpos'
    if (external) { qpos.position(r);  qpos.get(newpos, 0, newactive); } 
    else          { System.arraycopy(qposa, r, newpos, 0, newactive);  }
    //g.logmsg("    qpos = [%s]%n", Strings.join(" ",newpos, 0, newactive));
 
    ai=0;
    for(ni=0;  ni<newactive;  ni++) {
      while(ai<active && lenforact[ai]<q) ai++; 
      assert(ai==active || newpos[ni]<=activepos[ai])
      : String.format("spos=%d, ai/active=%d/%d, ni=%d, newpos=%d, activepos=%d, lenforact=%d",
          tp,ai,active,ni,newpos[ni],activepos[ai], lenforact[ai]);
      if (ai>=active || newpos[ni]!=activepos[ai]) { 
        // this is a new match:
        // determine lenfornew[ni] by comparing s[sp...] with t[tp...]
        int sp = newpos[ni] + q;
        for (offset = q;  ; sp++, offset++) {
          if ( !(s[sp]==t[tp+offset] && amap.isSymbol(s[sp])) ) break;
        }
        lenfornew[ni] = offset;
        sp -= offset;             // go back to start of match
        // maximal match (tp, sp, offset), i.e. ((seqnum,tp-seqstart), (i,sss), offset)
        if (offset>=minlen) { // report match
          int i = seqindex(newpos[ni]);
          int ttt = tp - seqstart;
          int sss = sp - (i==0? 0 : ssp[i-1]+1);
          if (sorted) { 
            matches.get(i).add(new Match(ttt, sss, offset)); 
          } else { 
            if (!selfcmp || sp>tp) globmatches.add(new GlobMatch(ttt, i, sss, offset));
          }
          seqmatches++;
        }
      } else { // this is an old (continuing) match
        lenfornew[ni] = lenforact[ai];
        ai++;
      }
      if (seqmatches > maxseqmatches) break;
    }
    
    // swap activepos <-> newpos  and  lenforact <-> lenfornew
    int[] tmp;
    tmp = activepos;  activepos = newpos;    newpos    = tmp;
    tmp = lenforact;  lenforact = lenfornew; lenfornew = tmp;
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
      if (ms>=minmatch*minlen) {
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
  private void writeGlobMatches() {
    if (globmatches.size()==0) return;
    if (globmatches.size()<minmatch) { globmatches.clear(); return; }
    if (globmatches.size()>maxseqmatches) {
      //g.logmsg("qmatch: Sequence %d has too many (>=%d/%d) matches, skipping output%n", seqnum, globmatches.size(), maxseqmatches);
      globmatches.clear();
      return;
    }    
    for(GlobMatch gm : globmatches) {
      out.printf("%d %d %d %d %d %d%n", seqnum, gm.tpos, gm.sseqnum, gm.spos, gm.len, (long)gm.spos-gm.tpos); 
      // (sequence number, sequence position, index sequence number, index sequence position, length, diagonal)      
    }
    globmatches.clear();
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
   private class GlobMatch {
     final int tpos;
     final int sseqnum;
     final int spos;
     final int len;
     public GlobMatch(final int tpos, final int sseqnum, final int spos, final int len) {
       this.tpos=tpos; this.sseqnum=sseqnum; this.spos=spos; this.len=len;
     }
   }
  
   
// end class
}

 /** exception thrown if too many hits occur */
class TooManyHitsException extends Exception { }

