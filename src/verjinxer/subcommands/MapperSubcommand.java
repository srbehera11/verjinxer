/*
 * Mapper.java 
 * Created on May 14, 2007, 9:08 AM 
 * TODO: qgramatonce, suffix, suffixatonce 
 * TODO: pvalues and Evalues 
 * TODO / BROKEN: Especially the alignment code needs review! 
 * TODO: adapt to 64-bit files
 */

package verjinxer.subcommands;

import static verjinxer.Globals.programname;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Locale;

import com.spinn3r.log5j.Logger;

import verjinxer.FileNameExtensions;
import verjinxer.Globals;
import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.sequenceanalysis.QGramCoder;
import verjinxer.sequenceanalysis.QGramFilter;
import verjinxer.util.ArrayFile;
import verjinxer.util.ArrayUtils;
import verjinxer.util.BitArray;
import verjinxer.util.IllegalOptionException;
import verjinxer.util.Options;
import verjinxer.util.ProjectInfo;
import verjinxer.util.StringUtils;
import verjinxer.util.TicToc;

/**
 * This class contains routines for mapping sequences to an index.
 * 
 * FIXME This class is not usable at the moment.
 * 
 * @author Sven Rahmann
 */
public class MapperSubcommand implements Subcommand {
   private static final Logger log = Globals.getLogger();
   private Globals g;

   /**
    * Creates a new instance of Mapper
    * 
    * @param gl
    *           the globals object to be used (contains information about logging streams, etc)
    */
   public MapperSubcommand(Globals gl) {
      g = gl;
   }

   /**
    * print help on usage
    */
   public void help() {
      log.info("Usage:");
      log.info("%n  %s map  [options]  <sequences>  <[@]index...> ", programname);
      log.info("Reports all occurrences of the sequences in the indices");
      log.info("in human-readable output format. One or more indices can be given");
      log.info("either by filename or in a file of filenames when preceded by @.");
      log.info("Writes .mapped and .nonmappable.");
      log.info("Options:");
      log.info("  -r, --rc                also map reverse complements (DNA only!)");
      log.info("  -e, --errorlevel <e>    error level for mapping [0.03]");
      log.info("  -c, --clip <num>        clip num characters at each end [0]");
      log.info("  -R, --repeat <T>        specify #blocks repeat threshold T [+inf]");
      log.info("  -Q, --qcomplexity #|<T> pre-filter sequences by q-vocabulary [0.0]");
      log.info("  -s, --select #|<file>   select previously unmapped (#) or specified sequences");
      log.info("  -o, --out <filename>    specify output file (use # for stdout)");
      log.info("  -m, --method <method>   one of 'qgram' (default), 'suffix', 'full'");
      log.info("  --atonce                match against all indices at once (high memory!)");
      log.info("QGRAM method options:");
      log.info("  -b, --blocksize <b>     blocksize for q-gram mapping [1024]");
      log.info("  -f, --filter <c:d>      apply q-gram filter <complexity:delta>");
   }

   /**
    * if run independently, call main
    * 
    * @param args
    *           the command line arguments
    */
   public static void main(String[] args) {
      new MapperSubcommand(new Globals()).run(args);
   }

   // Variables
   boolean atonce = false;
   boolean revcomp = false;
   double errorlevel = 0.03;
   int clip = 0;
   int repeatthreshold = Integer.MAX_VALUE;
   double qgramthreshold = 0.0;
   boolean qgramtabulate = false;

   String tname = null;
   ProjectInfo tproject;
   BitArray tselect = null;
   BitArray trepeat = null;
   BitArray tmapped = null;
   int tm = 0; // number of sequences
   byte[] tall = null; // the whole text
   // byte[] t = null; // the text (coded)
   long tn = 0; // length of t
   long[] tssp = null; // sequence separator positions in text t
   ArrayList<String> tdesc = null; // sequence descriptions of t
   long longestsequence = 0;
   long shortestsequence = 0;
   int asize = 0; // alphabet size
   Alphabet alphabet = null; // the alphabet map

   ArrayList<String> indices = null;
   long longestindexlen = 0; // length of longest index text
   long totalindexlen = 0; // total length of all index texts
   long longestpos = 0; // length of longest [q]pos
   String filterstring = null;

   PrintWriter allout = null;

   enum Method {
      QGRAM, SUFFIX, FULL
   }

   Method method;

