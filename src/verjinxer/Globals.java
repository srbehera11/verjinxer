/*
 * Globals.java
 *
 * Created on April 12, 2007, 9:17 AM
 *
 */

package verjinxer;

import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.*;
import verjinxer.sequenceanalysis.AlphabetMap;
import verjinxer.util.ArrayFile;
import verjinxer.util.Strings;

/**
 *
 * @author Sven Rahmann
 */
public class Globals {
  
  public final static String programname = "VerJInxer";
  public final static String version     = "0.4";
  public final static String extseq      = ".seq";
  public final static String extprj      = ".prj";
  public final static String extlog      = ".log";
  public final static String extdesc     = ".desc";
  public final static String extselect   = ".select-filter";
  public final static String extssp      = ".ssp";
  public final static String extalph     = ".alphabet";
  public final static String extqbck     = ".qbck";
  public final static String extqpos     = ".qpos";
  public final static String extqfreq    = ".qfreq";
  public final static String extqseqfreq = ".qsfrq";
  public final static String extrunseq   = ".runseq";
  public final static String extrunlen   = ".runlen";
  public final static String extrun2pos  = ".run2pos";
  public final static String extpos2run  = ".pos2run";
  public final static String extpos      = ".pos";      // suffix array
  public final static String extlcp      = ".lcp";      // lcp values
   
  private PrintStream  logger  = null;
  private PrintStream  loggerP = null;
  
  String       cmdname = programname; // prefix to diagnostic messages
  String[]     action  = null;        // all arguments to Main for logfile
  boolean      plog    = true;        // write to project log?
  String       dir     = "";          // project (working) dir
  String       outdir  = "";          // output dir -- do not use lightly!
  boolean      quiet   = false;       // suppress diagnostics to stdout?
  
  
  public final void warnmsg(String format, Object ... args) {
    System.err.printf(Locale.US, format, args);
    if (logger!=null)  logger.printf(Locale.US, format, args);
    if (plog && loggerP!=null) loggerP.printf(Locale.US, format, args);
  }
  
  public final void logmsg(String format, Object ... args) {
    if (!quiet)        System.out.printf(Locale.US, format, args);
    if (logger!=null)  logger.printf(Locale.US, format, args);
    if (plog && loggerP!=null) loggerP.printf(Locale.US, format, args);
    
  }
  
  public final void setlogger(PrintStream l) {
    logger = l;
  }
  
  private final void setloggerP(PrintStream l) {
    loggerP = l;
  }
  
  public final void startplog(String fname, boolean startnew) {
    if(!plog) return;
    try {
      setloggerP(new PrintStream(new FileOutputStream(fname,!startnew), true));
      loggerP.printf("%n# %s%n", new Date().toString());
      loggerP.printf("# \"%s\"%n", Strings.join("\" \"",action));
    } catch (FileNotFoundException ex) {
      warnmsg("%s: could not open project log '%s'; continuing...", programname, fname);
    }
  }
  
  public final void startplog(String fname) {
    startplog(fname, false);
  }
  
  public final void stopplog() {
    loggerP.close();
  }
  
  
  public final void terminate(int exitcode) {
    System.exit(exitcode);
  }
  
  public void terminate(String msg) {
    warnmsg("%s%n",msg);
    terminate(1);
  }
  
  /**************************************************************************/
  
  /** read prjfile or terminate
   * @param filename  the complete filename of the .prj file
   * @return the project information in a Properties object
   */
  public final Properties readProject(String filename) {
    Properties prj = new Properties();
    BufferedReader prjfile = null;
    try {
      prjfile = new BufferedReader(new FileReader(filename));
      prj.load(prjfile);
      prjfile.close();
    } catch (Exception ex) {
      warnmsg("%s: could not read project file [%s]%n", cmdname, ex.toString());
      terminate(1);
    }
    return prj;
  }
  
  /* write prj file */
  final void writeProject(Properties prj, String filename)
  throws IOException {
    PrintWriter prjfile = null;
    try {
      prjfile = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
      prj.store(prjfile,null);
      prjfile.close();
    } catch (Exception ex) {
      warnmsg("%s: %s%n", cmdname, ex.toString());
      throw new IOException();
    }
  }
  
  /** read alphabet map file */
  final AlphabetMap readAlphabetMap(String fname) {
    AlphabetMap amap = null;
    try {
      amap = new AlphabetMap().init(fname);
    } catch (IOException ex) {
      warnmsg("%s: could not read alphabet map '%s'. Stop.%n", cmdname, fname);
      terminate(1);
    }
    return amap;
  }
  
// ======================= slurp methods with diagnostics ==================
  
