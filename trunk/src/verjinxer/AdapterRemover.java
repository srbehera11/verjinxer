package verjinxer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.spinn3r.log5j.Logger;

import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.sequenceanalysis.InvalidSymbolException;
import verjinxer.sequenceanalysis.SequenceWriter;
import verjinxer.sequenceanalysis.Sequences;
import verjinxer.sequenceanalysis.alignment.Aligner;
import verjinxer.sequenceanalysis.alignment.AlignmentResult;
import verjinxer.sequenceanalysis.alignment.Aligner.MatrixPosition;

/**
 * @author Markus Kemmerling
 */
public class AdapterRemover {
   
   private static final Logger log = Globals.getLogger();

   /** Whether to use color space mode. */
   private final boolean colorspace;
   
   /** How often it is tried to remove an adapter. */
   private final int times;
   
   /** Maximum error rate (errors divided by length of matching region). */
   private final double max_error_rate;
   
   /** How long an alignment must be to be printed. */
   private final int min_print_align_length;
   
   /** The aligner to use. */
   private final Aligner aligner;

   /**
    * 
    * @param colorspace
    *           Whether to use color space mode.
    * @param times
    *           How often it is tried to remove an adapter.
    * @param max_error_rate
    *           Maximum error rate (errors divided by length of matching region).
    * @param min_print_align_length
    *           How long an alignment must be to be printed.
    * @param aligner
    *           The aligner to use.
    */
   public AdapterRemover(boolean colorspace, int times, double max_error_rate,
         int min_print_align_length, Aligner aligner) {
      this.colorspace = colorspace;
      this.times = times;
      this.max_error_rate = max_error_rate;
      this.min_print_align_length = min_print_align_length;
      this.aligner = aligner;
   }

   /**
    * Tries to remove the given adapters from the sequences in the project and writes the resulting
    * sequences with the sequenceWriter.
    * 
    * @param sequenceProject
    *           The sequences to be cut.
    * @param adapters
    *           The adapters.
    * @param sequenceWriter
    *           Writer to store the resulting sequences.
    */
   public void cutAndWriteSequences(Project sequenceProject, Sequences adapters, SequenceWriter sequenceWriter) {
      // get sequences and quality values to cut
      Sequences sequences = sequenceProject.readSequences();
      Alphabet alphabet = sequenceProject.readAlphabet();
      ArrayList<String> descriptions = sequences.getDescriptions();
      byte[] sequence = sequences.array();
      byte[] qualityValues = null;
      try {
         qualityValues = sequences.getQualityValues();
      } catch (IOException e) {
         // do nothing
      }
      
      // Statistics
      int middle = 0; // number of times an adapter is in the middle of an read
      final long maxAlignment = Math.max(adapters.getMaximumLength(), // maximum length for an
                                                                      // alignment
            sequenceProject.getLongProperty("LongestSequence")) + 1;
      int[][] lengths_front = new int[adapters.getNumberSequences()][(int) maxAlignment];
      // lengths_front[a][l] = how often adapter a was at the begin of a read with length l
      int[][] lengths_back = new int[adapters.getNumberSequences()][(int) maxAlignment];
      // lengths_back[a][l] = how often adapter a was at the end of a read with length l
      int[] sequencesLengthAfterCutting = new int[sequenceProject.getIntProperty("LongestSequence") + 1];
      // sequencesLengthAfterCutting[l] = how many sequences with length l exist after cutting
      
      for (int i = 0; i < sequences.getNumberSequences(); i++) { //for each sequence
         final int[] sequenceBoundaries = sequences.getSequenceBoundaries(i);
         int beginSequence = sequenceBoundaries[0];
         int endSequence = sequenceBoundaries[1];

         if (colorspace && qualityValues == null) {
            beginSequence += 2;   // if qualityValues were given, this was already done while translation
         }

         for (int k = 0; k < times; k++) { // maybe try several times to remove an adapter
            
            // Build alignment for each adapter
            AlignmentResult bestResult = new AlignmentResult();
            int bestAdapter = findBestAlignment(bestResult, adapters, sequence, beginSequence,
                  endSequence, alphabet);
            assert bestResult != null;
            
            // if the best founded alignment is sufficient, than cut sequence
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
                  endSequence = beginSequence; // set read to length 0
                  break; // read can not be cut again
                  
               } else if (adapterAlignment[0] == Aligner.GAP) {
                  // The adapter is at the end of the read
                  if (adapterAlignment[adapterAlignment.length - 1] == Aligner.GAP) {
                     // The adapter is in the middle of the read
                     middle++;
                  }

                  if (colorspace) {
                     endSequence = beginSequence + bestResult.getBeginPosition().column - 1;
                  } else {
                     endSequence = beginSequence + bestResult.getBeginPosition().column;
                  }
                  
                  // Statistics
                  lengths_back[bestAdapter][bestResult.getLength()]++;

               } else if (adapterAlignment[adapterAlignment.length - 1] == Aligner.GAP) {
                  // The adapter is in the beginning of the read
                  beginSequence += bestResult.getLength();

                  // Statistics
                  lengths_front[bestAdapter][bestResult.getLength()]++;
                  
               } else {
                  // This should not happen!
                  assert false;
               }
            } else {
               break; // If no gratifying adapter was found, there is no need to try it again.
            }

         }
         
