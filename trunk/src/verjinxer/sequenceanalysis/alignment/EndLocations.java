package verjinxer.sequenceanalysis.alignment;

public abstract class EndLocations {
   
   abstract MatrixPosition getEndPosition(IAligner.Entry[][] table);

   public static class MatrixPosition {
      public final int row, column;

      public MatrixPosition(int row, int column) {
         this.row = row;
         this.column = column;
      }
   }
}
