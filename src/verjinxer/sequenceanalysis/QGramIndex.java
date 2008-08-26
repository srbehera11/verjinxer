package verjinxer.sequenceanalysis;

import java.nio.IntBuffer;

import verjinxer.Globals;

// TODO remove dependency on Globals
/**
 * The q-gram index. Use this class to retrieve q-gram positions from an
 * on-disk q-gram index. The index is stored in a '.qpos' and in a
 * '.qbck' file.
 */
public class QGramIndex {
   private Globals g;
   int[]       qbck        = null;   // bucket boundaries
   IntBuffer   qpos        = null;   // q-gram positions as buffer
   int[]       qposa       = null;   // q-gram positions as array
   boolean external = false;

   public QGramIndex(Globals gl, final String qposfile, final String qbckfile, boolean external) {
      this.g = gl;
      this.external = external;
      qbck = g.slurpIntArray(qbckfile);

      if (external) qpos  = g.mapR(qposfile).asIntBuffer();
      else          qposa = g.slurpIntArray(qposfile);
   }
   
   public int bucketSize(int qcode) {
      return qbck[qcode+1] - qbck[qcode];
      
   }
   
   /** Gets the positions of the given q-gram (represented by its q-code).
    * The destination array must already be allocated and have a length
    * of at least bucketSize(qcode).
    * 
    * @param qcode  the q-code corresponding to the desired q-gram
    * @param dest   the destination array into which the positions are copied 
    * @return q-gram positions in an array */ 
   public void getQGramPositions(int qcode, int[] dest) {
      int r = qbck[qcode];
      // copy starting positions of current matches positions into 'newpos'
      if (external) { qpos.position(r);  qpos.get(dest, 0, bucketSize(qcode)); } 
      else          { System.arraycopy(qposa, r, dest, 0, bucketSize(qcode));  }
      //g.logmsg("    qpos = [%s]%n", Strings.join(" ",newpos, 0, newactive));
   }
}