         // Statistics: how long are the sequences after cutting
         sequencesLengthAfterCutting[endSequence - beginSequence]++;

         // Add a separator at the end of the sequence
         final byte separator = (byte) (sequenceProject.getIntProperty("Separator"));
         if (endSequence < sequenceBoundaries[1]) {
            sequence[endSequence] = separator;
            if (qualityValues != null) {
               qualityValues[endSequence] = Byte.MIN_VALUE;
            }
         }
         assert endSequence < sequence.length: String.format("endSequence: %d%nsequence.length:%d", endSequence, sequence.length);
         assert sequence[endSequence] == separator;
         endSequence++;

         // Write cuted sequence to target project
         try {
            final long lastbyte = sequenceWriter.addSequence(ByteBuffer.wrap(sequence, beginSequence,
                  endSequence - beginSequence));
            if (qualityValues != null) {
               sequenceWriter.addQualityValues(ByteBuffer.wrap(qualityValues, beginSequence,
                     endSequence - beginSequence));
            }

            // Sequence contains as last element a separator. So its length in proper meaning is
            // sequence.length-1.
            final String newDescription = descriptions.get(i).replaceAll("length=[0-9]*",
                  "length=" + (endSequence-beginSequence - 1));
            sequenceWriter.addInfo(newDescription, endSequence-beginSequence - 1, (int) (lastbyte - 1));
            
         } catch (IOException ex) {
            log.error("rmadapt: could not write cutted sequences; %s", ex);
            //return 1; TODO
         }

      }
      try {
         // write 'end of line'
         sequenceWriter.appendCharacter(alphabet.codeEndOfLine());
      } catch (InvalidSymbolException ex) {
         log.error("translate: %s", ex);
      } catch (IOException ex) {
         log.error("translate: %s", ex);
      }
      
      // print statistics
      AdapterRemover.printStatistics(sequences.getNumberSequences(), sequencesLengthAfterCutting, middle, adapters, lengths_front, lengths_back);
   }

   /**
    * Prints the given statistical informations.
    * 
    * @param numberSequences
    *           How many sequences were cut.
    * @param sequencesLengthAfterCutting
    *           Length of the sequences after cutting.
    * @param middle
    *           Number of times an adapter was found in the middle of a sequence.
    * @param adapters
    *           The adapter that were removed.
    * @param lengths_front
    *           Length of the alignments when an adapter was found at the beginning of a sequence.
    * @param lengths_back
    *           Length of the alignments when an adapter was found at the end of a sequence.
    */
   private static void printStatistics(int numberSequences, int[] sequencesLengthAfterCutting,
         int middle, Sequences adapters, int[][] lengths_front, int[][] lengths_back) {
      log.info("# There are %7d sequences in this data set.", numberSequences);
      log.info("# length\tnumber");
      for (int length = 0; length < sequencesLengthAfterCutting.length; length++) {
         if (sequencesLengthAfterCutting[length] > 0) {
            log.info("%d\t%d", length, sequencesLengthAfterCutting[length]);
         }
      }
      log.info("");
      log.info("middle: %s", middle);
      final byte[] adaptersArray = adapters.array();
      for (int j = 0; j < adapters.getNumberSequences(); j++) {
         final int[] adapterBoundaries = adapters.getSequenceBoundaries(j);
         final int adapterLength = adapterBoundaries[1] - adapterBoundaries[0];
         StringBuilder adapterString = new StringBuilder(adapterLength);
         for (int i = adapterBoundaries[0]; i < adapterBoundaries[1]; i++) {
            adapterString.append(adaptersArray[i]);
         }
         log.info("# Statistics for adapter %s (%s, length %d)", j, adapterString, adapterLength);
         log.info("#");
         log.info("# adapter found in the beginning of the sequence");
         log.info("# length\tnumber");
         int total = 0;
         for (int length = 1; length < lengths_front[j].length; length++) {
            if (lengths_front[j][length] > 0) {
               log.info("%d\t%d", length, lengths_front[j][length]);
               total += lengths_front[j][length];
            }
         }
         log.info("total: %s", total);
         log.info("");
         log.info("");
         log.info("# adapter found at the end of the sequence");
         log.info("# length\tnumber");
         total = 0;
         for (int length = 1; length < lengths_back[j].length; length++) {
            if (lengths_back[j][length] > 0) {
               log.info("%d\t%d", length, lengths_back[j][length]);
               total += lengths_back[j][length];
            }
         }
         log.info("total: %s", total);
         log.info("");
         log.info("");
      }
   }

   /**
    * This method calculates for each adapter and the given part of the sequence the semiglobal
    * alignment. The relevant part of the sequence for that the alignment is build is specified with
    * beginSequence and endSequence. The longest alignment will be stored in bestResult and
    * additionally the index of the adapter with that the longest alignment was build will be
    * returned.
    * 
    * @param bestResult
    *           The longest alignment will be stored in it.
    * @param adapters
    *           The adapters the alignment is calculated with.
    * @param sequence
    *           A array with many sequences, where one of them (depending on beginSequence and
    *           endSequence) is used for calculating the alignment.
    * @param beginSequence
    *           The index where the relevant sequence starts in sequences.
    * @param endSequence
    *           The first index behind the relevant sequence in sequences.
    * @param alphabet
    *           The alphabet used for the sequence and the adapters.
    * @return The index of the adapter with that the longest alignment was build.
    */
   private int findBestAlignment(AlignmentResult bestResult,
         final Sequences adapters, final byte[] sequence, final int beginSequence,
         final int endSequence, Alphabet alphabet) {

      bestResult.setAllAttributes(null, null, new MatrixPosition(0, 0), new MatrixPosition(0, 0),
            Integer.MIN_VALUE, 0);
      assert bestResult.getLength() - bestResult.getErrors() == Integer.MIN_VALUE;
      int bestAdapter = -1;
      final byte[] adapterArrays = adapters.array();
      for (int j = 0; j < adapters.getNumberSequences(); j++) {
         final int[] boundaries = adapters.getSequenceBoundaries(j);
         AlignmentResult result = aligner.align(
               adapterArrays, boundaries[0], boundaries[1], sequence, beginSequence, endSequence,
               alphabet);

         if (result.getLength() - result.getErrors() > bestResult.getLength()
               - bestResult.getErrors()) {
            bestResult.setAllAttributes(result.getSequence1(), result.getSequence2(),
                  result.getBeginPosition(), result.getEndPosition(), result.getLength(),
                  result.getErrors());
            bestAdapter = j;
         }
      }
      assert bestAdapter >= 0;
      return bestAdapter;
   }

}
