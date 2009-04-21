import java.util.Iterator;

public class LargeArrayWrapper {
   final private int[][] arrays;
   final public long length;
   
   final private static int BITS = 30;
   final private static int BINSIZE = 1 << BITS;
   final private static int BITMASK_HIGH = (-1) << BITS;
   final private static int BITMASK_LOW = ~BITMASK_HIGH;

   LargeArrayWrapper(int length) {
      this.length = length;
      final int bins = (length >> BITS) + 1; // last bin is potentially empty
      arrays = new int[bins][];
      for (int i = 0; i < bins - 1; ++i) {
         arrays[i] = new int[BINSIZE];
      }
      arrays[bins - 1] = new int[length & BITMASK_LOW];
   }

   public final void set(long i, int value) {
      arrays[(int)(i >> BITS)][(int)(i & BITMASK_LOW)] = value;
   }

   public final int get(long i) {
      return arrays[(int)(i >> BITS)][(int)(i & BITMASK_LOW)];
   }

//   @Override
   public Iterator<Integer> iterator() {
      // TODO Auto-generated method stub
      return null;
   }
}

