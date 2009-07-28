package verjinxer.sequenceanalysis.alignment;

/**
 * @author Markus Kemmerling
 */
public interface IAligner {

   public static class COSTS {
      public static int INDEL = 1;
      public static int MISMATCH = 1;
   }

   /** DP table entry */
   public static class Entry {
      public int score;
      public Direction backtrack;
   }

   public static class CostEntry {
      public int cost;
      public Direction backtrack;
   }

   /** direction constants for traceback table */
   public enum Direction {
      LEFT, UP, DIAG
   }

   public static class MatrixPosition {
      public final int row, column;
   
      public MatrixPosition(int row, int column) {
         this.row = row;
         this.column = column;
      }
   }
   
   public static final byte GAP = -1; // TODO

}