   /**
    * @param args
    *           the command line arguments
    * @return zero on success, nonzero if there is a problem
    */
   public int run(String[] args) {
      g.cmdname = "map";
      String dt;

      Options opt = new Options(
            "m=method:,e=error=errorlevel:,b=blocksize:,r=rc=revcomp,s=select:,R=repeat=repeatthreshold:,Q=qcomplexity:,c=clip:,o=out:");
      try {
         args = opt.parse(args);
      } catch (IllegalOptionException ex) {
         help();
         log.error("map: " + ex);
         return 1;
      }

      if (args.length < 2) {
         help();
         log.error("map: both a sequence file and an index must be specified.");
         return 1;
      }
      tname = args[0];
      dt = g.dir + tname;
      g.startProjectLogging(dt);

      // Determine method
      method = null;
      String mext = null;
      if (opt.isGiven("m")) {
         if (opt.get("m").startsWith("q")) {
            method = Method.QGRAM;
            mext = FileNameExtensions.qpositions;
         } else if (opt.get("m").startsWith("s")) {
            method = Method.SUFFIX;
            mext = FileNameExtensions.pos;
         } else if (opt.get("m").startsWith("f")) {
            method = Method.FULL;
            mext = FileNameExtensions.seq;
         } else {
            help();
            log.error("map: Unknown method '%s'.", opt.get("m"));
            return 1;
         }
      } else {
         method = Method.QGRAM;
         mext = FileNameExtensions.qpositions;
      }

      // Determine options
      int blocksize = 1024;
      if (opt.isGiven("e"))
         errorlevel = Double.parseDouble(opt.get("e"));
      if (errorlevel < 0.0 || errorlevel > 1.0) {
         log.error("map: Illegal error level specified.");
         return 1;
      }
      if (opt.isGiven("c"))
         clip = Integer.parseInt(opt.get("c"));
      if (clip < 0) {
         log.error("map: Illegal number of characters to clip specified.");
         return 1;
      }
      if (opt.isGiven("b")) {
         blocksize = Integer.parseInt(opt.get("b"));
         if (blocksize < 1) {
            log.error("map: Illegal blocksize specified.");
            return 1;
         }
      }
      if (opt.isGiven("R"))
         repeatthreshold = Integer.parseInt(opt.get("R"));
      if (repeatthreshold < 0) {
         log.error("map: Illegal repeat threshold specified.");
         return 1;
      }
      if (opt.isGiven("Q")) {
         if (opt.get("Q").startsWith("#"))
            qgramtabulate = true;
         else
            qgramthreshold = Double.parseDouble(opt.get("Q"));
      }
      if (qgramthreshold < 0 || qgramthreshold > 1) {
         log.error("map: Illegal q-gram complexity threshold, must be in [0..1].");
         return 1;
      }
      revcomp = opt.isGiven("r");
      atonce = opt.isGiven("atonce");
      filterstring = opt.get("f");
      if (filterstring == null)
         filterstring = "0:0";

      // Read sequence project
      try {
         tproject = ProjectInfo.createFromFile(dt);
      } catch (IOException ex) {
         log.error("could not read project file: %s", ex);
         return 1;
      }
      tm = tproject.getIntProperty("NumberSequences");

      // Select sequences
      if (opt.isGiven("s")) {
         if (opt.get("s").startsWith("#")) {
            tselect = g.slurpBitArray(dt + FileNameExtensions.select);
         } else {
            tselect = g.slurpBitArray(g.dir + opt.get("s"));
         }
      } else { // select all
         tselect = new BitArray(tm);
         for (int bbb = 0; bbb < tm; bbb++)
            tselect.set(bbb, 1);
      }
      // initialize the other bit arrays
      trepeat = new BitArray(tm);
      tmapped = new BitArray(tm);

      // Get index names and check indices
      int inum = args.length - 1;
      indices = new ArrayList<String>(inum);
      try {
         for (int i = 0; i < inum; i++) {
            String ix = args[i + 1];
            if (ix.startsWith("@")) {
               BufferedReader br = new BufferedReader(new FileReader(new File(g.dir
                     + ix.substring(1))));
               String line;
               while ((line = br.readLine()) != null) {
                  int colon = line.indexOf(':');
                  if (colon == -1)
                     indices.add(line);
                  else
                     indices.add(line.substring(0, colon));
               }
               br.close();
            } else {
               indices.add(g.dir + ix);
            }
         }
      } catch (IOException ex) {
         log.error("map: could not read indices; " + ex);
         return 1;
      }
      inum = indices.size();
      for (int i = 0; i < inum; i++) {
         String fin = indices.get(i) + mext;
         try {
            final ArrayFile fi = new ArrayFile(fin, 0).openR().close();
            long filen = fi.length();
            if (filen % 4 == 0) {
               if (filen / 4 > longestpos)
                  longestpos = filen / 4;
            }
         } catch (IOException ex) {
            log.error("map: could not open index '%s'; %s.", fin, ex);
            return 1;
         }
         fin = indices.get(i) + FileNameExtensions.seq;
         try {
            final ArrayFile fi = new ArrayFile(fin, 0).openR().close();
            long filen = fi.length();
            totalindexlen += filen;
            if (filen > longestindexlen)
               longestindexlen = filen;
         } catch (IOException ex) {
            log.error(String.format("map: could not open index '%s'; %s.", fin, ex));
            return 1;
         }
      } // end for index i

      // read information about sequences
      asize = tproject.getIntProperty("LargestSymbol") + 1;
      if (revcomp && asize != 4) {
         log.error("map: can only use reverse complement option with DNA sequences. Stop.");
         return 1;
      }
      alphabet = g.readAlphabet(g.dir + tname + FileNameExtensions.alphabet);
      String tsspfile = dt + FileNameExtensions.ssp;
      tssp = g.slurpLongArray(tsspfile);
      assert (tm == tssp.length);
      tdesc = g.slurpTextFile(dt + FileNameExtensions.desc, tm);
      longestsequence = tproject.getLongProperty("LongestSequence");
      shortestsequence = tproject.getLongProperty("ShortestSequence");
      tall = g.slurpByteArray(dt + FileNameExtensions.seq);

      int selected = tselect.cardinality();
      log.info("map: comparing %d/%d sequences against %d indices (%s) using method %s%s...",
            selected, tm, inum, StringUtils.join(", ", indices.toArray(new String[0])),
            method.toString(), (atonce ? " (at-once)" : ""));
      // log.info("map: shortest=%d, longest=%d", shortestsequence, longestsequence);

      if (selected == 0) {
         log.error("map: no sequences selected; nothing to do; stop.");
         return 0;
      }

      if (qgramthreshold > 0 || qgramtabulate)
         filtersequences();

      // open output file
      boolean closeout = true;
      try {
         if (opt.isGiven("o")) {
            if (opt.get("o").startsWith("#")) {
               allout = new PrintWriter(System.out);
               closeout = false;
            } else {
               allout = new PrintWriter(g.outdir + opt.get("o") + ".allmapped");
            }
         } else
            allout = new PrintWriter(g.outdir + tname + ".allmapped");
      } catch (IOException ex) {
         log.error("map: could not create output file; %s", ex);
         return 1;
      }

      // run chosen method
      if (method == Method.QGRAM && !atonce)
         mapByQGram(blocksize);
      else if (method == Method.FULL && atonce)
         mapByAlignmentAtOnce();
      else
         throw new UnsupportedOperationException(
               "The requested method has not yet been implemented!");

      // clean up and report
      allout.flush();
      log.info(
            "map: successfully mapped %d / %d selected (%d total) sequences;%n     found %d repeats, %d sequences remain.",
            tmapped.cardinality(), selected, tm, trepeat.cardinality(), tselect.cardinality());
      g.dumpBitArray(dt + FileNameExtensions.select, tselect);
      g.dumpBitArray(dt + ".repeat-filter", trepeat);
      g.stopplog();
      if (closeout)
         allout.close();
      return 0;
   }

