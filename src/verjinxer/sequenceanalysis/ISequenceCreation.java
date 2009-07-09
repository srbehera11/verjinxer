package verjinxer.sequenceanalysis;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;

import verjinxer.util.ArrayFile;

/**
 * @author Markus Kemmerling
 */
public interface ISequenceCreation {

   /**
    * @return The filename of the Sequence.
    */
   public abstract File getSequenceFile();

   /**
    * @return The filename of the separator positions
    */
   public abstract File getSequencesSeparatorPositionsFile();

   /**
    * @return The filename of the descriptions
    */
   public abstract File getDescriptionFile();

   /**
    * @return The filename of the Qualityfile
    */
   public abstract File getQualityFile();

   /**
    * Appends the content of the given buffer to the concatenated sequences.
    * 
    * @param tr
    * @return Accumulated length of all sequences (length of .seq file) after writing.
    * @throws IOException
    */
   public abstract long addSequence(ByteBuffer tr) throws IOException;

   /**
    * Adds a new info the one of the sequences.
    * 
    * @param header
    *           Sequenceheader
    * @param length
    *           Sequencelength
    * @param ssp
    *           Separator position after sequence.
    */
   public abstract void addInfo(String header, long length, long ssp);

   /**
    * @return Accumulated length of all sequences (length of .seq file).
    */
   public abstract long length();

   /**
    * @return Number of concatenated sequences.
    */
   public abstract int getNumberSequences();

   public abstract void addQualityValues(ByteBuffer buffer) throws IOException;

   /**
    * @return Maximum length of sequences.
    */
   public long getMaximumLength();

   /**
    * @return Minimum length of sequences.
    */
   public long getMinimumLength();

}