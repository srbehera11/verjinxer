package verjinxer.sequenceanalysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class QGramIndexTest {

   @Test
   public void testQGramIndex() {
      //int the qpos and the qbck files
      int[] qpos = { 0, 3, 5, 7, 23, 24,
                     7, 12, 15, 18, 23, 25, 27, 30, 31, 32,
                     1, 2, 4, 7, 9, 33,
            40, 100, 111 };
      int[] qbck = { 0, 6, 16, qpos.length };

      File qposfile = null;
      File qbckfile = null;
      try {
         qposfile = File.createTempFile("testing", ".qpos");
         
         FileOutputStream fos = new FileOutputStream(qposfile);
         for (int i : qpos) {
            fos.write((byte) (i >> 24));
            fos.write((byte) (i >> 16));
            fos.write((byte) (i >> 8));
            fos.write((byte) (i));
         }
         qbckfile = File.createTempFile("testing", ".qbck");
         fos = new FileOutputStream(qbckfile);
         for (int i : qbck) {
            fos.write((byte) (i >> 24));
            fos.write((byte) (i >> 16));
            fos.write((byte) (i >> 8));
            fos.write((byte) (i));
         }
         //generate QGramIndex
         //TODO the generation never terminates
         QGramIndex index = new QGramIndex(qposfile.getAbsolutePath(), qbckfile.getAbsolutePath(), 10, 5, 1);
         assertEquals(5, index.q);
         assertEquals(10, index.maximumBucketSize);
         
      } catch (IOException e) {
         fail(e.toString());
      } finally {
         qposfile.delete();
         qbckfile.delete();
      }
      
      
   }

}
