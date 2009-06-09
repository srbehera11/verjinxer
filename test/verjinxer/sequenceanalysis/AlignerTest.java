package verjinxer.sequenceanalysis;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class AlignerTest {
   
   public static final byte GAP = Aligner.GAP; 

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
      
      Aligner.SemiglobalAlignmentResult result = Aligner.semiglobalAlign(s1, s2);
      
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getBegin(), begin);
      assertEquals(result.getLength(), length);
      assertEquals(result.getErrors(), error);
   }

}
