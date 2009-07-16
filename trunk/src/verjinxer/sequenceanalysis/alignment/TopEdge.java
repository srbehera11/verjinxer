package verjinxer.sequenceanalysis.alignment;

import verjinxer.sequenceanalysis.alignment.IAligner.Entry;

public class TopEdge extends BeginLocations {

   @Override
   public void initMatrix(Entry[][] table) {
      super.initMatrix(table);
      
      int score = 0;
      // init left edge
      for (int row = 0; row < table.length; ++row) {
         table[row][0].score = score--;
      }
      // init top edge
      for (int column = 0; column < table[0].length; ++column) {
         table[0][column].score = 0;
      }

   }
   
   public boolean isValid(int row, int column) {
      return row == 0; // complete top edge
   }

}
