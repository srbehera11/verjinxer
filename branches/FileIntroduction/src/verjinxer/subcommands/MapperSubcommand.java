/**
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

import verjinxer.Globals;
import verjinxer.MapperByAlignment;
import verjinxer.MapperByQGrams;
import verjinxer.Project;
import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.sequenceanalysis.QGramCoder;
import verjinxer.util.ArrayFile;
import verjinxer.util.BitArray;
import verjinxer.util.FileTypes;
import verjinxer.util.IllegalOptionException;
import verjinxer.util.Options;
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
    *           the globals object to be used
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
      log.info("Writes %s and %s.", FileTypes.MAPPED, FileTypes.NONMAPPABLE);
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

   private int repeatthreshold = Integer.MAX_VALUE;
   private double qgramthreshold = 0.0;
   private boolean qgramtabulate = false;

   // byte[] t = null; // the text (coded)


   private long totalindexlen = 0; // total length of all index texts
   private String filterstring = null;

   enum Method {
      QGRAM, SUFFIX, FULL
   }

   private Method method;

   /**
    * @param args
    *           the command line arguments
    * @return zero on success, nonzero if there is a problem
    */
   public int run(String[] args) {
      g.cmdname = "map";

      Options opt = new Options(
            "atonce,m=method:,e=error=errorlevel:,b=blocksize:,r=rc=revcomp,s=select:,R=repeat=repeatthreshold:,Q=qcomplexity:,c=clip:,o=out:");
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
      final File tProjectName = new File(args[0]);

      // Determine method
      method = null;
      FileTypes mext = null;
      if (opt.isGiven("m")) {
         if (opt.get("m").startsWith("q")) {
            method = Method.QGRAM;
            mext = FileTypes.QPOSITIONS;
         } else if (opt.get("m").startsWith("s")) {
            method = Method.SUFFIX;
            mext = FileTypes.POS;
         } else if (opt.get("m").startsWith("f")) {
            method = Method.FULL;
            mext = FileTypes.SEQ;
         } else {
            help();
            log.error("map: Unknown method '%s'.", opt.get("m"));
            return 1;
         }
      } else {
         method = Method.QGRAM;
         mext = FileTypes.QPOSITIONS;
      }

      // Determine options
      int blocksize = 1024;
      double errorlevel = 0.03;
      if (opt.isGiven("e"))
         errorlevel = Double.parseDouble(opt.get("e"));
      if (errorlevel < 0.0 || errorlevel > 1.0) {
         log.error("map: Illegal error level specified.");
         return 1;
      }
      int clip = 0;
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
      final boolean revcomp = opt.isGiven("r");
      boolean atonce = opt.isGiven("atonce");
      filterstring = opt.get("f");
      if (filterstring == null)
         filterstring = "0:0";

      // Read sequence project
      final Project tproject;
      try {
         tproject = Project.createFromFile(tProjectName);
      } catch (IOException ex) {
         log.error("could not read project file: %s", ex);
         return 1;
      }
      g.startProjectLogging(tproject);
      final int tm = tproject.getIntProperty("NumberSequences");

      final BitArray tselect;

      // Select sequences
      if (opt.isGiven("s")) {
         if (opt.get("s").startsWith("#")) {
            tselect = g.slurpBitArray(tproject.makeFile(FileTypes.SELECT));
         } else {
            tselect = g.slurpBitArray(new File(opt.get("s")));
         }
      } else { // select all
         tselect = new BitArray(tm);
         for (int bbb = 0; bbb < tm; bbb++)
            tselect.set(bbb, 1);
      }
      // initialize the other bit arrays
      final BitArray trepeat = new BitArray(tm);
      final BitArray tmapped = new BitArray(tm);

      // Get index names and check indices
      int inum = args.length - 1;
      final ArrayList<String> indices = new ArrayList<String>(inum);
      try {
         for (int i = 0; i < inum; i++) {
            String ix = args[i + 1];
            if (ix.startsWith("@")) {
               BufferedReader br = new BufferedReader(new FileReader(new File(ix.substring(1))));
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
               indices.add(ix);
            }
         }
      } catch (IOException ex) {
         log.error("map: could not read indices; " + ex);
         return 1;
      }
      inum = indices.size();
      long longestpos = 0; // length of longest [q]pos
      long longestindexlen = 0; // length of longest index text
      for (int i = 0; i < inum; i++) {
         File fin = new File(indices.get(i) + mext);
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
         fin = new File(indices.get(i) + FileTypes.SEQ);
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
      final int asize = tproject.getIntProperty("LargestSymbol") + 1;
      if (revcomp && asize != 4) {
         log.error("map: can only use reverse complement option with DNA sequences. Stop.");
         return 1;
      }
      Alphabet alphabet = tproject.readAlphabet();
      File tsspfile = tproject.makeFile(FileTypes.SSP);

      final long[] tssp = g.slurpLongArray(tsspfile);
      assert tm == tssp.length;
      final ArrayList<String> tdesc = Globals.slurpTextFile(tproject.makeFile(FileTypes.DESC), tm);
      long longestsequence = tproject.getLongProperty("LongestSequence");
      long shortestsequence = tproject.getLongProperty("ShortestSequence");
      final byte[] tall = g.slurpByteArray(tproject.makeFile(FileTypes.SEQ));

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
         filtersequences(longestsequence, shortestsequence, tm, asize, tproject, tall, trepeat, tssp);

      final PrintWriter allout;
      // open output file
      boolean closeout = true;
      try {
         if (opt.isGiven("o")) {
            if (opt.get("o").startsWith("#")) {
               allout = new PrintWriter(System.out);
               closeout = false;
            } else {
               allout = new PrintWriter(opt.get("o"));
            }
         } else
            allout = new PrintWriter(tproject.makeFile(FileTypes.ALLMAPPED));
      } catch (IOException ex) {
         log.error("map: could not create output file; %s", ex);
         return 1;
      }

      // run chosen method
      if (method == Method.QGRAM && !atonce) {
         MapperByQGrams m = new MapperByQGrams(g, longestsequence, alphabet, tdesc, tssp, tm, indices, longestpos, longestindexlen);
         m.mapByQGram(blocksize);
      } else if (method == Method.FULL && atonce) {
         MapperByAlignment m = new MapperByAlignment(g, longestsequence, tm,
               asize, allout, tselect, trepeat, tmapped, tall, tssp, tproject, indices);
         m.mapByAlignmentAtOnce(clip, errorlevel, revcomp);

      } else
         throw new UnsupportedOperationException(
               "The requested method has not yet been implemented!");

      // clean up and report
      allout.flush();
      log.info(
            "map: successfully mapped %d / %d selected (%d total) sequences;%n     found %d repeats, %d sequences remain.",
            tmapped.cardinality(), selected, tm, trepeat.cardinality(), tselect.cardinality());
      g.dumpBitArray(tproject.makeFile(FileTypes.SELECT), tselect);
      g.dumpBitArray(tproject.makeFile(FileTypes.REPEAT_FILTER), trepeat);
      g.stopplog();
      if (closeout)
         allout.close();
      return 0;
   }

   /**
    * Filter sequences according to contained number of distinct q-grams. Needs asize, tall[],
    * qgram[], ...
    * 
    * @param shortestsequence
    */
   final void filtersequences(long longestsequence, long shortestsequence, int tm, final int asize,
         final Project tProject, byte[] tall, BitArray trepeat, long[] tssp) {
      PrintWriter out = null;
      final int as = asize;
      TicToc timer = new TicToc();
      // set q-gram length q and other parameters
      int qq = 4 + (int) (Math.ceil(Math.log(longestsequence) / Math.log(as - 1)));
      if (qgramtabulate)
         log.info("map: tabulating %d-gram complexity of all reads", qq);
      else
         log.info("map: pre-filtering reads with %d-gram complexity < %f", qq, qgramthreshold);
      QGramCoder coder = new QGramCoder(qq, as);
      byte[] qgram = new byte[qq];
      int qcode;
      int jstart, jstop, jlength, success;
      byte next;
      BitArray seen = new BitArray(coder.numberOfQGrams);
      int maxqgrams, goodqgrams, seenqgrams;
      double goodfrac, seenfrac;

      // out has not yet been opened; open it for tabulating.
      if (qgramtabulate) {
         try {
            out = new PrintWriter(tProject.makeFile(tProject.getName() + qq + FileTypes.QCOMPLEXITY));
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
               assert qcode >= 0;
               goodqgrams++;
               if (seen.get(qcode) == 0) {
                  seenqgrams++;
                  seen.set(qcode, true);
               }
            }
         } // end for i
         assert seenqgrams <= goodqgrams && goodqgrams <= maxqgrams : String.format(
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
   final double[] computeEValues(long max, int q, long textlen, int bwidth, double match,
         double errorlevel) {
      final int maxlen = (int) max;
      double[] ev = new double[maxlen + 1];
      double eclumps;
      int t;
      for (int ll = 0; ll <= maxlen; ll++) {
         // compute threshold for given errorlevel
         t = (ll - q) - (int) (Math.ceil(ll * errorlevel * q));
         // qgram hits occur in clumps: distribution of number is Poisson
         // compute the expected number of clumps in a paralleogram of block witdth
         eclumps = (double) ll * bwidth * (1 - match) * Math.pow(match, q);
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
