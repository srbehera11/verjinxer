package verjinxer;

import static java.lang.Math.floor;
import static java.lang.Math.log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import com.spinn3r.log5j.Logger;

import verjinxer.sequenceanalysis.BisulfiteQGramCoder;
import verjinxer.sequenceanalysis.QGramCoder;
import verjinxer.sequenceanalysis.QGramFilter;
import verjinxer.sequenceanalysis.Sequence;
import verjinxer.util.ArrayFile;
import verjinxer.util.ArrayUtils;
import verjinxer.util.BitArray;
import verjinxer.util.ProjectInfo;
import verjinxer.util.StringUtils;
import verjinxer.util.TicToc;

public class QGramIndexer {
   private static final Logger log = Globals.log;
   final boolean external;
   final boolean bisulfiteIndex;
   final int stride;
   Globals g;

   final ProjectInfo project;
   final int q;
   final int asize;
   final byte separator;
   final QGramFilter thefilter;

   private int maximumFrequency = 0;
   private int maximumBucketSize = 0;
   private double[] times;

   public QGramIndexer(Globals g, ProjectInfo project, int q) {
      this(g, project, q, false, false, 1, null);
   }

   public QGramIndexer(Globals g, ProjectInfo project, int q, boolean external,
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
         long filesize = project.getLongProperty("Length");
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
    *           If this is not null, an array sfrq will be written to this file. 
    *           sfrq[i] == n means: q-gram i appears in n different sequences.
    * @param separator
    *           a sequence separator, only used when qseqfreqfile != null.
    * @return An array of q-gram frequencies. frq[i] == n means that q-gram with code i occurs n
    *         times.
    * 
    *         TODO not only computes frequencies but also writes sequence frequencies
    */
   private int[] computeFrequencies(final ByteBuffer in, final QGramCoder coder,
         final String qseqfreqfile, final byte separator) {

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
      for (long pc : coder.sparseQGrams(in, doseqfreq, separator)) {
         final int qcode = (int) pc;
         final int pos = (int) (pc >> 32);
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
   private int[] computeFrequencies(final Sequence in, final QGramCoder coder,
         final String qseqfreqfile, final byte separator) {

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
      for (long pc : coder.sparseQGrams(in, doseqfreq, separator)) {
         final int qcode = (int) pc;
         final int pos = (int) (pc >> 32);
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
    *         buckets (that is, the number of q-grams in the index).
    */
   private Object[] computeBucketStartPositions(final int[] frq, final boolean overwrite) {
      final int[] bck = (overwrite) ? frq : Arrays.copyOf(frq, frq.length);
      // Java 5: bck = new int[frq.length]; System.arraycopy(frq,0,bck,0,frq.length);
      final int aq = bck.length - 1; // subtract sentinel space

      // apply filter: at the moment, bck contains frequencies (frq), not yet bucket sizes!
      for (int c = 0; c < aq; c++)
         if (thefilter.isFiltered(c))
            bck[c] = 0; // reduce frequency to zero

      // compute bck from frq
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

   public void generateAndWriteIndex(final String seqfile, final String bucketfile,
         final String qposfile) throws IOException {
      generateAndWriteIndex(seqfile, bucketfile, qposfile, null, null);
   }

   public void generateAndWriteIndex() throws IOException {

      final String seqfile = project.getName() + FileNameExtensions.seq;
      final String qbucketsfile = project.getName() + FileNameExtensions.qbuckets;
      final String qpositionsfile = project.getName() + FileNameExtensions.qpositions;

      generateAndWriteIndex(seqfile, qbucketsfile, qpositionsfile, null, null);
   }

   /**
    * Generates a q-gram index of a given file.
    * 
    * @param seqfile
    *           name of the sequence file (i.e., of the translated text)
    * @param bucketfile
    *           filename of the q-bucket file (can be null)
    * @param qposfile
    *           filename of the q-gram index (can be null)
    * @param qfreqfile
    *           filename of the q-gram frequency file (can be null)
    * @param qseqfreqfile
    *           filename for q-gram sequence frequencies, i.e., in how many different sequences does
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
    *            if bisulfiteIndex is set, but asize is not 4
    */
   public void generateAndWriteIndex(final String seqfile, final String bucketfile,
         final String qposfile, final String qfreqfile, final String qseqfreqfile)
         throws IOException {
      final TicToc totalTimer = new TicToc();
      final Sequence in = new Sequence(seqfile);
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
      int[] frq = computeFrequencies(in, coder, qseqfreqfile, separator);
      maximumFrequency = ArrayUtils.maximumElement(frq);
      if (qfreqfile != null)
         g.dumpIntArray(qfreqfile, frq, 0, aq);
      final double timeFrequencyCounting = timer.tocs();

      // Compute and write bucket array bck
      // bck[i] == r means: positions of q-grams with code i start at index r within qpos
      timer.tic();
      final Object[] r = computeBucketStartPositions(frq, external);
      final int[] bck = (int[]) r[0];
      maximumBucketSize = (Integer) r[1]; // only needed to return to caller
      if (external)
         frq = null; // in this case, frq has been overwritten with bck anyway
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
      final int[] qposslice = ArrayUtils.getIntSlice(sum);
      final int slicesize = qposslice.length;

      // open outfile and process each slice
      final ArrayFile outfile = new ArrayFile(qposfile).openW();
      int bckstart = 0;
      while (bckstart < aq) {
         TicToc wtimer = new TicToc();
         wtimer.tic();
         final int qposstart = bck[bckstart]; //
         int bckend;
         for (bckend = bckstart; bckend < aq && (bck[bckend + 1] - qposstart) <= slicesize; bckend++) {
         }
         final int qposend = bck[bckend];
         final int qpossize = qposend - qposstart;
         final double percentdone = (sum == 0) ? 100.0 : 100.0 * (double) qposend
               / ((double) sum + 0);
         log.info("  collecting qcodes [%d..%d] = qpos[%d..%d] --> %.1f%%", bckstart, bckend - 1,
               qposstart, qposend - 1, percentdone);
         assert ((qpossize <= slicesize && qpossize > 0) || sum == 0) : "qgram: internal consistency error";

         // read through input and collect all qgrams with bckstart<=qcode<bckend
         for (long pc : coder.sparseQGrams(in)) {
            final int pos = (int) (pc >>> 32);
            if (pos % stride != 0)
               continue;
            final int qcode = (int) (pc);
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

   public double[] getLastTimes() {
      return times;
   }

   /**
    * Checks the q-gram index of the given files for correctness.
    * 
    * @param seqfile
    *           name of the sequence file
    * @param q
    *           q-gram length
    * @param asize
    *           alphabet size
    * @param bucketfile
    *           filename of the q-bucket file
    * @param qposfile
    *           filename of the q-gram index
    * @param thefilter
    *           a QGramFilter, filtered q-grams should not appear in the index
    * @param bisulfiteIndex
    *           whether the index also contains bisulfite-treated q-grams
    * @return an index &gt;=0 where the first error in qpos is found, or -N-1&lt;0, where N is the
    *         number of q-grams in the index.
    * @throws java.io.IOException
    */
   public int checkQGramIndex(final String seqfile, final int q, final int asize,
         final String bucketfile, final String qposfile, final QGramFilter thefilter,
         final boolean bisulfiteIndex) throws IOException {
      // Read sequence and bucketfile into arrays
      System.gc();
      TicToc timer = new TicToc();
      log.info("  reading %s and %s", seqfile, bucketfile);
      final ArrayFile arf = new ArrayFile(null);
      byte[] s = arf.setFilename(seqfile).readArray((byte[]) null);
      int[] bck = arf.setFilename(bucketfile).readArray((int[]) null);
      log.info("  reading finished after %.2f sec", timer.tocs());

      final int n = s.length;
      final BitArray sok = new BitArray(n - q + 1); // TODO what is sok?
      log.info("  %s has length %d, contains %d %d-grams", seqfile, n, n - q + 1, q);

      // Initialize q-gram storage
      final QGramCoder coder;
      if (bisulfiteIndex) {
         coder = new BisulfiteQGramCoder(q);
      } else {
         coder = new QGramCoder(q, asize);
      }
      byte[] qgram = new byte[q];

      // Read the q-position array.
      IntBuffer qpos = g.mapR(qposfile).asIntBuffer();
      int b = -1; // current bucket
      int bold;
      int i = -1;
      log.info("  scanning %s, memory-mapped...", qposfile);
      int r; // can't declare r in 'for' statement, because we need it as return value!
      final int rend = qpos.capacity();
      for (r = 0; r < rend; r++) {
         i = qpos.get();
         bold = b;
         // maybe have reached a new bucket; in this case, generate the new correct q-gram
         while (r == bck[b + 1])
            b++;
         if (bold != b)
            qgram = coder.qGram(b, qgram);
         // TODO: check whether qgram (in index) is bis-compatible to s[i...i+q-1] (text q-gram).
         if (!coder.areCompatible(qgram, 0, s, i)) {
            log.info("r=%d, i=%d, expected: %s, observed: %s.", r, i, StringUtils.join("", qgram,
                  0, q), StringUtils.join("", s, i, q));
            return r;
         }
         assert i >= 0 && i <= n - q;
         sok.set(i, true);
      }

      // now check that all untouched text positions in fact contain filtered or invalid q-grams
      log.info("  checking %d remaining text positions...", n - q + 1 - sok.cardinality());
      for (int ii = 0; ii < n - q + 1; ii++) {
         if (sok.get(ii) == 1)
            continue;
         final int cd = coder.code(s, ii);
         if (cd < 0 || thefilter.get(cd) == 1)
            continue;
         log.info("  ERROR: qgram at pos %d has code %d [%s], not seen in qpos and not filtered!",
               ii, cd, StringUtils.join("", coder.qGram(cd, qgram), 0, q));
         return n; // error: return length of the text ( > number of q-positions)
      }

      log.info("  checking finished after (total) %.2f sec", timer.tocs());
      return (-r - 1);
   }

   /**
    * Reads a sequence file from disk into a ByteBuffer.
    * 
    * @param seqfile
    *           name of the sequence file
    * @param memoryMapped
    *           whether to use memory mapping instead of reading the sequence
    * @return the sequence in a ByteBuffer
    */
   private ByteBuffer readSequenceFile(final String seqfile, boolean memoryMapped)
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

   /**
    * Check a project's q-gram index.
    * 
    * @param in
    *           base file name of the sequence file
    * @param project
    *           project
    * @return a negative value, if the index is ok; a positive r value indicates an index in qpos
    *         which seems to be wrong (ie, the the q-gram at qpos[r] in the text seems to disagree
    *         with the opinion of the index.
    */
   public int docheck(final String in, final ProjectInfo project) {
      g.cmdname = "qgramcheck";
      // Parse prj -> q, asize
      int asize = 0;
      int q = 0;
      try {
         asize = project.getIntProperty("qAlphabetSize");
         q = project.getIntProperty("q");
      } catch (NumberFormatException ex) {
         log.error("qgramcheck: q-gram index does not seem to exist (create it!)");
         g.terminate(1);
      }
      final int ffc = project.getIntProperty("qFilterComplexity");
      final int ffm = project.getIntProperty("qFilterDelta");
      final QGramFilter fff = new QGramFilter(q, asize, ffc, ffm);
      final boolean bisulfiteIndex = project.isBisulfiteIndex();

      // call checking routine
      log.info("qgramcheck: checking %s... q=%d, asize=%d", in, q, asize);
      log.info("qgramcheck: filter %d:%d filters %d q-grams", ffc, ffm, fff.cardinality());
      int result = 0;
      try {
         result = checkQGramIndex(in + FileNameExtensions.seq, q, asize, in
               + FileNameExtensions.qbuckets, in + FileNameExtensions.qpositions, fff,
               bisulfiteIndex);
      } catch (IOException ex) {
         log.warn("qgramcheck: error on %s: %s", in, ex);
      }

      // log result and return the result
      if (result < 0)
         log.info("qgramcheck: %s is OK", in);
      else
         log.info("qgramcheck: %s has an error at qpos[%d]", in, result);
      return result;
   }
}
