package verjinxer;

import verjinxer.sequenceanalysis.QGramIndex;

/**
 * @author Markus Kemmerling
 */
public class QGramIndexCompressionAnalyzer {
	
	private QGramIndex qgramindex;
	private int minStepSize;
	private int maxStepSize;
	

	public QGramIndexCompressionAnalyzer(QGramIndex qgramindex) {
		super();
		this.qgramindex = qgramindex;
	}

	public void analyze() {
		int positions[] = new int[qgramindex.getMaximumBucketSize()];
		for(int qcode = 0; qcode < qgramindex.getNumberOfBuckets(); qcode++){
			qgramindex.getQGramPositions(qcode, positions);
			
			//TODO analyze
			System.out.printf("%d positions for q-code %d:%n", qgramindex.getBucketSize(qcode), qcode);
			for (int i = 0; i < qgramindex.getBucketSize(qcode); ++i) {
				System.out.format("%d ", positions[i]);
			}
		}
	}
}
