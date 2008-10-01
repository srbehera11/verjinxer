package verjinxer;

import static verjinxer.Globals.extalph;
import static verjinxer.Globals.extdesc;
import static verjinxer.Globals.extlog;
import static verjinxer.Globals.extpos2run;
import static verjinxer.Globals.extprj;
import static verjinxer.Globals.extrun2pos;
import static verjinxer.Globals.extrunlen;
import static verjinxer.Globals.extrunseq;
import static verjinxer.Globals.extseq;
import static verjinxer.Globals.extssp;
import static verjinxer.Globals.programname;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import verjinxer.sequenceanalysis.AlphabetMap;
import verjinxer.sequenceanalysis.InvalidSymbolException;
import verjinxer.util.AnnotatedArrayFile;
import verjinxer.util.IllegalOptionException;
import verjinxer.util.Options;
import verjinxer.util.StringUtils;
import verjinxer.util.TicToc;

public class TranslaterSubcommand implements Subcommand {
   final Globals g;

   TranslaterSubcommand(Globals g) {
      this.g = g;
   }

   /**
    * prints help on usage and options
    */
   public void help() {
      g.logmsg("Usage:%n  %s translate [options] <TextAndFastaFiles...>%n", programname);
      g.logmsg("translates one or more text or FASTA files, using an alphabet map;%n");
      g.logmsg("creates %s, %s, %s, %s, %s;%n", extseq, extdesc, extalph, extssp, extprj);
      g.logmsg("with option -r, also creates %s, %s, %s, %s.%n", extrunseq, extrunlen, extrun2pos,
            extpos2run);
      g.logmsg("Options:%n");
      g.logmsg("  -i, --index <name>   name of index files [first filename]%n");
      g.logmsg("  -t, --trim           trim non-symbol characters at both ends%n");
      g.logmsg("  -a, --amap  <file>   filename of alphabet map%n");
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
    * @param args the command line arguments
    */
   public static void main(String[] args) {
     new TranslaterSubcommand(new Globals()).run(args);
   }

   @Override
   public int run(String[] args) {
      TicToc gtimer = new TicToc();
      g.cmdname = "translate";
      Properties prj = new Properties();
      prj.setProperty("TranslateAction", "translate \"" + StringUtils.join("\" \"", args) + "\"");

      Options opt = new Options(
            "i=index=indexname:,t=trim,a=amap:,dna,rc=rconly,dnarc:,dnabi,masked,protein,r=run=runs");
      try {
         args = opt.parse(args);
      } catch (IllegalOptionException e) {
         g.terminate(e.toString());
      }

      if (args.length == 0) {
         help();
         g.logmsg("translate: no files given%n");
         g.terminate(0);
      }
      prj.setProperty("NumberSourceFiles", Integer.toString(args.length));

      // determine trimming
      boolean trim = opt.isGiven("t");
      prj.setProperty("TrimmedSequences", Boolean.toString(trim));

      // determine the name of the index
      String outname;
      if (opt.isGiven("i"))
         outname = opt.get("i");
      else { // take base name of first FASTA file
         outname = new File(args[0]).getName();
         int lastdot = outname.lastIndexOf('.');
         if (lastdot >= 0)
            outname = outname.substring(0, lastdot);
      }
      outname = g.outdir + outname;
      g.startplog(outname + extlog, true); // start new project log

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

      AlphabetMap amap = null;
      if (opt.isGiven("a"))
         amap = g.readAlphabetMap(g.dir + opt.get("a"));
      if (opt.isGiven("dna") || opt.isGiven("dnarc"))
         amap = opt.isGiven("masked") ? AlphabetMap.maskedDNA() : AlphabetMap.DNA();

      boolean reverse = false;
      if (opt.isGiven("rc")) {
         reverse = true;
         amap = opt.isGiven("masked") ? AlphabetMap.maskedcDNA() : AlphabetMap.cDNA();
      }
      AlphabetMap amap2 = null;
      boolean addrc = false;
      String dnarcstring = null;
      boolean separateRCByWildcard = false;
      if (opt.isGiven("dnarc")) {
         amap2 = opt.isGiven("masked") ? AlphabetMap.maskedcDNA() : AlphabetMap.cDNA();
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
         amap = AlphabetMap.DNA(); // do translation on-line
      }
      if (opt.isGiven("protein"))
         amap = AlphabetMap.Protein();

      if (amap == null)
         g.terminate("translate: no alphabet map given; use one of {-a, --dna, --rconly, --dnarc, --protein}.");

      
      // determine the file types: FASTA or TEXT
      // FASTA 'f': First non-whitespace character is a '>''
      // TEXT 't': all others
      FileType[] filetype = new FileType[args.length];
      for (int i = 0; i < args.length; i++) {
         String filename = g.dir + args[i];
         try {
            filetype[i] = determineFileType(filename);
         } catch (IOException e) {
            g.terminate("translate: could not open sequence file '" + filename + "'; " + e.toString());
         }
      }

      // open the output file stream
      g.logmsg("translate: creating index '%s'...%n", outname);
      AnnotatedArrayFile out = new AnnotatedArrayFile(outname + extseq); // use default buffer
      // size
      try {
         out.openW();
      } catch (IOException ex) {
         g.warnmsg("translate: could not create output file '%s'; %s", outname + extseq,
               ex.toString());
      }

      Translater translater = new Translater(g, trim, amap, amap2, separateRCByWildcard,
            reverse, addrc, bisulfite, dnarcstring);
      // process each file according to type
      for (int i = 0; i < args.length; i++) {
         String fname = g.dir + args[i];
         g.logmsg("  processing '%s' (%s)...%n", fname, filetype[i].toString());
         if (filetype[i] == FileType.FASTA)
            translater.processFasta(fname, out);
         else if (bisulfite && filetype[i] == FileType.FASTA) // TODO this is never executed
            translater.processFastaB(fname, out);
         else if (filetype[i] == FileType.TEXT)
            translater.processText(fname, out);
         else
            g.terminate("translate: unsupported file type for file " + args[i]);
      }
      // DONE processing all files.
      try {
         out.close();
      } catch (IOException ex) {
      }
      long totallength = out.length();
      g.logmsg("translate: translated sequence length: %d%n", totallength);
      if (totallength >= (2L * 1024 * 1024 * 1024))
         g.warnmsg("translate: length %d exceeds 2 GB limit!!%n", totallength);
      else if (totallength >= (2L * 1024 * 1024 * 1024 * 127) / 128)
         g.warnmsg("translate: long sequence, %d is within 99% of 2GB limit!%n", totallength);
      prj.setProperty("Length", Long.toString(totallength));

      // Write the ssp array.
      g.dumpLongArray(outname + extssp, out.getSsps());
      prj.setProperty("NumberSequences", Integer.toString(out.getSsps().length));

      // Write sequence length statistics.
      long maxseqlen = 0;
      long minseqlen = Long.MAX_VALUE;
      for (long seqlen : out.getLengths()) {
         if (seqlen > maxseqlen)
            maxseqlen = seqlen;
         if (seqlen < minseqlen)
            minseqlen = seqlen;
      }
      prj.setProperty("LongestSequence", Long.toString(maxseqlen));
      prj.setProperty("ShortestSequence", Long.toString(minseqlen));

      // Write the descriptions
      PrintWriter descfile = null;
      try {
         descfile = new PrintWriter(outname + extdesc);
         for (String s : out.getDescriptions())
            descfile.println(s);
         descfile.close();
      } catch (IOException ex) {
         g.warnmsg("translate: %s%s: %s%n", outname, extdesc, ex.toString());
         g.terminate(1);
      }

      // Write the alphabet and project file
      PrintWriter alfile = null;
      try {
         // alfile = new PrintWriter(outname + extamap);
         // amap.showImage(alfile);
         // alfile.close();
         alfile = new PrintWriter(outname + extalph);
         amap.showSourceStrings(alfile);
         alfile.close();
      } catch (IOException ex) {
         g.terminate("translate: could not write alphabet: " + ex.toString());
      }

      prj.setProperty("SmallestSymbol", Integer.toString(amap.smallestSymbol()));
      prj.setProperty("LargestSymbol", Integer.toString(amap.largestSymbol()));
      prj.setProperty("LastAction", "translate");
      try {
         prj.setProperty("Separator", Byte.toString(amap.codeSeparator()));
      } catch (InvalidSymbolException ex) {
         prj.setProperty("Separator", "128"); // illegal byte code 128 -> nothing
      }
      g.logmsg("translate: finished translation after %.1f secs.%n", gtimer.tocs());

      // compute runs
      if (opt.isGiven("r")) {
         g.logmsg("translate: computing runs...%n");
         long runs = 0;
         try {
            runs = translater.computeRuns(outname);
         } catch (IOException ex) {
            g.terminate("translate: could not create run-related files; " + ex.toString());
         }
         prj.setProperty("Runs", Long.toString(runs));
      }

      // write project file
      try {
         g.writeProject(prj, outname + extprj);
      } catch (IOException ex) {
         g.terminate(String.format("translate: could not write project file; %s", ex.toString()));
      }

      // that's all
      g.logmsg("translate: done; total time was %.1f secs.%n", gtimer.tocs());
      return 0;
   }

   private FileType determineFileType(String filename) throws IOException {
      int ch = ' ';
   
      FileReader reader = new FileReader(filename);
      for (ch = reader.read(); ch != -1 && Character.isWhitespace(ch); ch = reader.read()) {
      }
      reader.close();
      if (ch == '>')
         return FileType.FASTA;
      else
         return FileType.TEXT;
   }
   
   private enum FileType {
      FASTA, TEXT
   }
}
