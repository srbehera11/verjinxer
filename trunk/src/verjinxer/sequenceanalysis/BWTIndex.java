package verjinxer.sequenceanalysis;

import java.util.Arrays;

import verjinxer.BWTBuilder;
import verjinxer.util.MathUtils;

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
   private final int[] el;
   
   /**
    * Maps particular position in the bwt to position in the original text/sequence;
    */
   private final int[] bwt2text;

   /**
    * For each multiple of this value, the position mapper bwt2text exists. In particular, for a
    * position p within the bwt, if p 'modulo' baseIndex == 0, then bwt2text[p/baseIndex] is the
    * corresponding position in the original text/sequence.
    */
   private final int baseIndex;
   
   /**
    * Bitmask to find multiple of the baseIndex. More precisely, (x & bitmask) = (x 'modulo' baseIndex).
    */
   private final int bitmask;
   
   /**
    * (x >> bitshift) = (x / baseIndex);
    */
   private final int bitshift;

   /**
    * 
    * @param c
    * @param el
    * @param bwt2text
    */
   public BWTIndex(final int[] c, final int[] el, final int[] bwt2text) {
      this.c = c;
      this.el = el;
      this.bwt2text = bwt2text;
      this.baseIndex = BWTBuilder.calculateBaseIndex(el.length);
      this.bitmask = baseIndex-1;
      this.bitshift = (int)MathUtils.log2(baseIndex);
      assert(1<<bitshift == baseIndex);
      assert(bitmask>>bitshift == 0);
      
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
         for (int i = highestCharacter; i >= lowestCharacter; i--) {
            if (c[i] <= pos) {
               return (byte) (i - 128);
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
    * @param pos
    *           Position within this index.
    * @return For a given position within this index the corresponding position within the original text/sequence;
    */
   public int map2text(int pos) {
      assert pos < el.length:String.format("el has length %d. Position %d is invalid", el.length, pos);
      pos = el[pos]; // the character at indexPos within this index occurs at el[indexPos] within the bwt.
      int r = 0;
      //while ((pos&bitmask)!=0) {
      while ((pos%baseIndex)!=0) {
         // while no mapping to the text exists, go to the next index position of the succeeding character (regarding the origin text).
         pos = el[pos];
         r++;
      }
      
      //final int mapPos = pos>>bitshift;
      final int mapPos = pos/baseIndex;
      final int textPos = bwt2text[mapPos]-r;
      if (textPos >= 0){
         assert textPos < el.length;
         return textPos;
      } else {
         return el.length+textPos;
      }
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
