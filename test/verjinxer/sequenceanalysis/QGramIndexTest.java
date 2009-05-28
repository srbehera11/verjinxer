package verjinxer.sequenceanalysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import verjinxer.util.ArrayFile;

/**
 * This test is a white box test for <code>QGramIndex<code>. It uses the class <code>ArrayFile<code>
 * to generate a qpos and a qbck file on disk. The tested class <code>QGramIndex<code> also uses 
 * an <code>ArrayFile<code> to read the files.
 * In the beginning, a new directory 'testdata' is created, where the needed files are stored.
 * If the directory already exists, no test are made.
 * The new qpos and qbck files are generated before each test and deleted after.
 * After all tested were made, the 'testdata' directory will also be deleted.
 * 
 * @author Markus Kemmerling
 */
public class QGramIndexTest {

   private static final String QPOS_FILENAME = "testing.qpos";
   private static final String QBCK_FILENAME = "testing.qbck";

   private int[] qpos;
   private int[] qbck;
   private int[] bucketSizes;

   private File qposfile;
   private File qbckfile;
   private static File testdataDirectory;

   /**
    * @throws java.lang.Exception
    */
   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
      testdataDirectory = new File("testdata");
      if (testdataDirectory.exists()) {
         System.err.printf("The directory %s already exists. Exiting before damage some data.",
               testdataDirectory.getAbsolutePath());
         System.exit(-1);
      } else {
         testdataDirectory.mkdir();
         assert testdataDirectory.exists();
         assert testdataDirectory.isDirectory();
      }
   }

   /**
    * @throws java.lang.Exception
    */
   @AfterClass
   public static void tearDownAfterClass() throws Exception {
      System.out.println("Deleting directory.");
      assert testdataDirectory.exists();
      testdataDirectory.delete();
      assert !testdataDirectory.exists();
      System.out.println("Directory deleted.");
   }

   /**
    * @throws java.lang.Exception
    */
   @Before
   public void setUp() throws Exception {
      qposfile = new File(testdataDirectory.getAbsolutePath() + File.separator + QPOS_FILENAME);
      assert !qposfile.exists();
      qposfile.createNewFile();
      assert qposfile.exists();

      qbckfile = new File(testdataDirectory.getAbsolutePath() + File.separator + QBCK_FILENAME);
      assert !qbckfile.exists();
      qbckfile.createNewFile();
      assert qbckfile.exists();
   }

   /**
    * @throws java.lang.Exception
    */
   @After
   public void tearDown() throws Exception {
      System.out.println("Deleting files.");
      assert qposfile.exists();
      qposfile.delete();
      assert !qposfile.exists();
      qposfile = null;

      assert qbckfile.exists();
      qbckfile.delete();
      assert !qbckfile.exists();
      qposfile = null;

      qpos = null;
      qbck = null;
      bucketSizes = null;

      System.gc();
      System.out.println("Files deleted.");
   }

   @Test
   public void smallTest() {
      qpos = new int[] { 0, 3, 5, 7, 23, 24, 7, 12, 15, 18, 23, 25, 27, 30, 31, 32, 1, 2, 4, 7, 9,
            33, 40, 100, 111 };
      qbck = new int[] { 0, 6, 6, 16, 22, qpos.length };
      bucketSizes = new int[] { 6, 0, 10, 6, 3 };

      try {

         writeArraysToDisc();

         int q = 5;
         int maximumBucketSize = 10;
         int stride = 1;

         // generate QGramIndex
         QGramIndex index = new QGramIndex(qposfile, qbckfile, maximumBucketSize, q, stride);
         testIndex(index, maximumBucketSize, q, stride);

      } catch (IOException e) {
         fail(e.toString());
      }
   }

   /**
    * Writes qpos into qposfile and qbck into qbckfile.
    * 
    * @throws IOException
    */
   private void writeArraysToDisc() throws IOException {
      ArrayFile af = new ArrayFile(qposfile.getAbsolutePath());
      af.writeArray(qpos);
      af.flush();

      af.setFilename(qbckfile.getAbsolutePath());
      af.writeArray(qbck);
      af.flush();
      af.close();
   }

   /**
    * Test the following methods of QGramIndex:<br>
    * {@link verjinxer.sequenceanalysis.QGramIndex#getStride()}
    * {@link verjinxer.sequenceanalysis.QGramIndex#getNumberOfPositions()}
    * {@link verjinxer.sequenceanalysis.QGramIndex#getNumberOfBuckets()}
    * {@link verjinxer.sequenceanalysis.QGramIndex#getBucketSize(int)}
    * {@link verjinxer.sequenceanalysis.QGramIndex#getQGramPositions(int, int[])} And for the
    * following fields: {@link verjinxer.sequenceanalysis.QGramIndex#q}
    * {@link verjinxer.sequenceanalysis.QGramIndex#maximumBucketSize}
    * 
    * @param index
    *           QGramIndex to test
    * @param q
    *           the expected q
    * @param maximumBucketSize
    *           the expected size of the buckets
    * @param stride
    *           the expected stride
    */
   private void testIndex(QGramIndex index, int maximumBucketSize, int q, int stride) {
      System.out.println("Testing q.");
      assertEquals(q, index.q);

      System.out.println("Testing maximum bucket size.");
      assertEquals(maximumBucketSize, index.maximumBucketSize);

      System.out.println("Testing stride.");
      assertEquals(stride, index.getStride());

      System.out.println("Testing number of positions.");
      assertEquals(qpos.length, index.getNumberOfPositions());

      System.out.println("Testing number of buckets.");
      assertEquals(bucketSizes.length, index.getNumberOfBuckets());

      System.out.println("Testing sizes of buckets.");
      for (int i = 0; i < bucketSizes.length; i++) {
         assertEquals(String.format("Wrong bucket size for qgram %s:", i), bucketSizes[i],
               index.getBucketSize(i));
      }

      for (int i = 0; i < bucketSizes.length; i++) {
         System.out.printf("Testing positions for qgram %s.%n", i);
         int[] array = new int[bucketSizes[i]];
         index.getQGramPositions(i, array);
         for (int j = 0; j < bucketSizes[i]; j++) {
            assertEquals(String.format("Position %s for qgram %s is wrong.", j, i), qpos[j
                  + qbck[i]], array[j]);
         }
      }
   }

}
