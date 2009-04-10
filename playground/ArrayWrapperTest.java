import java.util.Iterator;
import java.util.Random;

import verjinxer.util.HugeIntArray;
import verjinxer.util.TicToc;

/**
 * This is a simple class to test how fast a class is that simply wraps an int array.
 * 
 * The results so far are that the ArrayWrapper is consistently a bit faster (!) than using a raw
 * array when only reading entries and that there is no difference when both reading and writing.
 * 
 * @author Marcel Martin
 * 
 */
public class ArrayWrapperTest {

   final static int LENGTH = 100000;
   final static int RUNS = 100;

   public static void main(String[] args) {
      Random r = new Random();
      int[] a = new int[LENGTH];

      HugeIntArray hia = new HugeIntArray(LENGTH);
      LargeArrayWrapper hia2 = new LargeArrayWrapper(LENGTH);
      // Integer[] ai = new Integer[LENGTH];
      SmallArrayWrapper aw = new SmallArrayWrapper(LENGTH);
      // ArrayList<Integer> al = new ArrayList<Integer>(LENGTH);

      for (int i = 0; i < LENGTH; ++i) {
         int v = r.nextInt();
         a[i] = v;
         aw.set(i, v);
         hia.set(i, v);
         hia2.set(i, v);
         // al.add(v);
         // ai[i] = v;
      }
      // al.trimToSize();

      for (int run = 0; run < 10; ++run) {
         TicToc t = new TicToc();
         long actual_sum = 0;
         long sum;

         t.tic();
         for (int k = 0; k < RUNS; ++k) {
            for (int i = 1; i < LENGTH; ++i) {
               actual_sum += a[i];
               a[i-1] = (int)actual_sum;
            }
         }
         System.out.println("int array: " + t.tocs());
         
         /*
          * t.tic(); result = 0; for (int v : a) { result += v; // a[i-1] = (int)result; }
          * System.out.println("sum:" + result);
          * System.out.println("elapsed for int array using iterator: " + t.tocs());
          */

         t.tic();
         sum = 0;
         for (int k = 0; k < RUNS; ++k) {
            sum = 0;
            for (int i = 1; i < LENGTH; ++i) {
               sum += hia.get(i);
               aw.set(i-1, (int)sum);
            }
         }
         assert sum == actual_sum;
         System.out.println("HugeIntArray: " + t.tocs());

         t.tic();
         for (int k = 0; k < RUNS; ++k) {
            sum = 0;
            for (int i = 1; i < LENGTH; ++i) {
               sum += aw.get(i);
               aw.set(i-1, (int)sum);
   
            }
         }
         assert sum == actual_sum;
         System.out.println("Wrapper: " + t.tocs());

         t.tic();
         sum = 0;
         for (int k = 0; k < RUNS; ++k) {
            sum = 0;
            for (int i = 1; i < LENGTH; ++i) {
               sum += hia2.get(i);
               aw.set(i-1, (int)sum);
            }
         }
         assert sum == actual_sum;
         System.out.println("HugeIntArray2: " + t.tocs());


         /*
          * t.tic(); result = 0; for (int i = 1; i < LENGTH; ++i) { result += al.get(i); //
          * aw.set(i-1, (int)result); } System.out.println("sum:" + result);
          * System.out.println("elapsed for ArrayList: " + t.tocs());
          */
         /*
          * t.tic(); result = 0; int prev = 0; int previ = 0; for (int v : aw) { result += v; }
          * System.out.println("sum:" + result);
          * System.out.println("elapsed for Wrapper with iterator: " + t.tocs());
          */

         /*
          * this is extremely slow! t.tic(); result = 0; for (int i = 1; i < LENGTH; ++i) { result
          * += ai[i]; ai[i-1] = (int)result; } System.out.println("sum:" + result);
          * System.out.println("elapsed for Integer array: " + t.tocs());
          */

         System.out.println();
      }
   }
}

interface ArrayWrapper extends Iterable<Integer> {

   public abstract void set(long i, int value);

   public abstract int get(long i);

   public abstract Iterator<Integer> iterator();

}


class SmallArrayWrapper implements ArrayWrapper {
   int[] array;
   SmallArrayWrapper(int length) {
      array = new int[length];
   }

   /* (non-Javadoc)
    * @see ArrayWrapper#set(int, int)
    */
   public final void set(long i, int value) {
      array[(int)i] = value;
   }

   /* (non-Javadoc)
    * @see ArrayWrapper#get(int)
    */
   public final int get(long i) {
      return array[(int)i];
   }

   /* (non-Javadoc)
    * @see ArrayWrapper#iterator()
    */
   @Override
   public Iterator<Integer> iterator() {
      return new Iterator<Integer>() {

         int i = 0; // points to next item in array

         @Override
         public boolean hasNext() {
            return i < array.length;
         }

         @Override
         public Integer next() {
            return array[i];
         }

         @Override
         public void remove() {
            throw new UnsupportedOperationException();
         }

      };
   }
}


class LargeArrayWrapper implements ArrayWrapper {
   private int[][] arrays;
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

   public void set(long i, int value) {
      arrays[(int)(i >> BITS)][(int)(i & BITMASK_LOW)] = value;
   }

   public int get(long i) {
      return arrays[(int)(i >> BITS)][(int)(i & BITMASK_LOW)];
   }

   @Override
   public Iterator<Integer> iterator() {
      // TODO Auto-generated method stub
      return null;
   }
}
