/**
 * @author Marcel Martin
 */
package verjinxer.sequenceanalysis;

import java.io.IOException;

import verjinxer.util.ArrayFile;
import verjinxer.util.ProjectInfo;

/**
 * The q-gram index. Currently, this class can only be used to <em>read</em> an index from disk. The
 * entire index is kept in memory. On disk, the index must be stored in two files, the qpositions
 * file and the qbuckets file (extensions .qpos and .qbck).
 * 
 * Conceptually, a q-gram index maps every q-code to a list of positions. This list of positions is
 * called a bucket. All positions of all buckets are concatenated (in q-code order) and stored in
 * the qpositions file. The qbuckets file contains the indices of the beginning of each bucket.
 * 
 * That is, all positions for a q-code x are stored at indices qbuckets[x] to qbuckets[x+1] - 1 in
 * qpositions.
 * 
 * The qbuckets file additionally contains as very last value the total number of positions in the
 * index. In total, there must be asize**q + 1 values in the qbuckets file.
 */
public class QGramIndex {

   /** Bucket boundaries */
   final int[] qbck;

   /**
    * q-gram positions. Since a regular array would often not be large enough (only 2**31 entries in
    * an int array in Java), the index is split up into 'superbuckets' containing 2**BITS buckets
    * each.
    */
   final int[][] qpos;

   /** the q of this q-gram index */
   /** TODO public */
   final public int q;

   /** Maximum size of a bucket */
   final int maximumBucketSize;

   /**
    * Stride length of this index (only q-grams whose positions are divisible by stride are indexed)
    */
   final int stride;

   /**
    * Size of a superbucket, in bits. That is, if this is 10, then each superbucket contains 2**10
    * buckets
    */
   final private static int BITS = 10;

   /** Bitwise-AND this mask with a bucket index to get the superbucket */
   final private static int BITMASK_HIGH = (-1) << BITS;

   /** Bitwise-AND this mask with a bucket index to get its index within the superbucket */
   final private static int BITMASK_LOW = ~BITMASK_HIGH;

   /**
    * Initializes a q-gram index by reading it from the qpositions and qbuckets files (.qpos,
    * .qbck). You should usually use the QGramIndex(ProjectInfo) constructor.
    * 
    * 
    * @param qposfile
    *           name of the .qpos file
    * @param qbckfile
    *           name of the .qbck file
    * @param maximumBucketSize
    *           maximum size of a bucket
    * @param q
    *           q
    * @throws IOException
    */
   public QGramIndex(final String qposfile, final String qbckfile, int maximumBucketSize, int q,
         int stride) throws IOException {
      assert q >= 5 : "Sorry, cannot work with a q<5 for now";
      ArrayFile af = new ArrayFile(qbckfile);
      this.qbck = af.readArray(this.qbck);
      this.stride = stride;
      final int buckets = qbck.length - 1;
      assert (buckets & BITMASK_LOW) == 0;

      this.qpos = new int[(buckets >> BITS) + 1][];

      // piecewise read of the qpos file into qposa
      int start, end;

      af.setFilename(qposfile);

      // read a superbucket (consisting of 2**BITS buckets) at a time
      for (int i = 0; i < (buckets >> BITS); ++i) {
         start = qbck[i << BITS];
         assert ((i + 1) << BITS) < qbck.length;
         end = qbck[(i + 1) << BITS];
         qpos[i] = new int[end - start];
         af.readArray(qpos[i], 0, end - start, start);
      }
      af.close();
      this.maximumBucketSize = maximumBucketSize;
      this.q = q;
   }

