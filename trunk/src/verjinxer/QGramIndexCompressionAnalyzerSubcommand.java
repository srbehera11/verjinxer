package verjinxer;

import static verjinxer.Globals.programname;

import java.io.IOException;

import verjinxer.sequenceanalysis.QGramIndex;
import verjinxer.util.ProjectInfo;
import verjinxer.util.TicToc;
import com.spinn3r.log5j.Logger;

/**
 * @author Markus Kemmerling
 */
public class QGramIndexCompressionAnalyzerSubcommand implements Subcommand {

   private static final Logger log = Globals.log;
   private static final String command = "analyzer";
   final Globals g;

   public QGramIndexCompressionAnalyzerSubcommand(Globals g) {
      this.g = g;
   }

   /*
    * prints help on usage and options
    */
   @Override
   public void help() {
      log.info("Usage:  %s %s <projectname>", programname, command);
      log.info("analyzes the index of the given project how it can be compressed;");
   }

   @Override
   public int run(String[] args) {
      TicToc timer = new TicToc();
      g.cmdname = command;

      if (args.length == 0) {
         help();
         log.info("%s: no project given", command);
         return 0;
      }

      final String projectname = g.dir + args[0];
      ProjectInfo project;
      try {
         project = ProjectInfo.createFromFile(projectname);
      } catch (IOException ex) {
         log.error("%s: cannot read project files.", command);
         return 1;
      }

      QGramIndex qgramindex;
      try {
         qgramindex = new QGramIndex(project);
      } catch (IOException e) {
         log.error("%s: cannot read index files.", command);
         return 1;
      }

      QGramIndexCompressionAnalyzer compressionAnalyzer = new QGramIndexCompressionAnalyzer(
            qgramindex);
      compressionAnalyzer.setSequenceLength(project.getIntProperty("Length"));
      compressionAnalyzer.analyze();
      compressionAnalyzer.printStatistic();

      log.info("%s: done; total time was %.1f secs.", command, timer.tocs());
      return 0;
   }

}
