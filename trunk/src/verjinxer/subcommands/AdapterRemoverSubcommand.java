package verjinxer.subcommands;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.spinn3r.log5j.Logger;
import static verjinxer.Globals.programname;
import verjinxer.AdapterRemover;
import verjinxer.Globals;
import verjinxer.Project;
import verjinxer.Translater;
import verjinxer.sequenceanalysis.SequenceWriter;
import verjinxer.sequenceanalysis.Sequences;
import verjinxer.sequenceanalysis.alignment.BottomAndRightEdges;
import verjinxer.sequenceanalysis.alignment.IAligner;
import verjinxer.sequenceanalysis.alignment.SemiglobalAligner;
import verjinxer.sequenceanalysis.alignment.TopAndLeftEdges;
import verjinxer.util.IllegalOptionException;
import verjinxer.util.Options;
import verjinxer.util.TicToc;

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
      log.info("Usage:  %s rmadapt [options] <sequence> <outProject> <adapters as FASTA file>", programname); // TODO verbalize usage better
      log.info("Reads a FASTA file, finds and removes adapters,");
      log.info("and writes the changed sequence to outProject.");
      log.info("When finished, statistics are printed to standard output (not yet implemented).");
      log.info("");
      log.info("  -e error_rate   Maximum error rate (errors divided by length of matching region)");
      log.info("  -p length       Print the found alignments if they are longer than length (not yet implemented).");
      log.info("  -n <count>      Try to remove adapters at most <count> times (not yet implemented)");
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
      TicToc totalTimer = new TicToc();
      Options opt = new Options("e:,p:,n:");
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
      double max_error_rate = 2.2 / 18;
      int times = 1;

      if (opt.isGiven("p")) {
         min_print_align_length = Integer.parseInt(opt.get("p"));
      }
      if (opt.isGiven("n")) {
         times = Integer.parseInt(opt.get("n"));
      }
      if (opt.isGiven("e")) {
         String[] tmp = opt.get("e").split("/");
         if (tmp.length == 1) {
            max_error_rate = Double.parseDouble(tmp[0]);
         } else {
            max_error_rate = Double.parseDouble(tmp[0]) / Double.parseDouble(tmp[1]);
         }
         log.info("Maximum error rate: %s", max_error_rate);
      }

      Project sequenceProject = null, targetProject = null;
      final File sequenceProjectFile = new File(args[0]);
      final File targetProjectFile = new File(args[1]);
      final File[] adapterFiles = new File[args.length - 2];
      for (int i = 0; i < adapterFiles.length; i++) {
         adapterFiles[i] = new File(args[i + 2]);
      }
      try {
         sequenceProject = Project.createFromFile(sequenceProjectFile);
         targetProject = Project.createFlatCopy(sequenceProjectFile, targetProjectFile);
      } catch (IOException e) {
         log.error("rmadapt: cannot read project files; %s", e);
         return 1;
      }

      TicToc subTimer = new TicToc();
      log.info("rmadapt: start adapter translation.");
      // translate adapters
      Sequences adapters = Sequences.createEmptySequencesInMemory();
      Translater translater = new Translater(null, targetProject.readAlphabet());
      for (int i = 0; i < adapterFiles.length; i++) {
         translater.translateFasta(adapterFiles[i], adapters);
      }
      log.info("rmadapt: done; time for adapter translation was %.1f secs.", subTimer.tocs());

      final boolean colorspace = sequenceProject.getBooleanProperty("ColorSpaceAlphabet");
      
      // create the aligner to use
      SemiglobalAligner aligner = new SemiglobalAligner();
      aligner.setBeginLocations(new TopAndLeftEdges());
      aligner.setEndLocations(new BottomAndRightEdges());
      
      SequenceWriter sequenceWriter = null;
      try {
         sequenceWriter = new SequenceWriter(targetProject);
      } catch (IOException ex) {
         log.error("rmadapt: could not create output files for cutted sequences; %s", ex);
         return 1;
      }
      
      subTimer.tic();
      log.info("rmadapt: start to cut the reads.");
      AdapterRemover adapterRemover = new AdapterRemover(colorspace, times, max_error_rate,
            min_print_align_length, aligner);
      adapterRemover.cutAndWriteSequences(sequenceProject, adapters, sequenceWriter);
      log.info("rmadapt: done; time to cut the reads was %.1f secs.", subTimer.tocs());

      // Store the whole target project
      try {
         sequenceWriter.store(); // stores seq, ssp and desc
      } catch (IOException ex) {
         log.error("rmadapt: could not store cutted sequences; %s", ex);
         return 1;
      }
      long totallength = sequenceWriter.length();
      targetProject.setProperty("Length", totallength);
      targetProject.setProperty("NumberSequences", sequenceWriter.getNumberSequences());

      // Write sequence length statistics.
      targetProject.setProperty("LongestSequence", sequenceWriter.getMaximumLength());
      targetProject.setProperty("ShortestSequence", sequenceWriter.getMinimumLength());

      targetProject.setProperty("LastAction", "rmadapt");
         
      log.info("rmadapt: done; total time was %.1f secs.", totalTimer.tocs());
      
      return 0;
   }

}

