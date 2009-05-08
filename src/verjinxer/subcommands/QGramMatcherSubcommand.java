/**
 * QgramMatcherSubcommand.java Created on 25. April 2007, 12:32
 * 
 * TODO: adapt to 64-bit files
 * 
 * Sample usage: To map a 454 sequencing run ('run1') against human chromosome 1: <br>
 * ... qmatch -l 25 -M 15 -F 2:0 -t # run1 c01
 * 
 * We expect a perfect read of length 100. This should give rise to at least one perfect match of
 * length >= 25. We expect a unique location on the genome. This should give rise to at most 15 hits
 * of length >= 25. We do not allow hits to start a degenerate q-grams that consist of only 2
 * different nucleotides. Running hits can extend beyond these, however. We keep track of the
 * sequences that have already been identified as repeats (ie, have too many hits).
 */

package verjinxer.subcommands;

import static verjinxer.Globals.programname;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import com.spinn3r.log5j.Logger;

import verjinxer.Globals;
import verjinxer.QGramMatcher;
import verjinxer.sequenceanalysis.BisulfiteQGramCoder;
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
public class QGramMatcherSubcommand implements Subcommand {
   private static final Logger log = Globals.getLogger();
   private Globals g;

   /**
    * prints help on usage
    */
   public void help() {
      log.info("Usage:");
      log.info("  %s qmatch  [options]  [<sequences>]  <index>", programname);
      log.info("Reports all maximal matches at least as long as a given length >=q,");
      log.info("in human-readable output format. Writes .matches or .sorted-matches.");
      log.info("Options:");
      log.info("  -l, --length <len>   minimum match length [q of q-gram-index]");
      log.info("  -s, --sort           write matches sorted per index sequence");
      log.info("  -m, --min    <min>   show matches only if >=min (per index seq if sorted)");
      log.info("  -M, --max    <max>   stop considering sequences at max+1 matches");
      log.info("  -F, --filter <c:d>   apply q-gram filter <complexity:delta>");
      log.info("  --self               compare index against itself");
      log.info("  -t, --tmh <filename> name of too-many-hits-filter file (use #)");
      log.info("  -o, --out <filename> specify output file (use # for stdout)");
//      log.info("  -x, --external       save memory at the cost of lower speed");
      log.info("  -c, --cmatchesc      C matches C, even if not before G");
      log.info("  -b, --bisulfite      index is over bisulfite-modified DNA reads.");
      log.info("                       simulates bisulfite modification for the queries");
   }

   /**
    * 
    */
   public static void main(String[] args) {
      new QGramMatcherSubcommand(new Globals()).run(args);
   }

   /**
    * @param gl
    *           the Globals structure
    */
   public QGramMatcherSubcommand(Globals gl) {
      g = gl;
   }

