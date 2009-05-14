package verjinxer.subcommands;

import static verjinxer.Globals.programname;

import java.io.IOException;

import com.spinn3r.log5j.Logger;

import verjinxer.FileNameExtensions;
import verjinxer.Globals;
import verjinxer.Translater;
import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.util.FileTypes;
import verjinxer.util.FileUtils;
import verjinxer.util.IllegalOptionException;
import verjinxer.util.Options;
import verjinxer.util.ProjectInfo;
import verjinxer.util.StringUtils;
import verjinxer.util.TicToc;

public class TranslaterSubcommand implements Subcommand {
   final Globals g;

   private static final Logger log = Globals.getLogger();

   // TODO should not be public
   public TranslaterSubcommand(Globals g) {
      this.g = g;
   }

   /**
    * prints help on usage and options
    */
   public void help() {
      log.info("Usage:  %s translate [options] <TextAndFastaFiles...>", programname);
      log.info("translates one or more text, CSFASTA or FASTA files, using an alphabet map;");
      log.info("creates %s, %s, %s, %s, %s;", FileNameExtensions.seq, FileNameExtensions.desc,
            FileNameExtensions.alphabet, FileNameExtensions.ssp, FileNameExtensions.prj);
      log.info("with option -r, also creates %s, %s, %s, %s.", FileNameExtensions.runseq,
            FileNameExtensions.runlen, FileNameExtensions.run2pos, FileNameExtensions.pos2run);
      log.info("When translating CSFASTA files, an alphabet map must not be given.");
      log.info("Options:");
      log.info("  -i, --index <name>   name of index files [first filename]");
      log.info("  -t, --trim           trim non-symbol characters at both ends");
      log.info("  -a, --alphabet <file>   filename of alphabet");
      log.info("  --dna                use standard DNA alphabet");
      log.info("  --rconly             translate to reverse DNA complement");
      log.info("  --dnarc     <desc>   combines --dna and --rconly;");
      log.info("     if <desc> is empty or '#', concatenate rc with dna; otherwise,");
      log.info("     generate new rc sequences and add <desc> to their headers.");
//      log.info("  -b, --bisulfite      translates DNA to a three-letter alphabet"); // FIXME only for C->T currently
      log.info("  --dnabi              translate to bisulfite-treated DNA");
      log.info("  --protein            use standard protein alphabet");
      log.info("  -c, --colorspace     translate DNA to color space sequence");
      log.info("  --masked             lowercase bases are translated to wildcards (only for DNA alphabets)");
      log.info("  --reverse            reverse sequence before applying alphabet (use with -a)");
      log.info("  -r, --runs           additionally create run-related files");
      log.info("  -q, --quality <file> read quality file (only for CSFASTA)");
   }

   /**
    * @param args
    *           the command line arguments
    */
   public static void main(String[] args) {
      System.exit(new TranslaterSubcommand(new Globals()).run(args));
   }

