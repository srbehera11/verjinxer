package verjinxer.sequenceanalysis.alignment;

public abstract class EndLocations {
   
   abstract IAligner.MatrixPosition getEndPosition(IAligner.Entry[][] table);
}
