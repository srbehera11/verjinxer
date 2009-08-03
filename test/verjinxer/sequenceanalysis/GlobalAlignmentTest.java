package verjinxer.sequenceanalysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import verjinxer.sequenceanalysis.alignment.AlignerFactory;
import verjinxer.sequenceanalysis.alignment.IAligner;
import verjinxer.sequenceanalysis.alignment.SemiglobalAligner;

/**
 * @author Markus Kemmerling
 */
public class GlobalAlignmentTest {
   public static final byte GAP = IAligner.GAP;
   // a dummy alphabet so that all elements in the test arrays are treated as symbols
   private static final Alphabet alphabet = new Alphabet(new String[] { "##symbols:0", "#", "#",
         "#", "#", "#", "#", "#", "#", "#", "#", "#", "#", "#", "#", "#", "#", "#", "#", "#", "#",
         "#", "#", "#", "#", "#", "#", "#", "#", "#", "#", "#", "#", " ", "!", "\"", "#", "$", "%",
         "&", "'", "(", ")", "*", "+", ",", "-", ".", "/", "0", "1", "2", "3", "4", "5", "6", "7",
         "8", "9", ":", ";", "<", "=", ">", "?", "@", "A", "B", "C", "D", "E", "F", "G", "H", "I",
         "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "#",
         "#", "#", "#", "#", "#", "#", "#", "#", "#", "#", "#", "#", "#", "#", "#", "#", "#", "#",
         "#", "#", "#", "#", "#", "#", "#", "#", "#", "#", "#", "#", "#", "#", "#", "#", "#" });
   
   /**
    * Tests if a and b contains a gap at the same position (a[i] == GAP && b[i] == GAP). Such a
    * position must not be exist.
    * 
    * @param a
    * @param b
    */
   private static void noOppositeGaps(byte[] a, byte[] b) {
      for (int i = 0; i < a.length && i < b.length; i++) {
         assertTrue(String.format("At position %d are two GAPs oppositly.", i), 
               a[i] != GAP || b[i] != GAP);
      }
   }

   /**
    * Removes all gaps in the arrays and tests if the uprising arrays are the same.
    * 
    * @param a
    * @param b
    */
   private static void equalWithoutGAPs(byte[] a, byte[] b) {
      int i = 0;
      int j = 0;
      while (i < a.length && j < b.length) {
         while (i < a.length && a[i] == GAP) {
            i++;
         }
         while (j < b.length && b[j] == GAP) {
            j++;
         }
         if (i >= a.length || j >= b.length) {
            break;
         }
         assertEquals(String.format("Position %d in a (%d) differs from position %d in b (%d).", i,
               a[i], j, b[j]), a[i], b[j]);
         i++;
         j++;
      }

      while (i < a.length) {
         assertEquals(GAP, a[i]);
         i++;
      }
      while (j < b.length) {
         assertEquals(GAP, b[j]);
         j++;
      }
   }

   /**
    * Compares each position of the two given arrays and counts the mismatches.
    * 
    * @param query
    * @param reference
    * @return
    */
   private static int countErrorsAndTestEnd(byte[] query, byte[] reference) {
      int errors = 0;
      int i = query.length-1;
      
      for ( ; i >= 0; i--) { 
         if (query[i] != reference[i]) {
            errors++;
         }
      }
      return errors;
   }
   
   @Test
   public void testGlobalAligner() {
      System.out.println("testGlobalAligner()");
      byte[] s1 = {3,0,3,3,0}; // SISSI
      byte[] s2 = {1,0,3,3,0,3,3,0,2,2,0}; // MISSISSIPPI
      
//      byte[] r1 = {GAP,GAP,GAP,3,0,3,3,0,GAP,GAP,GAP}; // [GAP, GAP, GAP, 'S', 'I', 'S', 'S', 'I', GAP, GAP, GAP]
//      byte[] r2 = {1,0,3,3,0,3,3,0,2,2,0}; // [ 'M',  'I',  'S', 'S', 'I', 'S', 'S', 'I',  'P',  'P',  'I']
//      
//      int begin = 3;
//      int length = 5;
//      int error = 0;
      
      SemiglobalAligner aligner = AlignerFactory.createGlobalAligner();
      //aligner.debug();
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2, Alphabet.DNA());
      
      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); //left top corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); //left top corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); //bottom right corner 
      assertEquals("Alingment ends in wrong column.", s2.length, result.getEndPosition().column); //bottom right corner 
      
      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2()), result.getErrors());
      
      
      System.out.println(result.toString());
      System.out.println(result.getErrors());
      System.out.println();
      
