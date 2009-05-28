package verjinxer.subcommands;

import static verjinxer.Globals.programname;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

import verjinxer.Globals;
import verjinxer.Project;
import verjinxer.sequenceanalysis.Aligner;
import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.sequenceanalysis.Sequences;
import verjinxer.util.FileTypes;
import verjinxer.util.FileUtils;
import verjinxer.util.IllegalOptionException;
import verjinxer.util.Match;
import verjinxer.util.MatchesReader;
import verjinxer.util.Options;
import verjinxer.util.TicToc;

import com.spinn3r.log5j.Logger;

/**
 * 
 * @author Marcel Martin
 *
 */
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
      log.info("  -o, --output <filename>  Output file (use # for stdout) (%s is appended)", FileTypes.MAPPED);
      log.info("  -q, --qualities          Use qualities while aligning.");
      log.info("  -e <rate>                Maximum error rate");
   }

   @Override
   public int run(String[] args) {
      TicToc totalTimer = new TicToc();
      g.cmdname = "align";

      Options opt = new Options("q=qualities:,e:,o=output:");
      try {
         args = opt.parse(args);
      } catch (IllegalOptionException ex) {
         log.error(ex);
         return 1;
      }
      if (args.length != 3) {
         help();
         log.error("Three parameters required!");
         return 1;
      }
      String readsProjectFileName = args[0];
      String referenceProjectFileName = args[1];
      String matchesFileName = args[2];

      // --qualities
      if (opt.isGiven("q")) {
         log.error("qualities not supported, yet!");
         return 1;
      }
      
      // -e
      double maximumErrorRate = 0.1;
      if (opt.isGiven("e")) {
         maximumErrorRate = Double.parseDouble(opt.get("e"));
      }

      Project queriesProject, referencesProject;
      try {
         queriesProject = Project.createFromFile(readsProjectFileName);
         referencesProject = Project.createFromFile(referenceProjectFileName);
      } catch (IOException ex) {
         log.error("Cannot read project file " + ex.getMessage());
         return 1;
      }
      g.startProjectLogging(referencesProject);

      // --output
      String outputFileName = FileUtils.extensionRemoved(matchesFileName) + FileTypes.MAPPED;
      outputFileName = queriesProject.getWorkingDirectory().getAbsolutePath() + File.separator
            + outputFileName;
      if (opt.isGiven("o")) {
         if (outputFileName.length() == 0 || outputFileName.startsWith("#")) {
            outputFileName = null;
         }
         else {
            outputFileName = opt.get("o");
            if (!outputFileName.endsWith(FileTypes.MAPPED.toString())) { //file extension should be .mapped
               outputFileName += FileTypes.MAPPED;
            }
         }
      }
      
      PrintWriter out;
      if (outputFileName == null) {
         out = new PrintWriter(System.out);
      } else {
         try {
            out = new PrintWriter(
                  new BufferedOutputStream(new FileOutputStream(outputFileName), 32 * 1024), false);
         } catch (FileNotFoundException ex) {
            log.error("could not create output file: " + ex.getMessage());
            return 1;
         }
      }

      log.info("Will write results to %s", (outputFileName != null ? "'" + outputFileName + "'" : "stdout"));
      log.info("Maximum error rate set to %f", maximumErrorRate);

      Sequences queries = queriesProject.readSequences();
      Sequences references = referencesProject.readSequences();
      Alphabet alphabet = queriesProject.readAlphabet();
      long[] referencesSeparatorPositions = references.getSeparatorPositions();
      long[] queriesSeparatorPositions = queries.getSeparatorPositions();
      final ArrayList<String> queriesDescriptions = queries.getDescriptions();
      final ArrayList<String> referenceDescriptions = references.getDescriptions();


      assert queriesDescriptions.size() == queriesSeparatorPositions.length;
      assert referenceDescriptions.size() == referencesSeparatorPositions.length;

      MatchesReader matchesReader;
      try {
         matchesReader = new MatchesReader(matchesFileName);
      } catch (IOException ex) {
         ex.printStackTrace();
         return 1;
      }
      Match match;

      int i = 1;
      try {
         while ((match = matchesReader.readMatch()) != null) {
            // System.out.format("aligning match: %d %d %d %d %d%n", match.getQueryNumber(),
            // match.getQueryPosition(), match.getReferenceNumber(),
            // match.getReferencePosition(), match.getLength());

            // copy query and reference
            int qn = match.getQueryNumber();
            // TODO implement a method Sequences.getCopyOfSequence(int i) and use it here
            byte[] query = Arrays.copyOfRange(queries.array(), qn == 0 ? 0
                  : (int) queriesSeparatorPositions[qn - 1] + 1,
                  (int) queriesSeparatorPositions[qn]);
            int rn = match.getReferenceNumber();
            byte[] reference = Arrays.copyOfRange(references.array(), rn == 0 ? 0
                  : (int) referencesSeparatorPositions[rn - 1] + 1,
                  (int) referencesSeparatorPositions[rn]);
            Aligner.AlignedQuery aligned = Aligner.alignMatchToReference(query, reference,
                  match.getQueryPosition(), match.getReferencePosition(), match.getLength(),
                  maximumErrorRate);
            if (aligned != null) {
               out.format("%d %d %d %d %d%n", match.getQueryNumber(),
                     match.getReferenceNumber(), aligned.getStart(), aligned.getStop(),
                     aligned.getErrors());
            }
            if (i % 100000 == 0) {
               System.out.printf("%9d matches processed (%.1fs)%n", i, totalTimer.tocs());
            }
            i++;
         }
      } catch (IOException ex) {
         ex.printStackTrace();
         return 1;
      } catch (NumberFormatException ex) {
         ex.printStackTrace();
         return 1;
      }

      if (outputFileName != null) {
         out.close();
      }
      log.info("done; total time was %.1f sec", totalTimer.tocs());
      g.stopplog();

      return 0;
   }
}
