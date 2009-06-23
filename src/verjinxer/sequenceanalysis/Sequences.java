package verjinxer.sequenceanalysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import verjinxer.Project;
import verjinxer.util.ArrayFile;
import verjinxer.util.FileTypes;

/**
 * @author Markus Kemmerling
 */
public class Sequences {

   protected final File seqFile;
   protected final File sspFile;
   protected final File descFile;
   protected final File qualityFile;

   private byte[] sequence = null;
   private long[] separatorPositions = null;
   private ArrayList<String> descriptions = null;
   private byte[] qualityValues = null;

   public Sequences(final Project project) throws IOException {
      seqFile = project.makeFile(FileTypes.SEQ);
      sspFile = project.makeFile(FileTypes.SSP);
      descFile = project.makeFile(FileTypes.DESC);
      qualityFile = project.makeFile(FileTypes.QUALITIY);
      load();
   }
   
   /**
    * Creates a new sequences from files with the given name depending to the given project
    * 
    * @param project
    * @param name
    * @throws IOException
    */
   public Sequences(Project project, String name) throws IOException {
      seqFile = project.makeFile(FileTypes.SEQ, name);
      sspFile = project.makeFile(FileTypes.SSP, name);
      descFile = project.makeFile(FileTypes.DESC, name);
      qualityFile = project.makeFile(FileTypes.QUALITIY, name);
      load();
   }

   /**
    * @return The filename of the Sequence.
    */
   public File getSequenceFile() {
      return seqFile;
   }

   /**
    * @return The filename of the separator positions
    */
   public File getSequencesSeparatorPositionsFile() {
      return sspFile;
   }

   /**
    * @return The filename of the descriptions
    */
   public File getDescriptionFile() {
      return descFile;
   }

   /**
    * @return The filename of the Qualityfile
    */
   public File getQualityFile() {
      return qualityFile;
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
         sequence = new ArrayFile().setFile(seqFile).readArray(sequence);
         separatorPositions = new ArrayFile().setFile(sspFile).readArray(separatorPositions);
         assert sequence != null : String.format("No sequence for %s", seqFile);
         assert separatorPositions != null : String.format("No ssp for %s", sspFile);
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
         BufferedReader in = new BufferedReader(new FileReader(descFile));
         String line;
         while ((line = in.readLine()) != null) {
            descriptions.add(line);
         }
      } catch (IOException ex) {
         ex.printStackTrace();
         System.exit(1);
      }
      assert descriptions.size() > 0 : String.format("No description for %s", descFile);
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
   
   /**
    * Returns the n-th of the underlying sequences.
    * 
    * @param n
    *           Number of the sequence to return.
    * @return The n-th sequence.
    */
   public byte[] getSequence(int n) {
      if (n == 0) {
         return Arrays.copyOfRange(sequence, 0, (int) separatorPositions[0]);
      } else {
         return Arrays.copyOfRange(sequence, (int) separatorPositions[n - 1] + 1,
               (int) separatorPositions[n]);
      }
      // TODO why is separatorPositions of type long[]?
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
         qualityValues = new ArrayFile().setFile(qualityFile).readArray(qualityValues);
      }
      return qualityValues;
   }
   
   /**
    * Returns the quality values for the n-th of the underlying sequences.
    * 
    * @param n
    *           Number of the Sequence for the quality values.
    * @return Quality values for the n-th sequence or null if not available.
    * @throws IOException
    */
   public byte[] getQualityValuesForSequence(int n) throws IOException {
      if (qualityValues == null) {
         qualityValues = new ArrayFile().setFile(qualityFile).readArray(qualityValues);
      }
      if (n == 0) {
         return Arrays.copyOfRange(qualityValues, 0, (int) separatorPositions[0]);
      } else {
         return Arrays.copyOfRange(qualityValues, (int) separatorPositions[n - 1] + 1,
               (int) separatorPositions[n]);
      }
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
