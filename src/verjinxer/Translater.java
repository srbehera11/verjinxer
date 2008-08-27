/*
 * Translater.java
 * Created on 30. Januar 2007, 14:57
 */

package verjinxer;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import verjinxer.sequenceanalysis.*;
import verjinxer.util.*;
import static verjinxer.Globals.*;

/**
 * VerJInxer Module to translate a set of text or FASTA files into a byte file.
 * @author Sven Rahmann
 */
public class Translater {
  
  final Globals g;
  
  
  // Translater variables
  final boolean trim;
  final AlphabetMap amap, amap2;
  final boolean separateRCByWildcard;
  final boolean reverse;
  final boolean addrc;
  final boolean bisulfite;
  final String dnarcstring;
  
  public Translater(Globals g, boolean trim, AlphabetMap amap, AlphabetMap amap2, boolean separateRCByWildcard,
        boolean reverse,
        boolean addrc,
        boolean bisulfite,
        String dnarcstring) {
     this.g = g;
     this.trim = trim;
     this.amap = amap;
     this.amap2 = amap2;
     this.separateRCByWildcard = separateRCByWildcard;
     this.reverse = reverse;
     this.bisulfite = bisulfite;
     this.dnarcstring = dnarcstring;
     this.addrc = addrc;
   }

 
  /************* processing methods ******************************************************/
  
  void processFasta(final String fname, final AnnotatedArrayFile out) {
    FastaFile f = new FastaFile(fname);
    FastaSequence fseq = null;
    ByteBuffer tr = null;
    final int appendforward = (addrc && separateRCByWildcard)? 1 : 2;
    final int appendreverse = 2; // always append separator
    long lastbyte = 0;
    
    try { 
       f.open(); 
    } 
    catch (IOException e) { 
       g.warnmsg("translate: skipping '%s': %s%n",fname, e.toString()); 
       return; 
    }
    while(true) {
      try {
        fseq = f.read(); // reads one fasta sequence from file f
        if (fseq==null) break;
        tr = fseq.translateTo(tr, trim, amap, reverse, appendforward);
        lastbyte = out.writeBuffer(tr);
        if (addrc) {
          if (!separateRCByWildcard)
             out.addInfo(fseq.getHeader(), fseq.length(), (int)(lastbyte-1));
          tr = fseq.translateTo(tr, trim, amap2, true, appendreverse);
          lastbyte = out.writeBuffer(tr);
          if (separateRCByWildcard) 
             out.addInfo(fseq.getHeader(), 2*fseq.length()+1, (int)(lastbyte-1));
          else 
             out.addInfo(fseq.getHeader()+" "+dnarcstring, fseq.length(), (int)(lastbyte-1));
        } else { // no reverse complement
          out.addInfo(fseq.getHeader(), fseq.length(), (int)(lastbyte-1)); 
        }
      } catch (InvalidSymbolException e) {
         g.warnmsg("translate: %s%n", e.toString()); 
         break; 
      }
      catch (IOException e) {
         g.warnmsg("translate: %s%n", e.toString()); 
         break;
      }
      catch (FastaFormatException e) {
         g.warnmsg("translate: %s%n", e.toString()); 
         break;
      }
    }
    // close file
    try {  
       f.close(); 
    } catch (IOException e) { 
       g.warnmsg("translate: %s%n",e.toString()); 
    }
  }

  
  void processFastaB(final String fname, final AnnotatedArrayFile out) {
    FastaFile f = new FastaFile(fname);
    FastaSequence fseq = null;
    ByteBuffer tr = null;
    long lastbyte = 0;
    
    try { f.open(); } catch (Exception e) { g.warnmsg("translate: skipping '%s': %s%n",fname, e.toString()); return; }
    while(true) {
      try {
        fseq = f.read(); // reads one fasta sequence from file f
        if (fseq==null) break;
        tr = fseq.translateDNABiTo(tr, trim, false, false, 1);  // nonmeth-bis-#
        lastbyte = out.writeBuffer(tr);
        tr = fseq.translateDNABiTo(tr, trim, false, true,  2);  // nonmeth-cbis-$
        lastbyte = out.writeBuffer(tr);
        out.addInfo(fseq.getHeader()+" /nonmeth-bis+cbis", 2*fseq.length()+1, (int)(lastbyte-1));
        tr = fseq.translateDNABiTo(tr, trim, true,  false, 1);  // meth-bis-#
        lastbyte = out.writeBuffer(tr);
        tr = fseq.translateDNABiTo(tr, trim, true,  true,  2);  // meth-cbis-$
        lastbyte = out.writeBuffer(tr);
        out.addInfo(fseq.getHeader()+" /meth-bis+cbis", 2*fseq.length()+1, (int)(lastbyte-1));
      } catch (Exception e) {
        g.warnmsg("translate: %s%n",e.toString()); break; }
    }
    // close file
    try {  f.close(); } catch (Exception e) { g.warnmsg("translate: %s%n",e.toString()); }
  }

  
  /** text file processing differs from fasta processing by the following:
   * -- reverse complement option is ignored, even for DNA text file;
   * -- each cr/lf is replaced by whitespace (error if the alphabet map does not allow whitespace);
   * -- description is simply the filename;
   * -- separator is appended (never the wildcard).
   */
  void processText(final String fname, final AnnotatedArrayFile out) {
    ByteBuffer tr = null;
    long lastbyte = 0;
    byte appender;
    int len=0;
    String current = null;
    BufferedReader br = null;
    
    try {
      br = new BufferedReader(new FileReader(fname), 512*1024);
      String next = br.readLine();
      while (next!=null) {
        current=next;
        next=br.readLine();
        try {
          appender = (next==null)? amap.codeSeparator() : amap.codeWhitespace();
          len += current.length() + (next==null? 0:1);
          tr = amap.applyTo(current, tr, true, appender);
        } catch (InvalidSymbolException ex) {
          throw new IOException(ex);
        }
      }
      lastbyte = out.writeBuffer(tr);
      out.addInfo(fname, len, (int)(lastbyte-1));
    } catch(IOException ex) {
      g.warnmsg("translate: error translating '%s': %s%n",fname,ex.toString());
      g.terminate(1);
    } finally {
      try { if (br!=null) {br.close();} } catch(IOException e) {}
    }
  }
  
  
  /******************************* runs ***********************************/
    
