/**
 * 
 */
package verjinxer.sequenceanalysis;

import java.util.Arrays;

/**
 * @author Markus Kemmerling
 */
public class SuffixDLL implements ISuffixDLL {
   // encapsulated suffix array
   private int[] lexfirstpos = new int[256];
   private int[] lexlastpos = new int[256];
   private int[] lexprevpos = null;
   private int[] lexnextpos = null;

   public SuffixDLL(int n) {
      lexprevpos = new int[n];
      lexnextpos = new int[n];
      Arrays.fill(lexfirstpos, -1);
      Arrays.fill(lexlastpos, -1);
   }

   // internal state
   private int currentPosition = -1;

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

   public int getLexNextPos(int i) {
      return lexnextpos[i];
   }

   public int getLexPreviousPos(int i) {
      return lexprevpos[i];
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

}
