package verjinxer;

import java.util.Arrays;
import java.util.Comparator;

import verjinxer.sequenceanalysis.Alphabet;

/**
 * Record type for suffixes and some simple methods for building suffix arrays.
 * 
 * @author Markus Kemmerling
 */
public class Suffix {

   /** The suffix itself */
   public byte[] string;

   /** The position of the suffix in the origin text */
   public int pos;

   /**
    * Builds a suffix array for the given sequence.
    * 
    * @param sequence
    *           The suffix array is build for this sequence.
    * @param comparator
    *           The suffixes within the arrays are ordered according to this comparator.
    * @return The suffix array.
    */
   static Suffix[] buildSuffixArray(byte[] sequence, Comparator<Suffix> comparator) {
      Suffix[] sa = new Suffix[sequence.length];
      for (int i = 0; i < sequence.length; i++) {
         sa[i] = new Suffix();
         sa[i].pos = i;
         sa[i].string = Arrays.copyOfRange(sequence, i, sequence.length);
      }

      Arrays.sort(sa, comparator);

      return sa;
   }

   /************************************************************************************
    *********************** Comparators for ordering suffixes***************************
    ************************************************************************************/

   /**
    * Compares two suffixes character by character lexicographically until a special character
    * occurs. Special characters are compared by there position in the origin text.
    */
   static class ComparatorByPos implements Comparator<Suffix> {

      private final Alphabet alphabet;

      public ComparatorByPos(Alphabet alphabet) {
         super();
         this.alphabet = alphabet;
      }

      /**
       * Compares two suffixes and calculates the lcp value (longest common prefix).<br>
       * If 0 is returned, both suffixes are the same.<br>
       * If -i (a negative integer) is returned, the first suffix stands before the second in a
       * suffix array. The lcp value is '(i-1)'.<br>
       * If i (a positive integer) is returned, the first suffix stands behind the second in a suffix
       * array. The lcp value is '(i-1')'.<br>
       * The lcp value can be received with ' i==0 ? o1.length : Math.abs(i)-1', where 'i' is the
       * return value.
       */
      @Override
      public int compare(Suffix o1, Suffix o2) {
         for (int i = 0; i < o1.string.length && i < o2.string.length; i++) {
            if (o1.string[i] < o2.string[i]) {
               return -(i + 1);
            } else if (o1.string[i] > o2.string[i]) {
               return (i + 1);
            } else if (alphabet.isSpecial(o1.string[i])) {
               if (o1.pos < o2.pos) {
                  return -(i + 1);
               } else if (o1.pos > o2.pos) {
                  return (i + 1);
               }
            }
         }

         if (o1.string.length < o2.string.length) {
            return -(o1.string.length + 1);
         } else if (o1.string.length > o2.string.length) {
            return (o2.string.length + 1);
         } else {
            return 0;
         }
      }

   }

   /**
    * Compares two suffixes character by character lexicographically. Even special characters are
    * compared lexicographically.
    */
   static class ComparatorBySuffix implements Comparator<Suffix> {

      /**
       * Compares two suffixes and calculates the lcp value (longest common prefix).<br>
       * If 0 is returned, both suffixes are the same.<br>
       * If -i (a negative integer) is returned, the first suffix stands before the second in a
       * suffix array. The lcp value is '(i-1)'.<br>
       * If i (a positive integer) is returned, the first suffix stands behind the second in a suffix
       * array. The lcp value is '(i-1')'.<br>
       * Generally the lcp value can be received with ' i==0 ? o1.length : Math.abs(i)-1', where 'i'
       * is the return value.
       */
      @Override
      public int compare(Suffix o1, Suffix o2) {
         for (int i = 0; i < o1.string.length && i < o2.string.length; i++) {
            if (o1.string[i] < o2.string[i]) {
               return -(i + 1);
            } else if (o1.string[i] > o2.string[i]) {
               return (i + 1);
            }
         }

         if (o1.string.length < o2.string.length) {
            return -(o1.string.length + 1);
         } else if (o1.string.length > o2.string.length) {
            return (o2.string.length + 1);
         } else {
            return 0;
         }
      }

   }
}
