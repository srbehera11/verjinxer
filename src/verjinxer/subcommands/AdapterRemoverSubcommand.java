package verjinxer.subcommands;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;

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
import verjinxer.util.TicToc;

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
      log.info("Reads a FASTA file, finds and removes adapters,");
      log.info("and writes the changed sequence to outProject.");
      log.info("When finished, statistics are printed to standard output (not yet implemented).");
      log.info("");
      log.info("  -e error_rate   Maximum error rate (errors divided by length of matching region)");
      log.info("  -p length       Print the found alignments if they are longer than length (not yet implemented).");
      log.info("  -c              Colorspace mode: Removes first nucleotide; trims adapter correctly."); // TODO is this option necessary? Can be determined by project properties
      log.info("  -n <count>      Try to remove adapters at most <count> times (not yet implemented)");
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
      TicToc totalTimer = new TicToc();
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
      double max_error_rate = 2.2 / 18;
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
         if (tmp.length == 1) {
            max_error_rate = Double.parseDouble(tmp[0]);
         } else {
            max_error_rate = Double.parseDouble(tmp[0]) / Double.parseDouble(tmp[1]);
         }
         log.info("Maximum error rate: %s", max_error_rate);
      }

      Project sequenceProject = null, targetProject = null;
      final File sequenceProjectFile = new File(args[0]);
      final File targetProjectFile = new File(args[1]);
      final File[] adapterFiles = new File[args.length - 2];
      for (int i = 0; i < adapterFiles.length; i++) {
         adapterFiles[i] = new File(args[i + 2]);
      }
      try {
         sequenceProject = Project.createFromFile(sequenceProjectFile);
         targetProject = Project.createFlatCopy(sequenceProjectFile, targetProjectFile);
      } catch (IOException e) {
         log.error("rmadapt: cannot read project files; %s", e);
         return 1;
      }

      TicToc subTimer = new TicToc();
      log.info("rmadapt: start adapter translation.");
      // translate adapters
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
      log.info("rmadapt: done; time for adapter translation was %.1f secs.", subTimer.tocs());

      Sequences sequences = sequenceProject.readSequences();
      ArrayList<String> descriptions = sequences.getDescriptions();
      Sequences adapters = targetProject.readSequences("adapters");
      int middle = 0; // number of times an adapter is in the middle of an read
      Vector<HashMap<Integer, Integer>> lengths_front = new Vector<HashMap<Integer, Integer>>(adapters.getNumberSequences());
      Vector<HashMap<Integer, Integer>> lengths_back  = new Vector<HashMap<Integer, Integer>>(adapters.getNumberSequences());
      for(int i = 0; i < adapters.getNumberSequences(); i++) {
         lengths_front.add(new HashMap<Integer, Integer>());
         lengths_back.add(new HashMap<Integer, Integer>());
      }
      assert lengths_front.size() == adapters.getNumberSequences(): lengths_front.size();
      assert lengths_back.size()  == adapters.getNumberSequences(): lengths_back.size();

      SequenceWriter sequenceWriter = null;
      long lastbyte = 0;
      try {
         sequenceWriter = new SequenceWriter(targetProject);
      } catch (IOException ex) {
         log.error("rmadapt: could not create output files for cutted sequences; %s", ex);
         return 1;
      }

      subTimer.tic();
      log.info("rmadapt: start to cut the reads.");
      for (int i = 0; i < sequences.getNumberSequences(); i++) {
         byte[] sequence = sequences.getSequence(i);
         byte[] qualityValues = null;
         try {
            qualityValues = sequences.getQualityValuesForSequence(i);
         } catch (IOException e) {
            // do nothing
         }

         if (colorspace && qualityValues == null) {
            sequence = Arrays.copyOfRange(sequence, 2, sequence.length);
         }

         for (int k = 0; k < times; k++) { // maybe try several times to remove an adapter

            Aligner.SemiglobalAlignmentResult bestResult = new Aligner.SemiglobalAlignmentResult(
                  null, null, 0, Integer.MIN_VALUE, 0);
            assert bestResult.getLength() - bestResult.getErrors() == Integer.MIN_VALUE;
            
            // Build alignment for each adapter
            int bestAdapter = -1;
            for (int j = 0; j < adapters.getNumberSequences(); j++) {
               Aligner.SemiglobalAlignmentResult result = Aligner.semiglobalAlign(
                     adapters.getSequence(j), sequence);
               
               if (result.getLength() - result.getErrors() > bestResult.getLength()
                     - bestResult.getErrors()) {
                  bestResult = result;
                  bestAdapter = j;
               }
            }
            assert bestAdapter >= 0;
            
            if (bestResult.getLength() > 0
                  && (double) bestResult.getErrors() / bestResult.getLength() <= max_error_rate) {
               
               assert bestResult.getLength() - bestResult.getErrors() > 0;
               
               if (min_print_align_length >= 0 && bestResult.getLength() >= min_print_align_length) {
                  log.info("read no %d (%s):", i + 1, descriptions.get(i));
                  log.info(bestResult.toString());
                  log.info("");
                  log.info("");
               }
               
               final byte[] adapterAlignment = bestResult.getSequence1();
               if (adapterAlignment[0] != Aligner.GAP
                     && adapterAlignment[adapterAlignment.length - 1] != Aligner.GAP) {
                  // The adapter or parts of it covers the entire read
                  log.info("read %s is covered entirely by the adapter %s:",
                        descriptions.get(i).split(" ")[0], bestAdapter);
                  log.info(bestResult.toString());
                  log.info("");
                  log.info("");
                  // TODO what now?
               } else if (adapterAlignment[0] == Aligner.GAP) {
                  // The adapter is at the end of the read
                  if (adapterAlignment[adapterAlignment.length - 1] == Aligner.GAP) {
                     // The adapter is in the middle of the read
                     if (min_print_align_length >= 0) {
                        log.info("adapter %s is in the middle of read %s:", bestAdapter,
                              descriptions.get(i).split(" ")[0]);
                        log.info(bestResult.toString());
                        log.info("");
                        log.info("");
                     }
                     middle++;
                     // TODO what now?
                  }

                  // TODO maybe this should only be done if the adapter is not in the middle of the
                  // read (else to the upper if).
                  if (colorspace) {
                     sequence = Arrays.copyOfRange(sequence, 0, bestResult.getBegin() - 1);
                     if (qualityValues != null) {
                        qualityValues = Arrays.copyOfRange(qualityValues, 0,
                              bestResult.getBegin() - 1);
                        assert qualityValues.length == sequence.length;
                     }
                  } else {
                     sequence = Arrays.copyOfRange(sequence, 0, bestResult.getBegin());
                  }
                  
                  // Statistics
                  if (lengths_back.get(bestAdapter).containsKey(bestResult.getLength())) {
                     final int oldCounter = lengths_back.get(bestAdapter).get(
                           bestResult.getLength());
                     lengths_back.get(bestAdapter).put(bestResult.getLength(), oldCounter + 1);
                  } else {
                     lengths_back.get(bestAdapter).put(bestResult.getLength(), 1);
                  }
               } else if (adapterAlignment[adapterAlignment.length - 1] == Aligner.GAP) {
                  // The adapter is in the beginning of the read
                  sequence = Arrays.copyOfRange(sequence, bestResult.getLength(), sequence.length);
                  if (qualityValues != null) {
                     qualityValues = Arrays.copyOfRange(qualityValues, bestResult.getLength(),
                           qualityValues.length);
                     assert qualityValues.length == sequence.length;
                  }
                  
                  // Statistics
                  if (lengths_front.get(bestAdapter).containsKey(bestResult.getLength())) {
                     final int oldCounter = lengths_front.get(bestAdapter).get(
                           bestResult.getLength());
                     lengths_front.get(bestAdapter).put(bestResult.getLength(), oldCounter + 1);
                  } else {
                     lengths_front.get(bestAdapter).put(bestResult.getLength(), 1);
                  }
               } else {
                  // This should not happen!
               }
            } else {
               break; // If no gratifying adapter was found, there is no need to try it again.
            }

         }

         // Add a separator at the end of the sequence
         final byte separator = (byte) (targetProject.getIntProperty("Separator"));
         sequence = Arrays.copyOf(sequence, sequence.length + 1);
         sequence[sequence.length - 1] = separator;
         if (qualityValues != null) {
            qualityValues = Arrays.copyOf(qualityValues, qualityValues.length + 1);
            qualityValues[sequence.length - 1] = Byte.MIN_VALUE;
         }

         // Write cuted sequence to target project
         try {
            lastbyte = sequenceWriter.writeBuffer(ByteBuffer.wrap(sequence));
            if (qualityValues != null) {
               sequenceWriter.addQualityValues(ByteBuffer.wrap(qualityValues));
            }

            // Sequence contains as last element a separator. So its length in proper meaning is
            // sequence.length-1.
            final String newDescription = descriptions.get(i).replaceAll("length=[0-9]*",
                  "length=" + (sequence.length - 1));
            sequenceWriter.addInfo(newDescription, sequence.length - 1, (int) (lastbyte - 1));
            
         } catch (IOException ex) {
            log.error("rmadapt: could not write cutted sequences; %s", ex);
            return 1;
         }

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
      
      // print statistics
      log.info("# There are %7d sequences in this data set.", sequences.getNumberSequences());
      log.info("");
      log.info("middle: %s", middle);
      for (int j = 0; j < adapters.getNumberSequences(); j++) {
         final byte[] adapter = adapters.getSequence(j);
         StringBuilder adapterString = new StringBuilder(adapter.length);
         for (byte b : adapter) {
            adapterString.append(b);
         }
         log.info("# Statistics for adapter %s (%s, length %d)", j, adapterString,
               adapter.length);
         log.info("#");
         log.info("# adapter found in the beginning of the sequence");
         log.info("# length\tnumber");
         int total = 0;
         for (Integer key : lengths_front.get(j).keySet()) {
            final Integer value = lengths_front.get(j).get(key);
            log.info("%d\t%d", key, value);
            total += value;
         }
         log.info("total: %s", total);
         log.info("");
         log.info("");
         log.info("# adapter found at the end of the sequence");
         log.info("# length\tnumber");
         total = 0;
         for (Integer key : lengths_back.get(j).keySet()) {
            final Integer value = lengths_back.get(j).get(key);
            log.info("%d\t%d", key, value);
            total += value;
         }
         log.info("total: %s", total);
         log.info("");
         log.info("");
         
         log.info("rmadapt: done; time to cut the reads was %.1f secs.", subTimer.tocs());
         log.info("rmadapt: done; total time was %.1f secs.", totalTimer.tocs());
      }
      

      return 0;
   }
}

