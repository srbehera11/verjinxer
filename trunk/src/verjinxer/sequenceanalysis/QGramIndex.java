/**
 * @author Marcel Martin
 */
package verjinxer.sequenceanalysis;

import java.io.IOException;

import verjinxer.Globals;
import verjinxer.util.ArrayFile;
import verjinxer.util.ProjectInfo;

// TODO remove dependency on Globals
// TODO document qgramindex file format here
// TODO add a q member field
/**
 * The q-gram index. Use this class to retrieve q-gram positions from an on-disk q-gram index. The
 * index is stored in a '.qpos' and in a '.qbck' file.
 */
public class QGramIndex {
   /** Bucket boundaries */
   final int[] qbck;
   
   /** q-gram positions. Since a regular array would often not be large enough (only
    *  2**31 entries in an int array in Java), the index is split up into 'superbuckets' 
    *  containing 2**BITS buckets each. */
   final int[][] qpos;
   
   /** Size of a superbucket, in bits. That is, if this is 10, then each superbucket 
    * contains 2**10 buckets */
   final private static int BITS = 10;
   
   /** Bitwise-AND this mask with a bucket index to get the superbucket */
   final private static int BITMASK_HIGH = (-1) << BITS;
   
   /** Bitwise-AND this mask with a bucket index to get its index within the superbucket */
   final private static int BITMASK_LOW = ~BITMASK_HIGH;
   
   /** Maximum size of a bucket */
   final int maximumBucketSize;

   /**
    * TODO remove the maxactive parameter and add a project parameter
    * @param gl
    * @param qposfile
    * @param qbckfile
    * @param maxactive
    * @throws IOException
    */
   public QGramIndex(final String qposfile, final String qbckfile, int maxactive) throws IOException {
      this.qbck = (new Globals()).slurpIntArray(qbckfile); // TODO remove Globals
      final int buckets = qbck.length - 1;
            
      this.qpos = new int[(buckets >> BITS) + 1][];
      
      // piecewise read of the qpos file into qposa
      int start = 0;
      int end = qbck[1>>BITS];
      ArrayFile af = new ArrayFile(qposfile);
      assert (buckets & BITMASK_LOW) == 0;

      // read 1 << BITS buckets at a time
      for (int i = 0; i < (buckets >> BITS); ++i) {
         start = qbck[i << BITS];
         assert ((i+1) << BITS) < qbck.length;
         end = qbck[(i+1) << BITS];
         qpos[i] = new int[end-start];
         af.readArray(qpos[i], 0, end-start, start);
      }
      af.close();
      maximumBucketSize = maxactive; // TODO //Integer.parseInt(prj.getProperty("qbckMax"));
   }
   
   public QGramIndex(final ProjectInfo project) throws IOException {
      this(project.getQPositionsFileName(), project.getQBucketsFileName(), project.getMaximumBucketSize());
   }

   /** @return maximum bucket size */
   public int getMaximumBucketSize() {
      return maximumBucketSize;
   }

   public int getBucketSize(int qcode) {
      return qbck[qcode + 1] - qbck[qcode];

   }

   /**
    * @return number of buckets in this q-gram index
    */
   public int getNumberOfBuckets() {
      return qbck.length-1;
   }
   
   /**
    * Gets the positions of the given q-gram (represented by its q-code). The destination array must
    * not be written to. It will have length bucketSize(qcode).
    * 
    * @param qcode
    *           the q-code corresponding to the desired q-gram
    *
    */
   public void getQGramPositions(int qcode, int[] dest) {
      final int i = qcode >> BITS;
      final int x = qcode & BITMASK_HIGH;
      final int from = qbck[qcode] - qbck[x];
      //final int to = qbck[qcode+1] - qbck[x];
      System.arraycopy(qpos[i], from, dest, 0, getBucketSize(qcode));
      
      
      //int [] result = Arrays.copyOfRange(qposa[i], from, to);
      //int[] oldresult = Arrays.copyOfRange(qposold, qbck[qcode], qbck[qcode+1]);
//      for (int j = 0; j< getBucketSize(qcode); ++j)
//         assert oldresult[j] == dest[j];
   
      //      return qpos.copyRangeToInt(qbck[qcode], qbck[qcode+1]);
      
      
      // Variante 1: 5s
      
//      return Arrays.copyOfRange(qposa, qbck[qcode], qbck[qcode+1]);
      
      // Variante 2: auch 5s
      
//      final int r = qbck[qcode];
//      //final int l = getBucketSize(qcode);
//      int[] dest = new int[getBucketSize(qcode)];
//      System.arraycopy(qposa, r, dest, 0, getBucketSize(qcode));
//      return dest;
      
      // Variante 3: mit Listen
      
      //int[] x = qposa;
      
//      List l = Arrays.asList(qposa);
//      
//      if (qbck[qcode+1] >=  l.size()) {
//         System.out.format("boing. qposa.length=%d. qcode=%d. qbck[qcode]=%d. qbck[qcode+1]=%d. l.size()=%d%n", qposa.length, qcode, qbck[qcode], qbck[qcode+1], l.size());
//      }
//      return Arrays.asList(qposa).subList(qbck[qcode], qbck[qcode+1]);
      
      
      // Variante x: mit IntBuffer
//      if (external) {
//         qpos.position(qbck[qcode]);
//         IntBuffer b = qpos.slice();
//         b.limit(getBucketSize(qcode));
//         return b;
//      } else return null;
      /*
      if (external) {
         qpos.position(r);
         qpos.get(dest, 0, getBucketSize(qcode));
      } else {
         System.arraycopy(qposa, r, dest, 0, getBucketSize(qcode));
      }
      // g.logmsg(" qpos = [%s]%n", Strings.join(" ",newpos, 0, newactive));
      */
      
      
   }
   
//   public static class Bucket
//   {
//      private final int[] a;
//      private final int len;
//      private final int offset;
//
//      Bucket(int[] array, int offset, int len) {
//         if (array==null)
//            throw new NullPointerException();
//         a = array;
//         if (offset > a.length)
//            throw new IndexOutOfBoundsException("offset > length ("+offset+" > "+a.length+")");
//         if (len < 0 || len+offset > a.length)
//            throw new IndexOutOfBoundsException("len < 0 or len+offset > array length");
//         this.offset = offset;
//         this.len = len;
//      }
//
//      public int size() {
//         return len;
//      }
//
//      public int get(int i) {
//         return a[offset + i];
//      }
//   }
 
}
