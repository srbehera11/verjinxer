package verjinxer.sequenceanalysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * This class is a special QGramCoder that simulates bisulfite treatment on its input sequence.
 * It creates all possible q-grams. The alphabet size is fixed to four.
 * 
 * Make sure that different threads use different instances of a BisulfiteQGramCoder.
 *
 * @author Marcel Martin
 * @author Sven Rahmann
 */
public final class BisulfiteQGramCoder {
   
   public final QGramCoder coder; // underlying q-gram coder
   public final int q;            // q-gram length
   
   /**
    * A cache for the results of the bisulfite treatment. Note that the indexing is special:
    * To get the actual index into the cache, use qCodeToIndex.
    */
   private final int[][] cache; // qgrams bis.-compatible to a given q-gram
   private final boolean[] cached;
   private final ArrayList<Integer> tmpQCodes;
   private final byte[] tmpQGram;
   private final int[] empty;
   
   /**
    * Creates a new BisulfiteQGramCoder. The alphabet size is fixed to 4 (A, C, G, T).
    * @param q length of the q-grams coded by this instance
    */
   public BisulfiteQGramCoder(int q) {
      coder = new QGramCoder(q, ASIZE);
      this.q = q;
      empty = new int[0];
      cache = new int[coder.numberOfQGrams * 4][];
      cached = new boolean[coder.numberOfQGrams];
      for (int i = 0; i < cached.length; ++i) 
         cached[i] = false;
      tmpQGram = new byte[q];
      final int zweiHochQHalbe = (1<<((q+1)/2)) + 2; // 2^ceil(q/2) plus some extra space
      tmpQCodes = new ArrayList<Integer>(zweiHochQHalbe);
   }

   /* TODO delete this comment 
    * For each q-code, computes the q-codes that are
    * compatible under bisulfite treatment rules.
    * For each q-code, there are four different possible
    * sets of compatible q-codes, depending on whether
    * the corresponding q-gram is preceded by a C or
    * followed by a G. The four different combinations are
    * numbered as follows:
    * 0: no C | qgram | no G
    * 1: no C | qgram | G
    * 2: C    | qgram | no G
    * 3: C    | qgram | G
    * 
    * compatibleQCodes accordingly contains, at each position, a four-element
    * array of int arrays.
    */


   // (hardcoded) encoding for nucleotides
   public static final byte NUCLEOTIDE_A = 0;
   public static final byte NUCLEOTIDE_C = 1;
   public static final byte NUCLEOTIDE_G = 2;
   public static final byte NUCLEOTIDE_T = 3;
   private static final int ASIZE = 4; // alphabet size

   /** Converts a qcode to an index usable for the cache array */
   private int qCodeToIndex(int qcode, boolean reverse, boolean specialBorder) {
      int i = qcode << 2;
      if (reverse) i |= 2;
      if (specialBorder) i |= 1;
      return i;
   }
   
   /**
    * Checks whether a given q-gram matches another given q-gram, 
    * under bisulfite replacement rules.
    * @param qgram given q-gram (in index, bis.-treated)
    * @param i     starting position within qgram
    * @param s     another given q-gram (original, in text)
    * @param p     starting position within s
    * @return true iff qgram[i..i+q-1] can be derived from s[p..p+q-1] under bisulfite rules.
    */   
   public boolean areCompatible(final byte[] qgram, final int i, final byte[] s, final int p) {
      int biCode = coder.code(qgram, i);
      boolean CBefore = (p > 0 && s[p-1] == NUCLEOTIDE_C);
      boolean GFollows = (p+coder.q < s.length && s[p+coder.q] == NUCLEOTIDE_G);
      
      int origCode = coder.code(s, p);
      if (!cached[origCode]) {
         computeCompatibleQCodes(origCode);
      }
      
      // forward
      for (int code : cache[qCodeToIndex(origCode, false, GFollows)]) {
         if (code == biCode) return true;
      }
      
      // reverse
      for (int code : cache[qCodeToIndex(origCode, true, CBefore)]) {
         if (code == biCode) return true;
      }

      return false; 
   }
   
