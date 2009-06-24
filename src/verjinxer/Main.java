/*
 * Main.java Created on 30. Januar 2007, 14:38
 */

package verjinxer;

import static verjinxer.Globals.programname;
import static verjinxer.Globals.version;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;

import verjinxer.subcommands.AdapterRemoverSubcommand;
import verjinxer.subcommands.AlignerSubcommand;
import verjinxer.subcommands.MapperSubcommand;
import verjinxer.subcommands.QGramIndexCompressionAnalyzerSubcommand;
import verjinxer.subcommands.QGramIndexerSubcommand;
import verjinxer.subcommands.QGramMatcherSubcommand;
import verjinxer.subcommands.Sequence2FastqSubcommand;
import verjinxer.subcommands.Subcommand;
import verjinxer.subcommands.TranslaterSubcommand;
import verjinxer.util.IllegalOptionException;
import verjinxer.util.Options;

import com.spinn3r.log5j.Logger;

/**
 * 
 * @author Sven Rahmann
 */

public class Main {

   static Logger log = Globals.getLogger();
   Globals g;

   public Main(Globals gl) {
      g = gl;
   }

   /**
    * print usage information on the Vinxer application and exit
    */
   private void usage() {
      log.info("Usage: %s [global-options] <command> [command-options] <arguments>", programname);
      log.info("Valid commands:");
      log.info("  help |  help <command>    general or command-specific help on usage");
      log.info("  translate    ...          apply alphabet map to text or FASTA files");
      log.info("  cut          ...          cut translated sequences at specific patterns");
      log.info("  qgram        ...          create qgram-index of translated file(s)");
      log.info("  analyzer     ...          analyze compressibility of qgram-index");
      log.info("  qfreq        ...          report most frequent q-grams in an index");
      log.info("  qmatch       ...          report maximal matches of sequences vs index");
      log.info("  suffix       ...          build suffixtray of translated file(s)");
      log.info("  bigsuffix    ...          build suffixtray of HUGE translated file(s)");
      log.info("  map          ...          map sequences to an index");
      log.info("  align        ...          aligns sequences using results from qmatch");
      log.info("  nonunique    ...          find non-unique specific probes in an index");
      log.info("  rmadapt      ...          remove adapters from sequences");
      log.info("  seq2fastq    ...          converts a sequence into a fastq file");
      log.info("Global options:");
      log.info("  -Q, --quiet               quiet mode, don't print messages to stdout");
      log.info("  -L, --log    <logfile>    add'l file to print diagnostic messages to");
      log.info("  -P, --noplog              DON'T write messages to project .log file");
      log.info("  -h, --help                show this help");
   }

   public static void main(String[] args) {
      System.exit(new Main(new Globals()).run(args));
   }

   public int run(String[] args) {
      g.action = args;
      Options opt = new Options(
            "Q=quiet,D=dir:,L=log=logfile:,P=noplog=noprojectlog,h=help");
      try {
         args = opt.parse(args);
      } catch (IllegalOptionException ex) {
         log.error("%s", ex);
         return 1;
      }

      if (opt.isGiven("h")) {
         usage();
         return 0;
      }

      // Determine quietmode
      if (opt.isGiven("Q"))
         log.setLevel(Level.WARN);

      // extra log file
      if (opt.isGiven("L")) {
         try {
            log.addAppender(new FileAppender(new PatternLayout("%m%n"), opt.get("L")));
         } catch (IOException ex) {
            log.warn("%s", ex);
         }
      }

      g.logToFile = !opt.isGiven("P");
      // g.startProjectLogging(project);
      // TODO
      // // no -P option (--noplog) given means: project log is requested
      //       
      // loggerP.printf("%n# %s%n", new Date());
      // loggerP.printf("# \"%s\"%n", StringUtils.join("\" \"",action));
      //
      // } catch (FileNotFoundException ex) {
      // warnmsg("%s: could not open project log '%s'; continuing...", programname, fname);
      //       
      // Logger.getRootLogger().
      // Logger.getRootLogger().addAppender(new FileAppender(new PatternLayout("%m%n"),
      // logfilename));
      // g.plog = false;
      // }

      title();
      if (args.length == 0) {
         usage();
         return 0;
      }

      String command = args[0].toLowerCase();
      String[] rest = Arrays.asList(args).subList(1, args.length).toArray(new String[0]);

      Subcommand subcommand = null;

      // Process all available commands
      if (command.startsWith("he")) {
         help(rest);
      } else if (command.startsWith("al")) {
         subcommand = new AlignerSubcommand(g);
      } else if (command.startsWith("tr")) {
         subcommand = new TranslaterSubcommand(g);
      } else if (command.startsWith("cut")) {
         new Cutter(g).run(rest);
      } else if (command.startsWith("qg")) {
         subcommand = new QGramIndexerSubcommand(g);
      } else if (command.startsWith("an")) {
         subcommand = new QGramIndexCompressionAnalyzerSubcommand(g);
      } else if (command.startsWith("qf")) {
         new QGramFrequencer(g).run(rest);
      } else if (command.startsWith("qm")) {
         subcommand = new QGramMatcherSubcommand(g);
      } else if (command.startsWith("su")) {
         new SuffixTrayBuilder(g).run(rest);
      } else if (command.startsWith("bigsu")) {
         new BigSuffixTrayBuilder(g).run(rest);
      } else if (command.startsWith("ma")) {
         subcommand = new MapperSubcommand(g);
      } else if (command.startsWith("nu") || command.startsWith("nonunique")) {
         new NonUniqueProbeDesigner(g).run(rest);
      } else if (command.startsWith("rm")) {
         subcommand = new AdapterRemoverSubcommand(g);
      } else if (command.startsWith("seq2fastq")) {
         subcommand = new Sequence2FastqSubcommand(g);
      } else {
         usage();
         log.error("Unrecognized command: '%s'", command);
         return 1;
      }
      if (subcommand != null)
         return subcommand.run(rest);
      return 1;
   }

   private void title() {
      log.info("%s, a versatile Java-based Indexer, v%s", programname, version);
   }

   private int help(String[] args) {
      if (args.length == 0) {
         usage();
         return 0;
      }

      String command = args[0];
      // String[] rest = Arrays.asList(args).subList(1, args.length).toArray(new String[0]);
      // waere schoen, wenn man rest=args[1..end] einfacher erhalten koennte

      Subcommand subcommand = null;

      // Process help on each command
      if (command.startsWith("he")) {
         usage();
         return 0;
      } else if (command.startsWith("al")) {
         subcommand = new AlignerSubcommand(g);
      } else if (command.startsWith("cut")) {
         new Cutter(g).help();
      } else if (command.startsWith("tr")) {
         subcommand = new TranslaterSubcommand(g);
      } else if (command.startsWith("qg")) {
         subcommand = new QGramIndexerSubcommand(g);
      } else if (command.startsWith("qf")) {
         new QGramFrequencer(g).help();
      } else if (command.startsWith("qm")) {
         subcommand = new QGramMatcherSubcommand(g);
      } else if (command.startsWith("su")) {
         new SuffixTrayBuilder(g).help();
      } else if (command.startsWith("bigsu")) {
         new BigSuffixTrayBuilder(g).help();
      } else if (command.startsWith("ma")) {
         subcommand = new MapperSubcommand(g);
      } else if (command.startsWith("nu") || command.startsWith("nonunique")) {
         new NonUniqueProbeDesigner(g).help();
      } else {
         usage();
         log.warn("Unrecognized command: '%s'", command);
         return 1;
      }
      if (subcommand != null)
         subcommand.help();
      return 0;
   }

}