   // TODO
   // static private int[][] convertFromRegularBucketsToSuperbuckets(int[] qbck, int[] qpos) {
   // final int buckets = qbck.length - 1;
   // assert (buckets & BITMASK_LOW) == 0;
   //
   // int[][] superqpos = new int[(buckets >> BITS) + 1][];
   //      
   // int start = 0;
   // int end = qbck[1 >> BITS];
   //
   // // read a superbucket (consisting of 2**BITS buckets) at a time
   // for (int i = 0; i < (buckets >> BITS); ++i) {
   // start = qbck[i << BITS];
   // assert ((i + 1) << BITS) < qbck.length;
   // end = qbck[(i + 1) << BITS];
   // qpos[i] = new int[end - start];
   // af.readArray(qpos[i], 0, end - start, start);
   // }
   //
   // // copy given qpos to our qpos
   //      
   //      
   //      
   // // read a superbucket (consisting of 2**BITS buckets) at a time
   // for (int i = 0; i < (buckets >> BITS); ++i) {
   // start = qbck[i << BITS];
   // assert ((i + 1) << BITS) < qbck.length;
   // end = qbck[(i + 1) << BITS];
   // qpos[i] = new int[end - start];
   // af.readArray(qpos[i], 0, end - start, start);
   // }
   //      
   //      
   // this.qpos = qpos;
   // }

   // QGramIndex(int[] qbck, int[] qpos, int q, int maximumBucketSize) {
   // this.q = q;
   // this.qbck = qbck;
   // this.maximumBucketSize = maximumBucketSize;
   // this.qpos = convertFromRegularBucketsToSuperbuckets(qbck, qpos);
   // }

   /**
    * Reads a q-gram index stored on disk. The getQPositionsFileName() and getQBucketsFileName()
    * methods of ProjectInfo are used to get the file names.
    * 
    * @param project
    *           the project info that is associated with the index
    * 
    * @throws IOException
    */
   public QGramIndex(final ProjectInfo project) throws IOException {
      this(project.getQPositionsFileName(), project.getQBucketsFileName(),
            project.getMaximumBucketSize(), project.getIntProperty("q"), project.getStride());
   }

   /** @return maximum bucket size */
   public int getMaximumBucketSize() {
      return maximumBucketSize;
   }

   /**
    * TODO where should meta information about the index go? into this class?
    * 
    * @return stride width of this index
    */
   public int getStride() {
      return stride;
   }

   /**
    * @return size of a q-gram bucket, that is, the number of positions stored for the given q-code.
    */
   public int getBucketSize(int qcode) {
      return qbck[qcode + 1] - qbck[qcode];
   }

   /**
    * @return total number of buckets in this q-gram index
    */
   public int getNumberOfBuckets() {
      return qbck.length - 1;
   }

   /**
    * Gets the positions of the given q-grams (represented by their q-code). The q-gram positions
    * will be copied into dest, starting from index 0. If dest is not large enough or null, it will
    * be reallocated at twice the original size, but at least 10000 elements and at least
    * dest.length. It is recommended to reuse dest in subsequent invocations of this method.
    * 
    * @param qcode
    *           the q-codes corresponding to the desired q-grams
    * @param dest
    *           the destination array, will be reallocated if not large enough. 
    *           dest[dest.length-1] contains the number of entries copied into dest.
    *           TODO this is quite ugly.
    */
//   public int[] getQGramPositions(int[] qcodes, int[] dest) {
//      int size = 1; // one element extra for the length entry
//      for (int i = 0; i < qcodes.length; ++i) {
//         size += getBucketSize(qcodes[i]);
//      }
//      if (dest == null || dest.length < size) {
//         // reallocate
//         int newlength = Math.max(10000, size);
//         if (dest != null) {
//            newlength = Math.max(dest.length * 2, newlength);
//         }
//         dest = new int[newlength];
//      }
//      int pos = 1;
//      for (int qcode : qcodes) {
//         int i = qcode >> BITS;
//         int x = qcode & BITMASK_HIGH;
//         int from = qbck[qcode] - qbck[x];
//         // final int to = qbck[qcode+1] - qbck[x];
//         int len = getBucketSize(qcode);
//         System.arraycopy(qpos[i], from, dest, pos, len);
//         pos += len;
//      }
//      dest[dest.length-1] = size;
//      return dest;
//   }
   
