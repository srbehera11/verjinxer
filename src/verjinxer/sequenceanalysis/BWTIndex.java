package verjinxer.sequenceanalysis;

/**
 * @author Markus Kemmerling
 */
public class BWTIndex {

   /**
    * For a character/byte b, c[b] is the position in e with the fist appearance of b.
    */
   private int[] c = new int[256];

   /**
    * Contains all characters of a text in lexicographical order
    */
   private byte[] e;

   /**
    * For a character c that exists at position i in e, el[i] is the position in e where the
    * succeding character of c (regarding of the origin text) can be found.
    */
   private int[] el;

   /**
    * @param character
    * @return First occurence of the character in this index
    */
   public int getFirstIndex(byte character) {
      return c[character];
   }

   /**
    * For a character c that exists at the given position in this index. This method returns the
    * position in this index where to find the successor regarding to the origin text.
    * 
    * @param index
    * @return Position in this index where to find the preceding character
    */
   public int getSuccedingIndex(int index) {
      return el[index];
   }

   public void setC(int[] c) {
      this.c = c;
   }

   public void setE(byte[] e) {
      this.e = e;
   }

   public void setEl(int[] el) {
      this.el = el;
   }
}
