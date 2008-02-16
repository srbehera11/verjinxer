/*
 * WriteFibonacciString.java
 *
 * Created on May 3, 2007, 5:33 PM
 *
 */

package verjinxer.util;

import java.io.IOException;
import verjinxer.sequenceanalysis.FastaFile;

/**
 * executable class that writes a Fibonacci string to a FASTA file
 * @author Sven Rahmann
 */
public class WriteFibonacciString {
   
   private WriteFibonacciString() {
   }
  
  public static final long defaultlength = 14000000L;
  
  /** call this class with arguments (n, fname),
   * where n is the length of the Fibonacci string,
   * and fname is an optional filename,
   * to write a FASTA file with this string as its contents.
   * @param args  the arguments n and fname
   */
  public static void main(String[] args) {
    long fmax = (args.length<1? defaultlength : Long.parseLong(args[0]));
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
