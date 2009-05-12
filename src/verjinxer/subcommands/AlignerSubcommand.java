package verjinxer.subcommands;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

import verjinxer.FileNameExtensions;
import verjinxer.Globals;
import verjinxer.QGramMatcher;
import verjinxer.sequenceanalysis.Aligner;
import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.sequenceanalysis.BisulfiteQGramCoder;
import verjinxer.sequenceanalysis.QGramCoder;
import verjinxer.sequenceanalysis.QGramFilter;
import verjinxer.sequenceanalysis.Sequence;
import verjinxer.util.ArrayUtils;
import verjinxer.util.HugeByteArray;
import verjinxer.util.IllegalOptionException;
import verjinxer.util.Match;
import verjinxer.util.MatchesReader;
import verjinxer.util.Options;
import verjinxer.util.ProjectInfo;
import verjinxer.util.TicToc;
import static verjinxer.Globals.programname;
import com.spinn3r.log5j.Logger;

public class AlignerSubcommand implements Subcommand {
   private static final Logger log = Globals.getLogger();
   private Globals g;

   public AlignerSubcommand(Globals g) {
      this.g = g;
   }

   @Override
   public void help() {
      log.info("Usage:");
      log.info("  %s align  [options]  <reads> <reference> <matches file>", programname);
      log.info("Aligns exact seeds from a .matches file between a reads and a reference.");
      log.info("Writes a .mapped file.");
      log.info("Options:");
      log.info("  -q, --qualities      Use qualities while aligning.");
      log.info("  -e <rate>            Maximum error rate");
   }

