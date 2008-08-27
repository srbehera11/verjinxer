/*
 * QgramMatcherSubcommand.java
 * Created on 25. April 2007, 12:32
 *
 * TODO: adapt to 64-bit files
 * 
 * Sample usage:
 *
 * To map a 454 sequencing run ('run1') against human chromosome 1:
 * ... qmatch -l 25 -M 15 -F 2:0 -t #  run1  c01
 * We expect a perfect read of length 100.  This should give rise to at least one perfect match of length >= 25.
 * We expect a unique location on the genome. This should give rise to at most 15 hits of length >= 25.
 * We do not allow hits to start a degenerate q-grams that consist  of only 2 different nucleotides.
 * Running hits can extend beyond these, however.
 * We keep track of the sequences that have already been identified as repeats (ie, have too many hits). 
 *
 */

package verjinxer;

import java.io.*;
import java.util.Properties;

import verjinxer.util.*;
import verjinxer.sequenceanalysis.*;
import verjinxer.sequenceanalysis.QGramCoder;
import static verjinxer.Globals.*;


/**
 *
 * @author Sven Rahmann
 * @author Marcel Martin
 */
public class QgramMatcherSubcommand implements Subcommand {
      
  private Globals g;
  
  /**
   * print help on usage
   * TODO use 
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
    g.logmsg("  -F, --filter <c:d>   apply q-gram filter <complexity:delta>%n");
    g.logmsg("  --self               compare index against itself%n");
    g.logmsg("  -t, --tmh <filename> name of too-many-hits-filter file (use #)%n");
    g.logmsg("  -o, --out <filename> specify output file (use # for stdout)%n");
    g.logmsg("  -x, --external       save memory at the cost of lower speed%n");
    g.logmsg("  -c, --cmatchesc      C matches C, even if not before G%n");
  }
  
  /** if run independently, call main
   *@param args (ignored)
   */
  public static void main(String[] args) {
    new QgramMatcherSubcommand(new Globals()).run(args);
  }
 
  /** 
   * @param gl the Globals structure
   * @param args the command line arguments
   */
  public QgramMatcherSubcommand(Globals gl) {
     g = gl;
  }

  public int run(String[] args) {
     TicToc totalTimer = new TicToc();
     g.cmdname = "qmatch";

     Options opt = new Options
         ("l=length:,s=sort=sorted,o=out=output:,x=external,m=min=minmatch=minmatches:,M=max:,F=filter:,t=tmh:,self,c=cmatchesc");
     try {
       args = opt.parse(args);
     } catch (IllegalOptionException ex) {
       g.terminate("qmatch: "+ex.toString());
     }

     boolean selfcmp = opt.isGiven("self");
     if (args.length<1 || (args.length==1 && !selfcmp)) {
       help(); 
       g.terminate("qmatch: need both <sequences> and <index>, or --self and <index>!");
     }

     // t: text to find
     // s: sequence in which to search
     String tname, sname, dt, ds;
     
     if (args.length==1) {
       assert selfcmp;
       tname = args[0];
       sname = tname;
     } else {
       assert args.length>=2;
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
     
     // TODO some ugly things because I insist that asize and q be final
     
     int asizetmp, qtmp;
     
     // Read project data and determine asize, q; read alphabet map
     try { 
       asizetmp = Integer.parseInt(prj.getProperty("qAlphabetSize"));
       qtmp = Integer.parseInt(prj.getProperty("q"));
     } catch (NumberFormatException ex) {
       g.warnmsg("qmatch: q-grams for index '%s' not found (Re-create the q-gram index!); %s%n", ds, ex.toString());
       g.terminate(1);
       
       // TODO the compiler does not know that g.terminate terminates the program
       asizetmp = 0; qtmp = 0;
     } 
     final int asize=asizetmp, q=qtmp;

     // q-gram filter options from -F option
     final int[] filterparam = QGramFilter.parseFilterParameters(opt.get("F"));
     final QGramFilter qgramfilter = new QGramFilter(q, asize, filterparam[0], filterparam[1]);

     // Prepare the sequence filter
     int maxseqmatches = Integer.MAX_VALUE;
     if (opt.isGiven("M")) maxseqmatches = Integer.parseInt(opt.get("M"));

     String toomanyhitsfilename = null;
     if (opt.isGiven("t")) {
       if (opt.get("t").startsWith("#")) {
         toomanyhitsfilename = dt+".toomanyhits-filter";
       } else {
         toomanyhitsfilename = g.dir + opt.get("t");
       }
     }

     // Determine option values
     boolean external = opt.isGiven("x");
     boolean sorted = opt.isGiven("s");
     int minlen = (opt.isGiven("l")? Integer.parseInt(opt.get("l")) : q);
     if (minlen<q) {
       g.warnmsg("qmatch: increasing minimum match length to q=%d!%n",q);
       minlen=q;
     }
     int minseqmatches = (opt.isGiven("m")? Integer.parseInt(opt.get("m")) : 1);
     if (minseqmatches<1) {
       g.warnmsg("qmatch: increasing minimum match number to 1!%n");
       minseqmatches=1;
     }

     String outname = String.format("%s-%s-%dx%d", tname, sname, minseqmatches, minlen);
     if (opt.isGiven("o")) {
       if (outname.length()==0 || outname.startsWith("#"))  outname = null;
       else outname=opt.get("o");
     }
     if (outname!=null)  outname = g.outdir + outname + (sorted? ".sorted-matches" : ".matches");

     boolean c_matches_c = opt.isGiven("c");

     if (c_matches_c) g.logmsg("qmatch: C matches C, even if no G follows%n");
     else g.logmsg("qmatch: C matches C only before G%n");

     // end of option parsing

     // start output
     PrintWriter out = new PrintWriter(System.out);
     if (outname!=null) {
       try {
         out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(outname),32*1024), false);
       } catch (FileNotFoundException ex) {
         g.terminate("qmatch: could not create output file. Stop.");
       }
     }
     g.logmsg("qmatch: will write results to %s%n", (outname!=null? "'"+outname+"'" : "stdout"));
     
     final int maxactive = Integer.parseInt(prj.getProperty("qbckMax"));
     final QGramCoder qgramcoder = new QGramCoder(q, asize);
     final boolean bisulfite = Boolean.parseBoolean(prj.getProperty("Bisulfite"));
     if (bisulfite) g.logmsg("qmatch: index is for bisulfite sequences, using bisulfite matching%n");
 
     QgramMatcher qgrammatcher = new QgramMatcher(
           g,
           dt, 
           ds, 
           toomanyhitsfilename,
           maxseqmatches, 
           minseqmatches, 
           minlen,
           maxactive,
           qgramcoder,
           qgramfilter,
           out,
           sorted, 
           external,
           selfcmp, 
           bisulfite,
           c_matches_c);
     qgrammatcher.match(qgramcoder, maxactive, qgramfilter);
     qgrammatcher.tooManyHits(dt+".toomanyhits-filter");

     out.close();
     g.logmsg("qmatch: done; total time was %.1f sec%n", totalTimer.tocs());
     g.stopplog();
     
     return 0;
  }
}

