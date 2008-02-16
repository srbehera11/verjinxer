/*
 * ArrayFile.java
 */
package verjinxer.util;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import static java.nio.channels.FileChannel.*;
import verjinxer.sequenceanalysis.*;

/**
 * This class provides a connection between an array of a primitive type,
 * such as byte[], short[], int[], long[].
 * It provides methods to write/read an array to/from disk efficiently.
 * You should create exactly one ArrayFile per thread.
 * To re-use an ArrayFile for a different array or file, just set a new name.
 *
 * @author Sven Rahmann
 */
public class ArrayFile {

   private String name = null;                   // file name on disk
   private File file = null;                     // Java.io.File for this file
   private FileChannel channel = null;           // channel, null if not open
   private DataOutputStream dataout = null;
   private DataInputStream datain = null;
   private static final int DEFAULT_BUFSIZE = 512 * 1024; // default buffer size is 512K
   private final int bufsize;                     // size of internal buffer, must be divisible by 16
   private ByteBuffer internalBuffer = null;      // internal buffer

   /**
    * create a new instance of ArrayFile with the given filename and buffer size
    * @param filename  the name of this file on disk
    * @param bufsize   size of the internal buffer
    */
   public ArrayFile(String filename, int bufsize) {
      name = filename;
      file = null;
      if (bufsize%16 !=0) throw new IllegalArgumentException("bufsize must be divisible by 16");
      this.bufsize = bufsize;
      internalBuffer = ByteBuffer.allocateDirect(bufsize).order(ByteOrder.nativeOrder());
      // Note: native byte order is not portable between different architectures, but crucial for efficiency.
   }

   /**
    * create a new instance of ArrayFile with the given filename and default buffer size
    * @param filename  the name of this file on disk
    */
   public ArrayFile(String filename) {
      this(filename, DEFAULT_BUFSIZE);
   }

   /**
    * create a new instance of ArrayFile with default buffer size, 
    * without specifying a file name yet (this can be done later with setFilename()).
    */
   public ArrayFile() {
      this(null, DEFAULT_BUFSIZE);
   }


   /**
    * assign a new file name to this ArrayFile.
    * If 'this' is currently open, it is first closed.
    * After assigning the new name, the file is closed.
    * @param filename
    * @return this ArrayFile (for chaining methods)
    * @throws java.io.IOException 
    */
   public ArrayFile setFilename(String filename) throws IOException {
      close();
      name = filename;
      file = null;
      internalBuffer.clear();
      return this;
   }

   /** returns the length of the ArrayFile in bytes
    * @return length of this file in bytes
    */
   public long length() {
      if (file == null)  file = new File(name);
      return (file.length());
   }

   /** returns the length of the ArrayFile in bytes, synonymous to length()
    * @return length of this file in bytes
    */
   public long size() {
      return (length());
   }

   /**
    * closes this ArrayFile
    * @return this ArrayFile (for chaining methods)
    * @throws java.io.IOException 
    */
   public ArrayFile close() throws IOException {
      // close the DataXXXStream before the channel; otherwise it might not be flushed!
      if (dataout!=null) { dataout.close(); }
      dataout=null;
      if (datain!=null) { datain.close(); }
      datain=null;
      if (channel != null) channel.close();
      channel = null;
      return this;
   }

   /** open this ArrayFile for writing via a channel
    * @return this ArrayFile (for method chaining)
    * @throws java.io.IOException 
    */
   public ArrayFile openW() throws IOException {
      if (channel != null) throw new IOException("ArrayFile already open");
      FileOutputStream fos = new FileOutputStream(name);
      channel = fos.getChannel();
      //dataout = new DataOutputStream(new BufferedOutputStream(fos, BUFSIZE));
      return this;
   }

   /** opens a channel to this ArrayFile for reading via a channel
    * @return this ArrayFile (for method chaining)
    * @throws java.io.IOException 
    */
   public ArrayFile openR() throws IOException {
      if (channel != null) throw new IOException("ArrayFile already open");
      FileInputStream fis = new FileInputStream(name);
      channel = fis.getChannel();
      //datain = new DataInputStream(new BufferedInputStream(fis, BUFSIZE));
      return this;
   }

   /** open this ArrayFile for writing via a stream
    * @return this ArrayFile (for method chaining)
    * @throws java.io.IOException  if an error occurs
    */
   public ArrayFile openWStream() throws IOException {
      if (dataout != null || datain != null) throw new IOException("ArrayFile already open");
      FileOutputStream fos = new FileOutputStream(name);
      dataout = new DataOutputStream(new BufferedOutputStream(fos, bufsize));
      return this;
   }

