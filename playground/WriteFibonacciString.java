

import java.io.File;
import java.io.IOException;
import verjinxer.sequenceanalysis.FastaFile;
import verjinxer.util.MathUtils;

/**
 * executable class that writes a Fibonacci string to a FASTA file
 * @author Sven Rahmann
 */
public class WriteFibonacciString {
   
   private WriteFibonacciString() {
   }
  
  public static final long defaultlength = 30000000L;
  
  /** call this class with arguments (n, fname),
   * where n is the length of the Fibonacci string,
   * and fname is an optional filename,
   * to write a FASTA file with this string as its contents.
   * @param args  the arguments n and fname
   */
  public static void main(String[] args) {
    long fmax = (args.length<1? defaultlength : Long.parseLong(args[0]));
    File file = (args.length < 2 ? null : new File(args[1]));
    int n = MathUtils.fibFind(fmax);
    if (file == null) {
         file = new File(String.format("fib-%d.fa", n));
    }
    System.out.printf("Computing FS(%d) of length %d...%n", n, MathUtils.fib(n));
    String s = MathUtils.fibString(n);
    FastaFile ff = new FastaFile(file);
    System.out.printf("Writing file...%n");
    try {
      ff.open(FastaFile.FastaMode.WRITE);
      ff.writeString(s,file.getName());
      ff.close();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
    System.out.printf("Done.%n");
  }

}
