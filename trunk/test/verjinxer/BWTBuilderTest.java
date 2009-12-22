package verjinxer;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import org.junit.Test;

import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.sequenceanalysis.Sequences;
import verjinxer.sequenceanalysis.SuffixXorDLL;

/**
 * @author Markus Kemmerling
 */
public class BWTBuilderTest {
   
   final static Alphabet alphabet = Alphabet.DNA();
   
   @Test
   public void testBuildDet() {
      byte[][] s = { 
            {1,1,3,1,2,1,1,3,1,0,2,4,0,-1,-2},
            {1,1,3,1,2,1,1,3,1,2,0,-1,-2},
            {4,4,2,2,4,1,2,4,4,2,1,0,3,3,3,2,2,1,4,1,1,0,0,0,0,1,3,2,2,4,0,-1,-2},
            {4,4,2,2,4,1,2,4,4,0,2,1,3,3,3,2,2,1,4,1,1,1,3,2,2,4,0,-1,-2},
            {4,3,2,1,4,3,0,2,1,4,3,2,4,1,-1,3,2,4,1,3,2,4,4,4,2,3,1,3,2,4,1,4,0,-1,-2},
            {3,2,4,3,0,2,4,4,1,3,4,0,2,1,4,2,3,4,2,0,3,4,4,2,1,3,4,1,4,4,3,2,1,2,3,1,3,2,1,2,3,1,3,2,1,2,1,0,-1,-2},
            {4,3,1,4,3,2,4,1,2,1,3,2,1,2,3,1,4,2,3,4,1,2,0,3,4,1,2,3,1,2,3,1,2,3,1,2,3,1,2,3,1,2,3,0,-1,-2},
            {3,1,4,3,1,3,0,2,4,1,0,3,2,1,3,2,4,1,3,2,-1,1,2,3,4,1,3,2,1,3,2,4,0,-1,-2},
            {4,3,4,3,2,1,3,2,4,1,3,2,4,1,3,2,4,1,2,3,4,1,3,2,4,1,3,2,4,1,2,3,4,0,-1,-2},
            {4,3,2,1,4,3,2,1,2,3,2,3,4,3,2,1,3,2,4,2,3,1,0,-1,-2},
            {1,2,4,1,4,2,3,1,0,4,2,1,4,3,2,1,4,3,2,0,-1,-2},
            {-1,-1,-1,-2},
            {4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,-1,-2},
            {1,2,1,0,0,3,2,4,2,1,2,-1,2,1,4,4,4,-1,-1,4,4,4,-1,4,4,4,2,1,0,0,2,4,4,4-1,-2}
      };
      
      for (int i = 0; i < s.length; i++) {
         Sequences sequence = Sequences.createEmptySequencesInMemory();
         try {
            sequence.addSequence(ByteBuffer.wrap(s[i]));
         } catch (IOException e) {
            e.printStackTrace();
         }

         final SuffixTrayBuilder stb = new SuffixTrayBuilder(sequence, alphabet, "suffix");
         stb.build("bothLR"); // WARNING: change the method and you must change the type cast in the
                              // next line!
         assert (stb.getSuffixDLL() instanceof SuffixXorDLL);
         final SuffixXorDLL suffixDLL = (SuffixXorDLL) stb.getSuffixDLL(); // type cast is okay
                                                                           // because I used method
                                                                           // 'bothLR' to build the
                                                                           // list
         
         byte[] referenceBWT = bwt(s[i]);
         BWTBuilder.BWT bwt = BWTBuilder.build(suffixDLL);
         
         assertArrayEquals(String.format("Failure by sequence %d wile using SuffixXorDLL", i), referenceBWT , bwt.bwt);
         checkSampledSuffixArray(bwt.bwt, bwt.sampledSuffixArray, sequence.array());
         
         suffixDLL.resetToBegin();
         int[] a = new int[suffixDLL.capacity()];
         int j = 0;
         if (suffixDLL.getCurrentPosition() != -1) {
            a[j++] = suffixDLL.getCurrentPosition();
            while (suffixDLL.hasNextUp()) {
               suffixDLL.nextUp();
               a[j++] = suffixDLL.getCurrentPosition();
            }
         }
         IntBuffer buf = IntBuffer.wrap(a);
         bwt = BWTBuilder.build(buf, sequence);
         assertArrayEquals(String.format("Failure by sequence %d wile using IntBuffer", i), referenceBWT , bwt.bwt);
         checkSampledSuffixArray(bwt.bwt, bwt.sampledSuffixArray, sequence.array());
         
      }
   }
   
   @Test
   public void testBuildRandome() {
      byte[]s = null;
      
      Random rand = new Random(100);
      
      for (int i = 0; i < 50; i++) {
         s = new byte[500 + rand.nextInt(500)];
         
         for (int j = 0; j < s.length - 1; j++) {
            s[j] = (byte) (rand.nextInt(6) - 1);
         }
         s[s.length - 1] = -2;
         
         Sequences sequence = Sequences.createEmptySequencesInMemory();
         try {
            sequence.addSequence(ByteBuffer.wrap(s));
         } catch (IOException e) {
            e.printStackTrace();
         }

         final SuffixTrayBuilder stb = new SuffixTrayBuilder(sequence, alphabet, "suffix");
         stb.build("bothLR"); // WARNING: change the method and you must change the type cast in the
                              // next line!
         assert (stb.getSuffixDLL() instanceof SuffixXorDLL);
         final SuffixXorDLL suffixDLL = (SuffixXorDLL) stb.getSuffixDLL(); // type cast is okay
                                                                           // because I used method
                                                                           // 'bothLR' to build the
                                                                           // list
         
         byte[] referenceBWT = bwt(s);
         BWTBuilder.BWT bwt = BWTBuilder.build(suffixDLL);
         
         assertArrayEquals(String.format("Failure by sequence %d wile using SuffixXorDLL", i), referenceBWT , bwt.bwt);
         checkSampledSuffixArray(bwt.bwt, bwt.sampledSuffixArray, sequence.array());
         
         suffixDLL.resetToBegin();
         int[] a = new int[suffixDLL.capacity()];
         int j = 0;
         if (suffixDLL.getCurrentPosition() != -1) {
            a[j++] = suffixDLL.getCurrentPosition();
            while (suffixDLL.hasNextUp()) {
               suffixDLL.nextUp();
               a[j++] = suffixDLL.getCurrentPosition();
            }
         }
         IntBuffer buf = IntBuffer.wrap(a);
         bwt = BWTBuilder.build(buf, sequence);
         assertArrayEquals(String.format("Failure by sequence %d wile using IntBuffer", i), referenceBWT , bwt.bwt);
         checkSampledSuffixArray(bwt.bwt, bwt.sampledSuffixArray, sequence.array());
      }
   }
   
