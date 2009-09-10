package verjinxer;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.junit.Test;

import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.sequenceanalysis.BWTIndex;
import verjinxer.sequenceanalysis.Sequences;
import verjinxer.sequenceanalysis.SuffixXorDLL;

public class BWTIndexBuilderTest {

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
      BWTIndex reference = new BWTIndex(c,e,el);
      
      for(int i = -128; i < 128; i++) {
         assertEquals(String.format("Array 'c' differs at position %s (expected: %s - actual: %s)", i, c[i+128], index.getFirstIndexPosition((byte)i)), c[i+128], index.getFirstIndexPosition((byte)i));
      }

      for(int i = 0; i < e.length; i++) {
         assertEquals(String.format("Array 'e' differs at position %s (expected: %s - actual: %s)", i, e[i], index.getCharacterAtIndexPosition(i)), e[i], index.getCharacterAtIndexPosition(i));
      }

      for(int i = 0; i < el.length; i++) {
         assertEquals(String.format("Array 'el' differs at position %s (expected: %s - actual: %s)", i, el[i], index.getSuccedingIndexPosition(i)), el[i], index.getSuccedingIndexPosition(i));
      }
      
      assertTrue(reference.equals(index));
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
      BWTIndex reference = new BWTIndex(c,e,el);
      
      for(int i = -128; i < 128; i++) {
         assertEquals(String.format("Array 'c' differs at position %s (expected: %s - actual: %s)", i, c[i+128], index.getFirstIndexPosition((byte)i)), c[i+128], index.getFirstIndexPosition((byte)i));
      }

      for(int i = 0; i < e.length; i++) {
         assertEquals(String.format("Array 'e' differs at position %s (expected: %s - actual: %s)", i, e[i], index.getCharacterAtIndexPosition(i)), e[i], index.getCharacterAtIndexPosition(i));
      }

      for(int i = 0; i < el.length; i++) {
         assertEquals(String.format("Array 'el' differs at position %s (expected: %s - actual: %s)", i, el[i], index.getSuccedingIndexPosition(i)), el[i], index.getSuccedingIndexPosition(i));
      }
      
      assertTrue(reference.equals(index));
   }
}
