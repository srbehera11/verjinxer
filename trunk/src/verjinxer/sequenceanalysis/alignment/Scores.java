package verjinxer.sequenceanalysis.alignment;

/**
 * Scores used in an alignment table by the aligner. Distinguishing between scores for insertion,
 * deletion, match and mismatch.
 * 
 * @see Aligner
 * @author Markus Kemmerling
 */
public class Scores {
   
   private final int insertion;
   private final int deletion;
   private final int match;
   private final int mismatch;
   
   /**
    * Initializes the score with default values:<br>
    * insertion = -1<br>
    * deletion = -1<br>
    * match = 0<br>
    * mismatch = -1<br>
    */
   public Scores() {
      this.insertion = -1;
      this.deletion = -1;
      this.match = 0;
      this.mismatch = -1;
   }
   
   /**
    * Initializes the score with the given values.
    * 
    * @param insertion
    * @param deletion
    * @param match
    * @param mismatch
    */
   public Scores(int insertion, int deletion, int match, int mismatch) {
      this.insertion = insertion;
      this.deletion = deletion;
      this.match = match;
      this.mismatch = mismatch;
   }

   /**
    * @return The score for an insertion.
    */
   public int getInsertionScore() {
      return insertion;
   }

   /**
    * @return The score for a deletion.
    */
   public int getDeletionScore() {
      return deletion;
   }

   /**
    * @return The score for a match.
    */
   public int getMatchScore() {
      return match;
   }

   /**
    * @return The score for a mismatch.
    */
   public int getMismatchScore() {
      return mismatch;
   }
   
   
}
