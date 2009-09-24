package verjinxer.subcommands;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import verjinxer.BWTIndexBuilder;
import verjinxer.BWTSearch;
import verjinxer.GlobalMatchReporter;
import verjinxer.Globals;
import verjinxer.Project;
import verjinxer.sequenceanalysis.BWTIndex;
import verjinxer.sequenceanalysis.Sequences;
import verjinxer.util.FileTypes;
import verjinxer.util.StringUtils;
import verjinxer.util.TicToc;
import static verjinxer.Globals.programname;
import com.spinn3r.log5j.Logger;

/**
 * @author Markus Kemmerling
 */
public class BWTSearchSubcommand implements Subcommand {
   private static final Logger log = Globals.getLogger();
   private Globals g;
   private static final String commandname = "bwtsearch";

   /**
    * prints help on usage
    */
   @Override
   public void help() {
      log.info("Usage:");
      log.info("  %s %s  [options]  <query>  <reference>", programname, commandname);
      log.info("Reports for each sequences in query all occurances in reference");
      log.info("in human-readable output format. Writes %s.", FileTypes.MATCHES);
      log.info("Options:");
   }
   
   /**
    * @param gl
    *           the Globals structure
    */
   public BWTSearchSubcommand(Globals gl) {
      g = gl;
   }

   @Override
   public int run(String[] args) {
      TicToc totalTimer = new TicToc();
      Globals.cmdname = commandname;
      String action = commandname + " \"" + StringUtils.join("\" \"", args) + "\"";
      
      File queryProjectName = null;
      File referenceProjectName = null;
      if (args.length == 2) {
         queryProjectName = new File(args[0]);
         referenceProjectName = new File(args[1]);
      } else {
         help();
         log.error("%s: two projects must be given.",commandname);
         return 1;
      }

      // Read project data
      Project referenceProject, queryProject;
      try {
         queryProject = Project.createFromFile(queryProjectName);
         referenceProject = Project.createFromFile(referenceProjectName);
      } catch (IOException ex) {
         log.error("%s: cannot read project files.",commandname);
         return 1;
      }
      g.startProjectLogging(referenceProject);
      
      // Read BWT from disc and create BWTIndex from it.
      File bwtFile = referenceProject.makeFile(FileTypes.BWT);
      File bwt2textFile = referenceProject.makeFile(FileTypes.SAMPLEDPOS);
      byte[] bwt;
      int[] bwt2text;
      if (bwtFile.exists()) {
         log.info("%s: reading bwt from disc.", commandname);
         totalTimer.tic();
         bwt = g.slurpByteArray(bwtFile);
         bwt2text = g.slurpIntArray(bwt2textFile);
         log.info("%s: reading took %.1f secs.", commandname, totalTimer.tocs());
      } else {
         log.error("%s: pleace build a bwt of the reference project first.",commandname);
         return 1;
      }
      
      log.info("%s: constructing bwt index.", commandname);
      totalTimer.tic();
      BWTIndex referenceIndex = BWTIndexBuilder.build(bwt, bwt2text);
      log.info("%s: bwt index completed after %.1f secs.", commandname, totalTimer.tocs());
      
      // determine where to write result:
      final File outfile = referenceProject.makeFile(FileTypes.MATCHES);
      PrintWriter out;
      try {
         out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(outfile), 32 * 1024),
               false);
      } catch (FileNotFoundException ex) {
         log.error("%s: could not create output file.", commandname);
         return 1;
      }
      
      log.info("%s: reading query sequence from disc.", commandname);
      totalTimer.tic();
      Sequences querySequences = queryProject.readSequences();
      log.info("%s: reading took %.1f secs.", commandname, totalTimer.tocs());

      
      final File sspfile = referenceProject.makeFile(FileTypes.SSP);
      final long[] ssp  = g.slurpLongArray(sspfile); // no need for the sequence, so only read sspfile instead of referenceProject.readSequences()
      GlobalMatchReporter matchReporter = new GlobalMatchReporter(ssp, 0, referenceIndex.size(), false, out);

      log.info("%s: start searching.", commandname);
      totalTimer.tic();
      //search each query sequence in reference (resp. in its index)
      for (int i = 0; i < querySequences.getNumberSequences(); i++) { //for each query
         final int[] queryBoundaries = querySequences.getSequenceBoundaries(i);
         final int beginQuery = queryBoundaries[0];
         final int endQuery = queryBoundaries[1];
         matchReporter.setSequenceStart(beginQuery);
         
//         System.out.printf("%s: searching for query %d%n.", commandname, i);
         
         final BWTSearch.BWTSearchResult result = BWTSearch.find(querySequences.array(), beginQuery, endQuery, referenceIndex);

//         System.out.printf("%s: reporing results for query %d%n.", commandname, i);
         
         for(int j = 0; j < result.positions.length; j++) {
            matchReporter.add(result.positions[j], 0, endQuery-beginQuery);
         }
//         System.out.printf("%s: write results for query %d%n.", commandname, i);
         matchReporter.write(i);
         matchReporter.clear();
      }
      out.close();
      log.info("%s: search finished after %.1f secs.", commandname, totalTimer.tocs());

      // update project data
      referenceProject.setProperty("BWTSearchAction", action);
      referenceProject.setProperty("LastAction", commandname);
      // write project data
      try {
         referenceProject.store();
      } catch (IOException ex) {
         log.warn("%s: could not write %s (%s)!", commandname,
               referenceProject.getFile().getPath(), ex);
         return 1;
      }
      return 0;
   }

}
