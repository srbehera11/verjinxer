package verjinxer.sequenceanalysis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import verjinxer.util.ArrayFile;
import verjinxer.util.FileTypes;
import verjinxer.util.ProjectInfo;

/**
 * @author Markus Kemmerling
 */
public class Sequences {

   protected final String seqFilename;
   protected final String sspFilename;
   protected final String descFilename;
   protected final String qualityFilename;

   private byte[] sequence = null;
   private long[] separatorPositions = null;
   private ArrayList<String> descriptions = null;
   private byte[] qualityValues = null;

   public Sequences(final ProjectInfo project) throws IOException {
      seqFilename = project.makeFileName(FileTypes.SEQ);
      sspFilename = project.makeFileName(FileTypes.SSP);
      descFilename = project.makeFileName(FileTypes.DESC);
      qualityFilename = project.makeFileName(FileTypes.QUALITIY);
      load();
   }
   
   /**
    * @return The filename of the Sequence.
    */
   public String getSequenceFilename() {
      return seqFilename;
   }

   /**
    * @return The filename of the separator positions
    */
   public String getSequencesSeparatorPositionsFilename() {
      return sspFilename;
   }

   /**
    * @return The filename of the descriptions
    */
   public String getDescriptionFilename() {
      return descFilename;
   }

   /**
    * @return The filename of the Qualityfile
    */
   public String getQualityFilename() {
      return qualityFilename;
   }

   /**
    * Reads the .seq and .ssp files into memory. sequence and ssp are null until this method is
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
    * Reads the .desc file into memory. description is null until this method is invoked.
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

   /**
    * @return Accumulated length of all sequences (length of .seq file).
    */
   public long length() {
      return sequence.length;
   }

   /**
    * @return The underlying array for the concatenated sequences.
    */
   public byte[] array() {
      return sequence;
   }

   // /**
   // * Rewinds this sequence. The position is set to zero.
   // */
   // public void rewind() {
   // //TODO
   // }

   /**
    * @return Number of concatenated sequences.
    */
   public int getNumberSequences() {
      return separatorPositions.length;
   }

   // /**
   // * @return Array containing the length of each sequence.
   // */
   // public long[] getLengths() {
   // //TODO creat infos from separatorPositions
   // }

   /**
    * Returns array of quality values (concatenated, with separators); i.e. for each k,
    * getQualityValues()[k] contains the quality of the character arrar()[k].
    * 
    * @return Returns null of qualities are not available.
    */
   public byte[] getQualityValues() throws IOException {
      if (qualityValues == null) {
         qualityValues = new ArrayFile().setFilename(qualityFilename).readArray(qualityValues);
      }
      return qualityValues;
   }

   /**
    * @return The positions of the separators between the sequences.
    */
   public long[] getSeparatorPositions() {
      return separatorPositions;
   }

   /**
    * @return The descriptions of all sequences.
    */
   public ArrayList<String> getDescriptions() {
      if (descriptions == null) {
         loadDescription();
      }
      return descriptions;
   }

}
