package verjinxer.sequenceanalysis.alignment;

import verjinxer.sequenceanalysis.alignment.IAligner.Entry;

public class RightEdge extends EndLocations {

   /*
    * The Alignment ends at the right edge. There, the bottommost entry is taken, cause of symmetry
    * to BottomEdge.getEndPosition(Entry[][]).
    */
   @Override
   MatrixPosition getEndPosition(Entry[][] table) {

      final int n = table[0].length-1;
      
      IAligner.Entry bestEntry = table[1][n];
      int bestRow = 1;
   
      for (int row = 2; row < table.length ; ++row) { // start by 2 cause bestEntry is already set to table[1][n]
         if (table[row][n].score >= bestEntry.score) { // must be >= cause better to start near corner
            bestRow = row;
            bestEntry = table[row][n];
         }
      }
      
      return new MatrixPosition(bestRow, n);
   }

}
