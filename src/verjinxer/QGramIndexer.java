package verjinxer;

import static java.lang.Math.floor;
import static java.lang.Math.log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import com.spinn3r.log5j.Logger;

import verjinxer.sequenceanalysis.BisulfiteQGramCoder;
import verjinxer.sequenceanalysis.QGramCoder;
import verjinxer.sequenceanalysis.QGramFilter;
import verjinxer.sequenceanalysis.Sequences;
import verjinxer.util.ArrayFile;
import verjinxer.util.ArrayUtils;
import verjinxer.util.FileTypes;
import verjinxer.util.PositionQCodePair;
import verjinxer.util.TicToc;

public class QGramIndexer {
   private static final Logger log = Globals.getLogger();
   
   /** Whether not to save memory at the cost of lower speed */
   final boolean external;

   /** Whether to simulate bisulfite treatment */
   final boolean bisulfiteIndex;
   
   /**
    * Only store q-grams whose positions are divisible by stride (default: 1) 
    * TODO maybe also 64Bit???
    */
   final int stride;
   
   Globals g;
   
   /** Project info */
   final Project project;
   
   /** Length of q-gram */
   final int q;
   
   /** Alphabet size */
   final int asize;
   
   /** Separator for different sequences according to alphabet */
   final byte separator;
   
   /** Filter, which q-gram is not considered */
   final QGramFilter thefilter;

   // TODO 64Bit
   private int maximumFrequency = 0;
   private int maximumBucketSize = 0;
   private double[] times;

   /**
    * Factory method. Generates a QGramIndexer, either 32Bit or 64Bit, according to the sequence
    * length
    * 
    * @param g
    * @param project
    * @param q
    *           Length of q-gram
    * @return New instance of QGramIndexer
    */
   public static QGramIndexer generateQGramIndexer(Globals g, Project project, int q) {
      return new QGramIndexer(g, project, q);
   }

   /**
    * Factory method. Generates a QGramIndexer, either 32Bit or 64Bit, according to the sequence
    * length
    * 
    * @param g
    * @param project
    * @param q
    *           Length of q-gram
    * @param external
    *           Whether not to save memory at the cost of lower speed
    * @param bisulfiteIndex
    *           Whether to simulate bisulfite treatment
    * @param stride
    *           Only store q-grams whose positions are divisible by stride
    * @param filterparam
    * @return New instance of QGramIndexer
    */
   public static QGramIndexer generateQGramIndexer(Globals g, Project project, int q,
         boolean external, boolean bisulfiteIndex, int stride, String filterparam) {

      return new QGramIndexer(g, project, q, external, bisulfiteIndex, stride, filterparam);
   }

   /**
    * 
    * @param g
    * @param project
    * @param q
    *           Length of q-gram
    */
   public QGramIndexer(Globals g, Project project, int q) {
      this(g, project, q, false, false, 1, null);
   }

   /**
    * 
    * @param g
    * @param project
    * @param q
    *           Length of q-gram
    * @param external
    *           Whether not to save memory at the cost of lower speed
    * @param bisulfiteIndex
    *           Whether to simulate bisulfite treatment
    * @param stride
    *           Only store q-grams whose positions are divisible by stride
    * @param filterparam
    */
   public QGramIndexer(Globals g, Project project, int q, boolean external,
         boolean bisulfiteIndex, int stride, String filterparam) {
      this.g = g;
      this.external = external;
      this.stride = stride;
      this.bisulfiteIndex = bisulfiteIndex;
      this.project = project;

      asize = project.getIntProperty("LargestSymbol") + 1;
      separator = (byte) project.getIntProperty("Separator");
      project.setProperty("LastAction", "qgram");
      project.setBisulfiteIndex(bisulfiteIndex);
      project.setProperty("qAlphabetSize", asize);
      project.setStride(stride);

      // determine q-gram size qq, either from given -q option, or default by length
      int qq;
      if (q > 0)
         qq = q;
      else {
         long filesize = project.getLongProperty("Length"); // length of the sequence
         qq = computeSensibleQ(filesize);
      }
      project.setProperty("q", qq);
      this.q = qq;

      // create the q-gram filter
      log.info("qgram: processing: project=%s, asize=%d, q=%d", project.getName(), asize, qq);
      thefilter = new QGramFilter(qq, asize, filterparam);
      project.setProperty("qFilterComplexity", thefilter.getComplexity());
      project.setProperty("qFilterDelta", thefilter.getDelta());
      final int qfiltered = thefilter.cardinality();
      log.info("qgram: filtering %d q-grams", qfiltered);
      project.setProperty("qFiltered", qfiltered);
      // for (int i = thefilter.nextSetBit(0); i >= 0; i = thefilter.nextSetBit(i+1))
      // log.info(" %s", coder.qGramString(i,AlphabetMap.DNA()));
   }