   @Test
   public void testBuildchrM() {
      final byte[] s = {2,0,3,1,0,1,0,2,2,3,1,3,0,3,1,0,1,1,1,3,0,3,3,0,0,1,1,0,1,3,1,0,1,2,2,2,0,2,1,3,1,3,1,1,0,3,2,1,0,3,3,3,2,2,3,0,3,3,3,3,1,2,3,1,3,2,2,2,2,2,2,3,2,3,2,1,0,1,2,1,2,0,3,0,2,1,0,3,3,2,1,2,0,2,0,1,2,1,3,2,2,0,2,1,1,2,2,0,2,1,0,1,1,1,3,0,3,2,3,1,2,1,0,2,3,0,3,1,3,2,3,1,3,3,3,2,0,3,3,1,1,3,2,1,1,3,1,0,3,3,1,3,0,3,3,0,3,3,3,0,3,1,2,1,0,1,1,3,0,1,2,3,3,1,0,0,3,0,3,3,0,1,0,2,2,1,2,0,0,1,0,3,0,1,1,3,0,1,3,0,0,0,2,3,2,3,2,3,3,0,0,3,3,0,0,3,3,0,0,3,2,1,3,3,2,3,0,2,2,0,1,0,3,0,0,3,0,0,3,0,0,1,0,0,3,3,2,0,0,3,2,3,1,3,2,1,0,1,0,2,1,1,2,1,3,3,3,1,1,0,1,0,1,0,2,0,1,0,3,1,0,3,0,0,1,0,0,0,0,0,0,3,3,3,1,1,0,1,1,0,0,0,1,1,1,1,1,1,1,1,3,1,1,1,1,1,1,2,1,3,3,1,3,2,2,1,1,0,1,0,2,1,0,1,3,3,0,0,0,1,0,1,0,3,1,3,1,3,2,1,1,0,0,0,1,1,1,1,0,0,0,0,0,1,0,0,0,2,0,0,1,1,1,3,0,0,1,0,1,1,0,2,1,1,3,0,0,1,1,0,2,0,3,3,3,1,0,0,0,3,3,3,3,0,3,1,3,3,3,0,2,2,1,2,2,3,0,3,2,1,0,1,3,3,3,3,0,0,1,0,2,3,1,0,1,1,1,1,1,1,0,0,1,3,0,0,1,0,1,0,3,3,0,3,3,3,3,1,1,1,1,3,1,1,1,0,1,3,1,1,1,0,3,0,1,3,0,1,3,0,0,3,1,3,1,0,3,1,0,0,3,0,1,0,0,1,1,1,1,1,2,1,1,1,0,3,1,1,3,0,1,1,1,0,2,1,0,1,0,1,0,1,0,1,0,1,1,2,1,3,2,1,3,0,0,1,1,1,1,0,3,0,1,1,1,1,2,0,0,1,1,0,0,1,1,0,0,0,1,1,1,1,0,0,0,2,0,1,0,1,1,1,1,1,1,0,1,0,2,3,3,3,0,3,2,3,0,2,1,3,3,0,1,1,3,1,1,3,1,0,0,0,2,1,0,0,3,0,1,0,1,3,2,0,0,0,0,3,2,3,3,3,0,2,0,1,2,2,2,1,3,1,0,1,0,3,1,0,1,1,1,1,0,3,0,0,0,1,0,0,0,3,0,2,2,3,3,3,2,2,3,1,1,3,0,2,1,1,3,3,3,1,3,0,3,3,0,2,1,3,1,3,3,0,2,3,0,0,2,0,3,3,0,1,0,1,0,3,2,1,0,0,2,1,0,3,1,1,1,1,2,3,3,1,1,0,2,3,2,0,2,3,3,1,0,1,1,1,3,1,3,0,0,0,3,1,0,1,1,0,1,2,0,3,1,0,0,0,0,2,2,2,0,1,0,0,2,1,0,3,1,0,0,2,1,0,1,2,1,0,2,1,0,0,3,2,1,0,2,1,3,1,0,0,0,0,1,2,1,3,3,0,2,1,1,3,0,2,1,1,0,1,0,1,1,1,1,1,0,1,2,2,2,0,0,0,1,0,2,1,0,2,3,2,0,3,3,0,0,1,1,3,3,3,0,2,1,0,0,3,0,0,0,1,2,0,0,0,2,3,3,3,0,0,1,3,0,0,2,1,3,0,3,0,1,3,0,0,1,1,1,1,0,2,2,2,3,3,2,2,3,1,0,0,3,3,3,1,2,3,2,1,1,0,2,1,1,0,1,1,2,1,2,2,3,1,0,1,0,1,2,0,3,3,0,0,1,1,1,0,0,2,3,1,0,0,3,0,2,0,0,2,1,1,2,2,1,2,3,0,0,0,2,0,2,3,2,3,3,3,3,0,2,0,3,1,0,1,1,1,1,1,3,1,1,1,1,0,0,3,0,0,0,2,1,3,0,0,0,0,1,3,1,0,1,1,3,2,0,2,3,3,2,3,0,0,0,0,0,0,1,3,1,1,0,2,3,3,2,0,1,0,1,0,0,0,0,3,0,2,0,1,3,0,1,2,0,0,0,2,3,2,2,1,3,3,3,0,0,1,0,3,0,3,1,3,2,0,0,1,0,1,0,1,0,0,3,0,2,1,3,0,0,2,0,1,1,1,0,0,0,1,3,2,2,2,0,3,3,0,2,0,3,0,1,1,1,1,0,1,3,0,3,2,1,3,3,0,2,1,1,1,3,0,0,0,1,1,3,1,0,0,1,0,2,3,3,0,0,0,3,1,0,0,1,0,0,0,0,1,3,2,1,3,1,2,1,1,0,2,0,0,1,0,1,3,0,1,2,0,2,1,1,0,1,0,2,1,3,3,0,0,0,0,1,3,1,0,0,0,2,2,0,1,1,3,2,2,1,2,2,3,2,1,3,3,1,0,3,0,3,1,1,1,3,1,3,0,2,0,2,2,0,2,1,1,3,2,3,3,1,3,2,3,0,0,3,1,2,0,3,0,0,0,1,1,1,1,2,0,3,1,0,0,1,1,3,1,0,1,1,0,1,1,3,1,3,3,2,1,3,1,0,2,1,1,3,0,3,0,3,0,1,1,2,1,1,0,3,1,3,3,1,0,2,1,0,0,0,1,1,1,3,2,0,3,2,0,0,2,2,1,3,0,1,0,0,0,2,3,0,0,2,1,2,1,0,0,2,3,0,1,1,1,0,1,2,3,0,0,0,2,0,1,2,3,3,0,2,2,3,1,0,0,2,2,3,2,3,0,2,1,1,1,0,3,2,0,2,2,3,2,2,1,0,0,2,0,0,0,3,2,2,2,1,3,0,1,0,3,3,3,3,1,3,0,1,1,1,1,0,2,0,0,0,0,1,3,0,1,2,0,3,0,2,1,1,1,3,3,0,3,2,0,0,0,1,3,3,0,0,2,2,2,3,1,2,0,0,2,2,3,2,2,0,3,3,3,0,2,1,0,2,3,0,0,0,1,3,2,0,2,0,2,3,0,2,0,2,3,2,1,3,3,0,2,3,3,2,0,0,1,0,2,2,2,1,1,1,3,2,0,0,2,1,2,1,2,3,0,1,0,1,0,1,1,2,1,1,1,2,3,1,0,1,1,1,3,1,1,3,1,0,0,2,3,0,3,0,1,3,3,1,0,0,0,2,2,0,1,0,3,3,3,0,0,1,3,0,0,0,0,1,1,1,1,3,0,1,2,1,0,3,3,3,0,3,0,3,0,2,0,2,2,0,2,0,1,0,0,2,3,1,2,3,0,0,1,0,3,2,2,3,0,0,2,3,2,3,0,1,3,2,2,0,0,0,2,3,2,1,0,1,3,3,2,2,0,1,2,0,0,1,1,0,2,0,2,3,2,3,0,2,1,3,3,0,0,1,0,1,0,0,0,2,1,0,1,1,1,0,0,1,3,3,0,1,0,1,3,3,0,2,2,0,2,0,3,3,3,1,0,0,1,3,3,0,0,1,3,3,2,0,1,1,2,1,3,1,3,2,0,2,1,3,0,0,0,1,1,3,0,2,1,1,1,1,0,0,0,1,1,1,0,1,3,1,1,0,1,1,3,3,0,1,3,0,1,1,0,2,0,1,0,0,1,1,3,3,0,2,1,1,0,0,0,1,1,0,3,3,3,0,1,1,1,0,0,0,3,0,0,0,2,3,0,3,0,2,2,1,2,0,3,0,2,0,0,0,3,3,2,0,0,0,1,1,3,2,2,1,2,1,0,0,3,0,2,0,3,0,3,0,2,3,0,1,1,2,1,0,0,2,2,2,0,0,0,2,0,3,2,0,0,0,0,0,3,3,0,3,0,0,1,1,0,0,2,1,0,3,0,0,3,0,3,0,2,1,0,0,2,2,0,1,3,0,0,1,1,1,1,3,0,3,0,1,1,3,3,1,3,2,1,0,3,0,0,3,2,0,0,3,3,0,0,1,3,0,2,0,0,0,3,0,0,1,3,3,3,2,1,0,0,2,2,0,2,0,2,1,1,0,0,0,2,1,3,0,0,2,0,1,1,1,1,1,2,0,0,0,1,1,0,2,0,1,2,0,2,1,3,0,1,1,3,0,0,2,0,0,1,0,2,1,3,0,0,0,0,2,0,2,1,0,1,0,1,1,1,2,3,1,3,0,3,2,3,0,2,1,0,0,0,0,3,0,2,3,2,2,2,0,0,2,0,3,3,3,0,3,0,2,2,3,0,2,0,2,2,1,2,0,1,0,0,0,1,1,3,0,1,1,2,0,2,1,1,3,2,2,3,2,0,3,0,2,1,3,2,2,3,3,2,3,1,1,0,0,2,0,3,0,2,0,0,3,1,3,3,0,2,3,3,1,0,0,1,3,3,3,0,0,0,3,3,3,2,1,1,1,0,1,0,2,0,0,1,1,1,3,1,3,0,0,0,3,1,1,1,1,3,3,2,3,0,0,0,3,3,3,0,0,1,3,2,3,3,0,2,3,1,1,0,0,0,2,0,2,2,0,0,1,0,2,1,3,1,3,3,3,2,2,0,1,0,1,3,0,2,2,0,0,0,0,0,0,1,1,3,3,2,3,0,2,0,2,0,2,0,2,3,0,0,0,0,0,0,3,3,3,0,0,1,0,1,1,1,0,3,0,2,3,0,2,2,1,1,3,0,0,0,0,2,1,0,2,1,1,0,1,1,0,0,3,3,0,0,2,0,0,0,2,1,2,3,3,1,0,0,2,1,3,1,0,0,1,0,1,1,1,0,1,3,0,1,1,3,0,0,0,0,0,0,3,1,1,1,0,0,0,1,0,3,0,3,0,0,1,3,2,0,0,1,3,1,1,3,1,0,1,0,1,1,1,0,0,3,3,2,2,0,1,1,0,0,3,1,3,0,3,1,0,1,1,1,3,0,3,0,2,0,0,2,0,0,1,3,0,0,3,2,3,3,0,2,3,0,3,0,0,2,3,0,0,1,0,3,2,0,0,0,0,1,0,3,3,1,3,1,1,3,1,1,2,1,0,3,0,0,2,1,1,3,2,1,2,3,1,0,2,0,3,1,0,0,0,0,1,0,1,3,2,0,0,1,3,2,0,1,0,0,3,3,0,0,1,0,2,1,1,1,0,0,3,0,3,1,3,0,1,0,0,3,1,0,0,1,1,0,0,1,0,0,2,3,1,0,3,3,0,3,3,0,1,1,1,3,1,0,1,3,2,3,1,0,0,1,1,1,0,0,1,0,1,0,2,2,1,0,3,2,1,3,1,0,3,0,0,2,2,0,0,0,2,2,3,3,0,0,0,0,0,0,0,2,3,0,0,0,0,2,2,0,0,1,3,1,2,2,1,0,0,0,1,1,3,3,0,1,1,1,1,2,1,1,3,2,3,3,3,0,1,1,0,0,0,0,0,1,0,3,1,0,1,1,3,1,3,0,2,1,0,3,1,0,1,1,0,2,3,0,3,3,0,2,0,2,2,1,0,1,1,2,1,1,3,2,1,1,1,0,2,3,2,0,1,0,1,0,3,2,3,3,3,0,0,1,2,2,1,1,2,1,2,2,3,0,1,1,1,3,0,0,1,1,2,3,2,1,0,0,0,2,2,3,0,2,1,0,3,0,0,3,1,0,1,3,3,2,3,3,1,1,3,3,0,0,0,3,0,2,2,2,0,1,1,3,2,3,0,3,2,0,0,3,2,2,1,3,1,1,0,1,2,0,2,2,2,3,3,1,0,2,1,3,2,3,1,3,1,3,3,0,1,3,3,3,3,0,0,1,1,0,2,3,2,0,0,0,3,3,2,0,1,1,3,2,1,1,1,2,3,2,0,0,2,0,2,2,1,2,2,2,1,0,3,2,0,1,0,1,0,2,1,0,0,2,0,1,2,0,2,0,0,2,0,1,1,1,3,0,3,2,2,0,2,1,3,3,3,0,0,3,3,3,0,3,3,0,0,3,2,1,0,0,0,1,0,2,3,0,1,1,3,0,0,1,0,0,0,1,1,1,0,1,0,2,2,3,1,1,3,0,0,0,1,3,0,1,1,0,0,0,1,1,3,2,1,0,3,3,0,0,0,0,0,3,3,3,1,2,2,3,3,2,2,2,2,1,2,0,1,1,3,1,2,2,0,2,1,0,2,0,0,1,1,1,0,0,1,1,3,1,1,2,0,2,1,0,2,3,0,1,0,3,2,1,3,0,0,2,0,1,3,3,1,0,1,1,0,2,3,1,0,0,0,2,1,2,0,0,1,3,0,1,3,0,3,0,1,3,1,0,0,3,3,2,0,3,1,1,0,0,3,0,0,1,3,3,2,0,1,1,0,0,1,2,2,0,0,1,0,0,2,3,3,0,1,1,1,3,0,2,2,2,0,3,0,0,1,0,2,1,2,1,0,0,3,1,1,3,0,3,3,1,3,0,2,0,2,3,1,1,0,3,0,3,1,0,0,1,0,0,3,0,2,2,2,3,3,3,0,1,2,0,1,1,3,1,2,0,3,2,3,3,2,2,0,3,1,0,2,2,0,1,0,3,1,1,1,2,0,3,2,2,3,2,1,0,2,1,1,2,1,3,0,3,3,0,0,0,2,2,3,3,1,2,3,3,3,2,3,3,1,0,0,1,2,0,3,3,0,0,0,2,3,1,1,3,0,1,2,3,2,0,3,1,3,2,0,2,3,3,1,0,2,0,1,1,2,2,0,2,3,0,0,3,1,1,0,2,2,3,1,2,2,3,3,3,1,3,0,3,1,3,0,1,3,3,1,0,0,0,3,3,1,1,3,1,1,1,3,2,3,0,1,2,0,0,0,2,2,0,1,0,0,2,0,2,0,0,0,3,0,0,2,2,1,1,3,0,1,3,3,1,0,1,0,0,0,2,1,2,1,1,3,3,1,1,1,1,1,2,3,0,0,0,3,2,0,3,0,3,1,0,3,1,3,1,0,0,1,3,3,0,2,3,0,3,3,0,3,0,1,1,1,0,1,0,1,1,1,0,1,1,1,0,0,2,0,0,1,0,2,2,2,3,3,3,2,3,3,0,0,2,0,3,2,2,1,0,2,0,2,1,1,1,2,2,3,0,0,3,1,2,1,0,3,0,0,0,0,1,3,3,0,0,0,0,1,3,3,3,0,1,0,2,3,1,0,2,0,2,2,3,3,1,0,0,3,3,1,1,3,1,3,3,1,3,3,0,0,1,0,0,1,0,3,0,1,1,1,0,3,2,2,1,1,0,0,1,1,3,1,1,3,0,1,3,1,1,3,1,0,3,3,2,3,0,1,1,1,0,3,3,1,3,0,0,3,1,2,1,0,0,3,2,2,1,0,3,3,1,1,3,0,0,3,2,1,3,3,0,1,1,2,0,0,1,2,0,0,0,0,0,3,3,1,3,0,2,2,1,3,0,3,0,3,0,1,0,0,1,3,0,1,2,1,0,0,0,2,2,1,1,1,1,0,0,1,2,3,3,2,3,0,2,2,1,1,1,1,3,0,1,2,2,2,1,3,0,1,3,0,1,0,0,1,1,1,3,3,1,2,1,3,2,0,1,2,1,1,0,3,0,0,0,0,1,3,1,3,3,1,0,1,1,0,0,0,2,0,2,1,1,1,1,3,0,0,0,0,1,1,1,2,1,1,0,1,0,3,1,3,0,1,1,0,3,1,0,1,1,1,3,1,3,0,1,0,3,1,0,1,1,2,1,1,1,1,2,0,1,1,3,3,0,2,1,3,1,3,1,0,1,1,0,3,1,2,1,3,1,3,3,1,3,0,1,3,0,3,2,0,0,1,1,1,1,1,1,3,1,1,1,1,0,3,0,1,1,1,0,0,1,1,1,1,1,3,2,2,3,1,0,0,1,1,3,1,0,0,1,1,3,0,2,2,1,1,3,1,1,3,0,3,3,3,0,3,3,1,3,0,2,1,1,0,1,1,3,1,3,0,2,1,1,3,0,2,1,1,2,3,3,3,0,1,3,1,0,0,3,1,1,3,1,3,2,0,3,1,0,2,2,2,3,2,0,2,1,0,3,1,0,0,0,1,3,1,0,0,0,1,3,0,1,2,1,1,1,3,2,0,3,1,2,2,1,2,1,0,1,3,2,1,2,0,2,1,0,2,3,0,2,1,1,1,0,0,0,1,0,0,3,1,3,1,0,3,0,3,2,0,0,2,3,1,0,1,1,1,3,0,2,1,1,0,3,1,0,3,3,1,3,0,1,3,0,3,1,0,0,1,0,3,3,0,1,3,0,0,3,0,0,2,3,2,2,1,3,1,1,3,3,3,0,0,1,1,3,1,3,1,1,0,1,1,1,3,3,0,3,1,0,1,0,0,1,0,1,0,0,2,0,0,1,0,1,1,3,1,3,2,0,3,3,0,1,3,1,1,3,2,1,1,0,3,1,0,3,2,0,1,1,1,3,3,2,2,1,1,0,3,0,0,3,0,3,2,0,3,3,3,0,3,1,3,1,1,0,1,0,1,3,0,2,1,0,2,0,2,0,1,1,0,0,1,1,2,0,0,1,1,1,1,1,3,3,1,2,0,1,1,3,3,2,1,1,2,0,0,2,2,2,2,0,2,3,1,1,2,0,0,1,3,0,2,3,1,3,1,0,2,2,1,3,3,1,0,0,1,0,3,1,2,0,0,3,0,1,2,1,1,2,1,0,2,2,1,1,1,1,3,3,1,2,1,1,1,3,0,3,3,1,3,3,1,0,3,0,2,1,1,2,0,0,3,0,1,0,1,0,0,0,1,0,3,3,0,3,3,0,3,0,0,3,0,0,0,1,0,1,1,1,3,1,0,1,1,0,1,3,0,1,0,0,3,1,3,3,1,1,3,0,2,2,0,0,1,0,0,1,0,3,0,3,2,0,1,2,1,0,1,3,1,3,1,1,1,1,3,2,0,0,1,3,1,3,0,1,0,1,0,0,1,0,3,0,3,3,3,3,2,3,1,0,1,1,0,0,2,0,1,1,1,3,0,1,3,3,1,3,0,0,1,1,3,1,1,1,3,2,3,3,1,3,3,0,3,2,0,0,3,3,1,2,0,0,1,0,2,1,0,3,0,1,1,1,1,1,2,0,3,3,1,1,2,1,3,0,1,2,0,1,1,0,0,1,3,1,0,3,0,1,0,1,1,3,1,1,3,0,3,2,0,0,0,0,0,0,1,3,3,1,1,3,0,1,1,0,1,3,1,0,1,1,1,3,0,2,1,0,3,3,0,1,3,3,0,3,0,3,2,0,3,0,3,2,3,1,3,1,1,0,3,0,1,1,1,0,3,3,0,1,0,0,3,1,3,1,1,0,2,1,0,3,3,1,1,1,1,1,3,1,0,0,0,1,1,3,0,0,2,0,0,0,3,0,3,2,3,1,3,2,0,3,0,0,0,0,2,0,2,3,3,0,1,3,3,3,2,0,3,0,2,0,2,3,0,0,0,3,0,0,3,0,2,2,0,2,1,3,3,0,0,0,1,1,1,1,1,3,3,0,3,3,3,1,3,0,2,2,0,1,3,0,3,2,0,2,0,0,3,1,2,0,0,1,1,1,0,3,1,1,1,3,2,0,2,0,0,3,1,1,0,0,0,0,3,3,1,3,1,1,2,3,2,1,1,0,1,1,3,0,3,1,0,1,0,1,1,1,1,0,3,1,1,3,0,0,0,2,3,0,0,2,2,3,1,0,2,1,3,0,0,0,3,0,0,2,1,3,0,3,1,2,2,2,1,1,1,0,3,0,1,1,1,1,2,0,0,0,0,3,2,3,3,2,2,3,3,0,3,0,1,1,1,3,3,1,1,1,2,3,0,1,3,0,0,3,3,0,0,3,1,1,1,1,3,2,2,1,1,1,0,0,1,1,1,2,3,1,0,3,1,3,0,1,3,1,3,0,1,1,0,3,1,3,3,3,2,1,0,2,2,1,0,1,0,1,3,1,0,3,1,0,1,0,2,1,2,1,3,0,0,2,1,3,1,2,1,0,1,3,2,0,3,3,3,3,3,3,0,1,1,3,2,0,2,3,0,2,2,1,1,3,0,2,0,0,0,3,0,0,0,1,0,3,2,1,3,0,2,1,3,3,3,3,0,3,3,1,1,0,2,3,3,1,3,0,0,1,1,0,0,0,0,0,0,0,3,0,0,0,1,1,1,3,1,2,3,3,1,1,0,1,0,2,0,0,2,1,3,2,1,1,0,3,1,0,0,2,3,0,3,3,3,1,1,3,1,0,1,2,1,0,0,2,1,0,0,1,1,2,1,0,3,1,1,0,3,0,0,3,1,1,3,3,1,3,0,0,3,0,2,1,3,0,3,1,1,3,1,3,3,1,0,0,1,0,0,3,0,3,0,1,3,1,3,1,1,2,2,0,1,0,0,3,2,0,0,1,1,0,3,0,0,1,1,0,0,3,0,1,3,0,1,1,0,0,3,1,0,0,3,0,1,3,1,0,3,1,0,3,3,0,0,3,0,0,3,1,0,3,0,0,3,2,2,1,3,0,3,0,2,1,0,0,3,0,0,0,0,1,3,0,2,2,0,0,3,0,2,1,1,1,1,1,3,3,3,1,0,1,3,3,1,3,2,0,2,3,1,1,1,0,2,0,2,2,3,3,0,1,1,1,0,0,2,2,1,0,1,1,1,1,3,1,3,2,0,1,0,3,1,1,2,2,1,1,3,2,1,3,3,1,3,3,1,3,1,0,1,0,3,2,0,1,0,0,0,0,0,1,3,0,2,1,1,1,1,1,0,3,1,3,1,0,0,3,1,0,3,0,3,0,1,1,0,0,0,3,1,3,1,3,1,1,1,3,1,0,1,3,0,0,0,1,2,3,0,0,2,1,1,3,3,1,3,1,1,3,1,0,1,3,1,3,1,3,1,0,0,3,1,3,3,0,3,1,1,0,3,1,0,3,0,2,1,0,2,2,1,0,2,3,3,2,0,2,2,3,2,2,0,3,3,0,0,0,1,1,0,0,0,1,1,1,0,2,1,3,0,1,2,1,0,0,0,0,3,1,3,3,0,2,1,0,3,0,1,3,1,1,3,1,0,0,3,3,0,1,1,1,0,1,0,3,0,2,2,0,3,2,0,0,3,0,0,3,0,2,1,0,2,3,3,1,3,0,1,1,2,3,0,1,0,0,1,1,1,3,0,0,1,0,3,0,0,1,1,0,3,3,1,3,3,0,0,3,3,3,0,0,1,3,0,3,3,3,0,3,0,3,3,0,3,1,1,3,0,0,1,3,0,1,3,0,1,1,2,1,0,3,3,1,1,3,0,1,3,0,1,3,1,0,0,1,3,3,0,0,0,1,3,1,1,0,2,1,0,1,1,0,1,2,0,1,1,1,3,0,1,3,0,1,3,0,3,1,3,1,2,1,0,1,1,3,2,0,0,0,1,0,0,2,1,3,0,0,1,0,3,2,0,1,3,0,0,1,0,1,1,1,3,3,0,0,3,3,1,1,0,3,1,1,0,1,1,1,3,1,1,3,1,3,1,1,1,3,0,2,2,0,2,2,1,1,3,2,1,1,1,1,1,2,1,3,0,0,1,1,2,2,1,3,3,3,3,3,2,1,1,1,0,0,0,3,2,2,2,1,1,0,3,3,0,3,1,2,0,0,2,0,0,3,3,1,0,1,0,0,0,0,0,0,1,0,0,3,0,2,1,1,3,1,0,3,1,0,3,1,1,1,1,0,1,1,0,3,1,0,3,0,2,1,1,0,1,1,0,3,1,0,1,1,1,3,1,1,3,3,0,0,1,1,3,1,3,0,1,3,3,1,3,0,1,1,3,0,1,2,1,1,3,0,0,3,1,3,0,1,3,1,1,0,1,1,3,1,0,0,3,1,0,1,0,1,3,0,1,3,1,1,1,1,0,3,0,3,1,3,0,0,1,0,0,1,2,3,0,0,0,0,0,3,0,0,0,0,3,2,0,1,0,2,3,3,3,2,0,0,1,0,3,0,1,0,0,0,0,1,1,1,0,1,1,1,1,0,3,3,1,1,3,1,1,1,1,0,1,0,1,3,1,0,3,1,2,1,1,1,3,3,0,1,1,0,1,2,1,3,0,1,3,1,1,3,0,1,1,3,0,3,1,3,1,1,1,1,3,3,3,3,0,3,0,1,3,0,0,3,0,0,3,1,3,3,0,3,0,2,0,0,0,3,3,3,0,2,2,3,3,0,0,0,3,0,1,0,2,0,1,1,0,0,2,0,2,1,1,3,3,1,0,0,0,2,1,1,1,3,1,0,2,3,0,0,2,3,3,2,1,0,0,3,0,1,3,3,0,0,3,3,3,1,3,2,1,0,0,1,0,2,1,3,0,0,2,2,0,1,3,2,1,0,0,0,0,1,1,1,1,0,1,3,1,3,2,1,0,3,1,0,0,1,3,2,0,0,1,2,1,0,0,0,3,1,0,2,1,1,0,1,3,3,3,0,0,3,3,0,0,2,1,3,0,0,2,1,1,1,3,3,0,1,3,0,2,0,1,1,0,0,3,2,2,2,0,1,3,3,0,0,0,1,1,1,0,1,0,0,0,1,0,1,3,3,0,2,3,3,0,0,1,0,2,1,3,0,0,2,1,0,1,1,1,3,0,0,3,1,0,0,1,3,2,2,1,3,3,1,0,0,3,1,3,0,1,3,3,1,3,1,1,1,2,1,1,2,1,1,2,2,2,0,0,0,0,0,0,2,2,1,2,2,2,0,2,0,0,2,1,1,1,1,2,2,1,0,2,2,3,3,3,2,0,0,2,1,3,2,1,3,3,1,3,3,1,2,0,0,3,3,3,2,1,0,0,3,3,1,0,0,3,0,3,2,0,0,0,0,3,1,0,1,1,3,1,2,2,0,2,1,3,2,2,3,0,0,0,0,0,2,0,2,2,1,1,3,0,0,1,1,1,1,3,2,3,1,3,3,3,0,2,0,3,3,3,0,1,0,2,3,1,1,0,0,3,2,1,3,3,1,0,1,3,1,0,2,1,1,0,3,3,3,3,0,1,1,3,1,0,1,1,1,1,1,0,1,3,2,0,3,2,3,3,1,2,1,1,2,0,1,1,2,3,3,2,0,1,3,0,3,3,1,3,1,3,0,1,0,0,0,1,1,0,1,0,0,0,2,0,1,0,3,3,2,2,0,0,1,0,1,3,0,3,0,1,1,3,0,3,3,0,3,3,1,2,2,1,2,1,0,3,2,0,2,1,3,2,2,0,2,3,1,1,3,0,2,2,1,0,1,0,2,1,3,1,3,0,0,2,1,1,3,1,1,3,3,0,3,3,1,2,0,2,1,1,2,0,2,1,3,2,2,2,1,1,0,2,1,1,0,2,2,1,0,0,1,1,3,3,1,3,0,2,2,3,0,0,1,2,0,1,1,0,1,0,3,1,3,0,1,0,0,1,2,3,3,0,3,1,2,3,1,0,1,0,2,1,1,1,0,3,2,1,0,3,3,3,2,3,0,0,3,0,0,3,1,3,3,1,3,3,1,0,3,0,2,3,0,0,3,0,1,1,1,0,3,1,0,3,0,0,3,1,2,2,0,2,2,1,3,3,3,2,2,1,0,0,1,3,2,0,1,3,0,2,3,3,1,1,1,1,3,0,0,3,0,0,3,1,2,2,3,2,1,1,1,1,1,2,0,3,0,3,2,2,1,2,3,3,3,1,1,1,1,2,1,0,3,0,0,0,1,0,0,1,0,3,0,0,2,1,3,3,1,3,2,0,1,3,1,3,3,0,1,1,3,1,1,1,3,1,3,1,3,1,1,3,0,1,3,1,1,3,2,1,3,1,2,1,0,3,1,3,2,1,3,0,3,0,2,3,2,2,0,2,2,1,1,2,2,0,2,1,0,2,2,0,0,1,0,2,2,3,3,2,0,0,1,0,2,3,1,3,0,1,1,1,3,1,1,1,3,3,0,2,1,0,2,2,2,0,0,1,3,0,1,3,1,1,1,0,1,1,1,3,2,2,0,2,1,1,3,1,1,2,3,0,2,0,1,1,3,0,0,1,1,0,3,1,3,3,1,3,1,1,3,3,0,1,0,1,1,3,0,2,1,0,2,2,3,2,3,1,3,1,1,3,1,3,0,3,1,3,3,0,2,2,2,2,1,1,0,3,1,0,0,3,3,3,1,0,3,1,0,1,0,0,1,0,0,3,3,0,3,1,0,0,3,0,3,0,0,0,0,1,1,1,1,1,3,2,1,1,0,3,0,0,1,1,1,0,0,3,0,1,1,0,0,0,1,2,1,1,1,1,3,1,3,3,1,2,3,1,3,2,0,3,1,1,2,3,1,1,3,0,0,3,1,0,1,0,2,1,0,2,3,1,1,3,0,1,3,3,1,3,1,1,3,0,3,1,3,1,3,1,1,1,0,2,3,1,1,3,0,2,1,3,2,1,3,2,2,1,0,3,1,0,1,3,0,3,0,1,3,0,1,3,0,0,1,0,2,0,1,1,2,1,0,0,1,1,3,1,0,0,1,0,1,1,0,1,1,3,3,1,3,3,1,2,0,1,1,1,1,2,1,1,2,2,0,2,2,0,2,2,0,2,0,1,1,1,1,0,3,3,1,3,0,3,0,1,1,0,0,1,0,1,1,3,0,3,3,1,3,2,0,3,3,3,3,3,1,2,2,3,1,0,1,1,1,3,2,0,0,2,3,3,3,0,3,0,3,3,1,3,3,0,3,1,1,3,0,1,1,0,2,2,1,3,3,1,2,2,0,0,3,0,0,3,1,3,1,1,1,0,3,0,3,3,2,3,0,0,1,3,3,0,1,3,0,1,3,1,1,2,2,0,0,0,0,0,0,0,2,0,0,1,1,0,3,3,3,2,2,0,3,0,1,0,3,0,2,2,3,0,3,2,2,3,1,3,2,0,2,1,3,0,3,2,0,3,0,3,1,0,0,3,3,2,2,1,3,3,1,1,3,0,2,2,2,3,3,3,0,3,1,2,3,2,3,2,0,2,1,0,1,0,1,1,0,3,0,3,0,3,3,3,0,1,0,2,3,0,2,2,0,0,3,0,2,0,1,2,3,0,2,0,1,0,1,0,1,2,0,2,1,0,3,0,3,3,3,1,0,1,1,3,1,1,2,1,3,0,1,1,0,3,0,0,3,1,0,3,1,2,1,3,0,3,1,1,1,1,0,1,1,2,2,1,2,3,1,0,0,0,2,3,0,3,3,3,0,2,1,3,2,0,1,3,1,2,1,1,0,1,0,1,3,1,1,0,1,2,2,0,0,2,1,0,0,3,0,3,2,0,0,0,3,2,0,3,1,3,2,1,3,2,1,0,2,3,2,1,3,1,3,2,0,2,1,1,1,3,0,2,2,0,3,3,1,0,3,1,3,3,3,1,3,3,3,3,1,0,1,1,2,3,0,2,2,3,2,2,1,1,3,2,0,1,3,2,2,1,0,3,3,2,3,0,3,3,0,2,1,0,0,0,1,3,1,0,3,1,0,1,3,0,2,0,1,0,3,1,2,3,0,1,3,0,1,0,1,2,0,1,0,1,2,3,0,1,3,0,1,2,3,3,2,3,0,2,1,3,1,0,1,3,3,1,1,0,1,3,0,3,2,3,1,1,3,0,3,1,0,0,3,0,2,2,0,2,1,3,2,3,0,3,3,3,2,1,1,0,3,1,0,3,0,2,2,0,2,2,1,3,3,1,0,3,3,1,0,1,3,2,0,3,3,3,1,1,1,1,3,0,3,3,1,3,1,0,2,2,1,3,0,1,0,1,1,1,3,0,2,0,1,1,0,0,0,1,1,3,0,1,2,1,1,0,0,0,0,3,1,1,0,3,3,3,1,0,1,3,0,3,1,0,3,0,3,3,1,0,3,1,2,2,1,2,3,0,0,0,3,1,3,0,0,1,3,3,3,1,3,3,1,1,1,0,1,0,0,1,0,1,3,3,3,1,3,1,2,2,1,1,3,0,3,1,1,2,2,0,0,3,2,1,1,1,1,2,0,1,2,3,3,0,1,3,1,2,2,0,1,3,0,1,1,1,1,2,0,3,2,1,0,3,0,1,0,1,1,0,1,0,3,2,0,0,0,1,0,3,1,1,3,0,3,1,0,3,1,3,2,3,0,2,2,1,3,1,0,3,3,1,0,3,3,3,1,3,1,3,0,0,1,0,2,1,0,2,3,0,0,3,0,3,3,0,0,3,0,0,3,3,3,3,1,0,3,2,0,3,3,3,2,0,2,0,0,2,1,1,3,3,1,2,1,3,3,1,2,0,0,2,1,2,0,0,0,0,2,3,1,1,3,0,0,3,0,2,3,0,2,0,0,2,0,0,1,1,1,3,1,1,0,3,0,0,0,1,1,3,2,2,0,2,3,2,0,1,3,0,3,0,3,2,2,0,3,2,1,1,1,1,1,1,0,1,1,1,3,0,1,1,0,1,0,1,0,3,3,1,2,0,0,2,0,0,1,1,1,2,3,0,3,0,1,0,3,0,0,0,0,3,1,3,0,2,0,1,0,0,0,0,0,0,2,2,0,0,2,2,0,0,3,1,2,0,0,1,1,1,1,1,1,0,0,0,2,1,3,2,2,3,3,3,1,0,0,2,1,1,0,0,1,1,1,1,0,3,2,2,1,1,3,1,1,0,3,2,0,1,3,3,3,3,3,1,0,0,0,0,0,2,2,3,0,3,3,0,2,0,0,0,0,0,1,1,0,3,3,3,1,0,3,0,0,1,3,3,3,2,3,1,0,0,0,2,3,3,0,0,0,3,3,0,3,0,2,2,1,3,0,0,0,3,1,1,3,0,3,0,3,0,3,1,3,3,0,0,3,2,2,1,0,1,0,3,2,1,0,2,1,2,1,0,0,2,3,0,2,2,3,1,3,0,1,0,0,2,0,1,2,1,3,0,1,3,3,1,1,1,1,3,0,3,1,0,3,0,2,0,0,2,0,2,1,3,3,0,3,1,0,1,1,3,3,3,1,0,3,2,0,3,1,0,1,2,1,1,1,3,1,0,3,0,0,3,1,0,3,3,3,3,1,1,3,3,0,3,1,3,2,1,3,3,1,1,3,0,2,3,1,1,3,2,3,0,3,2,1,1,1,3,3,3,3,1,1,3,0,0,1,0,1,3,1,0,1,0,0,1,0,0,0,0,1,3,0,0,1,3,0,0,3,0,1,3,0,0,1,0,3,1,3,1,0,2,0,1,2,1,3,1,0,2,2,0,0,0,3,0,2,0,0,0,1,1,2,3,1,3,2,0,0,1,3,0,3,1,1,3,2,1,1,1,2,1,1,0,3,1,0,3,1,1,3,0,2,3,1,1,3,1,0,3,1,2,1,1,1,3,1,1,1,0,3,1,1,1,3,0,1,2,1,0,3,1,1,3,3,3,0,1,0,3,0,0,1,0,2,0,1,2,0,2,2,3,1,0,0,1,2,0,3,1,1,1,3,1,1,1,3,3,0,1,1,0,3,1,0,0,0,3,1,0,0,3,3,2,2,1,1,0,1,1,0,0,3,2,2,3,0,1,3,2,0,0,1,1,3,0,1,2,0,2,3,0,1,0,1,1,2,0,1,3,0,1,2,2,1,2,2,0,1,3,0,0,3,1,3,3,1,0,0,1,3,1,1,3,0,1,0,3,0,1,3,3,1,1,1,1,1,0,3,3,0,3,3,1,1,3,0,2,0,0,1,1,0,2,2,1,2,0,1,1,3,2,1,2,0,1,3,1,1,3,3,2,0,1,2,3,3,2,0,1,0,0,3,1,2,0,2,3,0,2,3,0,1,3,1,1,1,2,0,3,3,2,0,0,2,1,1,1,1,1,0,3,3,1,2,3,0,3,0,0,3,0,0,3,3,0,1,0,3,1,0,1,0,0,2,0,1,2,3,1,3,3,2,1,0,1,3,1,0,3,2,0,2,1,3,2,3,1,1,1,1,0,1,0,3,3,0,2,2,1,3,3,0,0,0,0,0,1,0,2,0,3,2,1,0,0,3,3,1,1,1,2,2,0,1,2,3,1,3,0,0,0,1,1,0,0,0,1,1,0,1,3,3,3,1,0,1,1,2,1,3,0,1,0,1,2,0,1,1,2,2,2,2,2,3,0,3,0,1,3,0,1,2,2,3,1,0,0,3,2,1,3,1,3,2,0,0,0,3,1,3,2,3,2,2,0,2,1,0,0,0,1,1,0,1,0,2,3,3,3,1,0,3,2,1,1,1,0,3,1,2,3,1,1,3,0,2,0,0,3,3,0,0,3,3,1,1,1,1,3,0,0,0,0,0,3,1,3,3,3,2,0,0,0,3,0,2,2,2,1,1,1,2,3,0,3,3,3,0,1,1,1,3,0,3,0,2,1,0,1,1,1,1,1,3,1,3,0,1,1,1,1,1,3,1,3,0,2,0,2,1,1,1,0,1,3,2,3,0,0,0,2,1,3,0,0,1,3,3,0,2,1,0,3,3,0,0,1,1,3,3,3,3,0,0,2,3,3,0,0,0,2,0,3,3,0,0,2,0,2,0,0,1,1,0,0,1,0,1,1,3,1,3,3,3,0,1,0,2,3,2,0,0,0,3,2,1,1,1,1,0,0,1,3,0,0,0,3,0,1,3,0,1,1,2,3,0,3,2,2,1,1,1,0,1,1,0,3,0,0,3,3,0,1,1,1,1,1,0,3,0,1,3,1,1,3,3,0,1,0,1,3,0,3,3,1,1,3,1,0,3,1,0,1,1,1,0,0,1,3,0,0,0,0,0,3,0,3,3,0,0,0,1,0,1,0,0,0,1,3,0,1,1,0,1,1,3,0,1,1,3,1,1,1,3,1,0,1,1,0,0,0,2,1,1,1,0,3,0,0,0,0,0,3,0,0,0,0,0,0,3,3,0,3,0,0,1,0,0,0,1,1,1,3,2,0,2,0,0,1,1,0,0,0,0,3,2,0,0,1,2,0,0,0,0,3,1,3,2,3,3,1,2,1,3,3,1,0,3,3,1,0,3,3,2,1,1,1,1,1,0,1,0,0,3,1,1,3,0,2,2,1,1,3,0,1,1,1,2,1,1,2,1,0,2,3,0,1,3,2,0,3,1,0,3,3,1,3,0,3,3,3,1,1,1,1,1,3,1,3,0,3,3,2,0,3,1,1,1,1,0,1,1,3,1,1,0,0,0,3,0,3,1,3,1,0,3,1,0,0,1,0,0,1,1,2,0,1,3,0,0,3,1,0,1,1,0,1,1,1,0,0,1,0,0,3,2,0,1,3,0,0,3,1,0,0,0,1,3,0,0,1,1,3,1,0,0,0,0,1,0,0,0,3,2,0,3,0,2,1,1,0,3,0,1,0,1,0,0,1,0,1,3,0,0,0,2,2,0,1,2,0,0,1,1,3,2,0,3,1,3,1,3,3,0,3,0,1,3,0,2,3,0,3,1,1,3,3,0,0,3,1,0,3,3,3,3,3,0,3,3,2,1,1,0,1,0,0,1,3,0,0,1,1,3,1,1,3,1,2,2,0,1,3,1,1,3,2,1,1,3,1,0,1,3,1,0,3,3,3,0,1,0,1,1,0,0,1,1,0,1,1,1,0,0,1,3,0,3,1,3,0,3,0,0,0,1,1,3,0,2,1,1,0,3,2,2,1,1,0,3,1,1,1,1,3,3,0,3,2,0,2,1,2,2,2,1,2,1,0,2,3,2,0,3,3,0,3,0,2,2,1,3,3,3,1,2,1,3,1,3,0,0,2,0,3,3,0,0,0,0,0,3,2,1,1,1,3,0,2,1,1,1,0,1,3,3,1,3,3,0,1,1,0,1,0,0,2,2,1,0,1,0,1,1,3,0,1,0,1,1,1,1,3,3,0,3,1,1,1,1,0,3,0,1,3,0,2,3,3,0,3,3,0,3,1,2,0,0,0,1,1,0,3,1,0,2,1,1,3,0,1,3,1,0,3,3,1,0,0,1,1,0,0,3,0,2,1,1,1,3,2,2,1,1,2,3,0,1,2,1,1,3,0,0,1,1,2,1,3,0,0,1,0,3,3,0,1,3,2,1,0,2,2,1,1,0,1,1,3,0,1,3,1,0,3,2,1,0,1,1,3,0,0,3,3,2,2,0,0,2,1,2,1,1,0,1,1,1,3,0,2,1,0,0,3,0,3,1,0,0,1,1,0,3,3,0,0,1,1,3,3,1,1,1,3,1,3,0,1,0,1,3,3,0,3,1,0,3,1,3,3,1,0,1,0,0,3,3,1,3,0,0,3,3,1,3,0,1,3,2,0,1,3,0,3,1,1,3,0,2,0,0,0,3,1,2,1,3,2,3,1,2,1,1,3,3,0,0,3,1,1,0,0,2,1,1,3,0,1,2,3,3,3,3,1,0,1,0,1,3,3,1,3,0,2,3,0,0,2,1,1,3,1,3,0,1,1,3,2,1,0,1,2,0,1,0,0,1,0,1,0,3,0,0,3,2,0,1,1,1,0,1,1,0,0,3,1,0,1,0,3,2,1,1,3,0,3,1,0,3,0,3,0,2,3,0,0,0,0,1,1,1,0,2,1,1,1,0,3,2,0,1,1,1,1,3,0,0,1,0,2,2,2,2,1,1,1,3,1,3,1,0,2,1,1,1,3,1,1,3,0,0,3,2,0,1,1,3,1,1,2,2,1,1,3,0,2,1,1,0,3,2,3,2,0,3,3,3,1,0,1,3,3,1,1,0,1,3,1,1,0,3,0,0,1,2,1,3,1,1,3,1,0,3,0,1,3,0,2,2,1,1,3,0,1,3,0,0,1,1,0,0,1,0,1,0,1,3,0,0,1,1,0,3,0,3,0,1,1,0,0,3,2,2,3,2,2,1,2,1,2,0,3,2,3,0,0,1,0,1,2,0,2,0,0,0,2,1,0,1,0,3,0,1,1,0,0,2,2,1,1,0,1,1,0,1,0,1,0,1,1,0,1,1,3,2,3,1,1,0,0,0,0,0,2,2,1,1,3,3,1,2,0,3,0,1,2,2,2,0,3,0,0,3,1,1,3,0,3,3,3,0,3,3,0,1,1,3,1,0,2,0,0,2,3,3,3,3,3,3,3,1,3,3,1,2,1,0,2,2,0,3,3,3,3,3,1,3,2,0,2,1,1,3,3,3,3,0,1,1,0,1,3,1,1,0,2,1,1,3,0,2,1,1,1,1,3,0,1,1,1,1,1,1,0,0,1,3,0,2,2,0,2,2,2,1,0,1,3,2,2,1,1,1,1,1,0,0,1,0,2,2,1,0,3,1,0,1,1,1,1,2,1,3,0,0,0,3,1,1,1,1,3,0,2,0,0,2,3,1,1,1,0,1,3,1,1,3,0,0,0,1,0,1,0,3,1,1,2,3,0,3,3,0,1,3,1,2,1,0,3,1,0,2,2,0,2,3,0,3,1,0,0,3,1,0,1,1,3,2,0,2,1,3,1,0,1,1,0,3,0,2,3,1,3,0,0,3,0,2,0,0,0,0,1,0,0,1,1,2,0,0,0,1,1,0,0,0,3,0,0,3,3,1,0,0,2,1,0,1,3,2,1,3,3,0,3,3,0,1,0,0,3,3,3,3,0,1,3,2,2,2,3,1,3,1,3,0,3,3,3,3,0,1,1,1,3,1,1,3,0,1,0,0,2,1,1,3,1,0,2,0,2,3,0,1,3,3,1,2,0,2,3,1,3,1,1,1,3,3,1,0,1,1,0,3,3,3,1,1,2,0,1,2,2,1,0,3,1,3,0,1,2,2,1,3,1,0,0,1,0,3,3,3,3,3,3,2,3,0,2,1,1,0,1,0,2,2,1,3,3,1,1,0,1,2,2,0,1,3,3,1,0,1,2,3,1,0,3,3,0,3,3,2,2,1,3,1,0,0,1,3,3,3,1,1,3,1,0,1,3,0,3,1,3,2,1,3,3,1,0,3,1,1,2,1,1,0,0,1,3,0,0,3,0,3,3,3,1,0,1,3,3,3,0,1,0,3,1,1,0,0,0,1,0,3,1,0,1,3,3,3,2,2,1,3,3,1,2,0,0,2,1,1,2,1,1,2,1,1,3,2,0,3,0,1,3,2,2,1,0,3,3,3,3,2,3,0,2,0,3,2,3,2,2,3,3,3,2,0,1,3,0,3,3,3,1,3,2,3,0,3,2,3,1,3,1,1,0,3,1,3,0,3,3,2,0,3,2,0,2,2,2,3,1,3,3,0,1,3,1,3,3,3,3,0,2,3,0,3,0,0,0,3,0,2,3,0,1,1,2,3,3,0,0,1,3,3,1,1,0,0,3,3,0,0,1,3,0,2,3,3,3,3,2,0,1,0,0,1,0,3,3,1,0,0,0,0,0,0,2,0,2,3,0,0,3,0,0,0,1,3,3,1,2,1,1,3,3,0,0,3,3,3,3,0,0,3,0,0,3,1,0,0,1,0,1,1,1,3,1,1,3,0,2,1,1,3,3,0,1,3,0,1,3,0,0,3,0,0,3,3,0,3,3,0,1,0,3,3,3,3,2,0,1,3,0,1,1,0,1,0,0,1,3,1,0,0,1,2,2,1,3,0,1,0,3,0,2,0,0,0,0,0,3,1,1,0,1,1,1,1,3,3,0,1,2,0,2,3,2,1,2,2,1,3,3,1,2,0,1,1,1,3,0,3,0,3,1,1,1,1,1,2,1,1,1,2,1,2,3,0,-1,-2};

      Sequences sequence = Sequences.createEmptySequencesInMemory();
      try {
         sequence.addSequence(ByteBuffer.wrap(s));
      } catch (IOException e) {
         e.printStackTrace();
      }

      final SuffixTrayBuilder stb = new SuffixTrayBuilder(sequence, alphabet, "suffix");
      stb.build("bothLR"); // WARNING: change the method and you must change the type cast in the
      // next line!
      assert (stb.getSuffixDLL() instanceof SuffixXorDLL);
      final SuffixXorDLL suffixDLL = (SuffixXorDLL) stb.getSuffixDLL(); // type cast is okay
      // because I used method
      // 'bothLR' to build the
      // list

      byte[] referenceBWT = bwt(s);
      BWTBuilder.BWT bwt = BWTBuilder.build(suffixDLL);

      assertArrayEquals(referenceBWT, bwt.bwt);
      checkSampledSuffixArray(bwt.bwt, bwt.sampledSuffixArray, sequence.array());

      suffixDLL.resetToBegin();
      int[] a = new int[suffixDLL.capacity()];
      int j = 0;
      if (suffixDLL.getCurrentPosition() != -1) {
         a[j++] = suffixDLL.getCurrentPosition();
         while (suffixDLL.hasNextUp()) {
            suffixDLL.nextUp();
            a[j++] = suffixDLL.getCurrentPosition();
         }
      }
      IntBuffer buf = IntBuffer.wrap(a);
      bwt = BWTBuilder.build(buf, sequence);
      assertArrayEquals(referenceBWT, bwt.bwt);
      checkSampledSuffixArray(bwt.bwt, bwt.sampledSuffixArray, sequence.array());
   }
   

