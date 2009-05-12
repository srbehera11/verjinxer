package verjinxer.subcommands;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import verjinxer.Globals;

/**
 * 
 * @author Markus Kemmerling
 */
public class TranslaterSubcommandTest {

   private static TranslaterSubcommand translaterSubcommand;
   private static File testdataDirectory;

   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
      System.out.println("Setting up.");
      Globals g = new Globals();
      File file = new File("data");
      g.dir = file.getAbsolutePath() + File.separator;
      assert g.dir.endsWith(File.separator);
      assert new File(g.dir).exists();
      assert new File(g.dir).isDirectory();
      testdataDirectory = new File("testdata" + File.separator);
      g.outdir = testdataDirectory.getAbsolutePath() + File.separator;
      assert g.outdir.endsWith(File.separator);

      if (testdataDirectory.exists()) {
         System.err.printf("The directory %s already exists. Exiting before damage some data.",
               testdataDirectory.getAbsolutePath());
         System.exit(-1);
      } else {
         testdataDirectory.mkdir();
         testdataDirectory.deleteOnExit();
         assert testdataDirectory.exists();
         assert testdataDirectory.isDirectory();
      }

      translaterSubcommand = new TranslaterSubcommand(g);
   }

   @AfterClass
   public static void tearDownAfterClass() throws Exception {
      System.out.println("Deleting directory.");
      assert testdataDirectory.exists();
      File[] files = testdataDirectory.listFiles();
      for (int i = 0; i < files.length; i++) {
         System.out.printf("File: %s%n", files[i].getAbsolutePath());
      }
      // TODO there are files like '.nfs00000000000017830000006f', so it will not delete
      // testdataDirectory !?!?
      // TODO analyze translaterSubcommand.run(args) where filestreams or channels are used and
      // close them!
      // TODO files cannot be deleted by invoking delete()
      // assert testdataDirectory.listFiles().length == 0;
      testdataDirectory.delete();
      // assert !testdataDirectory.exists();
   }

   @Before
   public void setUp() throws Exception {
   }

   @After
   public void tearDown() throws Exception {
      System.out.println("Deleting files in directory.");
      assert testdataDirectory.exists();
      File[] files = testdataDirectory.listFiles(new FileFilter() {
         public boolean accept(File pathname) {
            String name = pathname.getName();
            return !name.startsWith(".");
         }
      });
      for (File f : files) {
         assert f.isFile();
         f.delete();
         assert !f.exists() : String.format("The file %s was not deleted.%n", f.getAbsolutePath());
      }
   }

   @Test
   @Ignore
   public void testRunWithWrongOptions() {
      String[] args;
      String[] alphabets = { "-a", "--dna", "--rconly", "--dnarc", "--dnabi", "--protein", "-c",
            "--colorspace" };

      System.out.println("Testing, that you cannot set an alphabet map with a CSFASTA file.");
      for (String alphabet : alphabets) {
         args = new String[] { alphabet, "xyz.cs" }; // only one CSFASTA
         assertEquals(1, translaterSubcommand.run(args));
         assertEquals(0, testdataDirectory.list().length); // no files in directory

         args = new String[] { alphabet, "agds.fa", "adfaflj.fasta", "afljafljasl.fasta", "xyz.cs",
               "zppldgoe.fa", "afafiilrwe.fasta" }; // one CSFASTA beneath some FASTA
         assertEquals(1, translaterSubcommand.run(args));
         assertEquals(0, testdataDirectory.list().length); // no files in directory

         args = new String[] { alphabet, "abc.csfasta" }; // only one CSFASTA
         assertEquals(1, translaterSubcommand.run(args));
         assertEquals(0, testdataDirectory.list().length); // no files in directory

         args = new String[] { alphabet, "agds.fa", "adfaflj.fasta", "afljafljasl.fasta", "xyz.cs",
               "zppldgoe.fa", "afafiilrwe.fasta" }; // one CSFASTA beneath some FASTA
         assertEquals(1, translaterSubcommand.run(args));
         assertEquals(0, testdataDirectory.list().length); // no files in directory
      }

      System.out.println("Testing, that you cannot omit the alphabet map by a FASTA file.");
      for (String option : new String[] { "--trim", "--masked", "--reverse", "-r", "--runs" }) {
         args = new String[] { "abc.csfasta" };
         assertEquals(1, translaterSubcommand.run(args));
         assertEquals(0, testdataDirectory.list().length); // no files in directory
      }

   }

   @Test
   public void testRunFastaWithC() throws IOException {
      String[] args = { "-c", "colorspace.fa" };
      int ret = translaterSubcommand.run(args);
      assertEquals(0, ret);
      assertEqualFiles("data" + File.separator + "colorspace.seq",
            testdataDirectory.getAbsolutePath() + File.separator + "colorspace.seq");
   }

   @Test
   public void testRunCSFASTA() throws IOException {
      String[] args = { "colorspace.csfasta" };
      int ret = translaterSubcommand.run(args);
      assertEquals(0, ret);
      assertEqualFiles("data" + File.separator + "colorspace.seq",
            testdataDirectory.getAbsolutePath() + File.separator + "colorspace.seq");
   }

   /**
    * Tests if the given files have equal content.
    * 
    * @param filename1
    * @param filename2
    * @throws IOException
    */
   private void assertEqualFiles(String filename1, String filename2) throws IOException {
      File file1 = new File(filename1);
      File file2 = new File(filename2);
      System.out.printf("Comparing the files %s and %s.%n", file1.getAbsoluteFile(),
            file2.getAbsoluteFile());

      FileInputStream fileStream1 = new FileInputStream(file1);
      FileInputStream fileStream2 = new FileInputStream(file2);

      FileChannel channel1 = fileStream1.getChannel();
      FileChannel channel2 = fileStream2.getChannel();

      assertEquals(
            String.format("The files %s and %s have different length.", filename1, filename2),
            file1.length(), file2.length());

      ByteBuffer buffer1 = ByteBuffer.allocate(file1.length() > Integer.MAX_VALUE ? Integer.MAX_VALUE
            : (int) file1.length());
      ByteBuffer buffer2 = ByteBuffer.allocate(file2.length() > Integer.MAX_VALUE ? Integer.MAX_VALUE
            : (int) file2.length());

      int pos1 = 1;
      int pos2 = 1;
      while (pos1 == pos2 && pos1 > 0) {
         System.out.printf("Comparing position %d.%n", pos1);
         pos1 = channel1.read(buffer1);
         pos2 = channel2.read(buffer2);
         assertEquals(String.format("The files %s and %s have different conent.", filename1,
               filename2), 0, buffer1.compareTo(buffer2));
      }
      assertEquals(String.format("Reading the files %s and %s had end at different positions.",
            filename1, filename2), pos1, pos2);

      // clean up
      channel1.close();
      channel2.close();
      fileStream1.close();
      fileStream2.close();
   }

}
