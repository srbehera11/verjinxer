/*
 * QGramCoder.java Created on December 17, 2006, 3:43 PM
 */

package verjinxer.sequenceanalysis;

import java.nio.ByteBuffer;
import java.util.Iterator;

import verjinxer.util.HugeByteArray;
import verjinxer.util.PositionQCodePair;

/**
 * This class contains routines for coding byte sequences of a given length q (so-called q-grams)
 * over a given alphabet {0,1,...,asize-1} of size 'asize' as integers (base-asize numbers). The
 * byte source can be either an array (byte[]) or a buffer (ByteBuffer).
 * 
 * This class also provides iterables that generate all q-grams of a given byte source.
 * 
 * This class provides no filter functionality. Filtering is achieved by using a QGramFilter.
 * 
 * @author Sven Rahmann
 */
public class QGramCoder {

   /** the q-gram length */
   public final int q;

   /** alphabet size; the alphabet is {0,1, ..., asize-1} */
   public final int asize;

   /** the number of different q-grams. equals asize^q */
   public final int numberOfQGrams;

   /** equals asize^(q-1): q-1 q-2 ... 1 0 */
   private final int mod;

   /**
    * Creates a new instance of QGramCoder
    * 
    * @param q
    *           length of the q-grams coded by this instance
    * @param asize
    *           alphabet size of the q-grams coded by this instance
    */
   public QGramCoder(final int q, final int asize) {
      if (q < 0 || asize <= 0)
         throw new IllegalArgumentException("Need q>0; is " + q + ". Need asize>=0; is " + asize
               + ".");
      this.q = q;
      this.asize = asize;
      long power = 1;
      for (int i = 1; i <= q - 1; i++) {
         power *= asize;
         if (power < 0 || power > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Value asize^q exceeds maximum integer.");
      }
      if (power * asize < 0 || power * asize > Integer.MAX_VALUE)
         throw new IllegalArgumentException("Value asize^q exceeds maximum integer.");
      mod = (int) power; // asize^(q-1)
      numberOfQGrams = mod * asize;
   }

   public int getNumberOfQGrams() {
      return numberOfQGrams;
   }

   /**
    * computes the q-gram code (at position zero) of a byte array.
    * 
    * @param qgram
    *           a byte array with the numbers to be interpreted as base-asize number
    * @return the qgram code &gt;=0 if there is a valid q-gram; but if an illegal character appears
    *         at position i, return -i-1 &lt; 0.
    */
   public int code(final byte[] qgram) {
      return code(qgram, 0);
   }

   /**
    * computes the q-gram code at a given position of a byte array.
    * 
    * @param qgram
    *           a byte array for which to compute a q-gram code
    * @param offset
    *           position in the array at which to compute the q-gram code
    * @return the qgram code &gt;=0 if there is a valid q-gram; but if an illegal character appears
    *         at position i, return -i-1 &lt; 0.
    */
   public int code(final byte[] qgram, final int offset) {
      int c = 0;
      for (int i = offset; i < q + offset; i++) {
         final int qi = qgram[i];
         if (qi < 0 || qi >= asize)
            return (-1 - (i - offset));
         c %= mod;
         c *= asize;
         c += qi;
      }
      return c;
   }

   /**
    * compute the q-gram code at the given offset in a ByteBuffer.
    * 
    * @param qgram
    *           a ByteBuffer for which to compute a q-gram code
    * @param offset
    *           position in the buffer at which to compute the q-gram code
    * @return the qgram code &gt;=0 if there is a valid q-gram; but if an illegal character appears
    *         at position i, return -i-1 &lt; 0.
    */
   public int code(final ByteBuffer qgram, final int offset) {
      int c = 0;
      for (int i = offset; i < offset + q; i++) {
         final int qi = qgram.get(i);
         if (qi < 0 || qi >= asize)
            return (-1 - (i - offset));
         c %= mod;
         c *= asize;
         c += qi;
      }
      return c;
   }
   
   public int code(HugeByteArray qgram, long offset) {
	      int c = 0;
	      for (long i = offset; i < offset + q; i++) {
	         final int qi = qgram.get(i);
	         if (qi < 0 || qi >= asize)
	            return (-1 - (int)(i - offset));
	         c %= mod;
	         c *= asize;
	         c += qi;
	      }
	      return c;
	}

   /**
    * update the current code by shifting out the leftmost (most significant) character and
    * introducing the next one at the right (least significant).
    * 
    * @param old
    *           the old q-gram code. If 'old' is not a valid q-gram code (eg, -1), the behavior is
    *           unspecified!
    * @param next
    *           the next byte to shift in
    * @return the new q-gram code; or a negative value (-q) if 'next' is not in the alphabet
    */
   public int codeUpdate(final int old, final byte next) {
      assert old >= 0 && old < numberOfQGrams;
      if (next < 0 || next >= asize)
         return -q;
      return (old % mod) * asize + next;
   }

   /**
    * return the given q-gram code as q-gram in byte[]-form
    * 
    * @param qcode
    *           the q-gram code
    * @param qgram
    *           an existing byte[] of length>=q to store the q-gram; if null or too small, a new
    *           byte[] is created.
    * @return the existing or new byte[] containing the q-gram
    */
   public byte[] qGram(int qcode, byte[] qgram) {
      if (qgram == null || qgram.length < q)
         qgram = new byte[q];
      for (int p = q - 1; p >= 0; p--) {
         qgram[p] = (byte) (qcode % asize);
         qcode /= asize;
      }
      return qgram;
   }

   /**
    * return the given q-gram code as q-gram in byte[]-form
    * 
    * @param qcode
    *           the q-gram code
    * @return a new byte[] containing the q-gram
    */
   public byte[] qGram(int qcode) {
      return qGram(qcode, null);
   }

   /**
    * return the given q-gram code as a String via alphabet map translation
    * 
    * @param qcode
    *           the q-gram code
    * @param alphabet
    *           the alphabet map for translation
    * @return a new string of length q containing the q-gram text; null if invalid symbols are
    *         encountered
    */
   public String qGramString(final int qcode, final Alphabet alphabet) {
      if (qcode < 0 || qcode >= numberOfQGrams)
         return null;
      try {
         byte[] qgram = qGram(qcode);
         StringBuilder s = new StringBuilder(q);
         for (int i = 0; i < q; i++)
            s.append(alphabet.preimage(qgram[i]));
         return s.toString();
      } catch (InvalidSymbolException ex) {
      }
      return null;
   }

   /**
    * check wheter a given q-gram equals another given q-gram (q is a fixed parameter of this
    * QGramCoder).
    * 
    * @param qgram
    *           given q-gram
    * @param i
    *           starting position within qgram
    * @param s
    *           another given q-gram
    * @param p
    *           starting position within s
    * @return true iff qgram[i..i+q-1] equals s[p..p+q-1].
    */
   public boolean areCompatible(final byte[] qgram, final int i, final byte[] s, final int p) {
      for (int j = 0; j < q; j++)
         if (qgram[i + j] != s[p + j])
            return false;
      return true;
   }

   /**
    * Produces an object that iterates over all q-grams in 't'.
    * 
    * @param t
    *           the text
    * @return an iterable object that returns the q-grams in t in consecutive order, one for each
    *         position p in 0 &lt;= p &lt; length(t)-q+1. For each invalid q-gram (non-symbol
    *         containing q-gram), a negative value is produced. Otherwise the q-gram codes range in
    *         0 .. asize^q - 1.
    */
   public Iterable<Integer> qGrams(final byte[] t) {
      return new Iterable<Integer>() {
         public Iterator<Integer> iterator() {
            return new SimpleQGramIterator(t);
         }
      };
   }

   /**
    * Produces an object that iterates over all q-grams in 't'.
    * 
    * @param t
    *           the text
    * @return an iterable object that returns the q-grams in t in consecutive order, one for each
    *         position p in 0 &lt;= p &lt; length(t)-q+1. For each invalid q-gram (non-symbol
    *         containing q-gram), a negative value is produced. Otherwise the q-gram codes range in
    *         0 .. asize^q - 1.
    */
   public Iterable<Integer> qGrams(final ByteBuffer t) {
      return new Iterable<Integer>() {
         public Iterator<Integer> iterator() {
            return new SimpleQGramIterator(t);
         }
      };
   }

   /**
    * Produces an iterator that iterates over all q-grams in 't'. In contrast to
    * <code>qGrams</code>, only the valid q-grams are returned as a pair
    * pc=(pos,code), where pos is the starting position of the q-gram in t, and code is the q-gram
    * code. The pair is encoded in a <code>long</code> with pos in the high integer and code in the
    * low integer, such that pos = (int)(pc &gt;&gt; 32), and code = pc & 0xffffffff.
    * 
    * @param t
    * @return the iterable object
    */
   //TODO no longer a long-encoded pair; correct doc
   public Iterable<PositionQCodePair> sparseQGrams(final ByteBuffer t) {
      return new Iterable<PositionQCodePair>() {
         public Iterator<PositionQCodePair> iterator() {
            return sparseQGramIterator(t);
         }
      };
   }
   
   /**
    * @see sparseQGrams(ByteBuffer)
    * 
    * @param t
    * @return the iterable object
    */
   public Iterable<PositionQCodePair> sparseQGrams(final Sequences t) {
      return new Iterable<PositionQCodePair>() {
         public Iterator<PositionQCodePair> iterator() {
            return sparseQGramIterator(t);
         }
      };
   }

   /**
    * @see sparseQGrams(ByteBuffer)
    * 
    * @param t
    * @return the iterable object
    */
   public Iterable<PositionQCodePair> sparseQGrams(final byte[] t) {
      return new Iterable<PositionQCodePair>() {
         public Iterator<PositionQCodePair> iterator() {
            return sparseQGramIterator(t);
         }
      };
   }

   /**
    * Produces an object that iterates over all q-grams in 't', taking into account q-grams and
    * also separators.
    * 
    * @param t
    *           the text
    * @param separator
    *           the separator code
    * @return an iterable object that returns the q-grams in t, ordered by increasing position.
    *         q-grams are returned as a pair pc=(pos,code), where pos is the starting
    *         position of the q-gram in t. The pair is encoded in a <code>long</code> with pos in
    *         the high integer and code in the low integer, such that pos = (int)(pc &gt;&gt; 32),
    *         and code = (int)(pc).
    */
   //TODO no longer a long-encoded pair; correct doc
   public Iterable<PositionQCodePair> sparseQGrams(final ByteBuffer t, final byte separator) {
      return new Iterable<PositionQCodePair>() {
         public Iterator<PositionQCodePair> iterator() {
            return sparseQGramIterator(t, separator);
         }
      };
   }
   
   /** @see sparseQGrams(ByteBuffer,byte) */
   public Iterable<PositionQCodePair> sparseQGrams(final Sequences t, final byte separator) {
      return new Iterable<PositionQCodePair>() {
         public Iterator<PositionQCodePair> iterator() {
            return sparseQGramIterator(t, separator);
         }
      };
   }

   /** @see sparseQGrams(ByteBuffer,byte) */
   public Iterable<PositionQCodePair> sparseQGrams(final byte[] t, final byte separator) {
      return new Iterable<PositionQCodePair>() {
         public Iterator<PositionQCodePair> iterator() {
            return sparseQGramIterator(t, separator);
         }
      };
   }

   /**
    * Convenience method. Returns sparseQGrams(t,separator) if showSeparators is true
    * and sparseQGrams(t) otherwise. 
    * 
    * @param showSeparators
    *       whether to show separators      
    * @param t
    * @param separator
    *         the separator character. only used when showSeparators is true
    * @return the iterable object
    */
   public Iterable<PositionQCodePair> sparseQGrams(final ByteBuffer t, final boolean showSeparators, final byte separator) {
      if (showSeparators) {
         return sparseQGrams(t, separator);
      } else {
         return sparseQGrams(t);
      }
   }
   
   /** @see sparseQGrams(ByteBuffer,boolean,separator) */
   public Iterable<PositionQCodePair> sparseQGrams(final Sequences t, final boolean showSeparators, final byte separator) {
	  if (showSeparators) {
	     return sparseQGrams(t, separator);
	  } else {
	     return sparseQGrams(t);
	  }
	}

   /** @see sparseQGrams(ByteBuffer,boolean,separator) */
   public Iterable<PositionQCodePair> sparseQGrams(final byte[] t, final boolean showSeparators, final byte separator) {
      if (showSeparators) {
         return sparseQGrams(t, separator);
      } else {
         return sparseQGrams(t);
      }
   }

   /**
    * Produces a q-gram iterator over a byte source.
    * 
    * @param t
    *           the text
    * @return an iterator that iterates over valid q-grams in t, not over invald q-grams or
    *         separators.
    */
   protected Iterator<PositionQCodePair> sparseQGramIterator(final ByteBuffer t) {
      return new SparseQGramIterator(t);
   }
   
   /** @see sparseQGramIterator(ByteBuffer) */
   protected Iterator<PositionQCodePair> sparseQGramIterator(final Sequences t) {
	  return new SparseQGramIterator(t);
   }

   /**
    * Produces a q-gram iterator over a byte source.
    * 
    * @param t
    *           the text
    * @return an iterator that iterates over valid q-grams in t, not over invald q-grams or
    *         separators.
    */
   protected Iterator<PositionQCodePair> sparseQGramIterator(final byte[] t) {
      return new SparseQGramIterator(t);
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
   protected Iterator<PositionQCodePair> sparseQGramIterator(final byte[] t, final byte separator) {
      return new SparseQGramSepIterator(t, separator);
   }
   
   /** @see sparseQGramIterator(ByteBuffer,byte) */
   protected Iterator<PositionQCodePair> sparseQGramIterator(final Sequences t, final byte separator) {
	  return new SparseQGramSepIterator(t, separator);
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
   protected Iterator<PositionQCodePair> sparseQGramIterator(final ByteBuffer t, final byte separator) {
      return new SparseQGramSepIterator(t, separator);
   }

   /** simple iterator class, stops at every position and returns the code */
   protected class SimpleQGramIterator implements Iterator<Integer> {
      /** text as array */
      private final byte[] t;

      /** text as buffer */
      private final ByteBuffer b;

      /** current position in array or buffer */
      private int pos;

      /** length of t or b */
      private final int end;

      /** is there a next q-gram? */
      private boolean hasnext = true;

      /** next code to be returned; initially indicate new computation */
      private int nextc = -1;

      public SimpleQGramIterator(final byte[] t) {
         this.t = t;
         this.b = null;
         pos = 0;
         end = t.length - q + 1;
         findNextT();
      }

      public SimpleQGramIterator(final ByteBuffer b) {
         if (b.hasArray() && !b.isReadOnly()) {
            // if possible, use the backing array, it's more efficient
            this.t = b.array();
            this.b = null;
            pos = b.arrayOffset();
            end = t.length - q + 1;
            findNextT();
         } else {
            this.t = null;
            this.b = b;
            pos = 0;
            end = b.capacity() - q + 1;
            findNextB();
         }
      }

      @Override
      public boolean hasNext() {
         return hasnext;
      }

      @Override
      public Integer next() {
         assert nextc >= -q && nextc <= numberOfQGrams;
         assert pos >= 0 && pos < end;
         final int cod = nextc;
         pos++;
         if (b == null)
            findNextT();
         else
            findNextB();
         return cod;
      }

      private void findNextT() {
         if (pos >= end) {
            hasnext = false;
            nextc = -1;
            return;
         }
         if (nextc >= 0) {
            nextc = codeUpdate(nextc, t[pos + q - 1]); // this is >= 0 or == -q
         } else { // nextc<0
            nextc++;
            if (nextc == 0)
               nextc = code(t, pos);
         }
      }

      private void findNextB() {
         if (pos >= end) {
            hasnext = false;
            nextc = -1;
            return;
         }
         if (nextc >= 0) {
            nextc = codeUpdate(nextc, b.get(pos + q - 1)); // this is >= 0 or == -q
         } else { // nextc<0
            nextc++;
            if (nextc == 0)
               nextc = code(b, pos);
         }
      }

      /**
       * this iterator does not remove things, but remove() must be implemented; so we throw an
       * exception.
       */
      @Override
      public void remove() {
         throw new UnsupportedOperationException();
      }
   }

   /** sparse iterator class */
   protected class SparseQGramIterator implements Iterator<PositionQCodePair> {
      private final byte[] t; // text as array
      private final ByteBuffer b; // text as buffer
      private final int end; // 1 + starting position of last q-gram in t or b
      private int pos; // current position in array or buffer
      private int nextc = -1; // next valid code
      private boolean hasnext = true; // result of next hasnext() call

      /** constructs iterator from byte array */
      public SparseQGramIterator(final byte[] t) {
         this.t = t;
         this.b = null;
         pos = 0;
         end = t.length - q + 1;
         findNextT();
      }
      
      /** constructs iterator from Sequence */
      public SparseQGramIterator(final Sequences s) {
         this.t = s.array();
         this.b = null;
         pos = 0;
         end = t.length - q + 1;
         findNextT();
      }

      /** constructs iterator from byte buffer */
      public SparseQGramIterator(final ByteBuffer b) {
         if (b.hasArray() && !b.isReadOnly()) {
            // if possible, use the backing array, because it's more efficient
            this.t = b.array();
            this.b = null;
            pos = b.arrayOffset();
            end = t.length - q + 1;
            findNextT();
         } else {
            this.t = null;
            this.b = b;
            pos = 0;
            end = b.capacity() - q + 1;
            findNextB();
         }
      }

      @Override
      public final boolean hasNext() {
         return hasnext;
      }

      @Override
      public final PositionQCodePair next() {
         assert nextc >= 0 && nextc < numberOfQGrams;
         assert pos >= 0 && pos < end;
         final PositionQCodePair pc = new PositionQCodePair( pos++, nextc); // pos is incremented here!
         if (b == null)
            findNextT();
         else
            findNextB();
         return pc;
      }

      private void findNextT() { // current pos is first position to look at
         while (true) {
            // System.out.printf("in findNextT: pos=%d, nextc=%d, hasnext=%s%n", pos, nextc,
            // hasnext); // DEBUG
            if (pos >= end) {
               hasnext = false;
               nextc = -1;
               return;
            }
            if (nextc >= 0) {
               nextc = codeUpdate(nextc, t[pos + q - 1]);
               if (nextc >= 0)
                  return;
               pos += q; // the new character is invalid!
            } else { // nextc<0
               nextc = code(t, pos);
               if (nextc >= 0)
                  return;
               pos -= nextc; // now nextc == -(position of first invalid character), counting
               // starting at 1.
            }
         }
      }

      private void findNextB() { // current pos is first position to look at
         while (true) {
            if (pos >= end) {
               hasnext = false;
               nextc = -1;
               return;
            }
            if (nextc >= 0) {
               nextc = codeUpdate(nextc, b.get(pos + q - 1));
               if (nextc >= 0)
                  return;
               pos += q; // the new character is invalid!
            } else { // nextc<0
               nextc = code(b, pos);
               if (nextc >= 0)
                  return;
               pos -= nextc; // skip over invalid character
            }
         }
      }

      @Override
      public final void remove() {
         throw new UnsupportedOperationException();
      }
   }

   /** iterator class with separators */
   protected class SparseQGramSepIterator implements Iterator<PositionQCodePair> {
      private final byte[] t; // text as array
      private final ByteBuffer b; // text as buffer
      private final int end; // 1 + starting position of last q-gram in t or b
      private final int separator; // separator code
      private int pos; // current position in array or buffer
      private int nextc = -1; // next valid code
      private boolean hasnext = true; // result of next hasnext() call

      /** constructs iterator from byte array */
      public SparseQGramSepIterator(final byte[] t, final int sep) {
         this.t = t;
         this.b = null;
         pos = 0;
         end = t.length - q + 1;
         separator = sep;
         findNextT();
      }
      
      /** constructs iterator from Sequence */
      public SparseQGramSepIterator(final Sequences s, final int sep) {
         this.t = s.array();
         this.b = null;
         pos = 0;
         end = t.length - q + 1;
         separator = sep;
         findNextT();
      }

      /** constructs iterator from byte buffer */
      public SparseQGramSepIterator(final ByteBuffer b, final int sep) {
         separator = sep;
         if (b.hasArray() && !b.isReadOnly()) {
            // if possible, use the backing array, because it's more efficient
            this.t = b.array();
            this.b = null;
            pos = b.arrayOffset();
            end = t.length - q + 1;
            findNextT();
         } else {
            this.t = null;
            this.b = b;
            pos = 0;
            end = b.capacity() - q + 1;
            findNextB();
         }
      }

      @Override
      public final boolean hasNext() {
         return hasnext;
      }

      @Override
      public final PositionQCodePair next() {
         assert nextc >= -1 && nextc < numberOfQGrams; // -1 since separator may be used
         assert pos >= 0 && pos < end;
         final PositionQCodePair pc = new PositionQCodePair(pos++, nextc); // pos is incremented here!
         if (b == null)
            findNextT();
         else
            findNextB();
         return pc;
      }

      private void findNextT() { // current pos is first position to look at
         while (true) {
            // System.out.printf("in findNextT: pos=%d, nextc=%d, hasnext=%s%n", pos, nextc,
            // hasnext); // DEBUG
            if (pos >= end) {
               hasnext = false;
               nextc = -1;
               return;
            }
            if (nextc >= 0) {
               nextc = codeUpdate(nextc, t[pos + q - 1]);
               if (nextc >= 0)
                  return;
               pos += q - 1; // the new character is invalid, but may be a separator!
            } else { // nextc<0, may be looking at a separator!
               nextc = code(t, pos);
               if (nextc >= 0)
                  return;
               pos -= nextc + 1; // skip up to invalid character
               if (t[pos] == separator)
                  return;
               pos++;
            }
         }
      }

      private void findNextB() { // current pos is first position to look at
         while (true) {
            // System.out.printf("in findNextT: pos=%d, nextc=%d, hasnext=%s%n", pos, nextc,
            // hasnext);
            if (pos >= end) {
               hasnext = false;
               nextc = -1;
               return;
            }
            if (nextc >= 0) {
               nextc = codeUpdate(nextc, b.get(pos + q - 1));
               if (nextc >= 0)
                  return;
               pos += q - 1; // the new character is invalid, but may be a separator!
            } else { // nextc<0, may be looking at a separator!
               nextc = code(b, pos);
               if (nextc >= 0)
                  return;
               pos -= nextc + 1; // skip up to invalid character
               if (b.get(pos) == separator)
                  return;
               pos++;
            }
         }
      }

      @Override
      public final void remove() {
         throw new UnsupportedOperationException();
      }
   }


}
