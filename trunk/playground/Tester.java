

import java.io.FileNotFoundException;
import java.io.IOException;

import verjinxer.util.TicToc;

public class Tester {

   
   //  test with:
   // time java -ea -Xmx2000m -cp /home/martin/workspace/verjinxer/bin/ verjinxer.Tester chr18
   public static void main(String[] args) throws IOException, FileNotFoundException {
      TicToc timer = new TicToc();
      timer.tic();

//      run1(g, args[0]);
      
//      FileChannel channel = new FileInputStream(args[0]+".qpos").getChannel();
//      long size = channel.size();
//      IntBuffer buf = channel.map(MapMode.READ_ONLY, 0, size).order(ByteOrder.nativeOrder()).asIntBuffer();
//      ByteBuffer buf = channel.map(MapMode.READ_ONLY, 0, size);//.order(ByteOrder.nativeOrder()).asIntBuffer();
//      System.out.println("done mapping 1 ...\n\n");
//      System.out.println("first byte:"+buf.get(0));
      
   }

//   static void run1(Globals g, String fname) throws IOException {
//      TicToc timer = new TicToc();
//      timer.tic();
//      QGramIndex qgi = new QGramIndex(g, fname+".qpos", fname+".qbck");
//      
//      System.out.printf("reading (or mapping) took %fs%n", timer.tocs());
//      timer.tic();
//      Random r = new Random(15);
//
//      int buckets = qgi.getNumberOfBuckets();
//      int[] oldpositions = null;
//      int[] newpositions = null;
//
//      long c = 0;
//      long t = 0;
//      long x = 0;
//      for (int i = 0; i < 10000000; ++i) {
//         oldpositions = newpositions;
//         int qcode = r.nextInt(buckets);
//         newpositions = qgi.getQGramPositions(qcode);
//         assert newpositions.length == qgi.getBucketSize(qcode);
//         x += newpositions.length;
//         if (oldpositions == null) continue;
//         int n = (oldpositions.length < newpositions.length ) ? oldpositions.length : newpositions.length;
//
//         for (int j = 0; j < n; ++j) {
//            if (oldpositions[j] == newpositions[j])
//               c++;
//            t++;
//         }
//      }
//    
//      System.out.format("differences: %d of %d comparisons%n", c, t);
//      System.out.format("number of ints copied: %d%n", x);
//      double elapsed = timer.tocs();
//      System.out.format("time: %f (%f ns/int)%n", elapsed, elapsed/x*1e9);
//   }
//
//   /**
//    * @param args
//    */
//   public static void main2(String[] args) {
//      TicToc timer = new TicToc();
//      timer.tic();
//      Globals g = new Globals();
////      QGramIndex qgi = new QGramIndex(g, args[0]+".qpos", args[0]+".qbck", false);
//     
//      
////      int[] qbck;
////      int[] qposa;
////      qbck = g.slurpIntArray(args[0]+".qbck");
////      qposa = g.slurpIntArray(args[0]+".qpos");
//      
//      System.out.printf("reading took %fs%n", timer.tocs());
//      timer.tic();
//      Random r = new Random(15);
//      
////      int buckets = qbck.length-1;
//      
//      // Variante 4: (nur Indizes kopieren): 3.1s
////      int[] oldpositions = null;
////      int oldpositions_i = 0;
////      int oldpositions_l = 0;
////      int[] newpositions = null;
////      int newpositions_i = 0;
////      int newpositions_l = 0;
////      
////      long c = 0;
////      long t = 0;
////      long x = 0;
////      for (int i = 0; i < 10000000; ++i) {
////         oldpositions = newpositions;
////         oldpositions_i = newpositions_i;
////         oldpositions_l = newpositions_l;
////         int qcode = r.nextInt(buckets);
////         
////         newpositions = qposa;
////         newpositions_i = qbck[qcode];
////         newpositions_l = qbck[qcode+1] - newpositions_i;
////         
////         x += newpositions_l;
////         if (oldpositions == null) continue;
////         int n = (oldpositions_l < newpositions_l ) ? oldpositions_l : newpositions_l;
////
////         for (int j = 0; j < n; ++j) {
////            if (oldpositions[oldpositions_i+j] == newpositions[newpositions_i+j])
////               c++;
////            t++;
////         }
////
////      }
//
//
//      // This version: 3.4s
////      Slice oldpositions = null;
////      Slice newpositions = null;
////      
////      long c = 0;
////      long t = 0;
////      long x = 0;
////      for (int i = 0; i < 10000000; ++i) {
////         oldpositions = newpositions;
////         int qcode = r.nextInt(buckets);
////         
////         newpositions = new Slice(qposa, qbck[qcode], qbck[qcode+1] - qbck[qcode]);
////         
////         x += newpositions.size();
////         if (oldpositions == null) continue;
////         int n = (oldpositions.size() < newpositions.size() ) ? oldpositions.size() : newpositions.size();
////
////         for (int j = 0; j < n; ++j) {
////            if (oldpositions.get(j) == newpositions.get(j))
////               c++;
////            t++;
////         }
////
////      }
//      
//      
//      //      
////      int buckets = qgi.getNumberOfBuckets();
////      
////      List oldpositions = null;
////      int[] newpositions = null;
////      
////      long c = 0;
////      long t = 0;
////      long x = 0;
////      for (int i = 0; i < 10000000; ++i) {
////         oldpositions = newpositions;
////         int qcode = r.nextInt(buckets);
////         newpositions = qgi.getQGramPositions(qcode);
////         x += newpositions.size();
////         if (oldpositions == null) continue;
////         int n = (oldpositions.size() < newpositions.size() ) ? oldpositions.size() : newpositions.size();
////         
////         
////         for (int j = 0; j < n; ++j) {
////            if (oldpositions.get(j) == newpositions.get(j))
////               c++;
////            t++;
////         }
////         
////      }
//      
//      
////      System.out.format("differences: %d of %d comparisons%n", c, t);
////      System.out.format("number of ints copied: %d%n", x);
////      double elapsed = timer.tocs();
////      System.out.format("time: %f (%f ns/int)%n", elapsed, elapsed/x*1e9);
//   }
//
//   
//   public static void main3(String[] args) throws IOException, FileNotFoundException {
//      TicToc timer = new TicToc();
//      timer.tic();
//      Globals g = new Globals();
//
//      
////      FileChannel channel = new FileInputStream(args[0]+".qpos").getChannel();
////      long size = channel.size();
////      IntBuffer buf = channel.map(MapMode.READ_ONLY, 0, size).order(ByteOrder.nativeOrder()).asIntBuffer();
////      ByteBuffer buf = channel.map(MapMode.READ_ONLY, 0, size);//.order(ByteOrder.nativeOrder()).asIntBuffer();
////      System.out.println("done mapping 1 ...\n\n");
////      System.out.println("first byte:"+buf.get(0));
//      QGramIndex qgi = new QGramIndex(g, args[0]+".qpos", args[0]+".qbck");
//     
//      
//      System.out.printf("reading (or mapping) took %fs%n", timer.tocs());
//      timer.tic();
//      Random r = new Random(15);
//     
//
//      int buckets = qgi.getNumberOfBuckets();
//      int[] oldpositions = null;
//      int[] newpositions = null;
//
//      long c = 0;
//      long t = 0;
//      long x = 0;
//      for (int i = 0; i < 10000000; ++i) {
//         oldpositions = newpositions;
//         int qcode = r.nextInt(buckets);
//         newpositions = qgi.getQGramPositions(qcode);
//         assert newpositions.length == qgi.getBucketSize(qcode);
//         x += newpositions.length;
//         if (oldpositions == null) continue;
//         int n = (oldpositions.length < newpositions.length ) ? oldpositions.length : newpositions.length;
//
//         for (int j = 0; j < n; ++j) {
//            if (oldpositions[j] == newpositions[j])
//               c++;
//            t++;
//         }
//      }
//    
//    
//      System.out.format("differences: %d of %d comparisons%n", c, t);
//      System.out.format("number of ints copied: %d%n", x);
//      double elapsed = timer.tocs();
//      System.out.format("time: %f (%f ns/int)%n", elapsed, elapsed/x*1e9);
//      
//   }
//
   
   

}
