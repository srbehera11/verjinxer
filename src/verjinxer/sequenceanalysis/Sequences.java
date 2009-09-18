package verjinxer.sequenceanalysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import verjinxer.Project;
import verjinxer.util.ArrayFile;
import verjinxer.util.FileTypes;

/**
 * This class is a set for sequences. They can be loaded from disc (previously created and written
 * with the class SequenceWriter) or created from scratch in memory. There is no possibility to
 * store changes on disc.
 * 
 * @author Markus Kemmerling
 */
public class Sequences implements ISequenceCreation {

   protected final File seqFile;
   protected final File sspFile;
   protected final File descFile;
   protected final File qualityFile;

   private byte[] sequence = null;
   private long[] separatorPositions = null;
   private ArrayList<String> descriptions = null;
   private byte[] qualityValues = null;
   
   /**
    * Creates an instance of Sequences by reading the .sec and .ssp files of the given project.
    * 
    * @param project
    * @return An instance of Sequences.
    * @throws IOException
    */
   public static Sequences readSequencesFromDisc(final Project project) throws IOException {
      Sequences sequences = new Sequences(project);
      sequences.load();
      return sequences;
   }

   /**
    * Creates an instance of Sequences by reading the .sec and .ssp files of the given project with
    * the given name.
    * 
    * @param project
    * @param name
    * @return An instance of Sequences.
    * @throws IOException
    */
   public static Sequences readSequencesFromDisc(final Project project, final String name)
         throws IOException {
      Sequences sequences = new Sequences(project, name);
      sequences.load();
      return sequences;
   }

   /**
    * Creates a new and empty Sequences in memory.
    * 
    * @return An instance of Sequences.
    */
   public static Sequences createEmptySequencesInMemory() {
      return new Sequences();
   }

   /**
    * Constructs a new instance of Sequences and determines the files to be load from according to
    * the given project.
    * 
    * @param project
    */
   private Sequences(final Project project) {
      seqFile = project.makeFile(FileTypes.SEQ);
      sspFile = project.makeFile(FileTypes.SSP);
      descFile = project.makeFile(FileTypes.DESC);
      qualityFile = project.makeFile(FileTypes.QUALITIY);
   }
   
   /**
    * Creates a new sequences and determines the files to be load from according to the given
    * project and the given name.
    * 
    * @param project
    * @param name
    * @throws IOException
    */
   private Sequences(final Project project, final String name) {
      seqFile = project.makeFile(FileTypes.SEQ, name);
      sspFile = project.makeFile(FileTypes.SSP, name);
      descFile = project.makeFile(FileTypes.DESC, name);
      qualityFile = project.makeFile(FileTypes.QUALITIY, name);
   }

   /**
    * Constructs a new and empty instance of Sequences with no files to load from.
    */
   private Sequences() {
      seqFile = null; 
      sspFile = null;
      descFile = null;
      qualityFile = null;

      sequence = new byte[0];
      separatorPositions = new long[0];
   }

   /**
    * @return The filename of the Sequence (can be null if the Sequences were not load from disc).
    */
   public File getSequenceFile() {
      return seqFile;
   }

   /**
    * @return The filename of the separator positions (can be null if the Sequences were not load from disc).
    */
   public File getSequencesSeparatorPositionsFile() {
      return sspFile;
   }

   /**
    * @return The filename of the descriptions (can be null if the Sequences were not load from disc).
    */
   public File getDescriptionFile() {
      return descFile;
   }

   /**
    * @return The filename of the Qualityfile (can be null if the Sequences were not load from disc).
    */
   public File getQualityFile() {
      return qualityFile;
   }

