package verjinxer.sequenceanalysis;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import verjinxer.Project;
import verjinxer.util.ArrayFile;
import verjinxer.util.FileTypes;

/**
 * @author Markus Kemmerling
 */
public class SequenceWriter {

   private final File seqFile;
   private final File sspFile;
   private final File descFile;
   private final File qualityFile;

   private ArrayFile sequenceFile;
   private ArrayList<String> descriptions = new ArrayList<String>();
   private ArrayList<Long> sequenceLengths = new ArrayList<Long>();
   private long maxSequenceLength = 0;
   private long minSequenceLength = Long.MAX_VALUE;
   /** Sequence separator positions. */
   private ArrayList<Long> separatorPositions = new ArrayList<Long>();
   private ArrayFile qualityArrayFile = null;

   public SequenceWriter(final Project project) throws IOException {
      seqFile = project.makeFile(FileTypes.SEQ);
      sspFile = project.makeFile(FileTypes.SSP);
      descFile = project.makeFile(FileTypes.DESC);
      qualityFile = project.makeFile(FileTypes.QUALITIY);

      sequenceFile = new ArrayFile(seqFile);
      sequenceFile.openW();
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
    * Writes concatenated sequences, separator positions and descriptions into files.
    * 
    * @throws IOException
    */
   public void store() throws IOException {
      sequenceFile.close();

      // Write the ssp.
      long[] sspArray = new long[separatorPositions.size()];
      int i = 0;
      for (long l : separatorPositions)
         sspArray[i++] = l;
      new ArrayFile(sspFile).writeArray(sspArray, 0, separatorPositions.size());

      // Write the descriptions
      PrintWriter descfile = new PrintWriter(descFile);
      for (String s : descriptions)
         descfile.println(s);
      descfile.close();
   }

   /**
    * Appends the content of the given buffer to the concatenated sequences.
    * 
    * @param tr
    * @return Accumulated length of all sequences (length of .seq file) after writing.
    * @throws IOException
    */
   public long writeBuffer(ByteBuffer tr) throws IOException {
      return sequenceFile.writeBuffer(tr);
   }

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
   public void addInfo(String header, long length, long ssp) {
      descriptions.add(header);

      sequenceLengths.add(length);
      if (length < minSequenceLength)
         minSequenceLength = length;
      if (length > maxSequenceLength)
         maxSequenceLength = length;

      this.separatorPositions.add(ssp);
   }

   /**
    * @return Accumulated length of all sequences (length of .seq file).
    */
   public long length() {
      return sequenceFile.length();
   }

   /**
    * @return Number of concatenated sequences.
    */
   public int getNumberSequences() {
      return separatorPositions.size();
   }

   /**
    * @return Array containing the length of each sequence.
    */
   public long[] getLengths() {
      long[] lengths = new long[sequenceLengths.size()];
      int i = 0;
      for (long l : sequenceLengths)
         lengths[i++] = l;
      return lengths;
   }

   /**
    * @return Maximum length of sequences.
    */
   public long getMaximumLength() {
      return maxSequenceLength;
   }

   /**
    * @return Minimum length of sequences.
    */
   public long getMinimumLength() {
      return minSequenceLength;
   }

   public void addQualityValues(ByteBuffer buffer) throws IOException {
      if (qualityArrayFile == null) {
         qualityArrayFile = new ArrayFile(qualityFile);
         qualityArrayFile.openW();
      }
      qualityArrayFile.writeBuffer(buffer);
   }

}
