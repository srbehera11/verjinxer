package verjinxer;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.IntBuffer;

import com.spinn3r.log5j.Logger;

import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.sequenceanalysis.ISuffixDLL;
import verjinxer.sequenceanalysis.SuffixDLL;
import verjinxer.sequenceanalysis.SuffixXorDLL;

/**
 * Class responsible for checking the correctness of a suffix array of a text/sequence.
 * 
 * @author Markus Kemmerling
 */
public class SuffixTrayChecker {

   // attributes are there to reduce number of parameters for private methods
   // they are set in checkpos(ISuffixDLL, String) or in checkpos(Project)
   // and are used in checkpos_X(SuffixDLL), scmp(int, int) and suffixcmp(int, int)
   private static byte[] sequence = null;
   private static Alphabet alphabet = null;

   private static Logger log = null;

   public static void setLogger(Logger log) {
      SuffixTrayChecker.log = log;
   }

   /**
    * Checks the given suffix list with the given method.
    * 
    * @param suffixDLL
    *           Suffix list to check.
    * @param method
    *           How the suffix list shall be checked. Valid methods are 'L', 'R', 'minLR', 'bothLR'
    *           and 'bothLR2'.
    * @param specialCharacterOrder 
    *           How to compare/order special characters. Valid methods are 'pos' and 'suffix'.
    * @return 0 iff everything was okay.<br>
    *         The lowest bit is set to 1 when there are failures in the sorting of the suffixes.<br>
    *         The second bit is set to 1 when no first character can be found or when not all
    *         suffixes of the with the list associated text/sequences are included in the list.
    * @throws IllegalArgumentException
    *            when the given method is not valid or when the given method does not suits to the
    *            type of suffixDLL.
    */
   public static int checkpos(ISuffixDLL suffixDLL, String method, final String specialCharacterOrder) throws IllegalArgumentException {
      SuffixTrayChecker.sequence = suffixDLL.getSequence().array();
      SuffixTrayChecker.alphabet = suffixDLL.getAlphabet();

      int returnvalue = 0;

      if (method.equals("L")) {
         if (suffixDLL instanceof SuffixDLL) {
            returnvalue = checkpos_R((SuffixDLL) suffixDLL, specialCharacterOrder);
         } else {
            throw new IllegalArgumentException("Method '" + method + "' only suits to type 'SuffixDLL'!");
         }
      } else if (method.equals("R")) {
         if (suffixDLL instanceof SuffixDLL) {
            returnvalue = checkpos_R((SuffixDLL) suffixDLL, specialCharacterOrder);
         } else {
            throw new IllegalArgumentException("Method '" + method + "' only suits to type 'SuffixDLL'!");
         }
      } else if (method.equals("minLR")) {
         if (suffixDLL instanceof SuffixDLL) {
            returnvalue = checkpos_R((SuffixDLL) suffixDLL, specialCharacterOrder);
         } else {
            throw new IllegalArgumentException("Method '" + method + "' only suits to type 'SuffixDLL'!");
         }
      } else if (method.equals("bothLR")) {
         if (suffixDLL instanceof SuffixXorDLL) {
            returnvalue = checkpos_bothLR((SuffixXorDLL) suffixDLL, specialCharacterOrder);
         } else {
            throw new IllegalArgumentException("Method '" + method + "' only suits to type 'SuffixXorDLL'!");
         }
      } else if (method.equals("bothLR2")) {
         if (suffixDLL instanceof SuffixDLL) {
            returnvalue = checkpos_R((SuffixDLL) suffixDLL, specialCharacterOrder);
         } else {
            throw new IllegalArgumentException("Method '" + method + "' only suits to type 'SuffixDLL'!");
         }
      } else {
         // no more use for it
         SuffixTrayChecker.sequence = null;
         SuffixTrayChecker.alphabet = null;
         throw new IllegalArgumentException("Unsupported construction method '" + method + "'!");
      }

      // no more use for it
      SuffixTrayChecker.sequence = null;
      SuffixTrayChecker.alphabet = null;

      return returnvalue;
   }

