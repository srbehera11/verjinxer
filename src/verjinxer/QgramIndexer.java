/*
 * QgramIndexer.java
 * Created on 30. Januar 2007, 15:15
 */
package verjinxer;

import java.util.Arrays;
import java.util.Properties;
import static java.lang.Math.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import verjinxer.util.*;
import verjinxer.sequenceanalysis.*;
import static verjinxer.Globals.*;

/**
 * This class provides a q-gram indexer.
 * @author Sven Rahmann
 * @author Marcel Martin
 */
public final class QgramIndexer implements Subcommand {

   /** only store q-grams whose positions are divisible by stride */
   private int stride = 1;
   private Globals g;

   /** Creates a new instance of QgramIndexer
    * @param gl  the Globals object to use
    */
   public QgramIndexer(Globals gl) {
      g = gl;
   }

   /**
    * print help on usage
    */
   public void help() {
      g.logmsg("Usage:%n  %s qgram [options] Indexnames...%n", programname);
      g.logmsg("Builds a q-gram index of .seq files; filters out low-complexity q-grams;%n");
      g.logmsg("writes %s, %s, %s, %s.%n", FileNameExtensions.qbuckets, FileNameExtensions.qpositions, FileNameExtensions.qfreq, FileNameExtensions.qseqfreq);
      g.logmsg("Options:%n");
      g.logmsg("  -q  <q>                 q-gram length [0=reasonable]%n");
      g.logmsg("  -s, --stride <stride>   only store q-grams whose positions are divisible by stride (default: %d)%n", stride);
      g.logmsg("  -b, --bisulfite         simulate bisulfite treatment%n");
      g.logmsg("  -f, --allfreq           write (unfiltered) frequency files (--freq, --sfreq)%n");
      g.logmsg("  --freq                  write (unfiltered) q-gram frequencies (%s)%n", FileNameExtensions.qfreq);
      g.logmsg("  --seqfreq, --sfreq      write in how many sequences each qgram appears (%s)%n", FileNameExtensions.qseqfreq);
      g.logmsg("  -c, --check             additionally check index integrity%n");
      g.logmsg("  -C, --onlycheck         ONLY check index integrity%n");
      g.logmsg("  -F, --filter <cplx:occ> PERMANENTLY apply low-complexity filter to %s%n", FileNameExtensions.qbuckets);
      g.logmsg("  -X, --notexternal       DON'T save memory at the cost of lower speed%n");
   }

   /** if run independently, call main
    * @param args  the command line arguments
    */
   public static void main(String[] args) {
      new QgramIndexer(new Globals()).run(args);
   }

