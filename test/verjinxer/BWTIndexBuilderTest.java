package verjinxer;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.sequenceanalysis.BWTIndex;
import verjinxer.sequenceanalysis.Sequences;
import verjinxer.sequenceanalysis.SuffixXorDLL;

public class BWTIndexBuilderTest {
   
   final static Alphabet alphabet = new Alphabet(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9",
         "##separators:0" });
   
   @Test
   public void testBuild01() {
      byte[] s = {0,0,3,0,2,0,0,3,0,2,4,1}; //eenemeenemuh
      
      Sequences sequence = Sequences.createEmptySequencesInMemory();
      try {
         sequence.addSequence(ByteBuffer.wrap(s));
      } catch (IOException e) {
         e.printStackTrace();
      }
      
      final SuffixTrayBuilder stb = new SuffixTrayBuilder(sequence, Alphabet.DNA());
      stb.build("bothLR"); //WARNING: change the method and you must change the type cast in the next line!
      assert (stb.getSuffixDLL() instanceof SuffixXorDLL);
      final SuffixXorDLL suffixDLL = (SuffixXorDLL)stb.getSuffixDLL(); // type cast is okay because I used method 'bothLR' to build the list
      
      BWTIndex index = BWTIndexBuilder.build(suffixDLL);
      
    //test index
      final int[] c = new int[256];
      for(int i = 0; i < 128; i++) {
         c[i] = 0;
      }
      c[128] = 0;
      c[129] = 6;
      c[130] = 7;
      c[131] = 9;
      c[132] = 11;
      for(int i = 133; i < 256; i++) {
         c[i] = 12;
      }
      final byte[] e = {0,0,0,0,0,0,1,2,2,3,3,4};
      final int[] el = {4,5,7,8,9,10,0,1,11,2,3,6};
      
      for(int i = -128; i < 128; i++) {
         assertEquals(String.format("Array 'c' differs at position %s (expected: %s - actual: %s)", i, c[i+128], index.getFirstIndexPosition((byte)i)), c[i+128], index.getFirstIndexPosition((byte)i));
      }

      for(int i = 0; i < e.length; i++) {
         assertEquals(String.format("Array 'e' differs at position %s (expected: %s - actual: %s)", i, e[i], index.getCharacterAtIndexPosition(i)), e[i], index.getCharacterAtIndexPosition(i));
      }

      for(int i = 0; i < el.length; i++) {
         assertEquals(String.format("Array 'el' differs at position %s (expected: %s - actual: %s)", i, el[i], index.getSuccedingIndexPosition(i)), el[i], index.getSuccedingIndexPosition(i));
      }
      
      //test index
      bwt(s);
      
      for(int i = -128; i < 128; i++) {
         assertEquals(String.format("Array 'c' differs at position %s (expected: %s - actual: %s)", i, BWTIndexBuilderTest.c[i+128], index.getFirstIndexPosition((byte)i)), BWTIndexBuilderTest.c[i+128], index.getFirstIndexPosition((byte)i));
      }

      for(int i = 0; i < BWTIndexBuilderTest.e.length; i++) {
         assertEquals(String.format("Array 'e' differs at position %s (expected: %s - actual: %s)", i, BWTIndexBuilderTest.e[i], index.getCharacterAtIndexPosition(i)), BWTIndexBuilderTest.e[i], index.getCharacterAtIndexPosition(i));
      }

      for(int i = 0; i < BWTIndexBuilderTest.el.length; i++) {
         assertEquals(String.format("Array 'el' differs at position %s (expected: %s - actual: %s)", i, BWTIndexBuilderTest.el[i], index.getSuccedingIndexPosition(i)), BWTIndexBuilderTest.el[i], index.getSuccedingIndexPosition(i));
      }
   }

   @Test
   public void testBuild02() {
      byte[] s = {0,0,3,0,2,0,0,3,0,2,1}; //eenemeenemh
      
      Sequences sequence = Sequences.createEmptySequencesInMemory();
      try {
         sequence.addSequence(ByteBuffer.wrap(s));
      } catch (IOException e) {
         e.printStackTrace();
      }
      
      final SuffixTrayBuilder stb = new SuffixTrayBuilder(sequence, Alphabet.DNA());
      stb.build("bothLR"); //WARNING: change the method and you must change the type cast in the next line!
      assert (stb.getSuffixDLL() instanceof SuffixXorDLL);
      final SuffixXorDLL suffixDLL = (SuffixXorDLL)stb.getSuffixDLL(); // type cast is okay because I used method 'bothLR' to build the list
      
      BWTIndex index = BWTIndexBuilder.build(suffixDLL);
      
      //test index
      final int[] c = new int[256];
      for(int i = 0; i < 128; i++) {
         c[i] = 0;
      }
      c[128] = 0;
      c[129] = 6;
      c[130] = 7;
      c[131] = 9;
      for(int i = 132; i < 256; i++) {
         c[i] = 11;
      }
      final byte[] e = {0,0,0,0,0,0,1,2,2,3,3};
      final int[] el = {4,5,7,8,9,10,0,1,6,2,3};
      
      for(int i = -128; i < 128; i++) {
         assertEquals(String.format("Array 'c' differs at position %s (expected: %s - actual: %s)", i, c[i+128], index.getFirstIndexPosition((byte)i)), c[i+128], index.getFirstIndexPosition((byte)i));
      }

      for(int i = 0; i < e.length; i++) {
         assertEquals(String.format("Array 'e' differs at position %s (expected: %s - actual: %s)", i, e[i], index.getCharacterAtIndexPosition(i)), e[i], index.getCharacterAtIndexPosition(i));
      }

      for(int i = 0; i < el.length; i++) {
         assertEquals(String.format("Array 'el' differs at position %s (expected: %s - actual: %s)", i, el[i], index.getSuccedingIndexPosition(i)), el[i], index.getSuccedingIndexPosition(i));
      }
      
      //test index
      bwt(s);
      
      for(int i = -128; i < 128; i++) {
         assertEquals(String.format("Array 'c' differs at position %s (expected: %s - actual: %s)", i, BWTIndexBuilderTest.c[i+128], index.getFirstIndexPosition((byte)i)), BWTIndexBuilderTest.c[i+128], index.getFirstIndexPosition((byte)i));
      }

      for(int i = 0; i < BWTIndexBuilderTest.e.length; i++) {
         assertEquals(String.format("Array 'e' differs at position %s (expected: %s - actual: %s)", i, BWTIndexBuilderTest.e[i], index.getCharacterAtIndexPosition(i)), BWTIndexBuilderTest.e[i], index.getCharacterAtIndexPosition(i));
      }

      for(int i = 0; i < BWTIndexBuilderTest.el.length; i++) {
         assertEquals(String.format("Array 'el' differs at position %s (expected: %s - actual: %s)", i, BWTIndexBuilderTest.el[i], index.getSuccedingIndexPosition(i)), BWTIndexBuilderTest.el[i], index.getSuccedingIndexPosition(i));
      }
   }

   @Test
   public void testBuild04() {
      byte[] s = {4,4,2,2,4,1,2,4,4,2,1,3,3,3,2,2,1,4,1,1,1,3,2,2,4,0};
      
      Sequences sequence = Sequences.createEmptySequencesInMemory();
      try {
         sequence.addSequence(ByteBuffer.wrap(s));
      } catch (IOException e) {
         e.printStackTrace();
      }
      
      final SuffixTrayBuilder stb = new SuffixTrayBuilder(sequence, alphabet);
      stb.build("bothLR"); //WARNING: change the method and you must change the type cast in the next line!
      assert (stb.getSuffixDLL() instanceof SuffixXorDLL);
      final SuffixXorDLL suffixDLL = (SuffixXorDLL)stb.getSuffixDLL(); // type cast is okay because I used method 'bothLR' to build the list
      
      BWTIndex index = BWTIndexBuilder.build(suffixDLL);
      
      //test index
      bwt(s);
      
      for(int i = -128; i < 128; i++) {
         assertEquals(String.format("Array 'c' differs at position %s (expected: %s - actual: %s)", i, c[i+128], index.getFirstIndexPosition((byte)i)), c[i+128], index.getFirstIndexPosition((byte)i));
      }

      for(int i = 0; i < e.length; i++) {
         assertEquals(String.format("Array 'e' differs at position %s (expected: %s - actual: %s)", i, e[i], index.getCharacterAtIndexPosition(i)), e[i], index.getCharacterAtIndexPosition(i));
      }

      for(int i = 0; i < el.length; i++) {
         assertEquals(String.format("Array 'el' differs at position %s (expected: %s - actual: %s)", i, el[i], index.getSuccedingIndexPosition(i)), el[i], index.getSuccedingIndexPosition(i));
      }
   }
   
   @Test
   public void testBuild05() {
      byte[][] s = {{4,4,2,2,4,1,2,4,4,2,1,3,3,3,2,2,1,4,1,1,1,3,2,2,4,0},
            {4,3,2,1,4,3,2,1,4,3,2,4,1,3,2,4,1,3,2,4,4,4,2,3,1,3,2,4,1,4,0},
            {3,2,4,3,2,4,4,1,3,4,2,1,4,2,3,4,2,3,4,4,2,1,3,4,1,4,4,3,2,1,2,3,1,3,2,1,2,3,1,3,2,1,2,1,0},
            {4,3,1,4,3,2,4,1,2,1,3,2,1,2,3,1,4,2,3,4,1,2,3,4,1,2,3,1,2,3,1,2,3,1,2,3,1,2,3,1,2,3,0},
            {3,1,4,3,1,3,2,4,1,3,2,1,3,2,4,1,3,2,1,2,3,4,1,3,2,1,3,2,4,0},
            {4,3,4,3,2,1,3,2,4,1,3,2,4,1,3,2,4,1,2,3,4,1,3,2,4,1,3,2,4,1,2,3,4,0},
            {4,3,2,1,4,3,2,1,2,3,2,3,4,3,2,1,3,2,4,2,3,1,0},
            {1,2,4,1,4,2,3,1,4,2,1,4,3,2,1,4,3,2,0}
      };
      
      for (int si = 0; si < s.length; si++) {
         Sequences sequence = Sequences.createEmptySequencesInMemory();
         try {
            sequence.addSequence(ByteBuffer.wrap(s[si]));
         } catch (IOException e) {
            e.printStackTrace();
         }

         final SuffixTrayBuilder stb = new SuffixTrayBuilder(sequence, alphabet);
         stb.build("bothLR"); // WARNING: change the method and you must change the type cast in the
                              // next line!
         assert (stb.getSuffixDLL() instanceof SuffixXorDLL);
         final SuffixXorDLL suffixDLL = (SuffixXorDLL) stb.getSuffixDLL(); // type cast is okay
                                                                           // because I used method
                                                                           // 'bothLR' to build the
                                                                           // list

         BWTIndex index = BWTIndexBuilder.build(suffixDLL);

         // test index
         bwt(s[si]);

         for (int i = -128; i < 128; i++) {
            assertEquals(String.format(
                  "[Test %d] Array 'c' differs at position %s (expected: %s - actual: %s)", si, i,
                  c[i + 128], index.getFirstIndexPosition((byte) i)), c[i + 128],
                  index.getFirstIndexPosition((byte) i));
         }

         for (int i = 0; i < e.length; i++) {
            assertEquals(String.format(
                  "[Test %d] Array 'e' differs at position %s (expected: %s - actual: %s)", si, i,
                  e[i], index.getCharacterAtIndexPosition(i)), e[i],
                  index.getCharacterAtIndexPosition(i));
         }

         for (int i = 0; i < el.length; i++) {
            assertEquals(String.format(
                  "[Test %d] Array 'el' differs at position %s (expected: %s - actual: %s)", si, i,
                  el[i], index.getSuccedingIndexPosition(i)), el[i],
                  index.getSuccedingIndexPosition(i));
         }
      }
   }
   
   @Test
   public void testBuild06() {
      byte[]s = null;
      
      Random rand = new Random(100);
      
      for (int si = 0; si < 50; si++) {
         s = new byte[500 + rand.nextInt(500)];
         
         for(int j = 0; j < s.length-1; j++) {
            s[j] = (byte)(1 + rand.nextInt(8));
         }
         s[s.length-1] = 0;
         
         Sequences sequence = Sequences.createEmptySequencesInMemory();
         try {
            sequence.addSequence(ByteBuffer.wrap(s));
         } catch (IOException e) {
            e.printStackTrace();
         }

         final SuffixTrayBuilder stb = new SuffixTrayBuilder(sequence, alphabet);
         stb.build("bothLR"); // WARNING: change the method and you must change the type cast in the
                              // next line!
         assert (stb.getSuffixDLL() instanceof SuffixXorDLL);
         final SuffixXorDLL suffixDLL = (SuffixXorDLL) stb.getSuffixDLL(); // type cast is okay
                                                                           // because I used method
                                                                           // 'bothLR' to build the
                                                                           // list

         BWTIndex index = BWTIndexBuilder.build(suffixDLL);

         // test index
         bwt(s);

         for (int i = -128; i < 128; i++) {
            assertEquals(String.format(
                  "[Test %d] Array 'c' differs at position %s (expected: %s - actual: %s)", si, i,
                  c[i + 128], index.getFirstIndexPosition((byte) i)), c[i + 128],
                  index.getFirstIndexPosition((byte) i));
         }

         for (int i = 0; i < e.length; i++) {
            assertEquals(String.format(
                  "[Test %d] Array 'e' differs at position %s (expected: %s - actual: %s)", si, i,
                  e[i], index.getCharacterAtIndexPosition(i)), e[i],
                  index.getCharacterAtIndexPosition(i));
         }

         for (int i = 0; i < el.length; i++) {
            assertEquals(String.format(
                  "[Test %d] Array 'el' differs at position %s (expected: %s - actual: %s)", si, i,
                  el[i], index.getSuccedingIndexPosition(i)), el[i],
                  index.getSuccedingIndexPosition(i));
         }
      }
   }
   
   // here are results of bwt(byte[]) stored. 
   private static int[] c;
   private static byte[] e;
   private static int[] el;
   
   public static void bwt(final byte[] seq) {
      StringBuilder sb = new StringBuilder(seq.length);
      for(byte b: seq) {
         sb.append(b);
      }
      String org = sb.toString();
      
      String[] bwt_matrix = new String[seq.length];
      
      bwt_matrix[0] = org;
      for (int i = 1; i < bwt_matrix.length; i++) {
         bwt_matrix[i] = bwt_matrix[i-1].substring(1) + bwt_matrix[i-1].charAt(0);
      }
      
//      //print matrix
//      System.out.println("Matrix");
//      for(String s: bwt_matrix) {
//         System.out.println(s);
//      }
      
      Arrays.sort(bwt_matrix);
//      //print matrix
//      System.out.println("-----------------------------------------------------------------------------------------");
//      System.out.println("sorted Matrix");
//      for(String s: bwt_matrix) {
//         System.out.println(s);
//      }
      
      e = new byte[bwt_matrix.length];
      byte[] l = new byte[bwt_matrix.length];
      for (int i = 0; i < bwt_matrix.length; i++) {
         e[i] = Byte.parseByte(bwt_matrix[i].charAt(0)+"");
         l[i] = Byte.parseByte(bwt_matrix[i].charAt(bwt_matrix[i].length()-1)+"");
      }
      
      int[] counter = new int [256];
      for (byte b: e) {
         counter[b+128]++;
      }
      
      c = new int[256];
      c[0] = 0;
      for(int i = 1; i < c.length; i++) {
         c[i] = c[i-1] + counter[i-1];
      }
      
      counter = Arrays.copyOf(c, c.length);
      
      el = new int[e.length];
      for(int i = 0; i < l.length; i++) {
         final byte b = l[i];
         final int bi = b + 128;
         el[counter[bi]] = i;
         counter[bi]++;
      }
      
//      //print c
//      System.out.println("-----------------------------------------------------------------------------------------");
//      System.out.println("c");
//      for(int i = 0; i < c.length; i++) {
//         System.out.printf("%s: %s%n", i-128, c[i]);
//      }
//      
//      //print e
//      System.out.println("-----------------------------------------------------------------------------------------");
//      System.out.println("e");
//      for(int i = 0; i < e.length; i++) {
//         System.out.printf("%s: %s%n", i, e[i]);
//      }
//      
//      //print l
//      System.out.println("-----------------------------------------------------------------------------------------");
//      System.out.println("l");
//      for(int i = 0; i < e.length; i++) {
//         System.out.printf("%s: %s%n", i, l[i]);
//      }
//      
//      //print el
//      System.out.println("-----------------------------------------------------------------------------------------");
//      System.out.println("el");
//      for(int i = 0; i < e.length; i++) {
//         System.out.printf("%s: %s%n", i, el[i]);
//      }
      
      //check el array:
      for(int i = 0; i < el.length; i++) {
         final String tmp = bwt_matrix[i].substring(1) +  bwt_matrix[i].charAt(0);
         assertTrue(String.format("Failure in el reference array at position %d", i),  tmp.equals(bwt_matrix[el[i]]));
      }
      
      
   }
}
 