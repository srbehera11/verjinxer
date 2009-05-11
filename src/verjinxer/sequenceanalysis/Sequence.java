package verjinxer.sequenceanalysis;

import java.io.IOException;
import java.nio.ByteBuffer;

import verjinxer.FileNameExtensions;

/**
 * @author Markus Kemmerling
 */
public abstract class Sequence {

   public enum Mode {
      READ, WRITE
   }

   private final Mode mode;
   protected final String seqFilename;
   protected final String sspFilename;
   protected final String descFilename;
   protected final String qualityFilename;

   /**
    * Sets the Filenames for the sequence, ssp and description files.
    * 
    * @param projectname
    */
   public Sequence(final String projectname, Mode mode) {
      seqFilename = projectname + FileNameExtensions.seq;
      sspFilename = projectname + FileNameExtensions.ssp;
      descFilename = projectname + FileNameExtensions.desc;
      qualityFilename = projectname + FileNameExtensions.quality;
      this.mode = mode;
   }

   /**
    * Factory method. Generates, depending on the given mode, a Sequence to read information from
    * files into memory or a Sequence to write information into files.
    * 
    * @param projectname
    * @param mode
    * @return
    * @throws IOException
    */
   public static Sequence openSequence(final String projectname, Mode mode) throws IOException {
      if (mode == Mode.READ) {
         return new SequenceReader(projectname, mode);
      } else {
         return new SequenceWriter(projectname, mode);
      }
   }

   /**
    * Writes concatenated sequences, ssps and descriptions into files
    * 
    * @throws IOException
    */
   public void store() throws IOException {
      throw new UnsupportedOperationException(String.format("Operation not supported in %d mode",
            mode == Mode.READ ? "READ" : "WRITE"));
   }

   /**
    * @return Accumulated length of all sequences (length of .seq file)
    */
   public long length() {
      throw new UnsupportedOperationException(String.format("Operation not supported in %d mode",
            mode == Mode.READ ? "READ" : "WRITE"));
   }

   /**
    * @return The underlying array for the concatenated sequences.
    */
   public byte[] array() {
      throw new UnsupportedOperationException(String.format("Operation not supported in %d mode",
            mode == Mode.READ ? "READ" : "WRITE"));
   }

   /**
    * Rewinds this sequence. The position is set to zero.
    */
   public void rewind() {
      throw new UnsupportedOperationException(String.format("Operation not supported in %d mode",
            mode == Mode.READ ? "READ" : "WRITE"));
   }

   /**
    * Appends the content of the given buffer to the concatenated sequences
    * 
    * @param tr
    * @return Accumulated length of all sequences (length of .seq file) after writing
    * @throws IOException
    */
   public long writeBuffer(ByteBuffer tr) throws IOException {
      throw new UnsupportedOperationException(String.format("Operation not supported in %d mode",
            mode == Mode.READ ? "READ" : "WRITE"));
   }

   /**
    * Adds a new info the one of the sequences
    * 
    * @param header
    *           Sequenceheader
    * @param length
    *           Sequencelength
    * @param ssp
    *           Separator position after sequence
    */
   public void addInfo(String header, long length, long ssp) {
      throw new UnsupportedOperationException(String.format("Operation not supported in %d mode",
            mode == Mode.READ ? "READ" : "WRITE"));
   }

   /**
    * @return Number of concatenated sequences
    */
   public int getNumberSequences() {
      throw new UnsupportedOperationException(String.format("Operation not supported in %d mode",
            mode == Mode.READ ? "READ" : "WRITE"));
   }

   /**
    * @return Array containing the length of each sequence
    */
   public long[] getLengths() {
      throw new UnsupportedOperationException(String.format("Operation not supported in %d mode",
            mode == Mode.READ ? "READ" : "WRITE"));
   }

   /**
    * @return Maximum length of sequences
    */
   public long getMaximumSequenceLength() {
      throw new UnsupportedOperationException(String.format("Operation not supported in %d mode",
            mode == Mode.READ ? "READ" : "WRITE"));
   }

   /**
    * @return Minimum length of sequences
    */
   public long getMinimumSequenceLength() {
      throw new UnsupportedOperationException(String.format("Operation not supported in %d mode",
            mode == Mode.READ ? "READ" : "WRITE"));
   }

   public void addQualityValues(ByteBuffer buffer) throws IOException {
      throw new UnsupportedOperationException(String.format("Operation not supported in %d mode",
            mode == Mode.READ ? "READ" : "WRITE"));
   }

   /**
    * Returns array of quality values (concatenated, with separators); i.e. for each k,
    * getQualityValues()[k] contains the quality of the character arrar()[k].
    * @return Returns null of qualities are not available.
    */
   public byte[] getQualityValues() throws IOException {
      throw new UnsupportedOperationException(String.format("Operation not supported in %d mode",
            mode == Mode.READ ? "READ" : "WRITE"));
   }

}
