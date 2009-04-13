
package verjinxer.util;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Random;


/**
 * @author Markus Kemmerling
 *
 */
public class HugeShortArrayTest {
	
	private static HugeShortArray epsilon; 						//emtpy Array: length 0
	private static HugeShortArray arrayTwoPowerThirty;			//length 2^30
	private static HugeShortArray arrayTwoPowerThirtyMinusOne;	//length 2^30 - 1
	private static HugeShortArray arrayTwoPowerThirtyPlusOne;	//length 2^30 + 1
	private static HugeShortArray arrayTwoPowerThirtyThree;		//length 2^33
	
	private static final long seed = 5;
	private static final long twoPowerThirty = (long) Math.pow(2, 30);
	private static final long twoPowerThirtyThree = (long) Math.pow(2, 34);

	/**
	 * Generates a HugeByteArray with given length.
	 * The Array is filled with random shorts generated with class Random an given seed.
	 * @param seed Seed for Random
	 * @param length
	 * @return HugeShortArray of random content with given length.
	 */
	private static HugeShortArray generateRandomArray(final long seed, final long length){
		HugeShortArray array = new HugeShortArray(length);
		Random r = new Random(seed);
		short value;
		for(long l = 0; l < length; l++){
			value = (short)r.nextInt();
			array.set(l, value);
		}
		return array;
	}
	
