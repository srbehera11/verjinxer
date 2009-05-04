package verjinxer.sequenceanalysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
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
 * This test is a whiteboxtest for <code>QGramIndex<code>. It uses the class <code>ArrayFile<code>
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
         System.err.printf("The directory %s already exists. Exiting before damage some data.", testdataDirectory.getAbsolutePath());
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
      assert testdataDirectory.exists();
      testdataDirectory.delete();
      assert !testdataDirectory.exists();
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
   }
   

   @Test
   public void testQGramIndex() {
      //int the qpos and the qbck files
      int[] qpos = { 0, 3, 5, 7, 23, 24,
                     7, 12, 15, 18, 23, 25, 27, 30, 31, 32,
                     1, 2, 4, 7, 9, 33,
                     40, 100, 111 };
      int[] qbck = { 0, 6, 6, 16, 22, qpos.length };
      int[] bucketSizes = {6,0,10,6,3};

      File qposfile = null;
      File qbckfile = null;
      try {
         qposfile = new File("testing.qpos");
         qbckfile = new File("testing.qbck");
         
         ArrayFile af = new ArrayFile(qposfile.getAbsolutePath());
         af.writeArray(qpos);
         
         af.setFilename(qbckfile.getAbsolutePath());
         af.writeArray(qbck);
         
         
         //generate QGramIndex
         //TODO the generation never terminates
         QGramIndex index = new QGramIndex(qposfile.getAbsolutePath(), qbckfile.getAbsolutePath(), 10, 5, 1);
         assertEquals(5, index.q);
         assertEquals(10, index.maximumBucketSize);
         assertEquals(1, index.getStride());
         
         assertEquals(qpos.length, index.getNumberOfPositions());
         
         for(int i = 0; i < bucketSizes.length; i++){
            assertEquals(String.format("Error for qgram %s:", i) , bucketSizes[i], index.getBucketSize(i));
         }
         
         assertEquals(bucketSizes.length, index.getNumberOfBuckets());
         
         for(int i = 0; i < bucketSizes.length; i++){
            int[] array = new int[ bucketSizes[i] ];
            index.getQGramPositions(i, array);
            for(int j = 0; j < bucketSizes[i]; j++){
               assertEquals(qpos[ j+qbck[i] ], array[j]);
            }
         }
         
      } catch (Exception e) {
         fail(e.toString());
      } finally {
         qposfile.delete();
         qbckfile.delete();
      }
   }
   
   /**
    * 
    * @param index
    * @param q
    * @param maximumBucketSize
    * @param stride
    */
   private static void testIndex(QGramIndex index, int q, int maximumBucketSize, int stride){
      //TODO make assertEquals from testQGramIndex hier.
   }

}
