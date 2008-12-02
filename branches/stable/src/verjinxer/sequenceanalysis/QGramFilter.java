package verjinxer.sequenceanalysis;

import java.util.Arrays;
import verjinxer.util.BitArray;

/**
 * This class implements a filter for q-grams based on a BitArray. Each q-gram is represented by a
 * bit. If q-gram 'c' is to be ignored (filtered), bit number c is set to 1, and to 0 otherwise.
 * 
 * @author Sven Rahmann
 */
public class QGramFilter {

   /** the q-gram length */
   public final int q;

   /** the alphabet size */
   public final int asize;

   /** the size of this filter (number of q-grams) */
   public final int size;

   /** the filter bits */
   public final BitArray bits;

   final int complexity;
   final int delta;

   public QGramFilter(final int q, final int asize, final int complexity, final int delta) {
      // using a QGramCoder performs range checking on q and asize.
      this(new QGramCoder(q, asize), complexity, delta);
   }

   public QGramFilter(final QGramCoder coder, final int complexity, final int delta) {
      q = coder.q;
      asize = coder.asize;
      size = coder.numberOfQGrams;
      bits = setFilter(coder, complexity, delta);
      this.complexity = complexity;
      this.delta = delta;
   }

   public QGramFilter(final int q, final int asize, final String filterparam) {
      QGramCoder coder = new QGramCoder(q, asize);
      this.q = q;
      this.asize = coder.asize;
      this.size = coder.numberOfQGrams;
      final int[] param = parseFilterParameters(filterparam);
      this.complexity = param[0];
      this.delta = param[1];
      bits = setFilter(coder, complexity, delta);
   }

   /**
    * create a low-complexity q-gram filter. This is a BitArray with the property that the c-th bit
    * is 1 iff the q-gram Q corresponding to code c is low-complexity. Low-complexity means that at
    * most numchar distinct characters occur in Q after removing one occurrence of the least
    * frequent character for delta times (see source code for details).
    * 
    * @param numchar
    *           complexity threshold; the q-gram is low-complexity if it consists of at most this
    *           many characters.
    * @param delta
    *           before computing the number of characters, remove the least frequent one for delta
    *           times.
    * @return the filter BitSet, low-complexity q-grams have their corresponding bit set.
    */
   private BitArray setFilter(final QGramCoder coder, final int complexity, final int delta) {
      final int aq = size;
      final BitArray f = new BitArray(aq);
      if (complexity == 0)
         return f; // nothing to do, all-0 filter
      final int[] freq = new int[asize];
      final byte[] qgram = new byte[q];

      for (int c = 0; c < aq; c++) {
         coder.qGram(c, qgram); // write the q-gram for c into array qgram, ignore return value

         // count the number 'nc' of distinct characters in 'qgram'
         int nc = 0;
         Arrays.fill(freq, 0);
         for (int i = 0; i < q; i++)
            freq[qgram[i]]++;
         for (int a = 0; a < asize; a++)
            if (freq[a] > 0)
               nc++;
         // if there are sufficiently few distinct characters in 'qgram', set filter; done.
         if (nc <= complexity) {
            f.set(c, 1);
            continue;
         }
         // otherwise, we need to consider the 'delta' parameter
         // that may reduce the number of distinct characters
         int d = delta;
         while (d > 0) {
            int minfreq = q + 1;
            int mina = asize;
            for (int a = 0; a < asize; a++)
               if (freq[a] > 0 && freq[a] < minfreq) {
                  minfreq = freq[a];
                  mina = a;
               }
            if (minfreq <= d) {
               freq[mina] = 0;
               d -= minfreq;
               nc--;
            } else {
               freq[mina] -= d;
               d = 0;
            }
         }
         // if there are now sufficiently few distinct characters in 'qgram', set filter.
         if (nc <= complexity)
            f.set(c, 1);
      } // end for c
      return f;
   }

   /** @return number of filtered q-grams */
   public int cardinality() {
      return bits.cardinality();
   }

   /**
    * change the filter status of a q-gram
    * 
    * @param c
    *           q-gram whose filter value to change
    * @param val
    *           new value (0 or 1)
    */
   public void set(final int c, final int val) {
      bits.set(c, val);
   }

   /**
    * get the filter status of a q-gram
    * 
    * @param c
    *           q-gram whose filter value to change
    * @return 1 iff q-gram c is filtered; 0 otherwise.
    */
   public int get(final int c) {
      return bits.get(c);
   }

   public int getComplexity() {
      return complexity;
   }

   public int getDelta() {
      return delta;
   }

   /**
    * get the filter status of a q-gram as a boolean
    * 
    * @param c
    *           q-gram whose filter value to change
    * @return true iff q-gram c is filtered; false otherwise.
    */
   public boolean isFiltered(final int c) {
      return (bits.get(c) != 0);
   }

   /**
    * parse a string with filter parameters into
    * 
    * @param filterparams
    *           a string of the form "numchar:delta". numchar is the complexity threshold; the
    *           q-gram is low-complexity if it consists of at most this many characters. delta is
    *           another parameter: before computing the number of characters, remove the least
    *           frequent one for delta times.
    * @return an integer array with the complexity threshold at index 0, the delta value at index 1.
    */
   private static int[] parseFilterParameters(final String filterparams) {
      if (filterparams == null)
         return new int[] { 0, 0 };
      String[] fstring = filterparams.split(":");
      final int ffc = Integer.parseInt(fstring[0]);
      final int ffd = (fstring.length < 2) ? 0 : Integer.parseInt(fstring[1]);
      return new int[] { ffc, ffd };
   }

}
