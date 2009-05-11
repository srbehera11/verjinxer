package verjinxer.subcommands;

import com.spinn3r.log5j.Logger;
import static verjinxer.Globals.programname;
import verjinxer.Globals;


public class AdapterRemoverSubcommand implements Subcommand {

   final Globals g;
   private static final Logger log = Globals.getLogger();

   public AdapterRemoverSubcommand(Globals g) {
      this.g = g;
   }
   
   /**
    * prints help on usage and options
    */
   @Override
   public void help() {
      log.info("Usage:  %s translate [options] <TextAndFastaFiles...>", programname);
      log.info("translates one or more text or FASTA files, using an alphabet map;");

      log.info("Options:"); //TODO explain CSFASTA handling
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
   }

   /**
    * @param args
    *           the command line arguments
    */
   public static void main(String[] args) {
      System.exit(new AdapterRemoverSubcommand(new Globals()).run(args));
   }

   @Override
   public int run(String[] args) {
      // TODO Auto-generated method stub
      return 0;
   }
}

