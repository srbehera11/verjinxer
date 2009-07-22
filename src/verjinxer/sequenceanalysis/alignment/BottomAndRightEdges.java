package verjinxer.sequenceanalysis.alignment;

import verjinxer.sequenceanalysis.alignment.IAligner.Entry;

public class BottomAndRightEdges extends EndLocations {

   /*
    * The Alignment ends at the bottom or the right edge. There, the entry nearest to the corner is taken.
    * The base for the decision to take the rightmost entry if it ends at the bottom edge 
    * is illustrated by the following example:
    * 
    * query: Hallo
    * reference: Hella
    * 
    * By taken the rightmost entry at the bottom, we get this alignment that we intuitive expected:
    * Hallo
    * |x||x
    * Hella
    * 
    * If we would take the leftmost entry at the bottom edge, we would get that less intuitive alignment:
    * Hallo
    * |x||x
    * Hell-
    * 
    * If it ends et the right edge, the bottommost entry is taken cause of symmetry.
    */
   @Override
   IAligner.MatrixPosition getEndPosition(Entry[][] table) {
      
      final int m = table.length-1;
      final int n = table[0].length-1;
      
      IAligner.Entry bestEntry = table[m][1];
      int bestColumn = 1, bestRow = m;
      for (int column = 2; column < table[m].length; ++column) { // start by 2 cause bestEntry is
                                                             // already set to table[m][1]
         if (table[m][column].score >= bestEntry.score) { // must be >= cause better to start near corner
            bestColumn = column;
            bestEntry = table[m][column];
         }
      }
   
      for (int row = 1; row < table.length ; ++row) { // must be so, cause align.py was coded so and the test cases are from that code
         if (table[row][n].score >= bestEntry.score) { // must be >= cause better to start near corner
            bestRow = row;
            bestColumn = n;
            bestEntry = table[row][n];
         }
      }
      
      return new IAligner.MatrixPosition(bestRow, bestColumn);
   }

}
