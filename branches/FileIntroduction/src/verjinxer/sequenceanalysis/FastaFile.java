/*
 * FastaFile.java
 *
 * Created on December 12, 2006, 11:46 AM
 *
 */

package verjinxer.sequenceanalysis;

import java.io.*;

/**
 * 
 * @author Sven Rahmann
 */
public class FastaFile {
   public enum FastaMode {
      READ, WRITE
   };

   private String fname;
   private BufferedReader in;
   private PrintWriter out;
   private String nextHeader;

   /**
    * Creates a new instance of FastaFile
    * 
    * @param fname
    *           the file name of the FASTA file
    */
   public FastaFile(final String fname) {
      this.fname = fname;
   }

   /**
    * open this FastaFile either for reading or writing
    * 
    * @param mode
    *           either FastaMode.READ or FastaMode.WRITE
    * @return this FastaFile
    * @throws java.io.IOException
    */
   public FastaFile open(final FastaMode mode) throws IOException {
      if (in != null || out != null)
         throw new IOException("FastaFile already open");
      if (mode == FastaMode.READ) {
         in = new BufferedReader(new FileReader(fname), 1024 * 1024);
         out = null;
      } else if (mode == FastaMode.WRITE) {
         out = new PrintWriter(new FileWriter(fname));
         in = null;
      }
      this.nextHeader = null;
      return this;
   }

   /**
    * open this FastaFile for reading
    * 
    * @return this FastaFile
    * @throws java.io.IOException
    */
   public FastaFile open() throws IOException {
      return this.open(FastaMode.READ);
   }

   public void close() throws IOException {
      if (in != null)
         in.close();
      if (out != null)
         out.close();
      in = null;
      out = null;
      nextHeader = null;
   }

   public FastaSequence read() throws IOException, FastaFormatException {
      String line;
      FastaSequence seq;
      if (nextHeader != null)
         seq = new FastaSequence(nextHeader);
      else {
         while ((line = in.readLine()) != null) {
            line = line.trim();
            if (line.length() == 0 || line.startsWith("#"))
               continue;
            break;
         }
         if (line == null)
            return null;
         if (!(line.startsWith(">")))
            throw new FastaFormatException("FASTA Header expected, got " + line);
         seq = new FastaSequence(line);
      }
      // read sequence lines until next ">"
      while ((line = in.readLine()) != null) {
         line = line.trim();
         if (line.length() == 0 || line.startsWith("#"))
            continue;
         if (line.startsWith(">"))
            break;
         seq.append(line);
      }
      nextHeader = line;
      return seq;
   }

   /**
    * writes a string to this FASTA file with 60 characters/line. The file must be open for writing;
    * otherwise a runtime error results. The output is preceded by a FASTA header, as given by the
    * header argument
    * 
    * @param s
    *           the string to be written
    * @param header
    *           the header for the string
    */
   public void writeString(String s, String header) {
      out.printf(">%s%n", header);
      final int n = s.length();
      int end;
      for (int i = 0; i < n; i += 60) {
         end = i + 60;
         if (end >= n)
            end = n;
         out.println(s.substring(i, end));
      }
   }

   /**
    * translates the FASTA file into a AnnotatedArrayFile using the given alphbet map OUT OF DATE
    * 
    * @param bf
    *           the output file, often a <code>new AnnotatedArrayFile(filename)</code>
    * @param amap
    *           the alphabet map to use
    * @param append
    *           true if an existing file should be appended. In this case, the existing
    *           BiAnnotatedArrayFilehould be passed as first argument.
    * @param amapReverse
    *           if not null, alphabet map to use for the reverse sequence
    * @return reference to the biosequence file
    */
   /*
   public AnnotatedArrayFile translateToFile(final AnnotatedArrayFile bf, 
       final AlphabetMap amap, final boolean append, final AlphabetMap amapReverse)
   throws IOException, FastaFormatException, InvalidSymbolException
   {
      if (mode!=null && mode!=FastaMode.READ)
         throw new IOException("File "+this.fname+" seems to be for writing.");
      if (mode==null) this.open();
      boolean rc = (amapReverse!=null);
      if (!append) bf.clear();
      
      long lastbyte;
      byte[] translation;
      for(boolean first=true; true; first=false)
      {
         FastaSequence fseq = this.read();
         if (fseq==null) break;
         translation = fseq.toByteArray(1);   // space for separator/wildcard
         amap.applyTo(translation, true, rc); // translate and set wildcard iff rc, else separator
         lastbyte = bf.put(translation, (!first||append));
         if (rc)
         {
            translation = fseq.toReverseByteArray(1);    // space for separator/wildcard
            amapReverse.applyTo(translation,true,false); // translate and set separator
            lastbyte = bf.put(translation, true);
         }
         bf.addInfo(fseq.getHeader(), fseq.length(), (int)lastbyte-1);
      }
      this.close();
      return bf;
   }
   */
}
