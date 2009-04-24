package verjinxer.sequenceanalysis;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;

import verjinxer.util.HugeByteArray;
import verjinxer.util.PositionQCodePair;

/**
 * This class is a special QGramCoder that simulates bisulfite treatment on its input sequence.
 * It creates all possible q-grams. The alphabet size is fixed to four.
 * 
 * Make sure that different threads use different instances of a BisulfiteQGramCoder.
 *
 * @author Marcel Martin
 * @author Sven Rahmann
 */
public final class BisulfiteQGramCoder extends QGramCoder {

   /**
    * A cache for the results of the bisulfite treatment. Note that the indexing is special:
    * To get the actual index into the cache, use qCodeToIndex.
    */
   private final int[][] cache; // qgrams bis.-compatible to a given q-gram
   private final boolean[] cached;
   private final ArrayList<Integer> tmpQCodes;
   private final byte[] tmpQGram;
   private final int[] empty = new int[0];

   // (hardcoded) encoding for nucleotides
   public static final byte NUCLEOTIDE_A = 0;
   public static final byte NUCLEOTIDE_C = 1;
   public static final byte NUCLEOTIDE_G = 2;
   public static final byte NUCLEOTIDE_T = 3;

   /**
    * Creates a new BisulfiteQGramCoder. The alphabet size is fixed to 4 (A, C, G, T).
    * @param q length of the q-grams coded by this instance
    */
   public BisulfiteQGramCoder(int q) {
      super(q, 4);
      cache = new int[numberOfQGrams * 4][];
      cached = new boolean[numberOfQGrams];
      for (int i = 0; i < cached.length; ++i) 
         cached[i] = false;
      tmpQGram = new byte[q];
      final int zweiHochQHalbe = (1<<((q+1)/2)) + 2; // 2^ceil(q/2) plus some extra space
      tmpQCodes = new ArrayList<Integer>(zweiHochQHalbe);
   }

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
   @Override
   public boolean areCompatible(final byte[] qgram, final int i, final byte[] s, final int p) {
      int biCode = code(qgram, i);
      boolean CBefore = (p > 0 && s[p-1] == NUCLEOTIDE_C);
      boolean GFollows = (p+q < s.length && s[p+q] == NUCLEOTIDE_G);

      int origCode = code(s, p);
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
      qGram(qcode, tmpQGram); // get the q-gram into tmpQGram (which is pre-allocated)

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

   /**
    * Returns an array of all bisulfite qcodes of the q-gram at t[start..start+q-1]
    * @param t
    * @param reverse
    * @return
    */
   public int[] bisulfiteQCodes(byte[] t, int start, final boolean reverse) {
      int qcode = code(t, start);
      boolean specialBorder = (reverse && start > 0 && t[start-1] == NUCLEOTIDE_G) ||
            (!reverse && start+q < t.length && t[start+q] == NUCLEOTIDE_C);
      return bisulfiteQCodes(qcode, reverse, specialBorder);
   }
   
   /**
    * Returns an array of all bisulfite qcodes of the q-gram at t[start..start+q-1]
    * @param t
    * @param reverse
    * @return
    */
   public int[] bisulfiteQCodes(HugeByteArray t, long start, final boolean reverse) {
      int qcode = code(t, start);
      boolean specialBorder = (reverse && start > 0 && t.get(start-1) == NUCLEOTIDE_G) ||
            (!reverse && start+q < t.length && t.get(start+q) == NUCLEOTIDE_C);
      return bisulfiteQCodes(qcode, reverse, specialBorder);
   }

   /** @return the cache fill ratio (between 0 and 1) */
   public double getCacheFill() {
      int c = 0;
      for (int i = 0; i < cached.length; ++i) {
         if (cached[i]) c++;
      }
      return (double)c/cached.length;
   }

   /**
    * Produces a q-gram iterator over a byte source.
    * 
    * @param t
    *           the text
    * @return an iterator that iterates over valid q-grams in t, not over invald q-grams or
    *         separators.
    */
   @Override
   protected Iterator<PositionQCodePair> sparseQGramIterator(final ByteBuffer t) {
      return new BisulfiteSparseQGramIterator(t, super.sparseQGramIterator(t));
   }

   /**
    * Produces a q-gram iterator over a byte source.
    * 
    * @param t
    *           the text
    * @return an iterator that iterates over valid q-grams in t, not over invald q-grams or
    *         separators.
    */
   @Override
   protected Iterator<PositionQCodePair> sparseQGramIterator(final byte[] t) {
      return new BisulfiteSparseQGramIterator(t, super.sparseQGramIterator(t));
   }

   @Override
   protected Iterator<PositionQCodePair> sparseQGramIterator(Sequence t) {
      return new BisulfiteSparseQGramIterator(t, super.sparseQGramIterator(t));
   }

   /**
    * Produces a q-gram iterator over a byte source.
    * 
    * @param t
    *           the text
    * @param separator
    *           specifies the code of the separator in t.
    * @return an iterator that iterates over valid q-grams in t, and over separators.
    */
   @Override
   protected Iterator<PositionQCodePair> sparseQGramIterator(final byte[] t, final byte separator) {
      return new BisulfiteSparseQGramIterator(t, super.sparseQGramIterator(t, separator));
   }

   /**
    * Produces a q-gram iterator over a byte source.
    * 
    * @param t
    *           the text
    * @param separator
    *           specifies the code of the separator in t.
    * @return an iterator that iterates over valid q-grams in t, and over separators.
    */
   @Override
   protected Iterator<PositionQCodePair> sparseQGramIterator(final ByteBuffer t, final byte separator) {
      return new BisulfiteSparseQGramIterator(t, super.sparseQGramIterator(t, separator));
   }
   
   @Override
   protected Iterator<PositionQCodePair> sparseQGramIterator(Sequence t, byte separator) {
      return new BisulfiteSparseQGramIterator(t, super.sparseQGramIterator(t, separator));
   }

   /**
    * sparse iterator class for both standard and bisulfite q-grams
    * (if bisulfite == false in the enclosing instance,
    * the iterator is deferred to the QGramCoder class.)
    * 
    */
   class BisulfiteSparseQGramIterator implements Iterator<PositionQCodePair> {
      private final Iterator<PositionQCodePair> it; // iterator of the underlying QGramCoder
      private int bisFwdRemaining = 0; // number of remaining forward bisulfite codes
      private int bisRevRemaining = 0; // number of ramaining reverse bisulfite codes
      private int[] bisFwdCodes = null; // remaining forward bisulfite codes
      private int[] bisRevCodes = null; // remaining reverse bisulfite codes
      private int pos = -1;

      /** sequence over which to iterate */
      private final Object t;
      private final int tLength;

      BisulfiteSparseQGramIterator(final ByteBuffer t, Iterator<PositionQCodePair> it) {
         this.t = t;
         tLength = t.limit();
         this.it = it;
      }

      BisulfiteSparseQGramIterator(final byte[] t, Iterator<PositionQCodePair> it) {
         this.t = t;
         tLength = t.length;
         this.it = it;
      }

      public BisulfiteSparseQGramIterator(Sequence t, Iterator<PositionQCodePair> it) {
         this(t.array(), it);
      }

      public byte charAt(final int p) {
         if (p < 0 || p >= tLength)
            return ((byte) -1);
         return ((t instanceof byte[]) ? ((byte[]) t)[p] : ((ByteBuffer) t).get(p));
      }

      @Override
      public boolean hasNext() {
         return bisFwdRemaining > 0 || bisRevRemaining > 0 || it.hasNext();
      }

      @Override
      public PositionQCodePair next() {
         if (bisFwdRemaining > 0) {
            final PositionQCodePair pc = new PositionQCodePair(pos, bisFwdCodes[--bisFwdRemaining] );
            return pc;
         }
         if (bisRevRemaining > 0) {
            final PositionQCodePair pc = new PositionQCodePair(pos, bisRevCodes[--bisRevRemaining] );
            return pc;
         }
         // get standard q-code first (no bisulfite replacement)
         final PositionQCodePair pc = it.next();
         pos = pc.position;
         final int qcode = pc.qcode;
            
         // get forward bisulfite q-codes. If original q-code is invalid, get empty list.
         bisFwdCodes = bisulfiteQCodes(qcode, false,
               charAt(pos + q) == BisulfiteQGramCoder.NUCLEOTIDE_G);
         bisFwdRemaining = bisFwdCodes.length;
         // get reverse bisulfite q-codes. If original q-code is invalid, get empty list.
         bisRevCodes = bisulfiteQCodes(qcode, true,
               charAt(pos - 1) == BisulfiteQGramCoder.NUCLEOTIDE_C);
         bisRevRemaining = bisRevCodes.length;
         // System.out.printf("   [pos=%d, fwd=%d, rev=%d]%n", pos, bisFwdRemaining, bisRevRemaining);

         return pc;
      }

      @Override
      public void remove() {
         throw new UnsupportedOperationException("remove not supported.");
      }
   }

}