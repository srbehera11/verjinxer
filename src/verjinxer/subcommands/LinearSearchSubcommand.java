package verjinxer.subcommands;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

import verjinxer.GlobalMatchReporter;
import verjinxer.Globals;
import verjinxer.Project;
import verjinxer.sequenceanalysis.Sequences;
import verjinxer.util.FileTypes;
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
      
      final long[] ssp  = referenceSequences.getSeparatorPositions();
      GlobalMatchReporter matchReporter = new GlobalMatchReporter(ssp, 0, referenceSequences.array().length, false, out);

      log.info("%s: start searching.", commandname);
      totalTimer.tic();
      for (int i = 0; i < querySequences.getNumberSequences(); i++) { // for each query
         final int[] queryBoundaries = querySequences.getSequenceBoundaries(i);
         final int beginQuery = queryBoundaries[0];
         final int endQuery = queryBoundaries[1];
         matchReporter.setSequenceStart(beginQuery);

         int[] pos = search(querySequences.array(), beginQuery, endQuery, referenceSequences.array());
         
         for(int j = 0; j < pos.length; j++) {
            matchReporter.add(pos[j], 0, endQuery-beginQuery);
         }
         matchReporter.write(i);
         matchReporter.clear();

      }
      log.info("%s: search finished after %.1f secs.", commandname, totalTimer.tocs());
      return 0;
   }
   
   /**
    * Searches the query in chrM and returns the number of occurrence.
    * @param query
    *           String to search for.
    * @return Positions where query was found within chrM.
    */
   private int[] search(byte[] query, int beginQuery, int endQuery, byte[] reference) {
      Vector<Integer> posVec = new Vector<Integer>();
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
            posVec.add(i);
         }
      }
      
      int[] posArray = new int[posVec.size()];
      for(int i = 0; i < posArray.length; i++) {
         posArray[i] = posVec.get(i);
      }
      return posArray;
   }
}
