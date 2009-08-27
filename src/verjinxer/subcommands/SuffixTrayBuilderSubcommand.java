package verjinxer.subcommands;

import static verjinxer.Globals.programname;

import java.io.File;
import java.io.IOException;

import com.spinn3r.log5j.Logger;

import verjinxer.Globals;
import verjinxer.Project;
import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.sequenceanalysis.Sequences;
import verjinxer.util.FileTypes;
import verjinxer.util.HugeByteArray;
import verjinxer.util.IllegalOptionException;
import verjinxer.util.Options;
import verjinxer.util.StringUtils;
import verjinxer.util.TicToc;

public class SuffixTrayBuilderSubcommand implements Subcommand {
   private static final Logger log = Globals.getLogger();
   final Globals g;
   
   public SuffixTrayBuilderSubcommand(Globals g) {
      this.g = g;
   }

   @Override
   public void help() {
      log.info("Usage:%n  %s suffix [options] Indexnames...", programname);
      log.info("Builds the suffix tray (tree plus array) of a .seq file;");
      log.info("writes %s, %s (incl. variants 1,1x,2,2x).", FileTypes.POS, FileTypes.LCP);
      log.info("Options:");
      log.info("  -m, --method  <id>    select construction method, where <id> is one of:");
      log.info("      L%n" + "      R%n" + "      minLR%n" + "      bothLR%n" + "      bothLR2");
      log.info("  -l, --lcp[2|1]        build lcp array using int|short|byte");
      log.info("  -c, --check           additionally check index integrity");
      log.info("  -C, --onlycheck       ONLY check index integrity");
      log.info("  -b, --bigsuffix       build a 64-bit suffix tray");
   }

