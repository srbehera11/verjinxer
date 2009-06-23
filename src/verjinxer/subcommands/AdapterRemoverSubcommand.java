package verjinxer.subcommands;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import com.spinn3r.log5j.Logger;
import static verjinxer.Globals.programname;
import verjinxer.Globals;
import verjinxer.Project;
import verjinxer.Translater;
import verjinxer.sequenceanalysis.Aligner;
import verjinxer.sequenceanalysis.SequenceWriter;
import verjinxer.sequenceanalysis.Sequences;
import verjinxer.util.IllegalOptionException;
import verjinxer.util.Options;

/**
 * 
 * @author Markus Kemmerling
 */
public class AdapterRemoverSubcommand implements Subcommand {

   final Globals g;
   private static final Logger log = Globals.getLogger();

   public AdapterRemoverSubcommand(Globals g) {
      this.g = g;
   }
   
   /**
    * prints help on usage and options
    */
   @Override
   public void help() {
      log.info("Usage:  %s rmadapt [options] <sequence> <outProject> <adapters as FASTA file>", programname); // TODO verbalize usage better
                                                                                                              // TODO FASTA files must be translated with alphabet of project
                                                                                                              // TODO if sequence has quality files, this must be copied into outProject and cut like the sequence. For colorspace one more value must be cut (see python code).
      log.info("Reads a FASTA file, finds and removes adapters,");
      log.info("and writes the changed sequence to outProject.");
      log.info("When finished, statistics are printed to standard output (not yet implemented)."); // TODO
      log.info("");
      log.info("  -e error_rate   Maximum error rate (errors divided by length of matching region)");
      log.info("  -p length       Print the found alignments if they are longer than length (not yet implemented)."); // TODO
      log.info("  -c              Colorspace mode: Removes first nucleotide; trims adapter correctly.");
      log.info("  -n <count>      Try to remove adapters at most <count> times (not yet implemented)"); // TODO
   }

   /**
    * @param args
    *           the command line arguments
    */
   public static void main(String[] args) {
      System.exit(new AdapterRemoverSubcommand(new Globals()).run(args));
   }

