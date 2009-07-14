package verjinxer.sequenceanalysis.alignment;

import verjinxer.sequenceanalysis.alignment.IAligner.Entry;

public class TopAndLeftEdges extends BeginLocations {

   @Override
   public void initMatrix(Entry[][] table) {
      super.initMatrix(table);
      
      for (int row = 0; row < table.length; ++row) {
         table[row][0].score = 0;
         //table[row][0].backtrack is not set cause it is never read
      }
      for (int column = 0; column < table[0].length; ++column) {
         table[0][column].score = 0;
         //table[0][column].backtrack is not set cause it is never read
      }

   }

}
