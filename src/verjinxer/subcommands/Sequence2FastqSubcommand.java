package verjinxer.subcommands;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.spinn3r.log5j.Logger;

import verjinxer.Globals;
import verjinxer.Project;
import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.sequenceanalysis.InvalidSymbolException;
import verjinxer.sequenceanalysis.Sequences;
import verjinxer.util.FileTypes;
import verjinxer.util.IllegalOptionException;
import verjinxer.util.Options;
import static verjinxer.Globals.programname;

/**
 * 
 * @author Markus Kemmerling
 */
public class Sequence2FastqSubcommand implements Subcommand {

   private static final Logger log = Globals.getLogger();
   private Globals g;

   /**
    * Pattern to extract information out of a description. Used in {@link
    * createAndWriteOutput(Project, FileTypes)}
    */
   private static final Pattern oldDescriptionPattern = Pattern.compile("^([0-9]+)_([0-9]+)_([0-9]+)_[FR]3");

   public Sequence2FastqSubcommand(Globals g) {
      this.g = g;
   }

   @Override
   public void help() {
      log.info("Usage:  %s seq2fastq [options] <Projects ...>", programname);
      log.info("Creats a fastq file of %s, %s and %s file.", FileTypes.SEQ, FileTypes.DESC,
            FileTypes.QUALITIY);
      log.info("Writes %s for nucleotide space or %s for color space.", FileTypes.FASTQ,
            FileTypes.CSFASTQ);
      log.info("Options:");
      log.info("  -d             use double encoded color space for output file (writes %s)",
            FileTypes.DECSFASTQ);
   }

   @Override
   public int run(String[] args) {
      Options opt = new Options("d");
      try {
         args = opt.parse(args);
      } catch (IllegalOptionException ex) {
         log.error("seq2fastq: " + ex);
         return 1;
      }

      if (args.length == 0) {
         help();
         log.error("seq2fastq: no projects given");
         return 0;
      }

      boolean doubleEncodedCS = opt.isGiven("d");
      Project project = null;

      // Loop through all projects
      for (int i = 0; i < args.length; i++) {
         try {
            project = Project.createFromFile(new File(args[i]));
         } catch (IOException ex) {
            log.error("seq2fastq: cannot read project file.");
            return 1;
         }
         g.startProjectLogging(project);

         // determine output file type
         final FileTypes outputFileType;
         final boolean colorspace = project.getBooleanProperty("ColorSpaceAlphabet");
         if (colorspace) {
            if (doubleEncodedCS) {
               outputFileType = FileTypes.DECSFASTQ;
            } else {
               outputFileType = FileTypes.CSFASTQ;
            }
         } else {
            if (doubleEncodedCS) {
               log.warn(
                     "seq2fastq: Option -d only available for color space alphabet. Skipping %s%n.",
                     project.getName());
               outputFileType = null;
               continue;
            } else {
               outputFileType = FileTypes.FASTQ;
            }
         }

         if (!createAndWriteOutput(project, outputFileType)) {
            return 1;
         }
      }

      return 0;
   }

   /**
    * Creates and writes a fastq file for the given project. The extension of the generated filename
    * depends on the given type. Furthermore, if outputFileType is FileTypes.DECSFASTQ, the
    * sequences of the project will be converted into double encoded color space before writing.
    * 
    * @param project
    * @param outputFileType
    * @return true iff sequences are successfully written onto disc.
    */
   private boolean createAndWriteOutput(Project project, FileTypes outputFileType) {
      Alphabet alphabet = project.readAlphabet();
      Sequences sequences = project.readSequences();

      PrintWriter writer = null;
      try {
         writer = new PrintWriter(new BufferedWriter(new FileWriter(
               project.makeFile(outputFileType))));
      } catch (IOException e) {
         log.error("seq2fastq: cannot create output file %s: %s", project.makeFile(outputFileType),
               e);
         return false;
      }

      // Loop through all sequences in the project
      for (int i = 0; i < sequences.getNumberSequences(); i++) {
         final byte[] sequence = sequences.getSequence(i);
         final String description = sequences.getDescriptions().get(i);
         final byte[] qualityValues;
         try {
            qualityValues = sequences.getQualityValuesForSequence(i);
         } catch (IOException e) {
            log.error("seq2fastq: cannot read quality values for sequence %d: %s", i, e);
            writer.close();
            return false;
         }

         // Create and write description
         Matcher matcher = oldDescriptionPattern.matcher(description);
         final boolean matched = matcher.matches();
         assert matched;
         writer.printf("@%s:%d_%d_%d/1%n", project.getName(), Integer.parseInt(matcher.group(1)),
               Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));

         // Create and write sequence. Therefore first decode sequence and than maybe encode into
         // double encoded color space
         String decodedSequence = null;
         try {
            decodedSequence = alphabet.preimage(sequence);
         } catch (InvalidSymbolException e) {
            log.error("seq2fastq: cannot encode sequence %d: %s", i, e);
            writer.close();
            return false;
         }
         if (outputFileType == FileTypes.DECSFASTQ) {
            decodedSequence = doubleEncode(decodedSequence);
         }
         writer.println(decodedSequence);

         // +
         writer.println("+");

         // Create and write quality values
         StringBuilder sb = new StringBuilder(qualityValues.length);
         for (byte b : qualityValues) {
            sb.append((char) (b + 33));
         }
         writer.println(sb);
      }
      writer.close();
      return true;
   }

   /**
    * Converts the given sequence in color space into double encoded color space.
    * 
    * @param csSequence
    * @return The sequence in double encoded color space.
    */
   private String doubleEncode(final String csSequence) {
      StringBuilder sb = new StringBuilder(csSequence.length());
      for (char c : csSequence.toCharArray()) {
         switch (c) {
         case '0':
            sb.append('A');
            break;
         case '1':
            sb.append('C');
            break;
         case '2':
            sb.append('G');
            break;
         case '3':
            sb.append('T');
            break;
         default:
            sb.append('N');
            break;
         }
      }

      return sb.toString();
   }
}
