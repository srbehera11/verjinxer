package verjinxer.sequenceanalysis.alignment.endlocations;

import verjinxer.sequenceanalysis.alignment.Aligner.Entry;
import verjinxer.sequenceanalysis.alignment.Aligner.MatrixPosition;

/**
 * Subclasses of this class are used by the Aligner class to determine where within an alignment
 * table the alignment may end.
 * 
 * @see Aligner
 * @author Markus Kemmerling
 */
public abstract class EndLocations {
   
   /**
    * Searches in the given table the ending of the alignment. The table must be filled for that.
    * 
    * @param table
    *           The alignment table to initialize.
    * 
    * @return The position where the alignment ends.
    */
   public abstract MatrixPosition getEndPosition(Entry[][] table);
}
