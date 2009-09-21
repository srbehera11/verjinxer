package verjinxer.subcommands;

import java.io.File;
import java.io.IOException;

import verjinxer.Globals;
import verjinxer.Project;
import verjinxer.sequenceanalysis.Sequences;
import verjinxer.util.StringUtils;
import verjinxer.util.TicToc;

import com.spinn3r.log5j.Logger;

/**
 * This is search algorithm is very slow and mainly considered as reference algorithm for testing
 * purpose.
 * 
 * @author Markus Kemmerling
 */
public class LinearSearchSubcommand implements Subcommand {

   private static final Logger log = Globals.getLogger();
   private Globals g;
   private static final String commandname = "linearsearch";

   /**
    * prints help on usage
    */
   @Override
   public void help() {
      log.info("Usage:");
      log.info("  %s %s  [options]  <query>  <reference>", Globals.programname, commandname);
      log.info("Reports for each sequences in query all occurances in reference");
      log.info("Options:");
   }

   /**
    * @param gl
    *           the Globals structure
    */
   public LinearSearchSubcommand(Globals gl) {
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
         log.error("%s: two projects must be given.", commandname);
         return 1;
      }

      // Read project data
      Project referenceProject, queryProject;
      try {
         queryProject = Project.createFromFile(queryProjectName);
         referenceProject = Project.createFromFile(referenceProjectName);
      } catch (IOException ex) {
         log.error("%s: cannot read project files.", commandname);
         return 1;
      }
      g.startProjectLogging(referenceProject);

      Sequences querySequences = queryProject.readSequences();
      Sequences referenceSequences = referenceProject.readSequences();

      // TODO now it only counts the occurrences.
      // TODO make a real search and show the positions.

      log.info("%s: start searching.", commandname);
      totalTimer.tic();
      for (int i = 0; i < querySequences.getNumberSequences(); i++) { // for each query
         final int[] queryBoundaries = querySequences.getSequenceBoundaries(i);
         final int beginQuery = queryBoundaries[0];
         final int endQuery = queryBoundaries[1];

         int occ = search(querySequences.array(), beginQuery, endQuery, referenceSequences.array());
         System.out.printf("Query %d (%s) was found %s times in the reference.%n", i,
               querySequences.getDescriptions().get(i), occ);

      }
      log.info("%s: search finished after %.1f secs.", commandname, totalTimer.tocs());
      return 0;
   }

   private int search(byte[] query, int beginQuery, int endQuery, byte[] reference) {
      int number = 0;
      final int queryLength = endQuery - beginQuery;
      for (int i = 0; i < reference.length - queryLength + 1; i++) {
         boolean find = true;
         for (int j = 0; j < queryLength; j++) {
            if (reference[i + j] != query[beginQuery + j]) {
               find = false;
               break;
            }
         }

         if (find) {
            number++;
         }
      }
      // System.out.println(number);
      return number;
   }
}
