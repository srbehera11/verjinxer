package verjinxer.sequenceanalysis;

import static org.junit.Assert.*;

import org.junit.Test;

import verjinxer.sequenceanalysis.alignment.AlignerFactory;
import verjinxer.sequenceanalysis.alignment.Aligner;
import verjinxer.sequenceanalysis.alignment.AlignmentResult;

/**
 * @author Markus Kemmerling
 */
public class ForwardAlignmentTest {
   
   public static final byte GAP = Aligner.GAP; 
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
    * Compares each position of the two given arrays ignoring the gaps at the end of query and
    * counts the mismatches.
    * 
    * @param query
    * @param reference
    * @param gapsAtEnd
    *           Number of gaps at the end of query.
    * @return
    */
   private static int countErrorsAndTestEnd(byte[] query, byte[] reference, int gapsAtEnd) {
      int errors = 0;
      int i = query.length-1;
      // cause it is a ForwardAlignment, we only need to look till the end of the query
      while (i >= 0 && query[i]==GAP) {
         i--;
      }
      assertEquals("Alignment ends at the wrong position", gapsAtEnd, query.length - i);
      
      for ( ; i >= 0; i--) { 
         if (query[i] != reference[i]) {
            errors++;
         }
      }
      return errors;
   }
   
   @Test
   public void testForwardAligner1() {
      byte[] s1 = {'S', 'I', 'S', 'S', 'I'};
      byte[] s2 = {'M', 'I', 'S', 'S', 'I', 'S', 'S', 'I', 'P', 'P', 'I'};
      Aligner aligner = AlignerFactory.createForwardAligner();
      AlignmentResult result = aligner.align(s1, s2, alphabet);
      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // edge
      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2(),
            s2.length + 1 - result.getEndPosition().column), result.getErrors());

      byte[] r1 = {'S','I','S','S','I',GAP,GAP,GAP,GAP,GAP,GAP};
      byte[] r2 = {'M','I','S','S','I','S','S','I','P','P','I'};
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getErrors(), 1);
   }


   @Test
   public void testForwardAligner2() {
      byte[] s1 = {' ', 'l', 'a', 'm', 'e', 'n'};
      byte[] s2 = {'S', 'e', 'h', 'r', ' ', 'g', 'e', 'e', 'h', 'r', 't', 'e', ' ', 'D', 'a', 'm', 'e', 'n', ' ', 'u', 'n', 'd', ' ', 'h', 'e', 'r', 'r', 'e', 'n', '.', ' ', 'W', 'i', 'r', ' ', 'h', 'a', 'b', 'e', 'n', ' ', 'u', 'n', 's', ' ', 'h', 'e', 'u', 't', 'e', ' ', 'h', 'i', 'e', 'r', ' ', 'i', 'n', ' ', 'K', 'a', 'm', 'e', 'n', ' ', 'v', 'e', 'r', 's', 'a', 'm', 'm', 'e', 'l', 't', '.'};
      Aligner aligner = AlignerFactory.createForwardAligner();
      AlignmentResult result = aligner.align(s1, s2, alphabet);
      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // edge
      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2(),
            s2.length + 1 - result.getEndPosition().column), result.getErrors());

      byte[] r1 = {' ','l','a','m','e','n',GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP};
      byte[] r2 = {GAP,GAP,GAP,'S','e','h','r',' ','g','e','e','h','r','t','e',' ','D','a','m','e','n',' ','u','n','d',' ','h','e','r','r','e','n','.',' ','W','i','r',' ','h','a','b','e','n',' ','u','n','s',' ','h','e','u','t','e',' ','h','i','e','r',' ','i','n',' ','K','a','m','e','n',' ','v','e','r','s','a','m','m','e','l','t','.'};
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getErrors(), 5);
   }


   @Test
   public void testForwardAligner3() {
      byte[] s1 = {'T', 'G', 'A', 'G', 'A', 'C', 'A', 'C', 'G', 'C', 'A', 'A', 'C', 'A', 'T', 'G', 'G', 'G', 'A', 'A', 'A', 'G', 'G', 'C', 'A', 'A', 'G', 'G', 'C', 'A', 'C', 'A', 'C', 'A', 'G', 'G', 'G', 'G', 'A', 'T', 'A', 'G', 'G'};
      byte[] s2 = {'A', 'A', 'T', 'T', 'T', 'A', 'T', 'T', 'T', 'T', 'A', 'T', 'T', 'G', 'T', 'G', 'A', 'T', 'T', 'T', 'T', 'T', 'T', 'G', 'G', 'A', 'G', 'G', 'T', 'T', 'T', 'G', 'G', 'A', 'A', 'G', 'C', 'C', 'A', 'C', 'T', 'A', 'A', 'G', 'C', 'T', 'A', 'T', 'A', 'C', 'T', 'G', 'A', 'G', 'A', 'C', 'A', 'C', 'G', 'C', 'A', 'A', 'C', 'A', 'G', 'G', 'G', 'G', 'A', 'A', 'A', 'G', 'G', 'C', 'A', 'A', 'G', 'G', 'C', 'A', 'C', 'A'};
      Aligner aligner = AlignerFactory.createForwardAligner();
      AlignmentResult result = aligner.align(s1, s2, alphabet);
      
      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // edge
      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2(),
            s2.length + 1 - result.getEndPosition().column), result.getErrors());

      byte[] r1 = {'T','G','A','G','A','C','A','C','G','C','A','A','C','A','T','G','G','G','A','A','A','G','G','C','A','A','G','G','C','A','C','A','C','A','G','G','G','G','A','T','A','G','G',GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP};
      byte[] r2 = {GAP,'A','A','T','T','T','A','T','T','T','T','A',GAP,'T','T','G','T','G',GAP,'A','T','T','T','T','T','T','G','G',GAP,'A',GAP,'G','G','T','T','T','G','G','A',GAP,'A','G','C','C','A','C','T','A','A','G','C','T','A','T','A','C','T','G','A','G','A','C','A','C','G','C','A','A','C','A','G','G','G','G','A','A','A','G','G','C','A','A','G','G','C','A','C','A'};
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getErrors(), 28);
   }


   @Test
   public void testForwardAligner4() {
      byte[] s1 = {'T', 'C', 'C', 'A', 'T', 'C', 'T', 'C', 'A', 'T', 'C', 'C', 'C', 'T', 'G', 'C', 'G', 'T', 'G', 'T', 'C', 'C', 'C', 'A', 'T', 'C', 'T', 'G', 'T', 'T', 'C', 'C', 'C', 'T', 'C', 'C', 'C', 'T', 'G', 'T', 'C', 'T', 'C', 'A'};
      byte[] s2 = {'T', 'T', 'T', 'T', 'A', 'G', 'G', 'A', 'A', 'A', 'T', 'A', 'C', 'G', 'C', 'C', 'T', 'G', 'G', 'T', 'G', 'G', 'G', 'G', 'T', 'T', 'T', 'G', 'G', 'A', 'G', 'T', 'A', 'T', 'A', 'G', 'T', 'G', 'A', 'A', 'A', 'G', 'A', 'T', 'A', 'G', 'G', 'T', 'G', 'A', 'G', 'T', 'T', 'G', 'G', 'T', 'C', 'G', 'G', 'G', 'T', 'G'};
      Aligner aligner = AlignerFactory.createForwardAligner();
      AlignmentResult result = aligner.align(s1, s2, alphabet);
      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // edge
      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2(),
            s2.length + 1 - result.getEndPosition().column), result.getErrors());

      byte[] r1 = {GAP,'T','C','C','A','T','C','T','C','A','T',GAP,'C',GAP,'C','C','T','G','C','G','T','G','T','C','C','C','A','T','C','T','G','T','T','C','C','C','T','C','C','C','T','G','T','C','T','C','A',GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP};
      byte[] r2 = {'T','T','T','T','A','G','G','A','A','A','T','A','C','G','C','C','T','G',GAP,'G','T','G',GAP,GAP,GAP,GAP,GAP,GAP,'G','G','G','T','T',GAP,GAP,GAP,'T',GAP,'G','G','A','G','T','A','T',GAP,'A','G','T','G','A','A','A','G','A','T','A','G','G','T','G','A','G','T','T','G','G','T','C','G','G','G','T','G'};
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getErrors(), 27);
   }


   @Test
   public void testForwardAligner5() {
      byte[] s1 = {'T', 'C', 'T', 'G', 'T', 'T', 'C', 'C', 'C', 'T', 'C', 'C', 'C', 'T', 'G', 'T', 'C', 'T', 'C', 'A'};
      byte[] s2 = {'T', 'T', 'T', 'T', 'A', 'G', 'G', 'A', 'A', 'A', 'T', 'A', 'C', 'G', 'C', 'C'};
      Aligner aligner = AlignerFactory.createForwardAligner();
      AlignmentResult result = aligner.align(s1, s2, alphabet);
      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // edge
      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2(),
            s2.length + 1 - result.getEndPosition().column), result.getErrors());

      byte[] r1 = {'T','C','T','G','T','T','C','C','C','T','C','C','C','T','G','T','C','T','C','A'};
      byte[] r2 = {'T',GAP,'T',GAP,'T','T',GAP,'A','G','G','A','A','A','T',GAP,'A','C','G','C','C'};
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getErrors(), 13);
   }


   @Test
   public void testForwardAligner6() {
      byte[] s1 = {'T', 'G', 'A', 'G', 'A', 'C', 'A', 'C', 'G', 'C', 'A', 'A', 'C', 'A', 'G', 'G', 'G', 'G', 'A', 'A', 'A', 'G', 'G', 'C', 'A', 'A', 'G', 'G', 'C', 'A', 'C', 'A', 'C', 'A', 'G', 'G', 'G', 'G', 'A', 'T', 'A', 'G', 'G'};
      byte[] s2 = {'A', 'A', 'T', 'T', 'T', 'A', 'T', 'T', 'T', 'T', 'A', 'T', 'T', 'G', 'T', 'G', 'A', 'T', 'T', 'T', 'T', 'T', 'T', 'G', 'G', 'A', 'G', 'G', 'T', 'T', 'T', 'G', 'G', 'A', 'A', 'G', 'C', 'C', 'A', 'C', 'T', 'A', 'A', 'G', 'C', 'T', 'A', 'T', 'A', 'C', 'T', 'G', 'A', 'G', 'A', 'C', 'A', 'C', 'G', 'C', 'A', 'A', 'C', 'A', 'G', 'G', 'G', 'G', 'A', 'A', 'A', 'G', 'G', 'C', 'A', 'A', 'G', 'G', 'C', 'A', 'C', 'A', 'C', 'A', 'G', 'G', 'G', 'G', 'A', 'T', 'A', 'G', 'G'};
      Aligner aligner = AlignerFactory.createForwardAligner();
      AlignmentResult result = aligner.align(s1, s2, alphabet);
      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // edge
      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2(),
            s2.length + 1 - result.getEndPosition().column), result.getErrors());

      byte[] r1 = {'T','G','A','G','A','C','A','C','G','C','A','A','C','A','G','G','G','G','A','A','A','G','G','C','A','A','G','G','C','A','C','A','C','A','G','G','G','G','A','T','A','G','G',GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP};
      byte[] r2 = {GAP,'A','A','T','T','T','A','T','T','T','T','A',GAP,'T','T','G','T','G',GAP,'A','T','T','T','T','T','T','G','G',GAP,'A',GAP,'G','G','T','T','T','G','G','A',GAP,'A','G','C','C','A','C','T','A','A','G','C','T','A','T','A','C','T','G','A','G','A','C','A','C','G','C','A','A','C','A','G','G','G','G','A','A','A','G','G','C','A','A','G','G','C','A','C','A','C','A','G','G','G','G','A','T','A','G','G'};
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getErrors(), 29);
   }


   @Test
   public void testForwardAligner7() {
      byte[] s1 = {'T', 'G', 'A', 'G', 'A', 'C', 'A', 'C', 'G', 'C', 'A', 'A', 'C', 'A', 'G', 'G', 'G', 'G', 'A', 'A', 'A', 'G', 'G', 'C', 'A', 'A', 'G', 'G', 'C', 'A', 'C', 'A', 'C', 'A', 'G', 'G', 'G', 'G', 'A', 'T', 'A', 'G', 'G'};
      byte[] s2 = {'T', 'G', 'A', 'G', 'A', 'C', 'A', 'C', 'G', 'C', 'A', 'A', 'C', 'A', 'G', 'G', 'G', 'G', 'A', 'A', 'A', 'G', 'G', 'C', 'A', 'A', 'G', 'G', 'C', 'A', 'C', 'A', 'C', 'A', 'G', 'G', 'G', 'G', 'A', 'T', 'A', 'G', 'G'};
      Aligner aligner = AlignerFactory.createForwardAligner();
      AlignmentResult result = aligner.align(s1, s2, alphabet);
      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // edge
      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2(),
            s2.length + 1 - result.getEndPosition().column), result.getErrors());

      byte[] r1 = {'T','G','A','G','A','C','A','C','G','C','A','A','C','A','G','G','G','G','A','A','A','G','G','C','A','A','G','G','C','A','C','A','C','A','G','G','G','G','A','T','A','G','G'};
      byte[] r2 = {'T','G','A','G','A','C','A','C','G','C','A','A','C','A','G','G','G','G','A','A','A','G','G','C','A','A','G','G','C','A','C','A','C','A','G','G','G','G','A','T','A','G','G'};
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getErrors(), 0);
   }


   @Test
   public void testForwardAligner8() {
      byte[] s1 = {'T', 'G', 'A', 'G', 'A', 'C', 'A', 'C', 'G', 'C', 'A', 'A', 'C', 'A', 'G', 'G', 'G', 'G', 'A', 'A', 'A', 'G', 'G', 'C', 'A', 'A', 'G', 'G', 'C', 'A', 'C', 'A', 'C', 'A', 'G', 'G', 'G', 'G', 'A', 'T', 'A', 'G', 'G'};
      byte[] s2 = {'A', 'A', 'T', 'T', 'T', 'T', 'A', 'T', 'T', 'T', 'G', 'A', 'T', 'G', 'A', 'T', 'G', 'A', 'T', 'T', 'T', 'T', 'A', 'T', 'T', 'C', 'G', 'A', 'G', 'T', 'A', 'T', 'A', 'T', 'T', 'C', 'G', 'A', 'T', 'G', 'A', 'T', 'T', 'T', 'T', 'A', 'T', 'T', 'T', 'G', 'A', 'T', 'T', 'T', 'C', 'A', 'T', 'T', 'T', 'G', 'A', 'T', 'G', 'A', 'T', 'G', 'A', 'T', 'T', 'T', 'T', 'A', 'T', 'T', 'C', 'G', 'A', 'T', 'T', 'T', 'T', 'A', 'T', 'T', 'T', 'G', 'A', 'T', 'G', 'A', 'A', 'G', 'A', 'T', 'T', 'T', 'T', 'A', 'T', 'T', 'C', 'G', 'A', 'G', 'A', 'T', 'T', 'A', 'T', 'T', 'T', 'G', 'A', 'T', 'G', 'A', 'T', 'T', 'T', 'A', 'A', 'T', 'T'};
      Aligner aligner = AlignerFactory.createForwardAligner();
      AlignmentResult result = aligner.align(s1, s2, alphabet);
      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // edge
      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2(),
            s2.length + 1 - result.getEndPosition().column), result.getErrors());

      byte[] r1 = {'T','G','A','G','A','C','A','C','G','C','A','A','C','A','G','G','G','G','A','A','A','G','G','C','A','A','G','G','C','A','C','A','C','A','G','G','G','G','A','T','A','G','G',GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP};
      byte[] r2 = {GAP,GAP,'A',GAP,'A',GAP,'T','T','T','T','A','T','T','T','G','A','T','G',GAP,GAP,'A','T','G',GAP,'A','T','T','T','T','A','T','T','C',GAP,'G','A','G','T','A','T','A','T','T','C','G','A','T','G','A','T','T','T','T','A','T','T','T','G','A','T','T','T','C','A','T','T','T','G','A','T','G','A','T','G','A','T','T','T','T','A','T','T','C','G','A','T','T','T','T','A','T','T','T','G','A','T','G','A','A','G','A','T','T','T','T','A','T','T','C','G','A','G','A','T','T','A','T','T','T','G','A','T','G','A','T','T','T','A','A','T','T'};
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getErrors(), 28);
   }


   @Test
   public void testForwardAligner9() {
      byte[] s1 = {'A', 'B', 'C', 'D', 'E', 'F'};
      byte[] s2 = {'A', 'x', 'B', 'C', 'D', 'E', 'G', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x', 'x'};
      Aligner aligner = AlignerFactory.createForwardAligner();
      AlignmentResult result = aligner.align(s1, s2, alphabet);
      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // edge
      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2(),
            s2.length + 1 - result.getEndPosition().column), result.getErrors());

      byte[] r1 = {'A',GAP,'B','C','D','E','F',GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP};
      byte[] r2 = {'A','x','B','C','D','E','G','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x'};
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getErrors(), 2);
   }


   @Test
   public void testForwardAligner10() {
      byte[] s1 = {'G', 'G', 'A', 'A', 'T', 'C', 'C', 'C'};
      byte[] s2 = {'T', 'G', 'A', 'G', 'G', 'G', 'A', 'T', 'A', 'A', 'A', 'T', 'A', 'T', 'T', 'T', 'A', 'G', 'A', 'A', 'T', 'T', 'T', 'A', 'G', 'T', 'A', 'G', 'T', 'A', 'G', 'T', 'G', 'T', 'T'};
      Aligner aligner = AlignerFactory.createForwardAligner();
      AlignmentResult result = aligner.align(s1, s2, alphabet);
      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // edge
      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2(),
            s2.length + 1 - result.getEndPosition().column), result.getErrors());

      byte[] r1 = {'G','G','A','A','T','C','C','C',GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP};
      byte[] r2 = {'T','G','A','G','G','G','A','T','A','A','A','T','A','T','T','T','A','G','A','A','T','T','T','A','G','T','A','G','T','A','G','T','G','T','T'};
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getErrors(), 6);
   }


   @Test
   public void testForwardAligner11() {
      byte[] s1 = {'B', 'R', 'a', 'b', 'b', 'e', 'l'};
      byte[] s2 = {'b', 'r', ' ', 'a', 'b', 'b', 'e', 'l', 'r', 'a', 'b', 'a', 'b', 'b', 'e', 'l'};
      Aligner aligner = AlignerFactory.createForwardAligner();
      AlignmentResult result = aligner.align(s1, s2, alphabet);
      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // edge
      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2(),
            s2.length + 1 - result.getEndPosition().column), result.getErrors());

      byte[] r1 = {GAP,'B','R','a','b','b','e','l',GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP};
      byte[] r2 = {'b','r',' ','a','b','b','e','l','r','a','b','a','b','b','e','l'};
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getErrors(), 3);
   }


   @Test
   public void testForwardAligner12() {
      byte[] s1 = {'B', 'R', 'a', 'b', 'b', 'e', 'l'};
      byte[] s2 = {'B', 'R', 'a', 'b', 'b', 'e', 'l'};
      Aligner aligner = AlignerFactory.createForwardAligner();
      AlignmentResult result = aligner.align(s1, s2, alphabet);
      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // edge
      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2(),
            s2.length + 1 - result.getEndPosition().column), result.getErrors());

      byte[] r1 = {'B','R','a','b','b','e','l'};
      byte[] r2 = {'B','R','a','b','b','e','l'};
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getErrors(), 0);
   }


   @Test
   public void testForwardAligner13() {
      byte[] s1 = {'A'};
      byte[] s2 = {'T', 'C', 'T', 'G', 'C', 'T', 'C', 'C', 'T', 'G', 'G', 'C', 'C', 'C', 'A', 'T', 'G', 'A', 'T', 'C', 'G', 'T', 'A', 'T', 'A', 'A', 'C', 'T', 'T', 'T', 'C', 'A', 'A', 'A', 'T', 'T', 'T'};
      Aligner aligner = AlignerFactory.createForwardAligner();
      AlignmentResult result = aligner.align(s1, s2, alphabet);
      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // edge
      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2(),
            s2.length + 1 - result.getEndPosition().column), result.getErrors());

      byte[] r1 = {'A',GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP};
      byte[] r2 = {'T','C','T','G','C','T','C','C','T','G','G','C','C','C','A','T','G','A','T','C','G','T','A','T','A','A','C','T','T','T','C','A','A','A','T','T','T'};
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getErrors(), 1);
   }


   @Test
   public void testForwardAligner14() {
      byte[] s1 = {};
      byte[] s2 = {'C', 'G', 'T', 'G', 'A', 'A', 'C', 'C', 'C', 'G', 'G', 'G', 'G', 'G', 'T', 'G', 'G', 'A', 'G', 'C', 'T', 'T', 'G', 'C', 'A', 'G', 'T', 'G'};
      Aligner aligner = AlignerFactory.createForwardAligner();
      AlignmentResult result = aligner.align(s1, s2, alphabet);
      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // edge
      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2(),
            s2.length + 1 - result.getEndPosition().column), result.getErrors());

      byte[] r1 = {GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP};
      byte[] r2 = {'C','G','T','G','A','A','C','C','C','G','G','G','G','G','T','G','G','A','G','C','T','T','G','C','A','G','T','G'};
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getErrors(), 0);
   }


   @Test
   public void testForwardAligner15() {
      byte[] s1 = {'T', 'A', 'T', 'T', 'T', 'T', 'G', 'G', 'G', 'A', 'G', 'G', 'T', 'C', 'G', 'A', 'G', 'G', 'T', 'A', 'G', 'G', 'C', 'G', 'G', 'A', 'T', 'T', 'A', 'C', 'G', 'A', 'G', 'G', 'T', 'T', 'A', 'G', 'G', 'A', 'G', 'A', 'T', 'C', 'G', 'A', 'G', 'A', 'T', 'T', 'A', 'T', 'T', 'T', 'T', 'G', 'A', 'T', 'T', 'A', 'A'};
      byte[] s2 = {'C', 'A', 'C', 'T', 'T', 'T', 'G', 'G', 'G', 'A', 'G', 'G', 'C', 'C', 'A', 'A', 'G', 'G', 'C', 'G', 'G', 'G', 'C', 'G', 'G', 'A', 'T', 'C', 'A', 'C', 'G', 'A', 'G', 'G', 'T', 'C', 'A', 'G', 'G', 'A', 'G', 'A', 'T', 'C', 'G', 'A', 'G', 'A', 'C', 'C', 'A', 'T', 'C', 'C', 'T', 'G', 'G', 'C', 'T', 'A', 'G'};
      Aligner aligner = AlignerFactory.createForwardAligner();
      AlignmentResult result = aligner.align(s1, s2, alphabet);
      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // edge
      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2(),
            s2.length + 1 - result.getEndPosition().column), result.getErrors());

      byte[] r1 = {'T','A','T','T','T','T','G','G','G','A','G','G','T','C','G','A','G','G','T','A','G','G','C','G','G','A','T','T','A','C','G','A','G','G','T','T','A','G','G','A','G','A','T','C','G','A','G','A','T','T','A','T','T','T','T','G','A','T','T','A','A'};
      byte[] r2 = {'C','A','C','T','T','T','G','G','G','A','G','G','C','C','A','A','G','G','C','G','G','G','C','G','G','A','T','C','A','C','G','A','G','G','T','C','A','G','G','A','G','A','T','C','G','A','G','A','C','C','A','T','C','C','T','G','G','C','T','A','G'};
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getErrors(), 15);
   }


   @Test
   public void testForwardAligner16() {
      byte[] s1 = {'A', 'A', 'A', 'A', 'C', 'C', 'T', 'A', 'T', 'C', 'C', 'C', 'G', 'G', 'G', 'A', 'G', 'G', 'T', 'C', 'G', 'A', 'G', 'G', 'T', 'A'};
      byte[] s2 = {'A', 'A', 'A', 'A', 'C', 'C', 'T', 'A', 'T', 'C', 'C', 'C', 'G', 'G', 'G', 'A', 'G', 'G', 'T', 'C', 'G', 'A', 'G', 'G', 'T', 'A', 'G', 'G', 'C', 'G', 'G', 'A', 'T', 'T', 'A', 'C', 'G', 'A', 'G', 'G', 'T', 'T', 'A', 'G', 'G', 'A', 'G', 'A', 'T', 'C', 'G', 'A', 'G', 'A', 'T', 'T', 'A', 'T', 'T', 'T', 'T', 'G', 'A', 'T', 'T', 'A', 'A'};
      Aligner aligner = AlignerFactory.createForwardAligner();
      AlignmentResult result = aligner.align(s1, s2, alphabet);
      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // edge
      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2(),
            s2.length + 1 - result.getEndPosition().column), result.getErrors());

      byte[] r1 = {'A','A','A','A','C','C','T','A','T','C','C','C','G','G','G','A','G','G','T','C','G','A','G','G','T','A',GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP};
      byte[] r2 = {'A','A','A','A','C','C','T','A','T','C','C','C','G','G','G','A','G','G','T','C','G','A','G','G','T','A','G','G','C','G','G','A','T','T','A','C','G','A','G','G','T','T','A','G','G','A','G','A','T','C','G','A','G','A','T','T','A','T','T','T','T','G','A','T','T','A','A'};
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getErrors(), 0);
   }


   @Test
   public void testForwardAligner17() {
      byte[] s1 = {'T', 'T', 'T', 'G', 'T', 'A', 'A', 'T', 'T', 'T', 'T', 'A', 'G', 'T', 'T', 'A', 'C', 'T', 'C', 'G', 'G', 'G', 'A', 'G', 'G', 'T', 'T', 'G', 'A', 'G', 'G', 'T', 'A', 'G', 'G', 'A', 'G', 'A', 'A', 'T', 'C', 'G', 'T', 'T', 'T', 'G', 'A', 'A', 'T', 'T', 'C', 'G', 'G', 'G', 'A', 'G', 'G', 'T', 'A', 'G', 'A', 'G', 'G', 'T', 'T', 'G'};
      byte[] s2 = {'C', 'C', 'T', 'G', 'C', 'A', 'A', 'T', 'C', 'C', 'C', 'C', 'G', 'C', 'T', 'A', 'C', 'T', 'C', 'G', 'G', 'G', 'A', 'G', 'G', 'C', 'T', 'G', 'A', 'G', 'G', 'C', 'A', 'G', 'G', 'T', 'G', 'A', 'A', 'T', 'C', 'G', 'C', 'T', 'T', 'G', 'A', 'A', 'C', 'C', 'C', 'G', 'G', 'G', 'A', 'G', 'G', 'C', 'A', 'G', 'A', 'G', 'G', 'T', 'T', 'G'};
      Aligner aligner = AlignerFactory.createForwardAligner();
      AlignmentResult result = aligner.align(s1, s2, alphabet);
      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // edge
      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2(),
            s2.length + 1 - result.getEndPosition().column), result.getErrors());

      byte[] r1 = {'T','T','T','G','T','A','A','T','T','T','T','A','G','T','T','A','C','T','C','G','G','G','A','G','G','T','T','G','A','G','G','T','A','G','G','A','G','A','A','T','C','G','T','T','T','G','A','A','T','T','C','G','G','G','A','G','G','T','A','G','A','G','G','T','T','G'};
      byte[] r2 = {'C','C','T','G','C','A','A','T','C','C','C','C','G','C','T','A','C','T','C','G','G','G','A','G','G','C','T','G','A','G','G','C','A','G','G','T','G','A','A','T','C','G','C','T','T','G','A','A','C','C','C','G','G','G','A','G','G','C','A','G','A','G','G','T','T','G'};
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getErrors(), 15);
   }


   @Test
   public void testForwardAligner18() {
      byte[] s1 = {'A', 'B', 'C'};
      byte[] s2 = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L'};
      Aligner aligner = AlignerFactory.createForwardAligner();
      AlignmentResult result = aligner.align(s1, s2, alphabet);
      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // edge
      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2(),
            s2.length + 1 - result.getEndPosition().column), result.getErrors());

      byte[] r1 = {'A','B','C',GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP};
      byte[] r2 = {'A','B','C','D','E','F','G','H','I','J','K','L'};
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getErrors(), 0);
   }


   @Test
   public void testForwardAligner19() {
      byte[] s1 = {'C', 'G', 'T', 'G', 'A', 'A', 'C', 'C', 'C', 'G', 'G', 'G', 'G', 'G', 'T', 'G', 'G', 'A', 'G', 'C', 'T', 'T', 'G', 'C', 'A', 'G', 'T', 'G'};
      byte[] s2 = {};
      Aligner aligner = AlignerFactory.createForwardAligner();
      AlignmentResult result = aligner.align(s1, s2, alphabet);
      assertEquals("Alingment begins in wrong row.", 0, result.getBeginPosition().row); // left top
                                                                                        // corner
      assertEquals("Alingment begins in wrong column.", 0, result.getBeginPosition().column); // left
                                                                                              // top
                                                                                              // corner
      assertEquals("Alingment ends in wrong row.", s1.length, result.getEndPosition().row); // bottom
                                                                                            // edge
      equalWithoutGAPs(s1, result.getSequence1());
      equalWithoutGAPs(s2, result.getSequence2());
      noOppositeGaps(result.getSequence1(), result.getSequence2());
      assertEquals(countErrorsAndTestEnd(result.getSequence1(), result.getSequence2(),
            s2.length + 1 - result.getEndPosition().column), result.getErrors());

      byte[] r1 = {'C','G','T','G','A','A','C','C','C','G','G','G','G','G','T','G','G','A','G','C','T','T','G','C','A','G','T','G'};
      byte[] r2 = {GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP,GAP};
      assertArrayEquals(result.getSequence1(), r1);
      assertArrayEquals(result.getSequence2(), r2);
      assertEquals(result.getErrors(), 28);
   }

}
