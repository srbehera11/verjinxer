/*
 * QgramIndexer.java
 * Created on 30. Januar 2007, 15:15
 */
package verjinxer;

import static verjinxer.Globals.programname;

import java.io.IOException;

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
         g.terminate("qgram: " + ex);
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
         
         ProjectInfo project;
         try {
            project = ProjectInfo.createFromFile(di);
         } catch (IOException e) {
            g.warnmsg("qgram: cannot read project file.%n");
            return 1;
         }
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
         } catch (IOException e) {
            e.printStackTrace();
            g.warnmsg("qgram: failed on %s: %s; continuing with remainder...%n", indexname, e.toString());
            g.stopplog();
            continue;
         }
         
         project.setProperty("qfreqMax", qgramindexer.getMaximumFrequency());
         project.setProperty("qbckMax", qgramindexer.getMaximumBucketSize());
         
         final double[] times = qgramindexer.getLastTimes();
         g.logmsg("qgram: time for %s: %.1f sec or %.2f min%n", indexname, times[0], times[0] / 60.0);

         try {
            project.store();
         } catch (IOException ex) {
            g.warnmsg("qgram: could not write %s, skipping! (%s)%n", project.getFileName(), ex.toString());
         }
         if (check && qgramindexer.docheck(di, project) >= 0) returnvalue = 1;
         g.stopplog();
      } // end for each file
      
      return returnvalue; // 1 if failed on any of the indices; 0 if everything ok.
   }
}
