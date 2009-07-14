package verjinxer.sequenceanalysis.alignment;

public abstract class BeginLocations {
   void initMatrix(IAligner.Entry[][] table) {
      for (int row = 0; row < table.length; ++row) {
         for (int column = 0; column < table[row].length; ++column) {
            table[row][column] = new IAligner.Entry();
         }
      }
   }
}