  /** slurp the contents of a file into a byte[] while printing diagnostics.
   * Terminate the program when an error occurs.
   * @param file  the name of the file to be read
   * @return the newly created byte[] with the file's contents
   */
  byte[] slurpByteArray(String file) {
    byte[] a = null;
    logmsg("%s: reading '%s' into memory...%n", cmdname, file);
    try {
      a = new ArrayFile(file).slurp(a);
    } catch (Exception ex) {
      ex.printStackTrace();
      warnmsg("%s: could not read '%s'. Stop.%n",cmdname, file);
      terminate(1);
    }
    return a;
  }
  
  /** slurp selected contents of a file into a byte[] while printing diagnostics.
   * Terminate the program when an error occurs.
   * @param file  the name of the file to be read
   * @param startindex  where to start reading the file (inclusive)
   * @param stopindex   where to stop reading the file (exclusive); -1 reads the whole file.
   * @param a  an existing array or 'null'. If the existing array has length
   * at least stopindex-startindex, its first elements are filled with the contents of 
   * the file.  Otherwise, a new array of the correct size is created.
   * @return the newly created byte[] or a with the file's contents.
   */
  byte[] slurpByteArray(String file, long startindex, long stopindex, byte[] a) {
    //logmsg("%s: reading '%s' [%d..%d] into memory...%n", cmdname, file, startindex, stopindex);
    try {
      a = new ArrayFile(file).slurp(a, startindex, stopindex);
    } catch (Exception ex) {
      ex.printStackTrace();
      warnmsg("%s: could not read '%s'. Stop.%n",cmdname, file);
      terminate(1);
    }
    return a;
  }

  
  /** slurp the contents of a file into an int[] while printing diagnostics.
   * Terminate the program when an error occurs.
   * @param file  the name of the file to be read
   * @return the newly created int[] with the file's contents
   */
  int[] slurpIntArray(String file) {
    int[] a = null;
    logmsg("%s: reading '%s' into memory...%n", cmdname, file);
    try {
      a = new ArrayFile(file).slurp(a);
    } catch (Exception ex) {
      ex.printStackTrace();
      warnmsg("%s: could not read '%s'. Stop.%n",cmdname, file);
      terminate(1);
    }
    return a;
  }

  /** slurp the contents of a file into an int[] while printing diagnostics.
   * Terminate the program when an error occurs.
   * @param file  the name of the file to be read
   * @param a  an existing array to be used if large enough.
   * @return the newly created int[] with the file's contents
   */
  int[] slurpIntArray(String file, int[] a) {
    logmsg("%s: reading '%s' into memory...%n", cmdname, file);
    try {
      a = new ArrayFile(file).slurpIntoPrefix(a);
    } catch (Exception ex) {
      ex.printStackTrace();
      warnmsg("%s: could not read '%s'. Stop.%n",cmdname, file);
      terminate(1);
    }
    return a;
  }
  
  
  /** map the contents of a file into a ByteBuffer while printing diagnostics.
   * Terminate the program when an error occurs.
   * @param file  the name of the file to be read
   * @return the ByteBuffer with the mapped file's contents
   */
  @SuppressWarnings("empty-statement")
  MappedByteBuffer mapRByteArray(String file) {
    MappedByteBuffer b = null;
    logmsg("%s: memory-mapping '%s'...%n", cmdname, file);
    FileChannel fc = null;
    try {
      fc = new FileInputStream(file).getChannel();
      b = fc.map(MapMode.READ_ONLY, 0, fc.size());
      fc.close();
    } catch (IOException ex) {
      try {fc.close();} catch(IOException exx) {};
      warnmsg("%s: could not map '%s'; %s. Stop.%n", cmdname, file, ex.toString());
      terminate(1);
    }
    return b;
  }
  
  /** map the contents of a file into an IntBuffer while printing diagnostics.
   * Terminate the program when an error occurs.
   * @param file  the name of the file to be read
   * @return the IntBuffer with the mapped file's contents
   */
  @SuppressWarnings("empty-statement")
  IntBuffer mapRIntArray(String file) {
    IntBuffer b = null;
    logmsg("%s: memory-mapping '%s'...%n", cmdname, file);
    FileChannel fc = null;
    try {
      fc = new FileInputStream(file).getChannel();
      b = fc.map(MapMode.READ_ONLY, 0, fc.size()).asIntBuffer();
      fc.close();
    } catch (IOException ex) {
      try {if (fc!=null) fc.close();} catch(IOException exx) {};
      warnmsg("%s: could not map '%s'; %s. Stop.%n", cmdname, file, ex.toString());
      terminate(1);
    }
    return b;
  }
  
  //======================= text file readers =================================
  