   /** adds bisulfite qcodes of qcode to the cache. qcode must be >= 0 */
   private void computeCompatibleQCodes(int qcode) {
      coder.qGram(qcode, tmpQGram); // get the q-gram into tmpQGram (which is pre-allocated)
 
      ArrayList<Integer> result = tmpQCodes;
      
      // t is encoded as follows:
      // bit 0: specialBorder
      // bit 1: reverse

      for (int t = 0; t < 4; ++t) {
         boolean reverse = (t & 2) == 2;
         boolean specialBorder = (t & 1) == 1;

         int loneReactions = 0;
         result.clear();
         result.add(0);

         if (!reverse) {  // forward
            for (int p=0; p<q; p++) {
               final byte ch = tmpQGram[p];
               final byte chbi = (ch==NUCLEOTIDE_C)? NUCLEOTIDE_T : ch;
               final boolean GFollows = (p<q-1 && tmpQGram[p+1]==NUCLEOTIDE_G) || (p==q-1 && specialBorder);
               final boolean isCG = (ch==NUCLEOTIDE_C && GFollows);
               final int L = result.size();
               for (int l=0; l<L; l++) {
                  final int shifted = result.get(l)*4;
                  result.set(l,  shifted + chbi);
                  if (isCG) result.add(shifted+NUCLEOTIDE_C);
               }
               if (ch==NUCLEOTIDE_C && !GFollows) loneReactions++;
            }
         } else {         // reverse
            for (int p=0; p<q; p++) {
               final byte ch = tmpQGram[p];
               final byte chbi = (ch==NUCLEOTIDE_G)? NUCLEOTIDE_A : ch;
               final boolean CBefore = (p>0 && tmpQGram[p-1]==NUCLEOTIDE_C) || (p==0 && specialBorder);
               final boolean isCG = (ch==NUCLEOTIDE_G && CBefore);
               final int L = result.size();
               for (int l=0; l<L; l++) {
                  final int shifted = result.get(l)*4;
                  result.set(l, shifted + chbi);
                  if (isCG) result.add(shifted+NUCLEOTIDE_G);
               }
               if (ch==NUCLEOTIDE_G && !CBefore) loneReactions++;
            }
         } // end else (reverse case)
         if (loneReactions==0) { 
            // no lone reactions: we have re-generated the original q-code, too.
            // the original q-code must be the last one in the list -> remove it.
           final int last = result.size()-1;
           assert result.get(last) == qcode; // last inserted must be equal to original
           result.remove(last);
         }
         // add to cache 
         // TODO can this be done more efficiently?
         int[] c = new int[result.size()];
         for (int i = 0; i < result.size(); ++i) c[i] = result.get(i);
         cache[qcode<<2 | t] = c;
      }
      cached[qcode] = true;
   }
   
   /**
    * Returns 
    * @param s
    * @param p
    * @return
    */
   /*public ArrayList<byte[]> compatibleQGrams(final byte[] s, final int p) {
      throw new UnsupportedOperationException("Not yet implemented");
   }*/
   
   /**
    * For a given q-gram code (with alphabet size 4),
    * returns all q-gram codes that can arise from it by bisulfite treatment.
    * 
    * The result is stored in a cache. That is, if this function has been called before for a
    * qcode, it will return immediately.
    * 
    * The bisulfit replacement rules are:
    * (v1) all Cs -> T.
    * (v2) all Cs -> T, except CG remains CG;
    *      If there is a C in the last position, we need more information.
    * (v3) all Gs -> A.
    * (v4) all Gs -> A, except CG remains CG;
    *     If there is a G in the first q-gram position, we need more information.
    * @param qcode           the original q-gram code.
    * @param reverse         if true, produce revcomp-bisulfite-revcomp instead of forward-bisulfite q-gram codes
    * @param specialBorder   indicate whether there is a C after the last (if reverse, a G before the first) position
    * @return a list of bisulfite-treated q-gram codes for the given q-gram code.
    */
   public int[] bisulfiteQCodes(final int qcode, final boolean reverse, final boolean specialBorder) {
      if (qcode<0) return empty;
      if (!cached[qcode]) {
         computeCompatibleQCodes(qcode);
      }
      return cache[qCodeToIndex(qcode, reverse, specialBorder)];
   }
   
   /** @return the cache fill ratio (between 1 and 0) */
   public double getCacheFill() {
      int c = 0;
      for (int i = 0; i < cached.length; ++i) {
         if (cached[i]) c++;
      }
      return (double)c/cached.length;
   }
}
