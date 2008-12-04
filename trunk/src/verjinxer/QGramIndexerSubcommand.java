/*
 * QgramIndexer.java
 * Created on 30. Januar 2007, 15:15
 */
package verjinxer;

import static verjinxer.Globals.programname;

import java.io.IOException;

import com.spinn3r.log5j.Logger;

import verjinxer.util.IllegalOptionException;
import verjinxer.util.Options;
import verjinxer.util.ProjectInfo;
import verjinxer.util.StringUtils;

/**
 * This class provides a q-gram indexer.
 * @author Sven Rahmann
 * @author Marcel Martin
 */
public final class QGramIndexerSubcommand implements Subcommand {
   private static final Logger log = Globals.log;
   
   /** only store q-grams whose positions are divisible by stride */
   private int stride = 1;
   private Globals g;

   /** Creates a new instance of QgramIndexer
    * @param gl  the Globals object to use
    */
   public QGramIndexerSubcommand(Globals gl) {
      g = gl;
   }

   /**
    * print help on usage
    */
   public void help() {
      log.info("Usage:");
      log.info("%s qgram [options] Indexnames...", programname);
      log.info("Builds a q-gram index of .seq files; filters out low-complexity q-grams;");
      log.info("writes %s, %s, %s, %s.", FileNameExtensions.qbuckets, FileNameExtensions.qpositions, FileNameExtensions.qfreq, FileNameExtensions.qseqfreq);
      log.info("Options:");
      log.info("  -q  <q>                 q-gram length [0=reasonable]");
      log.info("  -s, --stride <stride>   only store q-grams whose positions are divisible by stride (default: %d)", stride);
      log.info("  -b, --bisulfite         simulate bisulfite treatment");
      log.info("  -f, --allfreq           write (unfiltered) frequency files (--freq, --sfreq)");
      log.info("  --freq                  write (unfiltered) q-gram frequencies (%s)", FileNameExtensions.qfreq);
      log.info("  --seqfreq, --sfreq      write in how many sequences each qgram appears (%s)", FileNameExtensions.qseqfreq);
      log.info("  -c, --check             additionally check index integrity");
      log.info("  -C, --onlycheck         ONLY check index integrity");
      log.info("  -F, --filter <cplx:occ> PERMANENTLY apply low-complexity filter to %s", FileNameExtensions.qbuckets);
      log.info("  -X, --notexternal       DON'T save memory at the cost of lower speed");
   }

   /** if run independently, call main
    * @param args  the command line arguments
    */
   public static void main(String[] args) {
      new QGramIndexerSubcommand(new Globals()).run(args);
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
         log.error("qgram: " + ex);
         return 1;
      }
      if (args.length == 0) {
         help();
         log.error("qgram: no index given");
         return 0;
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
      
      log.info("qgram: stride width is %d", stride);
      
      // Loop through all files
      for (String indexname : args) {
         String di = g.dir + indexname;
         String dout = g.outdir + indexname;

         // Read properties.
         // If we only check index integrity, do that and continue with next index.
         // Otherwise, extend the properties and go on building the index.
         
         ProjectInfo project;
         try {
            project = ProjectInfo.createFromFile(di);
         } catch (IOException ex) {
            log.error("qgram: cannot read project file.");
            return 1;
         }
         g.startProjectLogging(project);
         QGramIndexer qgramindexer = new QGramIndexer(g, project, q, external, bisulfite, stride, opt.get("F"));
         if (checkonly) {
            if (qgramindexer.docheck(di, project) >= 0) returnvalue = 1;
            continue;
         }
         project.setProperty("QGramAction", action);


         try {
            final String freqfile = (freq ? dout + FileNameExtensions.qfreq : null);
            final String sfreqfile = (sfreq ? dout + FileNameExtensions.qseqfreq : null);
            qgramindexer.generateAndWriteIndex(di + FileNameExtensions.seq, 
                  dout + FileNameExtensions.qbuckets, dout + FileNameExtensions.qpositions, freqfile, sfreqfile);
         } catch (IOException ex) {
            ex.printStackTrace();
            log.error("qgram: failed on %s: %s; continuing with remainder...", indexname, ex);
            g.stopplog();
            continue;
         }
         
         final double[] times = qgramindexer.getLastTimes();
         log.info("qgram: time for %s: %.1f sec or %.2f min", indexname, times[0], times[0] / 60.0);

         if (check && qgramindexer.docheck(di, project) >= 0) returnvalue = 1;
         g.stopplog();
      } // end for each file
      
      return returnvalue; // 1 if failed on any of the indices; 0 if everything ok.
   }
}