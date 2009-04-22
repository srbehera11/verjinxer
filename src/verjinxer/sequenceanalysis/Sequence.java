package verjinxer.sequenceanalysis;

import java.io.IOException;
import verjinxer.util.ArrayFile;

/**
 * Datastructure for a sequence
 * 
 * @author Markus Kemmerling
 */
public class Sequence {

   private byte[] sequence;

   /**
    * Reads a sequence from the given seqfile
    * 
    * @param seqfile
    */
   public Sequence(String seqfile) {
      // Code only copied from Globals.slurpByteArray(String file)
      try {
         sequence = new ArrayFile().setFilename(seqfile).readArray(sequence);
         assert sequence != null : String.format("No sequence for %s", seqfile);
      } catch (IOException ex) {
         ex.printStackTrace();
         System.exit(1);
      }
   }

   /**
    * @return Sequence length
    */
   public int length() {
      return sequence.length;
   }

   /**
    * @return The underlying array
    */
   public byte[] array() {
      return sequence;
   }

   /**
    * Rewinds this sequence. The position is set to zero.
    */
   public void rewind() {
      // TODO Auto-generated method stub
   }
}
