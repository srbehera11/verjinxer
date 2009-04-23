package verjinxer.sequenceanalysis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import verjinxer.util.ArrayFile;

public class SequenceReader extends Sequence{
   
   private byte[] sequence;
   private long[] ssp;
   private String[] description;
   
   SequenceReader(final String projectname, Mode mode){
      super(projectname, mode);
      load();
   }
   
   /**
    * Read the sequence and ssp from file.
    * sequence and ssp are null until this method is invoked.
    */
   public void load(){
      // Code only copied from Globals.slurpByteArray(String file)
      //TODO with runs, seqFile is f.e. chr22.runseq.seq, what is wrong
      try {
         sequence = new ArrayFile().setFilename(seqFile).readArray(sequence);
         ssp      = new ArrayFile().setFilename(sspFile).readArray(ssp);
         assert sequence != null : String.format("No sequence for %s", seqFile);
         assert ssp      != null : String.format("No ssp for %s", sspFile);
      } catch (IOException ex) {
         ex.printStackTrace();
         System.exit(1);
      }
   }
   
   /**
    * reads the description from file.
    * description is null until this method is invoked.
    */
   private void loadDescription(){
      ArrayList<String> desc = new ArrayList<String>();
      try {
         BufferedReader in = new BufferedReader(new FileReader(descFile));
         String line;
         while( (line=in.readLine())!= null ){
            desc.add(line);
         }
      } catch (IOException ex) {
         ex.printStackTrace();
         System.exit(1);
      }
      description = desc.toArray(description);
      assert description.length > 0 : String.format("No description for %s", descFile);
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
}