   /** opens a channel to this ArrayFile for reading via a stream
    * @return this ArrayFile (for method chaining)
    * @throws java.io.IOException  if an error occurs
    */
   public ArrayFile openRStream() throws IOException {
      if (dataout != null || datain != null) throw new IOException("ArrayFile already open");
      FileInputStream fis = new FileInputStream(name);
      datain = new DataInputStream(new BufferedInputStream(fis, bufsize));
      return this;
   }

   /**
    * @return  the output stream of this ArrayFile (null if closed)
    */
   public DataOutputStream out() { return dataout; }
   
   /**
    * @return  the input stream of this ArrayFile (null if closed)
    */
   public DataInputStream in()   { return datain;  }
   
   
   /**
    * @return the channel associated to this ArrayFile
    */
   public FileChannel channel() {
      return channel;
   }

   /** creates a MappedByteBuffer for a part of this ArrayFile;
    * the file need not (and probably should not) be open.
    * @param position the position in the file at which the mapping starts
    * @param size  the size of the region to be mapped (in bytes)
    * @return the MappedByteBuffer
    * @throws java.io.IOException  if an error occurs during mapping
    */
   public ByteBuffer mapR(final long position, final long size) throws IOException {
      if (channel!=null || datain!=null || dataout !=null)
         throw new IOException("ArrayFile already open");
      final FileChannel fc = new FileInputStream(name).getChannel();
      final ByteBuffer buf = fc.map(MapMode.READ_ONLY, position, size).order(ByteOrder.nativeOrder());
      fc.close();
      return buf;
   }

   /** creates a MappedByteBuffer for this whole ArrayFile;
    * the file need not (and probably should not) be open.
    * @return the MappedByteBuffer
    * @throws java.io.IOException  if an error occurs during mapping
    */
   public ByteBuffer mapR() throws IOException {
      return mapR(0, length());
   }

   /** creates a MappedByteBuffer for read/write for a part of this ArrayFile;
    * the file need not (and probably should not) be open.
    * @param position the position in the file at which the mapping starts
    * @param size  the size of the region to be mapped (in bytes)
    * @return the MappedByteBuffer
    * @throws java.io.IOException  if an error occurs during mapping
    */
   public ByteBuffer mapRW(final long position, final long size) throws IOException {
      if (channel!=null || datain!=null || dataout !=null)
         throw new IOException("ArrayFile already open");
      final FileChannel fc = new RandomAccessFile(name, "rw").getChannel();
      final ByteBuffer buf = fc.map(MapMode.READ_WRITE, position, size).order(ByteOrder.nativeOrder());
      fc.close();
      return buf;
   }

   /** creates a read/write MappedByteBuffer for this whole ArrayFile;
    * the file need not (and probably should not) be open.
    * @return the MappedByteBuffer
    * @throws java.io.IOException  if an error occurs during mapping
    */
   public ByteBuffer mapRW() throws IOException {
      return mapRW(0, length());
   }
   
   // =========================== write via internal buffer  =============================
   
   static final int BYTES_IN_byte  = 1;
   static final int BYTES_IN_short = 2;
   static final int BYTES_IN_int   = 4;
   static final int BYTES_IN_long  = 8;
   
   /**
    * write a part of a given array to disk via this ArrayFile. 
    * If this ArrayFile is open for writing, write at the current file position.
    * Otherwise, replace the whole file by the given (part of) the array.
    * @param a     the int[] to write
    * @param start position in array 'a' at which to start writing
    * @param len   number of entries to write
    * @return      length of this ArrayFile after writing
    * @throws java.io.IOException  if any I/O error occurs
    */
   public long write(final int[] a, int start, int len) throws IOException {
      final boolean openclose = (channel==null); // file is not open -> write complete file & close
      if (openclose) openW();
      internalBuffer.clear();
      final IntBuffer ib = internalBuffer.asIntBuffer(); // use type compatible to a
      while(len>0) {
         final int itemstowrite = (len<bufsize/BYTES_IN_int)? len : bufsize/BYTES_IN_int; // use type compatible to a
         ib.position(0).limit(itemstowrite);
         ib.put(a, start, itemstowrite); // put part of array into the type-compatible view of the internal buffer
         internalBuffer.position(0).limit(itemstowrite * BYTES_IN_int);
         channel.write(internalBuffer);  // write the internal buffer
         start += itemstowrite; len -= itemstowrite;
      }
      final long p = channel.position();
      if (openclose) close();
      return p;
   }
   
