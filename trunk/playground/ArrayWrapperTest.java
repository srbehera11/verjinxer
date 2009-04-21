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
      
      final int[] a = new int[LENGTH];
      LargeArrayWrapper law = new LargeArrayWrapper(LENGTH);
      SmallArrayWrapper saw = new SmallArrayWrapper(LENGTH);
      HugeIntArray hia = new HugeIntArray(LENGTH);
      // Integer[] ai = new Integer[LENGTH];
      // ArrayList<Integer> al = new ArrayList<Integer>(LENGTH);

      for (int i = 0; i < LENGTH; ++i) {
         int v = r.nextInt();
         a[i] = v;
         law.set(i, v+1);
         saw.set(i, v+3);
         hia.set(i, v+4);
         // al.add(v);
         // ai[i] = v;
      }
      // al.trimToSize();

      TicToc t = new TicToc();
      for (int run = 0; run < 10; ++run) {
         long actual_sum = 0;
         long sum;
         double x;
                 
         t.tic();
         actual_sum = 0;
         for (int k = 0; k < RUNS; ++k) {
            for (int i = 1; i < LENGTH; ++i) {
               actual_sum += a[i];
               a[i-1] = (int)actual_sum;
            }
         }
         x = t.tocs();
         System.out.format("%30s: %6.1fms\n", "int array", x*1000);
         
         t.tic();
         actual_sum = 0;
         for (int k = 0; k < RUNS; ++k) {
            for (int i = 1; i < LENGTH; ++i) {
               actual_sum += a[i];
               a[i-1] = (int)actual_sum;
            }
         }
         x = t.tocs();
         System.out.format("%30s: %6.1fms\n", "int array", x*1000);
         
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
               hia.set(i-1, (int)sum);
            }
         }
         x = t.tocs();
         System.out.format("%30s: %6.1fms\n", "HugeIntArray", x*1000);
//         assert sum == actual_sum;

         t.tic();
         sum = 0;
         for (int k = 0; k < RUNS; ++k) {
            sum = 0;
            for (int i = 1; i < LENGTH; ++i) {
               sum += law.get(i);
               law.set(i-1, (int)sum);
            }
         }
         x = t.tocs();
//         assert sum == actual_sum;
         System.out.format("%30s: %6.1fms\n", "LargeArrayWrapper", x*1000);
         
         t.tic();
         for (int k = 0; k < RUNS; ++k) {
            sum = 0;
            for (int i = 1; i < LENGTH; ++i) {
               sum += saw.get(i);
               saw.set(i-1, (int)sum);
            }
         }
         x = t.tocs();
//         assert sum == actual_sum;
         System.out.format("%30s: %6.1fms\n", "ShortArrayWrapper", x*1000);
         
         t.tic();
         actual_sum = 0;
         for (int k = 0; k < RUNS; ++k) {
            for (int i = 1; i < LENGTH; ++i) {
               actual_sum += a[i];
               a[i-1] = (int)actual_sum;
            }
         }
         x = t.tocs();
         System.out.format("%30s: %6.1fms\n", "int array", x*1000);
         

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
//
//interface ArrayWrapper extends Iterable<Integer> {
//
//   public abstract void set(long i, int value);
//
//   public abstract int get(long i);
//
//   public abstract Iterator<Integer> iterator();
//
//}


class SmallArrayWrapper {
   int[] array;
   SmallArrayWrapper(int length) {
      array = new int[length];
   }

   /* (non-Javadoc)
    * @see ArrayWrapper#set(int, int)
    */
   public final void set(int i, int value) {
      array[i] = value;
   }

   /* (non-Javadoc)
    * @see ArrayWrapper#get(int)
    */
   public final int get(int i) {
      return array[i];
   }

   /* (non-Javadoc)
    * @see ArrayWrapper#iterator()
    */
//   @Override
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