   @Override
   public int run(String[] args) {
      TicToc totalTimer = new TicToc();
      g.cmdname = "align";

      Options opt = new Options("q=qualities:");
      try {
         args = opt.parse(args);
      } catch (IllegalOptionException ex) {
         log.error(ex);
         return 1;
      }
      double maximumErrorRate = 0.8;
      if (opt.isGiven("q")) {
         maximumErrorRate = Double.parseDouble(opt.get("q"));
      }
      log.info("maximum error rate set to %f", maximumErrorRate);
      if (args.length != 3) {
         help();
         log.error("need three parameters");
         return 1;

      }
      String readsProjectFileName = args[0];
      String referenceProjectFileName = args[1];
      String matchesFileName = args[2];

      ProjectInfo queriesProject, referencesProject;
      try {
         queriesProject = ProjectInfo.createFromFile(readsProjectFileName);
         referencesProject = ProjectInfo.createFromFile(referenceProjectFileName);
      } catch (IOException ex) {
         log.error("Cannot read project file " + ex.getMessage());
         return 1;
      }
      g.startProjectLogging(referencesProject);
      // t = new HugeByteArray(tproject.getLongProperty("Length"));
      // t.read(tfile, 0, -1, 0);
      // tssp = g.slurpLongArray(tsspfile);
      // final ArrayList<String> tdesc = g.slurpTextFile(dt + FileNameExtensions.desc, tssp.length);
      // assert tdesc.size() == tssp.length;
      // public class Match {
      //
      // }
      Sequence queries, references;
      try {
         queries = Sequence.openSequence(queriesProject.getName(), Sequence.Mode.READ);
         references = Sequence.openSequence(referencesProject.getName(), Sequence.Mode.READ);
      } catch (IOException ex) {
         ex.printStackTrace();
         return 1;
      }
      Alphabet alphabet = g.readAlphabet(g.dir + queriesProject.getName()
            + FileNameExtensions.alphabet);

      final String referenceSeparatorPositionsFileName = g.dir + referencesProject.getName()
            + FileNameExtensions.ssp;
      final String queriesSeparatorPositionsFileName = g.dir + queriesProject.getName()
            + FileNameExtensions.ssp;

      long[] referencesSeparatorPositions = g.slurpLongArray(referenceSeparatorPositionsFileName);
      long[] queriesSeparatorPositions = g.slurpLongArray(queriesSeparatorPositionsFileName);
      final ArrayList<String> readDescriptions = g.slurpTextFile(g.dir + queriesProject.getName()
            + FileNameExtensions.desc, -1);
      final ArrayList<String> referenceDescriptions = g.slurpTextFile(g.dir
            + referencesProject.getName() + FileNameExtensions.desc, -1);

      assert readDescriptions.size() == queriesSeparatorPositions.length;
      assert referenceDescriptions.size() == referencesSeparatorPositions.length;

      MatchesReader matchesReader;
      try {
         matchesReader = new MatchesReader(matchesFileName);
      } catch (IOException ex) {
         ex.printStackTrace();
         return 1;
      }
      Match match;

      try {
         while ((match = matchesReader.readMatch()) != null) {
            System.out.format("aligning match: %d %d %d %d %d%n", match.getQueryNumber(),
                  match.getQueryPosition(), match.getReferenceNumber(),
                  match.getReferencePosition(), match.getLength());

            // copy query and reference
            int qn = match.getQueryNumber();
            byte[] query = Arrays.copyOfRange(queries.array(), qn == 0 ? 0
                  : (int) queriesSeparatorPositions[qn-1], (int) queriesSeparatorPositions[qn]);
            int rn = match.getReferenceNumber();
            byte[] reference = Arrays.copyOfRange(references.array(), rn == 0 ? 0
                  : (int) referencesSeparatorPositions[rn-1],
                  (int) referencesSeparatorPositions[rn]);
            AlignedQuery aligned = alignMatchToReference(query, reference, match.getQueryPosition(),
                  match.getReferencePosition(), match.getLength(), maximumErrorRate);
            if (aligned != null) {
               System.out.printf("errors: %d start: %d stop: %d%n", aligned.getErrors(), aligned.getStart(), aligned.getStop());
            }

         }
      } catch (IOException ex) {
         ex.printStackTrace();
         return 1;
      } catch (NumberFormatException ex) {
         ex.printStackTrace();
         return 1;
      }
      final int asize, q;

      // Read project data and determine asize, q; read alphabet map
      // try {
      // asize = indexProject.getIntProperty("qAlphabetSize");
      // q = indexProject.getIntProperty("q");
      // } catch (NumberFormatException ex) {
      // log.error("q-grams for index '%s' not found (Re-create the q-gram index!); %s",
      // ds, ex);
      // return 1;
      // }

      // q-gram filter options from -F option
      // final QGramFilter qgramfilter = new QGramFilter(q, asize, opt.get("F"));

      // Prepare the sequence filter
      int maxseqmatches = Integer.MAX_VALUE;
      if (opt.isGiven("M"))
         maxseqmatches = Integer.parseInt(opt.get("M"));

      // String toomanyhitsfilename = null;
      // if (opt.isGiven("t")) {
      // if (opt.get("t").startsWith("#")) {
      // toomanyhitsfilename = dt + ".toomanyhits-filter";
      // } else {
      // toomanyhitsfilename = g.dir + opt.get("t");
      // }
      // }

      // Determine option values
      // boolean sorted = opt.isGiven("s");
      // int minlen = (opt.isGiven("l") ? Integer.parseInt(opt.get("l")) : q);
      // if (minlen < q) {
      // log.warn("increasing minimum match length to q=%d!", q);
      // minlen = q;
      // }
      // int minseqmatches = (opt.isGiven("m") ? Integer.parseInt(opt.get("m")) : 1);
      // if (minseqmatches < 1) {
      // log.warn("increasing minimum match number to 1!");
      // minseqmatches = 1;
      // }

      // String outname = String.format("%s-%s-%dx%d", tname, sname, minseqmatches, minlen);
      String outname = "blabla";
      if (opt.isGiven("o")) {
         if (outname.length() == 0 || outname.startsWith("#"))
            outname = null;
         else
            outname = opt.get("o");
      }
      if (outname != null)
         outname = g.outdir + outname + ".sorted-matches";
      log.info("will write results to %s", (outname != null ? "'" + outname + "'" : "stdout"));

      final boolean bisulfiteQueries = opt.isGiven("b");

      // start output
      PrintWriter out = new PrintWriter(System.out);
      if (outname != null) {
         try {
            out = new PrintWriter(
                  new BufferedOutputStream(new FileOutputStream(outname), 32 * 1024), false);
         } catch (FileNotFoundException ex) {
            log.error("could not create output file.");
            return 1;
         }
      }
      out.close();
      log.info("done; total time was %.1f sec", totalTimer.tocs());
      g.stopplog();

      return 0;
   }

