/*
 * Translater.java Created on 30. Januar 2007, 14:57
 */

package verjinxer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.StringTokenizer;

import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.sequenceanalysis.FastaFile;
import verjinxer.sequenceanalysis.FastaFormatException;
import verjinxer.sequenceanalysis.FastaSequence;
import verjinxer.sequenceanalysis.InvalidSymbolException;
import verjinxer.sequenceanalysis.SequenceWriter;
import verjinxer.sequenceanalysis.Sequences;
import verjinxer.util.ArrayFile;
import verjinxer.util.FileUtils;
import verjinxer.util.FileTypes;

import com.spinn3r.log5j.Logger;

/**
 * translates a set of text or FASTA files into a byte file.
 * 
 * @author Sven Rahmann
 */
public class Translater {
   private final static Logger log = Globals.getLogger();
   final Globals g;
   final boolean trim;
   final Alphabet alphabet, alphabet2;
   final boolean separateRCByWildcard;
   final boolean reverse;
   final boolean addrc;
   final boolean bisulfite;
   final String dnarcstring;
   final boolean colorspace;
   final File csfastaQualityFile;

   /**
    * @param csfastaQualityFile
    *           File with quality values for each character. Only has an effect of CSFASTA files.
    *           May be null.
    */
   public Translater(Globals g, boolean trim, Alphabet alphabet, Alphabet amap2,
         boolean separateRCByWildcard, boolean reverse, boolean addrc, boolean bisulfite,
         String dnarcstring, final boolean colorspace, final File csfastaQualityFile) {

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
      this.colorspace = colorspace;
      this.csfastaQualityFile = csfastaQualityFile;
   }

   public Translater(Globals g, Alphabet alphabet) {
      this(g, false, alphabet, null, false, false, false, false, "", false, null);
   }