   // ======================== general index data ==============================

   String iname = null; // current index name
   ProjectInfo iproject; // current index project data
   int ilength = 0; // current index text length
   byte[] itext = null; // current index text
   int[] Dcol = null;
   byte[] rcj = null;
   int enddelta = -1;

   /** keep track of best hits' error */
   int[] seqbesterror = null;

   /** number of hits with best error */
   int[] seqbesthits = null;

   /** number of all hits */
   int[] seqallhits = null;

   // ======================== q-gram based methods ==============================

   /** current qbck array */
   int[] iqbck = null;

   /** current qpos array */
   int[] iqpos = null;

   /** current qgram filter */
   QGramFilter ifilter = null;
   byte[] qgram = null;

   /** E-value table */
   double[] etable = null;

   /**
    * Proceed index-by-index, then sequence-by-sequence. Count the number of common q-grams of each
    * sequence and each index block.
    */
   void mapByQGram(final int blocksize) {
      int oldq = -1;
      log.info("map: allocating memory");
      rcj = new byte[(int) longestsequence + 1]; // +1 for separator
      Dcol = new int[(int) longestsequence + 1]; // +1 for separator
      int inum = indices.size();
      iqpos = new int[(int) longestpos];
      itext = new byte[(int) longestindexlen];

      final int blow = (int) java.lang.Math.floor(-(longestsequence + 1.0) / blocksize);
      assert (blow < 0);
      final int bhi = (int) java.lang.Math.floor((double) longestindexlen / blocksize);
      assert (bhi >= 0);
      seqbesterror = new int[tm]; // keep track of best hits' error
      seqbesthits = new int[tm]; // number of hits with best error
      seqallhits = new int[tm]; // number of all hits
      java.util.Arrays.fill(seqbesterror, (int) longestsequence + 2);
      log.info("map: allocating %d parallelogram counters, width=%d", bhi - blow + 1, blocksize);
      final int[] bcounter = new int[bhi - blow + 1];
      QGramCoder coder = null;

      for (int idx = 0; idx < inum; idx++) {
         // load idx-th q-gram index
         iname = indices.get(idx);
         try {
            iproject = ProjectInfo.createFromFile(iname);
         } catch (IOException ex) {
            log.error("could not read project file: %s", ex);
            g.terminate(1);
         }
         int iq = iproject.getIntProperty("q");
         log.info("map: processing index '%s', q=%d, filter=%s...", iname, iq, filterstring);
         int as = iproject.getIntProperty("qAlphabetSize");
         assert (asize == as);
         if (iq != oldq) {
            coder = new QGramCoder(iq, asize);
            qgram = new byte[iq];
            etable = computeEValues(longestsequence, iq, totalindexlen, blocksize,
                  1 / (asize - 1.0));
            oldq = iq;
         }
         iqbck = g.slurpIntArray(iname + FileNameExtensions.qbuckets, iqbck); // overwrite if
         // possible
         iqpos = g.slurpIntArray(iname + FileNameExtensions.qpositions, iqpos); // overwrite
         itext = g.slurpByteArray(iname + FileNameExtensions.seq, 0, -1, itext); // overwrite
         ilength = iproject.getIntProperty("Length");
         ifilter = new QGramFilter(coder.q, coder.asize, filterstring);

         for (int j = 0; j < tm; j++) {
            if (tselect.get(j) == 0 || trepeat.get(j) == 1)
               continue; // skip if not selected, or if repeat
            int jstart = (j == 0 ? 0 : (int) (tssp[j - 1] + 1)); // first character
            int jstop = (int) (tssp[j] + 1); // one behind last character = [separatorpos. +1]
            final int jlength = jstop - jstart; // length including separator
            processQGramBlocks(idx, coder, tall, jstart, jlength, j, 1, clip, bcounter, blocksize,
                  blow);
            if (revcomp && trepeat.get(j) == 0) {
               ArrayUtils.revcompArray(tall, jstart, jstop - 1, (byte) 4, rcj);
               rcj[jlength - 1] = -1;
               // log.info("+: %s",Strings.join("",tall,jstart,jlength));
               // log.info("-: %s",Strings.join("",rcj,0,jlength));
               processQGramBlocks(idx, coder, rcj, 0, jlength, j, -1, clip, bcounter, blocksize,
                     blow);
            }
            if (seqallhits[j] > 0)
               tmapped.set(j, true);
         } // end for j
         allout.flush();
         g.dumpIntArray(String.format("%s%s.%d.mapbesterror", g.outdir, tname, idx), seqbesterror);
         g.dumpIntArray(String.format("%s%s.%d.mapbesthits", g.outdir, tname, idx), seqbesthits);
         g.dumpIntArray(String.format("%s%s.%d.mapallhits", g.outdir, tname, idx), seqallhits);
      } // end for i
      // at the very end, de-select all mapped sequences and all repeats!
      for (int j = 0; j < tm; j++)
         if (tmapped.get(j) == 1 || trepeat.get(j) == 1)
            tselect.set(j, false);
   } // end mapByQGram

