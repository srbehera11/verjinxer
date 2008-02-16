/*
 * FastaSequence.java
 *
 * Created on December 15, 2006, 8:25 AM
 *
 */

package verjinxer.sequenceanalysis;

import java.nio.ByteBuffer;

/**
 *
 * @author Sven Rahmann
 */
public class FastaSequence {
  
  static final AlphabetMap DNA = AlphabetMap.DNA();
 
  private String header;
  private StringBuilder sequence;
  
  /** Creates a new instance of FastaSequence
   * @param header  the header (title) of this sequence
   */
  public FastaSequence(String header) {
    if (header==null) this.header = "";
    else if (header.startsWith(">")) this.header = header.substring(1).trim();
    else this.header = header.trim();
    sequence = new StringBuilder(2048);
  }
  
  public void append(String line) {
    sequence.append(line);
  }
  
  public String getHeader() { return header; }
  
  public String getSequence() { return sequence.toString(); }
  
  public String getReverseSequence() {
    return ((new StringBuilder(sequence)).reverse().toString());
  }
  
  public int length() {
    return sequence.length();
  }
  
  /** converts sequence to a byte array, appending additional -1's
   * @param additional append this many -1 bytes at the end
   * @return a byte array representation of this FASTA sequence
   */
  public byte[] toByteArray(int additional) {
    int ll = sequence.length();
    byte[] ba = new byte[ll+additional];
    for (int i=0; i<ll; i++) ba[i]=(byte)sequence.charAt(i);
    for (int i=ll; i<ll+additional; i++) ba[i]=-1;
    return ba;
  }
  
  /** converts reverse sequence to a byte array, appending additional -1's
   * @param additional append this many -1 bytes at the end
   * @return a byte array representation of this FASTA sequence, reversed
   */
  public byte[] toReverseByteArray(int additional) {
    int ll = sequence.length();
    byte[] ba = new byte[ll+additional];
    for (int i=0; i<ll; i++) ba[i]=(byte)sequence.charAt(ll-1-i);
    for (int i=ll; i<ll+additional; i++) ba[i]=-1;
    return ba;
  }
  
  
  // ======================== translateTo methods ============================
  
  /** translates a Fasta sequence into given ByteBuffer
   * @param buf the buffer to write to; can be null, creates a new buffer
   * @param trim  set to true if you want to trim DNA non-symbol characters at either end.
   *   TODO: trimming only applies to DNA at the moment -- this should be more general
   * @param amap the alphabet map by which to translate
   * @param reverse if true, translate the reverse sequence
   * @param append a flag (0: append nothing, 1: append wildcard, 2: append separator)
   * @return the ByteBuffer for convenience
   * @throws verjinxer.sequenceanalysis.InvalidSymbolException 
   */
  public ByteBuffer translateTo(ByteBuffer buf, final boolean trim, final AlphabetMap amap, final boolean reverse, final int append) 
  throws InvalidSymbolException {
    final int ll = sequence.length();
    int first=0, last=ll-1;
    if (trim) {
      for(first=0; first<ll && DNA.isWildcard(sequence.charAt(first)); first++) {}
      for(last=ll-1; last>=0 && DNA.isWildcard(sequence.charAt(last)); last--) {}
    }
    final int req = last-first+1 + (append!=0? 1:0);
    if (buf==null || buf.capacity()<req)  buf = ByteBuffer.allocateDirect(req+1024);
    buf.position(0);
    buf.limit(buf.capacity());
    if (!reverse) {
      for (int i=first; i<=last; i++) buf.put(amap.code((byte)sequence.charAt(i)));
    } else { // reverse
      for (int i=last; i>=first; i--) buf.put(amap.code((byte)sequence.charAt(i)));
    }
    if (append==1) buf.put(amap.codeWildcard());
    else if (append==2) buf.put(amap.codeSeparator());
    assert(buf.position()==req) : Integer.toString(buf.position())+" / "+Integer.toString(req);
    buf.flip(); // prepare for writing
    return buf;
  }

  
  /** translates a DNA Fasta sequence under bisulfite treatment into given ByteBuffer;
   * this modifies C->T (G->A under complementary rules), except if methylated.
   * @param buf the buffer to write to; can be null, then creates a new buffer
   * @param trim  set to true if you want to trim DNA non-symbol characters at either end.
   *   TODO: trimming only applies to DNA at the moment -- this should be more general
   * @param meth if true, assume all Cs before Gs are methylated (not modified)
   * @param compl  if true, translate under complementray rules
   * @param append a flag (0: append nothing, 1: append wildcard, 2: append separator)
   * @return the ByteBuffer for convenience
   * @throws verjinxer.sequenceanalysis.InvalidSymbolException 
   */
  public ByteBuffer translateDNABiTo(ByteBuffer buf, final boolean trim, final boolean meth, final boolean compl, final int append)
  throws InvalidSymbolException {
    final int ll = sequence.length();
    int first=0, last=ll-1;
    if (trim) {
      for(first=0; first<ll && DNA.isWildcard(sequence.charAt(first)); first++) {}
      for(last=ll-1; last>=0 && DNA.isWildcard(sequence.charAt(last)); last--) {}
    }
    final int req = last-first+1 + (append!=0? 1:0);
    if (buf==null || buf.capacity()<req)  buf = ByteBuffer.allocateDirect(req+1024);
    buf.position(0);
    buf.limit(buf.capacity());
    byte c=-1, prev;
    byte next = DNA.code((byte)sequence.charAt(0));
    for (int i=0; i<ll; i++) {
      prev = c;
      c = next;
      next = ( i+1==ll?  -1  :  DNA.code((byte)sequence.charAt(i+1)) );
      if (!compl && c==1 && (!meth || next!=2)) c=3; // replace C==1 by T==3
      if (compl  && c==2 && (!meth || prev!=1)) c=0; // replace G==2 by A==0
      buf.put(c);
    }
    if (append==1) buf.put(DNA.codeWildcard());
    else if (append==2) buf.put(DNA.codeSeparator());
    assert(buf.position()==req) : Integer.toString(buf.position())+" / "+Integer.toString(req);
    buf.flip(); // prepare for writing
    return buf;
  }
}
