/*
 * Translater.java Created on 30. Januar 2007, 14:57
 */

package verjinxer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.sequenceanalysis.FastaFile;
import verjinxer.sequenceanalysis.FastaFormatException;
import verjinxer.sequenceanalysis.FastaSequence;
import verjinxer.sequenceanalysis.InvalidSymbolException;
import verjinxer.util.AnnotatedArrayFile;
import verjinxer.util.ArrayFile;
import verjinxer.util.ProjectInfo;

import com.spinn3r.log5j.Logger;

/**
 * translates a set of text or FASTA files into a byte file.
 * 
 * @author Sven Rahmann
 */
public class Translater {
   private final static Logger log = Globals.log;
   final Globals g;
   final boolean trim;
   final Alphabet alphabet, alphabet2;
   final boolean separateRCByWildcard;
   final boolean reverse;
   final boolean addrc;
   final boolean bisulfite;
   final String dnarcstring;

   public Translater(Globals g, boolean trim, Alphabet alphabet, Alphabet amap2,
         boolean separateRCByWildcard, boolean reverse, boolean addrc, boolean bisulfite,
         String dnarcstring) {

      // for now, only print the message itself (%m), nothing else

      this.g = g;
      this.trim = trim;
      this.alphabet = alphabet;
      this.alphabet2 = amap2;
      this.separateRCByWildcard = separateRCByWildcard;
      this.reverse = reverse;
      this.bisulfite = bisulfite;
      this.dnarcstring = dnarcstring;
      this.addrc = addrc;
   }

   public Translater(Globals g, Alphabet alphabet) {
      this(g, false, alphabet, null, false, false, false, false, "");
   }

   /**
    * Translates all the given files. Writes .seq .alphabet and .desc. Essentially, this initializes
    * a new project. TODO should this method be really here? should it be called differently?
    */
   public void createProject(ProjectInfo project, String[] filenames) {
      project.setProperty("NumberSourceFiles", filenames.length);
      project.setProperty("TrimmedSequences", trim);

      // determine the file types: FASTA or TEXT
      // FASTA 'f': First non-whitespace character is a '>''
      // TEXT 't': all others
      FileType[] filetype = new FileType[filenames.length];
      for (int i = 0; i < filenames.length; i++) {
         String filename = g.dir + filenames[i];
         try {
            filetype[i] = determineFileType(filename);
         } catch (IOException ex) {
            log.error("translate: could not open sequence file '%s'; %s", filename, ex);
            g.terminate(1);
         }
      }

      // open the output file stream
      log.info("translate: creating index '%s'...", project.getName());
      // use default buffer size
      AnnotatedArrayFile out = new AnnotatedArrayFile(project.getName() + FileNameExtensions.seq);
      try {
         out.openW();
      } catch (IOException ex) {
         log.warn("translate: could not create output file '%s'; %s", project.getName()
               + FileNameExtensions.seq, ex);
      }

      // process each file according to type
      for (int i = 0; i < filenames.length; i++) {
         String fname = g.dir + filenames[i];
         log.info("  processing '%s' (%s)...", fname, filetype[i]);
         if (filetype[i] == FileType.FASTA)
            translateFasta(fname, out);
         else if (bisulfite && filetype[i] == FileType.FASTA) // TODO this is never executed
            translateFastaBisulfite(fname, out);
         else if (filetype[i] == FileType.TEXT)
            translateText(fname, out);
         else
            g.terminate("translate: unsupported file type for file " + filenames[i]);
      }
      // DONE processing all files.
      try {
         out.close();
      } catch (IOException ex) {
      }
      long totallength = out.length();
      log.info("translate: translated sequence length: %d", totallength);
      if (totallength >= (2L * 1024 * 1024 * 1024))
         log.warn("translate: length %d exceeds 2 GB limit!!", totallength);
      else if (totallength >= (2L * 1024 * 1024 * 1024 * 127) / 128)
         log.warn("translate: long sequence, %d is within 99% of 2GB limit!", totallength);
      project.setProperty("Length", totallength);

      // Write the ssp array.
      g.dumpLongArray(project.getName() + FileNameExtensions.ssp, out.getSsps());
      project.setProperty("NumberSequences", out.getSsps().length);

      // Write sequence length statistics.
      long maxseqlen = 0;
      long minseqlen = Long.MAX_VALUE;
      for (long seqlen : out.getLengths()) {
         if (seqlen > maxseqlen)
            maxseqlen = seqlen;
         if (seqlen < minseqlen)
            minseqlen = seqlen;
      }
      project.setProperty("LongestSequence", maxseqlen);
      project.setProperty("ShortestSequence", minseqlen);

      // Write the descriptions
      PrintWriter descfile = null;
      try {
         descfile = new PrintWriter(project.getName() + FileNameExtensions.desc);
         for (String s : out.getDescriptions())
            descfile.println(s);
         descfile.close();
      } catch (IOException ex) {
         log.error("translate: %s%s: %s", project.getName(), FileNameExtensions.desc, ex);
         g.terminate(1);
      }

      // Write the alphabet
      PrintWriter alphabetfile = null;
      try {
         alphabetfile = new PrintWriter(project.getName() + FileNameExtensions.alphabet);
         alphabet.showSourceStrings(alphabetfile);
         alphabetfile.close();
      } catch (IOException ex) {
         g.terminate("translate: could not write alphabet: " + ex);
      }
      project.setProperty("SmallestSymbol", alphabet.smallestSymbol());
      project.setProperty("LargestSymbol", alphabet.largestSymbol());
      try {
         project.setProperty("Separator", alphabet.codeSeparator());
      } catch (InvalidSymbolException ex) {
         project.setProperty("Separator", 128); // illegal byte code 128 -> nothing
      }
      project.setProperty("LastAction", "translate");
   }

