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
 * This class is a set for sequences. It should not be used to create and represent sequences in
 * memory but to create and write them efficiently to disc.
 * 
 * @author Markus Kemmerling
 */
public class SequenceWriter implements ISequenceCreation {

   private final File seqFile;
   private final File sspFile;
   private final File descFile;
   private final File qualityFile;

   private ArrayFile sequenceArrayFile;
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

      sequenceArrayFile = new ArrayFile(seqFile);
      sequenceArrayFile.openW();
   }

   /**
    * Creates a new SequenceWriter that files have not the name of the project but the given
    * fileName.
    * 
    * @param project
    * @param fileName
    * @throws IOException
    */
   public SequenceWriter(final Project project, String fileName) throws IOException {
      seqFile = project.makeFile(FileTypes.SEQ, fileName);
      sspFile = project.makeFile(FileTypes.SSP, fileName);
      descFile = project.makeFile(FileTypes.DESC, fileName);
      qualityFile = project.makeFile(FileTypes.QUALITIY, fileName);

      sequenceArrayFile = new ArrayFile(seqFile);
      sequenceArrayFile.openW();
   }

   /* (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISequenceWriter#getSequenceFile()
    */
   public File getSequenceFile() {
      return seqFile;
   }

   /* (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISequenceWriter#getSequencesSeparatorPositionsFile()
    */
   public File getSequencesSeparatorPositionsFile() {
      return sspFile;
   }

   /* (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISequenceWriter#getDescriptionFile()
    */
   public File getDescriptionFile() {
      return descFile;
   }

   /* (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISequenceWriter#getQualityFile()
    */
   public File getQualityFile() {
      return qualityFile;
   }

   /**
    * Writes separator positions and descriptions into files.
    * 
    * @throws IOException
    */
   public void store() throws IOException {
      sequenceArrayFile.close();

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
    * Appends the content of the given buffer to the concatenated sequences. This is done by
    * writing it direct into file.
    * 
    * @param tr
    * @return Accumulated length of all sequences (length of .seq file after writing to disc).
    * @throws IOException
    */
   public long addSequence(ByteBuffer tr) throws IOException {
      return sequenceArrayFile.writeBuffer(tr);
   }

   /* (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISequenceWriter#addInfo(java.lang.String, long, long)
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

   @Override
   public long appendCharacter(byte character) throws IOException {
      return sequenceArrayFile.writeArray(new byte[]{character});
   }

   /**
    * @return Accumulated length of all sequences (length of .seq file).
    */
   public long length() {
      return sequenceArrayFile.length();
   }

   /* (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISequenceWriter#getNumberSequences()
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

   /* (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISequenceWriter#getMaximumLength()
    */
   public long getMaximumLength() {
      return maxSequenceLength;
   }

   /* (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISequenceWriter#getMinimumLength()
    */
   public long getMinimumLength() {
      return minSequenceLength;
   }

   /* (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISequenceWriter#addQualityValues(java.nio.ByteBuffer)
    */
   public void addQualityValues(ByteBuffer buffer) throws IOException {
      if (qualityArrayFile == null) {
         qualityArrayFile = new ArrayFile(qualityFile);
         qualityArrayFile.openW();
      }
      qualityArrayFile.writeBuffer(buffer);
   }

}
