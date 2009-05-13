package verjinxer.sequenceanalysis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import verjinxer.util.ArrayFile;
import verjinxer.util.ProjectInfo;

public class SequenceReader extends Sequences {
//TODO merge with Sequences
   private byte[] sequence = null;
   private long[] separatorPositions = null;
   private ArrayList<String> descriptions = null;
   private byte[] qualityValues = null;

   @Deprecated
   SequenceReader(final String projectname, final Mode mode) throws IOException {
      super(projectname, mode);
      load();
   }
   
   SequenceReader(final ProjectInfo project, final Mode mode) throws IOException {
      super(project, mode);
      load();
   }

   /**
    * Read the .seq and .ssp files into memory. sequence and ssp are null until this method is
    * invoked.
    */
   private void load() throws IOException {
      // Code only copied from Globals.slurpByteArray(String file)
      // TODO with runs, seqFile is f.e. chr22.runseq.seq, what is wrong
      try {
         // TODO: uuuooh. think about static methods!
         sequence = new ArrayFile().setFilename(seqFilename).readArray(sequence);
         separatorPositions = new ArrayFile().setFilename(sspFilename).readArray(separatorPositions);
         assert sequence != null : String.format("No sequence for %s", seqFilename);
         assert separatorPositions != null : String.format("No ssp for %s", sspFilename);
      } catch (IOException ex) {
         ex.printStackTrace();
         System.exit(1);
      }
   }

   /**
    * reads the .desc file into memory. description is null until this method is invoked.
    */
   private void loadDescription() {
      descriptions = new ArrayList<String>();
      try {
         BufferedReader in = new BufferedReader(new FileReader(descFilename));
         String line;
         while ((line = in.readLine()) != null) {
            descriptions.add(line);
         }
      } catch (IOException ex) {
         ex.printStackTrace();
         System.exit(1);
      }
      assert descriptions.size() > 0 : String.format("No description for %s", descFilename);
   }

   @Override
   public long length() {
      return sequence.length;
   }

   @Override
   public byte[] array() {
      return sequence;
   }

   @Override
   public byte[] getQualityValues() throws IOException {
      if (qualityValues == null) {
         qualityValues = new ArrayFile().setFilename(qualityFilename).readArray(qualityValues);
      }
      return qualityValues;
   }
   
   @Override
   public long[] getSeparatorPositions() {
      return separatorPositions;
   }

   @Override
   public ArrayList<String> getDescriptions() {
      if (descriptions == null) {
         loadDescription();
      }
      return descriptions;
   }

}