   /**
    * write a whole given array to disk via this ArrayFile. 
    * If this ArrayFile is open for writing, write at the current file position.
    * Otherwise, replace the whole file by the given (part of) the array.
    * @param a     the int[] to write
    * @return      length of this ArrayFile after writing
    * @throws java.io.IOException  if any I/O error occurs
    */
   public long write(final int[] a) throws IOException {
      return write(a,0,a.length);
   }

   
   // TODO: copy the above code for short[] and long[]. For byte[], see below.

   /**
    * write a part of a given array to disk via this ArrayFile. 
    * If this ArrayFile is open for writing, write at the current file position.
    * Otherwise, replace the whole file by the given (part of) the array.
    * @param a     the byte[] to write
    * @param start position in array 'a' at which to start writing
    * @param len   number of entries to write
    * @return      length of this ArrayFile after writing
    * @throws java.io.IOException  if any I/O error occurs
    */
   public long write(final byte[] a, int start, int len) throws IOException {
      final ByteBuffer b = ByteBuffer.wrap(a, start, len);
      return write(b);
   }

  /**
    * write the given ByteBuffer (between position and limit) to disk via this ArrayFile. 
    * If this ArrayFile is open for writing, write at the current file position.
    * Otherwise, replace the whole file by the given (part of) the array.
    * @param b     the ByteBuffer to write
    * @return      length of this ArrayFile after writing
    * @throws java.io.IOException  if any I/O error occurs
    */
   public long write(final ByteBuffer b) throws IOException {
      final boolean openclose = (channel==null); // file is not open -> write complete file & close
      if (openclose) openW();
      channel.write(b);
      final long p = channel.position();
      if (openclose) close();
      return p;
   }

   // ========================= write via stram ================================
   // Note: These functions write in network byte order, not natively. Don't mix!
   
   
   /**
    * write a part of a given array to disk via this ArrayFile. 
    * Note: In contrast to the write() methods, these methods write in Network Byte Order!
    * If this ArrayFile is open for stream writing, write at the current file position.
    * Otherwise, replace the whole file by the given (part of) the array.
    * @param a     the int[] to write
    * @param start position in array 'a' at which to start writing
    * @param len   number of entries to write
    * @return      length of this ArrayFile after writing
    * @throws java.io.IOException  if any I/O error occurs
    */
   public long writeToStream(final int[] a, final int start, final int len) throws IOException {
      final boolean openclose = (dataout==null); // file is not open -> write complete file & close
      if (openclose) openWStream();
      for (int i = start; i < start + len; i++) dataout.writeInt(a[i]);
      if (openclose) close();
      return length();
   }
   
   // TODO: Copy the above code for short[] and long[]. For byte[], see below.

   /**
    * write a part of a given array to disk via this ArrayFile. 
    * If this ArrayFile is open for stream writing, write at the current file position.
    * Otherwise, replace the whole file by the given (part of) the array.
    * @param a     the byte[] to write
    * @param start position in array 'a' at which to start writing
    * @param len   number of entries to write
    * @return      length of this ArrayFile after writing
    * @throws java.io.IOException  if any I/O error occurs
    */
   public long writeToStream(final byte[] a, final int start, final int len) throws IOException {
      final boolean openclose = (dataout==null); // file is not open -> write complete file & close
      if (openclose) openWStream();
      dataout.write(a, start, len);
      if (openclose) close();
      return length();
   }

   
   // ******************************** read methods *******************************
   // native order

   
   /**
    * read a part of a file on disk via this ArrayFile into a part of an array,
    * a[start .. start+len-1].
    * If a is null, a sufficiently large new array is allocated.
    * If the size of the given array is smaller than (start+len), a runtime exception occurs.
    * If this ArrayFile is presently closed, it is opened, and closed when done.
    * We read 'len' items from the given position if the given position is &ge;= 0. 
    * If the given position is negative, read from the current position.
    * If len is negative, we read till the end.
    * @param a     the int[] to read
    * @param start position in array 'a' at which to start reading
    * @param len   number of entries to read. If negative, read the whole (remaining) file.
    * @param fpos  file index at which to start reading
    * @return      the int[]
    * @throws java.io.IOException  if any I/O error occurs
    */
   public int[] read(int[] a, int start, int len, final long fpos) throws IOException {
      final boolean openclose = (channel==null); // file is not open -> open & close file
      if (openclose) openR();
      if (fpos>=0) channel.position(BYTES_IN_int*fpos);  // factor depends on 'a'
      final IntBuffer ib = internalBuffer.asIntBuffer(); // type depends on 'a'
      if (len<0) len = (int)((channel.size()-channel.position())/BYTES_IN_int); // factor depends on 'a'
      if (a==null) a = new int[start+len]; // type depends on 'a'
      while(len>0) {
         final int bytestoread = (len*BYTES_IN_int < bufsize)? len*BYTES_IN_int : bufsize; // factor depends on 'a'
         internalBuffer.position(0).limit(bytestoread);
         final int itemsread = channel.read(internalBuffer) / BYTES_IN_int; // factor depends on 'a'
         ib.position(0).limit(itemsread);
         ib.get(a, start, itemsread);
         start += itemsread; len -= itemsread;
      }
      if (openclose) close();
      return a;
   }

