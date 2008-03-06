package verjinxer.sequenceanalysis;

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
   
   
   // ============================ q-gram iteration ===========================

   /**
    * Produces an object that iterates over all q-grams in 't',
    * exactly as the same method of QGramCoder.
    * This method completely ignores the bisulfite option.
    * In other words, even if this is a bisulfite-enabled MultiQGramCoder,
    * only the standard q-grams are returned, one per position.
    * 
    * @param t the text in form of a byte array or a ByteBuffer
    *  (other types of objects will raise a runtime exception)
    * @return an iterable object that returns the q-grams in t,
    *   one per position.
    */
  public Iterable<Integer> qGrams(final Object t) {
    return new Iterable<Integer>() {
      public Iterator<Integer> iterator() { return simpleQGramIterator(t); }
    };
  }
  
   /** an iterator over all q-grams in 't'
    * @param t   the text, either as a byte[], or as a ByteBuffer
    * @return    the iterator
    */
   public Iterator<Integer> simpleQGramIterator(final Object t) {
      return qcoder.simpleQGramIterator(t);
   }


  /**
    * Produces an object that iterates over all q-grams in 't',
    * taking into account q-grams as well as possibly separators, as specified.
    * 
    * If this MultiQGramCoder is instantiated without the bisulfite option,
    * it behaves exactly as the corresponding iterable object of the QGramCoder.
    * If the bisulfite option is specified, this iterable additionaly iterates
    * over all bisulfite-compatible q-grams (after each standard q-gram).
    * 
    * @param t the text in form of a byte array or a ByteBuffer
    *  (other types of objects will raise a runtime exception)
    * @param showSeparators also list separator positions (with a code of -1).
    * @param separator  the separator code
    * @return an iterable object that returns the q-grams in t,
    *  ordered by increasing position.
    *  q-grams are returned as a long-encoded pair pc=(pos,code),
    *  where pos is the starting position of the q-gram in t.
    *  The pair is encoded in a <code>long</code> with pos in the high integer
    *  and code in the low integer, such that
    *  pos =  (int)(pc &gt;&gt; 32), and code = (int)(pc).
    */
  public Iterable<Long> sparseQGrams(final Object t, final boolean showSeparators, final byte separator) {
    return new Iterable<Long>() {
      public Iterator<Long> iterator() { return sparseQGramIterator(t, showSeparators, separator); }
    };
  }
  
   /**
    * equvalent to sparseQGrams(t, true, separator)
    * @param t
    * @param separator
    * @return the iterable object
    */
  public Iterable<Long> sparseQGrams(final Object t, final byte separator) {
     return sparseQGrams(t, true, separator);
  }

   /**
    * equvalent to sparseQGrams(t, false, (any byte value))
    * @param t
    * @return the iterable object
    */
  public Iterable<Long> sparseQGrams(final Object t) {
     return sparseQGrams(t, false, (byte)0); // 0 could be any byte code
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
  public Iterator<Long> sparseQGramIterator(final Object t, boolean showSeparators, final byte separator) {
     return (bisulfite)?
        new SparseQGramIterator(t, showSeparators, separator)
        : (showSeparators)? 
           qcoder.sparseQGramIterator(t, separator)
           : qcoder.sparseQGramIterator(t);
  }
   
   // ------------------------------------------------------------------------------------
   // sparse iterator class for bisulfite!
   private class SparseQGramIterator implements Iterator<Long> {
      private final Iterator<Long> it; // iterator of the underlying QGramCoder
      private int bisRemaining = 0;    // number of remaining bisulfite codes
      private ArrayList<Integer> bisCodes = null;  // remaining bisulfite codes
      private long pos = -1;

      SparseQGramIterator(final Object t, final boolean stopAtSeparator, final int separator) {
         assert bisulfite;
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
         throw new UnsupportedOperationException("this does not work");
         /*
         if(bisRemaining>0) {
            final long pc = (pos<<32) + bisCodes.get(--bisRemaining);
            return pc;
         }
         final long pc = it.next();
         pos = pc>>>32;
         // TODO bisCodes = bicoder.compatibleQCodes((int)pc);
         bisRemaining = bisCodes.size();
         return pc;*/
      }

      public void remove() {
         throw new UnsupportedOperationException("remove not supported.");
      }
   } // end iterator class
   // --------------------------------------------------------------------------
   
   
   // =================== end iterator methods ==================================
   // ===========================================================================
   
   
   /**
    * Checks whether a given q-gram matches another given q-gram, 
    * possibly taking bisulfite treatment into account.
    * @param qgram given q-gram (bis.-treated)
    * @param i     starting position within qgram
    * @param s     another given q-gram (in text)
    * @param p     starting position within s
    * @return true iff qgram[i..i+q-1] equals s[p..p+q-1], possibly after bisulfite-treatment of s.
    */
   public boolean areCompatible(final byte[] qgram, final int i, final byte[] s, final int p) {
      if (qcoder.areCompatible(qgram, i, s, p)) return true;
      if (!bisulfite) return false;
      return bicoder.areCompatible(qgram, i, s, p);
   }
   
   public void update(byte next, byte after) {
      if (bisulfite) {
         assert 0 <= next && next < asize;
         bicoder.update(next, after);
      } else {
         qcode = qcoder.codeUpdate(qcode, next);
         assert qcode >= 0;
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