   /**
    * compute the q-gram block counts for txt[jstart..jstart+jlength-1].
    */
   final void processQGramBlocks(final int currenti, final QGramCoder coder, final byte[] txt,
         final int jstart, final int jlength, final int currentj, final int currentdir,
         final int trim, final int[] bcounter, final int blocksize, final int blow) {
      final int q = coder.q;
      int qcode = -1;
      int success;
      final int maxqgrams = jlength - 1 - q + 1 - 2 * trim;
      final int overlap = (int) (java.lang.Math.ceil(jlength * errorlevel));
      int subtract = (int) (java.lang.Math.ceil(jlength * errorlevel * q));
      if (q * seqbesterror[currentj] < subtract)
         subtract = q * seqbesterror[currentj];
      final int bthresh = maxqgrams - subtract;
      // 1. block counters are zero at this point.
      // 2. iterate through all q-grams of txt[0..jlength-1], increase block counters, count
      // high-scoring blocks
      final int idelta = -jstart - q + 1;
      final int jstop = jstart + jlength - 1 - trim; // points to behind last character to examine
      for (int i = jstart + trim; i < jstop; i++) {
         if (qcode >= 0) // attempt simple update
         {
            qcode = coder.codeUpdate(qcode, txt[i]);
            if (qcode >= 0 && !ifilter.isFiltered(qcode))
               incrementBCounters(i + idelta, qcode, overlap, bcounter, blocksize, blow);
            continue;
         }
         // No previous qcode available, scan ahead for at least q new bytes
         for (success = 0; success < q && i < jstop; i++) {
            final byte next = txt[i];
            if (next < 0 || next >= asize)
               success = 0;
            else
               qgram[success++] = next;
         }
         i--; // already incremented i beyond read position
         if (success == q) {
            qcode = coder.code(qgram);
            assert (qcode >= 0);
            if (!ifilter.isFiltered(qcode))
               incrementBCounters(i + idelta, qcode, overlap, bcounter, blocksize, blow);
         }
      } // end for i

      // 3. check high-scoring blocks in processQGramBlocks()
      int blockhits = 0, currenthits = 0;
      for (int b = 0; b < bcounter.length; b++)
         if (bcounter[b] >= bthresh)
            blockhits++;
      if (blockhits >= repeatthreshold) { // many block hits, this IS a repeat, probably
         log.info("map: sequence %d hits %d blocks, probably a repeat", currentj, blockhits);
         trepeat.set(currentj, true);
      } else { // this IS NOT a repeat, probably
         for (int b = 0; b < bcounter.length; b++) {
            if (bcounter[b] >= bthresh) {
               final int len = jlength - 1 - 2 * trim;
               final int bstart = (b + blow > 0) ? (b + blow) * blocksize : 0;
               int bend = bstart + blocksize + jlength + overlap + 1;
               if (bend > ilength)
                  bend = ilength;
               int endpos = align(txt, jstart + trim, len, itext, bstart, bend, overlap, Dcol); // also
               // have
               // enddelta!
               if (endpos >= 0) {
                  endpos += trim;
                  seqallhits[currentj]++;
                  currenthits++;
                  if (enddelta < seqbesterror[currentj]) {
                     seqbesterror[currentj] = enddelta;
                     seqbesthits[currentj] = 1;
                  } else if (enddelta == seqbesterror[currentj]) {
                     seqbesthits[currentj]++;
                  }
                  // log.info("%d%+d: [%s] %d %d", currentj, currentdir,
                  // Strings.join("",itext,endpos+1-enddelta-jlength+1, jlength-1+enddelta), endpos,
                  // enddelta);
                  // log.info("%d%+d: [%s] %d %d", currentj, currentdir,
                  // Strings.join("",itext,endpos+1-jlength+1, jlength-1), endpos, enddelta);
                  // log.info(" > [%s]", Strings.join("",txt,jstart,jlength));
                  allout.printf("%d %d  %d %d %d   %d %d %d%n", currentj, currentdir, currenti,
                        endpos, enddelta, bthresh, bcounter[b], maxqgrams);
               }
            }
         }
      } // end else (this is not a repeat)
      java.util.Arrays.fill(bcounter, 0);
      if (blockhits > 0)
         log.info(
               " seq %d%c: %d/%d hits/blocks this time; besterror=%d w. %d best (%d total) hits",
               currentj, currentdir > 0 ? '+' : '-', currenthits, blockhits,
               seqbesterror[currentj], seqbesthits[currentj], seqallhits[currentj]);
   }

