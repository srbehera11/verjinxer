package verjinxer.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * @author Markus Kemmerling
 */
public class HuffmanTree {
   private byte[] characters;
   private int frequency;
   private HuffmanTree left;
   private HuffmanTree right;
   
   private HuffmanTree(byte[] characters, int frequency, HuffmanTree left, HuffmanTree right) {
      super();
      this.characters = characters;
      this.frequency = frequency;
      this.left = left;
      this.right = right;
   }
   
   private HuffmanTree(byte[] characters, int frequency) {
      super();
      this.characters = characters;
      this.frequency = frequency;
      this.left = null;
      this.right = null;
   }
   
   public byte[] getCharacters() {
      return characters;
   }
   
   public void setCharacters(byte[] characters) {
      this.characters = characters;
   }
   
   public int getFrequency() {
      return frequency;
   }
   
   public void setFrequency(int frequency) {
      this.frequency = frequency;
   }
   
   public HuffmanTree getLeft() {
      return left;
   }
   
   public void setLeft(HuffmanTree left) {
      this.left = left;
   }
   
   public HuffmanTree getRight() {
      return right;
   }
   
   public void setRight(HuffmanTree right) {
      this.right = right;
   }
   
   public static HuffmanTree buildHuffmanTree(byte[] characters, int[] frequencies) {
      PriorityQueue<HuffmanTree> queue = new PriorityQueue<HuffmanTree>(characters.length, new HuffmanTreeComparator());
      
      for(int i = 0; i < characters.length; i++) {
         queue.add(new HuffmanTree(new byte[]{characters[i]}, frequencies[i]));
      }
      
      while(queue.size() > 1) {
         HuffmanTree t1 = queue.poll();
         HuffmanTree t2 = queue.poll();
         byte[] newChars = new byte[t1.characters.length + t2.characters.length];
         int i = 0;
         for(byte b: t1.characters) {
            newChars[i++] = b;
         }
         for(byte b: t2.characters) {
            newChars[i++] = b;
         }
         Arrays.sort(newChars);
         
         queue.add(new HuffmanTree(newChars, t1.frequency + t2.frequency, t1, t2));
      }
      
      return queue.isEmpty()? null: queue.poll();
   }
   
   private static class HuffmanTreeComparator implements Comparator<HuffmanTree> {
      @Override
      public int compare(HuffmanTree o1, HuffmanTree o2) {
         return o1.getFrequency() - o2.getFrequency();
      }
   }
   
}
