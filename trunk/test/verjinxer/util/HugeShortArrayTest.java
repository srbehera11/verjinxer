package verjinxer.util;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

/**
 * @author Markus Kemmerling
 * 
 */
public class HugeShortArrayTest {
   private static short[] randomValues;

   private static final long seed = 5;
   private static final long twoPowerThirty = (long) Math.pow(2, 30);
   private static final long twoPowerThirtyThree = (long) Math.pow(2, 34);

   /**
    * Generates a HugeByteArray with given length. The Array is filled with random shorts generated
    * with class Random an given seed.
    * 
    * @param seed
    *           Seed for Random
    * @param length
    * @return HugeShortArray of random content with given length.
    */
   private static HugeShortArray generateRandomArray(final long seed, final long length) {
      HugeShortArray array = new HugeShortArray(length);
      Random r = new Random(seed);
      short value;
      for (long l = 0; l < length; l++) {
         value = (short) r.nextInt();
         array.set(l, value);
      }
      return array;
   }

   /**
    * Generates a HugeShortArray with content of the given array so that the whole HugeShortArray
    * will be filled. HugeShortArray.get(l) = array[l%array.length]
    * 
    * @param array
    * @param length
    * @return HugeShortArray with the given length.
    */
   private static HugeShortArray arrayToHugeArray(final short[] array, final long length) {
      System.out.println("generate HugeShortArray");
      HugeShortArray hugeArray = new HugeShortArray(length);
      final long threshold = length / 10;
      for (long l = 0; l < length; l++) {
         if (l % threshold == 0) {
            System.out.println(String.format("%d", l / threshold * 10));
         }
         hugeArray.set(l, array[(int) (l % array.length)]);
      }
      return hugeArray;
   }

   /**
    * Puts the given values in the given array at eatch given positions
    * 
    * @param array
    *           Array to update
    * @param values
    *           Values to put in array
    * @param pos
    *           Positions where values to put in
    */
   private static void putValuesAtPositions(final HugeShortArray array, final short[] values,
         final long[] pos) {
      for (int i = 0; i < pos.length; i++) {
         for (int j = 0; j < values.length; j++) {
            array.set(pos[i] + j, values[j]);
         }
      }
   }

   /**
    * Fills the given Array with random shorts generated with class Random an given seed. If the
    * given Array has not the given length, than a new HugeShortArray is generated and filled.
    * 
    * @param array
    * @param seed
    *           Seed for Random
    * @param length
    * @return
    */
   private static HugeShortArray fillWithRandomShorts(HugeShortArray array, final long seed,
         final long length) {
      if (array.length != length) {
         array = new HugeShortArray(length);
      }

      Random r = new Random(seed);
      short value;
      for (long l = 0; l < length; l++) {
         value = (short) r.nextInt();
         array.set(l, value);
      }

      return array;
   }

