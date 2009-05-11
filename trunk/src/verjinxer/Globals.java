package verjinxer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;

import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.util.ArrayFile;
import verjinxer.util.BitArray;
import verjinxer.util.HugeByteArray;
import verjinxer.util.HugeIntArray;
import verjinxer.util.ProjectInfo;
import verjinxer.util.StringUtils;

import com.spinn3r.log5j.Logger;

/**
 * 
 * @author Sven Rahmann
 * 
 * TODO 
 */
public class Globals {
   private static final Logger log = Logger.getLogger(Globals.class);

   static {
      log.setLevel(Level.INFO);
      // only print the message itself (%m), nothing else
      log.addAppender(new ConsoleAppender(new PatternLayout("%m%n"), ConsoleAppender.SYSTEM_ERR));
   }

   public final static String programname = "VerJInxer";
   public final static String version = "0.4";

   private final ArrayFile arf;
   
   /** prefix to diagnostic messages */
   public static String cmdname = programname;
   
   /** all arguments to Main for logfile */
   String[] action = null;
   
   /** write to project log? */
   boolean plog = true;
   
   /** project (working) directory */
   public String dir = "";
   public String outdir = ""; // output dir -- do not use lightly!

   FileAppender projectlog = null;

   public Globals() {
      arf = new ArrayFile(); // array file to be used
   }

   // TODO mark as deprecated
   public static Logger getLogger() {
      return log;
   }

   // TODO rename
   public final void startProjectLogging(String projectname, boolean startnew) {
      if (!plog)
         return;
      try {
         projectlog = new FileAppender(new PatternLayout("%m%n"), projectname
               + FileNameExtensions.log, !startnew);
         log.addAppender(projectlog);

         // TODO previously, this was only logged to the project log file
         log.info("# %s", new Date());
         log.info("# \"%s\"", StringUtils.join("\" \"", action));
      } catch (IOException ex) {
         log.warn("%s: could not open project log '%s'; continuing...", programname, projectname);
      }
   }

   public void startProjectLogging(ProjectInfo project, boolean startnew) {
      startProjectLogging(project.getName(), startnew);
   }

   public void startProjectLogging(ProjectInfo project) {
      startProjectLogging(project.getName(), false);
   }

   public final void startProjectLogging(String fname) {
      startProjectLogging(fname, false);
   }

   @Deprecated
   public void setQuiet(boolean b) {
      if (b) {
         log.setLevel(Level.WARN);
      } else {
         log.setLevel(Level.INFO);
      }
   }

   public final void stopplog() {
      // TODO is this necessary?
      if (projectlog != null)
         projectlog.close();
   }

   @Deprecated
   public static void terminate(int exitcode) {
      System.exit(exitcode);
   }

   @Deprecated
   public static void terminate(String msg) {
      log.error("%s", msg);
      System.exit(1);
   }

   /**************************************************************************/

   /** read alphabet map file */
   public static final Alphabet readAlphabet(String fname) {
      Alphabet alphabet = null;
      try {
         alphabet = Alphabet.fromFile(fname);
      } catch (IOException ex) {
         log.warn("%s: could not read alphabet '%s'. Stop.", cmdname, fname);
         terminate(1);
      }
      return alphabet;
   }

   // ======================= slurp methods with diagnostics ==================

   /**
    * slurp the contents of a file into a byte[] while printing diagnostics. Terminate the program
    * when an error occurs.
    * 
    * @param file
    *           the name of the file to be read
    * @return the newly created byte[] with the file's contents
    */
   public byte[] slurpByteArray(String file) {
      byte[] a = null;
      log.info("%s: reading '%s' into memory...", cmdname, file);
      try {
         a = arf.setFilename(file).readArray(a);
      } catch (IOException ex) {
         ex.printStackTrace();
         log.warn("%s: could not read '%s'. Stop.", cmdname, file);
         terminate(1);
      }
      return a;
   }

   /**
    * slurp selected contents of a file into a byte[] while printing diagnostics. Terminate the
    * program when an error occurs.
    * 
    * @param file
    *           the name of the file to be read
    * @param startindex
    *           where to start reading the file (inclusive)
    * @param stopindex
    *           where to stop reading the file (exclusive); -1 reads the whole file.
    * @param a
    *           an existing array or 'null'. If the existing array has length at least
    *           stopindex-startindex, its first elements are filled with the contents of the file.
    *           Otherwise, a new array of the correct size is created.
    * @return the newly created byte[] or a with the file's contents.
    */
   public byte[] slurpByteArray(String file, long startindex, long stopindex, byte[] a) {
      // log.info("%s: reading '%s' [%d..%d] into memory...", cmdname, file, startindex, stopindex);
      try {
         a = arf.setFilename(file).readArray(a, 0, (int) (stopindex - startindex), startindex);
      } catch (IOException ex) {
         ex.printStackTrace();
         log.warn("%s: could not read '%s'. Stop.", cmdname, file);
         terminate(1);
      }
      return a;
   }

