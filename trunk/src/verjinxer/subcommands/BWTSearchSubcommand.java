package verjinxer.subcommands;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.IntBuffer;

import verjinxer.BWTIndexBuilder;
import verjinxer.BWTSearch;
import verjinxer.Globals;
import verjinxer.Project;
import verjinxer.SuffixTrayBuilder;
import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.sequenceanalysis.BWTIndex;
import verjinxer.sequenceanalysis.Sequences;
import verjinxer.sequenceanalysis.SuffixXorDLL;
import verjinxer.util.FileTypes;
import verjinxer.util.TicToc;
import static verjinxer.Globals.programname;
import com.spinn3r.log5j.Logger;

/**
 * @author Markus Kemmerling
 */
public class BWTSearchSubcommand implements Subcommand {
   private static final Logger log = Globals.getLogger();
   private Globals g;
   private static final String commandname = "bwt-search";

   /**
    * prints help on usage
    */
   @Override
   public void help() {
      log.info("Usage:");
      log.info("  %s %s  [options]  <query>  <reference>", programname, commandname);
      log.info("Reports for all sequences in query all occurances in reference");
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
      
      File queryProjectName = null;
      File referenceProjectName = null;
      if (args.length == 2) {
         queryProjectName = new File(args[0]);
         referenceProjectName = new File(args[1]);
      } else {
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
      byte[] bwt; 
      if (bwtFile.exists()) {
         bwt = g.slurpByteArray(bwtFile);
      } else {
         log.error("%s: pleace build a bwt of the reference project first.",commandname);
         return 1;
      }
      
      BWTIndex referenceIndex = BWTIndexBuilder.build(bwt);
      
      
      
      // determine where to write result:
      final File outfile = referenceProject.makeFile(FileTypes.MATCHES);
      PrintWriter out;
      if (outfile != null) {
         try {
            out = new PrintWriter(
                  new BufferedOutputStream(new FileOutputStream(outfile), 32 * 1024), false);
         } catch (FileNotFoundException ex) {
            log.error("%s: could not create output file.", commandname);
            return 1;
         }
      } else {
         out = new PrintWriter(System.out);
      }

      //search each query sequence in reference (resp. in its index)
      Sequences querySequences = queryProject.readSequences();
      for (int i = 0; i < querySequences.getNumberSequences(); i++) { //for each query
         final int[] queryBoundaries = querySequences.getSequenceBoundaries(i);
         final int beginQuery = queryBoundaries[0];
         final int endQuery = queryBoundaries[1];
         
         final BWTSearch.BWTSearchResult result = BWTSearch.find(querySequences.array(), beginQuery, endQuery, referenceIndex);
         
         // start output
         out.printf("Query %d (%s) was found %s times in the reference.%n", i,querySequences.getDescriptions().get(i), result.number );
      }
      
      return 0;
   }

}
