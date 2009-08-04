package verjinxer.sequenceanalysis.alignment.beginlocations;

import verjinxer.sequenceanalysis.alignment.Scores;
import verjinxer.sequenceanalysis.alignment.Aligner.Direction;
import verjinxer.sequenceanalysis.alignment.Aligner.Entry;

/**
 * Subclasses of this class are used by the Aligner class to determine where within an alignment
 * table the alignment may start.
 * 
 * @see Aligner
 * @author Markus Kemmerling
 */
public abstract class BeginLocations {
   
   /**
    * Initializes the given alignment table. Especially the left and top edge.
    * 
    * @param table
    *           The alignment table to initialize.
    */
   public void initMatrix(Entry[][] table, Scores scores) {

      // init left edge
      for (int row = 0; row < table.length; ++row) {
         table[row][0] = new Entry();
         table[row][0].backtrack = Direction.UP;
      }

      // init top edge
      for (int column = 0; column < table[0].length; ++column) {
         table[0][column] = new Entry();
         table[0][column].backtrack = Direction.LEFT;
      }

      // init inner matrix
      for (int row = 1; row < table.length; ++row) {
         for (int column = 1; column < table[row].length; ++column) {
            table[row][column] = new Entry();
         }
      }
   }
   
   /**
    * @param row
    * @param column
    * @return Whether the position is a valid starting position.
    */
   public abstract boolean isValid(int row, int column);
}
