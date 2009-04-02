/*
 * Globals.java Created on April 12, 2007, 9:17 AM
 */

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
 */
public class Globals {

   static final Logger log = Logger.getLogger(Globals.class);

   static {
      log.setLevel(Level.INFO);
      // only print the message itself (%m), nothing else
      log.addAppender(new ConsoleAppender(new PatternLayout("%m%n"), ConsoleAppender.SYSTEM_ERR));
   }

   public final static String programname = "VerJInxer";
   public final static String version = "0.4";

   private final ArrayFile arf;

   String cmdname = programname; // prefix to diagnostic messages
   String[] action = null; // all arguments to Main for logfile
   boolean plog = true; // write to project log?
   String dir = ""; // project (working) dir
   String outdir = ""; // output dir -- do not use lightly!

   FileAppender projectlog = null;

   public Globals() {
      arf = new ArrayFile(); // array file to be used
   }

   public static void errormsg(String format, Object... args) {
      if (format.endsWith("%n")) {
         format = format.substring(0, format.length() - 2);
      }
      log.error(format, args);
   }

   public static void warnmsg(String format, Object... args) {
      if (format.endsWith("%n")) {
         format = format.substring(0, format.length() - 2);
      }
      log.warn(format, args);
   }

   public static void logmsg(String format, Object... args) {
      if (format.endsWith("%n")) {
         format = format.substring(0, format.length() - 2);
      }
      log.info(format, args);
   }

   // TODO rename
   public final void startProjectLogging(String projectname, boolean startnew) {
      if (!plog)
         return;
      try {
         projectlog = new FileAppender(new PatternLayout("%m"), projectname
               + FileNameExtensions.log, !startnew);
         log.addAppender(projectlog);

         // TODO previously, this was only logged to the project log file
         log.info("%n# %s", new Date());
         log.info("# \"%s\"", StringUtils.join("\" \"", action));
      } catch (IOException ex) {
         warnmsg("%s: could not open project log '%s'; continuing...", programname, projectname);
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

   public static void terminate(int exitcode) {
      System.exit(exitcode);
   }

   public static void terminate(String msg) {
      errormsg("%s%n", msg);
      System.exit(1);
   }

   /**************************************************************************/

   /** read alphabet map file */
   final Alphabet readAlphabet(String fname) {
      Alphabet alphabet = null;
      try {
         alphabet = Alphabet.fromFile(fname);
      } catch (IOException ex) {
         warnmsg("%s: could not read alphabet '%s'. Stop.%n", cmdname, fname);
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
   byte[] slurpByteArray(String file) {
      byte[] a = null;
      logmsg("%s: reading '%s' into memory...%n", cmdname, file);
      try {
         a = arf.setFilename(file).readArray(a);
      } catch (IOException ex) {
         ex.printStackTrace();
         warnmsg("%s: could not read '%s'. Stop.%n", cmdname, file);
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
   byte[] slurpByteArray(String file, long startindex, long stopindex, byte[] a) {
      // logmsg("%s: reading '%s' [%d..%d] into memory...%n", cmdname, file, startindex, stopindex);
      try {
         a = arf.setFilename(file).readArray(a, 0, (int) (stopindex - startindex), startindex);
      } catch (IOException ex) {
         ex.printStackTrace();
         warnmsg("%s: could not read '%s'. Stop.%n", cmdname, file);
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
      logmsg("%s: reading '%s' into memory...%n", cmdname, file);
      try {
         a = HugeByteArray.fromFile(file);
      } catch (IOException ex) {
         ex.printStackTrace();
         warnmsg("%s: could not read '%s'. Stop.%n", cmdname, file);
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
      logmsg("%s: reading '%s' into memory...%n", cmdname, file);
      try {
         a = HugeIntArray.fromFile(file);
      } catch (IOException ex) {
         ex.printStackTrace();
         warnmsg("%s: could not read '%s'. Stop.%n", cmdname, file);
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
      logmsg("%s: reading '%s' into memory...%n", cmdname, file);
      try {
         a = arf.setFilename(file).readArray(a);
      } catch (IOException ex) {
         ex.printStackTrace();
         warnmsg("%s: could not read '%s'. Stop.%n", cmdname, file);
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
   int[] slurpIntArray(String file, int[] a) {
      logmsg("%s: reading '%s' into memory...%n", cmdname, file);
      try {
         a = arf.setFilename(file).readArray(a);
      } catch (IOException ex) {
         ex.printStackTrace();
         warnmsg("%s: could not read '%s'. Stop.%n", cmdname, file);
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
   long[] slurpLongArray(String file) {
      long[] a = null;
      logmsg("%s: reading '%s' into memory...%n", cmdname, file);
      try {
         a = arf.setFilename(file).readArray(a);
      } catch (IOException ex) {
         ex.printStackTrace();
         warnmsg("%s: could not read '%s'. Stop.%n", cmdname, file);
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
      logmsg("%s: reading '%s' into memory...%n", cmdname, file);
      try {
         a = arf.setFilename(file).readArray(a);
      } catch (IOException ex) {
         ex.printStackTrace();
         warnmsg("%s: could not read '%s'. Stop.%n", cmdname, file);
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
    */
   public ByteBuffer mapR(final String file) {
      ByteBuffer b = null;
      logmsg("%s: memory-mapping '%s'...%n", cmdname, file);
      try {
         b = new ArrayFile(file, 0).mapR();
      } catch (IOException ex) {
         warnmsg("%s: could not map '%s'; %s. Stop.%n", cmdname, file, ex);
         terminate(1);
      }
      return b;
   }

   // ======================= text file readers =================================

   ArrayList<String> slurpTextFile(String file, int ll) {
      if (ll <= 0)
         ll = 32;
      logmsg("%s: reading '%s'; expecting %d lines...%n", cmdname, file, ll);
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
         warnmsg("%s: could not read '%s'; %s%n", cmdname, file, ex);
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
      logmsg("%s: writing '%s'...%n", cmdname, file);
      try {
         arf.setFilename(file).writeArray(a, start, len);
      } catch (IOException ex) {
         warnmsg("%s: could not write '%s'; %s%n", cmdname, file, ex);
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
   final void dumpIntArray(final String file, final int[] a) {
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
      logmsg("%s: writing '%s'...%n", cmdname, file);
      try {
         arf.setFilename(file).writeArray(a, start, len);
      } catch (IOException ex) {
         warnmsg("%s: could not write '%s'; %s%n", cmdname, file, ex);
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
      logmsg("%s: writing '%s'...%n", cmdname, file);
      try {
         arf.setFilename(file).writeArray(a, start, len);
      } catch (IOException ex) {
         warnmsg("%s: could not write '%s'; %s%n", cmdname, file, ex);
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
   void dumpBitArray(final String filename, BitArray ba) {
      logmsg("%s: writing '%s'...%n", cmdname, filename);
      try {
         ba.writeTo(arf.setFilename(filename));
      } catch (IOException ex) {
         warnmsg("%s: could not write '%s'; %s%n", cmdname, filename, ex);
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
   BitArray slurpBitArray(final String filename) {
      logmsg("%s: reading '%s'...%n", cmdname, filename);
      BitArray ba = null;
      try {
         ba = BitArray.readFrom(arf.setFilename(filename));
      } catch (IOException ex) {
         warnmsg("%s: could not read '%s'; %s%n", cmdname, filename, ex);
         terminate(1);
      }
      return ba;
   }

}
