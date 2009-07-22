package verjinxer.sequenceanalysis.alignment;

import verjinxer.sequenceanalysis.alignment.IAligner.Entry;

public class BottomEdge extends EndLocations {

   /*
    * The Alignment ends at the bottom edge. There, the rightmost entry is taken.
    * The base for the decision to take the rightmost entry is illustrated by the following example:
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
    */
   @Override
   MatrixPosition getEndPosition(Entry[][] table) {

      final int m = table.length-1;
      
      IAligner.Entry bestEntry = table[m][1];
      int bestColumn = 1;
      for (int column = 2; column < table[m].length; ++column) { // start by 2 cause bestEntry is
                                                             // already set to table[m][1]
         if (table[m][column].score >= bestEntry.score) { // must be >= cause better to start near corner
            bestColumn = column;
            bestEntry = table[m][column];
         }
      }
      
      return new MatrixPosition(m, bestColumn);
   }

}