   /**
    * @param args the command line arguments
    * @return zero on success, nonzero if there is a problem
    */
   public int run(String[] args) {
      g.cmdname = "qgram";
      int returnvalue = 0;
      String action = "qgram \"" + StringUtils.join("\" \"", args) + "\"";
      Options opt = new Options(
            "q:,F=filter:,c=check,C=onlycheck,X=notexternal=nox=noexternal,b=bisulfite,s=stride:,freq=fr,sfreq=seqfreq=sf,f=allfreq");
      try {
         args = opt.parse(args);
      } catch (IllegalOptionException ex) {
         g.terminate("qgram: " + ex.toString());
      }
      if (args.length == 0) {
         help();
         g.logmsg("qgram: no index given%n");
         g.terminate(0);
      }

      // Determine values of boolean options
      final boolean external  = !(opt.isGiven("X"));
      final boolean freq      =  (opt.isGiven("f") || opt.isGiven("freq"));
      final boolean sfreq     =  (opt.isGiven("f") || opt.isGiven("sfreq"));
      final boolean check     =  (opt.isGiven("c"));
      final boolean checkonly =  (opt.isGiven("C"));
      final boolean bisulfite =  (opt.isGiven("b"));

      // Determine parameter q
      final int q = (opt.isGiven("q"))? Integer.parseInt(opt.get("q")) : 0;

      stride = opt.isGiven("s") ? Integer.parseInt(opt.get("s")) : 1;
      
      g.logmsg("qgram: stride width is %d%n", stride);
      // Loop through all files
      for (String indexname : args) {
         String di = g.dir + indexname;
         g.startplog(di + FileNameExtensions.log);
         String dout = g.outdir + indexname;

         // Read properties.
         // If we only check index integrity, do that and continue with next index.
         // Otherwise, extend the properties and go on building the index.
         Properties prj = g.readProject(di + FileNameExtensions.prj);
         if (checkonly) {
            if (docheck(di, prj) >= 0) returnvalue = 1;
            continue;
         }
         final int asize = Integer.parseInt(prj.getProperty("LargestSymbol")) + 1;
         final int separator = Integer.parseInt(prj.getProperty("Separator"));
         prj.setProperty("QGramAction", action);
         prj.setProperty("LastAction", "qgram");
         prj.setProperty("Bisulfite", Boolean.toString(bisulfite));
         prj.setProperty("qAlphabetSize", Integer.toString(asize));
         prj.setProperty("Stride", Integer.toString(stride));

         // determine q-gram size qq, either from given -q option, or default by length
         int qq;
         if (q > 0) qq = q;
         else {
            double fsize = Double.parseDouble(prj.getProperty("Length"));
            qq = (int) (floor(log(fsize) / log(asize))) - 2;
            if (qq < 1) qq = 1;
         }
         prj.setProperty("q", Integer.toString(qq));

         // create the q-gram filter
         g.logmsg("qgram: processing: indexname=%s, asize=%d, q=%d%n", indexname, asize, qq);
         final int[] filterparam = QGramFilter.parseFilterParameters(opt.get("F"));
         prj.setProperty("qFilterComplexity", String.valueOf(filterparam[0]));
         prj.setProperty("qFilterDelta", String.valueOf(filterparam[1]));
         final QGramFilter thefilter = new QGramFilter(qq, asize, filterparam[0], filterparam[1]);
         final int qfiltered = thefilter.cardinality();
         g.logmsg("qgram: filtering %d q-grams%n", qfiltered);
         prj.setProperty("qFiltered", String.valueOf(qfiltered));
         //for (int i = thefilter.nextSetBit(0); i >= 0; i = thefilter.nextSetBit(i+1))
         //   g.logmsg("  %s%n", coder.qGramString(i,AlphabetMap.DNA()));

         Object[] result = null;
         try {
            final String freqfile = (freq ? dout + FileNameExtensions.qfreq : null);
            final String sfreqfile = (sfreq ? dout + FileNameExtensions.qseqfreq : null);
            result = generateQGramIndex(di + FileNameExtensions.seq, qq, asize, (byte)separator,
                  dout + FileNameExtensions.qbuckets, dout + FileNameExtensions.qpositions, freqfile, sfreqfile, external, thefilter, bisulfite);
         } catch (Exception e) {
            e.printStackTrace();
            g.warnmsg("qgram: failed on %s: %s; continuing with remainder...%n", indexname, e.toString());
            g.stopplog();
            continue;
         }
         
         prj.setProperty("qfreqMax", result[4].toString());
         prj.setProperty("qbckMax", result[5].toString());
         final double[] times = (double[]) (result[3]);
         g.logmsg("qgram: time for %s: %.1f sec or %.2f min%n", indexname, times[0], times[0] / 60.0);

         try {
            g.writeProject(prj, di + FileNameExtensions.prj);
         } catch (IOException ex) {
            g.warnmsg("qgram: could not write %s, skipping! (%s)%n", di + FileNameExtensions.prj, ex.toString());
         }
         if (check && docheck(di, prj) >= 0) returnvalue = 1;
         g.stopplog();
      } // end for each file
      return returnvalue; // 1 if failed on any of the indices; 0 if everything ok.
   }

   
   
