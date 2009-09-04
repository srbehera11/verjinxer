package verjinxer.sequenceanalysis;

/**
 * @author Markus Kemmerling
 */
public interface ISuffixDLL {

   /**
    * insert i between p1 and p2 (they must be neighbors with p1<p2)
    * 
    * @param p1
    *           position after which to insert
    *@param p2
    *           position before which to insert
    *@param i
    *           what to insert
    */
   public abstract void insertbetween(final int p1, final int p2, final int i);

   /**
    * insert the first overall occurrence of a new character chi.
    * 
    * @param chi
    *           integer representation of character to insert
    *@param i
    *           what to insert
    */
   public abstract void insertnew(final int chi, final int i);

   public abstract void insertasfirst(int chi, int i);

   public abstract void insertaslast(int chi, int i);

   // ////////////////////////////////////////////////////////////////////
   // ////////////////////getter for suffix array/////////////////////////
   // ////////////////////////////////////////////////////////////////////
   public abstract int getFirstPos(int chi);

   public abstract int getLastPos(int chi);

   public abstract int getLowestCharacter();
   
   public abstract int length();

   // ////////////////////////////////////////////////////////////////////
   // ////////////////////////internal state//////////////////////////////
   // ////////////////////////////////////////////////////////////////////
   public abstract int getCurrentPosition();

   public abstract int getPredecessor();

   public abstract int getSuccessor();

   public abstract void resetToBegin();

   public abstract boolean hasNextUp();

   public abstract void nextUp();

   public abstract boolean hasNextDown();

   public abstract void nextDown();
   
   // ////////////////////////////////////////////////////////////////////
   // /////////////////////associated sequence////////////////////////////
   // ////////////////////////////////////////////////////////////////////
   
   public abstract Sequences getSequence();
   
   public abstract Alphabet getAlphabet();

}