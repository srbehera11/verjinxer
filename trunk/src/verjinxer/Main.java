/*
 * Main.java
 *
 * Created on 30. Januar 2007, 14:38
 *
 */

// TODO: mirgrate to Java.util.logging instead of my own logger!

package verjinxer;
import java.io.*;
import java.util.Arrays;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;

import com.spinn3r.log5j.Logger;


import verjinxer.util.IllegalOptionException;
import verjinxer.util.Options;
import static verjinxer.Globals.*;

/**
 *
 * @author Sven Rahmann
 */

public class Main {
  
   static Logger log = Logger.getLogger(Main.class);
   Globals g;
  
   public Main(Globals gl) {
      g = gl;
      log.setLevel(Level.INFO);
      // for now, only print the message itself (%m), nothing else
      Logger.getRootLogger().addAppender(new ConsoleAppender(new PatternLayout("%m%n")));
   }
  
  /**
   * print usage information on the Vinxer application and exit
   *  @params exitcode exit code passed to the operating system
   */
  private void usage() {
    log.info("Usage: %s [global-options] <command> [command-options] <arguments>%n", programname);
    log.info("Valid commands:%n");
    log.info("  help |  help <command>    general or command-specific help on usage%n");
    log.info("  translate    ...          apply alphabet map to text or FASTA files%n");
    log.info("  cut          ...          cut translated sequences at specific patterns%n");
    log.info("  qgram        ...          create qgram-index of translated file(s)%n");
    log.info("  qfreq        ...          report most frequent q-grams in an index%n");
    log.info("  qmatch       ...          report maximal matches of sequences vs index%n");
    log.info("  suffix       ...          build suffixtray of translated file(s)%n");
    log.info("  bigsuffix    ...          build suffixtray of HUGE translated file(s)%n");
    log.info("  map          ...          map sequences to an index%n");
    log.info("  nonunique    ...          find non-unique specific probes in an index%n");
    log.info("Global options:%n");
    log.info("  -Q, --quiet               quiet mode, don't print messages to stdout%n");
    log.info("  -D, --dir    <directory>  working directory%n");
    log.info("  -O, --outdir <directory>  output directory (not recommended)%n");
    log.info("  -L, --log    <logfile>    add'l file to print diagnostic messages to%n");
    log.info("  -P, --noplog              DON'T write messages to project .log file%n");
    log.info("  -h, --help                show this help%n");
  }
  
  public static void main(String[] args) {
    new Main(new Globals()).run(args);
  }
  
  
  public void run(String[] args) {
    g.action = args;
    Options opt = new Options("Q=quiet,D=dir:,O=outdir=outputdir:,L=log=logfile:,P=noplog=noprojectlog,h=help");
    try {
      args = opt.parse(args);
    } catch (IllegalOptionException e) {
      log.warn("%s%n", e.toString());
      System.exit(1);
    }

    if (opt.isGiven("h")) {
      usage(); g.terminate(0);
    }
 
    // Determine quietmode?
    if (opt.isGiven("Q")) g.quiet=true;
    
    // Determine working and output directory
    if (opt.isGiven("D")) {
      g.dir = opt.get("D");
      if (g.dir.charAt(g.dir.length()-1)!=File.separatorChar) g.dir += File.separatorChar;
    }
    g.outdir = g.dir;
    if (opt.isGiven("O")) {
      g.outdir = opt.get("O");
      if (g.outdir.charAt(g.outdir.length()-1)!=File.separatorChar) g.outdir += File.separatorChar;
    }
    
    // Determine the log file(s)
    if (opt.isGiven("P")) g.plog=false;
    try {
      if (opt.isGiven("L")) g.setlogger(new PrintStream(g.dir+opt.get("L")));
    } catch (FileNotFoundException ex) {
      log.warn("%s%n", ex.toString());
    }
    
    title();
    if (args.length == 0) {
      usage(); 
      g.terminate(0);
    }
    
    String   command = args[0].toLowerCase();
    String[] rest    = Arrays.asList(args).subList(1, args.length).toArray(new String[0]);
    
    Subcommand subcommand = null;
    
    // Process all available commands
    if (command.startsWith("he")) {
      help(rest);
    } else if (command.startsWith("tr")) {
      subcommand = new TranslaterSubcommand(g);
    } else if (command.startsWith("cut")) {
      new Cutter(g).run(rest);
    } else if (command.startsWith("qg")) {
      subcommand = new QgramIndexer(g);
    } else if (command.startsWith("qf")) {
      new QgramFrequencer(g).run(rest);
    } else if (command.startsWith("qm")) {
      subcommand = new QgramMatcherSubcommand(g);
    } else if (command.startsWith("su")) {
      new SuffixTrayBuilder(g).run(rest);
    } else if (command.startsWith("bigsu")) {
      new BigSuffixTrayBuilder(g).run(rest);
    } else if (command.startsWith("ma")) {
      subcommand = new Mapper(g);
    } else if (command.startsWith("nu") || command.startsWith("nonunique")) {
      new NonUniqueProbeDesigner(g).run(rest);
    } else {
      usage();
      log.warn("Unrecognized command: '%s'%n", command);
      g.terminate(1);
    }
    if (subcommand != null)
       subcommand.run(rest);
  }
  
  private void title() {
    log.info("%s, a versatile Java-based Indexer, v%s", programname, version);
  }
  
  private void help(String[] args) {
    if (args.length == 0) {
      usage(); 
      g.terminate(0);
    }
    
    String   command = args[0];
    //String[] rest    = Arrays.asList(args).subList(1, args.length).toArray(new String[0]);
      // waere schoen, wenn man rest=args[1..end] einfacher erhalten koennte
    
    Subcommand subcommand = null;

    // Process help on each command
    if (command.startsWith("he")) {
      usage(); g.terminate(0);
    } else if (command.startsWith("cut")) {
      new Cutter(g).help();
    } else if (command.startsWith("tr")) {
      subcommand = new TranslaterSubcommand(g);
    } else if (command.startsWith("qg")) {
      subcommand = new QgramIndexer(g);
    } else if (command.startsWith("qf")) {
      new QgramFrequencer(g).help();
    } else if (command.startsWith("qm")) {
      subcommand = new QgramMatcherSubcommand(g);
    } else if (command.startsWith("su")) {
      new SuffixTrayBuilder(g).help();
    } else if (command.startsWith("bigsu")) {
      new BigSuffixTrayBuilder(g).help();
    } else if (command.startsWith("ma")) {
      subcommand = new Mapper(g);
    } else if (command.startsWith("nu") || command.startsWith("nonunique")) {
      new NonUniqueProbeDesigner(g).help();
    } else {
      usage();
      log.warn("Unrecognized command: '%s'%n", command);
      g.terminate(1);
    }
    if (subcommand != null)
       subcommand.help();
  }
}