   public static byte[] bwt(final byte[] seq) {
      byte[][] bwt_matrix = new byte[seq.length][];
      
      bwt_matrix[0] = Arrays.copyOf(seq, seq.length);
      for (int i = 1; i < bwt_matrix.length; i++) {
         bwt_matrix[i] = new byte[seq.length];
         int j;
         for(j = 0; j < seq.length-1; j++) {
            bwt_matrix[i][j] = bwt_matrix[i-1][j+1];
         }
         bwt_matrix[i][j] = bwt_matrix[i-1][0];
      }
      
      Arrays.sort(bwt_matrix, new ComparatorBySuffix());
      byte[] l = new byte[bwt_matrix.length];
      for (int i = 0; i < bwt_matrix.length; i++) {
         l[i] = bwt_matrix[i][bwt_matrix[i].length-1];
      }
      return l;
   }
   
   private static void checkSampledSuffixArray(byte[] bwt, int[] spos, byte[]sequence) {
      final int value = BWTBuilder.calculateBaseIndex(sequence.length);

//      System.out.println(value);
//      System.out.println(Arrays.toString(bwt));
//      System.out.println(Arrays.toString(spos));
//      System.out.println(Arrays.toString(sequence));
      
      for(int i = 0; i < spos.length; i++) {
         assertTrue(value*(i) < bwt.length);
         assertTrue(spos[i] < sequence.length);
         assertEquals(String.format("Iteration %d", i), bwt[value*(i)], sequence[spos[i]]);
      }
   }
   
   /**
    * Compares two byte arrays character by character lexicographically. Even special characters are
    * compared lexicographically.
    */
   static class ComparatorBySuffix implements Comparator<byte[]> {

      /**
       * Compares two byte arrays and calculates the lcp value (longest common prefix).<br>
       * If 0 is returned, both arrays are the same.<br>
       * If -i (a negative integer) is returned, the first arrays stands before the second in a
       * suffix array. The lcp value is '(i-1)'.<br>
       * If i (a positive integer) is returned, the first suffix stands behind the second in a suffix
       * array. The lcp value is '(i-1')'.<br>
       * Generally the lcp value can be received with ' i==0 ? o1.length : Math.abs(i)-1', where 'i'
       * is the return value.
       */
      @Override
      public int compare(byte[] o1, byte[] o2) {
         for (int i = 0; i < o1.length && i < o2.length; i++) {
            if (o1[i] < o2[i]) {
               return -(i + 1);
            } else if (o1[i] > o2[i]) {
               return (i + 1);
            }
         }

         if (o1.length < o2.length) {
            return -(o1.length + 1);
         } else if (o1.length > o2.length) {
            return (o2.length + 1);
         } else {
            return 0;
         }
      }

   }
}
