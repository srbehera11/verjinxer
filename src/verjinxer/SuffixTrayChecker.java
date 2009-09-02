package verjinxer;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.IntBuffer;

import com.spinn3r.log5j.Logger;

import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.sequenceanalysis.ISuffixDLL;
import verjinxer.sequenceanalysis.Sequences;
import verjinxer.sequenceanalysis.SuffixXorDLL;
import verjinxer.util.ArrayFile;
import verjinxer.util.FileTypes;

/**
 * @author Markus Kemmerling
 */
public class SuffixTrayChecker {

   // attributes are there to reduce number of parameters for private methods
   // they are set in checkpos(ISuffixDLL, String, Sequences, Alphabet) or in checkpos(Project,
   // Logger)
   private static byte[] sequence = null;
   private static Alphabet alphabet = null;

   private static Logger log = null;

   public static void setLogger(Logger log) {
      SuffixTrayChecker.log = log;
   }

   public static int checkpos(ISuffixDLL suffixDLL, String method, Sequences sequence,
         Alphabet alphabet) {
      SuffixTrayChecker.sequence = sequence.array();
      SuffixTrayChecker.alphabet = alphabet;

      if (method.equals("L")) {
         // buildpos_L();
         throw new UnsupportedOperationException("Method " + method + "is temporary not supported.");
      } else if (method.equals("R")) {
         // buildpos_R();
         throw new UnsupportedOperationException("Method " + method + "is temporary not supported.");
      } else if (method.equals("minLR")) {
         // buildpos_minLR(false);
         throw new UnsupportedOperationException("Method " + method + "is temporary not supported.");
      } else if (method.equals("bothLR")) {
         if (suffixDLL instanceof SuffixXorDLL) {
            return checkpos_bothLR((SuffixXorDLL) suffixDLL);
         } else {
            // TODO ???
            return 1;
         }
      } else if (method.equals("bothLR2")) {
         // buildpos_bothLR2();
         throw new UnsupportedOperationException("Method " + method + "is temporary not supported.");
      } else {
         throw new IllegalArgumentException("The Method " + method + " does not exist.");
      }
   }

   private static int checkpos_bothLR(SuffixXorDLL suffixDLL) {
      int chi, nn, comp;
      int returnvalue = 0;
      chi = suffixDLL.getLowestCharacter();
      if (chi >= 256) {
         if (sequence.length == 0)
            return 0;
         return 2;
      }
      nn = 1;
      suffixDLL.resetToBegin();
      assert (suffixDLL.getCurrentPosition() != -1);
      while (suffixDLL.hasNextUp()) {
         // if (log != null) {
         // log.info("  pos %d vs %d; text %d vs %d", suffixDLL.getCurrentPosition(),
         // suffixDLL.getSuccessor(), sequence[suffixDLL.getCurrentPosition()],
         // sequence[suffixDLL.getSuccessor()]);
         // }
         if (!((comp = suffixcmp(suffixDLL.getCurrentPosition(), suffixDLL.getSuccessor())) < 0)) {
            if (log != null) {
               log.warn(
                     "suffixcheck: sorting error at ranks %d, %d; pos %d, %d; text %d, %d; cmp %d",
                     nn - 1, nn, suffixDLL.getCurrentPosition(), suffixDLL.getSuccessor(),
                     sequence[suffixDLL.getCurrentPosition()], sequence[suffixDLL.getSuccessor()],
                     comp);
            }
            returnvalue = 1;
         }
         suffixDLL.nextUp();
         nn++;
      }
      if (nn != sequence.length) {
         if (log != null) {
            log.warn("suffixcheck: missing some suffixes; have %d / %d.", nn, sequence.length);
         }
         returnvalue += 2;
      }
      return returnvalue;
   }

   /**
    * check correctness of a suffix array on disk outputs warning messages if errors are found
    * 
    * @param log
    * 
    * @param di
    *           path and name of the index to check
    *@return 0 on success, 1 on sorting error, 2 on count error
    *@throws IOException
    *            when the pos file can not be read.
    */
   public static int checkpos(Project project) throws IOException {
      int returnvalue = 0;
      ArrayFile fpos = null;
      IntBuffer pos = null;

      sequence = project.readSequences().array();
      alphabet = project.readAlphabet();

      fpos = new ArrayFile(project.makeFile(FileTypes.POS), 0);
      pos = fpos.mapR().asIntBuffer();
      int p = pos.get();
      int nextp, comp;
      int nn = 1;
      while (true) {
         try {
            nextp = pos.get();
         } catch (BufferUnderflowException ex) {
            break;
         }
         if (!((comp = suffixcmp(p, nextp)) < 0)) {
            if (log != null) {
               log.warn(
                     "suffixcheck: sorting error at ranks %d, %d; pos %d, %d; text %d, %d; cmp %d",
                     nn - 1, nn, p, nextp, sequence[p], sequence[nextp], comp);
            }
            returnvalue = 1;
         }
         nn++;
         p = nextp;
      }
      if (nn != sequence.length) {
         if (log != null) {
            log.warn("suffixcheck: missing some suffixes; have %d / %d.", nn, sequence.length);
         }
         returnvalue += 2;
      }
      return returnvalue;
   }

   // ==================== checking routines ====================================

   /**
    * compare two characters of text s. "Symbols" are compared according to their order in the
    * alphabet map "special" characters (wildcards, separators) are compared by position
    * 
    * @param i
    *           first position
    *@param j
    *           second position
    *@return any value &lt; 0 iff s[i]&lt;s[j], as specified by alphabet map, zero(0) iff
    *         s[i]==s[j], any value &gt; 0 iff s[i]&gt;s[j], as specified by alphabet map.
    */
   private static final int scmp(final int i, final int j) {
      final int d = sequence[i] - sequence[j];
      if (d != 0 || alphabet.isSymbol(sequence[i]))
         return d;
      return i - j;
   }

   /**
    * compare two suffixes of text s. "Symbols" are compared according to their order in the
    * alphabet map "special" characters (wildcards, separators) are compared by position
    * 
    * @param i
    *           first position
    *@param j
    *           second position
    *@return any value &lt; 0 iff suffix(i)&lt;suffix(j) lexicographically, zero (0) iff i==j any
    *         value &gt; 0 iff suffix(i)&gt;suffix(j) lexicographically.
    */
   private static final int suffixcmp(final int i, final int j) {
      if (i == j)
         return 0;
      int off, c;
      for (off = 0; (c = scmp(i + off, j + off)) == 0; off++) {
      }
      // suffixes i and j disagree at offset off, thus have off characters in common
      return c;
   }

}
