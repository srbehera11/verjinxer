package verjinxer.sequenceanalysis;

import java.util.Arrays;

import verjinxer.util.HugeByteArray;
import verjinxer.util.HugeLongArray;

/**
 * Basic implementation of a huge suffix double linked list.
 * 
 * @author Markus Kemmerling
 */
public class BigSuffixDLL implements IBigSuffixDLL {
// encapsulated suffix array
   /**
    * Text/Sequence positions where the lexicographical first suffix that starts with a specific
    * character can be found.
    */
   long[] lexfirstpos= null;   // indexed by character
   
   /**
    * Text/Sequence positions where the lexicographical last suffix that starts with a specific
    * character can be found.
    */
   long[] lexlastpos = null;   // indexed by character
   
   /**
    * Text/Sequence positions where the lexicographical previous suffix can be found.
    */
   HugeLongArray lexprevpos = null;
   
   /**
    * Text/Sequence positions where the lexicographical next suffix can be found.
    */
   HugeLongArray lexnextpos = null;

   /** Capacity of this list */
   private final long capacity;
   
   /** The text/sequence associated with this suffix list */
   private final HugeByteArray sequence;
   
   /** Alphabet of the associated text/sequence. */
   private final Alphabet alphabet;
   
   // internal state
   private long currentPosition = -1;
   
   /**
    * Constructor, initializes suffix list of given sequence.
    * 
    * @param sequence
    *           The associated sequence (text).
    * @param alphabet
    *           The alphabet of the sequence.
    */
   public BigSuffixDLL(HugeByteArray sequence, Alphabet alphabet) {
      this.sequence = sequence;
      this.alphabet = alphabet;
      capacity = sequence.length; //normal sequences warp an array, so its length is int and not long
      lexprevpos = new HugeLongArray(capacity);
      lexnextpos = new HugeLongArray(capacity);
      Arrays.fill(lexfirstpos, -1);
      Arrays.fill(lexlastpos, -1);
   }

   @Override
   public void insertbetween(long p1, long p2, long i) {
   // before: ... p1, p2 ...
      // after: ... p1, i, p2 ...
      assert (p1 == -1 || lexnextpos.get(p1) == p2);
      assert (p2 == -1 || lexprevpos.get(p2) == p1);
      lexprevpos.set(i,p1);
      lexnextpos.set(i,p2);
      if (p2!=-1) {
         lexprevpos.set(p2,i);
      }
      if (p1!=-1) {
         lexnextpos.set(p1,i);
      }
      
      currentPosition = i;
   }

   @Override
   public void insertnew(int chi, long i) {
      int cp, cs;
      long ip, is;
      assert (lexfirstpos[chi] == -1);
      assert (lexlastpos[chi] == -1);
      lexfirstpos[chi] = i;
      lexlastpos[chi] = i;
      for (cp = chi - 1; cp >= 0 && lexlastpos[cp] == -1; cp--) {
      }
      ip = (cp >= 0 ? lexlastpos[cp] : -1);
      for (cs = chi + 1; cs < 256 && lexfirstpos[cs] == -1; cs++) {
      }
      is = (cs < 256 ? lexfirstpos[cs] : -1);
      // before: ... ip, is ...
      // after: ... ip, i, is ...
      insertbetween(ip, is, i);
   }

   @Override
   public void insertasfirst(int chi, long i) {
      final long p = lexfirstpos[chi];
      assert (p != -1);
      insertbetween(lexprevpos.get(p), p, i);
      lexfirstpos[chi] = i;
   }

   @Override
   public void insertaslast(int chi, long i) {
      final long p = lexlastpos[chi];
      assert(p!=-1);
      insertbetween(p,lexnextpos.get(p),i);
      lexlastpos[chi]=i;
   }

   // ////////////////////////////////////////////////////////////////////
   // ////////////////////getter for suffix array/////////////////////////
   // ////////////////////////////////////////////////////////////////////
   @Override
   public long getFirstPos(int chi) {
      return lexfirstpos[chi];
   }

   @Override
   public long getLastPos(int chi) {
      return lexlastpos[chi];
   }

   @Override
   public int getLowestCharacter() {
      int chi;
      for (chi = 0; chi < 256 && lexfirstpos[chi] == -1; chi++) {
      }
      return chi;
   }

   /**
    * @param i
    *           Text/Sequence position of a suffix.
    * @return Text/Sequence position where the lexicographical next suffix, according to the
    *         suffix at position i, can be found.
    */
   public long getLexNextPos(long i) {
      return lexnextpos.get(i);
   }

   /**
    * @param i
    *           Text/Sequence position of a suffix.
    * @return Text/Sequence position where the lexicographical previous suffix, according to the
    *         suffix at position i, can be found.
    */
   public long getLexPreviousPos(long i) {
      return lexprevpos.get(i);
   }

   /**
    * @return An array, where for text/sequence position, the position of lexicographical previous
    *         suffix is stored.
    */
   public HugeLongArray getLexPreviousPosArray() {
      // needed for lcp calculating.
      // lexprevpos set in SuffixTrayBuilderSubcommand as buffer
      // and BigLCP writes the lpc values in this buffer (overwrite lexprevpos - is no more needed in
      // SuffixTrayBuilderSubcommand and so memory is reused) before writing them in correct order
      // to disc
      return lexprevpos;
   }
   
   @Override
   public long capacity() {
      return capacity;
   }
   
   // ////////////////////////////////////////////////////////////////////
   // ////////////////////////internal state//////////////////////////////
   // ////////////////////////////////////////////////////////////////////
   @Override
   public long getCurrentPosition() {
      return currentPosition;
   }

   @Override
   public long getPredecessor() {
      return lexprevpos.get(currentPosition);
   }

   @Override
   public long getSuccessor() {
      return lexnextpos.get(currentPosition);
   }

   @Override
   public void resetToBegin() {
      int chi = getLowestCharacter();
      if (chi >= 0 && chi < 256) {
         currentPosition = lexfirstpos[chi];
      } else {
         currentPosition = -1;
      }
   }

   @Override
   public boolean hasNextDown() {
      return getPredecessor() != -1;
   }

   @Override
   public boolean hasNextUp() {
      return getSuccessor() != -1;
   }

   @Override
   public void nextDown() {
      currentPosition = getPredecessor();
   }

   @Override
   public void nextUp() {
      currentPosition = getSuccessor();
   }

   // ////////////////////////////////////////////////////////////////////
   // /////////////////////associated sequence////////////////////////////
   // ////////////////////////////////////////////////////////////////////
   @Override
   public Alphabet getAlphabet() {
      return alphabet;
   }

   @Override
   public HugeByteArray getSequence() {
      return sequence;
   }

   

}
