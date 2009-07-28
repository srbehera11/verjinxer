package verjinxer.sequenceanalysis.alignment;

/**
 * @author Markus Kemmerling
 */
public class Scores {
   
   private final int insertion;
   private final int deletion;
   private final int match;
   private final int mismatch;
   
   public Scores() {
      this.insertion = -1;
      this.deletion = -1;
      this.match = 0;
      this.mismatch = -1;
   }
   
   public Scores(int insertion, int deletion, int match, int mismatch) {
      this.insertion = insertion;
      this.deletion = deletion;
      this.match = match;
      this.mismatch = mismatch;
   }

   public int getInsertionScore() {
      return insertion;
   }

   public int getDeletionScore() {
      return deletion;
   }

   public int getMatchScore() {
      return match;
   }

   public int getMismatchScore() {
      return mismatch;
   }
   
   
}