   private class AlignedQuery {
      private final int start, stop, errors;

      public AlignedQuery(int start, int stop, int errors) {
         this.start = start;
         this.stop = stop;
         this.errors = errors;
      }

      public int getStart() {
         return start;
      }

      public int getStop() {
         return stop;
      }

      public int getErrors() {
         return errors;
      }
   }

   /**
    * Tries to extend a match to the left and to the right such that it covers the whole query.
    * 
    * TODO Documentation Returns tuple (start, stop, r1, r2, errors) r1 and r2 are sequences of
    * characters in which '-' denotes insertion and deletions. errors is the number of errors in the
    * alignment.
    * 
    * @param query
    *           The entire query sequence.
    * @param reference
    *           The entire reference sequence.
    * @param queryPosition
    *           Position of the exact match on the query sequence.
    * @param referencePosition
    *           Position of the exact match on the reference sequence.
    * @param length
    *           Length of the exact match.
    * @param maximumErrorRate
    *           Allow at most query.length * maximumErrorRate errors during the alignment.
    * @return If there were too many errors, null is returned. Otherwise, an AlignedQuery is
    *         returned.
    */
   public AlignedQuery alignMatchToReference(byte[] query, byte[] reference, int queryPosition,
         int referencePosition, int length, double maximumErrorRate) {
      // first, align part in query after the match to refseq
      // then, align part in query before the match to refseq
      int refseqpos = referencePosition;

      int max_errors = (int) (query.length * maximumErrorRate);
      int refseq_end = refseqpos + query.length + max_errors;
      int matchlen = length;
      
      
      byte[] endQuery = Arrays.copyOfRange(query,
            queryPosition + length, query.length);
      byte[] endReference = Arrays.copyOfRange(reference, refseqpos
            + matchlen, reference.length);
      Aligner.ForwardAlignmentResult alignedEnd = Aligner.forwardAlign(endQuery, endReference, max_errors);
      
      if (alignedEnd == null)
         return null;

      byte[] reversedFrontQuery = Arrays.copyOf(query, queryPosition);
      ArrayUtils.reverseArray(reversedFrontQuery, -1);
      byte[] reversedFrontReference = Arrays.copyOf(reference, referencePosition);
      ArrayUtils.reverseArray(reversedFrontReference, -1);

      int errors = alignedEnd.getErrors();

      // r1end, r2end, errors_end, alength_end = result;

      // q_rev = ''.join(reversed(query[:querypos]));
      // refseq_begin = max(refseqpos-querypos-max_errors, 0)
      // r_rev = ''.join(reversed(refseq[refseq_begin:refseqpos])) //
      // ''.join(reversed(refseq[refseq_begin:refseqpos]))

      Aligner.ForwardAlignmentResult alignedFront = Aligner.forwardAlign(reversedFrontQuery,
            reversedFrontReference, max_errors - errors);

      if (alignedFront == null) {
         return null;
      }
      assert alignedFront.getErrors() + alignedEnd.getErrors() <= max_errors;

      int start = referencePosition - alignedFront.getLengthOnReference();
      int stop = referencePosition + matchlen + alignedEnd.getLengthOnReference();

      
      
      return new AlignedQuery(start, stop, errors);
      // r1begin, r2begin, errors_begin, alength_begin = result

      // results.append( (errors_end+errors_middle+errors_begin, r1end, r2end, r1begin, r2begin,
      // start, stop, None) )
      // }
      // assert len(results) != 2 or (results[0][0] != results[1][0])

      // errors, r1end, r2end, r1begin, r2begin, start, stop, mtype = results[0];

      // if type(r1begin) is list:
      // r1begin = ''.join(r1begin)
      // r2begin = ''.join(r2begin)
      // r1begin = list(reversed(r1begin))
      // r2begin = list(reversed(r2begin))
      // r1 = r1begin
      // r1.extend(query[querypos:querypos+matchlen])
      // r1.extend(r1end)
      //
      // r2 = r2begin
      // r2.extend(refseq[refseqpos:refseqpos+matchlen])
      // r2.extend(r2end)
      //
      // return (start, stop, r1, r2, errors, None)

      // amap = { 0: 'A', 1: 'C', 2: 'G', 3: 'T', 4: 'N', 255: '$' }
   }
}
