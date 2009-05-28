/*
 * FastaSequence.java Created on December 15, 2006, 8:25 AM
 */

package verjinxer.sequenceanalysis;

import java.nio.ByteBuffer;
import java.util.HashMap;

/**
 * 
 * @author Sven Rahmann
 */
public class FastaSequence {

   static final Alphabet DNA = Alphabet.DNA();

   /**
    * Maps two encoded DNA bases to color space
    */
   private static final char[][] DNA2CS = new char[5][5];
   static {
      DNA2CS[0][0] = '0'; // aa aA Aa AA
      DNA2CS[0][1] = '1'; // ac aC Ac AC
      DNA2CS[0][2] = '2'; // ag aG Ag AG
      DNA2CS[0][3] = '3'; // at aT At AT
      DNA2CS[0][4] = '4'; // an aN ar aR am aM An AN Ar AR Am AM
      DNA2CS[1][0] = '1'; // ca cA Ca CA
      DNA2CS[1][1] = '0'; // cc cC Cc CC
      DNA2CS[1][2] = '3'; // cg cG Cg CG
      DNA2CS[1][3] = '2'; // ct cT Ct CT
      DNA2CS[1][4] = '4'; // cn cN cr cR cm cM Cn CN Cr CR Cm CM
      DNA2CS[2][0] = '2'; // ga gA Ga GA
      DNA2CS[2][1] = '3'; // gc gC Gc GC
      DNA2CS[2][2] = '0'; // gg gG Gg GG
      DNA2CS[2][3] = '1'; // gt gT Gt GT
      DNA2CS[2][4] = '4'; // gn gN gr gR gm gM Gn GN Gr GR Gm GM
      DNA2CS[3][0] = '3'; // ta tA Ta TA
      DNA2CS[3][1] = '2'; // tc tC Tc TC
      DNA2CS[3][2] = '1'; // tg tG Tg TG
      DNA2CS[3][3] = '0'; // tt tT Tt TT
      DNA2CS[3][4] = '4'; // tn tN tr tR tm tM Tn TN Tr TR Tm TM
      DNA2CS[4][0] = '4'; // na nA Na NA ra rA Ra RA ma mA Ma MA
      DNA2CS[4][1] = '4'; // nc nC Nc NC rc rC Rc RC mc mC Mc MC
      DNA2CS[4][2] = '4'; // ng nG Ng NG rg rG Rg RG mg mG Mg MG
      DNA2CS[4][3] = '4'; // nt nT Nt NT rt rT Rt RT mt mT Mt MT
      DNA2CS[4][4] = '4'; // nn nN nr nR nm nM Nn NN Nr NR Nm NM rn rN rr rR rm rM Rn RN Rr RR Rm RM
      // mn mN mr mR mm mM Mn MN Mr MR Mm MM
   }

   private String header;
   private StringBuilder sequence;

   /**
    * Creates a new instance of FastaSequence
    * 
    * @param header
    *           the header (title) of this sequence
    */
   public FastaSequence(String header) {
      if (header == null)
         this.header = "";
      else if (header.startsWith(">"))
         this.header = header.substring(1).trim();
      else
         this.header = header.trim();
      sequence = new StringBuilder(2048);
   }

   public void append(String line) {
      sequence.append(line);
   }

   public String getHeader() {
      return header;
   }

   public String getSequence() {
      return sequence.toString();
   }

   public String getReverseSequence() {
      return ((new StringBuilder(sequence)).reverse().toString());
   }

   public int length() {
      return sequence.length();
   }

   /**
    * converts sequence to a byte array, appending additional -1's
    * 
    * @param additional
    *           append this many -1 bytes at the end
    * @return a byte array representation of this FASTA sequence
    */
   public byte[] toByteArray(int additional) {
      int ll = sequence.length();
      byte[] ba = new byte[ll + additional];
      for (int i = 0; i < ll; i++)
         ba[i] = (byte) sequence.charAt(i);
      for (int i = ll; i < ll + additional; i++)
         ba[i] = -1;
      return ba;
   }

   /**
    * converts reverse sequence to a byte array, appending additional -1's
    * 
    * @param additional
    *           append this many -1 bytes at the end
    * @return a byte array representation of this FASTA sequence, reversed
    */
   public byte[] toReverseByteArray(int additional) {
      int ll = sequence.length();
      byte[] ba = new byte[ll + additional];
      for (int i = 0; i < ll; i++)
         ba[i] = (byte) sequence.charAt(ll - 1 - i);
      for (int i = ll; i < ll + additional; i++)
         ba[i] = -1;
      return ba;
   }

   // ======================== translateTo methods ============================

