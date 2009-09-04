package verjinxer.sequenceanalysis;

import java.util.Arrays;

/**
 * @author Markus Kemmerling
 */
public class SuffixXorDLL implements ISuffixDLL {
   // encapsulated suffix array
   private int[] lexfirstpos = new int[256];
   private int[] lexlastpos = new int[256];
   private int[] lexps = null;
   
   private final int length;
   private final Sequences sequence;
   private final Alphabet alphabet;

   // internal state
   private int currentPosition = -1;
   private int predecessor = -1;
   private int successor = -1;

   /**
    * constructor, initializes list of given capacity.
    * 
    * @param nn
    *           capacity
    */
   public SuffixXorDLL(Sequences sequence, Alphabet alphabet) {
      this.sequence = sequence;
      this.alphabet = alphabet;
      assert sequence.length() < Integer.MAX_VALUE;
      length = (int)sequence.length(); //normal sequences warp an array, so its length is int and not long
      lexps = new int[length];
      Arrays.fill(lexfirstpos, -1);
      Arrays.fill(lexlastpos, -1);
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#insertbetween(int, int, int)
    */
   public final void insertbetween(final int p1, final int p2, final int i) {
      // before: ... p1, p2 ...
      // after: ... p1, i, p2 ...
      lexps[i] = p1 ^ p2;
      if (p2 != -1) {
         lexps[p2] ^= p1 ^ i;
      }
      if (p1 != -1) {
         lexps[p1] ^= p2 ^ i;
      }
      // update internal state
      predecessor = p1;
      successor = p2;
      currentPosition = i;
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#insertnew(int, int)
    */
   public final void insertnew(final int chi, final int i) {
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
      insertbetween(ip, is, i);
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#insertasfirst(int, int)
    */
   public final void insertasfirst(int chi, int i) {
      int cp, ip;
      assert (lexfirstpos[chi] != -1);
      assert (lexlastpos[chi] != -1);
      for (cp = chi - 1; cp >= 0 && lexlastpos[cp] == -1; cp--) {
      }
      ip = (cp >= 0 ? lexlastpos[cp] : -1);
      insertbetween(ip, lexfirstpos[chi], i);
      lexfirstpos[chi] = i;
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#insertaslast(int, int)
    */
   public final void insertaslast(int chi, int i) {
      int cs, is;
      assert (lexfirstpos[chi] != -1);
      assert (lexlastpos[chi] != -1);
      for (cs = chi + 1; cs < 256 && lexfirstpos[cs] == -1; cs++) {
      }
      is = (cs < 256 ? lexfirstpos[cs] : -1);
      insertbetween(lexlastpos[chi], is, i);
      lexlastpos[chi] = i;
   }

   // ////////////////////////////////////////////////////////////////////
   // ////////////////////getter for suffix array/////////////////////////
   // ////////////////////////////////////////////////////////////////////
   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#getFirstPos(int)
    */
   public int getFirstPos(int chi) {
      return lexfirstpos[chi];
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#getLastPos(int)
    */
   public int getLastPos(int chi) {
      return lexlastpos[chi];
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#getLowestCharacter()
    */
   public int getLowestCharacter() {
      int chi;
      for (chi = 0; chi < 256 && lexfirstpos[chi] == -1; chi++) {
      }
      return chi;
   }

   public int getLexPS(int p) {
      return lexps[p];
   }
   
   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#length()
    */
   public int length() {
      return length;
   }

   // ////////////////////////////////////////////////////////////////////
   // ////////////////////////internal state//////////////////////////////
   // ////////////////////////////////////////////////////////////////////
   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#getCurrentPosition()
    */
   public int getCurrentPosition() {
      return currentPosition;
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#getPredecessor()
    */
   public int getPredecessor() {
      return predecessor;
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#getSuccessor()
    */
   public int getSuccessor() {
      return successor;
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#resetToBegin()
    */
   public void resetToBegin() {
      int chi = getLowestCharacter();
      if (chi >= 0 && chi < 256) {
         currentPosition = lexfirstpos[chi];
         predecessor = -1;
         successor = lexps[currentPosition] ^ -1;
      } else {
         currentPosition = -1;
         predecessor = -1;
         successor = -1;
      }
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#hasNextUp()
    */
   public boolean hasNextUp() {
      return successor != -1;
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#nextUp()
    */
   public void nextUp() {
      predecessor = currentPosition;
      currentPosition = successor;
      successor = lexps[currentPosition] ^ predecessor;
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#hasNextDown()
    */
   public boolean hasNextDown() {
      return predecessor != -1;
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#nextDown()
    */
   public void nextDown() {
      successor = currentPosition;
      currentPosition = predecessor;
      predecessor = lexps[currentPosition] ^ successor;
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
   // ////////////////////////////////////////////////////////////////////
}