   @Override
   public int run(String[] args) {
      Options opt = new Options("e:,p:,c,n:");
      try {
         args = opt.parse(args);
      } catch (IllegalOptionException ex) {
         log.error("%s", ex);
         return 1;
      }
      
      if (args.length < 1) {
         help();
         log.error("rmadapt: sequence file must be specified.");
         return 1;
      }
      
      int min_print_align_length = -1;
      double max_error_rate = 2.2/18;
      boolean colorspace = false;
      int times = 1;
      
      if (opt.isGiven("p")) {
         min_print_align_length = Integer.parseInt(opt.get("p"));
      }
      if (opt.isGiven("c")) {
         colorspace = true;
      }
      if (opt.isGiven("n")) {
         times = Integer.parseInt(opt.get("n"));
      }
      if (opt.isGiven("e")) {
         String[] tmp = opt.get("e").split("/");
         if ( tmp.length == 1 ) {
            max_error_rate = Double.parseDouble(tmp[0]);
         } else {
            max_error_rate = Double.parseDouble(tmp[0]) / Double.parseDouble(tmp[1]); 
         }
         System.out.println(max_error_rate);
      }
      
      Project sequenceProject = null, targetProject = null;
      final File sequenceProjectFile = new File(args[0]);
      final File targetProjectFile = new File(args[1]);
      final File[] adapterFiles = new File[args.length-2];
      for(int i = 0; i < adapterFiles.length; i++) {
         adapterFiles[i] = new File (args[i+2]);
      }
      try {
         sequenceProject = Project.createFromFile(sequenceProjectFile);
         targetProject = Project.createFlatCopy(sequenceProjectFile, targetProjectFile);
      } catch (IOException e) {
         log.error("rmadapt: cannot read project files; %s", e);
         return 1;
      }
      
      //translate adapters
      SequenceWriter adapterSequenceWriter = null;
      try {
         adapterSequenceWriter = new SequenceWriter(targetProject, "adapters");
      } catch (IOException ex) {
         log.error("rmadapt: could not create output files for translated adapters; %s", ex);
         return 1;
      }
      Translater translater = new Translater(null, targetProject.readAlphabet());
      for (int i = 0; i < adapterFiles.length; i++) {
         translater.translateFasta(adapterFiles[i], adapterSequenceWriter);
      }
      try {
         adapterSequenceWriter.store(); // stores seq, ssp and desc
      } catch (IOException ex) {
         log.error("rmadapt: could not store translated adapters; %s", ex);
         return 1;
      }
      
      Sequences sequences = sequenceProject.readSequences();
      ArrayList<String> descriptions = sequences.getDescriptions();
      Sequences adapters = targetProject.readSequences("adapters");
     
      SequenceWriter sequenceWriter = null;
      long lastbyte = 0;
      try {
         sequenceWriter = new SequenceWriter(targetProject);
      } catch (IOException ex) {
         log.error("rmadapt: could not create output files for cutted sequences; %s", ex);
         return 1;
      }
      
      for(int i = 0; i < sequences.getNumberSequences(); i++) {
         byte[] sequence = sequences.getSequence(i);

         if (colorspace) {
            sequence = Arrays.copyOfRange(sequence, 2, sequence.length);
            //TODO handle quality file
         }
         
         //TODO use times
         
         Aligner.SemiglobalAlignmentResult bestResult = new Aligner.SemiglobalAlignmentResult(null, null, 0,Integer.MIN_VALUE,0);
         assert bestResult.getLength() - bestResult.getErrors() == Integer.MIN_VALUE;
         for(int j = 0; j < adapters.getNumberSequences(); j++) {
            Aligner.SemiglobalAlignmentResult result = Aligner.semiglobalAlign(adapters.getSequence(j), sequence);
            if (result.getLength()-result.getErrors() > bestResult.getLength()-bestResult.getErrors()) {
               bestResult = result;
            }
         }
         
         if (bestResult.getLength() > 0 && (double)bestResult.getErrors() / bestResult.getLength() <= max_error_rate) {
            final byte[] adapterAlignment = bestResult.getSequence1();
            if(adapterAlignment[0] != Aligner.GAP && adapterAlignment[adapterAlignment.length-1] != Aligner.GAP) {
               // The adapter or parts of it covers the entire read
               //TODO what now?
            } else if (adapterAlignment[0] == Aligner.GAP) {
               // The adapter is at the end of the read
               if (adapterAlignment[adapterAlignment.length-1] == Aligner.GAP) {
                  // The adapter is in the middle of the read
                  //TODO what now?
               }
               
               //TODO maybe this should only be done if the adapter is not in the middle of the read (else to the upper if).
               if (colorspace) {
                  sequence = Arrays.copyOfRange(sequence, 0, bestResult.getBegin()-1);
                  //TODO handle quality file
               } else {
                  sequence = Arrays.copyOfRange(sequence, 0, bestResult.getBegin());
               }
            } else if (adapterAlignment[adapterAlignment.length-1] == Aligner.GAP) {
               // The adapter is in the beginning of the read
               sequence = Arrays.copyOfRange(sequence, bestResult.getLength(), sequence.length);
               //TODO handle quality file
            } else {
               // This should not happen!
            }
         }
         
         // Add a separator at the end of the sequence
         final byte separator = (byte) (targetProject.getIntProperty("Separator"));
         sequence = Arrays.copyOf(sequence, sequence.length+1);
         sequence[sequence.length-1] = separator;

         // Write cuted sequence to target project
         try {
            lastbyte = sequenceWriter.writeBuffer(ByteBuffer.wrap(sequence));
            sequenceWriter.addInfo(descriptions.get(i), sequence.length, (int) (lastbyte - 1));
            //TODO descriptions must be transformed, when length stands in
         } catch (IOException ex) {
            log.error("rmadapt: could not write cutted sequences; %s", ex);
            return 1;
         }
         
         //TODO write cuted quality file to target project
         
      }
      
      // Store the whole target project
      try {
         sequenceWriter.store(); // stores seq, ssp and desc
      } catch (IOException ex) {
         log.error("rmadapt: could not store cutted sequences; %s", ex);
         return 1;
      }
      long totallength = sequenceWriter.length();
      targetProject.setProperty("Length", totallength);
      targetProject.setProperty("NumberSequences", sequenceWriter.getNumberSequences());

      // Write sequence length statistics.
      targetProject.setProperty("LongestSequence", sequenceWriter.getMaximumLength());
      targetProject.setProperty("ShortestSequence", sequenceWriter.getMinimumLength());
      
      targetProject.setProperty("LastAction", "rmadapt");
      
      return 0;
   }
}

