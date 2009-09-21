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
   
   private int lowestCharacter;
   private int highestCharacter;

   /**
    * For a character c that exists at position i in e, el[i] is the position in e where the
    * succeeding character of c (regarding of the origin text) can be found.
    */
   final private int[] el;

   /**
    * 
    * @param c
    * @param el
    */
   public BWTIndex(int[] c, int[] el) {
      this.c = c;
      this.el = el;
      
      for (int i = 1; i < c.length; i++) {
         if (c[i] != 0) {
            lowestCharacter = i-1;
            break;
         }
      }
      
      final int last = c[c.length-1];
      for (int i = c.length-1; i >= 0; i--) {
         if (c[i] != last) {
            highestCharacter = i+1;
            break;
         }
      }
   }

   /**
    * @param character
    *           Byte representation of a character
    * @return First occurrence of the character in this index
    */
   public int getFirstIndexPosition(byte character) {
      int chi = character + 128;
      assert chi >= 0: chi+"";
      assert chi < c.length: chi+"";
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
      // linear search for a small range
      if (highestCharacter - lowestCharacter < 6) {
         for (int i = lowestCharacter; i <= highestCharacter; i++) {
            if (c[i] == pos) {
               return (byte) (i - 128);
            } else if (c[i] > pos) {
               return (byte) (i - 129);
            }
         }
         if (pos < 0 || pos >= el.length) {
            throw new IndexOutOfBoundsException(pos + "");
         } else {
            throw new RuntimeException(); // should not happen
         }

         // binary search for a greater range
      } else {

         int searchResult = Arrays.binarySearch(c, lowestCharacter, highestCharacter + 1, pos);
         if (searchResult >= 0) {
            // we need the last occurrence of pos in c!!!
            while (c[searchResult + 1] - c[searchResult] == 0) {
               searchResult++;
            }
            return (byte) (searchResult - 128);
         } else {
            return (byte) (-searchResult - 130); // ((searchResult + 2) * (-1)) - 128
         }
      }
   }

   /**
    * @return Size of this index (length of associated sequence).
    */
   public int size() {
      return el.length;
   }
   
   /**
    * @param index
    *           BWTIndex to compare with.
    * @return Whether this and the given indexes are equals (does not contain the explicit equality
    *         of the assosiated text/sequence and the alphabet).
    */
   public boolean equals(BWTIndex index) {
      return Arrays.equals(c, index.c) && Arrays.equals(el, index.el);
   }
}
