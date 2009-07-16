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
      return a;
   }
   
}
