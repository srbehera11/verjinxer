package verjinxer.subcommands;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import verjinxer.Globals;

/**
 * 
 * @author Markus Kemmerling
 */
public class TranslaterSubcommandTest {

   private static TranslaterSubcommand translaterSubcommand;
   private File testdataDirectory;

   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
   }

   @AfterClass
   public static void tearDownAfterClass() throws Exception {
   }

   @Before
   public void setUp() throws Exception {
      System.out.println("Setting up.");
      Globals g = new Globals();
      g.dir = "data" + File.separator;
      g.outdir = "testdata" + File.separator;
      testdataDirectory = new File(g.outdir);
      if (testdataDirectory.exists()) {
         System.err.printf("The directory %s already exists. Exiting before damage some data.",
               testdataDirectory.getAbsolutePath());
         System.exit(-1);
      } else {
         testdataDirectory.mkdir();
         assert testdataDirectory.exists();
         assert testdataDirectory.isDirectory();
      }

      translaterSubcommand = new TranslaterSubcommand(g);
   }

   @After
   public void tearDown() throws Exception {
      System.out.println("Deleting directory.");
      assert testdataDirectory.exists();
      File[] files = testdataDirectory.listFiles();
      for (File f : files) {
         assert f.isFile();
         f.delete();
         assert !f.exists();
      }
      testdataDirectory.delete();
      assert !testdataDirectory.exists();
   }

   @Test
   public void testRun() {
      fail("Not yet implemented");
   }

}
