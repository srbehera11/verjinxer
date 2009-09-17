package verjinxer.sequenceanalysis;

import java.util.Arrays;

/**
 * @author Markus Kemmerling
 */
public class BWTIndex {

   /**
    * For a character/byte b, c[b] is the position in e with the fist appearance of b.
    */
   final private int[] c;

   /**
    * Contains all characters of a text in lexicographical order
    */
   final private byte[] e;  //TODO do i really need it? (binary search in c)

   /**
    * For a character c that exists at position i in e, el[i] is the position in e where the
    * succeeding character of c (regarding of the origin text) can be found.
    */
   final private int[] el;

   /**
    * 
    * @param c
    * @param e
    * @param el
    */
   public BWTIndex(int[] c, byte[] e, int[] el) {
      this.c = c;
      this.e = e;
      this.el = el;
   }

   /**
    * @param character
    *           Byte representation of a character
    * @return First occurrence of the character in this index
    */
   public int getFirstIndexPosition(byte character) {
      int chi = character + 128;
      assert chi > 0;
      assert chi < c.length;
      return c[chi];
   }

   /**
    * For a character c that exists at the given position in this index. This method returns the
    * position in this index where to find the successor regarding to the origin text.
    * 
    * @param pos
    *           Position in this index.
    * @return Position in this index where to find the preceding character.
    */
   public int getSuccedingIndexPosition(int pos) {
      return el[pos];
   }
   
   /**
    * @param pos
    *           Position in this index.
    * @return Byte representation of the character at this position.
    */
   public byte getCharacterAtIndexPosition(int pos) {
      return e[pos];
   }

   /**
    * @return Size of this index (length of associated sequence).
    */
   public int size() {
      return e.length;
   }
   
   /**
    * @param index
    *           BWTIndex to compare with.
    * @return Whether this and the given indexes are equals (does not contain the explicit equality
    *         of the assosiated text/sequence and the alphabet).
    */
   public boolean equals(BWTIndex index) {
      return Arrays.equals(c, index.c) && Arrays.equals(e, index.e) && Arrays.equals(el, index.el);
   }
}
