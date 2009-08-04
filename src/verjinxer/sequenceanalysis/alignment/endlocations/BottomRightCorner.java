package verjinxer.sequenceanalysis.alignment.endlocations;

import verjinxer.sequenceanalysis.alignment.Aligner.Entry;
import verjinxer.sequenceanalysis.alignment.Aligner.MatrixPosition;

public class BottomRightCorner extends EndLocations {

   @Override
   public MatrixPosition getEndPosition(Entry[][] table) {

      final int m = table.length-1;
      final int n = table[0].length-1;
      
      return new MatrixPosition(m, n);
   }

}
