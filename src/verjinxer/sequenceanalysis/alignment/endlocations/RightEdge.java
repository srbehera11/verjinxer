package verjinxer.sequenceanalysis.alignment.endlocations;

import verjinxer.sequenceanalysis.alignment.Aligner.Entry;
import verjinxer.sequenceanalysis.alignment.Aligner.MatrixPosition;

/**
 * This class is used to build an alignment where the alignment may end at the right edge of the
 * alignment table. If two or more alignments end at the right edge with the same score the alignment
 * that ends nearest to the bottom is preferred.
 * 
 * @see Aligner
 * @author Markus Kemmerling
 */
public class RightEdge extends EndLocations {

   /*
    * The Alignment ends at the right edge. There, the bottommost entry is taken, cause of symmetry
    * to BottomEdge.getEndPosition(Entry[][]).
    */
   @Override
   public MatrixPosition getEndPosition(Entry[][] table) {

      final int n = table[0].length - 1;

      Entry bestEntry = table[0][n];
      int bestRow = 0;

      for (int row = 1; row < table.length; ++row) { // start by 1 cause bestEntry is already set to
                                                     // table[0][n]
         if (table[row][n].score >= bestEntry.score) { // must be >= cause better to start near
                                                       // corner
            bestRow = row;
            bestEntry = table[row][n];
         }
      }

      return new MatrixPosition(bestRow, n);
   }

}
