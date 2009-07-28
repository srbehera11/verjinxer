package verjinxer.sequenceanalysis.alignment;

import verjinxer.sequenceanalysis.alignment.IAligner.Entry;

public class TopAndLeftEdges extends BeginLocations {

   @Override
   public void initMatrix(Entry[][] table, Scores scores) {
      super.initMatrix(table, scores);

      // init left edge
      for (int row = 0; row < table.length; ++row) {
         table[row][0].score = 0;
      }
      // init top edge
      for (int column = 0; column < table[0].length; ++column) {
         table[0][column].score = 0;
      }

   }

   public boolean isValid(int row, int column) {
      return column == 0 || row == 0; // complete left and top edges
   }
}