   /**
    * read a file on disk via this ArrayFile into an array a[0 .. end].
    * If a is null, a sufficiently large new array is allocated.
    * If the size of the given array is too small, a runtime exception occurs.
    * If this ArrayFile is presently closed, it is opened, and closed when done.
    * We read from the current position (or the start) of the file to the end.
    * @param a     the int[] to read
    * @return      the int[]
    * @throws java.io.IOException  if any I/O error occurs
    */
   public int[] read(int[] a) throws IOException {
      return read(a,0,-1,-1);
   }
   
   //TODO: copy that code for short[] and long[]. For byte[] see below.
  
   
   /**
    * read a part of a file on disk via this ArrayFile into a part of an array,
    * a[start .. start+len-1].
    * If a is null, a sufficiently large new array is allocated.
    * If the size of the given array is smaller than (start+len), a runtime exception occurs.
    * If this ArrayFile is presently closed, it is opened, and closed when done.
    * We read 'len' items from the given position if the given position is &ge;= 0. 
    * If the given position is negative, read from the current position.
    * If len is negative, we read till the end.
    * @param a     the byte[] to read
    * @param start position in array 'a' at which to start reading
    * @param len   number of entries to read. If negative, read the whole (remaining) file.
    * @param fpos  file index at which to start reading
    * @return      the byte[]
    * @throws java.io.IOException  if any I/O error occurs
    */
   public byte[] read(byte[] a, int start, int len, final long fpos) throws IOException {
      final boolean openclose = (channel==null); // file is not open -> open & close file
      if (openclose) openR();
      if (fpos>=0) channel.position(fpos);
      if (len<0) len = (int)((channel.size()-channel.position()));
      if (a==null) a = new byte[start+len];
      final ByteBuffer b = ByteBuffer.wrap(a, start, len);
      for(int read=0; read<len; read+=channel.read(b)) {}
      if (openclose) close();
      return a;
   }

   /**
    * read a file on disk via this ArrayFile into an array a[0 .. end].
    * If a is null, a sufficiently large new array is allocated.
    * If the size of the given array is too small, a runtime exception occurs.
    * If this ArrayFile is presently closed, it is opened, and closed when done.
    * We read from the current position (or the start) of the file to the end.
    * @param a     the byte[] to read
    * @return      the byte[]
    * @throws java.io.IOException  if any I/O error occurs
    */
   public byte[] read(byte[] a) throws IOException {
      return read(a,0,-1,-1);
   }
   
   /**
    * read a file on disk via this ArrayFile into a newly allocated
    * ByteBuffer that exactly fits the size of the file,
    * or the remainder of the file, if the file is already open.
    * @return  an array-backed ByteBuffer containing the file contents
    * @throws java.io.IOException
    */
   public ByteBuffer readIntoNewBuffer() throws IOException {
      final boolean openclose = (channel==null); // file is not open -> open & close file
      if (openclose) openR();
      final int len = (int)((channel.size()-channel.position()));
      final ByteBuffer b = ByteBuffer.allocate(len);
      for(int read=0; read<len; read+=channel.read(b)) {}
      if (openclose) close();
      return b;
   }

   
   // ============ memory mapped read()s =======================================
   