   @Override
   public int run(final String[] args) {
      TicToc gtimer = new TicToc();
      g.cmdname = "translate";

      String filenames[];
      Options opt = new Options(
            "i=index=indexname:,t=trim,a=amap=alphabet:,dna,rc=rconly,dnarc:,dnabi,masked,protein,r=run=runs,reverse,c=color=colorspace,q=quality:");

      try {
         filenames = opt.parse(args);
      } catch (IllegalOptionException ex) {
         log.warn("%s", ex);
         return 1;
      }

      if (filenames.length == 0) {
         help();
         log.info("translate: no files given");
         return 0;
      }

      // determine the name of the index
      String projectname;
      if (opt.isGiven("i"))
         projectname = opt.get("i");
      else { 
         // take base name of first FASTA file
         projectname = FileUtils.extensionRemoved(filenames[0]);
      }
      ProjectInfo project = new ProjectInfo(projectname);

      project.setProperty("TranslateAction", "translate \"" + StringUtils.join("\" \"", args)
            + "\"");

      boolean trim = opt.isGiven("t");

      if (opt.isGiven("reverse") && !opt.isGiven("a")) {
         log.error("translate: option --reverse can only be used with custom alphabets");
         return 1;
      }
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
      if (opt.isGiven("bisulfite"))
         givenmaps++;
      if (opt.isGiven("colorspace"))
         givenmaps++;
      if (givenmaps > 1) {
         log.error("translate: use only one of {-a, --dna, --rconly, --dnarc, --dnabi, --protein, --bisulfite, --colorspace}.");
         return 1;
      }

      if (opt.isGiven("masked")
            && !(opt.isGiven("dna") || opt.isGiven("rc") || opt.isGiven("dnarc"))) {
         log.error("translate: --masked can be used only in combination with one of {--dna, --rconly, --dnarc}.");
         return 1;
      }
      Alphabet alphabet = null;
      if (opt.isGiven("a"))
         alphabet = Globals.readAlphabet(g.dir + opt.get("a"));
      if (opt.isGiven("dna") || opt.isGiven("dnarc"))
         alphabet = opt.isGiven("masked") ? Alphabet.maskedDNA() : Alphabet.DNA();

      boolean reverse = false;
      if (opt.isGiven("rc")) {
         reverse = true;
         alphabet = opt.isGiven("masked") ? Alphabet.maskedcDNA() : Alphabet.cDNA();
      }
      if (opt.isGiven("reverse")) {
         log.info("translate: reversing string before applying alphabet");
         reverse = true;
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
      boolean colorspace = false;
      if (opt.isGiven("colorspace")) {
         alphabet = Alphabet.CS();
         for (int i = 0; i < filenames.length; i++) {
            // all files must be FASTA
            if (FileUtils.determineFileType(filenames[i]) != FileTypes.FASTA) {
               log.error("translate: The option --colorspace is only valid for FASTA files.");
               return 1;
            }
         }
         colorspace = true;
      }
      String qualityFilename = null; 
      if (opt.isGiven("quality")) {
         qualityFilename = opt.get("quality");
         if ((filenames.length!=1) || (FileUtils.determineFileType(filenames[0]) != FileTypes.CSFASTA)) {
            log.error("translate: If option --quality is given, exactly one CSFASTA file is expected.");
            return 1;
         }
         if (addrc||reverse) {
            log.error("translate: Option --quality forbids use of reverse (complement).");
            return 1;
         }
      }

      if (alphabet == null) {
         for (int i = 0; i < filenames.length; i++) {
            if (FileUtils.determineFileType(filenames[i]) != FileTypes.CSFASTA) { // only for CSFASTA omitting alphabet map is allowed
               log.error("translate: no alphabet map given; use one of {-a, --dna, --rconly, --dnarc, --dnabi, --protein, --colorspace}.");
               return 1;
            }
         }
         // all files are CSFASTA -> go on
         alphabet = Alphabet.CS();
      } else {
         // A alphabet was set, test if all files are NOT CSFASTA
         for (int i = 0; i < filenames.length; i++)
            if (FileUtils.determineFileType(filenames[i]) == FileTypes.CSFASTA) { // invalid option for CSFASTA
               log.error("translate: the options -a, --dna, --rconly, --dnarc, --dnabi, --protein and --colorspace are not valid for CSFASTA files.");
               return 1;
            }
      }
      g.startProjectLogging(project, true);

      Translater translater = new Translater(g, trim, alphabet, alphabet2, separateRCByWildcard,
            reverse, addrc, bisulfite, dnarcstring, colorspace, qualityFilename);

      translater.createProject(project, filenames);

      log.info("translate: finished translation after %.1f secs.", gtimer.tocs());

      // compute runs
      if (opt.isGiven("r")) {
         log.info("translate: computing runs...");
         long runs = 0;
         try {
            runs = translater.computeRuns(project.getName());
         } catch (IOException ex) {
            log.error("translate: could not create run-related files; " + ex);
            return 1;
         }
         project.setProperty("Runs", runs);
      }

      // write project file
      try {
         project.store();
      } catch (IOException ex) {
         log.error(String.format("translate: could not write project file; %s", ex));
         return 1;
      }

      // that's all
      log.info("translate: done; total time was %.1f secs.", gtimer.tocs());
      return 0;
   }

}