   /**
    * Reads the .seq and .ssp files into memory. sequence and ssp are null until this method is
    * invoked.
    */
   private void load() {
      // Code only copied from Globals.slurpByteArray(String file)
      // TODO with runs, seqFile is f.e. chr22.runseq.seq, what is wrong
      try {
         // TODO: uuuooh. think about static methods!
         // method to slurp array into memory are available in Globals (but not static)
         ArrayFile af = new ArrayFile();
         sequence = af.setFile(seqFile).readArray(sequence);
         separatorPositions = af.setFile(sspFile).readArray(separatorPositions);
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
    * @return Accumulated length of all sequences.
    */
   public long length() {
      return sequence.length;
      // return type must be long because SequenceWriter.length() delegates to
      // ArrayFile.length() which returns long
   }

   /**
    * @return The underlying array for the concatenated sequences.
    */
   public byte[] array() {
      return sequence;
   }

   /**
    * @return Number of concatenated sequences.
    */
   public int getNumberSequences() {
      return separatorPositions.length;
   }
   
   /**
    * Returns the n-th of the underlying sequences. Be aware, that the last element in the returned
    * array is the separator.
    * 
    * @param n
    *           Number of the sequence to return.
    * @return The n-th sequence.
    */
   public byte[] getSequence(int n) {
      if (n == 0) {
         return Arrays.copyOfRange(sequence, 0, (int) separatorPositions[0] + 1);
      } else {
         return Arrays.copyOfRange(sequence, (int) separatorPositions[n - 1] + 1,
               (int) separatorPositions[n] + 1);
      }
      // TODO why is separatorPositions of type long[]?
   }

   /**
    * Returns array of quality values (concatenated, with separators); i.e. for each k,
    * getQualityValues()[k] contains the quality of the character arrar()[k].
    * 
    * @return Returns null if qualities are not available.
    */
   public byte[] getQualityValues() throws IOException {
      if (qualityValues == null) {
         qualityValues = new ArrayFile().setFile(qualityFile).readArray(qualityValues);
      }
      return qualityValues;
   }
   
   /**
    * Returns the quality values for the n-th of the underlying sequences. Be aware, that the last
    * element in the returned array is the separator.
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
         return Arrays.copyOfRange(qualityValues, 0, (int) separatorPositions[0] + 1);
      } else {
         return Arrays.copyOfRange(qualityValues, (int) separatorPositions[n - 1] + 1,
               (int) separatorPositions[n] + 1);
      }
   }

   /**
    * @return The positions of the separators between the sequences.
    */
   public long[] getSeparatorPositions() {
      return separatorPositions;
   }
   
   /**
    * Returns the start and end indices for the n-th sequence within the underlying array (all
    * sequences concatenated). The start index points to the first value of the n-th sequence. The
    * end index points to the separator after the n-th sequence and so points at the first position
    * after the sequence.
    * 
    * @param n
    *           Number of the sequence
    * @return An array of length 2 with a[0] = start index and a[1] = end index.
    */
   public int[] getSequenceBoundaries(final int n) {
      int[] boundaries = new int[2];
      if (n == 0) {
         boundaries[0] = 0;
         boundaries[1] = (int) separatorPositions[0];
      } else {
         boundaries[0] = (int) separatorPositions[n - 1] + 1;
         boundaries[1] = (int) separatorPositions[n];
      }
      return boundaries;
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

   @Override
   public void addInfo(String header, long length, long ssp) {
      if (descriptions == null) {
         descriptions = new ArrayList<String>();
      }
      descriptions.add(header);

      separatorPositions = Arrays.copyOf(separatorPositions, separatorPositions.length + 1);
      separatorPositions[separatorPositions.length - 1] = ssp;
   }

   @Override
   public void addQualityValues(ByteBuffer buffer) throws IOException {
      if (qualityValues == null) {
         qualityValues = new byte[0];
      }
      final int end = qualityValues.length;
      qualityValues = Arrays.copyOf(qualityValues, qualityValues.length + buffer.remaining());
      buffer.get(qualityValues, end, buffer.remaining());
   }

   @Override
   public long addSequence(ByteBuffer buffer) throws IOException {
      final int end = sequence.length;
      sequence = Arrays.copyOf(sequence, sequence.length + buffer.remaining());
      buffer.get(sequence, end, buffer.remaining());
      return sequence.length;
   }

   @Override
   public long getMinimumLength() {
      if (separatorPositions.length < 1) {
         return 0;
      }

      long min = separatorPositions[0];
      for (int i = 1; i < separatorPositions.length; i++) {
         if (separatorPositions[i] - separatorPositions[i - 1] < min) {
            min = separatorPositions[i] - separatorPositions[i - 1];
         }
      }
      return min;
   }

   @Override
   public long getMaximumLength() {
      if (separatorPositions.length < 1) {
         return 0;
      }

      long max = separatorPositions[0];
      for (int i = 1; i < separatorPositions.length; i++) {
         if (separatorPositions[i] - separatorPositions[i - 1] > max) {
            max = separatorPositions[i] - separatorPositions[i - 1];
         }
      }
      return max;
   }

}
