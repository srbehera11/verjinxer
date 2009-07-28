package verjinxer.sequenceanalysis.alignment;

/**
 * 
 * @author Markus Kemmerling
 * 
 */
public class AlignerFactory {

   /**
    * 
    * @return
    */
   public static SemiglobalAligner createSemiglobalAligner() {
      SemiglobalAligner a = new SemiglobalAligner();
      a.setBeginLocations(new TopAndLeftEdges());
      a.setEndLocations(new BottomAndRightEdges());
      a.setScores(new Scores(-1, -1, 1, -1));
      return a;
   }

   /**
    * 
    * @return
    */
   public static SemiglobalAligner createForwardAligner() {
      SemiglobalAligner a = new SemiglobalAligner();
      a.setBeginLocations(new TopLeftCorner());
      a.setEndLocations(new BottomEdge());
      a.setScores(new Scores(-1, -1, 0, -1));
      return a;
   }

   /**
    * 
    * @return
    */
   public static SemiglobalAligner createGlobalAligner() {
      SemiglobalAligner a = new SemiglobalAligner();
      a.setBeginLocations(new TopLeftCorner());
      a.setEndLocations(new BottomRightCorner());
      // a.setScores(new Scores(?, ?, ?, ?)); TODO
      return a;
   }

}