   /**
    * Reads a sequence file from disk into a ByteBuffer.
    * @param seqfile      name of the sequence file
    * @param memoryMapped whether to use memory mapping instead of reading the sequence
    * @return             the sequence in a ByteBuffer
    */
   private ByteBuffer readSequenceFile(final String seqfile, boolean memoryMapped) throws IOException {
      if (memoryMapped)
         return g.mapR(seqfile);
      // not external: read bytes into array-backed buffer
      g.logmsg("  reading sequence file '%s'...%n", seqfile);
      TicToc timer = new TicToc();
      ByteBuffer in = new ArrayFile(seqfile, 0).readArrayIntoNewBuffer();
      g.logmsg("  ...read %d bytes; this took %.1f sec%n", in.capacity(), timer.tocs());
      return in;
   }

   
   /**
    * Reads the given ByteBuffer and computes q-gram frequencies.
    * At this stage, filters are ignored:
    * The frequencies are the orignial frequencies in the byte file, before applying a filter.
    * This implementation uses a sparse q-gram iterator. 
    * @param in    the input ByteBuffer.
    * @param coder the q-gram coder.
    * @param qseqfreqfile If this is not null, an array sfrq will be written to this file.
    *           sfrq[i] == n means: q-gram i appears in n different sequences.
    * @param separator  a sequence separator, only used when qseqfreqfile != null.
    * @return An array of q-gram frequencies. frq[i] == n means that q-gram with code i occurs n times.
    */
   public int[] computeFrequencies(
         final ByteBuffer in, final MultiQGramCoder coder, final String qseqfreqfile, final byte separator) {
      
      final TicToc timer = new TicToc();
      final int aq = coder.numberOfQGrams;
      int[] lastseq = null;              // lastseq[i] == k := q-gram i was last seen in sequence k
      int[] sfrq = null;                 // sfrq[i] == n    := q-gram i appears in n distinct sequences. 
      final int[] frq = new int[aq + 1]; // frq[i]  == n    := q-gram i appears n times; space for sentinel at the end

      final boolean doseqfreq = (qseqfreqfile != null); // true if we compute sequence-based q-gram frequencies
      if (doseqfreq) {
         lastseq = new int[aq];
         Arrays.fill(lastseq, -1);
         sfrq = new int[aq];
      }

      //g.logmsg("doseqfreq = %s, separator = %d%n", doseqfreq, separator); // DEBUG
      int seqnum=0;
      for (long pc : coder.sparseQGrams(in, doseqfreq, separator)) {
         final int qcode = (int)pc;
         final int pos   = (int)(pc>>32); // pos only necessary for assert statement, otherwise unused!
         if (qcode<0) { assert(doseqfreq); seqnum++; continue; }
         assert(qcode>=0 && qcode<frq.length) 
               : String.format("Error: qcode=%d at pos %d (%s)%n", qcode, pos, StringUtils.join("",coder.qcoder.qGram(qcode),0,coder.q)); // DEBUG
         if (pos % stride == 0) frq[qcode]++;
         if (doseqfreq && lastseq[qcode] < seqnum) {
            lastseq[qcode] = seqnum;
            if (pos % stride==0) sfrq[qcode]++;
         }
      }
      in.rewind();
      g.logmsg("  time for word counting: %.2f sec%n", timer.tocs());     
      if (doseqfreq) g.dumpIntArray(qseqfreqfile, sfrq);
      return frq;
   }
   
   
   /**
    * Computes the necessary sizes of q-gram buckets from q-gram frequencies, 
    * while taking a filter into account.
    * @param frq         the array of q-gram frequencies
    * @param overwrite   if true, overwrites frq with bck; otherwise, creates a separate array for bck
    * @param thefilter   a filter that specifies which q-grams to ignore
    * @return
    * An array of size 2 containing {bck, maxbcksize},
    * where bck is the array of bucket positions and maxbcksize is the largest occurring bucket size.
    * The actual bucket starts are in bck[0..bck.length-2]. 
    * The element bck[bck.length-1] is the sum over all buckets (that is, the number of q-grams in the index).
    */
   public Object[] computeBuckets(final int[] frq, final boolean overwrite, final QGramFilter thefilter) {
      final int[] bck = (overwrite)? frq : Arrays.copyOf(frq, frq.length);
      // Java 5: bck = new int[frq.length]; System.arraycopy(frq,0,bck,0,frq.length);
      final int aq = bck.length - 1; // subtract sentinel space

      // apply filter: at the moment, bck contains frequencies (frq), not yet bucket sizes!
      for (int c = 0; c < aq; c++) if (thefilter.isFiltered(c)) bck[c] = 0; // reduce frequency to zero

      // compute bck from frq
      int sum = 0;
      int add = 0;
      int maxbcksize = 0; // maximum bucket size
      for (int c = 0; c < aq; c++) {
         add = bck[c];
         bck[c] = sum;
         sum += add;
         if (add > maxbcksize) maxbcksize = add;
      }
      bck[aq] = sum;  // last entry (sentinel) shows how many entries in index
      final Object[] result = {bck, maxbcksize};
      return result;
   }

