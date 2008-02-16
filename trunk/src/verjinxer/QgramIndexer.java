/*
 * QgramIndexer.java
 * Created on 30. Januar 2007, 15:15
 *
 */
package verjinxer;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Properties;
import static java.lang.Math.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.BufferUnderflowException;
import verjinxer.util.*;
import verjinxer.sequenceanalysis.*;
import static verjinxer.Globals.*;

/**
 * This class provides a q-gram indexer.
 * @author Sven Rahmann
 * @author Marcel Martin
 */
public final class QgramIndexer {

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
      g.logmsg("writes %s, %s, %s, %s.%n", extqbck, extqpos, extqfreq, extqseqfreq);
      g.logmsg("Options:%n");
      g.logmsg("  -q  <q>                 q-gram length [0=reasonable]%n");
      g.logmsg("  -b, --bisulfite         simulate bisulfite treatment%n");
      g.logmsg("  -f, --allfreq           write all frequency files (--freq, --sfreq)");
      g.logmsg("  --freq                  write q-gram frequencies (%s)%n", extqfreq);
      g.logmsg("  --seqfreq, --sfreq      write in how many sequences each qgram appears (%s)%n", extqseqfreq);
      g.logmsg("  -c, --check             additionally check index integrity%n");
      g.logmsg("  -C, --onlycheck         ONLY check index integrity%n");
      g.logmsg("  -F, --filter <cplx:occ> PERMANENTLY apply low-complexity filter%n");
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
            "q:,F=filter:,c=check,C=onlycheck,X=notexternal=nox=noexternal,b=bisulfite,freq=fr,sfreq=seqfreq=sf,f=allfreq");
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

      // Loop through all files
      for (String indexname : args) {
         String di = g.dir + indexname;
         g.startplog(di + extlog);
         String dout = g.outdir + indexname;

         // Read properties.
         // If we only check index integrity, do that and continue with next index
         // Otherwise, extend the properties and go on building the index.
         Properties prj = g.readProject(di + extprj);
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
         final QGramCoder coder = new QGramCoder(qq, asize);
         final BitSet thefilter = coder.createFilter(opt.get("F")); // returns empty filter if null
         prj.setProperty("qFilterComplexity", String.valueOf(coder.getFilterComplexity()));
         prj.setProperty("qFilterDelta", String.valueOf(coder.getFilterDelta()));

         final int qfiltered = thefilter.cardinality();
         g.logmsg("qgram: filtering %d q-grams%n", qfiltered);
         prj.setProperty("qFiltered", String.valueOf(qfiltered));
         //for (int i = thefilter.nextSetBit(0); i >= 0; i = thefilter.nextSetBit(i+1))
         //   g.logmsg("  %s%n", coder.qGramString(i,AlphabetMap.DNA()));
         //g.writeFilter(dout+extqfilter, thefilter);

         Object[] result = null;
         try {
            String freqfile = (freq ? dout + extqfreq : null);
            String sfreqfile = (sfreq ? dout + extqseqfreq : null);
            result = generateQGramIndex(di + extseq, qq, asize, separator,
                  dout + extqbck, dout + extqpos, freqfile, sfreqfile, external, thefilter, bisulfite);
         } catch (Exception e) {
            g.warnmsg("qgram: failed on %s: %s; continuing with remainder...%n", indexname, e.toString());
            g.stopplog();
            continue;
         }
         
         prj.setProperty("qfreqMax", result[4].toString());
         prj.setProperty("qbckMax", result[5].toString());
         final double[] times = (double[]) (result[3]);
         g.logmsg("qgram: time for %s: %d sec or %.2f min%n", indexname, times[0], times[0] / 60.0);

         try {
            g.writeProject(prj, di + extprj);
         } catch (IOException ex) {
            g.warnmsg("qgram: could not write %s, skipping! (%s)%n", di + extprj, ex.toString());
         }
         if (check && docheck(di, prj) >= 0) returnvalue = 1;
         g.stopplog();
      } // end for each file
      return returnvalue; // 1 if failed on any of the indices; 0 if everything ok.
   } // end run

   
   
   /**
    * Read a sequence file from disk into a ByteBuffer.
    * @param seqfile   name of the sequence file
    * @param external  whether to use memory mapping instead of reading the sequence
    * @return The sequence in a ByteBuffer
    */
   private ByteBuffer readSequenceFile(final String seqfile, boolean external) throws IOException {
      if (external)
         return g.mapRByteArray(seqfile);
      // not external: read bytes into array-backed buffer
      g.logmsg("  reading sequence file '%s'...%n", seqfile);
      TicToc timer = new TicToc();
      ByteBuffer in = new ArrayFile(seqfile, 0).readIntoNewBuffer();
      g.logmsg("  ...read %d bytes; this took %.1f sec%n", in.capacity(), timer.tocs());
      return in;
   }

   /**
    * Reads the given ByteBuffer and computes q-gram frequencies.
    * @param in The input ByteBuffer.
    * @param coder The q-gram coder.
    * @param qseqfreqfile If this is not null, an array sfrq will be written to this file.
    *           sfrq[i] == n means: q-gram i appears in n different sequences.
    * @param separator a sequence separator, only used when qseqfreqfile != null.
    * @return An array of q-gram frequencies. frq[i] == n means that q-gram with code i occurs n times.
    */
   private int[] computeFrequencies(ByteBuffer in, MultiQGramCoder coder, String qseqfreqfile, int separator) {
      final int aq = coder.numberOfQGrams();
      final int q = coder.getq();
      final int asize = coder.getAsize();
      int[] lastseq = null;              // lastseq[i] == k := q-gram i was last seen in sequence k
      int[] sfrq = null;                 // sfrq[i] == n    := q-gram i appears in n distinct sequences. 
      final int[] frq = new int[aq + 1]; // frq[i]  == n    := q-gram i appears n times; space for sentinel at the end

      final boolean doseqfreq = (qseqfreqfile != null);
      if (doseqfreq) {
         lastseq = new int[aq];
         Arrays.fill(lastseq, -1);
         sfrq = new int[aq];
      }

      int seqnum = 0;
      boolean atSequenceStart = true;
      in.position(0);
      long ll = in.limit();
      byte after;
      if (ll > 0)
         after = in.get();
      else
         after = -1;
      for (int i = 0; i < ll; i++)
         if (atSequenceStart) {
            int success;
            // No previous qcode available, scan ahead for at least q new bytes
            for (success = 0; success < q && i < ll; i++) {
               byte next = after;
               if (i < ll - 1)
                  after = in.get();
               else
                  after = -1;
               if (next < 0 || next >= asize) {
                  coder.reset();
                  success = 0;
                  if (next == separator)
                     seqnum++;
               } else {
                  coder.update(next, after);
                  success++;
               }
            }
            i--; // already incremented i beyond read position, so i--
            if (success == q)
               for (int qcode : coder.getCodes()) {
                  frq[qcode]++;
                  if (doseqfreq && lastseq[qcode] < seqnum) {
                     lastseq[qcode] = seqnum;
                     sfrq[qcode]++;
                  } // TODO is this still correct?
               }
            atSequenceStart = false;
         } else { // attempt simple update
            byte next = after;
            if (i < ll - 1)
               after = in.get();
            else
               after = -1;
            if (next == separator) {
               seqnum++;
               atSequenceStart = true;
               coder.reset();
            } else
               if (0 <= next && next < asize) {
                  coder.update(next, after); // read pos i, have q-gram at i-q+1
                  for (int qcode : coder.getCodes()) {
                     frq[qcode]++;
                     if (doseqfreq && lastseq[qcode] < seqnum) {
                        lastseq[qcode] = seqnum;
                        sfrq[qcode]++;
                     }
                  }
               } else {
                  atSequenceStart = true;
                  coder.reset();
               }
         }
      if (doseqfreq) g.dumpIntArray(qseqfreqfile, sfrq);
      sfrq = null;
      lastseq = null;  // hand over to garbage collector TODO necessary?
      return frq;
   }

   /**
    * Given q-gram frequencies, computes the necessary sizes of the buckets,
    * while taking a filter into account.
    * @param frq
    * @param external
    * @param thefilter
    * @param bucketfile
    * @return An array of size 2 containing {bck, maxbck}
    * where bck is the array of bucket positions and maxbck is the largest occurring bucket size.
    * The actual buckets are in bck[0] .. bck[bck.length-2]. The element bck[bck.length-1] is the sum
    * over all buckets.
    */
   private Object[] computeQGramBuckets(int[] frq, final boolean external, final BitSet thefilter, final String bucketfile) {
      int[] bck;
      if (external) {
         bck = frq;
         frq = null; // TODO MM What did this code do when it was within generateQGramIndex()?  
      } else
         // Java 5: bck = new int[frq.length]; System.arraycopy(frq,0,bck,0,frq.length);
         bck = Arrays.copyOf(frq, frq.length);
      int aq = bck.length - 1;

      // apply filter
      for (int c = 0; c < aq; c++)
         if (thefilter.get(c))
            bck[c] = 0; // reduce frequency to zero

      // compute bck from frq
      int sum = 0;
      int add = 0;
      int maxbck = 0; // maximum bucket size
      for (int c = 0; c < aq; c++) {
         add = bck[c];
         bck[c] = sum;
         sum += add;
         if (add > maxbck)
            maxbck = add;
      }
      bck[aq] = sum;
      if (bucketfile != null)
         g.dumpIntArray(bucketfile, bck);
      Object[] result = {bck, maxbck};
      return result;
   }

   /** generates a q-gram index of a given file.
    * @param seqfile  name of the sequence file (i.e., of the translated text)
    * @param q  q-gram length
    * @param asize  alphabet size; q-grams with characters outside 0..asize-1
    *         are not indexed
    * @param separator  the alphabet code of the sequence separator (only for seqfreq)
    * @param bucketfile filename of the q-bucket file (can be null)
    * @param qposfile  filename of the q-gram index (can be null)
    * @param qfreqfile filename of the q-gram frequency file (can be null)
    * @param qseqfreqfile  filename for q-gram sequence frequencies,
    *    i.e., in how many different sequences does the q-gram appear?
    * @param external if true, try to save memory and do more on disk
    * @param thefilter  q-gram filter in form of a BitSet; q-grams corresponding to
    *   1-bits are not considered for indexing
    * @param bisulfite whether to create an index that additionally includes bisulfite treated q-grams
    * @return array of size 6: { bck, qpos, qfreq, times, maxfreq, maxqbck }, where
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
         final int separator,
         final String bucketfile,
         final String qposfile,
         final String qfreqfile,
         final String qseqfreqfile,
         final boolean external,
         final BitSet thefilter,
         boolean bisulfite) throws IOException {
      
      final TicToc totalTimer = new TicToc();
      final ByteBuffer in = readSequenceFile(seqfile, external);
      final long ll = in.limit();

      final MultiQGramCoder coder = new MultiQGramCoder(q, asize, bisulfite);
      final int aq = coder.numberOfQGrams();
      g.logmsg("  counting %d different %d-grams...%n", aq, q);

      // Scan file once and count qgrams.
      // One file may contain multiple sequences, separated by the separator.
      final TicToc timer = new TicToc();
      int[] frq = computeFrequencies(in, coder, qseqfreqfile, separator);
      g.logmsg("  time for word counting: %.2f sec%n", timer.tocs());
      int maxfreq = ArrayUtils.maximumElement(frq);
      if (qfreqfile != null)  g.dumpIntArray(qfreqfile, frq, 0, aq);
      final double timeFreqCounting = timer.tocs();
      g.logmsg("  time for word counting and writing: %.2f sec%n", timeFreqCounting / 1000.0);

      // Compute and write bucket array
      // bck[i] == n means: positions of q-grams with code i start at index n within qpos
      timer.tic();
      Object[] r = computeQGramBuckets(frq, external, thefilter, bucketfile);
      int[] bck = (int[]) r[0];
      int maxbck = (Integer) r[1];
      if (external) frq = null;
      int sum = bck[aq];
      final double timeBckGeneration = timer.tocs();
      g.logmsg("  input file size: %d;  (filtered) qgrams in qbck: %d%n", ll, sum);
      g.logmsg("  average number of q-grams per sequence position is %.2f%n", ((double) sum) / ll);

      //if (sum==0) { Object[] ret = {bck, null, frq, times, maxfreq, maxqbck}; return ret; }

      // Determine slice size
      timer.tic();
      long free = ArrayUtils.largestAllocatable(4 * (sum + 4096) + 4);
      long memreq = 4 * sum;  // since sizeof(int)==4
      if (free < memreq)
         g.logmsg("  free memory: %d, need: %d, processing in slices!%n", free, memreq);
      long slicesize = (free / 4);  // since sizeof(int)==4
      g.logmsg("  largest needed allocatable int[] is int[%d]; found in %.1f sec%n", slicesize, timer.tocs());
      slicesize -= 4096; // save some resources
      if (slicesize > sum)
         slicesize = sum;
      if (slicesize < 1024 * 1024)
         slicesize = 1024 * 1024;
      final int[] qposslice = new int[(int) slicesize];
      //final ByteBuffer bqposslice = ByteBuffer.allocate(4*(int)slicesize);
      //final IntBuffer   qposslice = bqposslice.asIntBuffer();

      ArrayFile outfile = new ArrayFile(qposfile).openW();
      int bckstart = 0;
      while (bckstart < aq) {
         TicToc wtimer = new TicToc();
         wtimer.tic();
         int qposstart = bck[bckstart];
         int bckend;
         for (bckend = bckstart; bckend < aq && (bck[bckend + 1] - qposstart) <= slicesize; bckend++) {
         }
         int qposend = bck[bckend];
         int qpossize = qposend - qposstart;
         double percentdone = (sum == 0) ? 100.0 : 100.0 * (double) qposend / ((double) sum + 0);
         g.logmsg("  collecting qcodes [%d..%d] = qpos[%d..%d] --> %.1f%%%n",
               bckstart, bckend - 1, qposstart, qposend - 1, percentdone);
         assert ((qpossize <= slicesize && qpossize > 0) || sum == 0) : "qgram: internal consistency error";

         // read through input and collect all qgrams with  bckstart<=qcode<bckend
         boolean atSequenceStart = true;
         in.position(0);
         byte after;
         if (ll > 0)
            after = in.get();
         else
            after = -1;
         for (int i = 0; i < ll; i++)
            if (atSequenceStart) {
               // No previous qcode available, scan ahead for at least q new bytes
               int success;
               for (success = 0; success < q && i < ll; i++) {
                  byte next = after;
                  if (i < ll - 1)
                     after = in.get();
                  else
                     after = -1;
                  if (next < 0 || next >= asize) {
                     coder.reset();
                     success = 0;
                  } else {
                     coder.update(next, after);
                     success++;
                  }
               }
               i--; // already incremented i beyond read position
               if (success == q)
                  for (int qcode : coder.getCodes()) {
                     assert (qcode >= 0);
                     if (bckstart <= qcode && qcode < bckend && !thefilter.get(qcode))
                        //qposslice.put((bck[qcode]++)-qposstart,  i-q+1);
                        // the starting position of this q-gram is i-q+1
                        qposslice[(bck[qcode]++) - qposstart] = i - q + 1;
                  }
               atSequenceStart = false;
            } else {
               byte next = after;
               if (i < ll - 1)
                  after = in.get();
               else
                  after = -1;
               if (0 <= next && next < asize) {
                  coder.update(next, after);
                  for (int qcode : coder.getCodes())
                     if (qcode >= 0 && bckstart <= qcode && qcode < bckend && !thefilter.get(qcode))
                        //qposslice.put((bck[qcode]++)-qposstart,  i-q+1);
                        // the starting position of this q-gram is i-q+1
                        qposslice[(bck[qcode]++) - qposstart] = i - q + 1;
               } else {
                  atSequenceStart = true;
                  coder.reset();
               }
            }
         g.logmsg("    collecting slice took %.2f sec%n", wtimer.tocs());

         // write slice to file
         wtimer.tic();
         g.logmsg("    writing slice to %s%n", qposfile);
         //outfile.putslice(bqposslice, 4*qposstart, 4*qpossize);
         outfile.write(qposslice, 0, qpossize);
         g.logmsg("    writing slice took %.2f sec%n", wtimer.tocs());
         bckstart = bckend;
      }

      // clean up, return arrays only if not external and they fit in memory
      outfile.close();
      final double timeQpos = timer.tocMilliSeconds();
      final double timeTotal = totalTimer.tocMilliSeconds();
      final int[] qpos = (slicesize == sum && !external)? qposslice : null;
      if (external) { bck = null; frq = null; }

      double times[] = {timeTotal, timeFreqCounting, timeBckGeneration, timeQpos};
      Object[] ret = {bck, qpos, frq, times, maxfreq, maxbck};
      return ret;
   }

   /** checks the q-gram index of the given file for correctness.
    * @param seqfile name of the sequence file
    * @param q q-gram length
    * @param asize alphabet size
    * @param bucketfile filename of the q-bucket file
    * @param qposfile  filename of the q-gram index
    * @param thefilter a BitSet indicating which q-grams should be ignored
    * @return an index &gt;=0 where the first error in qpos is found, or
    *  -N-1&lt;0, where N is the number of q-grams in the index.
    * @throws java.io.IOException 
    */
   public int checkQGramIndex(final String seqfile, final int q, final int asize,
                               final String bucketfile, final String qposfile, final BitSet thefilter)
         throws IOException {

      // Initialize q-gram storage
      final QGramCoder coder = new QGramCoder(q, asize);
      byte[] qgram = new byte[q];

      // Read s and bck into arrays
      TicToc timer = new TicToc();
      System.gc();
      g.logmsg("  reading %s and %s%n", seqfile, bucketfile);
      final ArrayFile arf = new ArrayFile(null);
      byte[] s = arf.setFilename(seqfile).read((byte[]) null);
      int[] bck = arf.setFilename(bucketfile).read((int[]) null);
      g.logmsg("  reading finished after %.2f sec%n", timer.tocs());

      final int n = s.length;
      final BitSet sok = new BitSet(n - q + 1);

      // Read the q-position array.
      IntBuffer qpos = g.mapRIntArray(qposfile);
      int b = -1;
      int bold;
      int i = -1;
      int r;
      g.logmsg("  scanning %s, memory-mapped...%n", qposfile);
      for (r = 0; true; r++) {
         try {
            i = qpos.get();
         } catch (BufferUnderflowException e) {
            break;
         }
         bold = b;
         while (r == bck[b + 1])
            b++;
         if (bold != b)
            qgram = coder.qGram(b, qgram);
         for (int p = 0; p < q; p++)
            if (qgram[p] != s[i + p]) {
               g.logmsg("r=%d, i=%d, expected: %s, observed: %s.%n", r, i, StringUtils.join("", qgram, 0, q), StringUtils.join("", s, i, q));
               return (r);
            }
         assert (i >= 0 && i <= n - q);
         sok.set(i);
      }

      // now check that all untouched text positions in fact contain filtered or invalid q-grams
      g.logmsg("  checking %d remaining positions in the text...%n", n - sok.cardinality());
      for (int ii = sok.nextClearBit(0); ii < n; ii = sok.nextClearBit(ii + 1)) {
         int cd = coder.code(s, ii);
         if (cd == -1 || thefilter.get(cd))
            continue;
         g.logmsg("  ERROR: qgram at pos %d has code %d [%s], not seen in qpos and filtered!%n",
               ii, cd, StringUtils.join("", coder.qGram(cd, qgram), 0, q));
         return n; // error: return length of the text ( > number of q-positions)
      }

      g.logmsg("  checking finished after %.2f sec%n", timer.tocs());
      return (-r - 1);
   }

   public int docheck(String in, Properties prj) {
      g.cmdname = "qgramcheck";
      BitSet fff;

      // Parse prj -> q, asize
      int asize = 0;
      int q = 0;
      try {
         asize = Integer.parseInt(prj.getProperty("qAlphabetSize"));
         q = Integer.parseInt(prj.getProperty("q"));
      } catch (NumberFormatException ex) {
         g.terminate("qgramcheck: q-gram index does not seem to exist (create it!)");
      }
      int result = 0;
      g.logmsg("qgramcheck: checking %s... q=%d, asize=%d%n", in, q, asize);
      int ffc = Integer.parseInt(prj.getProperty("qFilterComplexity"));
      int ffm = Integer.parseInt(prj.getProperty("qFilterDelta"));
      fff = new QGramCoder(q, asize).createFilter(ffc, ffm);
      g.logmsg("qgramcheck: filter %d:%d filters %d q-grams%n", ffc, ffm, fff.cardinality());

      // call checking routine
      try {
         result = checkQGramIndex(in + extseq, q, asize, in + extqbck, in + extqpos, fff);
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
