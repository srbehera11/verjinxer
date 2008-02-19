package verjinxer.sequenceanalysis;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * A MultiQGramCoder contains both a (simple) QGramCoder and a
 * BisulfiteQGramCoder.
 *
 * @author Marcel Martin
 * @author Sven Rahmann
 */
public class MultiQGramCoder {

   /** the underlying q-gram coder */
   public final QGramCoder qcoder;
   /** the underlying bisulfite coder */
   public final BisulfiteQGramCoder bicoder;
   /** the q-gram length */
   public final int q;
   /** the alphabet size */
   public final int asize;
   /** number of q-grams, equals asize^q */
   public final int numberOfQGrams;
   /** using bisulfite? */
   public final boolean bisulfite;
   private int qcode = 0;  // TODO: why 0, not -1 or other invalid?
   

   /**
    * Creata a new MultiQGramcoder for the given q-gram length and alphabet size.
    * If bisulfite is false, this simply wraps a standard QGramCoder.
    * @param q
    * @param asize
    * @param bisulfite
    */
   public MultiQGramCoder(final int q, final int asize, final boolean bisulfite) {
      if (bisulfite) {
         if (asize != 4) throw new IllegalArgumentException("If bisulfite is true, asize must be 4.");
         bicoder = new BisulfiteQGramCoder(q);
         qcoder = bicoder.coder;
      } else {
         bicoder = null;
         qcoder = new QGramCoder(q, asize);
      }
      this.bisulfite = bisulfite;
      this.asize = asize;
      this.q = q;
      this.numberOfQGrams = qcoder.numberOfQGrams;
   }

   /**
    * Produces an object that iterates over all q-grams in 't',
    * taking into account standard and bisulfite q-grams,
    * as well as sequence separators.
    * @param t the text in form of a byte array or a ByteBuffer
    *  (other types of objects will raise a runtime exception)
    * @param separator  the separator code
    * @return an iterable object that returns the q-grams in t,
    *  ordered by increasing position.
    *  In contrast to <code>SimpleQGramListOf(t)</code>, 
    *  only the valid q-grams (and possibly the separators) 
    *  are returned as a pair pc=(pos,code),
    *  where pos is the starting position of the q-gram in t,
    *  and code is the q-gram code. 
    *  The pair is encoded in a <code>long</code> with pos in the high integer
    *  and code in the low integer, such that
    *  pos =  pc &gt;&gt; 32, and code = pc & 0xffff.
    */
  public Iterable<Long> sparseQGrams(final Object t, final int separator) {
    return new Iterable<Long>() {
      public Iterator<Long> iterator() { return sparseQGramIterator(t, true, separator); }
    };
  }

    public Iterable<Long> sparseQGrams(final Object t) {
    return new Iterable<Long>() {
      public Iterator<Long> iterator() { return sparseQGramIterator(t, false, 999); }
    };
  }

  /**
   * Produces an object that iterates over all q-grams in 't'.
   *  Only the valid q-grams (standard and 
   *  possibly bisulfite versions), and possibly the separators,
   *  are returned as a pair pc=(pos,code),
   *  where pos is the starting position of the q-gram in t,
   *  and code is the q-gram code. 
   *  The pair is encoded in a <code>long</code> with pos in the high integer
   *  and code in the low integer, such that
   *  pos =  (int)(pc &gt;&gt; 32), and code = pc & 0xffffffff.
   * @param t the text in form of a byte array or a ByteBuffer
   *  (other types of objects will raise a runtime exception)
   * @param showSeparators  whether to include separators in the list
   *  of returned q-grams.
   * @param separator  only used if listSeparators is true;
   *   in that case, it specifies the code of the separator in t.
   * @return an iterable object that returns the q-grams in t,
   *  ordered by increasing position, as <code>long</code> integers,
   *  as described above.
   */
  public Iterable<Long> sparseQGrams(final Object t, final boolean showSeparators, final int separator) {
     if (showSeparators) return sparseQGrams(t,separator);
     else return sparseQGrams(t);
  }

  
  /** q-gram iterator over a byte source.
   * @param t the text in form of a byte array or a ByteBuffer
   *  (other types of objects will raise a runtime exception)
   * @param showSeparators  if true, also iterate over positions of separators;
   *   the corresponding q-gram code will be -1.
   * @param separator  only used if listSeparators is true;
   *   in that case, it specifies the code of the separator in t.
   * @return an iterator that iterates over valid q-grams in t,
   *   and possibly over separators.
   */
  public Iterator<Long> sparseQGramIterator(final Object t, boolean showSeparators, final int separator) {
     return (t instanceof byte[])?
        new SparseQGramIterator((byte[])t, showSeparators, separator) 
        : new SparseQGramIterator((ByteBuffer)t, showSeparators, separator);
  }
   
   
   // iterator class
   private class SparseQGramIterator implements Iterator<Long> {
      private final Iterator<Long> it; // iterator of the underlying QGramCoder
      private int bisRemaining = 0;    // number of remaining bisulfite codes
      private ArrayList<Integer> bisCodes = null;  // remaining bisulfite codes
      private long pos = -1;

      SparseQGramIterator(final Object t, final boolean stopAtSeparator, final int separator) {
         if (stopAtSeparator) 
            it = qcoder.sparseQGramIterator(t, separator);
         else
            it = qcoder.sparseQGramIterator(t);
      }
      
      public boolean hasNext() {
         if(bisRemaining>0) return true;
         return it.hasNext();
      }

      public Long next() {
         if(bisRemaining>0) {
            final long pc = (pos<<32) + bisCodes.get(--bisRemaining);
            return pc;
         }
         final long pc = it.next();
         if (!bisulfite) return pc;
         pos = pc>>32;
         bisCodes = bicoder.compatibleQCodes((int)(pc&0xffffffff));
         bisRemaining = bisCodes.size();
         return pc;
      }

      public void remove() {
         throw new UnsupportedOperationException("remove not supported.");
      }
   } // end iterator class

   
   /**
    * check wheter a given q-gram matches another given q-gram, 
    * possibly taking bisulfite treatment into account.
    * @param qgram given q-gram
    * @param i     starting position within qgram
    * @param s     another given q-gram
    * @param p     starting position within s
    * @return true iff qgram[i..i+q-1] equals s[p..p+q-1], possibly after bisulfite-treatment of s.
    */
   public boolean areCompatible(final byte[] qgram, final int i, final byte[] s, final int p) {
      if (qcoder.areCompatible(qgram, i, s, p)) return true;
      if (!bisulfite) return false;
      return (bicoder.areCompatible(qgram, i, s, p));
   }
   
   // ======================================================================================================

   public void update(byte next, byte after) {
      if (bisulfite) {
         assert (0 <= next && next < asize);
         bicoder.update(next, after);
      } else {
         qcode = qcoder.codeUpdate(qcode, next);
         assert (qcode != -1);
      }
   }

   public void reset() {
      if (bisulfite)
         bicoder.reset();
      else
         qcode = 0;
   }

   /**
    * 
    * @return 
    */
   public Collection<Integer> getCodes() {
      if (bisulfite)
         return bicoder.getCodes();
      else {
         Collection<Integer> r = new ArrayList<Integer>(1);
         r.add(qcode);
         return r;
      }
   }
}