   /**
    * Translates all the given files. Writes .seq .alphabet and .desc. Essentially, this initializes
    * a new project. TODO should this method be really here? should it be called differently?
    */
   public void translateFilesAndCreateProject(Project project, File[] files) {
      project.setProperty("NumberSourceFiles", files.length);
      project.setProperty("TrimmedSequences", trim);
      project.setProperty("ColorSpaceAlphabet", colorspace);

      // determine the file types: FASTA or TEXT
      // FASTA 'f': First non-whitespace character is a '>''
      // TEXT 't': all others
      FileTypes[] filetype = new FileTypes[files.length];
      for (int i = 0; i < files.length; i++) {
         filetype[i] = FileUtils.determineFileType(files[i]);
      }

      // open the output file stream
      log.info("translate: creating index '%s'...", project.getName());
      // use default buffer size
      SequenceWriter sequence = null;
      try {
         sequence = new SequenceWriter(project);
      } catch (IOException ex) {
         log.warn("translate: could not create output file '%s'; %s",
               project.makeFile(FileTypes.SEQ), ex);
      }

      // process each file according to type
      for (int i = 0; i < files.length; i++) {
         File file = files[i];
         log.info("  processing '%s' (%s)...", file, filetype[i]);
         // if (filetype[i] == FileType.FASTA && alphabet.getName().equals("color space"))
         // TODO should this action depend on filetype and alphabet???
         // TODO translate to CSFASTA or in a sequence???
         if (filetype[i] == FileTypes.FASTA) {
            if (colorspace) {
               translateFastaFromDNA2CS(file, sequence);
            } else {
               translateFasta(file, sequence);
            }
         } else if (filetype[i] == FileTypes.CSFASTA)
            translateCSFasta(file, sequence);
         else if (bisulfite && filetype[i] == FileTypes.FASTA) // TODO this is never executed
            translateFastaBisulfite(file, sequence);
         else if (filetype[i] == FileTypes.TEXT) {
            throw new UnsupportedOperationException("Translating a textfile is currently untested.");
            // translateText(fname, sequence); //TODO Test this case and use it again
         } else
            g.terminate("translate: unsupported file type for file " + files[i]);
      }
      // DONE processing all files.
      try {
         sequence.store(); // stores seq, ssp and desc
      } catch (IOException ex) {
      }
      long totallength = sequence.length();
      log.info("translate: translated sequence length: %d", totallength);
      if (totallength >= (2L * 1024 * 1024 * 1024))
         log.warn("translate: length %d exceeds 2 GB limit!!", totallength);
      else if (totallength >= (2L * 1024 * 1024 * 1024 * 127) / 128)
         log.warn("translate: long sequence, %d is within 99% of 2GB limit!", totallength);
      project.setProperty("Length", totallength);

      project.setProperty("NumberSequences", sequence.getNumberSequences());

      // Write sequence length statistics.
      project.setProperty("LongestSequence", sequence.getMaximumLength());
      project.setProperty("ShortestSequence", sequence.getMinimumLength());

      // Write the alphabet
      PrintWriter alphabetfile = null;
      try {
         alphabetfile = new PrintWriter(project.makeFile(FileTypes.ALPHABET));
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
    * @see translateFasta(String,Sequence)
    */
   public void translateCSFasta(final File file, final SequenceWriter sequence) {
      // nothing special
      // FastaFile.read() already ignores comment lines with #
      // so call translateFasta(String,Sequence)
      assert FileUtils.determineFileType(file) == FileTypes.CSFASTA;
      translateFasta(file, sequence, csfastaQualityFile);
      // TODO maybe make some assertions like alphabet == CS???
   }

   /**
    * Interprets a given FASTA file with the DNA alphabet and encodes it into colospace alphabet.
    * Afterwards the result is translated into the given sequence.
    * 
    * @param file
    *           the FASTA file
    * @param sequence
    *           sequence to translate into
    * @see translateFasta(String,Sequence)
    */
   public void translateFastaFromDNA2CS(final File file, final SequenceWriter sequence) {
      FastaFile f = new FastaFile(file);
      FastaSequence fseq = null;
      ByteBuffer tr = null;
      final int appendforward = (addrc && separateRCByWildcard) ? 1 : 2;
      long lastbyte = 0;

      try {
         f.open();
      } catch (IOException ex) {
         log.warn("translate: skipping '%s': %s", file, ex);
         return;
      }
      while (true) {
         try {
            fseq = f.read(); // reads one fasta sequence from file f
            if (fseq == null)
               break;
            tr = fseq.translateDNAtoCS(tr, trim, appendforward);
            lastbyte = sequence.writeBuffer(tr);
            sequence.addInfo(fseq.getHeader(), fseq.length(), (int) (lastbyte - 1));
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

   public void translateFasta(final File file, final SequenceWriter sequence) {
      translateFasta(file, sequence, null);
   }

   /**
    * @param qualityFile
    *           File with FASTA-style quality information (one byte per character). May be null.
    */
   public void translateFasta(final File file, final SequenceWriter sequence, final File qualityFile) {
      FastaFile qualityFasta = null;
      if (qualityFile != null) {
         qualityFasta = new FastaFile(qualityFile);
         try {
            qualityFasta.open();
         } catch (IOException ex) {
            log.warn("translate: quality file not found (%s), skipping. (%s)", qualityFile, ex);
            return;
         }
         // qualityOutput = new
         // ArrayFile(FileUtils.extensionRemoved(fname)+FileNameExtensions.quality);
      }
      FastaFile f = new FastaFile(file);
      FastaSequence fseq = null;
      ByteBuffer tr = null;
      final int appendforward = (addrc && separateRCByWildcard) ? 1 : 2;
      final int appendreverse = 2; // always append separator
      long lastbyte = 0;

      try {
         f.open();
      } catch (IOException ex) {
         log.warn("translate: skipping '%s': %s", file, ex);
         return;
      }
      while (true) {
         try {
            fseq = f.read(); // reads one fasta sequence from file f
            if (fseq == null)
               break;
            tr = fseq.translateTo(tr, trim, alphabet, reverse, appendforward);
            lastbyte = sequence.writeBuffer(tr);
            if (addrc) {
               if (!separateRCByWildcard)
                  sequence.addInfo(fseq.getHeader(), fseq.length(), (int) (lastbyte - 1));
               tr = fseq.translateTo(tr, trim, alphabet2, true, appendreverse);
               lastbyte = sequence.writeBuffer(tr);
               if (separateRCByWildcard)
                  sequence.addInfo(fseq.getHeader(), 2 * fseq.length() + 1, (int) (lastbyte - 1));
               else
                  sequence.addInfo(fseq.getHeader() + " " + dnarcstring, fseq.length(),
                        (int) (lastbyte - 1));
            } else { // no reverse complement
               sequence.addInfo(fseq.getHeader(), fseq.length(), (int) (lastbyte - 1));
            }
            if (qualityFasta != null) {
               FastaSequence qualitySequence = qualityFasta.read();
               if (!qualitySequence.getHeader().equals(fseq.getHeader())) {
                  throw new IllegalArgumentException(
                        String.format(
                              "Annotations from CSFASTA and quality files do not match (\"%s\" != \"%s\").",
                              qualitySequence.getHeader(), fseq.getHeader()));
               }

               if (alphabet.isWildcard(alphabet.code((byte) fseq.getSequence().charAt(0)))) {
                  // CSFASTA begins with A,C,G or T
                  fseq.cutOffSequenceHead(2); // T32312131100... -> 2312131100...
                  int gap = qualitySequence.getSequence().indexOf(' '); // find gap between first
                                                                        // and second value
                  qualitySequence.cutOffSequenceHead(gap + 1); // 25 27 27 2 29 30 25 ... -> 27 27 2 29 30 25...
               }

               String qs = qualitySequence.getSequence();
               byte[] qualityArray = new byte[qs.length() / 2 + 2];
               int n = 0;
               StringTokenizer st = new StringTokenizer(qs, " ", false);
               while (st.hasMoreTokens())
                  qualityArray[n++] = Byte.parseByte(st.nextToken());
               if (n != fseq.length()) {
                  throw new IllegalArgumentException(
                        String.format(
                              "Length mismatch between CSFASTA (%d) and quality files (%d) (sequence name: \"%s\").",
                              fseq.length(), n, fseq.getHeader()));
               }
               qualityArray[n++] = Byte.MIN_VALUE;
               sequence.addQualityValues(ByteBuffer.wrap(qualityArray, 0, n));
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
    * @param file
    * @param sequence
    * @deprecated
    */
   void translateFastaBisulfite(final File file, final SequenceWriter sequence) {
      FastaFile f = new FastaFile(file);
      FastaSequence fseq = null;
      ByteBuffer tr = null;
      long lastbyte = 0;

      try {
         f.open();
      } catch (IOException ex) {
         log.warn("translate: skipping '%s': %s", file, ex);
         return;
      }
      while (true) {
         try {
            fseq = f.read(); // reads one fasta sequence from file f
            if (fseq == null)
               break;
            tr = fseq.translateDNABiTo(tr, trim, false, false, 1); // nonmeth-bis-#
            lastbyte = sequence.writeBuffer(tr);
            tr = fseq.translateDNABiTo(tr, trim, false, true, 2); // nonmeth-cbis-$
            lastbyte = sequence.writeBuffer(tr);
            sequence.addInfo(fseq.getHeader() + " /nonmeth-bis+cbis", 2 * fseq.length() + 1,
                  (int) (lastbyte - 1));
            tr = fseq.translateDNABiTo(tr, trim, true, false, 1); // meth-bis-#
            lastbyte = sequence.writeBuffer(tr);
            tr = fseq.translateDNABiTo(tr, trim, true, true, 2); // meth-cbis-$
            lastbyte = sequence.writeBuffer(tr);
            sequence.addInfo(fseq.getHeader() + " /meth-bis+cbis", 2 * fseq.length() + 1,
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
    * text file processing differs from fasta processing by the following:
    * 
    * -- reverse complement option is ignored, even for DNA text file;
    * 
    * -- each cr/lf is replaced by whitespace (error if the alphabet map does not allow whitespace);
    * 
    * -- description is simply the filename;
    * 
    * -- separator is appended (never the wildcard).
    */
   void translateText(final File file, final SequenceWriter out) {
      ByteBuffer tr = null;
      long lastbyte = 0;
      byte appender;
      int len = 0;
      String current = null;
      BufferedReader br = null;

      try {
         br = new BufferedReader(new FileReader(file), 512 * 1024);
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
         out.addInfo(file.getName(), len, (int) (lastbyte - 1));
      } catch (IOException ex) {
         log.error("translate: error translating '%s': %s", file, ex);
         System.exit(1);
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
   public long computeRuns(final Project project) throws IOException {
      return computeRunsAF(project);
   }

   /** compute runs using memory mapping where possible */
   private long computeRunsM(final Project project) throws IOException {
      int run = -1;
      ByteBuffer seq = new ArrayFile(project.makeFile(FileTypes.SEQ), 0).mapR();
      ArrayFile rseq = new ArrayFile(project.makeFile(FileTypes.RUNSEQ)).openW();
      ArrayFile rlen = new ArrayFile(project.makeFile(FileTypes.RUNLEN)).openW();
      IntBuffer p2r = new ArrayFile(project.makeFile(FileTypes.POS2RUN), 0).mapRW().asIntBuffer();
      ArrayFile r2p = new ArrayFile(project.makeFile(FileTypes.RUN2POS)).openW();
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
   private long computeRunsAF(final Project project) throws IOException {
      int run = -1;
      ByteBuffer seq = new ArrayFile(project.makeFile(FileTypes.SEQ), 0).mapR();
      ArrayFile rseq = new ArrayFile(project.makeFile(FileTypes.RUNSEQ)).openW();
      ArrayFile rlen = new ArrayFile(project.makeFile(FileTypes.RUNLEN)).openW();
      ArrayFile p2r = new ArrayFile(project.makeFile(FileTypes.POS2RUN)).openW();
      ArrayFile r2p = new ArrayFile(project.makeFile(FileTypes.RUN2POS)).openW();
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
}
