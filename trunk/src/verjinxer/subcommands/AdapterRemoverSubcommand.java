package verjinxer.subcommands;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.spinn3r.log5j.Logger;
import static verjinxer.Globals.programname;
import verjinxer.Globals;
import verjinxer.Project;
import verjinxer.util.IllegalOptionException;
import verjinxer.util.Options;

/**
 * 
 * @author Markus Kemmerling
 */
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
      log.info("Usage:  %s rmadapt [options] <sequence> <outProject> <adapters as FASTA files>", programname); // TODO verbalize usage better
                                                                                                               // TODO FASTA files must be translated with alphabet of project
                                                                                                               // TODO if sequence has quality files, this must be copied into putProject and cut like the sequence. For colorspace one more value must be cut (see python code).
      log.info("Reads a FASTA file, finds and removes adapters,");
      log.info("and writes the changed sequence to outfile.");
      log.info("When finished, statistics are printed to standard output.");
      log.info("");
      log.info("  -e error_rate   Maximum error rate (errors divided by length of matching region)");
      log.info("  -p length       Print the found alignments if they are longer than length.");
      log.info("  -c              Colorspace mode: Removes first nucleotide; trims adapter correctly.");
      log.info("  -n <count>      Try to remove adapters at most <count> times");
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
      Options opt = new Options("e:,o:,p:,a:,c,n:,r:,reverse,m:,q:");
      try {
         args = opt.parse(args);
      } catch (IllegalOptionException ex) {
         log.error("%s", ex);
         return 1;
      }
      
      if (args.length < 1) {
         help();
         log.error("rmadapt: sequence file must be specified.");
         return 1;
      }
      
      int min_print_align_length = -1;
      boolean show_progress = false;
      double max_error_rate = -1;
      boolean colorspace = false;
      int times = 1; //TODO Parameter?
      
      Project sequenceProject = null;
      try {
         sequenceProject = Project.createFromFile(new File(args[0]));
      } catch (IOException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      
      if (opt.isGiven("p")) {
         min_print_align_length = Integer.parseInt(opt.get("p"));
      }
      if (opt.isGiven("c")) {
         colorspace = true;
      }
      
      
      return 0;
   }
}