   private int computeSensibleQ(long fsize) {
      int qq;
      qq = (int) (floor(log(fsize) / log(asize))) - 2;
      // TODO this should be set to 1 as soon as the QGramIndex can handle that
      if (qq < 5)
         qq = 5;
      return qq;
   }

   /**
    * Reads the given ByteBuffer and computes q-gram frequencies. At this stage, filters are
    * ignored: The frequencies are the original frequencies in the byte file, before applying a
    * filter. This implementation uses a sparse q-gram iterator.
    * 
    * @param in
    *           the input ByteBuffer.
    * @param coder
    *           the q-gram coder.
    * @param qseqfreqfile
    *           If this is not null, an array sfrq will be written to this file. sfrq[i] == n means:
    *           q-gram i appears in n different sequences.
    * @param separator
    *           a sequence separator, only used when qseqfreqfile != null.
    * @return An array of q-gram frequencies. frq[i] == n means that q-gram with code i occurs n
    *         times.
    * 
    *         TODO not only computes frequencies but also writes sequence frequencies TODO 64BIT
    */
   private int[] computeFrequencies(final ByteBuffer in, final QGramCoder coder,
         final File qseqfreqfile, final byte separator) {

      final TicToc timer = new TicToc();
      final int aq = coder.getNumberOfQGrams();
      // TODO Maybe 64Bit
      int[] lastseq = null; // lastseq[i] == k := q-gram i was last seen in sequence k
      int[] sfrq = null; // sfrq[i] == n := q-gram i appears in n distinct sequences.
      // TODO 64BIT
      final int[] frq = new int[aq + 1]; // frq[i] == n := q-gram i appears n times; space for
      // sentinel at the end

      final boolean doseqfreq = (qseqfreqfile != null); // true if we compute sequence-based q-gram
      // frequencies
      if (doseqfreq) {
         lastseq = new int[aq];
         Arrays.fill(lastseq, -1);
         sfrq = new int[aq];
      }

      log.debug("doseqfreq = %s, separator = %d", doseqfreq, separator);
      // TODO also 64Bit???
      int seqnum = 0;
      // TODO sparseQGrams must be extended so that pos can be 32Bit
      for (PositionQCodePair pc : coder.sparseQGrams(in, doseqfreq, separator)) {
         final int qcode = pc.qcode; // lower 32bit
         // TODO Pos must be 64Bit,
         final int pos = pc.position; // higher 32bit
         if (qcode < 0) {
            assert doseqfreq;
            seqnum++;
            continue;
         }

         // TODO make this work again
         // assert qcode >= 0 && qcode < frq.length : String.format(
         // "Error: qcode=%d at pos %d (%s)", qcode, pos, StringUtils.join("",
         // coder.getQCoder().qGram(qcode), 0, coder.getQCoder().q)); // DEBUG
         if (pos % stride == 0)
            frq[qcode]++;
         if (doseqfreq && lastseq[qcode] < seqnum) {
            lastseq[qcode] = seqnum;
            if (pos % stride == 0)
               sfrq[qcode]++;
         }
      }
      in.rewind();
      log.info("  time for word counting: %.2f sec", timer.tocs());
      if (doseqfreq)
         g.dumpIntArray(qseqfreqfile, sfrq);

      return frq;
   }

