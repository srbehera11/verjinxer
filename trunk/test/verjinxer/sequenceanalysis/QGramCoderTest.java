
package verjinxer.sequenceanalysis;

import java.util.Arrays;
import junit.framework.TestCase;

/**
 *
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

   /**
    * Test of code method, of class QGramCoder.
    */
   public void testCode() {
   }

   /**
    * Test of codeUpdate method, of class QGramCoder.
    */
   public void testCodeUpdate() {
   }

   /**
    * Test of qGram method, of class QGramCoder.
    */
   public void testQGram() {
   }

   /**
    * Test of qGramString method, of class QGramCoder.
    */
   public void testQGramString() {
   }

   private final int SKIP = -99;
   final byte[] text = {0, 1, 2, 4, 5, 2, 0, -1, 9, 3, 2, 4, -1, 0};
   final int q=2;

   /**
    * Test of simple iteration
    */
   public void testQGrams() {
      final QGramCoder coder = new QGramCoder(q, 4); // |A|=4   
      final int[] correct = new int[text.length-q+1];
      for(int p=0; p<correct.length; p++) {
         final int c = coder.code(text, p);
         if (c>=0) correct[p]=c;
         else correct[p] = -1;
      }
      
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
    * Test iteration without separators
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
      for (long pc: coder.sparseQGrams(text)) {
         final int p = (int)(pc>>32);
         qcodes[p] = (int)pc;
      }
      for(int p=0; p<correct.length; p++)
         System.out.printf("pos=%d:  code=%d (correct: %d)%n", p, qcodes[p], correct[p]);
      System.out.println();
      for(int p=0; p<correct.length; p++) 
         assertEquals(String.format("Error at position %d",p),correct[p], qcodes[p]);
   }

   /**
    * test iteration with separators
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
      for (long pc: coder.sparseQGrams(text, -1)) {
         final int p = (int)(pc>>32);
         qcodes[p] = (int)pc;
      }
      for(int p=0; p<correct.length; p++)
         System.out.printf("pos=%d:  code=%d (correct: %d)%n", p, qcodes[p], correct[p]);
      System.out.println();
      for(int p=0; p<correct.length; p++) 
         assertEquals(String.format("Error at position %d",p),correct[p], qcodes[p]);
   }

}
