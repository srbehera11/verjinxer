/*
 * Cutter.java
 *
 * Created on 28. Juni 2007, 13:49
 *
 */

package verjinxer;

import java.io.*;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import rahmann.sequenceanalysis.*;
import rahmann.util.*;
import static verjinxer.Globals.*;

/**
 *
 * @author rahmann
 */
public class Cutter {
  
  private Globals g;
  
  /** Creates a new instance of QgramMatcher */
  public Cutter(Globals gl) {
    g = gl;
  }
  
  /**
   * print help on usage
   */
  public void help() {
    g.logmsg("Usage:%n  %s map  [options]  <sequencefile>%n", programname);
    g.logmsg("Cuts a .seq file at specific matching patterns,%n");
    g.logmsg("reports all cut points in increasing order in a .cut file.%n");
    g.logmsg("Options:%n");
    g.logmsg("  -p, --patterns <plist>  list of patterns where to cut%n");
    g.logmsg("  -i, --ignore   <dist>   ignore occurrences ending or starting within dist of ssp%n");
    g.logmsg("  -f <name>,<lmin>,<lmax> write fragments with length in [lmin,lmax] as FASTA%n");
    g.logmsg("  -r, --rc                also cut if reverse complement matches (DNA only!)%n");
    g.logmsg("  --nossp                 don't cut at sequence separarots%n");
  }
  
  
  /** if run independently, call main */
  public static void main(String[] args) {
    new Cutter(new Globals()).run(args);
  }
  