   /** @see computeFrequencies(ByteBuffer,QGramCoder,String,byte) */
   private int[] computeFrequencies(final Sequences in, final QGramCoder coder,
         final File qseqfreqfile, final byte separator) {

      final TicToc timer = new TicToc();
      final int aq = coder.getNumberOfQGrams();
      int[] lastseq = null; // lastseq[i] == k := q-gram i was last seen in sequence k
      int[] sfrq = null; // sfrq[i] == n := q-gram i appears in n distinct sequences.
      final int[] frq = new int[aq + 1]; // frq[i] == n := q-gram i appears n times; space for
      // sentinel at the end

      final boolean doseqfreq = (qseqfreqfile != null); // true if we compute sequence-based q-gram
      // frequencies
      if (doseqfreq) {
         lastseq = new int[aq];
         Arrays.fill(lastseq, -1);
         sfrq = new int[aq];
      }

      log.debug("doseqfreq = %s, separator = %d", doseqfreq, separator);
      int seqnum = 0;
      for (PositionQCodePair pc : coder.sparseQGrams(in, doseqfreq, separator)) {
         final int qcode = pc.qcode;
         final int pos = pc.position;
         if (qcode < 0) {
            assert doseqfreq;
            seqnum++;
            continue;
         }

         // TODO make this work again
         // assert qcode >= 0 && qcode < frq.length : String.format(
         // "Error: qcode=%d at pos %d (%s)", qcode, pos, StringUtils.join("",
         // coder.getQCoder().qGram(qcode), 0, coder.getQCoder().q)); // DEBUG
         if (pos % stride == 0)
            frq[qcode]++;
         if (doseqfreq && lastseq[qcode] < seqnum) {
            lastseq[qcode] = seqnum;
            if (pos % stride == 0)
               sfrq[qcode]++;
         }
      }
      log.info("  time for word counting: %.2f sec", timer.tocs());
      if (doseqfreq)
         g.dumpIntArray(qseqfreqfile, sfrq);

      return frq;
   }

   /**
    * Computes the necessary sizes of q-gram buckets from q-gram frequencies, while taking a filter
    * into account.
    * 
    * @param frq
    *           the array of q-gram frequencies
    * @param overwrite
    *           if true, overwrites frq with bck; otherwise, creates a separate array for bck
    * @param thefilter
    *           a filter that specifies which q-grams to ignore
    * @return An array of size 2 containing {bck, maxbcksize}, where bck is the array of bucket
    *         positions and maxbcksize is the largest occurring bucket size. The actual bucket
    *         starts are in bck[0..bck.length-2]. The element bck[bck.length-1] is the sum over all
    *         buckets (that is, the number of q-grams in the index). //TODO 64Bit
    */
   private Object[] computeBucketStartPositions(final int[] frq, final boolean overwrite) {
      // TODO 64Bit
      final int[] bck = (overwrite) ? frq : Arrays.copyOf(frq, frq.length);
      // Java 5: bck = new int[frq.length]; System.arraycopy(frq,0,bck,0,frq.length);
      final int aq = bck.length - 1; // subtract sentinel space

      // apply filter: at the moment, bck contains frequencies (frq), not yet bucket sizes!
      for (int c = 0; c < aq; c++)
         if (thefilter.isFiltered(c))
            bck[c] = 0; // reduce frequency to zero

      // compute bck from frq
      // TODO 64Bit
      int sum = 0;
      int add = 0;
      int maxbcksize = 0; // maximum bucket size
      for (int c = 0; c < aq; c++) {
         add = bck[c];
         bck[c] = sum;
         sum += add;
         if (add > maxbcksize)
            maxbcksize = add;
      }
      bck[aq] = sum; // last entry (sentinel) shows how many entries in index
      final Object[] result = { bck, maxbcksize };

      return result;
   }

   public void generateAndWriteIndex(final File seqfile, final File bucketfile,
         final File qposfile) throws IOException {
      generateAndWriteIndex(seqfile, bucketfile, qposfile, null, null);
   }