   /** Generates a q-gram index of a given file.
    * @param seqfile       name of the sequence file (i.e., of the translated text)
    * @param q             q-gram length
    * @param asize         alphabet size; 
    *                      q-grams with characters outside 0..asize-1 are not indexed.
    * @param separator     the alphabet code of the sequence separator (only for seqfreq)
    * @param bucketfile    filename of the q-bucket file (can be null)
    * @param qposfile      filename of the q-gram index (can be null)
    * @param qfreqfile     filename of the q-gram frequency file (can be null)
    * @param qseqfreqfile  filename for q-gram sequence frequencies,
    *                      i.e., in how many different sequences does the q-gram appear?
    * @param external      if true, keep as few arrays in memory at the same time as possible.
    *                      In particular, do not return bck, qpos, qfreq.
    * @param thefilter     a QGramFilter; filtered q-grams are not considered for indexing.
    * @param bisulfite     whether to create an index that additionally includes bisulfite treated q-grams.
    * @return  array of size 6: { bck, qpos, qfreq, times, maxfreq, maxqbck }, where
    *   int[]    bck:   array of q-bucket starts in qpos (null if external);
    *   int[]    qpos:  array of starting positions of q-grams in the text (null if external);
    *   int[]    qfreq: array of q-gram frequencies (null if external);
    *   double[] times: contains the times taken to compute the q-gram index in seconds;
    *   Integer  maxfreq:  largest frequency;
    *   Integer  maxqbck:  largest q-bucket size.
    * @throws java.io.IOException 
    * @throws java.lang.IllegalArgumentException if bisulfite is set, but asize is not 4
    */
   public Object[] generateQGramIndex(
         final String seqfile,
         final int q,
         final int asize,
         final byte separator,
         final String bucketfile,
         final String qposfile,
         final String qfreqfile,
         final String qseqfreqfile,
         final boolean external,
         final QGramFilter thefilter,
         boolean bisulfite) 
         throws IOException {
      
      final TicToc totalTimer = new TicToc();
      final ByteBuffer in = readSequenceFile(seqfile, external);
      final long ll = in.limit();

      final MultiQGramCoder coder = new MultiQGramCoder(q, asize, bisulfite);
      final int aq = coder.numberOfQGrams;
      g.logmsg("  counting %d different %d-grams...%n", aq, q);

      // Scan file once and count qgrams.
      // One file may contain multiple sequences, separated by the separator.
      // frq[i] == n means: q-gram with code i occurs n times in the input sequence.
      // This holds before applying a filter.
      final TicToc timer = new TicToc();
      int[] frq = computeFrequencies(in, coder, qseqfreqfile, separator);
      int maxfreq = ArrayUtils.maximumElement(frq);
      if (qfreqfile != null)  g.dumpIntArray(qfreqfile, frq, 0, aq);
      final double timeFreqCounting = timer.tocs();

      // Compute and write bucket array bck
      // bck[i] == r means: positions of q-grams with code i start at index r within qpos
      timer.tic();
      final Object[] r = computeBuckets(frq, external, thefilter);
      final int[] bck = (int[]) r[0];
      final int maxbck = (Integer) r[1]; // only needed to return to caller
      if (external) frq = null;          // in this case, frq has been overwritten with bck anyway
      final int sum = bck[aq];
      if (bucketfile != null)  g.dumpIntArray(bucketfile, bck);
      final double timeBckGeneration = timer.tocs();
      g.logmsg("  time for reading, word counting, and writing buckets: %.2f sec%n", totalTimer.tocs());
      g.logmsg("  input file size: %d;  (filtered) qgrams in qbck: %d%n", ll, sum);
      g.logmsg("  average number of q-grams per sequence position is %.2f%n", ((double) sum) / ll);

      //if (sum==0) { Object[] ret = {bck, null, frq, times, maxfreq, maxqbck}; return ret; }

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
         for (bckend = bckstart; bckend < aq && (bck[bckend + 1] - qposstart) <= slicesize; bckend++) { }
         final int qposend = bck[bckend];
         final int qpossize = qposend - qposstart;
         final double percentdone = (sum == 0) ? 100.0 : 100.0 * (double) qposend / ((double) sum + 0);
         g.logmsg("  collecting qcodes [%d..%d] = qpos[%d..%d] --> %.1f%%%n",
               bckstart, bckend - 1, qposstart, qposend - 1, percentdone);
         assert ((qpossize <= slicesize && qpossize > 0) || sum == 0) : "qgram: internal consistency error";

         // read through input and collect all qgrams with  bckstart<=qcode<bckend
         for (long pc : coder.sparseQGrams(in)) {
            final int pos = (int)(pc>>>32);
            if (pos % stride != 0)
               continue;            
            final int qcode = (int)(pc);
            if (bckstart <= qcode && qcode < bckend && thefilter.get(qcode)==0)
               qposslice[(bck[qcode]++) - qposstart] = pos;
         }
         g.logmsg("    collecting slice took %.2f sec%n", wtimer.tocs());

         // write slice to file
         g.logmsg("    writing slice to %s%n", qposfile);
         wtimer.tic();
         outfile.writeArray(qposslice, 0, qpossize);
         g.logmsg("    writing slice took %.2f sec%n", wtimer.tocs());
         bckstart = bckend;
      }

