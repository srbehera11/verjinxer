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

import static verjinxer.Globals.programname;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import com.spinn3r.log5j.Logger;

import verjinxer.sequenceanalysis.QGramCoder;
import verjinxer.sequenceanalysis.QGramFilter;
import verjinxer.util.IllegalOptionException;
import verjinxer.util.Options;
import verjinxer.util.ProjectInfo;
import verjinxer.util.TicToc;


/**
 *
 * @author Sven Rahmann
 * @author Marcel Martin
 */
public class QgramMatcherSubcommand implements Subcommand {
  private static final Logger log = Globals.log;    
  private Globals g;
  
  /**
   * print help on usage
   * TODO use 
   */
  public void help() {
    log.info("Usage:");
    log.info("  %s qmatch  [options]  [<sequences>]  <index>%n", programname);
    log.info("Reports all maximal matches at least as long as a given length >=q,%n");
    log.info("in human-readable output format. Writes .matches or .sorted-matches.%n");
    log.info("Options:%n");
    log.info("  -l, --length <len>   minimum match length [q of q-gram-index]%n");
    log.info("  -s, --sort           write matches sorted per index sequence%n");
    log.info("  -m, --min    <min>   show matches only if >=min (per index seq if sorted)%n");
    log.info("  -M, --max    <max>   stop considering sequences at max+1 matches%n");
    log.info("  -F, --filter <c:d>   apply q-gram filter <complexity:delta>%n");
    log.info("  --self               compare index against itself%n");
    log.info("  -t, --tmh <filename> name of too-many-hits-filter file (use #)%n");
    log.info("  -o, --out <filename> specify output file (use # for stdout)%n");
    log.info("  -x, --external       save memory at the cost of lower speed%n");
    log.info("  -c, --cmatchesc      C matches C, even if not before G%n");
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
       log.error("qmatch: "+ex);
       return 1;
     }

     boolean selfcmp = opt.isGiven("self");
     if (args.length<1 || (args.length==1 && !selfcmp)) {
       help(); 
       log.error("qmatch: need both <sequences> and <index>, or --self and <index>!");
       return 1;
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
       if (selfcmp) log.info("qmatch: using --self with indices will suppress symmetric matches%n");
       tname = args[0];
       sname = args[1];
       if (selfcmp && !tname.equals(sname)) log.warn("qmatch: using --self, but %s != %s%n", tname,sname);
     }
     dt = g.dir + tname;
     ds = g.dir + sname;

     // Read project data and determine asize, q; read alphabet map
     ProjectInfo project;
     try {
        project = ProjectInfo.createFromFile(ds);
     } catch (IOException ex) {
        log.error("qmatch: cannot read project file.%n");
        return 1;
     }
     g.startProjectLogging(project);
     
     // TODO some ugly things because I insist that asize and q be final
     
     int asizetmp, qtmp;
     
     // Read project data and determine asize, q; read alphabet map
     try { 
       asizetmp = project.getIntProperty("qAlphabetSize");
       qtmp = project.getIntProperty("q");
     } catch (NumberFormatException ex) {
       log.error("qmatch: q-grams for index '%s' not found (Re-create the q-gram index!); %s%n", ds, ex);
       return 1;
     } 
     final int asize=asizetmp, q=qtmp;

     // q-gram filter options from -F option
     final QGramFilter qgramfilter = new QGramFilter(q, asize, opt.get("F"));

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
       log.warn("qmatch: increasing minimum match length to q=%d!%n",q);
       minlen=q;
     }
     int minseqmatches = (opt.isGiven("m")? Integer.parseInt(opt.get("m")) : 1);
     if (minseqmatches<1) {
       log.warn("qmatch: increasing minimum match number to 1!%n");
       minseqmatches=1;
     }

     String outname = String.format("%s-%s-%dx%d", tname, sname, minseqmatches, minlen);
     if (opt.isGiven("o")) {
       if (outname.length()==0 || outname.startsWith("#"))  outname = null;
       else outname=opt.get("o");
     }
     if (outname!=null)  outname = g.outdir + outname + (sorted? ".sorted-matches" : ".matches");

     boolean c_matches_c = opt.isGiven("c");

     if (c_matches_c) log.info("qmatch: C matches C, even if no G follows%n");
     else log.info("qmatch: C matches C only before G%n");

     // end of option parsing

     // start output
     PrintWriter out = new PrintWriter(System.out);
     if (outname!=null) {
       try {
         out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(outname),32*1024), false);
       } catch (FileNotFoundException ex) {
         log.error("qmatch: could not create output file. Stop."); return 1;
       }
     }
     log.info("qmatch: will write results to %s%n", (outname!=null? "'"+outname+"'" : "stdout"));
     
     final QGramCoder qgramcoder = new QGramCoder(q, asize);
     final boolean bisulfite = project.getBooleanProperty("Bisulfite");
     if (bisulfite) log.info("qmatch: index is for bisulfite sequences, using bisulfite matching%n");
 
     try {
        QgramMatcher qgrammatcher = new QgramMatcher(
              g,
              dt, 
              ds, 
              toomanyhitsfilename,
              maxseqmatches, 
              minseqmatches, 
              minlen,
              qgramcoder,
              qgramfilter,
              out,
              sorted, 
              external,
              selfcmp, 
              bisulfite,
              c_matches_c,
              project);
        qgrammatcher.match(qgramcoder, qgramfilter);
        qgrammatcher.tooManyHits(dt+".toomanyhits-filter");
     } catch (IOException ex) {
        log.error("could not initialize qgrammatcher: "+ex.getMessage());
     }
     out.close();
     log.info("qmatch: done; total time was %.1f sec%n", totalTimer.tocs());
     g.stopplog();
     
     return 0;
  }
}

