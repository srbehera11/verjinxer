package verjinxer.subcommands;

import static verjinxer.Globals.programname;

import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;

import com.spinn3r.log5j.Logger;

import verjinxer.BWTBuilder;
import verjinxer.Globals;
import verjinxer.Project;
import verjinxer.SuffixTrayBuilder;
import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.sequenceanalysis.Sequences;
import verjinxer.sequenceanalysis.SuffixXorDLL;
import verjinxer.util.FileTypes;
import verjinxer.util.StringUtils;
import verjinxer.util.TicToc;

/**
 * @author Markus Kemmerling
 */
public class BWTBuilderSubcommand implements Subcommand {

   private static final Logger log = Globals.getLogger();
   private Globals g;
   private static final String commandname = "bwt-build";
   
   @Override
   public void help() {
      log.info("Usage:%n  %s bwtbuild [options] Indexnames...", programname);
      log.info("Builds the bwt of a .seq file;");
      log.info("writes %s.", FileTypes.BWT);
      log.info("Options:");
   }

   @Override
   public int run(String[] args) {
      int returnvalue = 0;
      String action = commandname + " \"" + StringUtils.join("\" \"", args) + "\"";
      
//      Options opt = new Options("c=check,C=onlycheck,m=method:,l=lcp=lcp4,lcp1,lcp2,b=bigsuffix");
//      try {
//         args = opt.parse(args);
//      } catch (IllegalOptionException ex) {
//         log.error("suffixtray: " + ex);
//         return 1;
//      }
      
      if (args.length == 0) {
         help();
         log.info("%s: no index given.", commandname);
         return 1;
      }
      
      TicToc totalTimer = new TicToc();
      
      // Loop through all files
      for (int i = 0; i < args.length; i++) {
         File projectFile = new File(args[i]);
         Project project;
         try {
            project = Project.createFromFile(projectFile);
         } catch (IOException ex) {
            log.error("%s: cannot read project file '%s' (%s).", commandname, projectFile, ex);
            returnvalue = 1;
            continue;
         }
         g.startProjectLogging(project);
         byte[] bwt;
         if (project.makeFile(FileTypes.POS).exists()) {
            //read suffix array and build BWT from that

            log.info("%s: reading suffix array from disc.", commandname);
            totalTimer.tic();
            IntBuffer pos = null;
            try {
               pos = project.readSuffixArray();
            } catch (IOException e) {
               log.error("%s: could not read suffix array from disc: %s", commandname, e);
               returnvalue = 1;
               continue;
            }
            log.info("%s:reading took %.1f secs %.1f secs.", commandname, totalTimer.tocs());
            
            log.info("%s: reading sequence from disc.", commandname);
            totalTimer.tic();
            final Sequences sequence = project.readSequences();
            log.info("%s:reading took %.1f secs %.1f secs.", commandname, totalTimer.tocs());
            
            
            log.info("%s: constructing bwt using suffix array.", commandname);
            totalTimer.tic();
            bwt = BWTBuilder.build(pos, sequence);
         } else {
            //create a suffix dll and build BWT-Index from that
            log.info("%s: reading sequence and alphabet from disc.", commandname);
            totalTimer.tic();
            final Sequences sequence = project.readSequences();
            final Alphabet alphabet = project.readAlphabet();
            log.info("%s:reading took %.1f secs %.1f secs.", commandname, totalTimer.tocs());

            final String method = "bothLR";
            log.info("%s: constructing suffix list using method '%s'...", commandname, method);
            final SuffixTrayBuilder builder = new SuffixTrayBuilder(sequence, alphabet);
            builder.build(method); //WARNING: change the method and you must change the type cast in the next line!
            assert (builder.getSuffixDLL() instanceof SuffixXorDLL);
            final SuffixXorDLL suffixDLL = (SuffixXorDLL)builder.getSuffixDLL(); // type cast is okay because I used method 'bothLR' to build the list
            log.info("%s: pos completed after %.1f secs using %d steps (%.2f/char)", commandname,
                  totalTimer.tocs(), builder.getSteps(), (double) builder.getSteps() / sequence.length());
            
            log.info("%s: constructing bwt using suffix list.", commandname);
            totalTimer.tic();
            bwt = BWTBuilder.build(suffixDLL);
            // suffixDLL is not stored on disc. If the user really need it, he can run SuffixTrayBuilderSubcommand explicitly.
         }
         log.info("%s: bwt completed after %.1f secs.", commandname, totalTimer.tocs());
         
         // write bwt to disc
         File bwtFile = project.makeFile(FileTypes.BWT);
         log.info("%s: writing '%s'...", commandname, bwtFile);
         totalTimer.tic();
         g.dumpByteArray(bwtFile, bwt);
         log.info("%s: writing took %.1f secs; done.", commandname, totalTimer.tocs());
         
         project.setProperty("BWTAction", action);
         project.setProperty("LastAction", commandname);
         // write project data
         try {
            project.store();
         } catch (IOException ex) {
            log.warn("%s: could not write %s (%s)!", commandname, project.getFile().getPath(), ex);
            returnvalue = 1;
            continue;
         }
         
         g.stopplog();
      }
      
      return returnvalue;
   }

}