   /**
    * slurp the contents of a file into an array while printing diagnostics. Terminate the program
    * when an error occurs.
    * 
    * @param file
    *           the name of the file to be read
    * @return the newly created HugeByteArray with the file's contents
    */
   public HugeByteArray slurpHugeByteArray(final String file) {
      HugeByteArray a = null;
      log.info("%s: reading '%s' into memory...", cmdname, file);
      try {
         a = HugeByteArray.fromFile(file);
      } catch (IOException ex) {
         ex.printStackTrace();
         log.warn("%s: could not read '%s'. Stop.", cmdname, file);
         terminate(1);
      }
      return a;
   }

   /**
    * slurp the contents of a file into an array while printing diagnostics. Terminate the program
    * when an error occurs.
    * 
    * @param file
    *           the name of the file to be read
    * @return the newly created HugeByteArray with the file's contents
    */
   public HugeIntArray slurpHugeIntArray(final String file) {
      HugeIntArray a = null;
      log.info("%s: reading '%s' into memory...", cmdname, file);
      try {
         a = HugeIntArray.fromFile(file);
      } catch (IOException ex) {
         ex.printStackTrace();
         log.warn("%s: could not read '%s'. Stop.", cmdname, file);
         terminate(1);
      }
      return a;
   }

   /**
    * slurp the contents of a file into an int[] while printing diagnostics. Terminate the program
    * when an error occurs.
    * 
    * @param file
    *           the name of the file to be read
    * @return the newly created int[] with the file's contents
    */
   public int[] slurpIntArray(String file) {
      int[] a = null;
      log.info("%s: reading '%s' into memory...", cmdname, file);
      try {
         a = arf.setFilename(file).readArray(a);
      } catch (IOException ex) {
         ex.printStackTrace();
         log.warn("%s: could not read '%s'. Stop.", cmdname, file);
         terminate(1);
      }
      return a;
   }

   /**
    * slurp the contents of a file into an int[] while printing diagnostics. Terminate the program
    * when an error occurs.
    * 
    * @param file
    *           the name of the file to be read
    * @param a
    *           an existing array to be used if large enough.
    * @return the int[] with the file's contents
    */
   public int[] slurpIntArray(String file, int[] a) {
      log.info("%s: reading '%s' into memory...", cmdname, file);
      try {
         a = arf.setFilename(file).readArray(a);
      } catch (IOException ex) {
         ex.printStackTrace();
         log.warn("%s: could not read '%s'. Stop.", cmdname, file);
         terminate(1);
      }
      return a;
   }

   /**
    * slurp the contents of a file into a long[] while printing diagnostics. Terminate the program
    * when an error occurs.
    * 
    * @param file
    *           the name of the file to be read
    * @return the newly created array with the file's contents
    */
   public long[] slurpLongArray(String file) {
      long[] a = null;
      log.info("%s: reading '%s' into memory...", cmdname, file);
      try {
         a = arf.setFilename(file).readArray(a);
      } catch (IOException ex) {
         ex.printStackTrace();
         log.warn("%s: could not read '%s'. Stop.", cmdname, file);
         terminate(1);
      }
      return a;
   }

   /**
    * slurp the contents of a file into an int[] while printing diagnostics. Terminate the program
    * when an error occurs.
    * 
    * @param file
    *           the name of the file to be read
    * @param a
    *           an existing array to be used if large enough.
    * @return the int[] with the file's contents
    */
   long[] slurpIntArray(String file, long[] a) {
      log.info("%s: reading '%s' into memory...", cmdname, file);
      try {
         a = arf.setFilename(file).readArray(a);
      } catch (IOException ex) {
         ex.printStackTrace();
         log.warn("%s: could not read '%s'. Stop.", cmdname, file);
         terminate(1);
      }
      return a;
   }

   /**
    * map the contents of a file into a ByteBuffer while printing diagnostics. Terminate the program
    * when an error occurs.
    * 
    * @param file
    *           the name of the file to be read
    * @return the ByteBuffer with the mapped file's contents
    * TODO outsourcing in other class
    */
   public static ByteBuffer mapR(final String file) {
      ByteBuffer b = null;
      log.info("%s: memory-mapping '%s'...", cmdname, file);
      try {
         b = new ArrayFile(file, 0).mapR();
      } catch (IOException ex) {
         log.warn("%s: could not map '%s'; %s. Stop.", cmdname, file, ex);
         terminate(1);
      }
      return b;
   }

   // ======================= text file readers =================================