	/**
	 * Fills the given Array with random shorts generated with class Random an given seed.
	 * If the given Array has not the given length, than a new HugeShortArray is generated and filled.
	 * @param array
	 * @param seed Seed for Random
	 * @param length
	 * @return
	 */
	private static HugeShortArray fillWithRandomShorts(HugeShortArray array, final long seed, final long length){
		if(array.length != length){
			array = new HugeShortArray(length);
		}
		
		Random r = new Random(seed);
		short value;
		for(long l = 0; l < length; l++){
			value = (short)r.nextInt();
			array.set(l, value);
		}
		
		return array;
	}
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
//		epsilon = new HugeShortArray(0);
		arrayTwoPowerThirty = new HugeShortArray( twoPowerThirty );
		arrayTwoPowerThirtyMinusOne = new HugeShortArray( twoPowerThirty - 1 );
		arrayTwoPowerThirtyPlusOne = new HugeShortArray( twoPowerThirty + 1 );
		arrayTwoPowerThirtyThree = new HugeShortArray( twoPowerThirtyThree );
		
//		twoPowerThirty = generateRandomArray(seed, l); 
//		twoPowerThirtyMinusOne = generateRandomArray(seed, l-1 );
//		twoPowerThirtyPlusOne = generateRandomArray(seed, l+1 );
//		twoPowerThirtyThree = generateRandomArray(seed, (long) Math.pow(2, 33) );
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
		epsilon = new HugeShortArray(0);
		arrayTwoPowerThirty = fillWithRandomShorts(arrayTwoPowerThirty, seed, twoPowerThirty );
		arrayTwoPowerThirtyMinusOne = fillWithRandomShorts(arrayTwoPowerThirtyMinusOne, seed, twoPowerThirty-1 );
		arrayTwoPowerThirtyPlusOne = fillWithRandomShorts(arrayTwoPowerThirtyPlusOne, seed, twoPowerThirty+1 );
		arrayTwoPowerThirtyThree = fillWithRandomShorts(arrayTwoPowerThirtyThree, seed, twoPowerThirtyThree ); //TODO fill not complete
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		System.gc();
	}

	/**
	 * Test method for {@link verjinxer.util.HugeShortArray#get(long)}.
	 */
	@Test
	public void testGet() {
		testRandomArray(arrayTwoPowerThirty, seed, twoPowerThirty);
		testRandomArray(arrayTwoPowerThirtyMinusOne, seed, twoPowerThirty-1);
		testRandomArray(arrayTwoPowerThirtyPlusOne, seed, twoPowerThirty+1);
		testRandomArray(arrayTwoPowerThirtyThree, seed, twoPowerThirtyThree); //TODO test not all positions
	}

	/**
	 * Generates a sequence with given length of random shorts with class Random and given seed.
	 * Test if the content of the given Array is equal to the random sequence
	 * @param array
	 * @param seed Seed for Random
	 */
	private static void testRandomArray(final HugeShortArray array, final long seed, final long length){
		assertEquals("Array has not the right length",array.length, length);
		
		Random r = new Random(seed);
		short value;
		for(long l = 0; l < array.length; l++){
			value = (short)r.nextInt();
			assertEquals(String.format("Error at position %d",l),array.get(l), value);
		}
	}
	
	/**
	 * Test method for {@link verjinxer.util.HugeShortArray#get(long)}.
	 */
	@Test ( expected = ArrayIndexOutOfBoundsException.class )
	public void testGetToLow1(){
//		HugeShortArray array = new HugeShortArray(0);
//		array.get(0);
		epsilon.get(0);
	}
	
	/**
	 * Test method for {@link verjinxer.util.HugeShortArray#set(long,short)}.
	 */
	@Test ( expected = ArrayIndexOutOfBoundsException.class )
	public void testSetToLow1(){
//		HugeShortArray array = new HugeShortArray(0);
//		array.set( 0, (short)6);
		epsilon.set( 0, (short)6);
	}
	
	/**
	 * Test method for {@link verjinxer.util.HugeShortArray#get(long)}.
	 */
	@Test ( expected = ArrayIndexOutOfBoundsException.class )
	public void testGetToHigh1(){
//		HugeShortArray array = new HugeShortArray(0);
//		array.get(1);
		epsilon.get(1);
	}
	
	/**
	 * Test method for {@link verjinxer.util.HugeShortArray#set(long,short)}.
	 */
	@Test ( expected = ArrayIndexOutOfBoundsException.class )
	public void testSetToHigh1(){
//		HugeShortArray array = new HugeShortArray(0);
//		array.set( 1, (short)6);
		epsilon.set( 1, (short)6);
	}
	
	/**
	 * Test method for {@link verjinxer.util.HugeShortArray#get(long)}.
	 */
	@Test ( expected = ArrayIndexOutOfBoundsException.class )
	public void testGetToLow2(){
//		final long length = (long) Math.pow(2, 60);
//		HugeShortArray array = new HugeShortArray(length);
//		array.get(-1);
		arrayTwoPowerThirtyThree.get(-1);
	}
	
	/**
	 * Test method for {@link verjinxer.util.HugeShortArray#set(long,short)}.
	 */
	@Test ( expected = ArrayIndexOutOfBoundsException.class )
	public void testSetToLow2(){
//		final long length = (long) Math.pow(2, 60);
//		HugeShortArray array = new HugeShortArray(length);
//		array.set(-1, (short)6 );
		arrayTwoPowerThirtyThree.set(-1, (short)6 );
	}
	
	/**
	 * Test method for {@link verjinxer.util.HugeShortArray#get(long)}.
	 */
	@Test ( expected = ArrayIndexOutOfBoundsException.class )
	public void testGetToHigh2(){
//		final long length = (long) Math.pow(2, 60);
//		HugeShortArray array = new HugeShortArray(length);
//		array.get(length);
		arrayTwoPowerThirtyThree.get(twoPowerThirtyThree);
	}
	
	/**
	 * Test method for {@link verjinxer.util.HugeShortArray#set(long,short)}.
	 */
	@Test ( expected = ArrayIndexOutOfBoundsException.class )
	public void testSetToHigh2(){
//		final long length = (long) Math.pow(2, 60);
//		HugeShortArray array = new HugeShortArray(length);
//		array.set(length, (short)6 );
		arrayTwoPowerThirtyThree.set( twoPowerThirtyThree, (short)6 );
	}

	/**
	 * Test method for {@link verjinxer.util.HugeShortArray#get(long)}.
	 */
	@Test ( expected = ArrayIndexOutOfBoundsException.class )
	public void testGetToLow3(){
//		final long length = (long) Math.pow(2, 60);
//		HugeShortArray array = new HugeShortArray(length);
//		array.get(Long.MIN_VALUE);
		arrayTwoPowerThirtyThree.get(Long.MIN_VALUE);
	}
	
	/**
	 * Test method for {@link verjinxer.util.HugeShortArray#set(long,short)}.
	 */
	@Test ( expected = ArrayIndexOutOfBoundsException.class )
	public void testSetToLow3(){
//		final long length = (long) Math.pow(2, 60);
//		HugeShortArray array = new HugeShortArray(length);
//		array.set(Long.MIN_VALUE, (short)6 );
		arrayTwoPowerThirtyThree.set(Long.MIN_VALUE, (short)6 );
	}
	
	/**
	 * Test method for {@link verjinxer.util.HugeShortArray#get(long)}.
	 */
	@Test ( expected = ArrayIndexOutOfBoundsException.class )
	public void testGetToHigh3(){
//		final long length = (long) Math.pow(2, 60);
//		HugeShortArray array = new HugeShortArray(length);
//		array.get(Long.MAX_VALUE);
		arrayTwoPowerThirtyThree.get(Long.MAX_VALUE);
	}
	
	/**
	 * Test method for {@link verjinxer.util.HugeShortArray#set(long,short)}.
	 */
	@Test ( expected = ArrayIndexOutOfBoundsException.class )
	public void testSetToHigh3(){
//		final long length = (long) Math.pow(2, 60);
//		HugeShortArray array = new HugeShortArray(length);
//		array.set(Long.MAX_VALUE, (short)6 );
		arrayTwoPowerThirtyThree.set(Long.MAX_VALUE, (short)6 );
	}
	
	/**
	 * Test method for {@link verjinxer.util.HugeShortArray#fill(short)}.
	 */
	@Test
	public void testFill() {
		arrayTwoPowerThirty.fill( (short)6 );
		testFill(arrayTwoPowerThirty, twoPowerThirty, (short)6 );
		
		arrayTwoPowerThirtyMinusOne.fill( (short)(-1) );
		testFill(arrayTwoPowerThirtyMinusOne, twoPowerThirty-1, (short)(-1) );
		
		arrayTwoPowerThirtyPlusOne.fill( Short.MAX_VALUE );
		testFill(arrayTwoPowerThirtyPlusOne, twoPowerThirty+1 , Short.MAX_VALUE );
		
		arrayTwoPowerThirtyThree.fill( Short.MIN_VALUE );
		testFill(arrayTwoPowerThirtyThree, twoPowerThirtyThree, Short.MIN_VALUE );
	}
	
	private static void testFill(final HugeShortArray array, final long length, final short value){
		assertEquals("Array has not the right length",array.length, length);
		
		for(long l = 0; l < array.length; l++){
			assertEquals(String.format("Error at position %d",l),array.get(l), value);
		}
	}

	/**
	 * Test method for {@link verjinxer.util.HugeShortArray#sort()}.
	 */
	@Test
	public void testSort() {
		arrayTwoPowerThirty.sort();
		testSort(arrayTwoPowerThirty, twoPowerThirty);
		
		arrayTwoPowerThirtyMinusOne.sort();
		testSort(arrayTwoPowerThirtyMinusOne, twoPowerThirty-1);
		
		arrayTwoPowerThirtyPlusOne.sort();
		testSort(arrayTwoPowerThirtyPlusOne, twoPowerThirty+1);
		
		arrayTwoPowerThirtyThree.sort();
		testSort(arrayTwoPowerThirtyThree, twoPowerThirtyThree);
	}
	
	private static void testSort(final HugeShortArray array, final long length){
		assertEquals("Array has not the right length",array.length, length);
		
		for(long l = 0; l < array.length-1 ; l++){
			assertTrue(String.format("Error at position %d",l), array.get(l) <= array.get(l+1) );
		}
	}

	/**
	 * Test method for {@link verjinxer.util.HugeShortArray#sort(long, long)}.
	 */
	@Test
	public void testSortLongLong() {
		//test if can sort over bucket boundary
		final long start = twoPowerThirty - 1500 ;
		final long length = 3000;
		arrayTwoPowerThirtyThree.sort(start, length);
		assertEquals("Array has not the right length",arrayTwoPowerThirtyThree.length, twoPowerThirtyThree);
		for( long l=start ; l < start+length-1 ; l++){
			assertTrue(String.format("Error at position %d",l), arrayTwoPowerThirtyThree.get(l) <= arrayTwoPowerThirtyThree.get(l+1) );
		}
		
		//test if it does only sort the asked positions.
		final long twoPowerSixteen = (long)Math.pow(2,16);
		HugeShortArray array = new HugeShortArray( twoPowerSixteen );
		short value = Short.MAX_VALUE;
		for(long l = 0; l < twoPowerSixteen; l++, value--){
			array.set(l,value);
		}
		final long twoPowerTen = (long)Math.pow(2,10);
		array.sort(twoPowerTen, twoPowerTen);
		testSortLongLong(array, twoPowerSixteen, twoPowerTen, twoPowerTen);
		
		//TODO Test what happens with bad parameters off and len
	}
	
	/**
	 * Test if the given array is sorted at positions start to start+sortedLength-1 an unsorted else.
	 * No test are applied to the cut interfaces (start-1, start) and (start+sortedLength-1, start+sortedLength).
	 * @param array Array to test
	 * @param arrayLength Expected length of array
	 * @param start First position at which the array is sorted
	 * @param sortedLength Length of the sphere which is expected to be sorted.
	 */
	private void testSortLongLong( final HugeShortArray array, final long arrayLength, final long start, final long sortedLength ){
		assertEquals("Array has not the right length",array.length, arrayLength);
		long l = 0;
		for( ; l < start-1 ; l++){
			assertFalse(String.format("Error at position %d",l), array.get(l) <= array.get(l+1) );
		}
		
		for( l++ ; l < start+sortedLength-1 ; l++){
			assertTrue(String.format("Error at position %d",l), array.get(l) <= array.get(l+1) );
		}
		
		for( l++ ; l < arrayLength; l++){
			assertFalse(String.format("Error at position %d",l), array.get(l) <= array.get(l+1) );
		}
		
	}

	/**
	 * Test method for {@link verjinxer.util.HugeShortArray#binarySearch(short)}.
	 */
	@Test
	public void testBinarySearchShort() {
		arrayTwoPowerThirty.sort();
		//random test
		short randomeValue;
		Random r = new Random(seed+174);
		long pos;
		for(int i = 0; i < 10; i++){
			randomeValue = (short) r.nextInt();
			pos = arrayTwoPowerThirty.binarySearch(randomeValue);
			if(pos >= 0){
				assertEquals(String.format("Error at position %d with value %d. Expected value %d ",pos,arrayTwoPowerThirty.get(pos),randomeValue ) ,randomeValue, arrayTwoPowerThirty.get(pos));
			} else {
				pos *= (-1);
				pos -= 1;
				assertTrue(String.format("Error at position %d",pos), arrayTwoPowerThirty.get(pos) > randomeValue );
				assertTrue(String.format("Error at position %d",pos), arrayTwoPowerThirty.get(pos-1) < randomeValue );
			}
		}
		
		//deterministic test
		HugeShortArray array = new HugeShortArray(110);
		pos = 0;
		for(short s = -100; s <= -6; s+=2){
			array.set(pos++,s);
		}
		array.set(pos++, (short)-5);
		for(short s = -4; s <= 6; s+=2){
			array.set(pos++,s);
		}
		for(short s = 0; s < 9; s++){
			array.set(pos++, (short)7);
		}
		for(short s = 8; s <= 100; s+=2){
			array.set(pos++,s);
		}
		
		//search one occurrence
		pos = array.binarySearch((short)-5);
		assertEquals("Could not find -5" ,48, pos);
		
		pos = array.binarySearch((short)-100);
		assertEquals("Could not find -100" ,0, pos);
		
		pos = array.binarySearch((short)100);
		assertEquals("Could not find 100" ,109, pos);
		
		// search multiple occurrence
		pos = array.binarySearch((short)7);
		assertTrue("Could not find 7" , 55 <= pos && pos <= 63 );
		
		// search value not in the array
		pos = array.binarySearch((short)1);
		assertEquals("Could not find position for 1" ,52, pos);
		
		pos = array.binarySearch((short)-101);
		assertEquals("Could not find position for -101" ,0, pos);
		
		pos = array.binarySearch((short)101);
		assertEquals("Could not find position for 101" ,110, pos);
	}

	/**
	 * Test method for {@link verjinxer.util.HugeShortArray#binarySearch(short, long, long)}.
	 */
	@Test
	public void testBinarySearchShortLongLong() {
		//test like unbounded at bin boundary
		arrayTwoPowerThirtyThree.sort();
		//random test
		short randomeValue;
		Random r = new Random(seed+174);
		long pos;
		for(int i = 0; i < 10; i++){
			randomeValue = (short) r.nextInt();
			pos = arrayTwoPowerThirtyThree.binarySearch(randomeValue, twoPowerThirty-100, twoPowerThirty+100);
			if(pos >= 0){
				assertEquals(String.format("Error at position %d with value %d. Expected value %d ",pos,arrayTwoPowerThirty.get(pos),randomeValue ) ,randomeValue, arrayTwoPowerThirty.get(pos));
			} else {
				pos *= (-1);
				pos -= 1;
				assertTrue(String.format("Error at position %d",pos), arrayTwoPowerThirty.get(pos) > randomeValue );
				assertTrue(String.format("Error at position %d",pos), arrayTwoPowerThirty.get(pos-1) < randomeValue );
			}
		}
		
		//deterministic test
		HugeShortArray array = new HugeShortArray(twoPowerThirty+55);
		array.fill((short)1);
		pos = twoPowerThirty-55;
		for(short s = -100; s <= -6; s+=2){
			array.set(pos++,s);
		}
		array.set(pos++, (short)-5);
		for(short s = -4; s <= 6; s+=2){
			array.set(pos++,s);
		}
		for(short s = 0; s < 9; s++){
			array.set(pos++, (short)7);
		}
		for(short s = 8; s <= 100; s+=2){
			array.set(pos++,s);
		}
		
		//search one occurrence
		pos = array.binarySearch((short)-5, twoPowerThirty-55, twoPowerThirty+54);
		assertEquals("Could not find -5" ,twoPowerThirty-55+48, pos);
		
		pos = array.binarySearch((short)6, twoPowerThirty-55, twoPowerThirty+54);
		assertEquals("Could not find 6" ,twoPowerThirty-55+54, pos);
		
		pos = array.binarySearch((short)-100, twoPowerThirty-55, twoPowerThirty+54);
		assertEquals("Could not find -100" ,twoPowerThirty-55+0, pos);
		
		pos = array.binarySearch((short)100, twoPowerThirty-55, twoPowerThirty+54);
		assertEquals("Could not find 100" ,twoPowerThirty-55+109, pos);
		
		// search multiple occurrence
		pos = array.binarySearch((short)7, twoPowerThirty-55, twoPowerThirty+54);
		assertTrue("Could not find 7" , twoPowerThirty-55+55 <= pos && pos <= twoPowerThirty-100+63 );
		
		// search value not in the array
		pos = array.binarySearch((short)1, twoPowerThirty-55, twoPowerThirty+54); //1 occurs in array beneath lower bound
		assertEquals("Could not find position for 1" ,twoPowerThirty-55+52, pos);
		
		pos = array.binarySearch((short)-101, twoPowerThirty-55, twoPowerThirty+54);
		assertEquals("Could not find position for -101" ,twoPowerThirty-55+0, pos);
		
		pos = array.binarySearch((short)101, twoPowerThirty-55, twoPowerThirty+54);
		assertEquals("Could not find position for 101" ,twoPowerThirty-55+110, pos);
	}

	/**
	 * Test method for {@link verjinxer.util.HugeShortArray#swap(long, long)}.
	 */
	@Test
	public void testSwap() {
		fail("Not yet implemented");
		//TODO test if two values swap positions
		
		//TODO test if all other positions are unmodified
		
		//TODO test what happens when positions to swap are not in array  
	}

	/**
	 * Test method for {@link verjinxer.util.HugeShortArray#copy()}.
	 */
	@Test
	public void testCopy() {
		fail("Not yet implemented");
		//TODO test returned array is not the same but equal in length an each position
		
	}

	/**
	 * Test method for {@link verjinxer.util.HugeShortArray#copyRange(long, long)}.
	 */
	@Test
	public void testCopyRange() {
		fail("Not yet implemented");
		//TODO test like testCopy() without range
	}

}
