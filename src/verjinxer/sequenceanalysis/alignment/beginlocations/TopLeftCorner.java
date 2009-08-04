package verjinxer.sequenceanalysis.alignment.beginlocations;

import verjinxer.sequenceanalysis.alignment.Scores;
import verjinxer.sequenceanalysis.alignment.Aligner.Entry;

/**
 * This class is used to build an alignment where the alignment may start at the top left corner of the
 * alignment table.
 * 
 * @see Aligner
 * @author Markus Kemmerling
 */
public class TopLeftCorner extends BeginLocations {

   @Override
   public void initMatrix(Entry[][] table, Scores scores) {
      super.initMatrix(table, scores);
      
      int score = 0;   
      // init left edge
      for (int row = 0; row < table.length; ++row) {
         table[row][0].score = score;
         score += scores.getDeletionScore();
      }
      score = 0;
      // init top edge
      for (int column = 0; column < table[0].length; ++column) {
         table[0][column].score = score;
         score += scores.getInsertionScore();
      }
   }
   
   public boolean isValid(int row, int column) {
      return column == 0 && row == 0; // only top left corner
   }

}
