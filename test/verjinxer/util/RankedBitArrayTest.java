package verjinxer.util;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Markus Kemmerling
 */
public class RankedBitArrayTest {

   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
   }

   @AfterClass
   public static void tearDownAfterClass() throws Exception {
   }

   @Test
   public void testGetBits() {
      RankedBitArray rba = new RankedBitArray(12);
      rba.set(5, 1);
      rba.set(6, 1);
      rba.set(7, 1);
      assertEquals(7, rba.getBits(5, 8));
      assertEquals(7, rba.getBits(5, 10));
      assertEquals(3, rba.getBits(6, 9));
      assertEquals(28, rba.getBits(3, 9));
      assertEquals(0, rba.getBits(9, 9));
      assertEquals(0, rba.getBits(9, 5));

      rba = new RankedBitArray(48);
      rba.set(30, 1);
      rba.set(31, 1);
      rba.set(32, 1);
      rba.set(33, 1);
      assertEquals(15, rba.getBits(30, 34));
      assertEquals(15, rba.getBits(30, 36));
      assertEquals(7, rba.getBits(31, 36));
      assertEquals(60, rba.getBits(28, 34));
      assertEquals(0, rba.getBits(31, 31));
      assertEquals(0, rba.getBits(32, 30));
      assertEquals(-536870912, rba.getBits(1, 38));

      for (int i = 0; i < 48; i++) {
         rba.set(i, 1);
      }

      assertEquals(-1, rba.getBits(0, 32));
      assertEquals(-1, rba.getBits(16, 48));
      assertEquals(-1, rba.getBits(16, 50));
      assertEquals(-1, rba.getBits(10, 50));

   }

}
