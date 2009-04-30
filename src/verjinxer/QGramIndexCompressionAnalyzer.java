package verjinxer;

import java.text.DecimalFormat;

import com.spinn3r.log5j.Logger;

import verjinxer.sequenceanalysis.QGramIndex;
import verjinxer.util.MathUtils;

/**
 * @author Markus Kemmerling
 */
public class QGramIndexCompressionAnalyzer {
   private static final Logger log = Globals.log;

   private QGramIndex qgramindex;
   private int minStepSize = Integer.MAX_VALUE;
   private int maxStepSize = 0;
   private long stepSizeSum = 0;
   private int numberSteps = 0;
   private long usedBytesNormal = 0;
   private long usedBytesCompressed = 0;
   private int numberPositionsCompressionPossible = 0; // counts, where compression method can be
                                                       // used
   private int numberPositionsCompressionImpossible = 0; // counts, where compression method can't
                                                         // be used
   private int sequenceLength = -1; // length of the sequence to that the index belongs
   private DecimalFormat doubleFormater = new DecimalFormat("###,###,###,###,##0.0");
   private DecimalFormat integerFormater = new DecimalFormat("###,###,###,###,##0");
   
   /**
    * @param qgramindex
    *           Index to be analyzed
    */
   public QGramIndexCompressionAnalyzer(QGramIndex qgramindex) {
      super();
      this.qgramindex = qgramindex;
   }

   /**
    * Analyzes the QGramIndex apropos the compression possibilities.
    */
   public void analyze() {
      int positions[] = new int[qgramindex.getMaximumBucketSize()];
      int stepSize;
      for (int qcode = 0; qcode < qgramindex.getNumberOfBuckets(); qcode++) {
         qgramindex.getQGramPositions(qcode, positions);

         if (qgramindex.getBucketSize(qcode) > 0) { // if bucket is empty, nothing to do
            // first position is ever stored as int:
            usedBytesNormal += 4;
            usedBytesCompressed += 4;
            assert usedBytesNormal > 0;
            assert usedBytesCompressed > 0;

            for (int i = 1; i < qgramindex.getBucketSize(qcode); ++i) {
               stepSize = positions[i] - positions[i - 1];
               assert stepSize > 0;
               if (stepSize < minStepSize)
                  minStepSize = stepSize;
               if (stepSize > maxStepSize)
                  maxStepSize = stepSize;

               stepSizeSum += stepSize;
               numberSteps++;
               assert stepSizeSum > 0;
               assert numberSteps > 0;

               // if possible, encode the step with a short in compressed storage, otherwise with a
               // integer
               if (stepSize > Short.MAX_VALUE) {
                  usedBytesCompressed += 4;
                  numberPositionsCompressionImpossible++;
               } else {
                  usedBytesCompressed += 2;
                  numberPositionsCompressionPossible++;
               }

               usedBytesNormal += 4;
               assert usedBytesNormal > 0;
               assert usedBytesCompressed > 0;
            }
         }
      }
   }

   /**
    * Prints informations about how good the index can be compressed.
    */
   private void printCompressionStatistic() {
      double averageStepSize = (double) stepSizeSum / numberSteps;

      log.info("Informations about the possible compression:");

      log.info("Average step size: %s", doubleFormater.format(averageStepSize));
      log.info("Average needed bits: %s", Math.ceil(MathUtils.log2(averageStepSize + 1)));

      log.info("Minimum step size: %s", integerFormater.format(minStepSize));
      log.info("Minimum needed bits: %s", Math.ceil(MathUtils.log2(minStepSize + 1)));

      log.info("Maximum step size: %s", integerFormater.format(maxStepSize));
      log.info("Maximum needed bits: %s", Math.ceil(MathUtils.log2(maxStepSize + 1)));

      if (usedBytesNormal < 1024)
         log.info("Used bytes for normal storage: %s", doubleFormater.format(usedBytesNormal));
      else if (usedBytesNormal < 1024 * 1024l)
         log.info("Used KB for normal storage: %s",
               doubleFormater.format((double) usedBytesNormal / 1024));
      else if (usedBytesNormal < 1024 * 1024 * 1024l)
         log.info("Used MB for normal storage: %s",
               doubleFormater.format((double) usedBytesNormal / 1024 / 1024));
      else if (usedBytesNormal < 1024 * 1024 * 1024 * 1024l)
         log.info("Used GB for normal storage: %s",
               doubleFormater.format((double) usedBytesNormal / 1024 / 1024 / 1024));

      if (usedBytesCompressed < 1024)
         log.info("Used bytes for compressed storage: %s",
               doubleFormater.format(usedBytesCompressed));
      else if (usedBytesCompressed < 1024 * 1024l)
         log.info("Used KB for compressed storage: %s",
               doubleFormater.format((double) usedBytesCompressed / 1024));
      else if (usedBytesCompressed < 1024 * 1024 * 1024l)
         log.info("Used MB for compressed storage: %s",
               doubleFormater.format((double) usedBytesCompressed / 1024 / 1024));
      else if (usedBytesCompressed < 1024 * 1024 * 1024 * 1024l)
         log.info("Used GB for compressed storage: %s",
               doubleFormater.format((double) usedBytesCompressed / 1024 / 1024 / 1024));

      log.info(
            "Memory saving with compressed storage in percent: %s",
            doubleFormater.format(((double) (usedBytesNormal - usedBytesCompressed) / usedBytesNormal) * 100));

      log.info(
            "Compression is possible at %s (%s percent) positions.",
            integerFormater.format(numberPositionsCompressionPossible),
            doubleFormater.format(((double) numberPositionsCompressionPossible / numberSteps) * 100));
      log.info(
            "Compression is impossible at %s (%s percent) positions.",
            integerFormater.format(numberPositionsCompressionImpossible),
            doubleFormater.format(((double) numberPositionsCompressionImpossible / numberSteps) * 100));
   }

   /**
    * Prints informations about the analyzed index.
    */
   private void printIndexInfo() {
      log.info("Infomations about the analyzed index:");
      log.info("q: %s", qgramindex.q);
      log.info("Stride: %s", qgramindex.getStride());
      log.info("Index contains %s positions.",
            integerFormater.format(qgramindex.getNumberOfPositions()));
      assert sequenceLength > 0;
      log.info(
            "Ratio of the total number of positions in the index to the length of the sequence: %s",
            doubleFormater.format((double) qgramindex.getNumberOfPositions() / sequenceLength)); // TODO
   }

   /**
    * Prints informations about the analyzed index and how good it can be compressed.
    */
   public void printStatistic() {
      log.info("The sequence, to that the analyzed index belongs, has a length of %s.",
            integerFormater.format(sequenceLength));
      printIndexInfo();
      printCompressionStatistic();
   }

   /**
    * @param sequenceLength
    *           The length of the sequence to that the analyzed index belongs
    */
   public void setSequenceLength(int sequenceLength) {
      this.sequenceLength = sequenceLength;
   }
}
