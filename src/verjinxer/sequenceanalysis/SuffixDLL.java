/**
 * 
 */
package verjinxer.sequenceanalysis;

import java.util.Arrays;

/**
 * Basic implementation of a suffix double linked list.
 * 
 * @author Markus Kemmerling
 */
public class SuffixDLL implements ISuffixDLL {
   // encapsulated suffix array
   /**
    * Text/Sequence positions where the lexicographical first suffix that starts with a specific
    * character can be found.
    */
   private int[] lexfirstpos = new int[256];
   
   /**
    * Text/Sequence positions where the lexicographical last suffix that starts with a specific
    * character can be found.
    */
   private int[] lexlastpos = new int[256];
   
   /**
    * Text/Sequence positions where the lexicographical previous suffix can be found.
    */
   private int[] lexprevpos = null;
   
   /**
    * Text/Sequence positions where the lexicographical next suffix can be found.
    */
   private int[] lexnextpos = null;
   
   /** Capacity of this list */
   private final int capacity;
   
   /** The text/sequence associated with this suffix list */
   private final Sequences sequence;
   
   /** Alphabet of the associated text/sequence. */
   private final Alphabet alphabet;

   // internal state
   private int currentPosition = -1;

   /**
    * Constructor, initializes suffix list of given sequence.
    * 
    * @param sequence
    *           The associated sequence (text).
    * @param alphabet
    *           The alphabet of the sequence.
    */
   public SuffixDLL(Sequences sequence, Alphabet alphabet) {
      this.sequence = sequence;
      this.alphabet = alphabet;
      assert sequence.length() < Integer.MAX_VALUE;
      capacity = (int)sequence.length(); //normal sequences warp an array, so its length is int and not long
      lexprevpos = new int[capacity];
      lexnextpos = new int[capacity];
      Arrays.fill(lexfirstpos, -1);
      Arrays.fill(lexlastpos, -1);
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#insertbetween(int, int, int)
    */
   @Override
   public void insertbetween(int p1, int p2, int i) {
      // before: ... p1, p2 ...
      // after: ... p1, i, p2 ...
      assert (p1 == -1 || lexnextpos[p1] == p2);
      assert (p2 == -1 || lexprevpos[p2] == p1);
      lexprevpos[i] = p1;
      lexnextpos[i] = p2;
      if (p2 != -1) {
         lexprevpos[p2] = i;
      }
      if (p1 != -1) {
         lexnextpos[p1] = i;
      }
      currentPosition = i;
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#insertnew(int, int)
    */
   @Override
   public void insertnew(int chi, int i) {
      int cp, cs, ip, is;
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

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#insertasfirst(int, int)
    */
   @Override
   public void insertasfirst(int chi, int i) {
      final int p = lexfirstpos[chi];
      assert (p != -1);
      insertbetween(lexprevpos[p], p, i);
      lexfirstpos[chi] = i;
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#insertaslast(int, int)
    */
   @Override
   public void insertaslast(int chi, int i) {
      final int p = lexlastpos[chi];
      assert (p != -1);
      insertbetween(p, lexnextpos[p], i);
      lexlastpos[chi] = i;
   }

   // ////////////////////////////////////////////////////////////////////
   // ////////////////////getter for suffix array/////////////////////////
   // ////////////////////////////////////////////////////////////////////
   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#getFirstPos(int)
    */
   @Override
   public int getFirstPos(int chi) {
      return lexfirstpos[chi];
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#getLastPos(int)
    */
   @Override
   public int getLastPos(int chi) {
      return lexlastpos[chi];
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#getLowestCharacter()
    */
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
   public int getLexNextPos(int i) {
      return lexnextpos[i];
   }

   /**
    * @param i
    *           Text/Sequence position of a suffix.
    * @return Text/Sequence position where the lexicographical previous suffix, according to the
    *         suffix at position i, can be found.
    */
   public int getLexPreviousPos(int i) {
      return lexprevpos[i];
   }
   
   /**
    * @return An array, where for text/sequence position, the position of lexicographical previous
    *         suffix is stored.
    */
   public int[] getLexPreviousPosArray() {
      // needed for lcp calculating.
      // lexprevpos set in SuffixTrayBuilderSubcommand as buffer
      // and LCP writes the lpc values in this buffer (overwrite lexprevpos - is no more needed in
      // SuffixTrayBuilderSubcommand and so memory is reused) before writing them in correct order
      // to disc
      return lexprevpos;
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#length()
    */
   public int capacity() {
      return capacity;
   }

   // ////////////////////////////////////////////////////////////////////
   // ////////////////////////internal state//////////////////////////////
   // ////////////////////////////////////////////////////////////////////
   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#getCurrentPosition()
    */
   @Override
   public int getCurrentPosition() {
      return currentPosition;
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#getPredecessor()
    */
   @Override
   public int getPredecessor() {
      return lexprevpos[currentPosition];
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#getSuccessor()
    */
   @Override
   public int getSuccessor() {
      return lexnextpos[currentPosition];
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#resetToBegin()
    */
   @Override
   public void resetToBegin() {
      int chi = getLowestCharacter();
      if (chi >= 0 && chi < 256) {
         currentPosition = lexfirstpos[chi];
      } else {
         currentPosition = -1;
      }
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#hasNextUp()
    */
   @Override
   public boolean hasNextUp() {
      return getSuccessor() != -1;
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#nextUp()
    */
   @Override
   public void nextUp() {
      currentPosition = getSuccessor();
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#hasNextDown()
    */
   @Override
   public boolean hasNextDown() {
      return getPredecessor() != -1;
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#nextDown()
    */
   @Override
   public void nextDown() {
      currentPosition = getPredecessor();
   }
   
   // ////////////////////////////////////////////////////////////////////
   // /////////////////////associated sequence////////////////////////////
   // ////////////////////////////////////////////////////////////////////
   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#getSequence()
    */
   public Sequences getSequence() {
      return sequence;
   }
   
   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#getAlphabet()
    */
   public Alphabet getAlphabet() {
      return alphabet;
   }

}
