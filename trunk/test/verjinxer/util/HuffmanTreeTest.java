package verjinxer.util;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class HuffmanTreeTest {

   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
   }

   @AfterClass
   public static void tearDownAfterClass() throws Exception {
   }

   @Test
   public void testBuildHuffmanTree01() {
      byte[] characters = {0,1,2,3};
      int[] frequencies = {10,15,20,26};
      
      HuffmanTree ht = HuffmanTree.buildHuffmanTree(characters, frequencies);
      
      assertEquals(71, ht.getFrequency());
      assertArrayEquals(new byte[]{0,1,2,3}, ht.getCharacters());
      assertEquals(26, ht.getLeft().getFrequency());
      assertArrayEquals(new byte[]{3}, ht.getLeft().getCharacters());
      ht = ht.getRight();
      assertEquals(45, ht.getFrequency());
      assertArrayEquals(new byte[]{0,1,2}, ht.getCharacters());
      assertEquals(20, ht.getLeft().getFrequency());
      assertArrayEquals(new byte[]{2}, ht.getLeft().getCharacters());
      ht = ht.getRight();
      assertEquals(25, ht.getFrequency());
      assertArrayEquals(new byte[]{0,1}, ht.getCharacters());
      assertEquals(10, ht.getLeft().getFrequency());
      assertArrayEquals(new byte[]{0}, ht.getLeft().getCharacters());
      assertEquals(15, ht.getRight().getFrequency());
      assertArrayEquals(new byte[]{1}, ht.getRight().getCharacters());
   }

   @Test
   public void testBuildHuffmanTree02() {
      byte[] characters = {0,1,2,3};
      int[] frequencies = {10,11,12,13};
      
      HuffmanTree ht = HuffmanTree.buildHuffmanTree(characters, frequencies);
      
      assertEquals(46, ht.getFrequency());
      assertArrayEquals(new byte[]{0,1,2,3}, ht.getCharacters());
      
      HuffmanTree htl = ht.getLeft();
      assertEquals(21, htl.getFrequency());
      assertArrayEquals(new byte[]{0,1}, htl.getCharacters());
      assertEquals(10, htl.getLeft().getFrequency());
      assertArrayEquals(new byte[]{0}, htl.getLeft().getCharacters());
      assertEquals(11, htl.getRight().getFrequency());
      assertArrayEquals(new byte[]{1}, htl.getRight().getCharacters());
      
      HuffmanTree htr = ht.getRight();
      assertEquals(25, htr.getFrequency());
      assertArrayEquals(new byte[]{2,3}, htr.getCharacters());
      assertEquals(12, htr.getLeft().getFrequency());
      assertArrayEquals(new byte[]{2}, htr.getLeft().getCharacters());
      assertEquals(13, htr.getRight().getFrequency());
      assertArrayEquals(new byte[]{3}, htr.getRight().getCharacters());
   }

   @Test
   public void testBuildHuffmanTree03() {
      byte[] characters = {0,1,2};
      int[] frequencies = {4,4,3};
      
      HuffmanTree ht = HuffmanTree.buildHuffmanTree(characters, frequencies);
      
      assertEquals(11, ht.getFrequency());
      assertArrayEquals(new byte[]{0,1,2}, ht.getCharacters());
      
      HuffmanTree htl = ht.getLeft();
      assertEquals(4, htl.getFrequency());
      assertArrayEquals(new byte[]{1}, htl.getCharacters());
      
      HuffmanTree htr = ht.getRight();
      assertEquals(7, htr.getFrequency());
      assertArrayEquals(new byte[]{0,2}, htr.getCharacters());
      assertEquals(3, htr.getLeft().getFrequency());
      assertArrayEquals(new byte[]{2}, htr.getLeft().getCharacters());
      assertEquals(4, htr.getRight().getFrequency());
      assertArrayEquals(new byte[]{0}, htr.getRight().getCharacters());
   }
   
   @Test
   public void testBuildEmpty() {
      byte[] characters = {};
      int[] frequencies = {};
      HuffmanTree ht = HuffmanTree.buildHuffmanTree(characters, frequencies);
      assertArrayEquals(new byte[]{}, ht.getCharacters() );
      assertTrue(ht.isEmpty());
      assertFalse(ht.contains((byte)0));
   }

}