   public void generateAndWriteIndex() throws IOException {

      final File seqfile = project.makeFile(FileTypes.SEQ);
      final File qbucketsfile = project.makeFile(FileTypes.QBUCKETS);
      final File qpositionsfile = project.makeFile(FileTypes.QPOSITIONS);

      generateAndWriteIndex(seqfile, qbucketsfile, qpositionsfile, null, null);
   }

   /**
    * Generates a q-gram index of a given file.
    * 
    * @param seqfile
    *           the sequence file (i.e., the translated text)
    * @param bucketfile
    *           the q-bucket file (can be null)
    * @param qposfile
    *           the q-gram index (can be null)
    * @param qfreqfile
    *           the q-gram frequency file (can be null)
    * @param qseqfreqfile
    *           q-gram sequence frequencies, i.e., in how many different sequences does
    *           the q-gram appear?
    * @return the QGramIndex unless external and unless not enough memory. note that the index is
    *         both written to disk and returned.
    * @return array of size 6: { bck, qpos, qfreq, times, maxfreq, maxqbck }, where in.t[] bck:
    *         array of q-bucket starts in qpos (null if external); int[] qpos: array of starting
    *         positions of q-grams in the text (null if external); int[] qfreq: array of q-gram
    *         frequencies (null if external); double[] times: contains the times taken to compute
    *         the q-gram index in seconds; Integer maxfreq: largest frequency; Integer maxqbck:
    *         largest q-bucket size.
    * @throws java.io.IOException
    * @throws java.lang.IllegalArgumentException
    *            if bisulfiteIndex is set, but asize is not 4 TODO 64Bit
    */
   public void generateAndWriteIndex(final File seqfile, final File bucketfile,
         final File qposfile, final File qfreqfile, final File qseqfreqfile)
         throws IOException {
      final TicToc totalTimer = new TicToc();
      final Sequences in = project.readSequences();
      final long ll = in.length();

      final QGramCoder coder;
      if (bisulfiteIndex) {
         coder = new BisulfiteQGramCoder(q);
      } else {
         coder = new QGramCoder(q, asize);
      }
      final int aq = coder.getNumberOfQGrams();
      log.info("  counting %d different %d-grams...", aq, q);

      // Scan file once and count qgrams.
      // One file may contain multiple sequences, separated by the separator.
      // frq[i] == n means: q-gram with code i occurs n times in the input sequence.
      // This holds before applying a filter.
      final TicToc timer = new TicToc();
      // TODO 64Bit
      int[] frq = computeFrequencies(in, coder, qseqfreqfile, separator);
      maximumFrequency = ArrayUtils.maximumElement(frq);
      if (qfreqfile != null)
         g.dumpIntArray(qfreqfile, frq, 0, aq);
      final double timeFrequencyCounting = timer.tocs();

      // Compute and write bucket array bck
      // bck[i] == r means: positions of q-grams with code i start at index r within qpos
      timer.tic();
      final Object[] r = computeBucketStartPositions(frq, external);
      // TODO 64Bit
      final int[] bck = (int[]) r[0];
      // TODO 64Bit
      maximumBucketSize = (Integer) r[1]; // only needed to return to caller
      if (external)
         frq = null; // in this case, frq has been overwritten with bck anyway
      // TODO 64Bit
      final int sum = bck[aq];
      if (bucketfile != null)
         g.dumpIntArray(bucketfile, bck);
      final double timeBucketsGeneration = timer.tocs();
      log.info("  time for reading, word counting, and writing buckets: %.2f sec",
            totalTimer.tocs());
      log.info("  input file size: %d;  (filtered) qgrams in qbck: %d", ll, sum);
      log.info("  average number of q-grams per sequence position is %.2f", ((double) sum) / ll);

      // if (sum==0) { Object[] ret = {bck, null, frq, times, maxfreq, maxqbck}; return ret; }

      // Determine slice size
      timer.tic();
      // TODO 64Bit
      final int[] qposslice = ArrayUtils.getIntSlice(sum);
      final int slicesize = qposslice.length;

      // open outfile and process each slice
      final ArrayFile outfile = new ArrayFile(qposfile).openW();
      int bckstart = 0;
      while (bckstart < aq) {
         TicToc wtimer = new TicToc();
         wtimer.tic();
         // TODO 64Bit
         final int qposstart = bck[bckstart]; //
         int bckend;
         for (bckend = bckstart; bckend < aq && (bck[bckend + 1] - qposstart) <= slicesize; bckend++) {
         }
         // TODO 64Bit
         final int qposend = bck[bckend];
         final int qpossize = qposend - qposstart; // remain int in 64Bit -> assertion
         final double percentdone = (sum == 0) ? 100.0 : 100.0 * (double) qposend
               / ((double) sum + 0);
         log.info("  collecting qcodes [%d..%d] = qpos[%d..%d] --> %.1f%%", bckstart, bckend - 1,
               qposstart, qposend - 1, percentdone);
         assert ((qpossize <= slicesize && qpossize > 0) || sum == 0) : "qgram: internal consistency error";

         // read through input and collect all qgrams with bckstart<=qcode<bckend
         // TODO 64Bit whole loop
         for (PositionQCodePair pc : coder.sparseQGrams(in)) {
            final int pos = pc.position;
            if (pos % stride != 0)
               continue;
            final int qcode = pc.qcode;
            if (bckstart <= qcode && qcode < bckend && thefilter.get(qcode) == 0)
               qposslice[(bck[qcode]++) - qposstart] = pos;
         }
         log.info("    collecting slice took %.2f sec", wtimer.tocs());

         // write slice to file
         log.info("    writing slice to %s", qposfile);
         wtimer.tic();
         outfile.writeArray(qposslice, 0, qpossize);
         log.info("    writing slice took %.2f sec", wtimer.tocs());
         bckstart = bckend;
      }

      project.setProperty("qfreqMax", maximumFrequency);
      project.setProperty("qbckMax", maximumBucketSize);
      project.store();
      // try {
      // project.store();
      // } catch (IOException ex) {
      // // TODO the following message
      // log.error("qgram: could not write %s, skipping! (%s)", project.getFileName(), ex);
      // }

      // clean up, return arrays only if not external and they fit in memory
      outfile.close();
      final double timeQpos = timer.tocs();
      final double timeTotal = totalTimer.tocs();
      final int[] qpos = (slicesize == sum && !external) ? qposslice : null;

      times = new double[] { timeTotal, timeFrequencyCounting, timeBucketsGeneration, timeQpos };

      // return new QGramIndex(bck, qpos, q, maxbck);
      // Object[] { (external ? null : bck), qpos, frq, times, maxfreq, maxbck };
   }