  /**
   * @param args the command line arguments
   * @return 0 on success, nonzero on problems
   */
  public int run(String[] args) {
    TicToc gtimer = new TicToc();
    g.cmdname = "cut";
    int returnvalue = 0;
    
    Options opt = new Options("r=rc,p=pattern=patterns:,nossp,i=ignore:,f:");
    try {
      args = opt.parse(args);
    } catch (IllegalOptionException ex) {
      help();
      g.terminate("cut: "+ex.toString());
    }
    
    if (opt.isGiven("r")) throw new UnsupportedOperationException();
    
    if (args.length<1) {
      help();
      g.terminate("cut: a sequence file must be specified.");
    }
    
    // set sequence file name, start log, read alphabet map
    String sname = args[0];
    String ds = g.dir + sname;
    g.startplog(ds+extlog);
    AlphabetMap amap = g.readAlphabetMap(ds + extalph);
    
    // get patterns
    String[] pstrings = null;
    if (opt.isGiven("p")) {
      pstrings =  opt.get("p").split(",");
    } else {
      pstrings = new String[0];
    }
    
    final int numpat = pstrings.length;
    byte[][] pattern = new byte[numpat][];
    int[] cutpoint   = new int[numpat];
    
    for (int p=0; p<numpat; p++) {
      String[] cutpattern = pstrings[p].split(":");
      try {
        pattern[p] = amap.applyTo(cutpattern[0], false);
      } catch (InvalidSymbolException ex) {
        g.warnmsg("cut: pattern '%s' does not map to alphabet%n", pstrings[p]);
        g.terminate(1);
      }
      cutpoint[p] = Integer.parseInt(cutpattern[1]);
      // g.logmsg("Pattern %d has length %d [0..%d], cuts before %d%n", p,pattern[p].length,pattern[p].length-1,cutpoint[p]);
    }
    
    // get option i
    int ignoredist = -1;
    if (opt.isGiven("i")) {
      ignoredist = Integer.parseInt(opt.get("i"));
    }
    
    //get option f
    final boolean writefasta = opt.isGiven("f");

    // init cutpoints with start and end of file, and ssps
    int totalMatches = 0;
    ArrayList<Integer> matchPositions = new ArrayList<Integer>(10*1024*1024);
    matchPositions.add(0); totalMatches++;

    int[] ssp = null;
    if (!opt.isGiven("nossp") || opt.isGiven("i")) {
      ssp = g.slurpIntArray(ds+extssp, new int[0]);
    }
    if (!opt.isGiven("nossp")) {
      for (int i: ssp) { matchPositions.add(i); matchPositions.add(i+1); }
      g.logmsg("cut: sequence separators cut 2*%d = %d times%n", ssp.length, 2*ssp.length);
      totalMatches += 2*ssp.length;
    }

    
    // Read sequence
    MappedByteBuffer in = null;
    try {
      in = new ArrayFile(ds+extseq).mapR();
    } catch (IOException ex) {
      g.terminate("map: "+ex.toString());
    }
    final int fsize = in.capacity();
    g.logmsg("cut: sequence file '%s' has length %d%n", ds+extseq, fsize);
    
    for (int p=0; p<numpat; p++) {
      int numMatches = 0;
      final byte[] pat = pattern[p];
      
      for (int i=0; i<fsize-pat.length+1; i++) {
        // check if there is a match with pat, starting at i
        boolean isMatch = true;
        for (int j=0; j<pat.length; j++) {
          if (pat[j] != in.get(i + j)) {
            isMatch = false;
            break;
          }
        }
        // if no match, try next position
        if (!isMatch) continue;
        if (ignoredist>0) {  // check whether to ignore match
          // check start of sequence
          int sspindex = java.util.Arrays.binarySearch(ssp,i);
          assert(sspindex<0);
          sspindex = -sspindex - 1;
          final int left  = (sspindex==0? -1 : ssp[sspindex-1]);
          final int right = ssp[sspindex];
          assert(left<i && i<right-pat.length+1);
          if (i-left <= ignoredist || right-pat.length-i+1 <= ignoredist) continue;
        }
        matchPositions.add(i+cutpoint[p]);
        numMatches++;
      }
      g.logmsg("cut: pattern '%s' cuts %d times (1 : %.2f)%n", pstrings[p], numMatches, (double)fsize/numMatches);
      totalMatches += numMatches;
    } // end for p
    
    matchPositions.add(fsize);  totalMatches++;
    
    // sort positions
    Integer[] mp = matchPositions.toArray(new Integer[0]);
    java.util.Arrays.sort(mp);
    
    // write output
    int written = 0;
    final String outfile = ds + ".cut";
    try {
      int recent = -1;
      DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outfile), 1024*1024));
      for (int i=0; i < mp.length; i++) {
        assert(mp[i]>=recent);
        if (mp[i]==recent) continue;
        recent = mp[i];
        out.writeInt(recent);
        written++;
      }
      out.close();
    } catch (IOException ex) {
      g.terminate("cut: could not write output file "+outfile);
    }
    g.logmsg("cut: wrote %d cutpoints, %d less than expected.%n", written, totalMatches-written);
  
    
    // write FASTA if desired
    if (!writefasta) return returnvalue;
    String[] fparameters = opt.get("f").split("\\s*,\\s*");
    String fname = g.outdir + fparameters[0];
    int fminlen = 0;
    int fmaxlen = Integer.MAX_VALUE;
    try {
      if (fparameters.length>=2) fminlen = Integer.parseInt(fparameters[1]);
    } catch (NumberFormatException ex) {
      fminlen = 0;
    }
    try {
      if (fparameters.length>=3) fmaxlen = Integer.parseInt(fparameters[2]);
    } catch (NumberFormatException ex) {
      fmaxlen = Integer.MAX_VALUE;
    }
    g.logmsg("cut: writing fragments with length in [%d,%d] to '%s'%n", fminlen, fmaxlen, fname);
    
    int fcount=0;
    long flen=0;
    boolean errors = false;
    FastaFile ffile = null;
    IntBuffer cutpoints = null;
    try {   
      cutpoints = new ArrayFile(outfile).mapR().asIntBuffer();
      ffile = new FastaFile(fname).open(FastaFile.FastaMode.WRITE);
    } catch (IOException ex) {
      g.terminate(String.format("cut: %s", ex.toString()));
    }
    byte[] fragment = new byte[fmaxlen];
    int lastcp = 0;
    while(cutpoints.hasRemaining()) {
      final int cp = cutpoints.get();
      assert(cp>=lastcp);
      final int len = cp - lastcp;
      if (len<fminlen || len>fmaxlen || len==0) { lastcp=cp; continue; }
      // output 'in' from lastcp to lastcp+len-1, inclusive
      in.position(lastcp);
      in.get(fragment,0,len);
      try {
        final String fstring = amap.preimage(fragment,0,len);
        ffile.writeString(fstring, String.format("Fragment OK SRC=%s NUM=%d BEG=%d LEN=%d", sname, fcount, lastcp, len));
      } catch (InvalidSymbolException ex) {
        ffile.writeString("", String.format("Fragment !INVALID! SRC=%s NUM=%d BEG=%d LEN=%d", sname, -1, lastcp, len));
        errors=true;
      }
      fcount++; flen+=len;
      lastcp = cp;
    }
    try { ffile.close(); } catch (IOException ex) { }
    g.logmsg("cut: wrote %d fragments, total length %d%n", fcount, flen);
    if (errors) { g.warnmsg("cut: could not back-translate some fragments"); returnvalue = 1; }
    in = null;
    cutpoints = null;
    return returnvalue;
  }
  
// end class
}