   /**
    * translates a Fasta sequence into given ByteBuffer
    * 
    * @param buf
    *           the buffer to write to; can be null, creates a new buffer
    * @param trim
    *           set to true if you want to trim DNA non-symbol characters at either end. TODO:
    *           trimming only applies to DNA at the moment -- this should be more general
    * @param alphabet
    *           the alphabet map by which to translate
    * @param reverse
    *           if true, translate the reverse sequence
    * @param append
    *           a flag (0: append nothing, 1: append wildcard, 2: append separator)
    * @return the ByteBuffer for convenience
    * @throws verjinxer.sequenceanalysis.InvalidSymbolException
    */
   public ByteBuffer translateTo(ByteBuffer buf, final boolean trim, final Alphabet alphabet,
         final boolean reverse, final int append) throws InvalidSymbolException {
      final int ll = sequence.length();
      int first = 0, last = ll - 1;
      if (trim) {
         for (first = 0; first < ll && DNA.isWildcard(sequence.charAt(first)); first++) {
         }
         for (last = ll - 1; last >= 0 && DNA.isWildcard(sequence.charAt(last)); last--) {
         }
      }
      final int req = last - first + 1 + (append != 0 ? 1 : 0);
      if (buf == null || buf.capacity() < req)
         buf = ByteBuffer.allocateDirect(req + 1024);
      buf.position(0);
      buf.limit(buf.capacity());
      if (!reverse) {
         for (int i = first; i <= last; i++)
            buf.put(alphabet.code((byte) sequence.charAt(i)));
      } else { // reverse
         for (int i = last; i >= first; i--)
            buf.put(alphabet.code((byte) sequence.charAt(i)));
      }
      if (append == 1)
         buf.put(alphabet.codeWildcard());
      else if (append == 2)
         buf.put(alphabet.codeSeparator());
      assert (buf.position() == req) : Integer.toString(buf.position()) + " / "
            + Integer.toString(req);
      buf.flip(); // prepare for writing
      return buf;
   }

   /**
    * translates a DNA Fasta sequence under bisulfite treatment into given ByteBuffer; this modifies
    * C->T (G->A under complementary rules), except if methylated.
    * 
    * @param buf
    *           the buffer to write to; can be null, then creates a new buffer
    * @param trim
    *           set to true if you want to trim DNA non-symbol characters at either end. TODO:
    *           trimming only applies to DNA at the moment -- this should be more general
    * @param meth
    *           if true, assume all Cs before Gs are methylated (not modified)
    * @param compl
    *           if true, translate under complementray rules
    * @param append
    *           a flag (0: append nothing, 1: append wildcard, 2: append separator)
    * @return the ByteBuffer for convenience
    * @throws verjinxer.sequenceanalysis.InvalidSymbolException
    */
   public ByteBuffer translateDNABiTo(ByteBuffer buf, final boolean trim, final boolean meth,
         final boolean compl, final int append) throws InvalidSymbolException {
      final int ll = sequence.length();
      int first = 0, last = ll - 1;
      if (trim) {
         for (first = 0; first < ll && DNA.isWildcard(sequence.charAt(first)); first++) {
         }
         for (last = ll - 1; last >= 0 && DNA.isWildcard(sequence.charAt(last)); last--) {
         }
      }
      final int req = last - first + 1 + (append != 0 ? 1 : 0);
      if (buf == null || buf.capacity() < req)
         buf = ByteBuffer.allocateDirect(req + 1024);
      buf.position(0);
      buf.limit(buf.capacity());
      byte c = -1, prev;
      byte next = DNA.code((byte) sequence.charAt(0));
      for (int i = 0; i < ll; i++) {
         prev = c;
         c = next;
         next = (i + 1 == ll ? -1 : DNA.code((byte) sequence.charAt(i + 1)));
         if (!compl && c == 1 && (!meth || next != 2))
            c = 3; // replace C==1 by T==3
         if (compl && c == 2 && (!meth || prev != 1))
            c = 0; // replace G==2 by A==0
         buf.put(c);
      }
      if (append == 1)
         buf.put(DNA.codeWildcard());
      else if (append == 2)
         buf.put(DNA.codeSeparator());
      assert (buf.position() == req) : Integer.toString(buf.position()) + " / "
            + Integer.toString(req);
      buf.flip(); // prepare for writing
      return buf;
   }

   /**
    * Translate a DNA Fasta sequence to color space and than into given ByteBuffer
    * 
    * @param buf
    *           the buffer to write to; can be null, then creates a new buffer
    * @param trim
    *           set to true if you want to trim DNA non-symbol characters at either end
    * @param append
    *           a flag (0: append nothing, 1: append wildcard, 2: append separator)
    * @return the ByteBuffer for convenience
    * @throws verjinxer.sequenceanalysis.InvalidSymbolException
    */
   public ByteBuffer translateDNAtoCS(ByteBuffer buf, final boolean trim, final int append)
         throws InvalidSymbolException {
      final Alphabet CS = Alphabet.CS();
      final int ll = sequence.length();
      int first = 0, last = ll - 1;
      if (trim) {
         for (first = 0; first < ll && DNA.isWildcard(sequence.charAt(first)); first++) {
         }
         for (last = ll - 1; last >= 0 && DNA.isWildcard(sequence.charAt(last)); last--) {
         }
      }
      final int req = last - first + 1 + (append != 0 ? 1 : 0);
      if (buf == null || buf.capacity() < req)
         buf = ByteBuffer.allocateDirect(req + 1024);
      buf.position(0);
      buf.limit(buf.capacity());
      buf.put(CS.code((byte) sequence.charAt(first)));
      byte prev = DNA.code((byte) sequence.charAt(first));
      byte next;
      for (int i = first + 1; i <= last; i++, prev = next) {
         next = DNA.code((byte) sequence.charAt(i));
         buf.put(CS.code((byte) DNA2CS[prev][next]));
      }
      if (append == 1)
         buf.put(CS.codeWildcard());
      else if (append == 2)
         buf.put(CS.codeSeparator());
      assert buf.position() == req : Integer.toString(buf.position()) + " / "
            + Integer.toString(req);
      buf.flip(); // prepare for writing
      return buf;
   }
}