   final void incrementBCounters(final int p, final int qcode, final int overlap,
         final int[] bcounter, final int blocksize, final int blow) {
      final int bckend = iqbck[qcode + 1];
      final int[] pos = iqpos;
      int d, b;
      for (int r = iqbck[qcode]; r < bckend; r++) {
         d = pos[r] - p;
         b = d / blocksize - blow;
         ++bcounter[b];
         if ((d % blocksize < overlap) && (b > 0))
            ++bcounter[b - 1];
      }
   }

   // =============== MAP BY ALIGNMENTS =================================================

   final void mapByAlignmentAtOnce() {
      this.iname = null;
      log.info("map: allocating memory for full alignment");
      rcj = new byte[(int) longestsequence + 1]; // +1 for separator
      Dcol = new int[(int) longestsequence + 1]; // +1 for separator
      int inum = indices.size();
      // itext = new byte[(int)longestindexlen];

      seqbesterror = new int[tm]; // keep track of best hits' error
      seqbesthits = new int[tm]; // number of hits with best error
      seqallhits = new int[tm]; // number of all hits
      java.util.Arrays.fill(seqbesterror, (int) longestsequence + 2);

      String[] iname = new String[inum];
      ProjectInfo[] iprj = new ProjectInfo[inum];
      byte[][] itext = new byte[inum][];
      int[] ilength = new int[inum];

      for (int idx = 0; idx < inum; idx++) {
         // load idx-th q-gram index
         iname[idx] = indices.get(idx);
         try {
            iprj[idx] = ProjectInfo.createFromFile(iname[idx]);
         } catch (IOException ex) {
            log.error("could not read project file %s: %s", iname[idx], ex);
            g.terminate(1);
         }
         log.info("map: processing index '%s', reading .seq", iname[idx]);
         int as = iprj[idx].getIntProperty("LargestSymbol") + 1;
         assert asize == as;
         itext[idx] = g.slurpByteArray(iname[idx] + FileNameExtensions.seq, 0, -1, null); // overwrite
         ilength[idx] = iprj[idx].getIntProperty("Length");
         assert itext[idx].length == ilength[idx];
      }
      for (int j = 0; j < tm; j++) {
         if (tselect.get(j) == 0 || trepeat.get(j) == 1)
            continue; // skip if not selected, or if repeat
         final int jstart = (j == 0 ? 0 : (int) (tssp[j - 1] + 1)); // first character
         final int jstop = (int) (tssp[j] + 1); // one behind last character = [separatorpos. +1]
         final int jlength = jstop - jstart; // length including separator
         for (int idx = 0; idx < inum; idx++) {
            log.info("  aligning seq %d against idx %d %s", j, idx, iname[idx]);
            doTheAlignment(idx, itext[idx], tall, jstart, jlength - 1, j, 1, clip);
            if (revcomp && trepeat.get(j) == 0) {
               ArrayUtils.revcompArray(tall, jstart, jstop - 1, (byte) 4, rcj);
               rcj[jlength - 1] = -1;
               doTheAlignment(idx, itext[idx], rcj, 0, jlength - 1, j, -1, clip);
            }
         }
         if (seqallhits[j] > 0)
            tmapped.set(j, true);
         if (tmapped.get(j) == 1 || trepeat.get(j) == 1)
            tselect.set(j, false);
      } // end for j
      g.dumpIntArray(String.format("%s%s.mapbesterror", g.outdir, tname), seqbesterror);
      g.dumpIntArray(String.format("%s%s.mapbesthits", g.outdir, tname), seqbesthits);
      g.dumpIntArray(String.format("%s%s.mapallhits", g.outdir, tname), seqallhits);
   } // end mapByAlignment

