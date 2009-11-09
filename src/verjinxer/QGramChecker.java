package verjinxer;

import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;

import com.spinn3r.log5j.Logger;

import verjinxer.sequenceanalysis.BisulfiteQGramCoder;
import verjinxer.sequenceanalysis.QGramCoder;
import verjinxer.sequenceanalysis.QGramFilter;
import verjinxer.util.ArrayFile;
import verjinxer.util.BitArray;
import verjinxer.util.FileTypes;
import verjinxer.util.StringUtils;
import verjinxer.util.TicToc;

public class QGramChecker {

   private static final Logger log = Globals.getLogger();

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
   public static int docheck(final Project project) {
      Globals.cmdname = "qgramcheck";
      // Parse prj -> q, asize
      int asize = 0;
      int q = 0;
      try {
         asize = project.getIntProperty("qAlphabetSize");
         q = project.getIntProperty("q");
      } catch (NumberFormatException ex) {
         log.error("qgramcheck: q-gram index does not seem to exist (create it!)");
         Globals.terminate(1);
      }
      final int ffc = project.getIntProperty("qFilterComplexity");
      final int ffm = project.getIntProperty("qFilterDelta");
      final QGramFilter fff = new QGramFilter(q, asize, ffc, ffm);
      final boolean bisulfiteIndex = project.isBisulfiteIndex();

      // call checking routine
      log.info("qgramcheck: checking %s... q=%d, asize=%d", project.getName(), q, asize);
      log.info("qgramcheck: filter %d:%d filters %d q-grams", ffc, ffm, fff.cardinality());
      int result = 0;
      try {
         result = checkQGramIndex(project.makeFile(FileTypes.SEQ), q, asize,
               project.makeFile(FileTypes.QBUCKETS), project.makeFile(FileTypes.QPOSITIONS), fff,
               bisulfiteIndex);
      } catch (IOException ex) {
         log.warn("qgramcheck: error on %s: %s", project.getName(), ex);
      }

      // log result and return the result
      if (result < 0)
         log.info("qgramcheck: %s is OK", project.getName());
      else
         log.info("qgramcheck: %s has an error at qpos[%d]", project.getName(), result);
      return result;
   }

   /**
    * Checks the q-gram index of the given files for correctness.
    * 
    * @param seqfile
    *           sequence file
    * @param q
    *           q-gram length
    * @param asize
    *           alphabet size
    * @param bucketfile
    *           q-bucket file
    * @param qposfile
    *           q-gram index
    * @param thefilter
    *           a QGramFilter, filtered q-grams should not appear in the index
    * @param bisulfiteIndex
    *           whether the index also contains bisulfite-treated q-grams
    * @return an index &gt;=0 where the first error in qpos is found, or -N-1&lt;0, where N is the
    *         number of q-grams in the index.
    * @throws java.io.IOException
    *            TODO 64Bit
    */
   private static int checkQGramIndex(final File seqfile, final int q, final int asize,
         final File bucketfile, final File qposfile, final QGramFilter thefilter,
         final boolean bisulfiteIndex) throws IOException {
      // Read sequence and bucketfile into arrays
      System.gc();
      TicToc timer = new TicToc();
      log.info("  reading %s and %s", seqfile, bucketfile);
      final ArrayFile arf = new ArrayFile((File) null);
      // TODO 64Bit HugeByteArray
      byte[] sequence = arf.setFile(seqfile).readArray((byte[]) null);
      // TODO 64Bit long
      int[] bck = arf.setFile(bucketfile).readArray((int[]) null);
      log.info("  reading finished after %.2f sec", timer.tocs());

      // TODO 64Bit long
      final int sequenceLength = sequence.length;
      // TODO 64Bit HugeBitArray
      final BitArray sok = new BitArray(sequenceLength - q + 1); // TODO what is sok? - sok[i] =
      // true iff
      log.info("  %s has length %d, contains %d %d-grams", seqfile, sequenceLength, sequenceLength
            - q + 1, q);

      // Initialize q-gram storage
      final QGramCoder coder;
      if (bisulfiteIndex) {
         coder = new BisulfiteQGramCoder(q);
      } else {
         coder = new QGramCoder(q, asize);
      }
      byte[] qgram = new byte[q];

      // Read the q-position array.
      // TODO 64Bit long wrong datatype??? capacity is int
      IntBuffer qpos = Globals.mapR(qposfile).asIntBuffer(); // to q-gram index ; change whole
      // method
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
         if (!coder.areCompatible(qgram, 0, sequence, i)) {
            log.info("r=%d, i=%d, expected: %s, observed: %s.", r, i, StringUtils.join("", qgram,
                  0, q), StringUtils.join("", sequence, i, q));
            return r;
         }
         assert i >= 0 && i <= sequenceLength - q;
         sok.set(i, true);
      }

      // now check that all untouched text positions in fact contain filtered or invalid q-grams
      log.info("  checking %d remaining text positions...", sequenceLength - q + 1
            - sok.cardinality());
      for (int ii = 0; ii < sequenceLength - q + 1; ii++) {
         if (sok.get(ii) == 1)
            continue;
         final int cd = coder.code(sequence, ii);
         if (cd < 0 || thefilter.get(cd) == 1)
            continue;
         log.info("  ERROR: qgram at pos %d has code %d [%s], not seen in qpos and not filtered!",
               ii, cd, StringUtils.join("", coder.qGram(cd, qgram), 0, q));
         return sequenceLength; // error: return length of the text ( > number of q-positions)
      }

      log.info("  checking finished after (total) %.2f sec", timer.tocs());
      return (-r - 1);
   }

}