   public int run(String[] args) {
      TicToc totalTimer = new TicToc();
      g.cmdname = "qmatch";

      Options opt = new Options(
            "b=bisulfite,l=length:,s=sort=sorted,o=out=output:,m=min=minmatch=minmatches:,M=max:,F=filter:,t=tmh:,self,c=cmatchesc");
      try {
         args = opt.parse(args);
      } catch (IllegalOptionException ex) {
         log.error("qmatch: " + ex);
         return 1;
      }

      boolean selfcmp = opt.isGiven("self");
      if (args.length < 1 || (args.length == 1 && !selfcmp)) {
         help();
         log.error("qmatch: need both <sequences> and <index>, or --self and <index>!");
         return 1;
      }

      // t: text to find
      // s: sequence in which to search
      String tname, sname, dt, ds;

      if (args.length == 1) {
         assert selfcmp;
         tname = args[0];
         sname = tname;
      } else {
         assert args.length >= 2;
         if (selfcmp)
            log.info("qmatch: using --self with indices will suppress symmetric matches");
         tname = args[0];
         sname = args[1];
         if (selfcmp && !tname.equals(sname))
            log.warn("qmatch: using --self, but %s != %s", tname, sname);
      }
      dt = g.dir + tname;
      ds = g.dir + sname;

      // Read project data and determine asize, q; read alphabet map
      ProjectInfo indexProject, queryProject;
      try {
         indexProject = ProjectInfo.createFromFile(ds);
         queryProject = ProjectInfo.createFromFile(dt);
      } catch (IOException ex) {
         log.error("qmatch: cannot read project files.");
         return 1;
      }
      g.startProjectLogging(indexProject);

      final int asize, q;

      // Read project data and determine asize, q; read alphabet map
      try {
         asize = indexProject.getIntProperty("qAlphabetSize");
         q = indexProject.getIntProperty("q");
      } catch (NumberFormatException ex) {
         log.error("qmatch: q-grams for index '%s' not found (Re-create the q-gram index!); %s",
               ds, ex);
         return 1;
      }

      // q-gram filter options from -F option
      final QGramFilter qgramfilter = new QGramFilter(q, asize, opt.get("F"));

      // Prepare the sequence filter
      int maxseqmatches = Integer.MAX_VALUE;
      if (opt.isGiven("M"))
         maxseqmatches = Integer.parseInt(opt.get("M"));

      String toomanyhitsfilename = null;
      if (opt.isGiven("t")) {
         if (opt.get("t").startsWith("#")) {
            toomanyhitsfilename = dt + ".toomanyhits-filter";
         } else {
            toomanyhitsfilename = g.dir + opt.get("t");
         }
      }

      // Determine option values
      boolean sorted = opt.isGiven("s");
      int minlen = (opt.isGiven("l") ? Integer.parseInt(opt.get("l")) : q);
      if (minlen < q) {
         log.warn("qmatch: increasing minimum match length to q=%d!", q);
         minlen = q;
      }
      int minseqmatches = (opt.isGiven("m") ? Integer.parseInt(opt.get("m")) : 1);
      if (minseqmatches < 1) {
         log.warn("qmatch: increasing minimum match number to 1!");
         minseqmatches = 1;
      }

      String outname = String.format("%s-%s-%dx%d", tname, sname, minseqmatches, minlen);
      if (opt.isGiven("o")) {
         if (outname.length() == 0 || outname.startsWith("#"))
            outname = null;
         else
            outname = opt.get("o");
      }
      if (outname != null)
         outname = g.outdir + outname + (sorted ? ".sorted-matches" : ".matches");
      log.info("qmatch: will write results to %s", (outname != null ? "'" + outname + "'"
            : "stdout"));

      final boolean bisulfiteQueries = opt.isGiven("b");
      final boolean bisulfiteIndex = indexProject.isBisulfiteIndex();
      if (bisulfiteIndex)
         log.info("qmatch: index contains simulated bisulfite-treated sequences, using bisulfite matching");

      if (bisulfiteQueries) {
         log.info("qmatch: will simulate bisulfite treatment for query sequences");
      }
      if (bisulfiteIndex && bisulfiteQueries) {
         log.error("qmatch: sorry, index contains bisulfite-simulated sequences and you also requested\n"
               + "bisulfite simulation of the query sequences. This does not work.");
         return 1;
      }
      if (bisulfiteQueries && asize != 4) {
         log.error("qmatch: alphabet size must be 4 when bisulfite simulation is requested.");
         return 1;
      }
      boolean c_matches_c = opt.isGiven("c");
      if (bisulfiteIndex || bisulfiteQueries) {
         if (c_matches_c)
            log.info("qmatch: C matches C, even if no G follows");
         else
            log.info("qmatch: C matches C only before G");
      }
      final int stride = indexProject.getStride();
      log.info("qmatch: stride length of index is %d", stride);

      // start output
      PrintWriter out = new PrintWriter(System.out);
      if (outname != null) {
         try {
            out = new PrintWriter(
                  new BufferedOutputStream(new FileOutputStream(outname), 32 * 1024), false);
         } catch (FileNotFoundException ex) {
            log.error("qmatch: could not create output file.");
            return 1;
         }
      }

      final QGramCoder qgramcoder; // computes qcodes of the queries
      if (bisulfiteQueries) {
         qgramcoder = new BisulfiteQGramCoder(q);
      } else {
         qgramcoder = new QGramCoder(q, asize);
      }

      try {
         QGramMatcher qgrammatcher = new QGramMatcher(g, queryProject, indexProject, toomanyhitsfilename,
               maxseqmatches, minseqmatches, minlen, qgramcoder, qgramfilter, out, sorted, selfcmp,
               c_matches_c);
         if (bisulfiteQueries) {
            qgrammatcher.bisulfiteMatch();
         } else {
            qgrammatcher.match();
         }
         qgrammatcher.tooManyHits(dt + ".toomanyhits-filter");
      } catch (IOException ex) {
         log.error("could not initialize qgrammatcher: " + ex.getMessage());
      }
      out.close();
      log.info("qmatch: done; total time was %.1f sec", totalTimer.tocs());
      g.stopplog();

      return 0;
   }
}
