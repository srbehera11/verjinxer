package verjinxer;

import java.io.File;
import java.io.IOException;

import com.spinn3r.log5j.Logger;

import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.sequenceanalysis.BigSuffixDLL;
import verjinxer.sequenceanalysis.BigSuffixXorDLL;
import verjinxer.sequenceanalysis.IBigSuffixDLL;
import verjinxer.sequenceanalysis.SuffixDLL;
import verjinxer.sequenceanalysis.SuffixXorDLL;
import verjinxer.util.ArrayFile;
import verjinxer.util.HugeByteArray;
import verjinxer.util.HugeLongArray;
import verjinxer.util.TicToc;

/**
 * @author Markus Kemmerling
 */
public class BigLCP {

   private static Logger logger = null;
   // attributes are there to reduce number of parameters for private methods
   // they are set in buildLcpAndWriteToFile(IBigSuffixDLL, String, int, File, long[])
   // and used in suffixlcp(int, int, int) and scmp(int, int)
   private static HugeByteArray sequence;
   private static Alphabet alphabet;
   

   public static void setLogger(Logger logger) {
      BigLCP.logger = logger;
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
    * @param dolcp
    *           Which lcp arrays to compute (0..7, any combination of 1+2+4)
    * @param file
    *           Basic file name to store the lcp arrays.
    * @param buffer
    *           Buffer for interim results. If the buffer has less capacity as suffixDLL, a new
    *           arrays is initialized and used as buffer.
    * @return Informations about the building process.
    * @throws IOException
    *            when the lcp arrays can not be written to disc.
    * @throws IllegalArgumentException
    *            when the given method is not valid or when the given method does not suits to the
    *            type of suffixDLL.
    */
   public static BigLCP.LcpInfo buildLcpAndWriteToFile(IBigSuffixDLL suffixDLL, String method,
         int dolcp, File file, HugeLongArray buffer) throws IOException, IllegalArgumentException {
      sequence = suffixDLL.getSequence();
      alphabet = suffixDLL.getAlphabet();

      LcpInfo returnvalue = null;

      if (method.equals("L")) {
         if (suffixDLL instanceof BigSuffixDLL) {
            returnvalue = lcp_L(file, dolcp, (BigSuffixDLL) suffixDLL, buffer);
         } else {
            throw new IllegalArgumentException("Method '" + method + "' only suits to type 'SuffixDLL'!");
         }
      } else if (method.equals("R")) {
         if (suffixDLL instanceof BigSuffixDLL) {
            returnvalue = lcp_L(file, dolcp, (BigSuffixDLL) suffixDLL, buffer);
         } else {
            throw new IllegalArgumentException("Method '" + method + "' only suits to type 'SuffixDLL'!");
         }
      } else if (method.equals("minLR")) {
         if (suffixDLL instanceof BigSuffixDLL) {
            returnvalue = lcp_L(file, dolcp, (BigSuffixDLL) suffixDLL, buffer);
         } else {
            throw new IllegalArgumentException("Method '" + method + "' only suits to type 'SuffixDLL'!");
         }
      } else if (method.equals("bothLR")) {
         if (suffixDLL instanceof BigSuffixXorDLL) {
            // lcp_bothLR(flcp, dolcp);
            throw new UnsupportedOperationException("Not yet implemented");
         } else {
            throw new IllegalArgumentException("Method '" + method + "' only suits to type 'SuffixXorDLL'!");
         }
      } else if (method.equals("bothLR2")) {
         if (suffixDLL instanceof BigSuffixDLL) {
            returnvalue = lcp_L(file, dolcp, (BigSuffixDLL) suffixDLL, buffer);
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
    *           arrays is initialized and used as buffer.
    * @throws IOException
    *            when the lcp arrays can not be written to disc.
    */
   private static BigLCP.LcpInfo lcp_L(File file, int dolcp,
         BigSuffixDLL suffixdll, HugeLongArray buffer) throws IOException {
      // buffer must be long enough
      if (buffer.length < suffixdll.capacity()) {
         buffer = new HugeLongArray(suffixdll.capacity());
      }

      long maxlcp = -1;
      long lcp1x = 0; // lcp1 exceptions
      long lcp2x = 0; // lcp2 exceptions
      long lcp4x = 0; // lcp4 exceptions

      TicToc timer = null;
      if (logger != null) {
         // time is only needed when a logger exists
         timer = new TicToc();
      }
      long p, prev, h;
      h = 0;
      for (p = 0; p < suffixdll.capacity(); p++) {
         prev = suffixdll.getLexPreviousPos(p);
         if (prev != -1)
            h = suffixlcp(prev, p, h);
         else {
            assert (h == 0);
         }
         assert (h >= 0);
         if (h > maxlcp)
            maxlcp = h;
         buffer.set(p, h); // only a buffer and not the real lcp array (different order!!!)
         if (h >= 255)
            lcp1x++;
         if (h >= 65535)
            lcp2x++;
         if (h >= (1L << 32) - 1)
            lcp4x++;
         if (h > 0)
            h--;
      }
      if (logger != null) {
         logger.info("suffixtray: lcp computation took %.2f secs; writing...", timer.tocs());
      }

      long r;
      int chi = suffixdll.getLowestCharacter();
      ArrayFile f8 = null, f4 = null, f2 = null, f1 = null, f4x = null, f2x = null, f1x = null;
      if ((dolcp & 8) != 0) {
         f8 = new ArrayFile(file + "8").openW();
      }
      if ((dolcp & 4) != 0) {
         f4 = new ArrayFile(file + "4").openW();
         f4x = new ArrayFile(file + "4x").openW(); // TODO original call was with 0 as second
                                                   // parameter
      }
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
         h = buffer.get(p);
         assert (h >= 0);
         if ((dolcp & 8) != 0) { // TODO this bit is never set in dolcp, so we never write to f8.
            f8.writeLong(h);
         }
         if ((dolcp & 4) != 0) {
            if (h >= (1L << 32) - 1) {
               f4.writeInt((int) -1);
               f4x.writeLong(r);
               f4x.writeLong(h);
            } else
               f4.writeInt((int) h);
         }
         if ((dolcp & 2) != 0) {
            if (h >= 65535) {
               f2.writeShort((short) -1);
               f2x.writeLong(r);
               f2x.writeLong(h);
            } else
               f2.writeShort((short) h);
         }
         if ((dolcp & 1) != 0) {
            if (h >= 255) {
               f1.writeByte((byte) -1);
               f1x.writeLong(r);
               f1x.writeLong(h);
            } else
               f1.writeByte((byte) h);
         }
      }
      assert (r == suffixdll.capacity());
      if ((dolcp & 8) != 0)
         f8.close();
      if ((dolcp & 4) != 0) {
         f4.close();
         f4x.close();
      }
      if ((dolcp & 2) != 0) {
         f2.close();
         f2x.close();
      }
      if ((dolcp & 1) != 0) {
         f1.close();
         f1x.close();
      }
      
      return new LcpInfo(maxlcp, lcp1x, lcp2x, lcp4x);
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
    * @return lcp length
    */
   private static long suffixlcp(long i, long j, long h) {
      if (i == j)
         return sequence.length - i + 1;
      long off;
      for (off = h; scmp(i + off, j + off) == 0; off++) {
      }
      return off;
   }

   /**
    * Compares two characters of associated text/sequence. "Symbols" are compared according to their order in the
    * alphabet map. "Special" characters (wildcards, separators) are compared by position.
    * 
    * @param i
    *           Position of first suffix.
    * @param j
    *           Position of second suffix.
    * @return Any value &lt; 0 iff s[i]&lt;s[j], as specified by alphabet map, zero(0) iff
    *         s[i]==s[j], any value &gt; 0 iff s[i]&gt;s[j], as specified by alphabet map.
    */
   // !!!the same method exist in BigSuffixTrayChecker - duplication is needed to get proper
   // decoupling!!!//
   private static long scmp(long i, long j) {
      final int d = sequence.get(i) - sequence.get(j);
      if (d != 0 || alphabet.isSymbol(sequence.get(i)))
         return d;
      return i - j;
   }

   /**
    * Record class to store informations about the lcp calculation process.
    * 
    * @author Markus Kemmerling
    */
   public static class LcpInfo {
      /** Max lcp value. */
      public final long maxlcp;
      
      /** Number of lcp1 exceptions. */
      public final long lcp1x;

      /** Number of lcp2 exceptions. */
      public final long lcp2x;
      
      /** Number of lcp4 exceptions. */
      public final long lcp4x;

      /**
       * 
       * @param maxlpc
       *           Max lcp value.
       * @param lcp1x
       *           Number of lcp1 exceptions.
       * @param lcp2x
       *           Number of lcp2 exceptions.
       * @param lcp4x
       *           Number of lcp4 exceptions.
       */
      private LcpInfo(long maxlpc, long lcp1x, long lcp2x, long lcp4x) {
         this.maxlcp = maxlpc;
         this.lcp1x = lcp1x;
         this.lcp2x = lcp2x;
         this.lcp4x = lcp4x;
      }
   }
}
