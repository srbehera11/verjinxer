package verjinxer.subcommands;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import verjinxer.Globals;
import verjinxer.QGramMatcher;
import verjinxer.sequenceanalysis.BisulfiteQGramCoder;
import verjinxer.sequenceanalysis.QGramCoder;
import verjinxer.sequenceanalysis.QGramFilter;
import verjinxer.util.IllegalOptionException;
import verjinxer.util.Options;
import verjinxer.util.ProjectInfo;
import verjinxer.util.TicToc;
import static verjinxer.Globals.programname;
import com.spinn3r.log5j.Logger;

public class AlignerSubcommand implements Subcommand {
   private static final Logger log = Globals.getLogger();
   private Globals g;

   public AlignerSubcommand(Globals g) {
      this.g = g;
   }

   @Override
   public void help() {
      log.info("Usage:");
      log.info("  %s align  [options]  <reads> <reference> <matches file>", programname);
      log.info("Aligns exact seeds from a .matches file between a reads and a reference.");
      log.info("Writes a .mapped file.");
      log.info("Options:");
      log.info("  -q, --qualities      Use qualities while aligning.");
   }

   @Override
   public int run(String[] args) {
      TicToc totalTimer = new TicToc();
      g.cmdname = "align";

      Options opt = new Options("q=qualities:");
      try {
         args = opt.parse(args);
      } catch (IllegalOptionException ex) {
         log.error(ex);
         return 1;
      }

      if (args.length != 3) {
         help();
         log.error("need three parameters");
         return 1;
      }
      String readsProjectName = args[0];
      String referenceProjectName = args[1];
      String matchesFilename = args[2];

      ProjectInfo readsProject, referenceProject;
      try {
         readsProject = ProjectInfo.createFromFile(readsProjectName);
         referenceProject = ProjectInfo.createFromFile(referenceProjectName);
      } catch (IOException ex) {
         log.error("cannot read project files.");
         return 1;
      }

      // TODO why index, not reads?
      g.startProjectLogging(referenceProject);

      final int asize, q;

      // Read project data and determine asize, q; read alphabet map
      // try {
      // asize = indexProject.getIntProperty("qAlphabetSize");
      // q = indexProject.getIntProperty("q");
      // } catch (NumberFormatException ex) {
      // log.error("q-grams for index '%s' not found (Re-create the q-gram index!); %s",
      // ds, ex);
      // return 1;
      // }

      // q-gram filter options from -F option
//      final QGramFilter qgramfilter = new QGramFilter(q, asize, opt.get("F"));

      // Prepare the sequence filter
      int maxseqmatches = Integer.MAX_VALUE;
      if (opt.isGiven("M"))
         maxseqmatches = Integer.parseInt(opt.get("M"));

      // String toomanyhitsfilename = null;
      // if (opt.isGiven("t")) {
      // if (opt.get("t").startsWith("#")) {
      // toomanyhitsfilename = dt + ".toomanyhits-filter";
      // } else {
      // toomanyhitsfilename = g.dir + opt.get("t");
      // }
      // }

      // Determine option values
      // boolean sorted = opt.isGiven("s");
      // int minlen = (opt.isGiven("l") ? Integer.parseInt(opt.get("l")) : q);
      // if (minlen < q) {
      // log.warn("increasing minimum match length to q=%d!", q);
      // minlen = q;
      // }
      int minseqmatches = (opt.isGiven("m") ? Integer.parseInt(opt.get("m")) : 1);
      if (minseqmatches < 1) {
         log.warn("increasing minimum match number to 1!");
         minseqmatches = 1;
      }

      // String outname = String.format("%s-%s-%dx%d", tname, sname, minseqmatches, minlen);
      String outname = "blabla";
      if (opt.isGiven("o")) {
         if (outname.length() == 0 || outname.startsWith("#"))
            outname = null;
         else
            outname = opt.get("o");
      }
      if (outname != null)
         outname = g.outdir + outname + ".sorted-matches";
      log.info("will write results to %s", (outname != null ? "'" + outname + "'" : "stdout"));

      final boolean bisulfiteQueries = opt.isGiven("b");

      // start output
      PrintWriter out = new PrintWriter(System.out);
      if (outname != null) {
         try {
            out = new PrintWriter(
                  new BufferedOutputStream(new FileOutputStream(outname), 32 * 1024), false);
         } catch (FileNotFoundException ex) {
            log.error("could not create output file.");
            return 1;
         }
      }

      out.close();
      log.info("done; total time was %.1f sec", totalTimer.tocs());
      g.stopplog();

      return 0;
   }
}
