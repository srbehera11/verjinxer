package verjinxer;

import com.spinn3r.log5j.Logger;

import verjinxer.sequenceanalysis.QGramIndex;

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
	private int numberPositionsCompressionPossible = 0; //counts, where compression method can be used
	private int numberPositionsCompressionImpossible = 0; //counts, where compression method can't be used
	

	public QGramIndexCompressionAnalyzer(QGramIndex qgramindex) {
		super();
		this.qgramindex = qgramindex;
	}

	public void analyze() {
		int positions[] = new int[qgramindex.getMaximumBucketSize()];
		int stepSize;
		for(int qcode = 0; qcode < qgramindex.getNumberOfBuckets(); qcode++){
			qgramindex.getQGramPositions(qcode, positions);
			
			if (qgramindex.getBucketSize(qcode) > 0) { //if bucket is empty, nothing to do
            // first position is ever stored as int:
            usedBytesNormal += 4;
            usedBytesCompressed += 4;
            assert usedBytesNormal > 0;
            assert usedBytesCompressed > 0;

            for (int i = 1; i < qgramindex.getBucketSize(qcode); ++i) {
               stepSize = positions[i] - positions[i - 1];
               assert stepSize > 0;
               if(stepSize < minStepSize) minStepSize = stepSize;
               if(stepSize > maxStepSize) maxStepSize = stepSize;
               
               stepSizeSum += stepSize;
               numberSteps++;
               assert stepSizeSum > 0;
               assert numberSteps > 0;
               
               //if possible, encode the step with a short in compressed storage, otherwise with a integer
               if (stepSize > Short.MAX_VALUE){ //Short.MAX_VALUE = 2^15-1, maybe use 2^16 [unsigned short]
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
	
	public void printStatistic(){
      double averageStepSize = (double)stepSizeSum / numberSteps;
      
      log.info("Average step size: %s", averageStepSize);
      log.info("Average needed bits: %s", Math.ceil(log2(averageStepSize))+1 );
      
      log.info("Minimum step size: %s", minStepSize);
      log.info("Minimum needed bits: %s", Math.ceil(log2(minStepSize))+1);
      
      log.info("Maximium step size: %s", maxStepSize);
      log.info("Maximium needed bits: %s", Math.ceil(log2(maxStepSize))+1);
      
      if(usedBytesNormal < 1024)
         log.info("Used bytes for nomale storage: %s", usedBytesNormal);
      else if(usedBytesNormal < 1024*1024l)
         log.info("Used KB for nomale storage: %s", (double)usedBytesNormal/1024);
      else if(usedBytesNormal < 1024*1024*1024l)
         log.info("Used MB for nomale storage: %s", (double)usedBytesNormal/1024/1024);
      else if(usedBytesNormal < 1024*1024*1024*1024l)
         log.info("Used GB for nomale storage: %s", (double)usedBytesNormal/1024/1024/1024);
      
      if(usedBytesCompressed < 1024)
         log.info("Used bytes for compressed storage: %s", usedBytesCompressed);
      else if(usedBytesCompressed < 1024*1024l)
         log.info("Used KB for compressed storage: %s", (double)usedBytesCompressed/1024);
      else if(usedBytesCompressed < 1024*1024*1024l)
         log.info("Used MB for compressed storage: %s", (double)usedBytesCompressed/1024/1024);
      else if(usedBytesCompressed < 1024*1024*1024*1024l)
         log.info("Used GB for compressed storage: %s", (double)usedBytesCompressed/1024/1024/1024);
      
      log.info("Memory saving with compressed storage in Percent: %s", (double)(usedBytesNormal-usedBytesCompressed)/usedBytesNormal );
      
      log.info("At %s positions is compression possible.", numberPositionsCompressionPossible);
      log.info("At %s positions is compression impossible.", numberPositionsCompressionImpossible);
	}
	
	private double log2(double value){
	   return Math.log10(value)/Math.log10(2);
	}
}
