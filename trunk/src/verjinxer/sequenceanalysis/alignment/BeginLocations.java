package verjinxer.sequenceanalysis.alignment;

import verjinxer.sequenceanalysis.alignment.IAligner.Direction;

public abstract class BeginLocations {
   
   /**
    * Initiates the given alignment table. Especially the left and top edge.
    * 
    * @param table
    */
   void initMatrix(IAligner.Entry[][] table, Scores scores) {

      // init left edge
      for (int row = 0; row < table.length; ++row) {
         table[row][0] = new IAligner.Entry();
         table[row][0].backtrack = Direction.UP;
      }

      // init top edge
      for (int column = 0; column < table[0].length; ++column) {
         table[0][column] = new IAligner.Entry();
         table[0][column].backtrack = Direction.LEFT;
      }

      // init inner matrix
      for (int row = 1; row < table.length; ++row) {
         for (int column = 1; column < table[row].length; ++column) {
            table[row][column] = new IAligner.Entry();
         }
      }
   }
   
   /**
    * @param row
    * @param column
    * @return Whether the position is a valid starting position
    */
   public abstract boolean isValid(int row, int column);
}