   /**
    * Checks correctness of the given suffix list.
    * 
    * @param suffixDLL
    *           Suffix list to check.
    * @param specialCharacterOrder 
    *           How to compare/order special characters. Valid methods are 'pos' and 'suffix'.
    * @return 0 iff everything was okay.<br>
    *         The lowest bit is set to 1 when there are failures in the sorting of the suffixes.<br>
    *         The second bit is set to 1 when no first character can be found or when not all
    *         suffixes of the with the list associated text/sequences are included in the list.
    */
   private static int checkpos_R(SuffixDLL suffixDLL, final String specialCharacterOrder) {
      // this method is a copy of checkpos_bothLR(SuffixXorDLL).
      // the parameter is the only difference. the body is exactly the same
      // we don't use a single method with ISuffixDLL as parameter to get
      // a static linking with inlining instead of a polymorphy and dynamic linking.
      // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
      int chi, nn, comp;
      int returnvalue = 0;
      chi = suffixDLL.getLowestCharacter();
      if (chi >= 256) {
         if (sequence.length == 0) {
            return 0;
         }
         if (log != null) {
            log.warn("suffixcheck: no first character found, but |s|!=0.");
         }
         return 2;
      }
      suffixDLL.resetToBegin();
      assert (suffixDLL.getCurrentPosition() != -1);
      nn = 1;
      while (suffixDLL.hasNextUp()) {
         // if (log != null) {
         // log.info("  pos %d vs %d; text %d vs %d", suffixDLL.getCurrentPosition(),
         // suffixDLL.getSuccessor(), sequence[suffixDLL.getCurrentPosition()],
         // sequence[suffixDLL.getSuccessor()]);
         // }
         if (!((comp = suffixcmp(suffixDLL.getCurrentPosition(), suffixDLL.getSuccessor(), specialCharacterOrder)) < 0)) {
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
    * Checks correctness of the given suffix list.
    * 
    * @param suffixDLL
    *           Suffix list to check.
    * @param specialCharacterOrder 
    *           How to compare/order special characters. Valid methods are 'pos' and 'suffix'.
    * @return 0 iff everything was okay.<br>
    *         The lowest bit is set to 1 when there are failures in the sorting of the suffixes.<br>
    *         The second bit is set to 1 when no first character can be found or when not all
    *         suffixes of the with the list associated text/sequences are included in the list.
    */
   private static int checkpos_bothLR(SuffixXorDLL suffixDLL, final String specialCharacterOrder) {
      // this method is a copy of checkpos_R(SuffixDLL).
      // the parameter is the only difference. the body is exactly the same
      // we don't use a single method with ISuffixDLL as parameter to get
      // a static linking with inlining instead of a polymorphy and dynamic linking.
      // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
      int chi, nn, comp;
      int returnvalue = 0;
      chi = suffixDLL.getLowestCharacter();
      if (chi >= 256) {
         if (sequence.length == 0) {
            return 0;
         }
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
         if (!((comp = suffixcmp(suffixDLL.getCurrentPosition(), suffixDLL.getSuccessor(), specialCharacterOrder)) < 0)) {
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
    * Checks correctness of a suffix array on disk outputs warning messages if errors are found
    * 
    * @param project
    *            Project for that the suffix array shall be checked.
    * @return 0 on success, 1 on sorting error, 2 on count error
    * @throws IOException
    *            When the pos file can not be read.
    */
   public static int checkpos(Project project) throws IOException {
      int returnvalue = 0;

      sequence = project.readSequences().array();
      alphabet = project.readAlphabet();
      final String specialCharacterOrder = project.getStringProperty("specialCharacterOrder4Suffix");
      IntBuffer pos = project.readSuffixArray();
      int p = pos.get();
      int nextp, comp;
      int nn = 1;
      while (true) {
         try {
            nextp = pos.get();
         } catch (BufferUnderflowException ex) {
            break;
         }
         if (!((comp = suffixcmp(p, nextp, specialCharacterOrder)) < 0)) {
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
      // no more use for it
      sequence = null;
      alphabet = null;
      return returnvalue;
   }

   // ==================== checking routines ====================================

   /**
    * Compares two characters of the associated text/sequence. "Symbols" are compared according to
    * their order in the alphabet map. "Special" characters (wildcards, separators, endofline) are
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
   //!!! the same method exist in LCP - duplication is needed to get proper decoupling !!!//
   private static final int scmp(final int i, final int j, final String specialCharacterOrder) {
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
    * Compares two suffixes of the associated text/sequence. "Symbols" are compared according to
    * their order in the alphabet map. "Special" characters (wildcards, separators, endofline) are
    * compared by position or by their order in the alphabet map (depends on the given
    * 'specialCharacterOrder').
    * 
    * @param i
    *           Position of first suffix.
    * @param j
    *           Position of second suffix.
    * @param specialCharacterOrder
    *           How to compare/order special characters. Valid methods are 'pos' and 'suffix'.
    * @return Any value &lt; 0 iff suffix(i)&lt;suffix(j) lexicographically, zero (0) iff i==j any
    *         value &gt; 0 iff suffix(i)&gt;suffix(j) lexicographically.
    */
   private static final int suffixcmp(final int i, final int j, final String specialCharacterOrder) {
      if (i == j)
         return 0;
      int off, c;
      for (off = 0; (c = scmp(i + off, j + off, specialCharacterOrder)) == 0; off++) {
      }
      // suffixes i and j disagree at offset off, thus have off characters in common
      return c;
   }

}