   // TODO remove this (?) should be in QGramIndex
   public int getMaximumFrequency() {
      return maximumFrequency;
   }

   public int getMaximumBucketSize() {
      return maximumBucketSize;
   }

   public void setMaximumFrequency(int maximumFrequency) {
      this.maximumFrequency = maximumFrequency;
   }

   public void setMaximumBucketSize(int maximumBucketSize) {
      this.maximumBucketSize = maximumBucketSize;
   }

   public double[] getLastTimes() {
      return times;
   }

   /**
    * Reads a sequence file from disk into a ByteBuffer.
    * 
    * @param seqfile
    *           the sequence file
    * @param memoryMapped
    *           whether to use memory mapping instead of reading the sequence
    * @return the sequence in a ByteBuffer TODO posible to write human gnome in ByteBuffer???
    */
   private ByteBuffer readSequenceFile(final File seqfile, boolean memoryMapped)
         throws IOException {
      if (memoryMapped)
         return g.mapR(seqfile);
      // not external: read bytes into array-backed buffer
      log.info("  reading sequence file '%s'...", seqfile);
      TicToc timer = new TicToc();
      ByteBuffer in = new ArrayFile(seqfile, 0).readArrayIntoNewBuffer();
      log.info("  ...read %d bytes; this took %.1f sec", in.capacity(), timer.tocs());
      return in;
   }
}