   /**
    * Gets the positions of the given q-gram (represented by its q-code). To avoid reallocations,
    * this method does not allocate an array, but instead requires that the dest array has already
    * been allocated and that it is large enough. The q-gram positions will be copied into dest,
    * starting from index 0. The length of dest will not be changed. It is recommended to allocate
    * an array of size getMaximumBucketSize() and reuse that for all invocations of this method.
    * 
    * @param qcode
    *           the q-code corresponding to the desired q-gram
    * @param dest
    *           the destination array. It must already be allocated and have a length of at least
    *           bucketSize(qcode).
    */
   public void getQGramPositions(int qcode, int[] dest) {
      getQGramPositions(qcode, dest, 0);
   }
   
   /**
    * Gets the positions of the given q-gram (represented by its q-code). To avoid reallocations,
    * this method does not allocate an array, but instead requires that the dest array has already
    * been allocated and that it is large enough. The q-gram positions will be copied into dest,
    * starting from index pos. The length of dest will not be changed.
    * 
    * @param qcode
    *           the q-code corresponding to the desired q-gram
    * @param dest
    *           the destination array. It must already be allocated and have a length of at least
    *           bucketSize(qcode) + pos.
    */
   public void getQGramPositions(int qcode, int[] dest, int pos) {
      final int i = qcode >> BITS;
      final int x = qcode & BITMASK_HIGH;
      final int from = qbck[qcode] - qbck[x];
      // final int to = qbck[qcode+1] - qbck[x];
      System.arraycopy(qpos[i], from, dest, pos, getBucketSize(qcode));

      /*
       * TODO document the experiments below a bit better some possibilities to make this method
       * nicer or faster or perhaps both.
       */

      // int [] result = Arrays.copyOfRange(qposa[i], from, to);
      // int[] oldresult = Arrays.copyOfRange(qposold, qbck[qcode], qbck[qcode+1]);
      // for (int j = 0; j< getBucketSize(qcode); ++j)
      // assert oldresult[j] == dest[j];
      // return qpos.copyRangeToInt(qbck[qcode], qbck[qcode+1]);
      // Variante 1: 5s
      // return Arrays.copyOfRange(qposa, qbck[qcode], qbck[qcode+1]);
      // Variante 2: auch 5s
      // final int r = qbck[qcode];
      // //final int l = getBucketSize(qcode);
      // int[] dest = new int[getBucketSize(qcode)];
      // System.arraycopy(qposa, r, dest, 0, getBucketSize(qcode));
      // return dest;
      // Variante 3: mit Listen
      // int[] x = qposa;
      // List l = Arrays.asList(qposa);
      //      
      // if (qbck[qcode+1] >= l.size()) {
      // System.out.format("boing. qposa.length=%d. qcode=%d. qbck[qcode]=%d. qbck[qcode+1]=%d.
      // l.size()=%d%n", qposa.length, qcode, qbck[qcode], qbck[qcode+1], l.size());
      // }
      // return Arrays.asList(qposa).subList(qbck[qcode], qbck[qcode+1]);
      // Variante x: mit IntBuffer
      // if (external) {
      // qpos.position(qbck[qcode]);
      // IntBuffer b = qpos.slice();
      // b.limit(getBucketSize(qcode));
      // return b;
      // } else return null;
      /*
       * if (external) { qpos.position(r); qpos.get(dest, 0, getBucketSize(qcode)); } else {
       * System.arraycopy(qposa, r, dest, 0, getBucketSize(qcode)); } // g.logmsg(" qpos = [%s]%n",
       * Strings.join(" ",newpos, 0, newactive));
       */
   }

   // public static class Bucket
   // {
   // private final int[] a;
   // private final int len;
   // private final int offset;
   //
   // Bucket(int[] array, int offset, int len) {
   // if (array==null)
   // throw new NullPointerException();
   // a = array;
   // if (offset > a.length)
   // throw new IndexOutOfBoundsException("offset > length ("+offset+" > "+a.length+")");
   // if (len < 0 || len+offset > a.length)
   // throw new IndexOutOfBoundsException("len < 0 or len+offset > array length");
   // this.offset = offset;
   // this.len = len;
   // }
   //
   // public int size() {
   // return len;
   // }
   //
   // public int get(int i) {
   // return a[offset + i];
   // }
   // }
}