   private final int doTheAlignment(final int currenti, final byte[] itext, final byte[] txt,
         final int jstart, final int jlength, final int currentj, final int currentdir,
         final int trim) {
      final int len = jlength - 2 * trim;
      final int tol = (int) java.lang.Math.ceil(jlength * errorlevel);
      log.debug(" tol=%d", tol);
      int endpos = align(txt, jstart + trim, len, itext, 0, itext.length, tol, Dcol); // also have
      // enddelta!
      if (endpos >= 0) {
         endpos += trim;
         seqallhits[currentj]++;
         if (enddelta < seqbesterror[currentj]) {
            seqbesterror[currentj] = enddelta;
            seqbesthits[currentj] = 1;
         } else if (enddelta == seqbesterror[currentj]) {
            seqbesthits[currentj]++;
         }
         allout.printf("%d %d  %d %d %d%n", currentj, currentdir, currenti, endpos, enddelta);
      }
      return endpos;
   }

   // =============== ALIGNMENTS =================================================

   /**
    * align text with parallelogram block #b. Requires blocksize, blow, itext[], asize
    * 
    * @param txt
    *           the text array
    * @param start
    *           start position in the text
    * @param len
    *           length of the text (part) to align with a block
    * @param bstart
    *           where to start alignming in the index
    * @param bend
    *           where to end aligning in the index (exclusive)
    * @param giventol
    *           absolute error tolerance
    * @param storage
    *           work storage area, must have length >=len
    * @return position where leftmost best match ends in index, -1 if none.
    */
   final int align(final byte[] txt, final int start, final int len, final byte[] index,
         final int bstart, final int bend, final int giventol, final int[] storage) {
      final int tol = giventol < len ? giventol : len;
      int bestpos = -1;
      int bestd = 2 * (len + 1); // as good as infinity
      final int as = asize;

      for (int k = 0; k < tol; k++)
         storage[k] = k + 1;
      int lei = tol - 1; // initial last essential index
      // int newlei;
      int dup, dul, diag, dmin;
      byte tk;
      for (int c = bstart; c < bend; c++) {
         dup = dul = dmin = 0;
         // newlei = lei+1; if(newlei>=len) newlei=len-1;
         for (int k = 0; k <= lei; k++) {
            tk = txt[start + k];
            dmin = 1 + ((dup < storage[k]) ? dup : storage[k]); // left or up
            diag = dul + ((index[c] != tk || tk < 0 || tk >= as) ? 1 : 0);
            if (diag < dmin)
               dmin = diag;
            dul = storage[k];
            storage[k] = dup = dmin;
         }
         // lei+1 could go diagonally
         if (lei + 1 < len) {
            final int k = ++lei;
            tk = txt[start + k];
            dmin = 1 + dup;
            diag = dul + ((index[c] != tk || tk < 0 || tk >= as) ? 1 : 0);
            if (diag < dmin)
               dmin = diag;
            storage[k] = dup = dmin;
         }
         // dmin now contains storage[lei]
         if (dmin > tol) {
            while (lei >= 0 && storage[lei] > tol)
               lei--;
            dmin = (lei >= 0 ? storage[lei] : 0);
         } else {
            while (dmin < tol && lei < len - 1) {
               storage[++lei] = ++dmin;
            }
         }
         // lei=newlei;
         assert ((lei == -1 && dmin == 0) || dmin == storage[lei]);
         if (lei == len - 1 && dmin < bestd) {
            bestd = dmin;
            bestpos = c;
         }
      }
      enddelta = bestd; // dirty coding for:: return bestd;
      return bestpos;
   }

