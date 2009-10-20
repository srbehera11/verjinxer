package verjinxer.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * @author Markus Kemmerling
 */
public class HuffmanTree {
   /** Characters within this tree. */
   private byte[] characters;

   /** Number of times the characters occur in the origin text/sequence. */
   private int frequency;

   /** Left subtree. */
   private HuffmanTree left;

   /** Right subtree. */
   private HuffmanTree right;

   /**
    * Creates a new Huffman Tree for the given characters.
    * 
    * @param characters
    *           Characters within this tree (should be the union of the characters within the left
    *           and right subtree).
    * @param frequency
    *           Number of times the characters occur in the origin text/sequence (should be the sum
    *           of the frequencies of the left and right subtree).
    * @param left
    *           Left subtree.
    * @param right
    *           Right subtree.
    */
   private HuffmanTree(byte[] characters, int frequency, HuffmanTree left, HuffmanTree right) {
      super();
      this.characters = characters;
      this.frequency = frequency;
      this.left = left;
      this.right = right;
   }

   /**
    * Creates a Huffman Tree that is a leaf (has not subtrees).
    * 
    * @param characters
    *           Characters within this tree.
    * @param frequency
    *           Number of times the characters occur in the origin text/sequence.
    */
   private HuffmanTree(byte[] characters, int frequency) {
      super();
      this.characters = characters;
      this.frequency = frequency;
      this.left = null;
      this.right = null;
   }

   /**
    * Whether the given character belongs to this Huffman Tree.
    * 
    * @param character
    * @return Truee iff the given character belongs to this Huffman Tree.
    */
   public boolean contains(byte character) {
      return Arrays.binarySearch(characters, character) >= 0;
   }

   /**
    * Returns an ordered array of characters belonging to this Huffman Tree.
    * 
    * @return
    */
   public byte[] getCharacters() {
      return characters;
   }

   /**
    * Sets the characters belonging to this Huffman Tree.
    * 
    * @param characters
    */
   public void setCharacters(byte[] characters) {
      this.characters = characters;
   }

   /**
    * @return Number of times the characters belonging to this Huffman Tree occur in the origin
    *         text/sequence.
    */
   public int getFrequency() {
      return frequency;
   }

   /**
    * Sets the number of times the characters belonging to this Huffman Tree occur in the origin
    * text/sequence.
    * 
    * @param frequency
    */
   public void setFrequency(int frequency) {
      this.frequency = frequency;
   }

   /**
    * Whether this Huffman Tree has a left subtree.
    * 
    * @return True iff a left subtree exists.
    */
   public boolean hasLeft() {
      return left != null;
   }

   /**
    * @return The left subtree or null when no one exists.
    */
   public HuffmanTree getLeft() {
      return left;
   }

   /**
    * Sets the left subtree.
    * 
    * @param left
    */
   public void setLeft(HuffmanTree left) {
      this.left = left;
   }

   /**
    * Whether this Huffman Tree has a right subtree.
    * 
    * @return True iff a right subtree exists.
    */
   public boolean hasRight() {
      return right != null;
   }

   /**
    * @return The right subtree or null when no one exists.
    */
   public HuffmanTree getRight() {
      return right;
   }

   /**
    * Sets the right subtree.
    * 
    * @param right
    */
   public void setRight(HuffmanTree right) {
      this.right = right;
   }

   /**
    * Whether the root of this Huffman Tree is a leaf (no subtrees exist).
    * 
    * @return True iff the root of this Huffman Tree is a leaf.
    */
   public boolean isLeaf() {
      return characters.length == 1;
   }

   /**
    * Whether the root of this Huffman Tree is a inner node (subtrees exist).
    * 
    * @return True iff the root of this Huffman Tree is a inner node.
    */
   public boolean isInnenNode() {
      return characters.length > 1;
   }

   /**
    * Whether this Huffman Tree is empty (contains no characters).
    * 
    * @return Truee iff this Huffman Tree is empty.
    */
   public boolean isEmpty() {
      return characters.length == 0;
   }

   /**
    * Builds a Huffman Tree for the given characters and the number of times this characters occur
    * in the origin text/sequence.
    * 
    * @param characters
    *           The characters for that this Huffman Tree will be build.
    * @param frequencies
    *           How often each character occur in the origin text/sequence (characters[i] occur
    *           frequencies[i] times in the text).
    * @return The Huffman Tree.
    */
   public static HuffmanTree buildHuffmanTree(byte[] characters, int[] frequencies) {
      if (characters == null || characters.length == 0) {
         return new HuffmanTree(new byte[] {}, 0);
      }

      PriorityQueue<HuffmanTree> queue = new PriorityQueue<HuffmanTree>(characters.length,
            new HuffmanTreeComparator());

      for (int i = 0; i < characters.length; i++) {
         queue.add(new HuffmanTree(new byte[] { characters[i] }, frequencies[i]));
      }

      while (queue.size() > 1) {
         HuffmanTree t1 = queue.poll();
         HuffmanTree t2 = queue.poll();
         byte[] newChars = new byte[t1.characters.length + t2.characters.length];
         int i = 0;
         for (byte b : t1.characters) {
            newChars[i++] = b;
         }
         for (byte b : t2.characters) {
            newChars[i++] = b;
         }
         Arrays.sort(newChars);

         queue.add(new HuffmanTree(newChars, t1.frequency + t2.frequency, t1, t2));
      }

      return queue.isEmpty() ? null : queue.poll();
   }

   /**
    * A Comparator to order Huffman Trees within a priority queue.
    * 
    * @author Markus Kemmerling
    */
   private static class HuffmanTreeComparator implements Comparator<HuffmanTree> {
      @Override
      public int compare(HuffmanTree o1, HuffmanTree o2) {
         return o1.getFrequency() - o2.getFrequency();
      }
   }

}
