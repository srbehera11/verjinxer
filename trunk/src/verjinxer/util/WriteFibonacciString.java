/*
 * WriteFibonacciString.java
 *
 * Created on May 3, 2007, 5:33 PM
 *
 */

package rahmann.util;

import java.io.IOException;
import rahmann.sequenceanalysis.FastaFile;

/**
 *
 * @author Sven Rahmann
 */
public class WriteFibonacciString {
  
  public static void main(String[] args) {
    long fmax = (args.length<1? 30000000L : Long.parseLong(args[0]));
    String fname = (args.length<2? null : args[1]);
    int n = Math.fibFind(fmax);
    if (fname==null) fname = String.format("fib-%d.fa",n);
    System.out.printf("Computing FS(%d) of length %d...%n", n, Math.fib(n));
    String s = Math.fibString(n);
    FastaFile ff = new FastaFile(fname);
    System.out.printf("Writing file...%n");
    try {
      ff.open(FastaFile.FastaMode.WRITE);
      ff.writeString(s,fname);
      ff.close();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
    System.out.printf("Done.%n");
  }
  
}
