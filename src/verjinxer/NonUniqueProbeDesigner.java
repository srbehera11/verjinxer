/*
 * NonUniqueProbeDesigner.java
 *
 * Created on 15. April 2007, 15:34
 *
 */

package verjinxer;

import static verjinxer.Globals.programname;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.sequenceanalysis.InvalidSymbolException;
import verjinxer.sequenceanalysis.QGramCoder;
import verjinxer.sequenceanalysis.QGramIndex;
import verjinxer.util.ArrayFile;
import verjinxer.util.IllegalOptionException;
import verjinxer.util.Options;
import verjinxer.util.ProjectInfo;
import verjinxer.util.StringUtils;
import verjinxer.util.TicToc;

import com.spinn3r.log5j.Logger;

/**
 *
 * @author Sven Rahmann
 */
public class NonUniqueProbeDesigner {
  
  private final static Logger log = Globals.log;
  private Globals g;
  
  /** Creates a new instance of NonUniqueProbeDesigner
   * @param gl the globals object containing information about logging streams, etc.
   */
  public NonUniqueProbeDesigner(Globals gl) {
    g = gl;
  }
  
  /**
   * print help on usage
   */
  public void help() {
    log.info("Usage:  %s nonunique  [options] Indexname", programname);
    log.info("Finds non unique, but specific, probes within an index built with '--dnarc #',");
    log.info("writes output to .<zero>-<one>-<length>.nuprobes file.");
    log.info("Options:");
    log.info("  -l, --length   <length>    length of probes to be computed [70]");
    log.info("  -0, --zero     <cutoff>    highest lcf for which a probe is absent [14]");
    log.info("  -1, --one      <cutoff>    lowest lcf for which a probe is present [probelength]");
    log.info("  -d, --details              output detailed probe statistics (HUGE output!)");
    log.info("  -u, --unique   <number>    report only probes in <= number of seqs [all - 1]");
    log.info("  -f, --ufrac    <fraction>  report only probes in <= fraction of seqs");
    log.info("  -o, --output   <filename>  output file name (.nuprobes/.nustats is appended)");
    log.info("  --noskip                   output (don't skip) reverse complementary probes");
//    log.info("  -x, --external             save memory at the cost of lower speed");
  }
  
  /** if run independently, call main
   * @param args  the command line arguments
   */
  public static void main(String[] args) {
    new NonUniqueProbeDesigner(new Globals()).run(args);
  }
  
  
  
  /* Variables */
  boolean     dontdorc = false;
  boolean outputdetails= false;
  int         pl    = 0;     // probe length
  int         c0    = 0;     // zero-cutoff
  int         c1    = 0;     // one-cutoff
  int         q     = 0;     // q-gram length
  int         asize = 0;     // alphabet size
  QGramCoder  coder = null;  // the q-gram coder
  Alphabet alphabet  = null;  // the alphabet map
  byte[]      s     = null;  // the text (coded)
  int         n     = 0;     // length of s
  long[]      ssp   = null;  // sequence separator positions
  int         m     = 0;     // number of sequences
  PrintWriter out= null;
  QGramIndex qgramindex;
  