   /**
    * FULLY align text with parallelogram block #b. Requires blocksize, blow, itext[], asize
    * 
    * @param txt
    *           the text array
    * @param start
    *           start position in the text
    * @param len
    *           length of the text (part) to align with a block
    * @param bstart
    *           where to start alignming in the index
    * @param bend
    *           where to end aligning in the index (exclusive)
    * @param giventol
    *           absolute error tolerance
    * @param storage
    *           work storage area, must have length >=len
    * @return position where leftmost best match ends in index, -1 if none.
    */
   final int fullalign(final byte[] txt, final int start, final int len, final int bstart,
         final int bend, final int giventol, final int[] storage) {
      // final int tol = giventol<len? giventol : len;
      int bestpos = -1;
      int bestd = 2 * (len + 1); // as good as infinity
      final byte[] index = itext;
      final int as = asize;

      for (int k = 0; k < len; k++)
         storage[k] = k + 1;
      int dup, dul, diag, dmin;
      byte tk;
      for (int c = bstart; c < bend; c++) {
         dup = dul = dmin = 0;
         for (int k = 0; k < len; k++) {
            tk = txt[start + k];
            dmin = 1 + ((dup < storage[k]) ? dup : storage[k]); // left or up
            diag = dul + ((index[c] != tk || tk < 0 || tk >= as) ? 1 : 0);
            if (diag < dmin)
               dmin = diag;
            dul = storage[k];
            storage[k] = dup = dmin;
         }
         // dmin now contains storage[len-1]
         if (dmin < bestd) {
            bestd = dmin;
            bestpos = c;
         }
      }
      enddelta = bestd; // dirty coding for:: return bestd;
      return bestpos;
   }

   // ==================== helpers ===========================================