   /**
    * read a part of a file on disk via this ArrayFile into a part of an array,
    * a[start .. start+len-1], using memory mapping.
    * If a is null, a sufficiently large new array is allocated.
    * If the size of the given array is smaller than (start+len), a runtime exception occurs.
    * We read 'len' items from the given position 'fpos'.
    * If 'len' is negative, we read till the end of the file.
    * @param a     the int[] to read
    * @param start position in array 'a' at which to start reading
    * @param len   number of entries to read. If negative, read the whole (remaining) file.
    * @param fpos  file index (in int's) at which to start reading
    * @return      the int[]
    * @throws java.io.IOException  if any I/O error occurs
    */
   public int[] readMapped(int[] a, int start, int len, final long fpos) throws IOException {
      final long size = this.length();
      if (size % BYTES_IN_int != 0) throw new IOException("Length of '" + this.name + "' does not align");
      final long items = size/BYTES_IN_int;   // items in file
      if (len<0) len = (int) (items - fpos);  // items to read
      IntBuffer ib = this.mapR(fpos*BYTES_IN_int, len*BYTES_IN_int).asIntBuffer();
      if (a==null) a = new int[start+len];    // type depends on 'a'
      for (int i = start; i < start+len; i++) a[i] = ib.get();
      return a;
   }

   /**
    * read a file on disk via this ArrayFile into an array a[0 .. end].
    * If a is null, a sufficiently large new array is allocated.
    * If the size of the given array is too small, a runtime exception occurs.
    * @param a     the int[] to read
    * @return      length of this ArrayFile after writing
    * @throws java.io.IOException  if any I/O error occurs
    */
   public int[] readMapped(int[] a) throws IOException {
      return readMapped(a,0,-1,0);
   }

   
   /**
    * read a part of a file on disk via this ArrayFile into a part of an array,
    * a[start .. start+len-1], using memory mapping.
    * If a is null, a sufficiently large new array is allocated.
    * If the size of the given array is smaller than (start+len), a runtime exception occurs.
    * We read 'len' items from the given position 'fpos'.
    * If 'len' is negative, we read till the end of the file.
    * @param a     the byte[] to read into
    * @param start position in array 'a' at which to start reading
    * @param len   number of entries to read. If negative, read the whole (remaining) file.
    * @param fpos  file index (in int's) at which to start reading
    * @return      the byte[]
    * @throws java.io.IOException  if any I/O error occurs
    */
   public byte[] readMapped(byte[] a, int start, int len, final long fpos) throws IOException {
      final long items = this.length();       // items in file
      if (len<0) len = (int) (items - fpos);  // items to read
      ByteBuffer ib = this.mapR(fpos,len);
      if (a==null) a = new byte[start+len]; // type depends on 'a'
      for (int i = start; i < start+len; i++) a[i] = ib.get();
      return a;
   }

   /**
    * read a file on disk via this ArrayFile into an array a[0 .. end].
    * If a is null, a sufficiently large new array is allocated.
    * If the size of the given array is too small, a runtime exception occurs.
    * @param a     the int[] to read
    * @return      length of this ArrayFile after writing
    * @throws java.io.IOException  if any I/O error occurs
    */
   public byte[] readMapped(byte[] a) throws IOException {
      return readMapped(a,0,-1,0);
   }

   
   //=============================================================================================
              

   /** counts the number of occurrences of each byte (0..255) in the file.
    * @param counts array where to store the counts. 
    *   If its size is different from 256, a new array is allocated. 
    * @return counts or a new long[256] containing the counts
    * @throws java.io.IOException 
    */
   public long[] byteCounts(long[] counts) throws IOException {
      if (counts == null || counts.length != 256) counts = new long[256];
      ByteBuffer buf = mapR();
      final long ll = buf.capacity();
      //FileChannel fc = new FileInputStream(name).getChannel();
      //long length = fc.size();
      //MappedByteBuffer buf = fc.map(MapMode.READ_ONLY, 0, length);
      //fc.close();
      for (long i = 0; i < ll; i++) {
         final byte b = buf.get();
         if (b < 0)  counts[b + 256]++;
         else        counts[b]++;
      }
      buf = null;
      return counts;
   }

   /** counts the number of occurrences of each byte (0..255) in the file.
    * @return a new long[256] array containing the counts
    * @throws java.io.IOException 
    */
   public long[] byteCounts() throws IOException {
      return byteCounts(null);
   }

   /** prints the given count array to the specified PrintStream.
    * This is a convenience method for testing/debugging.
    * @param out the stream to print to
    * @param counts the array containing counts
    */
   public static void showCounts(PrintStream out, long[] counts) {
      for (int i = 0; i < counts.length; i++)
         if (counts[i] > 0) {
            final char ch = (char) i;
            out.printf("%3d %c %d", i, Character.isISOControl(ch) ? '?' : ch, counts[i]);
            out.println();
         }
   }

}