  @SuppressWarnings("empty-statement")
  ArrayList<String> slurpTextFile(String file, int ll) {
    if (ll<=0) ll=32;
    logmsg("%s: reading '%s'; expecting %d lines...%n", cmdname, file, ll);
    ArrayList<String> lines = new ArrayList<String>(ll);
    String s;
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(file));
      while((s = br.readLine())!=null) lines.add(s);
      br.close();
    } catch (IOException ex) {
      try { if(br!=null) br.close();} catch(IOException exx) {};
      warnmsg("%s: could not read '%s'; %s. Stop.", cmdname, file, ex.toString());
      terminate(1);
    }
    return lines;
  }
  
  //======================= file writers ====================================
  
  /** Dump the first given number of ints of the given array to the given file,
   * while printing diagnostics. Terminate the program when an error occurs.
   * @param file  the name of the file to be written
   * @param a  the array to be dumped
   * @param start  index at which to start dumping a
   * @param len  the number of elements to dump
   */
  void dumpIntArray(final String file, final int[] a, final int start, final int len) {
    logmsg("%s: writing '%s'...%n", cmdname, file);
    try {
      new ArrayFile(file).dump(a,start,len);
    } catch (Exception ex) {
      ex.printStackTrace();
      warnmsg("%s: could not write '%s'. Stop.%n",cmdname, file);
      terminate(1);
    }
  }

  /** Dump the given array to the given file while printing diagnostics. 
   * Terminate the program when an error occurs.
   * @param file  the name of the file to be written
   * @param a  the array to be dumped
   */
  void dumpIntArray(final String file, final int[] a) {
    dumpIntArray(file, a, 0, a.length);
  }
  
  /** Dump the first given number of bytes of the given array to the given file,
   * while printing diagnostics. Terminate the program when an error occurs.
   * @param file  the name of the file to be written
   * @param a  the array to be dumped
   * @param start  index at which to start dumping a
   * @param len  the number of elements to dump
   */
  void dumpByteArray(final String file, final byte[] a, final int start, final int len) {
    logmsg("%s: writing '%s'...%n", cmdname, file);
    try {
      new ArrayFile(file).dump(a,start,len);
    } catch (Exception ex) {
      warnmsg("%s: could not write '%s'. Stop.%n",cmdname, file);
      terminate(1);
    }
  }
  
  /** Dump the given array to the given file while printing diagnostics. 
   * Terminate the program when an error occurs.
   * @param file  the name of the file to be written
   * @param a  the array to be dumped
   */
  void dumpByteArray(final String file, final byte[] a) {
    dumpByteArray(file, a, 0, a.length);
  }
  
  /** Write the given filter BitSet to a file, 
   * starting with the size, followed by the indices of 1-bits.
   * Terminate the program when an error occurs.
   * @param file  the name of the file
   * @param f  the filter
   */
  void writeFilter(final String file, BitSet f) {
    logmsg("%s: writing '%s'...%n", cmdname, file);
    try {
      final ArrayFile of = new ArrayFile(file).openW();
      of.out().writeInt(f.length());
      for (int i = f.nextSetBit(0); i >= 0; i = f.nextSetBit(i+1))
        of.out().writeInt(i);
      of.close();
    } catch (IOException ex) {
      warnmsg("%s: could not write '%s'. Stop.%n",cmdname, file);
      terminate(1);
    }
  }

 /** Read a filter BitSet from a file, 
  * starting with the size, followed by the indices of 1-bits.
  * Terminate the program when an error occurs.
  * @param file  the name of the file
  * @param size  the initizal size of the filter (use -1 for default)
  * @return the filter BitSet
  */
  BitSet readFilter(final String file, final int size) {
    try {
      final ArrayFile inf = new ArrayFile(file).openR();
      inf.close();
    } catch (IOException ex) {
      if (size>0) return new BitSet(size);
      return new BitSet();
    }
   logmsg("%s: reading '%s'...%n", cmdname, file);
   BitSet f = null;
   try {
      final ArrayFile inf = new ArrayFile(file).openR();
      final int numone = (int)(inf.length()/4 - 1);
      final int fsize = inf.in().readInt();
      if (size>0) assert (fsize==size);
      f = new BitSet(fsize);
      for (int i = 0; i<numone; i++) f.set(inf.in().readInt());
      inf.close();
    }  catch (IOException ex) {
      warnmsg("%s: could not read '%s'. Stop. %s%n",cmdname, file, ex.toString());
      terminate(1);
    }
    return f;
  }
 
 /** Read a filter BitSet from a file, 
  * starting with the size, followed by the indices of 1-bits.
  * Terminate the program when an error occurs.
  * @param file  the name of the file
  * @return the filter BitSet
  */
  BitSet readFilter(final String file) {
    return readFilter(file,-1);
  }
  
}