      // clean up, return arrays only if not external and they fit in memory
      outfile.close();
      final double timeQpos = timer.tocs();
      final double timeTotal = totalTimer.tocs();
      final int[] qpos = (slicesize == sum && !external)? qposslice : null;

      double times[] = {timeTotal, timeFreqCounting, timeBckGeneration, timeQpos};
      return new Object[] { (external? null : bck), qpos, frq, times, maxfreq, maxbck};
   }

   
   
   /** Checks the q-gram index of the given files for correctness.
    * @param seqfile     name of the sequence file
    * @param q           q-gram length
    * @param asize       alphabet size
    * @param bucketfile  filename of the q-bucket file
    * @param qposfile    filename of the q-gram index
    * @param thefilter   a QGramFilter, filtered q-grams should not appear in the index
    * @param bisulfite   whether the index also contains bisulfite-treated q-grams
    * @return an index &gt;=0 where the first error in qpos is found, or
    *  -N-1&lt;0, where N is the number of q-grams in the index.
    * @throws java.io.IOException 
    */
   public int checkQGramIndex(final String seqfile, final int q, final int asize,
         final String bucketfile, final String qposfile, final QGramFilter thefilter, final boolean bisulfite)
         throws IOException {
      // Read sequence and bucketfile into arrays
      System.gc();
      TicToc timer = new TicToc();
      g.logmsg("  reading %s and %s%n", seqfile, bucketfile);
      final ArrayFile arf = new ArrayFile(null);
      byte[] s = arf.setFilename(seqfile).readArray((byte[]) null);
      int[] bck = arf.setFilename(bucketfile).readArray((int[]) null);
      g.logmsg("  reading finished after %.2f sec%n", timer.tocs());

      final int n = s.length;
      final BitArray sok = new BitArray(n - q + 1); // TODO what is sok?
      g.logmsg("  %s has length %d, contains %d %d-grams%n", seqfile, n, n-q+1, q);

      // Initialize q-gram storage
      final MultiQGramCoder coder = new MultiQGramCoder(q, asize, bisulfite);
      final QGramCoder     qcoder = coder.qcoder;
      byte[] qgram = new byte[q];

      // Read the q-position array.
      IntBuffer qpos = g.mapR(qposfile).asIntBuffer();
      int b = -1; // current bucket
      int bold;
      int i = -1;
      g.logmsg("  scanning %s, memory-mapped...%n", qposfile);
      int r;  // can't declare r in 'for' statement, because we need it as return value!
      final int rend = qpos.capacity();
      for (r = 0; r<rend; r++) {
         i = qpos.get();
         bold = b;
         // maybe have reached a new bucket; in this case, generate the new correct q-gram
         while (r == bck[b + 1])   
            b++;
         if (bold != b) qgram = qcoder.qGram(b, qgram);
         // TODO: check whether qgram (in index) is  bis-compatible to s[i...i+q-1] (text q-gram).
         if (!coder.areCompatible(qgram, 0, s, i)) {
            g.logmsg("r=%d, i=%d, expected: %s, observed: %s.%n", 
                  r, i, StringUtils.join("", qgram, 0, q), StringUtils.join("", s, i, q));
            return r;
         }
         assert i >= 0 && i <= n - q;
         sok.set(i, true);
      }

      // now check that all untouched text positions in fact contain filtered or invalid q-grams
      g.logmsg("  checking %d remaining text positions...%n", n - q + 1 - sok.cardinality());
      for (int ii=0; ii<n-q+1; ii++) {
         if (sok.get(ii)==1) continue;
         final int cd = qcoder.code(s, ii);
         if (cd < 0 || thefilter.get(cd)==1) continue;
         g.logmsg("  ERROR: qgram at pos %d has code %d [%s], not seen in qpos and not filtered!%n",
               ii, cd, StringUtils.join("", qcoder.qGram(cd, qgram), 0, q));
         return n; // error: return length of the text ( > number of q-positions)
      }

      g.logmsg("  checking finished after (total) %.2f sec%n", timer.tocs());
      return (-r - 1);
   }

   
   
   /**
    * Check a project's q-gram index.
    * @param in   base file name of the sequence file
    * @param prj  project properties
    * @return     a negative value, if the index is ok; a positive r value indicates
    *   an index in qpos which seems to be wrong (ie, the the q-gram at qpos[r]
    *   in the text seems to disagree with the opinion of the index.
    */
   public int docheck(final String in, final Properties prj) {
      g.cmdname = "qgramcheck";
      // Parse prj -> q, asize
      int asize = 0;
      int q = 0;
      try {
         asize = Integer.parseInt(prj.getProperty("qAlphabetSize"));
         q = Integer.parseInt(prj.getProperty("q"));
      } catch (NumberFormatException ex) {
         g.terminate("qgramcheck: q-gram index does not seem to exist (create it!)");
      }
      final int ffc = Integer.parseInt(prj.getProperty("qFilterComplexity"));
      final int ffm = Integer.parseInt(prj.getProperty("qFilterDelta"));
      final QGramFilter fff = new QGramFilter(q, asize, ffc, ffm);
      final boolean bisulfite = Boolean.parseBoolean(prj.getProperty("Bisulfite"));

      // call checking routine
      g.logmsg("qgramcheck: checking %s... q=%d, asize=%d%n", in, q, asize);
      g.logmsg("qgramcheck: filter %d:%d filters %d q-grams%n", ffc, ffm, fff.cardinality());
      int result = 0;
      try {
         result = checkQGramIndex(in + FileNameExtensions.seq, q, asize, in + FileNameExtensions.qbuckets, in + FileNameExtensions.qpositions, fff, bisulfite);
      } catch (IOException ex) {
         g.warnmsg("qgramcheck: error on %s: %s%n", in, ex.toString());
      }

      // log result and return the result
      if (result < 0)
         g.logmsg("qgramcheck: %s is OK%n", in);
      else
         g.logmsg("qgramcheck: %s has an error at qpos[%d]%n", in, result);
      return result;
   }
}
