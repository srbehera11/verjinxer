package verjinxer.subcommands;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

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
   
   public Sequence2FastqSubcommand(Globals g) {
      this.g = g;
   }

   @Override
   public void help() {
      log.info("Usage:  %s seq2fastq [options] <Projects ...>", programname);
      log.info("Creats a fastq file of %s, %s and %s file.", FileTypes.SEQ, FileTypes.DESC, FileTypes.QUALITIY);
      log.info("Writes %s for nucleotide space or %s for color space.", FileTypes.FASTQ, FileTypes.CSFASTQ);
      log.info("Options:");
      log.info("  -d                      use double encoded color space for output file and (writes %s)", FileTypes.DECSFASTQ);
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
         final FileTypes outputFileType;
         final boolean colorspace =  project.getBooleanProperty("ColorSpaceAlphabet");
         if (colorspace) {
            if (doubleEncodedCS) {
               outputFileType = FileTypes.DECSFASTQ;
            } else {
               outputFileType = FileTypes.CSFASTQ;
            }
         } else {
            if (doubleEncodedCS) {
               log.warn("seq2fastq: Option -d only available for color space alphabet. Skipping %s%n.", project.getName());
               outputFileType = null;
               continue;
            } else {
               outputFileType = FileTypes.FASTQ;
            }
         }
         
            if (!encode(project, outputFileType)) {
               return 1;
            }
      }
      
      return 0;
   }

   private boolean encode(Project project, FileTypes outputFileType) {
      Alphabet alphabet = project.readAlphabet();
      Sequences sequences = project.readSequences();
      
      PrintWriter writer = null;
      try {
         writer = new PrintWriter( new BufferedWriter(new FileWriter(project.makeFile(outputFileType)) ) );
      } catch (IOException e) {
         log.error("seq2fastq: cannot create output file %s: %s",project.makeFile(outputFileType),e);
         return false;
      }
      
      for (int i = 0; i < sequences.getNumberSequences(); i++) {
         final byte[] sequence = sequences.getSequence(i);
         final String description = sequences.getDescriptions().get(i);
         final byte[] qualityValues;
         try {
            qualityValues = sequences.getQualityValuesForSequence(i); //TODO this throw an exception
         } catch (IOException e) {
            log.error("seq2fastq: cannot read quality values for sequence %d: %s",i,e);
            writer.close();
            return false;
         }
         
         writer.println("@" + description);
         try {
            writer.println(alphabet.preimage(sequence));
         } catch (InvalidSymbolException e) {
            log.error("seq2fastq: cannot encode sequence %d: %s",i,e);
            writer.close();
            return false;
         }
         writer.println("+");
         StringBuilder sb = new StringBuilder(qualityValues.length);
         for(byte b: qualityValues) {
            sb.append((char) b+33);
            sb.append(" ");
         }
         writer.println(sb);
      }
      writer.close();
      return true;
   }

}
