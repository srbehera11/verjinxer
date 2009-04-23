package verjinxer.sequenceanalysis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import verjinxer.util.ArrayFile;
import verjinxer.util.HugeByteArray;

/**
 * 
 * @author Markus Kemmerling
 *
 */
public class SequenceReader extends Sequence{
   
   private HugeByteArray sequence;
   private long[] ssp;
   private String[] description;
   
   SequenceReader(final String projectname, Mode mode) throws IOException{
      super(projectname, mode);
      load();
   }
   
   /**
    * Read the .seq and .ssp files into memory.
    * sequence and ssp are null until this method is invoked.
    */
   private void load() throws IOException{
      // Code only copied from Globals.slurpByteArray(String file)
      //TODO with runs, seqFile is f.e. chr22.runseq.seq, what is wrong
      try {
         sequence = HugeByteArray.fromFile(seqFile);
         ssp      = new ArrayFile().setFilename(sspFile).readArray(ssp);
         assert sequence != null : String.format("No sequence for %s", seqFile);
         assert ssp      != null : String.format("No ssp for %s", sspFile);
      } catch (IOException ex) {
         ex.printStackTrace();
         System.exit(1);
      }
   }
   
   /**
    * reads the .desc file into memory.
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
   
   @Override
   public long length() {
      return sequence.length;
   }

   @Override
   public HugeByteArray array() {
      return sequence;
   }
}
