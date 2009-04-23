package verjinxer.sequenceanalysis;

import verjinxer.FileNameExtensions;

/**
 * Datastructure for a sequence
 * 
 * @author Markus Kemmerling
 */
public abstract class Sequence {
   
   public enum Mode { READ, WRITE }
   private final Mode mode;
   protected final String seqFile;
   protected final String sspFile;
   protected final String descFile;

   /**
    * @param projectname 
    */
   public Sequence(final String projectname, Mode mode) {
      seqFile = projectname + FileNameExtensions.seq;
      sspFile = projectname + FileNameExtensions.ssp;
      descFile = projectname + FileNameExtensions.desc;
      this.mode = mode;
   }
   
   public static Sequence openSequence(final String projectname, Mode mode){
      if(mode == Mode.READ){
         return new SequenceReader(projectname, mode);
      } else {
         return new SequenceWriter(projectname, mode);
      }
   }
   
   /**
    * Read the sequence and ssp from file.
    * sequence and ssp are null until this method is invoked.
    */
   public void load(){
      throw new RuntimeException(String.format("Operation not supported in %d mode", mode==Mode.READ?"READ":"WRITE"));
   }
   
   /**
    * 
    */
   public void store(){
      throw new RuntimeException(String.format("Operation not supported in %d mode", mode==Mode.READ?"READ":"WRITE"));
   }

   /**
    * @return Sequence length
    */
   public int length() {
      throw new RuntimeException(String.format("Operation not supported in %d mode", mode==Mode.READ?"READ":"WRITE"));
   }

   /**
    * @return The underlying array
    */
   public byte[] array() {
      throw new RuntimeException(String.format("Operation not supported in %d mode", mode==Mode.READ?"READ":"WRITE"));
   }

   /**
    * Rewinds this sequence. The position is set to zero.
    */
   public void rewind() {
      throw new RuntimeException(String.format("Operation not supported in %d mode", mode==Mode.READ?"READ":"WRITE"));
   }
   
   
   
   
}
