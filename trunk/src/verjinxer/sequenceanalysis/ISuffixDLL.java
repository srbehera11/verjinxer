package verjinxer.sequenceanalysis;

/**
 * Interface for a double linked list that contains the start positions of suffixes in a
 * text/sequence. This suffixes are ordered lexicographically within this list.<br>
 * <br>
 * To build a suffix list use {@link SuffixTrayBuilder}.<br>
 * To check the correctness and the lexicographical order use {@link SuffixTrayChecker}.<br>
 * To Write a suffix list to disk use {@link SuffixTrayWriter}.
 * 
 * @see SuffixTrayBuilder
 * @see SuffixTrayChecker
 * @see SuffixTrayWriter
 * 
 * @author Markus Kemmerling
 */
public interface ISuffixDLL {

   /**
    * Inserts i between p1 and p2 (they must be neighbors with p1<p2).
    * 
    *@param p1
    *           Text/Sequence position after which to insert.
    *@param p2
    *           Text/Sequence position before which to insert.
    *@param i
    *           What to insert (position in text/sequence).
    */
   public abstract void insertbetween(final int p1, final int p2, final int i);

   /**
    * Inserts the first overall occurrence of a new character chi.
    * 
    *@param chi
    *           Integer representation of character to insert.
    *@param i
    *           What to insert (position in text/sequence).
    */
   public abstract void insertnew(final int chi, final int i);

   /**
    * Inserts character chi, in text/sequence at position i, and remember it as first occurrence
    * 
    * @param chi
    *           Integer representation of character to insert.
    * @param i
    *           What to insert (position in text/sequence).
    */
   public abstract void insertasfirst(int chi, int i);

   /**
    * Inserts character chi, in text/sequence at position i, and remember it as last occurrence
    * 
    * @param chi
    *           Integer representation of character to insert.
    * @param i
    *           What to insert (position in text/sequence).
    */
   public abstract void insertaslast(int chi, int i);

   // ////////////////////////////////////////////////////////////////////
   // ////////////////////getter for suffix array/////////////////////////
   // ////////////////////////////////////////////////////////////////////
   /**
    * @param chi
    *           Integer representation of character to insert.
    * @return The first position of character i in the text/sequence.
    */
   public abstract int getFirstPos(int chi);

   /**
    * @param chi
    *           Integer representation of character to insert.
    * @return The last position of character i in the text/sequence.
    */
   public abstract int getLastPos(int chi);

   /**
    * @return Integer representation of the lexicographical lowest character in this list.
    */
   public abstract int getLowestCharacter();

   /**
    * @return Capacity of this list (that is the length of the associated text/sequence).
    */
   public abstract int capacity();

   // ////////////////////////////////////////////////////////////////////
   // ////////////////////////internal state//////////////////////////////
   // ////////////////////////////////////////////////////////////////////
   /**
    * This list has an internal state and points to a position in the associated text/sequence (and
    * so to a suffix).
    * 
    * @return The current text/sequence position where the internal state points to.
    */
   public abstract int getCurrentPosition();

   /**
    * This list has an internal state and points to a position in the associated text/sequence (and
    * so to a suffix).
    * 
    * @return Position in text/sequence where the lexicographical previous suffix exists.
    */
   public abstract int getPredecessor();

   /**
    * This list has an internal state and points to a position in the associated text/sequence (and
    * so to a suffix).
    * 
    * @return Position in text/sequence where the lexicographical next suffix exists.
    */
   public abstract int getSuccessor();

   /**
    * This list has an internal state and points to a position in the associated text/sequence (and
    * so to a suffix).
    * 
    * This method resets the internal state so that it points to the position of the lexicographical
    * first suffix int the text/sequence.
    */
   public abstract void resetToBegin();

   /**
    * This list has an internal state and points to a position in the associated text/sequence (and
    * so to a suffix).
    * 
    * @return Whether there is a lexicographical next suffix.
    */
   public abstract boolean hasNextUp();

   /**
    * This list has an internal state and points to a position in the associated text/sequence (and
    * so to a suffix).
    * 
    * This method sets the internal state to the lexicographical next suffix.
    */
   public abstract void nextUp();

   /**
    * This list has an internal state and points to a position in the associated text/sequence (and
    * so to a suffix).
    * 
    * @return Whether there is a lexicographical previous suffix.
    */
   public abstract boolean hasNextDown();

   /**
    * This list has an internal state and points to a position in the associated text/sequence (and
    * so to a suffix).
    * 
    * This method sets the internal state to the lexicographical previous suffix.
    */
   public abstract void nextDown();

   // ////////////////////////////////////////////////////////////////////
   // /////////////////////associated sequence////////////////////////////
   // ////////////////////////////////////////////////////////////////////
   /**
    * @return The text/sequence to that this suffix list is associated with.
    */
   public abstract Sequences getSequence();

   /**
    * @return The alphabet of the associated text/sequence.
    */
   public abstract Alphabet getAlphabet();

}