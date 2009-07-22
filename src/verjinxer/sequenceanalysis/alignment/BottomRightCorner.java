package verjinxer.sequenceanalysis.alignment;

import verjinxer.sequenceanalysis.alignment.IAligner.Entry;

public class BottomRightCorner extends EndLocations {

   @Override
   IAligner.MatrixPosition getEndPosition(Entry[][] table) {

      final int m = table.length-1;
      final int n = table[0].length-1;
      
      return new IAligner.MatrixPosition(m, n);
   }

}