   /**
    * @throws java.lang.Exception
    */
   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
      randomValues = new short[100];
      Random r = new Random(seed);
      for (int i = 0; i < randomValues.length; i++) {
         randomValues[i] = (short) r.nextInt();
      }
   }

   /**
    * @throws java.lang.Exception
    */
   @AfterClass
   public static void tearDownAfterClass() throws Exception {
   }

   /**
    * @throws java.lang.Exception
    */
   @Before
   public void setUp() throws Exception {
   }

   /**
    * @throws java.lang.Exception
    */
   @After
   public void tearDown() throws Exception {
      System.gc();
      // Thread.sleep(1000);
   }

   /**
    * Test method for {@link verjinxer.util.HugeShortArray#testHugeShortArray(long)} and
    * {@link verjinxer.util.HugeShortArray#testHugeShortArray(HugeShortArray)}.
    */
   @Test
   @Ignore
   public void testHugeShortArray() {
      final long[] length = { 0, 1, 500, (long) Math.pow(2, 29), twoPowerThirty - 1,
            twoPowerThirty, twoPowerThirty + 1, twoPowerThirty * 2 - 1, twoPowerThirty * 2,
            twoPowerThirty * 2 + 1, (long) (twoPowerThirty * 2.5) };
      final int[] bins = { 0, 1, 1, 1, 1, 1, 2, 2, 2, 3, 3 };
      HugeShortArray array;
      HugeShortArray arrayCopy;
      for (int i = 0; i < length.length; i++) {
         // System.out.println(i);
         array = new HugeShortArray(length[i]);
         assertEquals(String.format("Iteration %d: Wrong length: %d", i, array.length), length[i],
               array.length);
         assertEquals(String.format("Iteration %d: Wrong bins: %d", i, array.getBins()), bins[i],
               array.getBins());
         // test copy constructor
         // if(i < 7){ //just for testing - need more than 8GB memory
         arrayCopy = new HugeShortArray(array);
         assertEquals(String.format("Iteration %d: Wrong length: %d", i, arrayCopy.length),
               length[i], arrayCopy.length);
         assertEquals(String.format("Iteration %d: Wrong bins: %d", i, arrayCopy.getBins()),
               bins[i], arrayCopy.getBins());
         // }
         array = null;
         arrayCopy = null;
         System.gc();

         try {
            Thread.sleep(1000);
         } catch (InterruptedException e) {
            e.printStackTrace();
         }
      }
   }

   /**
    * Test method for {@link verjinxer.util.HugeShortArray#get(long)}.
    */
   @Test
   @Ignore
   public void testGet1() {
      HugeShortArray arrayTwoPowerThirty = new HugeShortArray(twoPowerThirty);
      long pos[] = { 0, twoPowerThirty / 4, twoPowerThirty / 4 * 3,
            twoPowerThirty - randomValues.length };
      putValuesAtPositions(arrayTwoPowerThirty, randomValues, pos);
      testValuesAtPositions(arrayTwoPowerThirty, randomValues, pos, twoPowerThirty);
   }

   /**
    * Test method for {@link verjinxer.util.HugeShortArray#get(long)}.
    */
   @Test
   @Ignore
   public void testGet2() {
      HugeShortArray arrayTwoPowerThirtyMinusOne = new HugeShortArray(twoPowerThirty - 1);
      long pos[] = { 0, twoPowerThirty / 4, twoPowerThirty / 4 * 3,
            twoPowerThirty - 1 - randomValues.length };
      putValuesAtPositions(arrayTwoPowerThirtyMinusOne, randomValues, pos);
      testValuesAtPositions(arrayTwoPowerThirtyMinusOne, randomValues, pos, twoPowerThirty - 1);
   }

   /**
    * Test method for {@link verjinxer.util.HugeShortArray#get(long)}.
    */
   @Test
   @Ignore
   public void testGet3() {
      HugeShortArray arrayTwoPowerThirtyPlusOne = new HugeShortArray(twoPowerThirty + 1);
      long pos[] = { 0, twoPowerThirty / 4, twoPowerThirty / 4 * 3,
            twoPowerThirty + 1 - randomValues.length };
      putValuesAtPositions(arrayTwoPowerThirtyPlusOne, randomValues, pos);
      testValuesAtPositions(arrayTwoPowerThirtyPlusOne, randomValues, pos, twoPowerThirty + 1);
   }

   /**
    * Test method for {@link verjinxer.util.HugeShortArray#get(long)}.
    */
   @Test
   // @Ignore //needs more than 16GB memory
   public void testGet4() {
      HugeShortArray arrayTwoPowerThirtyThree = new HugeShortArray(twoPowerThirtyThree);
      long pos[] = { 0, twoPowerThirty / 4, twoPowerThirty / 4 * 3,
            twoPowerThirty - (randomValues.length / 2), (long) (twoPowerThirty * 1.5),
            twoPowerThirty * 2 - (randomValues.length / 2), (long) (twoPowerThirty * 3),
            twoPowerThirty * 4 - (randomValues.length / 2), (long) (twoPowerThirty * 5),
            twoPowerThirtyThree - randomValues.length };
      putValuesAtPositions(arrayTwoPowerThirtyThree, randomValues, pos);
      testValuesAtPositions(arrayTwoPowerThirtyThree, randomValues, pos, twoPowerThirtyThree);
   }

   /**
    * Test method for {@link verjinxer.util.HugeShortArray#get(long)}.
    */
   @Test
   // runs with 13BG memory
   @Ignore
   // need more than 8GB memory
   public void testGet5() {
      final long length = 4000005023l;
      HugeShortArray mrd5 = new HugeShortArray(length);
      long pos[] = { 0, twoPowerThirty * 2 - (randomValues.length / 2), length / 8, length / 4,
            length / 2, length / 4 * 3, length - randomValues.length };
      putValuesAtPositions(mrd5, randomValues, pos);
      testValuesAtPositions(mrd5, randomValues, pos, length);
   }

   /**
    * Generates a sequence with given length of random shorts with class Random and given seed. Test
    * if the content of the given Array is equal to the random sequence
    * 
    * @param array
    * @param seed
    *           Seed for Random
    */
   private static void testRandomArray(final HugeShortArray array, final long seed,
         final long length) {
      assertEquals("Array has not the right length", array.length, length);

      Random r = new Random(seed);
      short value;
      for (long l = 0; l < array.length; l++) {
         value = (short) r.nextInt();
         assertEquals(String.format("Error at position %d", l), array.get(l), value);
      }
   }

   /**
    * Tests if the array has the given length and if the given values are at the given positions in
    * the array
    * 
    * @param array
    * @param values
    * @param pos
    *           positions
    * @param length
    */
   private static void testValuesAtPositions(final HugeShortArray array, final short[] values,
         final long[] pos, final long length) {
      assertEquals("Array has not the right length", array.length, length);
      for (long l : pos) {
         for (int i = 0; i < values.length; i++) {
            assertEquals(String.format("Error at position %d", l + i), array.get(l + i), values[i]);
         }
      }
   }

   /**
    * Test if array.get(l) = values[ l % values.length ]
    * 
    * @param array
    * @param length
    * @param values
    */
   private static void testRandomArray(final HugeShortArray array, final long length,
         final short[] values) {
      System.out.println("test HugeShortArray");
      assertEquals("Array has not the right length", array.length, length);
      final long threshold = length / 10;
      for (long l = 0; l < length; l++) {
         if (l % threshold == 0) {
            System.out.println(String.format("%d", l / threshold * 10));
         }
         assertEquals(String.format("Error at position %d", l), array.get(l),
               values[(int) (l % values.length)]);
      }
   }

   /**
    * Test method for {@link verjinxer.util.HugeShortArray#get(long)}.
    */
   @Test(expected = ArrayIndexOutOfBoundsException.class)
   @Ignore
   public void testGetToLow1() {
      HugeShortArray epsilon = new HugeShortArray(0);
      epsilon.get(0);
   }

   /**
    * Test method for {@link verjinxer.util.HugeShortArray#set(long,short)}.
    */
   @Test(expected = ArrayIndexOutOfBoundsException.class)
   @Ignore
   public void testSetToLow1() {
      HugeShortArray epsilon = new HugeShortArray(0);
      epsilon.set(0, (short) 6);
   }

   /**
    * Test method for {@link verjinxer.util.HugeShortArray#get(long)}.
    */
   @Test(expected = ArrayIndexOutOfBoundsException.class)
   @Ignore
   public void testGetToHigh1() {
      HugeShortArray epsilon = new HugeShortArray(0);
      epsilon.get(1);
   }

   /**
    * Test method for {@link verjinxer.util.HugeShortArray#set(long,short)}.
    */
   @Test(expected = ArrayIndexOutOfBoundsException.class)
   @Ignore
   public void testSetToHigh1() {
      HugeShortArray epsilon = new HugeShortArray(0);
      epsilon.set(1, (short) 6);
   }

   /**
    * Test method for {@link verjinxer.util.HugeShortArray#get(long)}.
    */
   @Test(expected = ArrayIndexOutOfBoundsException.class)
   @Ignore
   public void testGetToLow2() {
      HugeShortArray arrayTwoPowerThirty = new HugeShortArray(twoPowerThirty);
      arrayTwoPowerThirty.get(-1);
   }

   /**
    * Test method for {@link verjinxer.util.HugeShortArray#set(long,short)}.
    */
   @Test(expected = ArrayIndexOutOfBoundsException.class)
   @Ignore
   public void testSetToLow2() {
      HugeShortArray array = new HugeShortArray(twoPowerThirty + 100);
      array.set(-1, (short) 6);
   }

   /**
    * Test method for {@link verjinxer.util.HugeShortArray#get(long)}.
    */
   @Test(expected = ArrayIndexOutOfBoundsException.class)
   @Ignore
   public void testGetToHigh2() {
      HugeShortArray array = new HugeShortArray(twoPowerThirty + 100);
      array.get(twoPowerThirty + 100);
   }

   /**
    * Test method for {@link verjinxer.util.HugeShortArray#set(long,short)}.
    */
   @Test(expected = ArrayIndexOutOfBoundsException.class)
   @Ignore
   public void testSetToHigh2() {
      HugeShortArray array = new HugeShortArray(twoPowerThirty);
      array.set(twoPowerThirty, (short) 6);
   }

   /**
    * Test method for {@link verjinxer.util.HugeShortArray#get(long)}.
    */
   @Test(expected = ArrayIndexOutOfBoundsException.class)
   @Ignore
   public void testGetToLow3() {
      HugeShortArray array = new HugeShortArray(500);
      array.get(-1000);
   }

   /**
    * Test method for {@link verjinxer.util.HugeShortArray#set(long,short)}.
    */
   @Test(expected = ArrayIndexOutOfBoundsException.class)
   @Ignore
   public void testSetToLow3() {
      HugeShortArray array = new HugeShortArray(512);
      array.set(Long.MIN_VALUE + 1000, (short) 6);
   }

   /**
    * Test method for {@link verjinxer.util.HugeShortArray#get(long)}.
    */
   @Test(expected = ArrayIndexOutOfBoundsException.class)
   @Ignore
   public void testGetToHigh3() {
      HugeShortArray array = new HugeShortArray(500);
      array.get(Long.MAX_VALUE);
   }

   /**
    * Test method for {@link verjinxer.util.HugeShortArray#set(long,short)}.
    */
   @Test(expected = ArrayIndexOutOfBoundsException.class)
   @Ignore
   public void testSetToHigh3() {
      HugeShortArray array = new HugeShortArray(500);
      array.set(Long.MAX_VALUE, (short) 6);
   }

   /**
    * Test method for {@link verjinxer.util.HugeShortArray#fill(short)}.
    */
   @Test
   @Ignore
   public void testFill1() {
      HugeShortArray arrayTwoPowerThirty = new HugeShortArray(twoPowerThirty);

      Random r = new Random(seed);
      int p;
      short value;
      for (int i = 0; i < 10; i++) {
         p = r.nextInt((int) twoPowerThirty);
         value = (short) r.nextInt();
         arrayTwoPowerThirty.set(p, value);
      }

      arrayTwoPowerThirty.fill((short) 6);
      short[] test = new short[100];
      Arrays.fill(test, (short) 6);
      // testFill(arrayTwoPowerThirty, twoPowerThirty, (short)6 );
      final long[] pos = { 0, twoPowerThirty / 4, twoPowerThirty / 2, twoPowerThirty / 4 * 3,
            twoPowerThirty - test.length };
      testValuesAtPositions(arrayTwoPowerThirty, test, pos, twoPowerThirty);
   }

   /**
    * Test method for {@link verjinxer.util.HugeShortArray#fill(short)}.
    */
   @Test
   @Ignore
   public void testFill2() {
      HugeShortArray arrayTwoPowerThirtyMinusOne = new HugeShortArray(twoPowerThirty - 1);

      Random r = new Random(seed);
      int p;
      short value;
      for (int i = 0; i < 10; i++) {
         p = r.nextInt((int) twoPowerThirty - 1);
         value = (short) r.nextInt();
         arrayTwoPowerThirtyMinusOne.set(p, value);
      }

      arrayTwoPowerThirtyMinusOne.fill((short) (-1));
      // testFill(arrayTwoPowerThirtyMinusOne, twoPowerThirty-1, (short)(-1) );
      short[] test = new short[100];
      Arrays.fill(test, (short) (-1));
      final long[] pos = { 0, twoPowerThirty / 4, twoPowerThirty / 2, twoPowerThirty / 4 * 3,
            twoPowerThirty - 1 - test.length };
      testValuesAtPositions(arrayTwoPowerThirtyMinusOne, test, pos, twoPowerThirty - 1);
   }

   /**
    * Test method for {@link verjinxer.util.HugeShortArray#fill(short)}.
    */
   @Test
   @Ignore
   public void testFill3() {
      HugeShortArray arrayTwoPowerThirtyPlusOne = new HugeShortArray(twoPowerThirty + 1);

      Random r = new Random(seed);
      int p;
      short value;
      for (int i = 0; i < 10; i++) {
         p = r.nextInt((int) twoPowerThirty + 1);
         value = (short) r.nextInt();
         arrayTwoPowerThirtyPlusOne.set(p, value);
      }

      arrayTwoPowerThirtyPlusOne.fill(Short.MAX_VALUE);
      // testFill(arrayTwoPowerThirtyPlusOne, twoPowerThirty+1 , Short.MAX_VALUE );
      short[] test = new short[100];
      Arrays.fill(test, Short.MAX_VALUE);
      final long[] pos = { 0, twoPowerThirty / 4, twoPowerThirty / 2, twoPowerThirty / 4 * 3,
            twoPowerThirty + 1 - test.length };
      testValuesAtPositions(arrayTwoPowerThirtyPlusOne, test, pos, twoPowerThirty + 1);
   }

   /**
    * Test method for {@link verjinxer.util.HugeShortArray#fill(short)}.
    */
   @Test
   // @Ignore //needs more than 16GB memory
   public void testFill4() {
      HugeShortArray arrayTwoPowerThirtyThree = new HugeShortArray(twoPowerThirtyThree);

      Random r = new Random(seed);
      int p;
      short value;
      for (int i = 0; i < 10; i++) {
         p = r.nextInt(Integer.MAX_VALUE);
         value = (short) r.nextInt();
         arrayTwoPowerThirtyThree.set(p, value);
      }

      arrayTwoPowerThirtyThree.fill(Short.MIN_VALUE);
      // testFill(arrayTwoPowerThirtyThree, twoPowerThirtyThree, Short.MIN_VALUE );
      short[] test = new short[100];
      Arrays.fill(test, Short.MIN_VALUE);
      final long[] pos = { 0, twoPowerThirty - (test.length / 2), (long) (twoPowerThirty * 1.5),
            twoPowerThirty * 2 - (test.length / 2), (long) (twoPowerThirty * 3),
            twoPowerThirty * 4 - (test.length / 2), (long) (twoPowerThirty * 5),
            twoPowerThirtyThree / 8, twoPowerThirtyThree / 4, twoPowerThirtyThree / 2,
            twoPowerThirtyThree / 4 * 3, twoPowerThirtyThree - test.length };
      testValuesAtPositions(arrayTwoPowerThirtyThree, test, pos, twoPowerThirtyThree);
   }

   /**
    * Test method for {@link verjinxer.util.HugeShortArray#fill(short)}.
    */
   @Test
   @Ignore
   // needs more than 8GB memory
   public void testFill5() {
      final long length = 4000005023l;
      HugeShortArray mrd4 = new HugeShortArray(length);

      Random r = new Random(seed);
      int p;
      short value;
      for (int i = 0; i < 10; i++) {
         p = r.nextInt(Integer.MAX_VALUE);
         value = (short) r.nextInt();
         mrd4.set(p, value);
      }

      mrd4.fill(Short.MIN_VALUE);
      // testFill(arrayTwoPowerThirtyThree, twoPowerThirtyThree, Short.MIN_VALUE );
      short[] test = new short[100];
      Arrays.fill(test, Short.MIN_VALUE);
      long pos[] = { 0, twoPowerThirty * 2 - (test.length / 2), length / 8, length / 4, length / 2,
            length / 4 * 3, length - test.length };
      testValuesAtPositions(mrd4, test, pos, length);
   }

   private static void testFill(final HugeShortArray array, final long length, final short value) {
      assertEquals("Array has not the right length", array.length, length);

      for (long l = 0; l < array.length; l++) {
         assertEquals(String.format("Error at position %d", l), array.get(l), value);
      }
   }

   /**
    * Test method for {@link verjinxer.util.HugeShortArray#sort()}.
    */
   @Test
   @Ignore
   public void testSort1() {
      HugeShortArray arrayTwoPowerThirty = generateRandomArray(seed, twoPowerThirty);
      arrayTwoPowerThirty.sort();
      final int n = 100;
      final long[] pos = { 0, twoPowerThirty / 4, twoPowerThirty / 2, twoPowerThirty / 4 * 3,
            twoPowerThirty - n };
      testSort(arrayTwoPowerThirty, twoPowerThirty, pos, n);
   }

   /**
    * Test method for {@link verjinxer.util.HugeShortArray#sort()}.
    */
   @Test
   @Ignore
   public void testSort2() {
      HugeShortArray arrayTwoPowerThirtyMinusOne = generateRandomArray(seed, twoPowerThirty - 1);
      arrayTwoPowerThirtyMinusOne.sort();
      final int n = 100;
      final long[] pos = { 0, twoPowerThirty / 4, twoPowerThirty / 2, twoPowerThirty / 4 * 3,
            twoPowerThirty - 1 - n };
      testSort(arrayTwoPowerThirtyMinusOne, twoPowerThirty - 1, pos, n);
   }

   /**
    * Test method for {@link verjinxer.util.HugeShortArray#sort()}.
    */
   @Test
   @Ignore
   public void testSort3() {
      HugeShortArray arrayTwoPowerThirtyPlusOne = generateRandomArray(seed, twoPowerThirty + 1);
      arrayTwoPowerThirtyPlusOne.sort();
      final int n = 100;
      final long[] pos = { 0, twoPowerThirty / 4, twoPowerThirty / 2, twoPowerThirty / 4 * 3,
            twoPowerThirty + 1 - n };
      testSort(arrayTwoPowerThirtyPlusOne, twoPowerThirty + 1, pos, n);
   }

   /**
    * Test method for {@link verjinxer.util.HugeShortArray#sort()}.
    */
   @Test
   // @Ignore //needs more than 16GB memory
   public void testSort4() {
      HugeShortArray arrayTwoPowerThirtyThree = generateRandomArray(seed, twoPowerThirtyThree);
      arrayTwoPowerThirtyThree.sort();
      final int n = 100;
      final long[] pos = { 0, twoPowerThirty - n / 2, (long) (twoPowerThirty * 1.5),
            twoPowerThirty * 2 - n / 2, (long) (twoPowerThirty * 3), twoPowerThirty * 4 - n / 2,
            (long) (twoPowerThirty * 5), twoPowerThirtyThree / 8, twoPowerThirtyThree / 4,
            twoPowerThirtyThree / 2, twoPowerThirtyThree / 4 * 3, twoPowerThirtyThree - n };
      testSort(arrayTwoPowerThirtyThree, twoPowerThirtyThree, pos, n);
   }

   /**
    * Test the whole array if it is sorted
    * 
    * @param array
    * @param length
    *           Expected length of array
    */
   private static void testSort(final HugeShortArray array, final long length) {
      assertEquals("Array has not the right length", array.length, length);

      for (long l = 0; l < array.length - 1; l++) {
         assertTrue(String.format("Error at position %d", l), array.get(l) <= array.get(l + 1));
      }
   }

   /**
    * Test, beginning from each given pos, the next n positions if they are sorted.
    * 
    * @param array
    * @param length
    *           Expected length of array
    * @param pos
    * @param n
    */
   private static void testSort(final HugeShortArray array, final long length, final long[] pos,
         final int n) {
      assertEquals("Array has not the right length", array.length, length);
      for (long l : pos) {
         for (int i = 0; i < n - 1; i++) {
            assertTrue(String.format("Error at position %d", l), array.get(l + i) <= array.get(l
                  + i + 1));
         }
      }
   }

   /**
    * Test method for {@link verjinxer.util.HugeShortArray#sort(long, long)}.
    */
   @Test
   @Ignore
   public void testSortLongLong1() {
      final long start = twoPowerThirty - 1500;
      final int length = 3000;

      HugeShortArray arrayTwoPowerThirtyOne = new HugeShortArray(twoPowerThirty * 2);
      final long pos[] = { 0, start, twoPowerThirty * 2 - length };
      final short values[] = new short[length];
      Random r = new Random(seed);
      for (int i = 0; i < values.length; i++) {
         values[i] = (short) r.nextInt();
      }
      putValuesAtPositions(arrayTwoPowerThirtyOne, values, pos);

      // test if it can sort at the beginning, over bucket boundaries and at the end
      for (long p : pos) {
         arrayTwoPowerThirtyOne.sort(p, length);
         assertEquals("Array has not the right length", arrayTwoPowerThirtyOne.length,
               twoPowerThirty * 2);
         for (long l = p; l < p + length - 1; l++) {
            assertTrue(String.format("Error at position %d", l),
                  arrayTwoPowerThirtyOne.get(l) <= arrayTwoPowerThirtyOne.get(l + 1));
         }
      }
   }

   /**
    * Test method for {@link verjinxer.util.HugeShortArray#sort(long, long)}.
    */
   @Test
   @Ignore
   public void testSortLongLong2() {
      // test if it does only sort the asked positions.
      final long twoPowerSixteen = (long) Math.pow(2, 16);
      HugeShortArray array = new HugeShortArray(twoPowerSixteen);
      short value = Short.MAX_VALUE;
      for (long l = 0; l < twoPowerSixteen; l++, value--) {
         array.set(l, value);
      }
      final long twoPowerTen = (long) Math.pow(2, 10);
      array.sort(twoPowerTen, twoPowerTen);
      testSortLongLong(array, twoPowerSixteen, twoPowerTen, twoPowerTen);

      // TODO Test what happens with bad parameters off and len
   }

   public static void main(String[] args) {
      HugeShortArrayTest t = new HugeShortArrayTest();
      randomValues = new short[100];
      Random r = new Random(seed);
      for (int i = 0; i < randomValues.length; i++) {
         randomValues[i] = (short) r.nextInt();
      }
      t.testSwap1();
   }

   /**
    * Test if the given array is sorted at positions start to start+sortedLength-1 an unsorted else.
    * No test are applied to the cut interfaces (start-1, start) and (start+sortedLength-1,
    * start+sortedLength).
    * 
    * @param array
    *           Array to test
    * @param arrayLength
    *           Expected length of array
    * @param start
    *           First position at which the array is sorted
    * @param sortedLength
    *           Length of the sphere which is expected to be sorted.
    */
   private void testSortLongLong(final HugeShortArray array, final long arrayLength,
         final long start, final long sortedLength) {
      assertEquals("Array has not the right length", array.length, arrayLength);
      long l = 0;
      for (; l < start - 1; l++) {
         assertFalse(String.format("Error at position %d", l), array.get(l) <= array.get(l + 1));
      }

      for (l++; l < start + sortedLength - 1; l++) {
         assertTrue(String.format("Error at position %d", l), array.get(l) <= array.get(l + 1));
      }

      for (l++; l < arrayLength - 1; l++) {
         assertFalse(String.format("Error at position %d", l), array.get(l) <= array.get(l + 1));
      }

   }

   /**
    * Test method for {@link verjinxer.util.HugeShortArray#binarySearch(short)}.
    */
   @Test
   @Ignore
   public void testBinarySearchShort() {
      final long twoPowerTen = (long) Math.pow(2, 10);
      HugeShortArray arrayTwoPowerTen = generateRandomArray(seed, twoPowerTen);
      arrayTwoPowerTen.sort();
      // random test
      short randomeValue;
      Random r = new Random(seed + 174);
      long pos;
      for (int i = 0; i < 10; i++) {
         randomeValue = (short) r.nextInt();
         pos = arrayTwoPowerTen.binarySearch(randomeValue);
         if (pos >= 0) {
            assertEquals(String.format("Error at position %d with value %d. Expected value %d ",
                  pos, arrayTwoPowerTen.get(pos), randomeValue), randomeValue,
                  arrayTwoPowerTen.get(pos));
         } else {
            pos *= (-1);
            pos -= 1;
            assertTrue(String.format("Error at position %d", pos),
                  arrayTwoPowerTen.get(pos) > randomeValue);
            assertTrue(String.format("Error at position %d", pos),
                  arrayTwoPowerTen.get(pos - 1) < randomeValue);
         }
      }

      // deterministic test
      HugeShortArray array = new HugeShortArray(110);
      pos = 0;
      for (short s = -100; s <= -6; s += 2) {
         array.set(pos++, s);
      }
      array.set(pos++, (short) -5);
      for (short s = -4; s <= 6; s += 2) {
         array.set(pos++, s);
      }
      for (short s = 0; s < 9; s++) {
         array.set(pos++, (short) 7);
      }
      for (short s = 8; s < 100; s += 2) {
         array.set(pos++, s);
      }

      // search one occurrence
      pos = array.binarySearch((short) -5);
      assertEquals("Could not find -5", 48, pos);

      pos = array.binarySearch((short) -100);
      assertEquals("Could not find -100", 0, pos);

      pos = array.binarySearch((short) 98);
      assertEquals("Could not find 98", 109, pos);

      // search multiple occurrence
      pos = array.binarySearch((short) 7);
      assertTrue("Could not find 7", 55 <= pos && pos <= 63);

      // search value not in the array
      pos = array.binarySearch((short) 1);
      pos = pos * -1;
      pos = pos - 1;
      assertEquals("Could not find position for 1. Says " + pos, 52, pos);

      pos = array.binarySearch((short) -101);
      pos = pos * -1;
      pos = pos - 1;
      assertEquals("Could not find position for -101. Says " + pos, 0, pos);

      pos = array.binarySearch((short) 101);
      pos = pos * -1;
      pos = pos - 1;
      assertEquals("Could not find position for 101. Says " + pos, 110, pos);
   }

   /**
    * Test method for {@link verjinxer.util.HugeShortArray#binarySearch(short, long, long)}.
    */
   @Test
   @Ignore
   public void testBinarySearchShortLongLong() {
      // test like unbounded at bin boundary
      HugeShortArray array = new HugeShortArray(twoPowerThirty + 55);
      final long tmp = Math.min(twoPowerThirty + 55 - randomValues.length, twoPowerThirty
            - randomValues.length / 2);
      putValuesAtPositions(array, randomValues, new long[] { tmp });
      array.sort(tmp, randomValues.length);
      // random test
      short randomeValue;
      Random r = new Random(seed + 174);
      long pos;
      for (int i = 0; i < 10; i++) {
         randomeValue = (short) r.nextInt();
         pos = array.binarySearch(randomeValue, tmp, tmp + randomValues.length);
         if (pos >= 0) {
            assertEquals(String.format("Error at position %d with value %d. Expected value %d ",
                  pos, array.get(pos), randomeValue), randomeValue, array.get(pos));
         } else {
            pos *= (-1);
            pos -= 1;
            assertTrue(String.format("Error at position %d", pos), array.get(pos) > randomeValue);
            assertTrue(String.format("Error at position %d", pos),
                  array.get(pos - 1) < randomeValue);
         }
      }

      // deterministic test

      array.fill((short) 1);
      pos = twoPowerThirty - 55;
      for (short s = -100; s <= -6; s += 2) {
         array.set(pos++, s);
      }
      array.set(pos++, (short) -5);
      for (short s = -4; s <= 6; s += 2) {
         array.set(pos++, s);
      }
      for (short s = 0; s < 9; s++) {
         array.set(pos++, (short) 7);
      }
      for (short s = 8; s < 100; s += 2) {
         array.set(pos++, s);
      }

      // search one occurrence
      pos = array.binarySearch((short) -5, twoPowerThirty - 55, twoPowerThirty + 55);
      assertEquals("Could not find -5", twoPowerThirty - 55 + 48, pos);

      pos = array.binarySearch((short) 6, twoPowerThirty - 55, twoPowerThirty + 55);
      assertEquals("Could not find 6", twoPowerThirty - 55 + 54, pos);

      pos = array.binarySearch((short) -100, twoPowerThirty - 55, twoPowerThirty + 55);
      assertEquals("Could not find -100", twoPowerThirty - 55 + 0, pos);

      pos = array.binarySearch((short) 98, twoPowerThirty - 55, twoPowerThirty + 55);
      assertEquals("Could not find 98", twoPowerThirty - 55 + 109, pos);

      // search multiple occurrence
      pos = array.binarySearch((short) 7, twoPowerThirty - 55, twoPowerThirty + 55);
      assertTrue("Could not find 7", twoPowerThirty - 55 + 55 <= pos
            && pos <= twoPowerThirty - 55 + 63);

      // search value not in the array
      pos = array.binarySearch((short) 1, twoPowerThirty - 55, twoPowerThirty + 55); // 1 occurs in
                                                                                     // array
                                                                                     // beneath
                                                                                     // lower bound
      pos = pos * -1;
      pos = pos - 1;
      assertEquals("Could not find position for 1. Says " + pos, twoPowerThirty - 55 + 52, pos);

      pos = array.binarySearch((short) -101, twoPowerThirty - 55, twoPowerThirty + 55);
      pos = pos * -1;
      pos = pos - 1;
      assertEquals("Could not find position for -101. Says " + pos, twoPowerThirty - 55 + 0, pos);

      pos = array.binarySearch((short) 101, twoPowerThirty - 55, twoPowerThirty + 55);
      pos = pos * -1;
      pos = pos - 1;
      assertEquals("Could not find position for 101. Says " + pos, twoPowerThirty - 55 + 110, pos);
   }

   /**
    * Test method for {@link verjinxer.util.HugeShortArray#swap(long, long)}.
    */
   @Test
   @Ignore
   public void testSwap1() {
      HugeShortArray array = new HugeShortArray(twoPowerThirty * 2 + 300);
      final long pos[] = { 0, twoPowerThirty - randomValues.length / 2,
            twoPowerThirty * 2 - randomValues.length / 2,
            twoPowerThirty * 2 + 300 - randomValues.length,
            154 - randomValues.length / 2 >= 0 ? 154 - randomValues.length / 2 : 0,
            63544 - randomValues.length / 2 >= 0 ? 63544 - randomValues.length / 2 : 0,
            199287 - randomValues.length / 2 >= 0 ? 199287 - randomValues.length / 2 : 0,
            7654345 - randomValues.length / 2 >= 0 ? 7654345 - randomValues.length / 2 : 0, };

      putValuesAtPositions(array, randomValues, pos);

      short s1, s2;
      long[] pos1 = { 0, twoPowerThirty, twoPowerThirty, twoPowerThirty, twoPowerThirty - 1, 154,
            7654345 };
      long[] pos2 = { twoPowerThirty * 2 + 300 - 1, twoPowerThirty + 1, twoPowerThirty * 2,
            twoPowerThirty - 1, twoPowerThirty + 1, 199287, 63544 };
      // test if two values swap positions
      for (int i = 0; i < pos1.length; i++) {
         s1 = array.get(pos1[i]);
         s2 = array.get(pos2[i]);
         array.swap(pos1[i], pos2[i]);
         assertEquals(String.format("Error while swapping positions %d and $d.", pos1[i], pos2[i]),
               s1, array.get(pos2[i]));
         assertEquals(String.format("Error while swapping positions %d and $d.", pos1[i], pos2[i]),
               s2, array.get(pos1[i]));
      }
   }

   /**
    * Test method for {@link verjinxer.util.HugeShortArray#swap(long, long)}.
    */
   @Test
   @Ignore
   public void testSwap2() {
      // test if all other positions are unmodified
      HugeShortArray array1 = generateRandomArray(seed, 500);
      HugeShortArray array2 = generateRandomArray(seed, 500); // both arrays are equal

      long[] pos1 = new long[] { 0, 50, 0, 499, 256 };
      long[] pos2 = new long[] { 499, 51, 403, 134, 16 };

      for (int i = 0; i < pos1.length; i++) {
         array1.swap(pos1[i], pos2[i]);
         assertEquals(String.format("Error while swapping positions %d and $d.", pos1[i], pos2[i]),
               array1.get(pos1[i]), array2.get(pos2[i]));
         assertEquals(String.format("Error while swapping positions %d and $d.", pos1[i], pos2[i]),
               array1.get(pos2[i]), array2.get(pos1[i]));
         // rest unmodified?
         for (long l = 0; l < array1.length; l++) {
            if (l != pos1[i] && l != pos2[i])
               assertEquals(String.format("Error, positions %d was modified.", l), array1.get(l),
                     array2.get(l));
         }

         // revoke changes in array1
         array1 = fillWithRandomShorts(array1, seed, 500);
      }

      // TODO test what happens when positions to swap are not in array
   }

   /**
    * Test method for {@link verjinxer.util.HugeShortArray#copy()}.
    */
   @Test
   @Ignore
   public void testCopy() {
      // test returned array is not the same but equal in length an each position
      HugeShortArray array = new HugeShortArray(0);
      HugeShortArray arrayCopy = array.copy();
      assertFalse("Error, copied array is the same", arrayCopy == array);
      assertEquals(String.format("Error, arrays have different length: %d, %d.", arrayCopy.length,
            array.length), arrayCopy.length, array.length);
      assertEquals(String.format("Error, arrays have different bins: %d, %d.", arrayCopy.getBins(),
            array.getBins()), arrayCopy.getBins(), array.getBins());

      array = generateRandomArray(seed, twoPowerThirty + 300);
      arrayCopy = array.copy();

      assertFalse("Error, copied array is the same", arrayCopy == array); // TODO better change one
                                                                          // and test if the other
                                                                          // remains unchanged
      assertEquals(String.format("Error, arrays have different length: %d, %d.", arrayCopy.length,
            array.length), arrayCopy.length, array.length);
      assertEquals(String.format("Error, arrays have different bins: %d, %d.", arrayCopy.getBins(),
            array.getBins()), arrayCopy.getBins(), array.getBins());
      for (long l = 0; l < arrayCopy.length; l++) {
         assertEquals(String.format("Error at position %d.", l), arrayCopy.get(l), array.get(l));
      }
   }

   /**
    * Test method for {@link verjinxer.util.HugeShortArray#copyRange(long, long)}.
    */
   @Test
   @Ignore
   public void testCopyRange() {
      // test returned array is not the same but equal in length an each position
      final long length = twoPowerThirty + 1030;
      final long[] from = { 0, twoPowerThirty - 100, length - 200 };
      final long[] to = { 200, twoPowerThirty + 100, length - 1 };
      HugeShortArray array = new HugeShortArray(length);
      Random r = new Random(seed);
      short value;
      for (int i = 0; i < from.length; i++)
         for (long pos = from[i] - 5; pos <= to[i] + 5; pos++) {
            if (pos < 0)
               pos = 0;
            else if (pos >= array.length)
               continue;
            value = (short) r.nextInt();
            array.set(pos, value);
         }

      HugeShortArray arrayCopy;
      for (int i = 0; i < from.length; i++) {
         arrayCopy = array.copyRange(from[i], to[i]);

         assertEquals(String.format("Error, array has wrong length: %d, %d.", arrayCopy.length,
               to[i] - from[i]), arrayCopy.length, to[i] - from[i]);
         for (long l = 0; l < arrayCopy.length; l++) {
            assertEquals(String.format("Error at position %d.", l), arrayCopy.get(l), array.get(l
                  + from[i]));
         }
         arrayCopy = null;
         System.gc();
      }
   }

}