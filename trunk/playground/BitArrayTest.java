import java.util.BitSet;


public class BitArrayTest {
   
   public static void main (String args[]) {
      
      Runtime r = Runtime.getRuntime();
      
      final int size = Integer.MAX_VALUE;
      
      long l = r.freeMemory();
      long time = System.currentTimeMillis();
      boolean b[] = new boolean[size];
      for (int i = 0; i < b.length; i++) {
         b[i] = true;
      }
      System.out.printf("Memory needed for boolean array with size %d: %d%n", b.length, l - r.freeMemory());
      System.out.printf("Time needed for boolean array with size %d: %d%n", b.length, System.currentTimeMillis() - time);
      
//      l = r.freeMemory();
//      long ll[] = new long[size];
//      for (int i = 0; i < ll.length; i++) {
//         ll[i] = 2;
//      }
//      System.out.printf("Memory needed for long array with size %d: %d%n", ll.length, l - r.freeMemory());
      
//         long l = r.freeMemory();
//         long time = System.currentTimeMillis();
//         BitSet bs = new BitSet(size);
//         System.out.printf("bs.cardinality(): %d%n",bs.cardinality());
//         System.out.printf("bs.length(): %d%n",bs.length());
//         System.out.printf("bs.size(): %d%n",bs.size());
//         for (int i = 0; i < size; i++) {
//            bs.set(i);
//         }
//         System.out.printf("bs.cardinality(): %d%n",bs.cardinality());
//         System.out.printf("bs.length(): %d%n",bs.length());
//         System.out.printf("bs.size(): %d%n",bs.size());
//         System.out.printf("Memory needed for bit set with size %d: %d%n", size, l - r.freeMemory());
//         System.out.printf("Time needed for bit set with size %d: %d%n", size, System.currentTimeMillis() - time);
      
      
   }

}
