package verjinxer;

import static verjinxer.Globals.programname;

import java.io.File;
import java.io.IOException;

import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.util.IllegalOptionException;
import verjinxer.util.Options;
import verjinxer.util.ProjectInfo;
import verjinxer.util.StringUtils;
import verjinxer.util.TicToc;

public class TranslaterSubcommand implements Subcommand {
   final Globals g;

   // TODO should not be public
   public TranslaterSubcommand(Globals g) {
      this.g = g;
   }

   /**
    * prints help on usage and options
    */
   public void help() {
      g.logmsg("Usage:%n  %s translate [options] <TextAndFastaFiles...>%n", programname);
      g.logmsg("translates one or more text or FASTA files, using an alphabet map;%n");
      g.logmsg("creates %s, %s, %s, %s, %s;%n", FileNameExtensions.seq, FileNameExtensions.desc,
            FileNameExtensions.alphabet, FileNameExtensions.ssp, FileNameExtensions.prj);
      g.logmsg("with option -r, also creates %s, %s, %s, %s.%n", FileNameExtensions.runseq,
            FileNameExtensions.runlen, FileNameExtensions.run2pos, FileNameExtensions.pos2run);
      g.logmsg("Options:%n");
      g.logmsg("  -i, --index <name>   name of index files [first filename]%n");
      g.logmsg("  -t, --trim           trim non-symbol characters at both ends%n");
      g.logmsg("  -a, --alphabet <file>   filename of alphabet%n");
      g.logmsg("  --dna                use standard DNA alphabet%n");
      g.logmsg("  --rconly             translate to reverse DNA complement%n");
      g.logmsg("  --dnarc     <desc>   combines --dna and --rconly;%n");
      g.logmsg("     if <desc> is empty or '#', concatenate rc with dna; otherwise,%n");
      g.logmsg("     generate new rc sequences and add <desc> to their headers.%n");
      g.logmsg("  --dnabi              translate to bisulfite-treated DNA%n");
      g.logmsg("  --protein            use standard protein alphabet%n");
      g.logmsg("  --masked             lowercase bases are replaced with wildcards (only for DNA alphabets)%n");
      g.logmsg("  -r, --runs           additionally create run-related files%n");
   }

   /**
    * @param args
    *           the command line arguments
    */
   public static void main(String[] args) {
      new TranslaterSubcommand(new Globals()).run(args);
   }

   /**
    * Removes a file name extension from a string. If no extension is found, the name is returned
    * unchanged.
    * 
    * @param name
    *           file name. For example, "hello.fa"
    * @return file name without extension. For example, "hello"
    */
   public static String extensionRemoved(String name) {
      name = new File(name).getName(); // TODO is this necessary?
      int lastdot = name.lastIndexOf('.');
      if (lastdot >= 0) {
         return name.substring(0, lastdot);
      } else {
         return name;
      }
   }

   @Override
   public int run(final String[] args) {
      TicToc gtimer = new TicToc();
      g.cmdname = "translate";

      String filenames[];
      Options opt = new Options(
            "i=index=indexname:,t=trim,a=amap=alphabet:,dna,rc=rconly,dnarc:,dnabi,masked,protein,r=run=runs");
      
      try {
         filenames = opt.parse(args);
      } catch (IllegalOptionException ex) {
         g.warnmsg("%s%n",ex);
         return 1;
      }

      if (filenames.length == 0) {
         help();
         g.logmsg("translate: no files given%n");
         g.terminate(0);
      }

      // determine the name of the index
      String projectname;
      if (opt.isGiven("i"))
         projectname = opt.get("i");
      else { // take base name of first FASTA file
         projectname = extensionRemoved(filenames[0]);
      }
      projectname = g.outdir + projectname;
      ProjectInfo project = new ProjectInfo(projectname);

      project.setProperty("TranslateAction", "translate \"" + StringUtils.join("\" \"", args)
            + "\"");

      boolean trim = opt.isGiven("t");

      // determine the alphabet map(s)
      int givenmaps = 0;
      if (opt.isGiven("a"))
         givenmaps++;
      if (opt.isGiven("dna"))
         givenmaps++;
      if (opt.isGiven("rconly"))
         givenmaps++;
      if (opt.isGiven("dnarc"))
         givenmaps++;
      if (opt.isGiven("dnabi"))
         givenmaps++;
      if (opt.isGiven("protein"))
         givenmaps++;
      if (givenmaps > 1)
         g.terminate("translate: use only one of {-a, --dna, --rconly, --dnarc, --protein}.");

      if (opt.isGiven("masked")
            && !(opt.isGiven("dna") || opt.isGiven("rc") || opt.isGiven("dnarc")))
         g.terminate("translate: --masked can be used only in combination with one of {--dna, --rconly, --dnarc}.");

      Alphabet alphabet = null;
      if (opt.isGiven("a"))
         alphabet = g.readAlphabet(g.dir + opt.get("a"));
      if (opt.isGiven("dna") || opt.isGiven("dnarc"))
         alphabet = opt.isGiven("masked") ? Alphabet.maskedDNA() : Alphabet.DNA();

      boolean reverse = false;
      if (opt.isGiven("rc")) {
         reverse = true;
         alphabet = opt.isGiven("masked") ? Alphabet.maskedcDNA() : Alphabet.cDNA();
      }
      Alphabet alphabet2 = null;
      boolean addrc = false;
      String dnarcstring = null;
      boolean separateRCByWildcard = false;
      if (opt.isGiven("dnarc")) {
         alphabet2 = opt.isGiven("masked") ? Alphabet.maskedcDNA() : Alphabet.cDNA();
         addrc = true;
         dnarcstring = opt.get("dnarc");
         if (dnarcstring.equals(""))
            separateRCByWildcard = true;
         if (dnarcstring.startsWith("#"))
            separateRCByWildcard = true;
      }
      boolean bisulfite = false;
      if (opt.isGiven("dnabi")) {
         bisulfite = true;
         alphabet = Alphabet.DNA(); // do translation on-line
      }
      if (opt.isGiven("protein"))
         alphabet = Alphabet.Protein();

      if (alphabet == null)
         g.terminate("translate: no alphabet map given; use one of {-a, --dna, --rconly, --dnarc, --protein}.");

      g.startplog(project.getName() + FileNameExtensions.log, true); // start new project log

      Translater translater = new Translater(g, trim, alphabet, alphabet2, separateRCByWildcard, reverse,
            addrc, bisulfite, dnarcstring);

      translater.createProject(project, filenames);
      
      g.logmsg("translate: finished translation after %.1f secs.%n", gtimer.tocs());

      // compute runs
      if (opt.isGiven("r")) {
         g.logmsg("translate: computing runs...%n");
         long runs = 0;
         try {
            runs = translater.computeRuns(project.getName());
         } catch (IOException ex) {
            g.terminate("translate: could not create run-related files; " + ex);
         }
         project.setProperty("Runs", runs);
      }

      // write project file
      try {
         project.store();
      } catch (IOException ex) {
         g.terminate(String.format("translate: could not write project file; %s", ex.toString()));
      }

      // that's all
      g.logmsg("translate: done; total time was %.1f secs.%n", gtimer.tocs());
      return 0;
   }

}
