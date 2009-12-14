package verjinxer;

import com.spinn3r.log5j.Logger;

import java.io.File;
import java.io.IOException;

import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.sequenceanalysis.ISuffixDLL;
import verjinxer.sequenceanalysis.SuffixDLL;
import verjinxer.sequenceanalysis.SuffixXorDLL;
import verjinxer.util.ArrayFile;
import verjinxer.util.TicToc;

/**
 * @author Markus Kemmerling
 */
public class LCP {

   private static Logger logger = null;
   // attributes are there to reduce number of parameters for private methods
   // they are set in buildLcpAndWriteToFile(ISuffixDLL, String, int, File, int[])
   // and used in suffixlcp(int, int, int) and scmp(int, int)
   private static byte[] sequence;
   private static Alphabet alphabet;
   

   public static void setLogger(Logger logger) {
      LCP.logger = logger;
   }

   /**
    * Calculates for a given suffix array/list the lcp array (longest common prefix) and writes it
    * to disc.
    * 
    * @param suffixDLL
    *           Suffix list for that the lcp is calculated.
    * @param method
    *           The method to use for calculation. Valid methods are 'L', 'R', 'minLR', 'bothLR' and
    *           'bothLR2'. The method must suit to the type suffixDLL and its internal structure.
    * @param specialCharacterOrder 
    *           How to compare/order special characters. Valid methods are 'pos' and 'suffix'.
    * @param dolcp
    *           Which lcp arrays to compute (0..7, any combination of 1+2+4)
    * @param file
    *           Basic file name to store the lcp arrays.
    * @param buffer
    *           Buffer for interim results. If the buffer has less capacity as suffixDLL, a new
    *           array is initialized and used as buffer.
    * @return Informations about the building process.
    * @throws IOException
    *            when the lcp arrays can not be written to disc.
    * @throws IllegalArgumentException
    *            when the given method is not valid or when the given method does not suits to the
    *            type of suffixDLL.
    */
   public static LcpInfo buildLcpAndWriteToFile(ISuffixDLL suffixDLL, String method,
         String specialCharacterOrder, int dolcp, File file, int[] buffer) throws IOException,
         IllegalArgumentException {
      sequence = suffixDLL.getSequence().array();
      alphabet = suffixDLL.getAlphabet();

      LcpInfo returnvalue = null;

      if (method.equals("L")) {
         if (suffixDLL instanceof SuffixDLL) {
            returnvalue = lcp_L(file, dolcp, (SuffixDLL) suffixDLL, buffer, specialCharacterOrder);
         } else {
            throw new IllegalArgumentException("Method '" + method + "' only suits to type 'SuffixDLL'!");
         }
      } else if (method.equals("R")) {
         if (suffixDLL instanceof SuffixDLL) {
            returnvalue = lcp_L(file, dolcp, (SuffixDLL) suffixDLL, buffer, specialCharacterOrder);
         } else {
            throw new IllegalArgumentException("Method '" + method + "' only suits to type 'SuffixDLL'!");
         }
      } else if (method.equals("minLR")) {
         if (suffixDLL instanceof SuffixDLL) {
            returnvalue = lcp_L(file, dolcp, (SuffixDLL) suffixDLL, buffer, specialCharacterOrder);
         } else {
            throw new IllegalArgumentException("Method '" + method + "' only suits to type 'SuffixDLL'!");
         }
      } else if (method.equals("bothLR")) {
         if (suffixDLL instanceof SuffixXorDLL) {
            // lcp_bothLR(flcp, dolcp);
            throw new UnsupportedOperationException("Not yet implemented");
         } else {
            throw new IllegalArgumentException("Method '" + method + "' only suits to type 'SuffixXorDLL'!");
         }
      } else if (method.equals("bothLR2")) {
         if (suffixDLL instanceof SuffixDLL) {
            returnvalue = lcp_L(file, dolcp, (SuffixDLL) suffixDLL, buffer, specialCharacterOrder);
         } else {
            throw new IllegalArgumentException("Method '" + method + "' only suits to type 'SuffixDLL'!");
         }
      } else {
         // no more use for it
         sequence = null;
         alphabet = null;
         throw new IllegalArgumentException("Unsupported construction method '" + method + "'!");
      }

      // no more use for it
      sequence = null;
      alphabet = null;

      return returnvalue;
   }