   /**
    * Filter sequences according to contained number of distinct q-grams. Needs asize, tall[],
    * qgram[], ...
    */
   final void filtersequences() {
      PrintWriter out = null;
      final int as = asize;
      TicToc timer = new TicToc();
      // set q-gram length q and other parameters
      int qq = 4 + (int) (java.lang.Math.ceil(java.lang.Math.log(longestsequence)
            / java.lang.Math.log(asize - 1)));
      if (qgramtabulate)
         log.info("map: tabulating %d-gram complexity of all reads", qq);
      else
         log.info("map: pre-filtering reads with %d-gram complexity < %f", qq, qgramthreshold);
      QGramCoder coder = new QGramCoder(qq, as);
      qgram = new byte[qq];
      int qcode;
      int jstart, jstop, jlength, success;
      byte next;
      BitArray seen = new BitArray(coder.numberOfQGrams);
      int maxqgrams, goodqgrams, seenqgrams;
      double goodfrac, seenfrac;

      // out has not yet been opened; open it for tabulating.
      if (qgramtabulate) {
         try {
            out = new PrintWriter(String.format("%s%s.%d.qcomplexity", g.outdir, tname, qq));
         } catch (FileNotFoundException ex) {
            log.error("map: could not tabulate q-gram complexities; " + ex);
            g.terminate(1);
         }
      }

      // sift through all sequences
      for (int j = 0; j < tm; j++) {
         jstart = (j == 0 ? 0 : (int) (tssp[j - 1] + 1));
         jstop = (int) tssp[j];
         jlength = jstop - jstart;
         maxqgrams = jlength - qq + 1;
         goodqgrams = seenqgrams = 0;
         seen.clear();
         qcode = -1;
         // iterate through all q-grams of txt[0..jlength-1], increase block counters, count
         // high-scoring blocks
         for (int i = jstart; i < jstop; i++) {
            if (qcode >= 0) // attempt simple update
            {
               qcode = coder.codeUpdate(qcode, tall[i]);
               if (qcode >= 0) {
                  goodqgrams++;
                  if (seen.get(qcode) == 0) {
                     seenqgrams++;
                     seen.set(qcode, true);
                  }
               }
               continue;
            }
            // No previous qcode available, scan ahead for at least q new bytes
            for (success = 0; success < qq && i < jstop; i++) {
               next = tall[i];
               if (next < 0 || next >= as)
                  success = 0;
               else
                  qgram[success++] = next;
            }
            i--; // already incremented i beyond read position
            if (success == qq) {
               qcode = coder.code(qgram);
               assert (qcode >= 0);
               goodqgrams++;
               if (seen.get(qcode) == 0) {
                  seenqgrams++;
                  seen.set(qcode, true);
               }
            }
         } // end for i
         assert (seenqgrams <= goodqgrams && goodqgrams <= maxqgrams) : String.format(
               "j=%d, seen=%d, good=%d, max=%d", j, seenqgrams, goodqgrams, maxqgrams);
         goodfrac = (double) goodqgrams / maxqgrams;
         seenfrac = (double) seenqgrams / goodqgrams;
         if (goodfrac < qgramthreshold || seenfrac < qgramthreshold)
            trepeat.set(j, true);
         if (qgramtabulate)
            out.printf(Locale.US, "%d  %d %d %d  %.2f %.2f%n", j, seenqgrams, goodqgrams,
                  maxqgrams, seenfrac, goodfrac);
         if (jlength < 50)
            trepeat.set(j, true); // TODO: find better length filter
      } // end for j
      if (qgramtabulate) {
         out.close();
         log.info("map: pre-filtered %d/%d reads due to length in %.2f sec", trepeat.cardinality(),
               tm, timer.tocs());
         g.terminate(0);
      }
      log.info("map: pre-filtered %d/%d reads due to low %d-gram complexity in %.2f sec",
            trepeat.cardinality(), tm, qq, timer.tocs());

   }

   /** compute expected number of hit parallelograms */
   @SuppressWarnings("unused")
   final double[] computeEValues(long max, int q, long textlen, int bwidth, double match) {
      final int maxlen = (int) max;
      double[] ev = new double[maxlen + 1];
      double eclumps;
      int t;
      for (int ll = 0; ll <= maxlen; ll++) {
         // compute threshold for given errorlevel
         t = (ll - q) - (int) (java.lang.Math.ceil(ll * errorlevel * q));
         // qgram hits occur in clumps: distribution of number is Poisson
         // compute the expected number of clumps in a paralleogram of block witdth
         eclumps = (double) ll * bwidth * (1 - match) * java.lang.Math.pow(match, q);
         // the number of q-gram hits H within a parallelogram is geometric, P(H=h) =
         // (1-match)*match^(h-1)
         if (t <= 0) {
            ev[ll] = 1.0;
            continue;
         }
         // TODO: Compute compound poisson distribution
      }
      return ev;
   }
}
