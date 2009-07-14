package verjinxer.sequenceanalysis;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import verjinxer.sequenceanalysis.alignment.BottomAndRightEdges;
import verjinxer.sequenceanalysis.alignment.IAligner;
import verjinxer.sequenceanalysis.alignment.SemiglobalAligner;
import verjinxer.sequenceanalysis.alignment.TopAndLeftEdges;

public class SemiglobalAlignerTest {
   
   public static final byte GAP = IAligner.GAP; 

   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
   }

   @AfterClass
   public static void tearDownAfterClass() throws Exception {
   }

   @Before
   public void setUp() throws Exception {
   }

   @After
   public void tearDown() throws Exception {
   }

   @Test
   public void testSemiglobalAlign() {
      byte[] s1 = {3,0,3,3,0}; // SISSI
      byte[] s2 = {1,0,3,3,0,3,3,0,2,2,0}; // MISSISSIPPI
      
      byte[] r1 = {GAP,GAP,GAP,3,0,3,3,0,GAP,GAP,GAP}; // [GAP, GAP, GAP, 'S', 'I', 'S', 'S', 'I', GAP, GAP, GAP]
      byte[] r2 = {1,0,3,3,0,3,3,0,2,2,0}; // [ 'M',  'I',  'S', 'S', 'I', 'S', 'S', 'I',  'P',  'P',  'I']
      
      int begin = 3;
      int length = 5;
      int error = 0;
      
      SemiglobalAligner aligner = new SemiglobalAligner();
      aligner.setBeginLocations(new TopAndLeftEdges());
      aligner.setEndLocations(new BottomAndRightEdges());
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2);
      
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getBegin(), begin);
      assertEquals(result.getLength(), length);
      assertEquals(result.getErrors(), error);
   }
      
   @Test
   public void testSemiglobalAlign1() {
      byte[] s1 = new byte[]{'S', 'I', 'S', 'S', 'I'};
      byte[] s2 = new byte[]{'M', 'I', 'S', 'S', 'I', 'S', 'S', 'I', 'P', 'P', 'I'};
      byte[] r1 = new byte[]{GAP, GAP, GAP, 'S', 'I', 'S', 'S', 'I', GAP, GAP, GAP};
      byte[] r2 = new byte[]{'M', 'I', 'S', 'S', 'I', 'S', 'S', 'I', 'P', 'P', 'I'};
      int error =  0 ;
      int length =  5 ;
      int begin =  3 ;
      
      SemiglobalAligner aligner = new SemiglobalAligner();
      aligner.setBeginLocations(new TopAndLeftEdges());
      aligner.setEndLocations(new BottomAndRightEdges());
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2);
      
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getBegin(), begin);
      assertEquals(result.getLength(), length);
      assertEquals(result.getErrors(), error);
   }


   @Test
   public void testSemiglobalAlign2() {
      byte[] s1 = new byte[]{' ', 'l', 'a', 'm', 'e', 'n'};
      byte[] s2 = new byte[]{'S', 'e', 'h', 'r', ' ', 'g', 'e', 'e', 'h', 'r', 't', 'e', ' ', 'D', 'a', 'm', 'e', 'n', ' ', 'u', 'n', 'd', ' ', 'h', 'e', 'r', 'r', 'e', 'n', '.', ' ', 'W', 'i', 'r', ' ', 'h', 'a', 'b', 'e', 'n', ' ', 'u', 'n', 's', ' ', 'h', 'e', 'u', 't', 'e', ' ', 'h', 'i', 'e', 'r', ' ', 'i', 'n', ' ', 'K', 'a', 'm', 'e', 'n', ' ', 'v', 'e', 'r', 's', 'a', 'm', 'm', 'e', 'l', 't', '.'};
      byte[] r1 = new byte[]{GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, ' ', 'l', 'a', 'm', 'e', 'n', GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP};
      byte[] r2 = new byte[]{'S', 'e', 'h', 'r', ' ', 'g', 'e', 'e', 'h', 'r', 't', 'e', ' ', 'D', 'a', 'm', 'e', 'n', ' ', 'u', 'n', 'd', ' ', 'h', 'e', 'r', 'r', 'e', 'n', '.', ' ', 'W', 'i', 'r', ' ', 'h', 'a', 'b', 'e', 'n', ' ', 'u', 'n', 's', ' ', 'h', 'e', 'u', 't', 'e', ' ', 'h', 'i', 'e', 'r', ' ', 'i', 'n', ' ', 'K', 'a', 'm', 'e', 'n', ' ', 'v', 'e', 'r', 's', 'a', 'm', 'm', 'e', 'l', 't', '.'};
      int error =  1 ;
      int length =  6 ;
      int begin =  58 ;

      SemiglobalAligner aligner = new SemiglobalAligner();
      aligner.setBeginLocations(new TopAndLeftEdges());
      aligner.setEndLocations(new BottomAndRightEdges());
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2);
      
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getBegin(), begin);
      assertEquals(result.getLength(), length);
      assertEquals(result.getErrors(), error);
   }


   @Test
   public void testSemiglobalAlign3() {
      byte[] s1 = new byte[]{'T', 'G', 'A', 'G', 'A', 'C', 'A', 'C', 'G', 'C', 'A', 'A', 'C', 'A', 'T', 'G', 'G', 'G', 'A', 'A', 'A', 'G', 'G', 'C', 'A', 'A', 'G', 'G', 'C', 'A', 'C', 'A', 'C', 'A', 'G', 'G', 'G', 'G', 'A', 'T', 'A', 'G', 'G'};
      byte[] s2 = new byte[]{'A', 'A', 'T', 'T', 'T', 'A', 'T', 'T', 'T', 'T', 'A', 'T', 'T', 'G', 'T', 'G', 'A', 'T', 'T', 'T', 'T', 'T', 'T', 'G', 'G', 'A', 'G', 'G', 'T', 'T', 'T', 'G', 'G', 'A', 'A', 'G', 'C', 'C', 'A', 'C', 'T', 'A', 'A', 'G', 'C', 'T', 'A', 'T', 'A', 'C', 'T', 'G', 'A', 'G', 'A', 'C', 'A', 'C', 'G', 'C', 'A', 'A', 'C', 'A', 'G', 'G', 'G', 'G', 'A', 'A', 'A', 'G', 'G', 'C', 'A', 'A', 'G', 'G', 'C', 'A', 'C', 'A'};
      byte[] r1 = new byte[]{GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, 'T', 'G', 'A', 'G', 'A', 'C', 'A', 'C', 'G', 'C', 'A', 'A', 'C', 'A', 'T', 'G', 'G', 'G', 'A', 'A', 'A', 'G', 'G', 'C', 'A', 'A', 'G', 'G', 'C', 'A', 'C', 'A', 'C', 'A', 'G', 'G', 'G', 'G', 'A', 'T', 'A', 'G', 'G'};
      byte[] r2 = new byte[]{'A', 'A', 'T', 'T', 'T', 'A', 'T', 'T', 'T', 'T', 'A', 'T', 'T', 'G', 'T', 'G', 'A', 'T', 'T', 'T', 'T', 'T', 'T', 'G', 'G', 'A', 'G', 'G', 'T', 'T', 'T', 'G', 'G', 'A', 'A', 'G', 'C', 'C', 'A', 'C', 'T', 'A', 'A', 'G', 'C', 'T', 'A', 'T', 'A', 'C', 'T', 'G', 'A', 'G', 'A', 'C', 'A', 'C', 'G', 'C', 'A', 'A', 'C', 'A', 'G', 'G', 'G', 'G', 'A', 'A', 'A', 'G', 'G', 'C', 'A', 'A', 'G', 'G', 'C', 'A', 'C', 'A', GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP};
      int error =  1 ;
      int length =  32 ;
      int begin =  50 ;

      SemiglobalAligner aligner = new SemiglobalAligner();
      aligner.setBeginLocations(new TopAndLeftEdges());
      aligner.setEndLocations(new BottomAndRightEdges());
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2);
      
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getBegin(), begin);
      assertEquals(result.getLength(), length);
      assertEquals(result.getErrors(), error);
   }


   @Test
   public void testSemiglobalAlign4() {
      byte[] s1 = new byte[]{'T', 'C', 'C', 'A', 'T', 'C', 'T', 'C', 'A', 'T', 'C', 'C', 'C', 'T', 'G', 'C', 'G', 'T', 'G', 'T', 'C', 'C', 'C', 'A', 'T', 'C', 'T', 'G', 'T', 'T', 'C', 'C', 'C', 'T', 'C', 'C', 'C', 'T', 'G', 'T', 'C', 'T', 'C', 'A'};
      byte[] s2 = new byte[]{'T', 'T', 'T', 'T', 'A', 'G', 'G', 'A', 'A', 'A', 'T', 'A', 'C', 'G', 'C', 'C', 'T', 'G', 'G', 'T', 'G', 'G', 'G', 'G', 'T', 'T', 'T', 'G', 'G', 'A', 'G', 'T', 'A', 'T', 'A', 'G', 'T', 'G', 'A', 'A', 'A', 'G', 'A', 'T', 'A', 'G', 'G', 'T', 'G', 'A', 'G', 'T', 'T', 'G', 'G', 'T', 'C', 'G', 'G', 'G', 'T', 'G'};
      byte[] r1 = new byte[]{'T', 'C', 'C', 'A', 'T', 'C', 'T', 'C', 'A', 'T', 'C', 'C', 'C', 'T', 'G', 'C', 'G', 'T', 'G', 'T', 'C', 'C', 'C', 'A', 'T', 'C', 'T', 'G', 'T', 'T', 'C', 'C', 'C', 'T', 'C', 'C', 'C', 'T', 'G', 'T', 'C', 'T', 'C', 'A', GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP};
      byte[] r2 = new byte[]{GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, 'T', 'T', 'T', 'T', 'A', 'G', 'G', 'A', 'A', 'A', 'T', 'A', 'C', 'G', 'C', 'C', 'T', 'G', 'G', 'T', 'G', 'G', 'G', 'G', 'T', 'T', 'T', 'G', 'G', 'A', 'G', 'T', 'A', 'T', 'A', 'G', 'T', 'G', 'A', 'A', 'A', 'G', 'A', 'T', 'A', 'G', 'G', 'T', 'G', 'A', 'G', 'T', 'T', 'G', 'G', 'T', 'C', 'G', 'G', 'G', 'T', 'G'};
      int error =  2 ;
      int length =  5 ;
      int begin =  39 ;

      SemiglobalAligner aligner = new SemiglobalAligner();
      aligner.setBeginLocations(new TopAndLeftEdges());
      aligner.setEndLocations(new BottomAndRightEdges());
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2);
      
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getBegin(), begin);
      assertEquals(result.getLength(), length);
      assertEquals(result.getErrors(), error);
   }


   @Test
   public void testSemiglobalAlign5() {
      byte[] s1 = new byte[]{'T', 'C', 'T', 'G', 'T', 'T', 'C', 'C', 'C', 'T', 'C', 'C', 'C', 'T', 'G', 'T', 'C', 'T', 'C', 'A'};
      byte[] s2 = new byte[]{'T', 'T', 'T', 'T', 'A', 'G', 'G', 'A', 'A', 'A', 'T', 'A', 'C', 'G', 'C', 'C'};
      byte[] r1 = new byte[]{GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, 'T', GAP, 'C', 'T', 'G', 'T', 'T', 'C', 'C', 'C', 'T', 'C', 'C', 'C', 'T', 'G', 'T', 'C', 'T', 'C', 'A'};
      byte[] r2 = new byte[]{'T', 'T', 'T', 'T', 'A', 'G', 'G', 'A', 'A', 'A', 'T', 'A', 'C', GAP, 'G', GAP, GAP, 'C', 'C', GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP};
      int error =  4 ;
      int length =  9 ;
      int begin =  10 ;

      SemiglobalAligner aligner = new SemiglobalAligner();
      aligner.setBeginLocations(new TopAndLeftEdges());
      aligner.setEndLocations(new BottomAndRightEdges());
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2);
      
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getBegin(), begin);
      assertEquals(result.getLength(), length);
      assertEquals(result.getErrors(), error);
   }


   @Test
   public void testSemiglobalAlign6() {
      byte[] s1 = new byte[]{'T', 'G', 'A', 'G', 'A', 'C', 'A', 'C', 'G', 'C', 'A', 'A', 'C', 'A', 'G', 'G', 'G', 'G', 'A', 'A', 'A', 'G', 'G', 'C', 'A', 'A', 'G', 'G', 'C', 'A', 'C', 'A', 'C', 'A', 'G', 'G', 'G', 'G', 'A', 'T', 'A', 'G', 'G'};
      byte[] s2 = new byte[]{'A', 'A', 'T', 'T', 'T', 'T', 'A', 'T', 'T', 'T', 'G', 'A', 'T', 'G', 'A', 'T', 'G', 'A', 'T', 'T', 'T', 'T', 'A', 'T', 'T', 'C', 'G', 'A', 'G', 'T', 'A', 'T', 'A', 'T', 'T', 'C', 'G', 'A', 'T', 'G', 'A', 'T', 'T', 'T', 'T', 'A', 'T', 'T', 'T', 'G', 'A', 'T', 'T', 'T', 'C', 'A', 'T', 'T', 'T', 'G', 'A', 'T', 'G', 'A', 'T', 'G', 'A', 'T', 'T', 'T', 'T', 'A', 'T', 'T', 'C', 'G', 'A', 'T', 'T', 'T', 'T', 'A', 'T', 'T', 'T', 'G', 'A', 'T', 'G', 'A', 'A', 'G', 'A', 'T', 'T', 'T', 'T', 'A', 'T', 'T', 'C', 'G', 'A', 'G', 'A', 'T', 'T', 'A', 'T', 'T', 'T', 'G', 'A', 'T', 'G', 'A', 'T', 'T', 'T', 'A', 'A', 'T', 'T'};
      byte[] r1 = new byte[]{GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, 'T', 'G', 'A', 'G', 'A', 'C', 'A', 'C', 'G', 'C', 'A', 'A', 'C', 'A', 'G', 'G', 'G', 'G', 'A', 'A', 'A', 'G', 'G', 'C', 'A', 'A', 'G', 'G', 'C', 'A', 'C', 'A', 'C', 'A', 'G', 'G', 'G', 'G', 'A', 'T', 'A', 'G', 'G'};
      byte[] r2 = new byte[]{'A', 'A', 'T', 'T', 'T', 'T', 'A', 'T', 'T', 'T', 'G', 'A', 'T', 'G', 'A', 'T', 'G', 'A', 'T', 'T', 'T', 'T', 'A', 'T', 'T', 'C', 'G', 'A', 'G', 'T', 'A', 'T', 'A', 'T', 'T', 'C', 'G', 'A', 'T', 'G', 'A', 'T', 'T', 'T', 'T', 'A', 'T', 'T', 'T', 'G', 'A', 'T', 'T', 'T', 'C', 'A', 'T', 'T', 'T', 'G', 'A', 'T', 'G', 'A', 'T', 'G', 'A', 'T', 'T', 'T', 'T', 'A', 'T', 'T', 'C', 'G', 'A', 'T', 'T', 'T', 'T', 'A', 'T', 'T', 'T', 'G', 'A', 'T', 'G', 'A', 'A', 'G', 'A', 'T', 'T', 'T', 'T', 'A', 'T', 'T', 'C', 'G', 'A', 'G', 'A', 'T', 'T', 'A', 'T', 'T', 'T', 'G', 'A', 'T', 'G', 'A', 'T', 'T', 'T', 'A', 'A', 'T', 'T', GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP};
      int error =  0 ;
      int length =  1 ;
      int begin =  122 ;

      SemiglobalAligner aligner = new SemiglobalAligner();
      aligner.setBeginLocations(new TopAndLeftEdges());
      aligner.setEndLocations(new BottomAndRightEdges());
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2);
      
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getBegin(), begin);
      assertEquals(result.getLength(), length);
      assertEquals(result.getErrors(), error);
   }


   @Test
   public void testSemiglobalAlign7() {
      byte[] s1 = new byte[]{'A', 'B', 'C', 'D', 'E', 'F'};
      byte[] s2 = new byte[]{'A', 'x', 'B', 'C', 'D', 'E', 'G', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x'};
      byte[] r1 = new byte[]{'A', GAP, 'B', 'C', 'D', 'E', 'F', GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP};
      byte[] r2 = new byte[]{'A', 'x', 'B', 'C', 'D', 'E', 'G', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x'};
      int error =  2 ;
      int length =  7 ;
      int begin =  0 ;

      SemiglobalAligner aligner = new SemiglobalAligner();
      aligner.setBeginLocations(new TopAndLeftEdges());
      aligner.setEndLocations(new BottomAndRightEdges());
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2);
      
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getBegin(), begin);
      assertEquals(result.getLength(), length);
      assertEquals(result.getErrors(), error);
   }


   @Test
   public void testSemiglobalAlign8() {
      byte[] s1 = new byte[]{'G', 'G', 'A', 'A', 'T', 'C', 'C', 'C'};
      byte[] s2 = new byte[]{'T', 'G', 'A', 'G', 'G', 'G', 'A', 'T', 'A', 'A', 'A', 'T', 'A', 'T', 'T', 'T', 'A', 'G', 'A', 'A', 'T', 'T', 'T', 'A', 'G', 'T', 'A', 'G', 'T', 'A', 'G', 'T', 'G', 'T', 'T'};
      byte[] r1 = new byte[]{GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, 'G', GAP, 'G', 'A', 'A', 'T', 'C', 'C', 'C'};
      byte[] r2 = new byte[]{'T', 'G', 'A', 'G', 'G', 'G', 'A', 'T', 'A', 'A', 'A', 'T', 'A', 'T', 'T', 'T', 'A', 'G', 'A', 'A', 'T', 'T', 'T', 'A', 'G', 'T', 'A', 'G', 'T', 'A', 'G', 'T', 'G', GAP, 'T', 'T', GAP, GAP, GAP};
      int error =  3 ;
      int length =  6 ;
      int begin =  30 ;

      SemiglobalAligner aligner = new SemiglobalAligner();
      aligner.setBeginLocations(new TopAndLeftEdges());
      aligner.setEndLocations(new BottomAndRightEdges());
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2);
      
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getBegin(), begin);
      assertEquals(result.getLength(), length);
      assertEquals(result.getErrors(), error);
   }


   @Test
   public void testSemiglobalAlign9() {
      byte[] s1 = new byte[]{'B', 'R', 'a', 'b', 'b', 'e', 'l'};
      byte[] s2 = new byte[]{'b', 'r', ' ', 'a', 'b', 'b', 'e', 'l', 'r', 'a', 'b', 'a', 'b', 'b', 'e', 'l'};
      byte[] r1 = new byte[]{GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, 'B', 'R', 'a', 'b', 'b', 'e', 'l'};
      byte[] r2 = new byte[]{'b', 'r', ' ', 'a', 'b', 'b', 'e', 'l', 'r', 'a', 'b', 'a', 'b', 'b', 'e', 'l'};
      int error =  2 ;
      int length =  7 ;
      int begin =  9 ;

      SemiglobalAligner aligner = new SemiglobalAligner();
      aligner.setBeginLocations(new TopAndLeftEdges());
      aligner.setEndLocations(new BottomAndRightEdges());
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2);
      
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getBegin(), begin);
      assertEquals(result.getLength(), length);
      assertEquals(result.getErrors(), error);
   }


   @Test
   public void testSemiglobalAlign10() {
      byte[] s1 = new byte[]{'B', 'R', 'a', 'b', 'b', 'e', 'l'};
      byte[] s2 = new byte[]{'B', 'R', 'a', 'b', 'b', 'e', 'l'};
      byte[] r1 = new byte[]{'B', 'R', 'a', 'b', 'b', 'e', 'l'};
      byte[] r2 = new byte[]{'B', 'R', 'a', 'b', 'b', 'e', 'l'};
      int error =  0 ;
      int length =  7 ;
      int begin =  0 ;

      SemiglobalAligner aligner = new SemiglobalAligner();
      aligner.setBeginLocations(new TopAndLeftEdges());
      aligner.setEndLocations(new BottomAndRightEdges());
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2);
      
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getBegin(), begin);
      assertEquals(result.getLength(), length);
      assertEquals(result.getErrors(), error);
   }


   @Test
   public void testSemiglobalAlign11() {
      byte[] s1 = new byte[]{'A'};
      byte[] s2 = new byte[]{'T', 'C', 'T', 'G', 'C', 'T', 'C', 'C', 'T', 'G', 'G', 'C', 'C', 'C', 'A', 'T', 'G', 'A', 'T', 'C', 'G', 'T', 'A', 'T', 'A', 'A', 'C', 'T', 'T', 'T', 'C', 'A', 'A', 'A', 'T', 'T', 'T'};
      byte[] r1 = new byte[]{GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, 'A', GAP, GAP, GAP};
      byte[] r2 = new byte[]{'T', 'C', 'T', 'G', 'C', 'T', 'C', 'C', 'T', 'G', 'G', 'C', 'C', 'C', 'A', 'T', 'G', 'A', 'T', 'C', 'G', 'T', 'A', 'T', 'A', 'A', 'C', 'T', 'T', 'T', 'C', 'A', 'A', 'A', 'T', 'T', 'T'};
      int error =  0 ;
      int length =  1 ;
      int begin =  33 ;

      SemiglobalAligner aligner = new SemiglobalAligner();
      aligner.setBeginLocations(new TopAndLeftEdges());
      aligner.setEndLocations(new BottomAndRightEdges());
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2);
      
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getBegin(), begin);
      assertEquals(result.getLength(), length);
      assertEquals(result.getErrors(), error);
   }


   @Test
   public void testSemiglobalAlign12() {
      byte[] s1 = new byte[]{};
      byte[] s2 = new byte[]{'C', 'G', 'T', 'G', 'A', 'A', 'C', 'C', 'C', 'G', 'G', 'G', 'G', 'G', 'T', 'G', 'G', 'A', 'G', 'C', 'T', 'T', 'G', 'C', 'A', 'G', 'T', 'G'};
      byte[] r1 = new byte[]{GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP};
      byte[] r2 = new byte[]{'C', 'G', 'T', 'G', 'A', 'A', 'C', 'C', 'C', 'G', 'G', 'G', 'G', 'G', 'T', 'G', 'G', 'A', 'G', 'C', 'T', 'T', 'G', 'C', 'A', 'G', 'T', 'G'};
      int error =  0 ;
      int length =  0 ;
      int begin =  28 ;

      SemiglobalAligner aligner = new SemiglobalAligner();
      aligner.setBeginLocations(new TopAndLeftEdges());
      aligner.setEndLocations(new BottomAndRightEdges());
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2);
      
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getBegin(), begin);
      assertEquals(result.getLength(), length);
      assertEquals(result.getErrors(), error);
   }


   @Test
   public void testSemiglobalAlign13() {
      byte[] s1 = new byte[]{'T', 'A', 'T', 'T', 'T', 'T', 'G', 'G', 'G', 'A', 'G', 'G', 'T', 'C', 'G', 'A', 'G', 'G', 'T', 'A', 'G', 'G', 'C', 'G', 'G', 'A', 'T', 'T', 'A', 'C', 'G', 'A', 'G', 'G', 'T', 'T', 'A', 'G', 'G', 'A', 'G', 'A', 'T', 'C', 'G', 'A', 'G', 'A', 'T', 'T', 'A', 'T', 'T', 'T', 'T', 'G', 'A', 'T', 'T', 'A', 'A'};
      byte[] s2 = new byte[]{'C', 'A', 'C', 'T', 'T', 'T', 'G', 'G', 'G', 'A', 'G', 'G', 'C', 'C', 'A', 'A', 'G', 'G', 'C', 'G', 'G', 'G', 'C', 'G', 'G', 'A', 'T', 'C', 'A', 'C', 'G', 'A', 'G', 'G', 'T', 'C', 'A', 'G', 'G', 'A', 'G', 'A', 'T', 'C', 'G', 'A', 'G', 'A', 'C', 'C', 'A', 'T', 'C', 'C', 'T', 'G', 'G', 'C', 'T', 'A', 'G'};
      byte[] r1 = new byte[]{'T', 'A', 'T', 'T', 'T', 'T', 'G', 'G', 'G', 'A', 'G', 'G', 'T', 'C', 'G', 'A', 'G', 'G', 'T', 'A', 'G', 'G', 'C', 'G', 'G', 'A', 'T', 'T', 'A', 'C', 'G', 'A', 'G', 'G', 'T', 'T', 'A', 'G', 'G', 'A', 'G', 'A', 'T', 'C', 'G', 'A', 'G', 'A', 'T', 'T', 'A', 'T', 'T', 'T', 'T', 'G', 'A', 'T', 'T', 'A', 'A'};
      byte[] r2 = new byte[]{'C', 'A', 'C', 'T', 'T', 'T', 'G', 'G', 'G', 'A', 'G', 'G', 'C', 'C', 'A', 'A', 'G', 'G', 'C', 'G', 'G', 'G', 'C', 'G', 'G', 'A', 'T', 'C', 'A', 'C', 'G', 'A', 'G', 'G', 'T', 'C', 'A', 'G', 'G', 'A', 'G', 'A', 'T', 'C', 'G', 'A', 'G', 'A', 'C', 'C', 'A', 'T', 'C', 'C', 'T', 'G', 'G', 'C', 'T', 'A', 'G'};
      int error =  15 ;
      int length =  61 ;
      int begin =  0 ;

      SemiglobalAligner aligner = new SemiglobalAligner();
      aligner.setBeginLocations(new TopAndLeftEdges());
      aligner.setEndLocations(new BottomAndRightEdges());
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2);
      
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getBegin(), begin);
      assertEquals(result.getLength(), length);
      assertEquals(result.getErrors(), error);
   }


   @Test
   public void testSemiglobalAlign14() {
      byte[] s1 = new byte[]{'A', 'A', 'A', 'A', 'C', 'C', 'T', 'A', 'T', 'C', 'C', 'C', 'G', 'G', 'G', 'A', 'G', 'G', 'T', 'C', 'G', 'A', 'G', 'G', 'T', 'A'};
      byte[] s2 = new byte[]{'A', 'A', 'A', 'A', 'C', 'C', 'T', 'A', 'T', 'C', 'C', 'C', 'G', 'G', 'G', 'A', 'G', 'G', 'T', 'C', 'G', 'A', 'G', 'G', 'T', 'A', 'G', 'G', 'C', 'G', 'G', 'A', 'T', 'T', 'A', 'C', 'G', 'A', 'G', 'G', 'T', 'T', 'A', 'G', 'G', 'A', 'G', 'A', 'T', 'C', 'G', 'A', 'G', 'A', 'T', 'T', 'A', 'T', 'T', 'T', 'T', 'G', 'A', 'T', 'T', 'A', 'A'};
      byte[] r1 = new byte[]{'A', 'A', 'A', 'A', 'C', 'C', 'T', 'A', 'T', 'C', 'C', 'C', 'G', 'G', 'G', 'A', 'G', 'G', 'T', 'C', 'G', 'A', 'G', 'G', 'T', 'A', GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP};
      byte[] r2 = new byte[]{'A', 'A', 'A', 'A', 'C', 'C', 'T', 'A', 'T', 'C', 'C', 'C', 'G', 'G', 'G', 'A', 'G', 'G', 'T', 'C', 'G', 'A', 'G', 'G', 'T', 'A', 'G', 'G', 'C', 'G', 'G', 'A', 'T', 'T', 'A', 'C', 'G', 'A', 'G', 'G', 'T', 'T', 'A', 'G', 'G', 'A', 'G', 'A', 'T', 'C', 'G', 'A', 'G', 'A', 'T', 'T', 'A', 'T', 'T', 'T', 'T', 'G', 'A', 'T', 'T', 'A', 'A'};
      int error =  0 ;
      int length =  26 ;
      int begin =  0 ;

      SemiglobalAligner aligner = new SemiglobalAligner();
      aligner.setBeginLocations(new TopAndLeftEdges());
      aligner.setEndLocations(new BottomAndRightEdges());
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2);
      
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getBegin(), begin);
      assertEquals(result.getLength(), length);
      assertEquals(result.getErrors(), error);
   }


   @Test
   public void testSemiglobalAlign15() {
      byte[] s1 = new byte[]{'T', 'T', 'T', 'G', 'T', 'A', 'A', 'T', 'T', 'T', 'T', 'A', 'G', 'T', 'T', 'A', 'C', 'T', 'C', 'G', 'G', 'G', 'A', 'G', 'G', 'T', 'T', 'G', 'A', 'G', 'G', 'T', 'A', 'G', 'G', 'A', 'G', 'A', 'A', 'T', 'C', 'G', 'T', 'T', 'T', 'G', 'A', 'A', 'T', 'T', 'C', 'G', 'G', 'G', 'A', 'G', 'G', 'T', 'A', 'G', 'A', 'G', 'G', 'T', 'T', 'G'};
      byte[] s2 = new byte[]{'C', 'C', 'T', 'G', 'C', 'A', 'A', 'T', 'C', 'C', 'C', 'C', 'G', 'C', 'T', 'A', 'C', 'T', 'C', 'G', 'G', 'G', 'A', 'G', 'G', 'C', 'T', 'G', 'A', 'G', 'G', 'C', 'A', 'G', 'G', 'T', 'G', 'A', 'A', 'T', 'C', 'G', 'C', 'T', 'T', 'G', 'A', 'A', 'C', 'C', 'C', 'G', 'G', 'G', 'A', 'G', 'G', 'C', 'A', 'G', 'A', 'G', 'G', 'T', 'T', 'G'};
      byte[] r1 = new byte[]{'T', 'T', 'T', 'G', 'T', 'A', 'A', 'T', 'T', 'T', 'T', 'A', 'G', 'T', 'T', 'A', 'C', 'T', 'C', 'G', 'G', 'G', 'A', 'G', 'G', 'T', 'T', 'G', 'A', 'G', 'G', 'T', 'A', 'G', 'G', 'A', 'G', 'A', 'A', 'T', 'C', 'G', 'T', 'T', 'T', 'G', 'A', 'A', 'T', 'T', 'C', 'G', 'G', 'G', 'A', 'G', 'G', 'T', 'A', 'G', 'A', 'G', 'G', 'T', 'T', 'G'};
      byte[] r2 = new byte[]{'C', 'C', 'T', 'G', 'C', 'A', 'A', 'T', 'C', 'C', 'C', 'C', 'G', 'C', 'T', 'A', 'C', 'T', 'C', 'G', 'G', 'G', 'A', 'G', 'G', 'C', 'T', 'G', 'A', 'G', 'G', 'C', 'A', 'G', 'G', 'T', 'G', 'A', 'A', 'T', 'C', 'G', 'C', 'T', 'T', 'G', 'A', 'A', 'C', 'C', 'C', 'G', 'G', 'G', 'A', 'G', 'G', 'C', 'A', 'G', 'A', 'G', 'G', 'T', 'T', 'G'};
      int error =  15 ;
      int length =  66 ;
      int begin =  0 ;

      SemiglobalAligner aligner = new SemiglobalAligner();
      aligner.setBeginLocations(new TopAndLeftEdges());
      aligner.setEndLocations(new BottomAndRightEdges());
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2);
      
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getBegin(), begin);
      assertEquals(result.getLength(), length);
      assertEquals(result.getErrors(), error);
   }


   @Test
   public void testSemiglobalAlign16() {
      byte[] s1 = new byte[]{'A', 'B', 'C'};
      byte[] s2 = new byte[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L'};
      byte[] r1 = new byte[]{'A', 'B', 'C', GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP};
      byte[] r2 = new byte[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L'};
      int error =  0 ;
      int length =  3 ;
      int begin =  0 ;

      SemiglobalAligner aligner = new SemiglobalAligner();
      aligner.setBeginLocations(new TopAndLeftEdges());
      aligner.setEndLocations(new BottomAndRightEdges());
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2);
      
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getBegin(), begin);
      assertEquals(result.getLength(), length);
      assertEquals(result.getErrors(), error);
   }
   
   
   @Test
   public void testSemiglobalAlign17() {
      byte[] s1 = new byte[] { 'C', 'G', 'T', 'G', 'A', 'A', 'C', 'C', 'C', 'G', 'G', 'G', 'G', 'G', 'T', 'G', 'G', 'A', 'G', 'C', 'T', 'T', 'G', 'C', 'A', 'G', 'T', 'G' };
      byte[] s2 = new byte[] {};
      byte[] r1 = new byte[] { 'C', 'G', 'T', 'G', 'A', 'A', 'C', 'C', 'C', 'G', 'G', 'G', 'G', 'G', 'T', 'G', 'G', 'A', 'G', 'C', 'T', 'T', 'G', 'C', 'A', 'G', 'T', 'G' };
      byte[] r2 = new byte[] { GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP, GAP };
      int error = 0;
      int length = 0;
      int begin = 28;

      SemiglobalAligner aligner = new SemiglobalAligner();
      aligner.setBeginLocations(new TopAndLeftEdges());
      aligner.setEndLocations(new BottomAndRightEdges());
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2);
      
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getBegin(), begin);
      assertEquals(result.getLength(), length);
      assertEquals(result.getErrors(), error);
   }



}