   /**
    * lcp computation according to Kasai et al.'s algorithm when lexprevpos[] is available.
    * 
    * @param file
    *           Basic file name for lcp array.
    * @param dolcp
    *           Which lcp arrays to compute (0..7, any combination of 1+2+4).
    * @param suffixdll
    *           Suffix list for that the lcp is calculated.
    * @param buffer
    *           Buffer for interim results. If the buffer has less capacity as suffixDLL, a new
    *           array is initialized and used as buffer.
    * @param specialCharacterOrder 
    *           How to compare/order special characters. Valid methods are 'pos' and 'suffix'.
    * @throws IOException
    *            when the lcp arrays can not be written to disc.
    */
   private static LcpInfo lcp_L(File file, int dolcp, SuffixDLL suffixdll, int[] buffer, String specialCharacterOrder)
         throws IOException {
      // buffer must be long enough
      if (buffer.length < suffixdll.capacity()) {
         buffer = new int[suffixdll.capacity()];
      }

      int maxlcp = -1;
      int lcp1x = 0; // lcp1 exceptions
      int lcp2x = 0; // lcp2 exceptions

      TicToc timer = null;
      if (logger != null) {
         // time is only needed when a logger exists
         timer = new TicToc();
      }
      int p, prev, h;
      h = 0;
      for (p = 0; p < suffixdll.capacity(); p++) {
         prev = suffixdll.getLexPreviousPos(p);
         if (prev != -1)
            h = suffixlcp(prev, p, h, specialCharacterOrder);
         else {
            assert (h == 0);
         }
         assert (h >= 0);
         if (h > maxlcp)
            maxlcp = h;
         buffer[p] = h; // only a buffer and not the real lcp array (different order!!!)
         if (h >= 255)
            lcp1x++;
         if (h >= 65535)
            lcp2x++;
         if (h > 0)
            h--;
      }
      if (logger != null) {
         logger.info("suffixtray: lcp computation took %.2f secs; writing...", timer.tocs());
      }

      int r;
      int chi = suffixdll.getLowestCharacter();
      ArrayFile f4 = null, f2 = null, f1 = null, f2x = null, f1x = null;
      if ((dolcp & 4) != 0)
         f4 = new ArrayFile(file).openW();
      if ((dolcp & 2) != 0) {
         f2 = new ArrayFile(file + "2").openW();
         f2x = new ArrayFile(file + "2x").openW(); // TODO original call was with 0 as second
                                                   // parameter
      }
      if ((dolcp & 1) != 0) {
         f1 = new ArrayFile(file + "1").openW();
         f1x = new ArrayFile(file + "1x").openW(); // TODO original call was with 0 as second
                                                   // parameter
      }
      for (r = 0, p = suffixdll.getFirstPos(chi); p != -1; p = suffixdll.getLexNextPos(p), r++) {
         h = buffer[p];
         assert (h >= 0);
         if ((dolcp & 4) != 0) {
            f4.writeInt(h);
         }
         if ((dolcp & 2) != 0) {
            if (h >= 65535) {
               f2.writeShort((short) -1);
               f2x.writeInt(r);
               f2x.writeInt(h);
            } else
               f2.writeShort((short) h);
         }
         if ((dolcp & 1) != 0) {
            if (h >= 255) {
               f1.writeByte((byte) -1);
               f1x.writeInt(r);
               f1x.writeInt(h);
            } else
               f1.writeByte((byte) h);
         }
      }
      assert (r == suffixdll.capacity());
      if ((dolcp & 4) != 0)
         f4.close();
      if ((dolcp & 2) != 0) {
         f2.close();
         f2x.close();
      }
      if ((dolcp & 1) != 0) {
         f1.close();
         f1x.close();
      }
      
      return new LcpInfo(maxlcp, lcp1x, lcp2x);
   }

   /**
    * Finds length of longest common prefix (lcp) of suffixes of associated text/sequence, given
    * prior knowledge that lcp &gt;= h.
    * 
    * @param i
    *           Position of first suffix.
    * @param j
    *           Position of second suffix.
    * @param h
    *           Minimum known lcp length.
    * @param specialCharacterOrder 
    *           How to compare/order special characters. Valid methods are 'pos' and 'suffix'.
    * @return lcp length
    */
   private static final int suffixlcp(final int i, final int j, final int h, String specialCharacterOrder) {
      if (i == j)
         return sequence.length - i + 1;
      int off;
      for (off = h; scmp(i + off, j + off, specialCharacterOrder) == 0; off++) {
      }
      return off;
   }

   /**
    * Compares two characters of associated text/sequence. "Symbols" are compared according to their
    * order in the alphabet map. "Special" characters (wildcards, separators, endofline) are
    * compared by position or by their order in the alphabet map (depends on the given
    * 'specialCharacterOrder').
    * 
    * @param i
    *           Position of first suffix.
    * @param j
    *           Position of second suffix.
    * @param specialCharacterOrder
    *           How to compare/order special characters. Valid methods are 'pos' and 'suffix'.
    * @return Any value &lt; 0 iff s[i]&lt;s[j], as specified by alphabet map, zero(0) iff
    *         s[i]==s[j], any value &gt; 0 iff s[i]&gt;s[j], as specified by alphabet map.
    */
   // !!!the same method exist in SuffixTrayChecker - duplication is needed to get proper
   // decoupling!!!//
   private static final int scmp(final int i, final int j, String specialCharacterOrder) {
      final int d = sequence[i] - sequence[j];
      if (d != 0 || alphabet.isSymbol(sequence[i])) {
         return d;
      }
      if (specialCharacterOrder.equals("pos")) {
         return i - j;
      } else {
         assert specialCharacterOrder.equals("suffix");
         return d;
      }
   }
   
   /**
    * Record class to store informations about the lcp calculation process.
    * 
    * @author Markus Kemmerling
    */
   public static class LcpInfo {
      /** Max lcp value. */
      public final int maxlcp;
      
      /** Number of lcp1 exceptions. */
      public final int lcp1x;

      /** Number of lcp2 exceptions. */
      public final int lcp2x;

      /**
       * 
       * @param maxlpc
       *           Max lcp value.
       * @param lcp1x
       *           Number of lcp1 exceptions.
       * @param lcp2x
       *           Number of lcp2 exceptions.
       */
      private LcpInfo(int maxlpc, int lcp1x, int lcp2x) {
         this.maxlcp = maxlpc;
         this.lcp1x = lcp1x;
         this.lcp2x = lcp2x;
      }
   }
}