  /**
   * @param args the command line arguments
   * @return zero on success, nonzero if there is a problem
   */
  public int run(String[] args) {
    TicToc gtimer = new TicToc();
    g.cmdname = "nonunique";
    int returnvalue = 0;
    Options opt = new Options
        ("l=length=probelength=pl:,0=zero:,1=one:,o=output:,u=unique=uniqueness:,f=ufrac=frac:,noskip,d=details");
    try {
      args = opt.parse(args);
    } catch (IllegalOptionException ex) {
      log.error("nonuique: "+ex);
      return 1;
      }
    if (args.length==0) {
      help(); log.error("nonunique: no index given"); return 0; }
    String indexname = args[0];
    String projectname = g.dir+indexname;
    if (args.length>1) {
      log.warn("nonunique: ignoring all arguments except first '%s'", args[0]); }
        
    // Determine options values
    dontdorc = !(opt.isGiven("noskip"));
    outputdetails = (opt.isGiven("details"));
    pl = (opt.isGiven("l")? Integer.parseInt(opt.get("l")) : 70);  // ell
    c0 = (opt.isGiven("0")? Integer.parseInt(opt.get("0")) : 14);  // zero
    c1 = (opt.isGiven("1")? Integer.parseInt(opt.get("1")) : pl);  // one
    String  outname  = ( opt.isGiven("o")? opt.get("o") : (indexname + String.format(".%d-%d-%d",c0,c1,pl)) );
    String  outfile  = g.outdir + outname + ".nuprobes";
    String  statfile = g.outdir + outname + ".nustats";
    int    m0    = ( opt.isGiven("u")? Integer.parseInt(opt.get("u")) : -1);
    double ufrac = ( opt.isGiven("f")? Double.parseDouble(opt.get("f")) : -1.0);
    
    // Read project data and determine asize, q; read alphabet map
    ProjectInfo project;
    try {
       project = ProjectInfo.createFromFile(projectname);
    } catch (IOException ex) {
       log.error("could not read project file: %s", ex);
       return 1;
    }
    g.startProjectLogging(project);
    try {
      asize = project.getIntProperty("qAlphabetSize");
      q = project.getIntProperty("q");
    } catch (NumberFormatException ex) {
      log.error("nonunique: q-grams for index '%s' not found. (Re-create the q-gram index!)", projectname);
      return 1;
    }
    if (!( (q-1)<=c0 && c0<c1 && c1<=pl )) {
      log.error("nonunique: need qGramLength-1 <= zeroCutoff < oneCutoff <= ProbeLength; is (%d-1, %d, %d, %d)",
          q, c0, c1, pl);
      return 1;
    }
    coder = new QGramCoder(q,asize);
    alphabet = g.readAlphabet(projectname+FileNameExtensions.alphabet);
    
    // Read seq, bck, ssp into arrays;  memory-map or read qpos
    TicToc timer = new TicToc();
    String seqfile  = projectname+FileNameExtensions.seq;
    String sspfile  = projectname+FileNameExtensions.ssp;
    System.gc();
    log.info("nonunique: reading '%s', '%s'...", seqfile, sspfile);
    try {
      final ArrayFile arf = new ArrayFile(null);
      s    = arf.setFilename(seqfile).readArray((byte[])null);
      ssp  = arf.setFilename(sspfile).readArray((long[])null);
    } catch (IOException ex) {
      log.error("nonunique: reading '%s', '%s' failed. Stop.",
          seqfile, sspfile);
      return 1;
    }
    final int maxactive = project.getMaximumBucketSize();
    try {
       qgramindex = new QGramIndex(project);
       assert qgramindex.getStride() == 1 : "Stride is not equal to 1. We don't know, yet, whether this works here!";
    } catch (IOException ex) {
       ex.printStackTrace();
       log.error(ex.getMessage());
       return 1;
    }
    log.info("  reading finished after %.1f sec", timer.tocs());
    log.info("nonunique: starting probe selection...");
    n = s.length;
    m = ssp.length;
        
    
    // Walk through s:
    // (A) Initialization
    timer.tic();
    final int slicesize = 1+n/100;
    int nextslice = 0;
    int percentdone = 0;
    if (m0<0) m0 = m-1;
    int m1 = (int)(m*ufrac);
    if (ufrac>0.0 && m1<m0 ) m0 = m1;
    int symremaining = 0;
    int seqnum = 0;
    int p = 0;
    int qcode;
    LRMM = new int[pl-q+1][m];
    activepos = new int[maxactive];  active=0;
    newpos    = new int[maxactive];
    lenforact = new int[maxactive];
    lenfornew = new int[maxactive];
    thislcf   = new int[m];
    incidence = new int[m];
    separation= new int[m][m+1];
    int firstgood = -2;
    int lastgood  = -3;
    try {
      out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(outfile),32*1024), false);
    } catch (FileNotFoundException ex) {
      log.error("nonunique: could not create output file. Stop.");
      return 1;
    }
    long middle = -1;
    if (dontdorc) {
      middle = (ssp[seqnum]-1)/2;
      //log.debug("ssp-1=%d,  %%2=%d,  middle=%d,  s[middle]=%d,  isWildcard=%b", ssp[seqnum]-1, (ssp[seqnum]-1)%2, middle, s[middle], amap.isWildcard(s[middle]));
      assert((ssp[seqnum]-1)%2==0 && alphabet.isWildcard(s[(int)middle])) :
        "nonunique: index does not contain reverse complements; use --noskip option";
    }
    
    // (B) Walking ...
    while (p < n) {
      // (1) Determine next valid position p such that there exists a potential oligo at p
      if (symremaining<pl) {   // next invalid is at p+symremaining
        p += symremaining; symremaining=0;
        for (; p<n && (!alphabet.isSymbol(s[p]));  p++) {
          if (alphabet.isSeparator(s[p])) {
            assert(p==ssp[seqnum]);
            if(p>=n-1) { p=n; break; }
            seqnum++; /*seqstart=p+1;*/
            if (dontdorc) {
              middle = (ssp[seqnum-1] + ssp[seqnum])/2; //ok
              assert((ssp[seqnum]-1)%2==0 && alphabet.isWildcard(s[(int)middle])) :
                "nonunique: index does not contain reverse complements; use --noskip option";
            }           
          } else if (p==middle && dontdorc) {
            p = (int)(ssp[seqnum]-1); // -1 because of p++
            //log.info("  skipping reverse complement to ssp at %d",p+1);
          }
        }
        if (p>=n) break;
        int i; // next valid symbol is now at p, count number of valid symbols
        for (i=p; i<n && alphabet.isSymbol(s[i]); i++) {}
        symremaining = i-p;
        if (symremaining < pl) continue;
      }
      assert alphabet.isSymbol(s[p]);
      assert symremaining >= pl;
      
      // (2) initialize LRMM matrix and thislcf
      active = 0;  // number of active q-grams
      qcode = coder.code(s,p);
      findlrmm(qcode, p, 0);
      for (int k=1; k<LRMM.length; k++) {
        qcode = coder.codeUpdate(qcode, s[p+k+q-1]);
        findlrmm(qcode, p+k, k);
      }
      java.util.Arrays.fill(thislcf,0);
      
      // (3) repeatedly process current position p; update LRMM at right end
      while (symremaining >=pl) {
        // (3x) Status
        while(p>=nextslice) {
          log.info("  %2d%% done, %.1f sec, pos %d/%d, seq %d/%d",
              percentdone, timer.tocs(), p, n-1, seqnum, m-1);
          percentdone += 1;  nextslice += slicesize;
        }
        
        // (3a) process present LRMM -> thislcf
        boolean goodoligo = true;
        int fullmatches = 0;
        int ti;
        for (int i=0; i<m; i++) {
          if (thislcf[i]>q) thislcf[i]--; else thislcf[i]=0;
          for (int k=0; k<LRMM.length; k++) {
            int rl = pl - k;
            if (thislcf[i]>=rl) break;
            int mmm = LRMM[k][i];
            if (mmm > rl) mmm=rl;
            if (thislcf[i]<mmm) thislcf[i]=mmm;
          }
          ti = thislcf[i];
          if (ti>=c1) fullmatches++;
          else if (ti>c0) goodoligo=false;
        }
        if (fullmatches>m0) goodoligo=false;
        if (goodoligo)  { // we have a good oligo
          if (p==lastgood+1) lastgood++;  // previous was also good
          else {                          // starting a new good block
            outputRange(firstgood,lastgood);  // output previous block
            firstgood=lastgood=p;
            for (int i=0; i<m; i++) incidence[i]=(thislcf[i]>=c1?1:0);
          }
          if(outputdetails) outputDetails(p, seqnum);
        }
        // (3b) update LRMM at its right end
        if (symremaining>pl) {
          qcode = coder.codeUpdate(qcode, s[p+LRMM.length+q-1]);
          shiftlrmm();
          findlrmm(qcode, p+LRMM.length, LRMM.length-1);
        }
        p++; symremaining--;
      } // end (3) while loop
      
      // (4) done with these p. Go to next iteration and skip a few more...
      {}
    }
    outputRange(firstgood, lastgood);
    out.close();
    log.info("nonunique: probe selection took %.1f sec", timer.tocs());
    
    // Finally, write statistics
    log.info("nonunique: writing statistics to '%s'",statfile);
    PrintWriter stw = null;
    try {
      stw = new PrintWriter(statfile);
    } catch (FileNotFoundException ex) {
      log.warn("nonunique: could not create statistics file. Skipping.");
      returnvalue = 2;
    }
    if (stw!=null) {
      stw.printf ("# Probe statistics for %d sequences:%n",m);
      stw.println("# row i, column j:  number of probes in seq i that DON'T appear in seq j");
      stw.println("# diagonal  (i,i):  number of UNIQUE probes in seq i (appear NOWHERE else)");
      stw.println("# last column    :  TOTAL number of probes for seq i (matrix is  m x (m+1))");
      for(int i=0; i<m; i++) {
        for(int j=0; j<m; j++)
          stw.printf("%8d ",separation[i][j]);
        stw.printf("    %9d%n",separation[i][m]);  // output total probes
      }
      stw.close();
    }
    
    // that's all
    log.info("nonunique: total time was %.1f sec", gtimer.tocs());
    return returnvalue;
  }
  
  
  
  private int[][] LRMM      = null;
  private int[]   activepos = null;
  private int     active    = 0;
  private int[]   newpos    = null;
  private int[]   lenforact = null;
  private int[]   lenfornew = null;
  private int[]   thislcf   = null;
  private int[]   incidence = null;
  private int[][] separation = null;
  
  
  private final void findlrmm(final int qcode, final int sp, final int lrmmrow) {
    int ai, ni;
    
    //log.info("  spos=%d, qcode=%d (%s),  row=%d.  rank=%d", sp, qcode, coder.qGramString(qcode,amap), lrmmrow, r);
    for(ai=0; ai<active; ai++) { activepos[ai]++;  if (lenforact[ai]>q) lenforact[ai]--; else lenforact[ai]=0; }
    if(lrmmrow>0)
      for(int i=0; i<m; i++) LRMM[lrmmrow][i] = (LRMM[lrmmrow-1][i]-1>q? LRMM[lrmmrow-1][i]-1 : 0);
    else
      java.util.Arrays.fill(LRMM[lrmmrow], 0);
    
    qgramindex.getQGramPositions(qcode, newpos);
    final int newactive = qgramindex.getBucketSize(qcode); // number of new active q-grams
    
    //log.info("    qpos = [%s]", Strings.join(" ",newpos, 0, newactive));
    for(ni=0, ai=0; ni<newactive; ni++) {
      while(ai<active && lenforact[ai]<q) ai++;
      assert(ai==active || newpos[ni]<=activepos[ai])
      : String.format("spos=%d, ai=%d, ni=%d, newpos=%d, activepos=%d",
          sp,ai,ni,newpos[ni],activepos[ai]);
      if (ai>=active || newpos[ni]!=activepos[ai]) {
        // determine lenfornew[ni] by comparins s[sp...] with s[p...]
        int p = newpos[ni] + q;
        int offset;
        for (offset = q;  ; p++, offset++) {
          if ( !(s[p]==s[sp+offset] && alphabet.isSymbol(s[p])) ) break;
        }
        lenfornew[ni] = offset;
        int i = seqindex(newpos[ni]);
        if (LRMM[lrmmrow][i]<offset) LRMM[lrmmrow][i]=offset;
      } else {
        lenfornew[ni] = lenforact[ai];
        ai++;
      }
    }
    int[] tmp;
    tmp = activepos;  activepos = newpos;    newpos    = tmp;
    tmp = lenforact;  lenforact = lenfornew; lenfornew = tmp;
    active = newactive;
  }
  
  
  private final void shiftlrmm() {
    final int[] tmp = LRMM[0];
    for(int k=0; k<LRMM.length-1; k++) LRMM[k]=LRMM[k+1];
    LRMM[LRMM.length-1] = tmp;
  }
  
  private final int seqindex(int p) {
    int si = java.util.Arrays.binarySearch(ssp, p);
    if (si>=0) return si; // return the index of the ssp position
    return(-si-1);        // we are in a sequence, return the index of the following ssp position
  }
  
  /** write last good oligo block to outfile; update separation[][] */
  private final void outputRange(int first, int last) {
    if (first<0) return;
    assert(first<=last);
    int si = seqindex(first);
    int ss = si==0? 0 : (int)(ssp[si-1]+1);
    int le = last-first+1;
    assert le >= 1;
    log.debug("%d %d-mers: seq %d [%d..%d];  pos %d..%d", le, pl, si, first-ss, last-ss, first, last);
    out.printf("%d @ seq=%d[%d..%d];  pos=%d..%d%n",
        le, si, first-ss, last-ss, first, last);
    try {
      out.printf("%s%n", alphabet.preimage(s,first,pl+le-1));
    } catch (InvalidSymbolException ex) {
      ex.printStackTrace();
      log.error("Error printing oligo");
      Globals.terminate(1);
    }
    out.printf("%s%n%n", StringUtils.join(" ",incidence,0,m));
    out.flush();
    for(int i=0; i<m; i++) {
      if(incidence[i]==0) continue; // nothing to add for seq i
      separation[i][m]+=le;         // le total new probes for seq i
      int sum = 0;
      for(int j=0; j<m; j++) 
        if (incidence[j]==0) { separation[i][j]+=le; }
        else sum++;
      assert(sum>=1);
      if(sum==1) separation[i][i]+=le;
    }
  }
  
  private final void outputDetails(int pp, int si) {
    int ss = (si==0? 0 : (int)(ssp[si-1]+1));
    out.printf("# seq=%d[%d];  pos=%d: ", si, pp-ss, pp);
    out.printf("%s%n", StringUtils.join(" ", thislcf, 0, m));
  }
}
