
package verjinxer.sequenceanalysis;

import java.util.Arrays;

import verjinxer.util.PositionQCodePair;
import junit.framework.TestCase;

/**
 * Unit Tests for QGramCoder, BisulfiteQGramCoder, and MultiQGramCoder
 * @author Sven Rahmann
 */
public class QGramCoderTest extends TestCase {
    
    public QGramCoderTest(String testName) {
        super(testName);
    }            

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }


   private final int SKIP = -99;
   // TODO: repeat tests for many texts and values of q
   final byte[] text = {0, 1, 2, 4, 5, 2, 2, 1, 2, 1, 0, -1, 9, 3, 2, 4, -1, 0, 0};
   //                   A  C  G  x  x  G  G  C  G  C  A  .   x  T  G  x  .   A  A
   final int q=3;

   
   /**
    * Test of simple iteration
    */
   public void testQGrams() {
      final QGramCoder coder = new QGramCoder(q, 4); // |A|=4   
      final int[] correct = new int[text.length-q+1];
      for(int p=0; p<correct.length; p++) correct[p] = coder.code(text, p);
      
      final int[] qcodes  = new int[correct.length];
      Arrays.fill(qcodes, SKIP);
      int pp=0;
      for (int c: coder.qGrams(text)) {
         qcodes[pp++] = c;
      }
      for(int p=0; p<correct.length; p++)
         System.out.printf("pos=%d:  code=%d (correct: %d)%n", p, qcodes[p], correct[p]);
      System.out.println();
      for(int p=0; p<correct.length; p++) 
         assertEquals(String.format("Error at position %d",p),correct[p], qcodes[p]);
   }

   

   /**
    * Test of sparse iteration without separators
    */
   public void testSparseQGrams() {
      final QGramCoder coder = new QGramCoder(q, 4); // |A|=4   
      final int[] correct = new int[text.length-q+1];
      for(int p=0; p<correct.length; p++) {
         final int c = coder.code(text, p);
         if (c>=0) correct[p]=c;
         else correct[p] = SKIP;
      }
      
      final int[] qcodes  = new int[correct.length];
      Arrays.fill(qcodes, SKIP);
      for (PositionQCodePair pc: coder.sparseQGrams(text)) {
         final int p = pc.position;
         qcodes[p] = pc.qcode;
      }
      for(int p=0; p<correct.length; p++)
         System.out.printf("pos=%d:  code=%d (correct: %d)%n", p, qcodes[p], correct[p]);
      System.out.println();
      for(int p=0; p<correct.length; p++) 
         assertEquals(String.format("Error at position %d",p),correct[p], qcodes[p]);
   }

   /**
    * test of sparse iteration with separators
    */
   public void testSparseQGramsWithSeparators() {
      final QGramCoder coder = new QGramCoder(q, 4); // |A|=4   
      final int[] correct = new int[text.length-q+1];
      for(int p=0; p<correct.length; p++) {
         final int c = coder.code(text, p);
         if (c>=0) correct[p]=c;
         else correct[p] = (text[p]<0)? -1 : SKIP;  
      }
      
      final int[] qcodes  = new int[correct.length];
      Arrays.fill(qcodes, SKIP);
      for (PositionQCodePair pc: coder.sparseQGrams(text, (byte)-1)) {
         final int p = pc.position;
         qcodes[p] = pc.qcode;
      }
      for(int p=0; p<correct.length; p++)
         System.out.printf("pos=%d:  code=%d (correct: %d)%n", p, qcodes[p], correct[p]);
      System.out.println();
      for(int p=0; p<correct.length; p++) 
         assertEquals(String.format("Error at position %d",p),correct[p], qcodes[p]);
   }
   
   public void testMulti() {
      final Alphabet DNA = Alphabet.DNA();
      final BisulfiteQGramCoder coder = new BisulfiteQGramCoder(q);
      for(PositionQCodePair pc: coder.sparseQGrams(text, (byte)-1)) {
         final int p = pc.position;
         final int c = pc.qcode;
         System.out.printf("pos=%d:  code=%d  (%s)%n", p, c, coder.qGramString(c, DNA));
      }
      System.out.println();
   }

}
