package verjinxer.sequenceanalysis.alignment;

import verjinxer.sequenceanalysis.alignment.beginlocations.TopAndLeftEdges;
import verjinxer.sequenceanalysis.alignment.beginlocations.TopLeftCorner;
import verjinxer.sequenceanalysis.alignment.endlocations.BottomAndRightEdges;
import verjinxer.sequenceanalysis.alignment.endlocations.BottomEdge;
import verjinxer.sequenceanalysis.alignment.endlocations.BottomRightCorner;

/**
 * Factory class for creating special aligner conveniently.
 * 
 * @author Markus Kemmerling
 */
public class AlignerFactory {

   /**
    * Creates an end-gap free aligner. Also called free-shift aligner or semiglobal aligner.
    */
   public static Aligner createSemiglobalAligner() {
      Aligner a = new Aligner();
      a.setBeginLocations(new TopAndLeftEdges());
      a.setEndLocations(new BottomAndRightEdges());
      a.setScores(new Scores(-1, -1, 1, -1));
      return a;
   }

   /**
    * Creates a forward aligner.
    */
   public static Aligner createForwardAligner() {
      Aligner a = new Aligner();
      a.setBeginLocations(new TopLeftCorner());
      a.setEndLocations(new BottomEdge());
      a.setScores(new Scores(-1, -1, 0, -1));
      return a;
   }

   /**
    * Creates a global aligner.
    */
   public static Aligner createGlobalAligner() {
      Aligner a = new Aligner();
      a.setBeginLocations(new TopLeftCorner());
      a.setEndLocations(new BottomRightCorner());
      // a.setScores(new Scores(?, ?, ?, ?)); TODO
      return a;
   }

}
