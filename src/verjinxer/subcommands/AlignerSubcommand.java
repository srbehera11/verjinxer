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
import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.sequenceanalysis.Sequences;
import verjinxer.sequenceanalysis.alignment.AlignerFactory;
import verjinxer.sequenceanalysis.alignment.Aligner;
import verjinxer.sequenceanalysis.alignment.AlignmentResult;
import verjinxer.util.ArrayUtils;
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
      File readsProjectFile = new File(args[0]);
      File referenceProjectFile = new File(args[1]);
      File matchesFile = new File(args[2]);

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
         queriesProject = Project.createFromFile(readsProjectFile);
         referencesProject = Project.createFromFile(referenceProjectFile);
      } catch (IOException ex) {
         log.error("Cannot read project file " + ex.getMessage());
         return 1;
      }
      g.startProjectLogging(referencesProject);

      // --output
      File outputFile = queriesProject.makeFile(FileTypes.MAPPED, FileUtils.removeExtension(matchesFile).getName());
      if (opt.isGiven("o")) {
         String option = opt.get("o");
         if (option.length() == 0 || option.startsWith("#")) {
            outputFile = null;
         }
         else {
            if (!option.endsWith(FileTypes.MAPPED.toString())) { //file extension should be .mapped
               option += FileTypes.MAPPED;
            }
            outputFile = new File(option);
         }
      }
      
      PrintWriter out;
      if (outputFile == null) {
         out = new PrintWriter(System.out);
      } else {
         try {
            out = new PrintWriter(
                  new BufferedOutputStream(new FileOutputStream(outputFile), 32 * 1024), false);
         } catch (FileNotFoundException ex) {
            log.error("could not create output file: " + ex.getMessage());
            return 1;
         }
      }

      log.info("Will write results to %s", (outputFile != null ? "'" + outputFile + "'" : "stdout"));
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
         matchesReader = new MatchesReader(matchesFile);
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
            AlignerSubcommand.AlignedQuery aligned = AlignerSubcommand.alignMatchToReference(query, reference,
                  match.getQueryPosition(), match.getReferencePosition(), match.getLength(),
                  maximumErrorRate, alphabet);
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

      if (outputFile != null) {
         out.close();
      }
      log.info("done; total time was %.1f sec", totalTimer.tocs());
      g.stopplog();

      return 0;
   }
   
   /** A query that has been aligned to a reference. */
   public static class AlignedQuery {
      private final int start, stop, errors;
   
      public AlignedQuery(int start, int stop, int errors) {
         this.start = start;
         this.stop = stop;
         this.errors = errors;
      }
   
      /** Start position of the query relative to the beginning of the reference */
      public int getStart() {
         return start;
      }
   
      /** Stop position of the query relative to the beginning of the reference */
      public int getStop() {
         return stop;
      }
   
      /** Number of errors of the alignment */
      public int getErrors() {
         return errors;
      }
   }

   /**
    * Aligns a query to a reference sequence, given an interval on query and reference that matches
    * exactly (seed). Starting from the exact match, the match is first extended to the right and
    * then to the left.
    * 
    * @param query
    *           The entire query sequence.
    * @param reference
    *           The entire reference sequence.
    * @param queryPosition
    *           Position of the exact match on the query sequence.
    * @param referencePosition
    *           Position of the exact match on the reference sequence.
    * @param length
    *           Length of the exact match.
    * @param maximumErrorRate
    *           Allow at most query.length * maximumErrorRate errors during the alignment.
    * @param alphabet
    *           The alphabet used for the query and the reference.
    * @return If there were too many errors, null is returned. Otherwise, an AlignedQuery is
    *         returned.
    */
   public static AlignedQuery alignMatchToReference(final byte[] query, final byte[] reference,
         final int queryPosition, final int referencePosition, final int length,
         final double maximumErrorRate, Alphabet alphabet) {

      final int maximumNumberOfErrors = (int) (query.length * maximumErrorRate);

      // if you want to convert a byte[] to a String for debugging, use alphabet.preimage(array)

      Aligner aligner = AlignerFactory.createForwardAligner();
      AlignmentResult alignedEnd = aligner.align(query, queryPosition + length,
            query.length, reference, referencePosition + length, reference.length, alphabet);

      if (alignedEnd.getErrors() > maximumNumberOfErrors) {
         return null;
      }

      byte[] reversedFrontQuery = Arrays.copyOf(query, queryPosition);
      ArrayUtils.reverseArray(reversedFrontQuery, -1);
      byte[] reversedFrontReference = Arrays.copyOf(reference, referencePosition);
      ArrayUtils.reverseArray(reversedFrontReference, -1);

      AlignmentResult alignedFront = aligner.align(reversedFrontQuery,
            reversedFrontReference, alphabet);

      if (alignedFront.getErrors() > maximumNumberOfErrors - alignedEnd.getErrors()) {
         return null;
      }
      assert alignedFront.getErrors() + alignedEnd.getErrors() <= maximumNumberOfErrors;

      int start = referencePosition
            - (alignedFront.getEndPosition().column - alignedFront.getBeginPosition().column);
      int stop = referencePosition + length
            + (alignedEnd.getEndPosition().column - alignedEnd.getBeginPosition().column);
      int errors = alignedFront.getErrors() + alignedEnd.getErrors();

      return new AlignedQuery(start, stop, errors);

      // TODO some string twiddling would be necessary here to compute the actual alignment
      // TODO if an actual alignment is not needed, we could use a faster version of forwardAlign
   }
}