  /** reads translated sequence (using memory-mapping) 
   * and writes run-related files
   * .runseq: the character sequence of runs (same as original sequence, but each run condensed to a single character);
   * .runlen: the length of each run (as a byte); for run lengths &gt; 127, we store -1;
   * .pos2run: pos2run[p]=r means that we are in run r at position p;
   * .run2pos: run2pos[r]=p means that run r starts at position p.
   * The run-related files are written using streams in the native byte order.
   * @param fname  filename (without extension) of the sequence file
   * @return number of runs in the sequence file
   * @throws java.io.IOException 
   */
  long computeRuns(final String fname) throws IOException {
     return computeRunsAF(fname);
  }

  /** compute runs using memory mapping where possible */
  private long computeRunsM(final String fname) throws IOException {
    int run = -1;
    ByteBuffer seq = new ArrayFile(fname+extseq,0).mapR();
    ArrayFile rseq = new ArrayFile(fname+extrunseq).openW();
    ArrayFile rlen = new ArrayFile(fname+extrunlen).openW();
    IntBuffer  p2r = new ArrayFile(fname+extpos2run,0).mapRW().asIntBuffer();
    ArrayFile  r2p = new ArrayFile(fname+extrun2pos).openW();
    final int n = seq.limit();
    
    byte next;
    byte prev=-1;
    int start = 0;
    int len;
    for (int p=0; p<n; p++) {
      next = seq.get();
      if (next!=prev || p==0) {
        run++;
        prev=next;
        len=p-start;  
        assert(len>0 || p==0);  
        if(len>127) len=-1;
        if (p!=0) rlen.writeByte((byte)len);
        start=p;
        rseq.writeByte(next);
        r2p.writeInt(p);
      }
      p2r.put(run);
    }
    run++;                 // number of runs
    len=n-start;  
    assert(len>0);  
    if(len>127) len=-1;
    rlen.writeByte((byte)len); // while 'len' is an int, we only write the least significant byte!
    r2p.writeInt(n); // write sentinel
    rseq.close(); rlen.close(); r2p.close();
    seq=null;
    
    assert(4*run == r2p.length()-4) :
      String.format("n=%d, runs=%d, rseq=%d. 4*run=%d, run2pos=%d, pos2run=%d",
        n, run, rseq.length(), 4*run, r2p.length(), p2r.position());
    return run;
  }

  /** compute runs using array files for writing, mmap only for reading */
  private long computeRunsAF(final String fname) throws IOException {
    int run = -1;
    ByteBuffer seq = new ArrayFile(fname+extseq,0).mapR();
    ArrayFile rseq = new ArrayFile(fname+extrunseq).openW();
    ArrayFile rlen = new ArrayFile(fname+extrunlen).openW();
    ArrayFile  p2r = new ArrayFile(fname+extpos2run).openW();
    ArrayFile  r2p = new ArrayFile(fname+extrun2pos).openW();
    final int n = seq.limit();
    byte next;
    byte prev=-1;
    int start = 0;
    int len;
    for (int p=0; p<n; p++) {
      next = seq.get();
      if (next!=prev || p==0) {
        run++;
        prev=next;
        len=p-start;  
        assert(len>0 || p==0);  
        if(len>127) len=-1;
        if (p!=0) rlen.writeByte((byte)len);
        start=p;
        rseq.writeByte(next);
        r2p.writeInt(p);
      }
      p2r.writeInt(run);
    }
    run++;                 // number of runs
    len=n-start;  
    assert(len>0);  
    if(len>127) len=-1;
    rlen.writeByte((byte)len); // while 'len' is an int, we only write the least significant byte!
    r2p.writeInt(n); // write sentinel
    rseq.close(); rlen.close(); p2r.close(); r2p.close();
    seq=null;
    
    assert(4*run == r2p.length()-4) :
      String.format("n=%d, runs=%d, rseq=%d. 4*run=%d, run2pos=%d, pos2run=%d",
        n, run, rseq.length(), 4*run, r2p.length(), p2r.length());
    return run;
  }
}