   /**
    * 
    */
   public void translateFasta(final String fname, final AnnotatedArrayFile out) {
      FastaFile f = new FastaFile(fname);
      FastaSequence fseq = null;
      ByteBuffer tr = null;
      final int appendforward = (addrc && separateRCByWildcard) ? 1 : 2;
      final int appendreverse = 2; // always append separator
      long lastbyte = 0;

      try {
         f.open();
      } catch (IOException ex) {
         log.warn("translate: skipping '%s': %s", fname, ex);
         return;
      }
      while (true) {
         try {
            fseq = f.read(); // reads one fasta sequence from file f
            if (fseq == null)
               break;
            tr = fseq.translateTo(tr, trim, alphabet, reverse, appendforward);
            lastbyte = out.writeBuffer(tr);
            if (addrc) {
               if (!separateRCByWildcard)
                  out.addInfo(fseq.getHeader(), fseq.length(), (int) (lastbyte - 1));
               tr = fseq.translateTo(tr, trim, alphabet2, true, appendreverse);
               lastbyte = out.writeBuffer(tr);
               if (separateRCByWildcard)
                  out.addInfo(fseq.getHeader(), 2 * fseq.length() + 1, (int) (lastbyte - 1));
               else
                  out.addInfo(fseq.getHeader() + " " + dnarcstring, fseq.length(),
                        (int) (lastbyte - 1));
            } else { // no reverse complement
               out.addInfo(fseq.getHeader(), fseq.length(), (int) (lastbyte - 1));
            }
         } catch (InvalidSymbolException ex) {
            log.error("translate: %s", ex);
            break;
         } catch (IOException ex) {
            log.error("translate: %s", ex);
            break;
         } catch (FastaFormatException ex) {
            log.error("translate: %s", ex);
            break;
         }
      }
      // close file
      try {
         f.close();
      } catch (IOException ex) {
         log.error("translate: %s", ex);
      }
   }

   /**
    * something with bisulfite
    * 
    * @param fname
    * @param out
    * @deprecated
    */
   void translateFastaBisulfite(final String fname, final AnnotatedArrayFile out) {
      FastaFile f = new FastaFile(fname);
      FastaSequence fseq = null;
      ByteBuffer tr = null;
      long lastbyte = 0;

      try {
         f.open();
      } catch (IOException ex) {
         log.warn("translate: skipping '%s': %s", fname, ex);
         return;
      }
      while (true) {
         try {
            fseq = f.read(); // reads one fasta sequence from file f
            if (fseq == null)
               break;
            tr = fseq.translateDNABiTo(tr, trim, false, false, 1); // nonmeth-bis-#
            lastbyte = out.writeBuffer(tr);
            tr = fseq.translateDNABiTo(tr, trim, false, true, 2); // nonmeth-cbis-$
            lastbyte = out.writeBuffer(tr);
            out.addInfo(fseq.getHeader() + " /nonmeth-bis+cbis", 2 * fseq.length() + 1,
                  (int) (lastbyte - 1));
            tr = fseq.translateDNABiTo(tr, trim, true, false, 1); // meth-bis-#
            lastbyte = out.writeBuffer(tr);
            tr = fseq.translateDNABiTo(tr, trim, true, true, 2); // meth-cbis-$
            lastbyte = out.writeBuffer(tr);
            out.addInfo(fseq.getHeader() + " /meth-bis+cbis", 2 * fseq.length() + 1,
                  (int) (lastbyte - 1));
         } catch (Exception ex) {
            log.error("translate: %s", ex);
            break;
         }
      }
      // close file
      try {
         f.close();
      } catch (IOException ex) {
         log.error("translate: %s", ex);
      }
   }

