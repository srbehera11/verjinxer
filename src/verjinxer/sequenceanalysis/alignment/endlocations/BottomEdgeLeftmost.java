package verjinxer.sequenceanalysis.alignment.endlocations;

import verjinxer.sequenceanalysis.alignment.Aligner.Entry;
import verjinxer.sequenceanalysis.alignment.Aligner.MatrixPosition;

public class BottomEdgeLeftmost extends EndLocations {

   /*
    * Searches the best leftmost entry at the bottom edge and not the rightmost as normal. This is
    * needed to replace the old align() method used in MapperByAlignment.doTheAlignment(int, byte[],
    * byte[], int, int, int, int, int, double).
    * 
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.alignment.EndLocations#getEndPosition(verjinxer.sequenceanalysis.alignment.IAligner.Entry[][])
    */
   @Override
   public MatrixPosition getEndPosition(Entry[][] table) {
      final int m = table.length - 1;

      Entry bestEntry = table[m][0];
      int bestColumn = 0;
      for (int column = 1; column < table[m].length; ++column) { // start by 0 cause bestEntry is
         // already set to table[m][0]
         if (table[m][column].score > bestEntry.score) { // must be > to get best leftmost entry
            bestColumn = column;
            bestEntry = table[m][column];
         }
      }

      return new MatrixPosition(m, bestColumn);
   }

}
