package verjinxer.util;

/**
 * @author Markus Kemmerling
 */
public interface IWaveletTree {

   /**
    * Returns the character that exists at the given position within the origin text/sequence.
    * 
    * @param position
    * @return
    * @throws IndexOutOfBoundsException
    *            When the position is negative or greater than the length of the origin
    *            text/sequence.
    */
   public byte getCharacter(int position);

   /**
    * Returns the number of times the given character appears in determined prefix of the origin
    * text/sequence More specific, it counts the occurrence of the given character in
    * S[0,...,prefixLength-1], where S is the origin text/sequence and S[0,...,i] describes the
    * subsequence of S from position 0 (inclusive) to i (inclusive).
    * 
    * @param character
    *           Character for the the number of occurrence is calculated.
    * @param prefixLength
    *           Length of the prefix of the origin text within the character is counted. If the
    *           length is negative, 0 is returned.
    * @return Number of times the given character appears in the given text prefix.
    */
   public int rank(byte character, int prefixLength);
}