   /**
    * text file processing differs from fasta processing by the following: -- reverse complement
    * option is ignored, even for DNA text file; -- each cr/lf is replaced by whitespace (error if
    * the alphabet map does not allow whitespace); -- description is simply the filename; --
    * separator is appended (never the wildcard).
    */
   void translateText(final String fname, final AnnotatedArrayFile out) {
      ByteBuffer tr = null;
      long lastbyte = 0;
      byte appender;
      int len = 0;
      String current = null;
      BufferedReader br = null;

      try {
         br = new BufferedReader(new FileReader(fname), 512 * 1024);
         String next = br.readLine();
         while (next != null) {
            current = next;
            next = br.readLine();
            try {
               appender = (next == null) ? alphabet.codeSeparator() : alphabet.codeWhitespace();
               len += current.length() + (next == null ? 0 : 1);
               tr = alphabet.applyTo(current, tr, true, appender);
            } catch (InvalidSymbolException ex) {
               throw new IOException(ex);
            }
         }
         lastbyte = out.writeBuffer(tr);
         out.addInfo(fname, len, (int) (lastbyte - 1));
      } catch (IOException ex) {
         log.error("translate: error translating '%s': %s", fname, ex);
         g.terminate(1);
      } finally {
         try {
            if (br != null) {
               br.close();
            }
         } catch (IOException ex) {
         }
      }
   }

   /** ***************************** runs ********************************** */

   /**
    * reads translated sequence (using memory-mapping) and writes run-related files
    * 
    * .runseq: the character sequence of runs (same as original sequence, but each run condensed to
    * a single character);
    * 
    * .runlen: the length of each run (as a byte); for run lengths &gt; 127, we store -1;
    * 
    * .pos2run: pos2run[p]=r means that we are in run r at position p;
    * 
    * .run2pos: run2pos[r]=p means that run r starts at position p. The run-related files are
    * written using streams in the native byte order.
    * 
    * @param fname
    *           filename (without extension) of the sequence file
    * @return number of runs in the sequence file
    * @throws java.io.IOException
    */
   long computeRuns(final String fname) throws IOException {
      return computeRunsAF(fname);
   }

   /** compute runs using memory mapping where possible */
   private long computeRunsM(final String fname) throws IOException {
      int run = -1;
      ByteBuffer seq = new ArrayFile(fname + FileNameExtensions.seq, 0).mapR();
      ArrayFile rseq = new ArrayFile(fname + FileNameExtensions.runseq).openW();
      ArrayFile rlen = new ArrayFile(fname + FileNameExtensions.runlen).openW();
      IntBuffer p2r = new ArrayFile(fname + FileNameExtensions.pos2run, 0).mapRW().asIntBuffer();
      ArrayFile r2p = new ArrayFile(fname + FileNameExtensions.run2pos).openW();
      final int n = seq.limit();

      byte current;
      byte prev = -1;
      int start = 0;
      int len;
      for (int p = 0; p < n; p++) {
         current = seq.get();
         if (current != prev || p == 0) {
            run++;
            prev = current;
            len = p - start;
            assert len > 0 || p == 0;
            if (len > 127)
               len = -1;
            if (p != 0)
               rlen.writeByte((byte) len);
            start = p;
            rseq.writeByte(current);
            r2p.writeInt(p);
         }
         p2r.put(run);
      }
      run++; // number of runs
      len = n - start;
      assert (len > 0);
      if (len > 127)
         len = -1;
      // while 'len' is an int, we only write the least significant byte!
      rlen.writeByte((byte) len);
      r2p.writeInt(n); // write sentinel
      rseq.close();
      rlen.close();
      r2p.close();
      seq = null;

      assert (4 * run == r2p.length() - 4) : String.format(
            "n=%d, runs=%d, rseq=%d. 4*run=%d, run2pos=%d, pos2run=%d", n, run, rseq.length(),
            4 * run, r2p.length(), p2r.position());
      return run;
   }

   /** compute runs using array files for writing, mmap only for reading */
   private long computeRunsAF(final String fname) throws IOException {
      int run = -1;
      ByteBuffer seq = new ArrayFile(fname + FileNameExtensions.seq, 0).mapR();
      ArrayFile rseq = new ArrayFile(fname + FileNameExtensions.runseq).openW();
      ArrayFile rlen = new ArrayFile(fname + FileNameExtensions.runlen).openW();
      ArrayFile p2r = new ArrayFile(fname + FileNameExtensions.pos2run).openW();
      ArrayFile r2p = new ArrayFile(fname + FileNameExtensions.run2pos).openW();
      final int n = seq.limit();
      byte current;
      byte prev = -1;
      int start = 0;
      int len;
      for (int p = 0; p < n; p++) {
         current = seq.get();
         if (current != prev || p == 0) {
            run++;
            prev = current;
            len = p - start;
            assert len > 0 || p == 0;
            if (len > 127)
               len = -1;
            if (p != 0)
               rlen.writeByte((byte) len);
            start = p;
            rseq.writeByte(current);
            r2p.writeInt(p);
         }
         p2r.writeInt(run);
      }
      run++; // number of runs
      len = n - start;
      assert len > 0;
      if (len > 127)
         len = -1;
      // while 'len' is an int, we only write the least significant byte!
      rlen.writeByte((byte) len); 
      r2p.writeInt(n); // write sentinel
      rseq.close();
      rlen.close();
      p2r.close();
      r2p.close();
      seq = null;

      assert 4 * run == r2p.length() - 4 : String.format(
            "n=%d, runs=%d, rseq=%d. 4*run=%d, run2pos=%d, pos2run=%d", n, run, rseq.length(),
            4 * run, r2p.length(), p2r.length());
      return run;
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