   public static ArrayList<String> slurpTextFile(String file, int ll) {
      if (ll <= 0)
         ll = 32;
      log.info("%s: reading '%s'; expecting %d lines...", cmdname, file, ll);
      ArrayList<String> lines = new ArrayList<String>(ll);
      String s;
      BufferedReader br = null;
      try {
         br = new BufferedReader(new FileReader(file));
         while ((s = br.readLine()) != null)
            lines.add(s);
         br.close();
      } catch (IOException ex) {
         try {
            if (br != null)
               br.close();
         } catch (IOException exx) {
         }
         log.warn("%s: could not read '%s'; %s", cmdname, file, ex);
         terminate(1);
      }
      return lines;
   }

   // ======================= file writers ====================================

   /**
    * Dump the first given number of ints of the given array to the given file, while printing
    * diagnostics. Terminate the program when an error occurs.
    * 
    * @param file
    *           the name of the file to be written
    * @param a
    *           the array to be dumped
    * @param start
    *           index at which to start dumping a
    * @param len
    *           the number of elements to dump
    */
   final void dumpIntArray(final String file, final int[] a, final int start, final int len) {
      log.info("%s: writing '%s'...", cmdname, file);
      try {
         arf.setFilename(file).writeArray(a, start, len);
      } catch (IOException ex) {
         log.warn("%s: could not write '%s'; %s", cmdname, file, ex);
         terminate(1);
      }
   }

   /**
    * Dump the given array to the given file while printing diagnostics. Terminate the program when
    * an error occurs.
    * 
    * @param file
    *           the name of the file to be written
    * @param a
    *           the array to be dumped
    */
   public final void dumpIntArray(final String file, final int[] a) {
      dumpIntArray(file, a, 0, a.length);
   }

   /**
    * Dump the first given number of ints of the given array to the given file, while printing
    * diagnostics. Terminate the program when an error occurs.
    * 
    * @param file
    *           the name of the file to be written
    * @param a
    *           the array to be dumped
    * @param start
    *           index at which to start dumping a
    * @param len
    *           the number of elements to dump
    */
   final void dumpLongArray(final String file, final long[] a, final int start, final int len) {
      log.info("%s: writing '%s'...", cmdname, file);
      try {
         arf.setFilename(file).writeArray(a, start, len);
      } catch (IOException ex) {
         log.warn("%s: could not write '%s'; %s", cmdname, file, ex);
         terminate(1);
      }
   }

   /**
    * Dump the given array to the given file while printing diagnostics. Terminate the program when
    * an error occurs.
    * 
    * @param file
    *           the name of the file to be written
    * @param a
    *           the array to be dumped
    */
   final void dumpLongArray(final String file, final long[] a) {
      dumpLongArray(file, a, 0, a.length);
   }

   /**
    * Dump the first given number of bytes of the given array to the given file, while printing
    * diagnostics. Terminate the program when an error occurs.
    * 
    * @param file
    *           the name of the file to be written
    * @param a
    *           the array to be dumped
    * @param start
    *           index at which to start dumping a
    * @param len
    *           the number of elements to dump
    */
   void dumpByteArray(final String file, final byte[] a, final int start, final int len) {
      log.info("%s: writing '%s'...", cmdname, file);
      try {
         arf.setFilename(file).writeArray(a, start, len);
      } catch (IOException ex) {
         log.warn("%s: could not write '%s'; %s", cmdname, file, ex);
         terminate(1);
      }
   }

   /**
    * Dump the given array to the given file while printing diagnostics. Terminate the program when
    * an error occurs.
    * 
    * @param file
    *           the name of the file to be written
    * @param a
    *           the array to be dumped
    */
   void dumpByteArray(final String file, final byte[] a) {
      dumpByteArray(file, a, 0, a.length);
   }

   /**
    * Write the given BitArray to a file, while printing diagnostics. Terminate the program when an
    * error occurs.
    * 
    * @param filename
    *           the name of the file
    * @param ba
    *           the bit array
    */
   public void dumpBitArray(final String filename, BitArray ba) {
      log.info("%s: writing '%s'...", cmdname, filename);
      try {
         ba.writeTo(arf.setFilename(filename));
      } catch (IOException ex) {
         log.warn("%s: could not write '%s'; %s", cmdname, filename, ex);
         terminate(1);
      }
   }

   /**
    * Read a BitArray from a file, while printing diagnostics. Terminate the program when an error
    * occurs.
    * 
    * @param filename
    *           the name of the file
    * @return the bit array
    */
   public BitArray slurpBitArray(final String filename) {
      log.info("%s: reading '%s'...", cmdname, filename);
      BitArray ba = null;
      try {
         ba = BitArray.readFrom(arf.setFilename(filename));
      } catch (IOException ex) {
         log.warn("%s: could not read '%s'; %s", cmdname, filename, ex);
         terminate(1);
      }
      return ba;
   }

}
