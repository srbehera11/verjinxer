package verjinxer.sequenceanalysis;

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

   private final String seqFilename;
   private final String sspFilename;
   private final String descFilename;
   private final String qualityFilename;

   private ArrayFile sequenceFile;
   private ArrayList<String> descriptions = new ArrayList<String>();
   private ArrayList<Long> sequenceLengths = new ArrayList<Long>();
   private long maxSequenceLength = 0;
   private long minSequenceLength = Long.MAX_VALUE;
   /** Sequence separator positions. */
   private ArrayList<Long> separatorPositions = new ArrayList<Long>();
   private ArrayFile qualityFile = null;

   public SequenceWriter(final Project project) throws IOException {
      seqFilename = project.makeFileName(FileTypes.SEQ);
      sspFilename = project.makeFileName(FileTypes.SSP);
      descFilename = project.makeFileName(FileTypes.DESC);
      qualityFilename = project.makeFileName(FileTypes.QUALITIY);

      sequenceFile = new ArrayFile(seqFilename);
      sequenceFile.openW();
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
      new ArrayFile(sspFilename).writeArray(sspArray, 0, separatorPositions.size());

      // Write the descriptions
      PrintWriter descfile = new PrintWriter(descFilename);
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
      if (qualityFile == null) {
         qualityFile = new ArrayFile(qualityFilename);
         qualityFile.openW();
      }
      qualityFile.writeBuffer(buffer);
   }

}