   @Override
   public int run(String[] args) {
      Globals.cmdname = "suffixtray";
      int returnvalue = 0;
      String action = "suffixtray \"" + StringUtils.join("\" \"", args) + "\"";

      Options opt = new Options(
            "c=check,C=onlycheck,m=method:,l=lcp=lcp4,lcp1,lcp2,b=bigsuffix");
      try {
         args = opt.parse(args);
      } catch (IllegalOptionException ex) {
         Globals.terminate("suffixtray: " + ex);
      }
      if (args.length == 0) {
         help();
         log.info("suffixtray: no index given");
         return 1;
      }
      
      // Determine check?, onlycheck?, bigsuffix options
      final boolean check = (opt.isGiven("c"));
      final boolean onlycheck = (opt.isGiven("C"));
      final boolean bigsuffix = (opt.isGiven("b"));
      int dolcp = 0;
      if (opt.isGiven("l")) {
         dolcp += 4;
      }
      if (opt.isGiven("lcp2")) {
         dolcp += 2;
      }
      if (opt.isGiven("lcp1")) {
         dolcp += 1;
      }
       
      // Get indexname and di
      File indexname = new File(args[0]);
      if (args.length > 1)
         log.warn("suffixtray: ignoring all arguments except first '%s'", args[0]);

      Project project;
      try {
         project = Project.createFromFile(indexname);
      } catch (IOException ex) {
         log.warn("cannot read project file.");
         return 1;
      }
      g.startProjectLogging(project);
      final int asize = project.getIntProperty("LargestSymbol") + 1;
      
      // load alphabet map and text
      final Alphabet alphabet = project.readAlphabet();
      HugeByteArray bigSequence = null;
      Sequences sequence = null;
      long n;
      if (bigsuffix) {
         bigSequence = g.slurpHugeByteArray(project.makeFile(FileTypes.SEQ));
         n = bigSequence.length;
      } else {
         sequence = project.readSequences();
         n = sequence.length();
      }
      
      //TODO
//      if (onlycheck) {
//         if (bigsuffix) {
//            returnvalue = BigSuffixTrayChecker.checkpos(project);
//         } else {
//            returnvalue = SuffixTrayChecker.checkpos(project);
//         }
//         g.stopplog();
//         return returnvalue;
//      }
      project.setProperty("SuffixAction", action);
      project.setProperty("LastAction", "suffixtray");
      project.setProperty("AlphabetSize", asize);
      
      final String method = (opt.isGiven("m") ? opt.get("m") : "L"); // default method
      log.info("suffixtray: constructing pos using method '%s'...", method);
      // TODO assert (alphabet.isSeparator(s[n - 1])) : "last character in text needs to be a separator";
      TicToc timer = new TicToc();
      long steps;
      //TODO
      //try {
//      if (bigsuffix) {
//         BigSuffixTrayBuilder
//         steps = BigSuffixTrayBuilder.build(project, method);
//      } else {
//         steps = SuffixTrayBuilder.build(project, method);
//      }
//      //}catch (???) {
//      // log.error("suffixtray: Unsupported construction method '" + method + "'!");
//      // return 1;
//      //}
//      log.info("suffixtray: pos completed after %.1f secs using %d steps (%.2f/char)",
//            timer.tocs(), steps, (double) steps / n);
//      project.setProperty("SuffixTrayMethod", method);
//      project.setProperty("SuffixTraySteps", steps);
//      project.setProperty("SuffixTrayStepsPerChar", (double) steps / n);
      
//      TODO
//      if (check) {
//         timer.tic();
//         log.info("suffixcheck: checking pos...");
//         //try {
//         if (bigsuffix) {
//            returnvalue = BigSuffixTrayChecker.checkpos(method);
//         } else {
//            returnvalue = SuffixTrayChecker.checkpos(method);
//         }
//         //}catch (???) {
//         // log.error("suffixcheck: Unsupported construction method '" + method + "'!");
//         // return 1;
//         //}
//         if (returnvalue == 0) {
//            log.info("suffixcheck: pos looks OK!");
//         }
//         log.info("suffixcheck: done after %.1f secs", timer.tocs());
//      }
      
      //TODO
//      if (returnvalue == 0) {
//         timer.tic();
//         File fpos = project.makeFile(FileTypes.POS);
//         log.info("suffixtray: writing '%s'...", fpos);
//         //try {
//         if (bigsuffix) {
//            returnvalue = BigSuffixTrayWriter.write(method);
//         } else {
//            returnvalue = SuffixTrayWriter.write(method);
//         }
//         //}catch (???) {
//         // log.error("suffixtray: Unsupported construction method '" + method + "'!");
//         // return 1;
//         //}
//         log.info("suffixtray: writing took %.1f secs; done.", timer.tocs());
//      }
      
      // TODO
      // do lcp if desired
//      if (dolcp > 0 && returnvalue == 0) {
//         timer.tic();
//         File flcp = project.makeFile(FileTypes.LCP);
//         log.info("suffixtray: computing lcp array...");
//         if (method.equals("L"))
//            lcp_L(flcp, dolcp);
//         else if (method.equals("R"))
//            lcp_L(flcp, dolcp);
//         else if (method.equals("minLR"))
//            lcp_L(flcp, dolcp);
//         else if (method.equals("bothLR"))
//            lcp_bothLR(flcp, dolcp);
//         else if (method.equals("bothLR2"))
//            lcp_L(flcp, dolcp);
//         else
//            g.terminate("suffixtray: Unsupported construction method '" + method + "'!");
//         log.info("suffixtray: lcp computation and writing took %.1f secs; done.", timer.tocs());
//         project.setProperty("lcp1Exceptions", lcp1x);
//         project.setProperty("lcp2Exceptions", lcp2x);
//         project.setProperty("lcp1Size", n + 8 * lcp1x);
//         project.setProperty("lcp2Size", 2 * n + 8 * lcp2x);
//         project.setProperty("lcp4Size", 4 * n);
//         project.setProperty("lcpMax", maxlcp);
//      }

      // write project data
      try {
         project.store();
      } catch (IOException ex) {
         log.warn("suffix: could not write %s (%s)!", project.getFile().getPath(), ex);
         return 1;
      }
      g.stopplog();
      return returnvalue;
   }

}