//      assertArrayEquals(result.getSequence1(), r1);
//      assertArrayEquals(result.getSequence2(), r2);
//      assertEquals(result.getBegin(), begin);
//      assertEquals(result.getLength(), length);
//      assertEquals(result.getErrors(), error);
   }
   
   @Test
   public void testGlobalAligner1() {
      System.out.println("testGlobalAligner1()");
      byte[] s1 = {'S', 'I', 'S', 'S', 'I'};
      byte[] s2 = {'M', 'I', 'S', 'S', 'I', 'S', 'S', 'I', 'P', 'P', 'I'};
      SemiglobalAligner aligner = AlignerFactory.createGlobalAligner();
      //aligner.debug();
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2, alphabet);

      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // right
                                                                                            // corner
      assertEquals("Alingment ends in wrong column.", s2.length, result.getEndPosition().column); // bottom
                                                                                                  // right
                                                                                                  // corner

      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2()),
            result.getErrors());

      System.out.println(result.printAsChars());
      System.out.println(result.getErrors());
      System.out.println();
   }


   @Test
   public void testGlobalAligner2() {
      System.out.println("testGlobalAligner2()");
      byte[] s1 = {' ', 'l', 'a', 'm', 'e', 'n'};
      byte[] s2 = {'S', 'e', 'h', 'r', ' ', 'g', 'e', 'e', 'h', 'r', 't', 'e', ' ', 'D', 'a', 'm', 'e', 'n', ' ', 'u', 'n', 'd', ' ', 'h', 'e', 'r', 'r', 'e', 'n', '.', ' ', 'W', 'i', 'r', ' ', 'h', 'a', 'b', 'e', 'n', ' ', 'u', 'n', 's', ' ', 'h', 'e', 'u', 't', 'e', ' ', 'h', 'i', 'e', 'r', ' ', 'i', 'n', ' ', 'K', 'a', 'm', 'e', 'n', ' ', 'v', 'e', 'r', 's', 'a', 'm', 'm', 'e', 'l', 't', '.'};
      SemiglobalAligner aligner = AlignerFactory.createGlobalAligner();
      //aligner.debug();
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2, alphabet);

      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // right
                                                                                            // corner
      assertEquals("Alingment ends in wrong column.", s2.length, result.getEndPosition().column); // bottom
                                                                                                  // right
                                                                                                  // corner

      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2()),
            result.getErrors());

      System.out.println(result.printAsChars());
      System.out.println(result.getErrors());
      System.out.println();
   }


   @Test
   public void testGlobalAligner3() {
      System.out.println("testGlobalAligner3()");
      byte[] s1 = {'T', 'G', 'A', 'G', 'A', 'C', 'A', 'C', 'G', 'C', 'A', 'A', 'C', 'A', 'T', 'G', 'G', 'G', 'A', 'A', 'A', 'G', 'G', 'C', 'A', 'A', 'G', 'G', 'C', 'A', 'C', 'A', 'C', 'A', 'G', 'G', 'G', 'G', 'A', 'T', 'A', 'G', 'G'};
      byte[] s2 = {'A', 'A', 'T', 'T', 'T', 'A', 'T', 'T', 'T', 'T', 'A', 'T', 'T', 'G', 'T', 'G', 'A', 'T', 'T', 'T', 'T', 'T', 'T', 'G', 'G', 'A', 'G', 'G', 'T', 'T', 'T', 'G', 'G', 'A', 'A', 'G', 'C', 'C', 'A', 'C', 'T', 'A', 'A', 'G', 'C', 'T', 'A', 'T', 'A', 'C', 'T', 'G', 'A', 'G', 'A', 'C', 'A', 'C', 'G', 'C', 'A', 'A', 'C', 'A', 'G', 'G', 'G', 'G', 'A', 'A', 'A', 'G', 'G', 'C', 'A', 'A', 'G', 'G', 'C', 'A', 'C', 'A'};
      SemiglobalAligner aligner = AlignerFactory.createGlobalAligner();
      //aligner.debug();
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2, alphabet);

      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // right
                                                                                            // corner
      assertEquals("Alingment ends in wrong column.", s2.length, result.getEndPosition().column); // bottom
                                                                                                  // right
                                                                                                  // corner

      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2()),
            result.getErrors());

      System.out.println(result.printAsChars());
      System.out.println(result.getErrors());
      System.out.println();
   }


   @Test
   public void testGlobalAligner4() {
      System.out.println("testGlobalAligner4()");
      byte[] s1 = {'T', 'C', 'C', 'A', 'T', 'C', 'T', 'C', 'A', 'T', 'C', 'C', 'C', 'T', 'G', 'C', 'G', 'T', 'G', 'T', 'C', 'C', 'C', 'A', 'T', 'C', 'T', 'G', 'T', 'T', 'C', 'C', 'C', 'T', 'C', 'C', 'C', 'T', 'G', 'T', 'C', 'T', 'C', 'A'};
      byte[] s2 = {'T', 'T', 'T', 'T', 'A', 'G', 'G', 'A', 'A', 'A', 'T', 'A', 'C', 'G', 'C', 'C', 'T', 'G', 'G', 'T', 'G', 'G', 'G', 'G', 'T', 'T', 'T', 'G', 'G', 'A', 'G', 'T', 'A', 'T', 'A', 'G', 'T', 'G', 'A', 'A', 'A', 'G', 'A', 'T', 'A', 'G', 'G', 'T', 'G', 'A', 'G', 'T', 'T', 'G', 'G', 'T', 'C', 'G', 'G', 'G', 'T', 'G'};
      SemiglobalAligner aligner = AlignerFactory.createGlobalAligner();
      //aligner.debug();
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2, alphabet);

      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // right
                                                                                            // corner
      assertEquals("Alingment ends in wrong column.", s2.length, result.getEndPosition().column); // bottom
                                                                                                  // right
                                                                                                  // corner

      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2()),
            result.getErrors());

      System.out.println(result.printAsChars());
      System.out.println(result.getErrors());
      System.out.println();
   }


   @Test
   public void testGlobalAligner5() {
      System.out.println("testGlobalAligner5()");
      byte[] s1 = {'T', 'C', 'T', 'G', 'T', 'T', 'C', 'C', 'C', 'T', 'C', 'C', 'C', 'T', 'G', 'T', 'C', 'T', 'C', 'A'};
      byte[] s2 = {'T', 'T', 'T', 'T', 'A', 'G', 'G', 'A', 'A', 'A', 'T', 'A', 'C', 'G', 'C', 'C'};
      SemiglobalAligner aligner = AlignerFactory.createGlobalAligner();
      //aligner.debug();
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2, alphabet);

      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // right
                                                                                            // corner
      assertEquals("Alingment ends in wrong column.", s2.length, result.getEndPosition().column); // bottom
                                                                                                  // right
                                                                                                  // corner

      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2()),
            result.getErrors());

      System.out.println(result.printAsChars());
      System.out.println(result.getErrors());
      System.out.println();
   }


   @Test
   public void testGlobalAligner6() {
      System.out.println("testGlobalAligner6()");
      byte[] s1 = {'T', 'G', 'A', 'G', 'A', 'C', 'A', 'C', 'G', 'C', 'A', 'A', 'C', 'A', 'G', 'G', 'G', 'G', 'A', 'A', 'A', 'G', 'G', 'C', 'A', 'A', 'G', 'G', 'C', 'A', 'C', 'A', 'C', 'A', 'G', 'G', 'G', 'G', 'A', 'T', 'A', 'G', 'G'};
      byte[] s2 = {'A', 'A', 'T', 'T', 'T', 'A', 'T', 'T', 'T', 'T', 'A', 'T', 'T', 'G', 'T', 'G', 'A', 'T', 'T', 'T', 'T', 'T', 'T', 'G', 'G', 'A', 'G', 'G', 'T', 'T', 'T', 'G', 'G', 'A', 'A', 'G', 'C', 'C', 'A', 'C', 'T', 'A', 'A', 'G', 'C', 'T', 'A', 'T', 'A', 'C', 'T', 'G', 'A', 'G', 'A', 'C', 'A', 'C', 'G', 'C', 'A', 'A', 'C', 'A', 'G', 'G', 'G', 'G', 'A', 'A', 'A', 'G', 'G', 'C', 'A', 'A', 'G', 'G', 'C', 'A', 'C', 'A', 'C', 'A', 'G', 'G', 'G', 'G', 'A', 'T', 'A', 'G', 'G'};
      SemiglobalAligner aligner = AlignerFactory.createGlobalAligner();
      //aligner.debug();
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2, alphabet);

      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // right
                                                                                            // corner
      assertEquals("Alingment ends in wrong column.", s2.length, result.getEndPosition().column); // bottom
                                                                                                  // right
                                                                                                  // corner

      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2()),
            result.getErrors());

      System.out.println(result.printAsChars());
      System.out.println(result.getErrors());
      System.out.println();
   }


   @Test
   public void testGlobalAligner7() {
      System.out.println("testGlobalAligner7()");
      byte[] s1 = {'T', 'G', 'A', 'G', 'A', 'C', 'A', 'C', 'G', 'C', 'A', 'A', 'C', 'A', 'G', 'G', 'G', 'G', 'A', 'A', 'A', 'G', 'G', 'C', 'A', 'A', 'G', 'G', 'C', 'A', 'C', 'A', 'C', 'A', 'G', 'G', 'G', 'G', 'A', 'T', 'A', 'G', 'G'};
      byte[] s2 = {'T', 'G', 'A', 'G', 'A', 'C', 'A', 'C', 'G', 'C', 'A', 'A', 'C', 'A', 'G', 'G', 'G', 'G', 'A', 'A', 'A', 'G', 'G', 'C', 'A', 'A', 'G', 'G', 'C', 'A', 'C', 'A', 'C', 'A', 'G', 'G', 'G', 'G', 'A', 'T', 'A', 'G', 'G'};
      SemiglobalAligner aligner = AlignerFactory.createGlobalAligner();
      //aligner.debug();
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2, alphabet);

      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // right
                                                                                            // corner
      assertEquals("Alingment ends in wrong column.", s2.length, result.getEndPosition().column); // bottom
                                                                                                  // right
                                                                                                  // corner

      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2()),
            result.getErrors());

      System.out.println(result.printAsChars());
      System.out.println(result.getErrors());
      System.out.println();
   }


   @Test
   public void testGlobalAligner8() {
      System.out.println("testGlobalAligner8()");
      byte[] s1 = {'T', 'G', 'A', 'G', 'A', 'C', 'A', 'C', 'G', 'C', 'A', 'A', 'C', 'A', 'G', 'G', 'G', 'G', 'A', 'A', 'A', 'G', 'G', 'C', 'A', 'A', 'G', 'G', 'C', 'A', 'C', 'A', 'C', 'A', 'G', 'G', 'G', 'G', 'A', 'T', 'A', 'G', 'G'};
      byte[] s2 = {'A', 'A', 'T', 'T', 'T', 'T', 'A', 'T', 'T', 'T', 'G', 'A', 'T', 'G', 'A', 'T', 'G', 'A', 'T', 'T', 'T', 'T', 'A', 'T', 'T', 'C', 'G', 'A', 'G', 'T', 'A', 'T', 'A', 'T', 'T', 'C', 'G', 'A', 'T', 'G', 'A', 'T', 'T', 'T', 'T', 'A', 'T', 'T', 'T', 'G', 'A', 'T', 'T', 'T', 'C', 'A', 'T', 'T', 'T', 'G', 'A', 'T', 'G', 'A', 'T', 'G', 'A', 'T', 'T', 'T', 'T', 'A', 'T', 'T', 'C', 'G', 'A', 'T', 'T', 'T', 'T', 'A', 'T', 'T', 'T', 'G', 'A', 'T', 'G', 'A', 'A', 'G', 'A', 'T', 'T', 'T', 'T', 'A', 'T', 'T', 'C', 'G', 'A', 'G', 'A', 'T', 'T', 'A', 'T', 'T', 'T', 'G', 'A', 'T', 'G', 'A', 'T', 'T', 'T', 'A', 'A', 'T', 'T'};
      SemiglobalAligner aligner = AlignerFactory.createGlobalAligner();
      //aligner.debug();
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2, alphabet);

      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // right
                                                                                            // corner
      assertEquals("Alingment ends in wrong column.", s2.length, result.getEndPosition().column); // bottom
                                                                                                  // right
                                                                                                  // corner

      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2()),
            result.getErrors());

      System.out.println(result.printAsChars());
      System.out.println(result.getErrors());
      System.out.println();
   }


   @Test
   public void testGlobalAligner9() {
      System.out.println("testGlobalAligner9()");
      byte[] s1 = {'A', 'B', 'C', 'D', 'E', 'F'};
      byte[] s2 = {'A', 'x', 'B', 'C', 'D', 'E', 'G', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x'};
      SemiglobalAligner aligner = AlignerFactory.createGlobalAligner();
      //aligner.debug();
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2, alphabet);

      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // right
                                                                                            // corner
      assertEquals("Alingment ends in wrong column.", s2.length, result.getEndPosition().column); // bottom
                                                                                                  // right
                                                                                                  // corner

      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2()),
            result.getErrors());

      System.out.println(result.printAsChars());
      System.out.println(result.getErrors());
      System.out.println();
   }


   @Test
   public void testGlobalAligner10() {
      System.out.println("testGlobalAligner10()");
      byte[] s1 = {'G', 'G', 'A', 'A', 'T', 'C', 'C', 'C'};
      byte[] s2 = {'T', 'G', 'A', 'G', 'G', 'G', 'A', 'T', 'A', 'A', 'A', 'T', 'A', 'T', 'T', 'T', 'A', 'G', 'A', 'A', 'T', 'T', 'T', 'A', 'G', 'T', 'A', 'G', 'T', 'A', 'G', 'T', 'G', 'T', 'T'};
      SemiglobalAligner aligner = AlignerFactory.createGlobalAligner();
      //aligner.debug();
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2, alphabet);

      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // right
                                                                                            // corner
      assertEquals("Alingment ends in wrong column.", s2.length, result.getEndPosition().column); // bottom
                                                                                                  // right
                                                                                                  // corner

      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2()),
            result.getErrors());

      System.out.println(result.printAsChars());
      System.out.println(result.getErrors());
      System.out.println();
   }


   @Test
   public void testGlobalAligner11() {
      System.out.println("testGlobalAligner11()");
      byte[] s1 = {'B', 'R', 'a', 'b', 'b', 'e', 'l'};
      byte[] s2 = {'b', 'r', ' ', 'a', 'b', 'b', 'e', 'l', 'r', 'a', 'b', 'a', 'b', 'b', 'e', 'l'};
      SemiglobalAligner aligner = AlignerFactory.createGlobalAligner();
      //aligner.debug();
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2, alphabet);

      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // right
                                                                                            // corner
      assertEquals("Alingment ends in wrong column.", s2.length, result.getEndPosition().column); // bottom
                                                                                                  // right
                                                                                                  // corner

      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2()),
            result.getErrors());

      System.out.println(result.printAsChars());
      System.out.println(result.getErrors());
      System.out.println();
   }


   @Test
   public void testGlobalAligner12() {
      System.out.println("testGlobalAligner12()");
      byte[] s1 = {'B', 'R', 'a', 'b', 'b', 'e', 'l'};
      byte[] s2 = {'B', 'R', 'a', 'b', 'b', 'e', 'l'};
      SemiglobalAligner aligner = AlignerFactory.createGlobalAligner();
      //aligner.debug();
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2, alphabet);

      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // right
                                                                                            // corner
      assertEquals("Alingment ends in wrong column.", s2.length, result.getEndPosition().column); // bottom
                                                                                                  // right
                                                                                                  // corner

      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2()),
            result.getErrors());

      System.out.println(result.printAsChars());
      System.out.println(result.getErrors());
      System.out.println();
   }


   @Test
   public void testGlobalAligner13() {
      System.out.println("testGlobalAligner13()");
      byte[] s1 = {'A'};
      byte[] s2 = {'T', 'C', 'T', 'G', 'C', 'T', 'C', 'C', 'T', 'G', 'G', 'C', 'C', 'C', 'A', 'T', 'G', 'A', 'T', 'C', 'G', 'T', 'A', 'T', 'A', 'A', 'C', 'T', 'T', 'T', 'C', 'A', 'A', 'A', 'T', 'T', 'T'};
      SemiglobalAligner aligner = AlignerFactory.createGlobalAligner();
      //aligner.debug();
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2, alphabet);

      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // right
                                                                                            // corner
      assertEquals("Alingment ends in wrong column.", s2.length, result.getEndPosition().column); // bottom
                                                                                                  // right
                                                                                                  // corner

      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2()),
            result.getErrors());

      System.out.println(result.printAsChars());
      System.out.println(result.getErrors());
      System.out.println();
   }


   @Test
   public void testGlobalAligner14() {
      System.out.println("testGlobalAligner14()");
      byte[] s1 = {};
      byte[] s2 = {'C', 'G', 'T', 'G', 'A', 'A', 'C', 'C', 'C', 'G', 'G', 'G', 'G', 'G', 'T', 'G', 'G', 'A', 'G', 'C', 'T', 'T', 'G', 'C', 'A', 'G', 'T', 'G'};
      SemiglobalAligner aligner = AlignerFactory.createGlobalAligner();
      //aligner.debug();
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2, alphabet);

      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // right
                                                                                            // corner
      assertEquals("Alingment ends in wrong column.", s2.length, result.getEndPosition().column); // bottom
                                                                                                  // right
                                                                                                  // corner

      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2()),
            result.getErrors());

      System.out.println(result.printAsChars());
      System.out.println(result.getErrors());
      System.out.println();
   }


   @Test
   public void testGlobalAligner15() {
      System.out.println("testGlobalAligner15()");
      byte[] s1 = {'T', 'A', 'T', 'T', 'T', 'T', 'G', 'G', 'G', 'A', 'G', 'G', 'T', 'C', 'G', 'A', 'G', 'G', 'T', 'A', 'G', 'G', 'C', 'G', 'G', 'A', 'T', 'T', 'A', 'C', 'G', 'A', 'G', 'G', 'T', 'T', 'A', 'G', 'G', 'A', 'G', 'A', 'T', 'C', 'G', 'A', 'G', 'A', 'T', 'T', 'A', 'T', 'T', 'T', 'T', 'G', 'A', 'T', 'T', 'A', 'A'};
      byte[] s2 = {'C', 'A', 'C', 'T', 'T', 'T', 'G', 'G', 'G', 'A', 'G', 'G', 'C', 'C', 'A', 'A', 'G', 'G', 'C', 'G', 'G', 'G', 'C', 'G', 'G', 'A', 'T', 'C', 'A', 'C', 'G', 'A', 'G', 'G', 'T', 'C', 'A', 'G', 'G', 'A', 'G', 'A', 'T', 'C', 'G', 'A', 'G', 'A', 'C', 'C', 'A', 'T', 'C', 'C', 'T', 'G', 'G', 'C', 'T', 'A', 'G'};
      SemiglobalAligner aligner = AlignerFactory.createGlobalAligner();
      //aligner.debug();
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2, alphabet);

      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // right
                                                                                            // corner
      assertEquals("Alingment ends in wrong column.", s2.length, result.getEndPosition().column); // bottom
                                                                                                  // right
                                                                                                  // corner

      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2()),
            result.getErrors());

      System.out.println(result.printAsChars());
      System.out.println(result.getErrors());
      System.out.println();
   }


   @Test
   public void testGlobalAligner16() {
      System.out.println("testGlobalAligner16()");
      byte[] s1 = {'A', 'A', 'A', 'A', 'C', 'C', 'T', 'A', 'T', 'C', 'C', 'C', 'G', 'G', 'G', 'A', 'G', 'G', 'T', 'C', 'G', 'A', 'G', 'G', 'T', 'A'};
      byte[] s2 = {'A', 'A', 'A', 'A', 'C', 'C', 'T', 'A', 'T', 'C', 'C', 'C', 'G', 'G', 'G', 'A', 'G', 'G', 'T', 'C', 'G', 'A', 'G', 'G', 'T', 'A', 'G', 'G', 'C', 'G', 'G', 'A', 'T', 'T', 'A', 'C', 'G', 'A', 'G', 'G', 'T', 'T', 'A', 'G', 'G', 'A', 'G', 'A', 'T', 'C', 'G', 'A', 'G', 'A', 'T', 'T', 'A', 'T', 'T', 'T', 'T', 'G', 'A', 'T', 'T', 'A', 'A'};
      SemiglobalAligner aligner = AlignerFactory.createGlobalAligner();
      //aligner.debug();
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2, alphabet);

      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // right
                                                                                            // corner
      assertEquals("Alingment ends in wrong column.", s2.length, result.getEndPosition().column); // bottom
                                                                                                  // right
                                                                                                  // corner

      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2()),
            result.getErrors());

      System.out.println(result.printAsChars());
      System.out.println(result.getErrors());
      System.out.println();
   }


   @Test
   public void testGlobalAligner17() {
      System.out.println("testGlobalAligner17()");
      byte[] s1 = {'T', 'T', 'T', 'G', 'T', 'A', 'A', 'T', 'T', 'T', 'T', 'A', 'G', 'T', 'T', 'A', 'C', 'T', 'C', 'G', 'G', 'G', 'A', 'G', 'G', 'T', 'T', 'G', 'A', 'G', 'G', 'T', 'A', 'G', 'G', 'A', 'G', 'A', 'A', 'T', 'C', 'G', 'T', 'T', 'T', 'G', 'A', 'A', 'T', 'T', 'C', 'G', 'G', 'G', 'A', 'G', 'G', 'T', 'A', 'G', 'A', 'G', 'G', 'T', 'T', 'G'};
      byte[] s2 = {'C', 'C', 'T', 'G', 'C', 'A', 'A', 'T', 'C', 'C', 'C', 'C', 'G', 'C', 'T', 'A', 'C', 'T', 'C', 'G', 'G', 'G', 'A', 'G', 'G', 'C', 'T', 'G', 'A', 'G', 'G', 'C', 'A', 'G', 'G', 'T', 'G', 'A', 'A', 'T', 'C', 'G', 'C', 'T', 'T', 'G', 'A', 'A', 'C', 'C', 'C', 'G', 'G', 'G', 'A', 'G', 'G', 'C', 'A', 'G', 'A', 'G', 'G', 'T', 'T', 'G'};
      SemiglobalAligner aligner = AlignerFactory.createGlobalAligner();
      //aligner.debug();
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2, alphabet);

      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // right
                                                                                            // corner
      assertEquals("Alingment ends in wrong column.", s2.length, result.getEndPosition().column); // bottom
                                                                                                  // right
                                                                                                  // corner

      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2()),
            result.getErrors());

      System.out.println(result.printAsChars());
      System.out.println(result.getErrors());
      System.out.println();
   }


   @Test
   public void testGlobalAligner18() {
      System.out.println("testGlobalAligner18()");
      byte[] s1 = {'A', 'B', 'C'};
      byte[] s2 = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L'};
      SemiglobalAligner aligner = AlignerFactory.createGlobalAligner();
      //aligner.debug();
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2, alphabet);

      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // right
                                                                                            // corner
      assertEquals("Alingment ends in wrong column.", s2.length, result.getEndPosition().column); // bottom
                                                                                                  // right
                                                                                                  // corner

      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2()),
            result.getErrors());

      System.out.println(result.printAsChars());
      System.out.println(result.getErrors());
      System.out.println();
   }


   @Test
   public void testGlobalAligner19() {
      System.out.println("testGlobalAligner19()");
      byte[] s1 = {'C', 'G', 'T', 'G', 'A', 'A', 'C', 'C', 'C', 'G', 'G', 'G', 'G', 'G', 'T', 'G', 'G', 'A', 'G', 'C', 'T', 'T', 'G', 'C', 'A', 'G', 'T', 'G'};
      byte[] s2 = {};
      SemiglobalAligner aligner = AlignerFactory.createGlobalAligner();
      //aligner.debug();
      SemiglobalAligner.SemiglobalAlignmentResult result = aligner.semiglobalAlign(s1, s2, alphabet);

      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // right
                                                                                            // corner
      assertEquals("Alingment ends in wrong column.", s2.length, result.getEndPosition().column); // bottom
                                                                                                  // right
                                                                                                  // corner

      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2()),
            result.getErrors());

      System.out.println(result.printAsChars());
      System.out.println(result.getErrors());
      System.out.println();
   }

}
