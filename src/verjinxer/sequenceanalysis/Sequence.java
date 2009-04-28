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
   protected final String seqFile;
   protected final String sspFile;
   protected final String descFile;

   /**
    * Sets the Filenames for the sequence, ssp and description files.
    * 
    * @param projectname
    */
   public Sequence(final String projectname, Mode mode) {
      seqFile = projectname + FileNameExtensions.seq;
      sspFile = projectname + FileNameExtensions.ssp;
      descFile = projectname + FileNameExtensions.desc;
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
      throw new RuntimeException(String.format("Operation not supported in %d mode",
            mode == Mode.READ ? "READ" : "WRITE"));
   }

   /**
    * @return Accumulated length of all sequences (length of .seq file)
    */
   public long length() {
      throw new RuntimeException(String.format("Operation not supported in %d mode",
            mode == Mode.READ ? "READ" : "WRITE"));
   }

   /**
    * @return The underlying array for the concatenated sequences.
    */
   public byte[] array() {
      throw new RuntimeException(String.format("Operation not supported in %d mode",
            mode == Mode.READ ? "READ" : "WRITE"));
   }

   /**
    * Rewinds this sequence. The position is set to zero.
    */
   public void rewind() {
      throw new RuntimeException(String.format("Operation not supported in %d mode",
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
      throw new RuntimeException(String.format("Operation not supported in %d mode",
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
      throw new RuntimeException(String.format("Operation not supported in %d mode",
            mode == Mode.READ ? "READ" : "WRITE"));
   }

   /**
    * @return Number of concatenated sequences
    */
   public int getNumberSequences() {
      throw new RuntimeException(String.format("Operation not supported in %d mode",
            mode == Mode.READ ? "READ" : "WRITE"));
   }

   /**
    * @return Array containing the length of each sequence
    */
   public long[] getLengths() {
      throw new RuntimeException(String.format("Operation not supported in %d mode",
            mode == Mode.READ ? "READ" : "WRITE"));
   }

   /**
    * @return Maximum lengths of sequences
    */
   public long getMaximumSequenceLength() {
      throw new RuntimeException(String.format("Operation not supported in %d mode",
            mode == Mode.READ ? "READ" : "WRITE"));
   }

   /**
    * @return Minimum length of sequences
    */
   public long getMinimumSequenceLength() {
      throw new RuntimeException(String.format("Operation not supported in %d mode",
            mode == Mode.READ ? "READ" : "WRITE"));
   }

}
