package verjinxer.sequenceanalysis;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import verjinxer.util.ArrayFile;

/**
 * @author Markus Kemmerling
 */
public class SequenceWriter extends Sequence {

   private ArrayFile sequenceFile;
   private ArrayList<String> descriptions = new ArrayList<String>();
   private ArrayList<Long> sequenceLengths = new ArrayList<Long>();
   private long maxSequenceLength = 0;
   private long minSequenceLength = Long.MAX_VALUE;
   /** Sequence separator positions. */
   private ArrayList<Long> ssps = new ArrayList<Long>();
   private ArrayFile qualityFile = null;

   SequenceWriter(final String projectname, Mode mode) throws IOException {
      super(projectname, mode);

      sequenceFile = new ArrayFile(seqFilename);
      sequenceFile.openW();
   }

   @Override
   public void store() throws IOException {
      sequenceFile.close();

      // Write the ssp.
      long[] sspArray = new long[ssps.size()];
      int i = 0;
      for (long l : ssps)
         sspArray[i++] = l;
      new ArrayFile(sspFilename).writeArray(sspArray, 0, ssps.size());

      // Write the descriptions
      PrintWriter descfile = new PrintWriter(descFilename);
      for (String s : descriptions)
         descfile.println(s);
      descfile.close();
   }

   @Override
   public long writeBuffer(ByteBuffer tr) throws IOException {
      return sequenceFile.writeBuffer(tr);
   }

   @Override
   public void addInfo(String header, long length, long ssp) {
      descriptions.add(header);

      sequenceLengths.add(length);
      if (length < minSequenceLength)
         minSequenceLength = length;
      if (length > maxSequenceLength)
         maxSequenceLength = length;

      this.ssps.add(ssp);
   }

   @Override
   public long length() {
      return sequenceFile.length();
   }

   @Override
   public int getNumberSequences() {
      return ssps.size();
   }

   @Override
   public long[] getLengths() {
      long[] lengths = new long[sequenceLengths.size()];
      int i = 0;
      for (long l : sequenceLengths)
         lengths[i++] = l;
      return lengths;
   }

   @Override
   public long getMaximumSequenceLength() {
      return maxSequenceLength;
   }

   @Override
   public long getMinimumSequenceLength() {
      return minSequenceLength;
   }

   @Override
   public void addQualityValues(ByteBuffer buffer) throws IOException {
      if (qualityFile==null) {
         qualityFile = new ArrayFile(qualityFilename);
         qualityFile.openW();
      }
      qualityFile.writeBuffer(buffer);
   }
   
}
