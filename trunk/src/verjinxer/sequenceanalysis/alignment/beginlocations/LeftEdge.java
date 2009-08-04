package verjinxer.sequenceanalysis.alignment.beginlocations;

import verjinxer.sequenceanalysis.alignment.Scores;
import verjinxer.sequenceanalysis.alignment.Aligner.Entry;

public class LeftEdge extends BeginLocations {

   @Override
   public void initMatrix(Entry[][] table, Scores scores) {
      super.initMatrix(table, scores);

      int score = 0;
      // init left edge
      for (int row = 0; row < table.length; ++row) {
         table[row][0].score = 0;
      }
      // init top edge
      for (int column = 0; column < table[0].length; ++column) {
         table[0][column].score = score;
         score += scores.getInsertionScore();
      }
   }

   public boolean isValid(int row, int column) {
      return column == 0; // complete left edge
   }
}
