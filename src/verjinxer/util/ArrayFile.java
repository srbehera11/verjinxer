/*
 * ArrayFile.java
 */
package verjinxer.util;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import static java.nio.channels.FileChannel.*;

/**
 * This class provides a connection between an array of a primitive type,
 * such as byte[], short[], int[], long[],
 * and files on disk storing such arrays in <b>native</b> byte order.
 * It provides methods to write/read an array to/from disk efficiently.
 * 
 * You should create only as many ArrayFiles per thread as need to
 * be open concurrently. The reason is that each ArrayFile allocates
 * a buffer for temporary storage (and thus consumes memory).
 * The buffer size can be passed to the constructor, but not modified
 * subsequently.
 * 
 * To re-use an ArrayFile for a different array or file, just set a new name
 * with the <code>setFilename()</code> method.
 *
 * @author Sven Rahmann
 */
public class ArrayFile {
   
   private String name = null;                   // file name on disk
   private File file = null;                     // Java.io.File for this file
   private FileChannel channel = null;           // channel, null if not open
   private Mode mode = null;   
   private final int bufsize;                    // size of internal buffer, must be divisible by 16
   private final ByteBuffer internalBuffer;      // internal buffer

   private static final int BUFBLOCKS    = 1023;     // default buffer size is this blocks
   private static final int BUFBLOCKSIZE = 1024;     // each block has this size in byes
   private static final int BUFCYCLES    = 16;       // number of buffers with different sizes
   private static int numbuf = 0;                    // counter: current number of buffers;
                                                     // buffer #i gets (BUFBLOCKS - i % BUFCYCLES) blocks,
                                                     // so each buffer has a slightly different size
  
   private enum Mode { READ, WRITE };
   
   /**
    * Creates a new instance of ArrayFile with the given filename and buffer size.
    * The buffer size must be divisible by 16. If it is not, it is rounded up
    * to be divisible by 16. The buffer is allocated as a direct buffer.
    * @param filename  the name of this file on disk;
    *   it can be null and specified/changed later by <code>setFilename()</code>
    * @param bufsize   size of the internal buffer of this ArrayFile in bytes.
    *   If bufsize==0, no buffer object is created (not even one of size zero)!
    *   This is useful if and only if this ArrayFile will only be used with memory mapping.
    *   Attempting anything else will then throw a NullPointerException.
    */
   public ArrayFile(final String filename, int bufsize) {
      bufsize += (16 - (bufsize%16)) %16; // make buffer size divisible by 16 bytes
      this.bufsize = bufsize;
      internalBuffer = bufsize>0? ByteBuffer.allocateDirect(bufsize).order(ByteOrder.nativeOrder()) : null;
      // Note 1: A direct buffer should be faster when writing to disk (see ByteBuffer javadoc)
      // Note 2: native byte order is not portable between different architectures, but crucial for efficiency.
      setFilename(filename);
   }

   /**
    * Creates a new instance of ArrayFile with the given filename and default buffer size
    * @param filename  the name of this file on disk;
    *   it can be null and specified/changed later by <code>setFilename()</code>
    */
   public ArrayFile(final String filename) {
      this(filename, (BUFBLOCKS - numbuf++ % BUFCYCLES) * BUFBLOCKSIZE);
   }

   /**
    * creates a new instance of ArrayFile with default buffer size, 
    * without specifying a file name yet (this can be done later with setFilename()).
    */
   public ArrayFile() {
      this((String)null);
   }

   /**
    * Create a new instance of ArrayFile with the given buffer size.
    * The buffer size (in bytes) must be divisible by 16. 
    * If it is not, it is rounded up to be divisible by 16.
    * No file name is associated yet; this must be done later through
    * the <code>setFilename()</code> method.
    * @param bufsize   size of the internal buffer in bytes
    *   If bufsize==0, no buffer object is created (not even one of size zero)!
    *   This is useful if and only if this ArrayFile will only be used with memory mapping.
    *   Attempting anything else will then throw a NullPointerException.
    */
   public ArrayFile(int bufsize) {
      this((String)null, bufsize);
   }

   
   /**
    * Assigns a (new) file name to this ArrayFile.
    * If this ArrayFile is currently open, it is first closed.
    * After assigning the new name, the file is in closed state.
    * @param filename  the name of the file to associate
    * @return this ArrayFile (for chaining methods)
    */
   public ArrayFile setFilename(String filename) {
      try { 
         close();
      } catch (IOException ex) {
         throw new RuntimeException("open ArrayFile could not be closed", ex);
      }
      name = filename;
      if (internalBuffer!=null) internalBuffer.clear();
      file = (name!=null)? new File(name) : (File)null;
      return this;
   }

   /** Returns the length of the ArrayFile in bytes
    * @return length of this file in bytes
    */
   public long length() {
      return (file.length());
   }

   /** Returns the length of the ArrayFile in bytes, synonymous to length()
    * @return length of this file in bytes
    */
   public long size() {
      return (length());
   }
   
   public String getMode() {
      return (mode==null)? "CLOSED" : mode.toString();
   }

   /**
    * Closes this ArrayFile
    * @return this ArrayFile (for chaining methods)
    * @throws java.io.IOException 
    */
   public ArrayFile close() throws IOException {
      // Note: close any streams written to, before closing the channel;
      // otherwise the streams might not be flushed!
      flush();
      if (internalBuffer!=null) internalBuffer.clear();
      if (channel != null) { channel.force(true); channel.close(); }
      channel = null;
      mode = null;
      return this;
   }
   
   /** Writes the contents of the internal buffer to a file,
    * (if the file is open in write mode).
    * @return this ArrayFile (for method chaining)
    * @throws java.io.IOException 
    */
   public ArrayFile flush() throws IOException {
      if (channel==null || mode!=Mode.WRITE) return this;
      internalBuffer.flip();
      int bytestowrite = internalBuffer.limit();
      while(bytestowrite>0) bytestowrite -= channel.write(internalBuffer);
      //channel.force(false);
      internalBuffer.clear();
      return this;
   }

   /** Opens this ArrayFile for writing 
    * @return this ArrayFile (for method chaining)
    * @throws java.io.IOException 
    */
   public ArrayFile openW() throws IOException {
      if (channel!=null) // || datain!=null || dataout !=null)
         throw new IOException("ArrayFile already open");
      channel = new FileOutputStream(name).getChannel();
      internalBuffer.clear();
      mode=Mode.WRITE;
      return this;
   }

   /** Opens this ArrayFile for reading
    * @return this ArrayFile (for method chaining)
    * @throws java.io.IOException 
    */
   public ArrayFile openR() throws IOException {
      if (channel!=null) // || datain!=null || dataout !=null)
         throw new IOException("ArrayFile already open");
      channel = new FileInputStream(name).getChannel();
      mode=Mode.READ;
      return this;
   }
   
   /** Opens this ArrayFile for reading and writing
    * @return this ArrayFile (for method chaining)
    * @throws java.io.IOException 
    */
   public ArrayFile openRW() throws IOException {
      if (channel!=null) // || datain!=null || dataout !=null)
         throw new IOException("ArrayFile already open");
      channel = new RandomAccessFile(name,"rw").getChannel();
      mode=null;
      return this;
   }

   
   /**
    * @return the channel associated to this ArrayFile (null if closed)
    */
   public FileChannel channel() {
      return channel;
   }

   /** Creates a MappedByteBuffer for a part of this ArrayFile.
    * The file may already be open for reading and writing, 
    * but this is not recommended (results unspecified).
    * @param position the position in the file at which the mapping starts
    * @param size  the size of the region to be mapped (in bytes)
    * @return the MappedByteBuffer
    * @throws java.io.IOException  if an error occurs during mapping
    */
   public ByteBuffer mapR(final long position, final long size) throws IOException {
      final FileChannel fc = (channel!=null)? channel : new FileInputStream(name).getChannel();
      final ByteBuffer buf = fc.map(MapMode.READ_ONLY, position, size).order(ByteOrder.nativeOrder());
      fc.close();
      return buf;
   }

   /** Creates a MappedByteBuffer for this whole ArrayFile;
    * the file need not (and probably should not) be open.
    * @return the MappedByteBuffer
    * @throws java.io.IOException  if an error occurs during mapping
    */
   public ByteBuffer mapR() throws IOException {
      return mapR(0, length());
   }

   /** Creates a MappedByteBuffer for reading/writing a part of this ArrayFile;
    * the file may already be open, but this is not recommended (results unspecified).
    * @param position the position in the file at which the mapping starts
    * @param size  the size of the region to be mapped (in bytes)
    * @return the MappedByteBuffer
    * @throws java.io.IOException  if an error occurs during mapping
    */
   public ByteBuffer mapRW(final long position, final long size) throws IOException {
      flush();
      final FileChannel fc = (channel!=null)? channel : new RandomAccessFile(name, "rw").getChannel();
      final ByteBuffer buf = fc.map(MapMode.READ_WRITE, position, size).order(ByteOrder.nativeOrder());
      fc.close();
      return buf;
   }

   /** Creates a read/write MappedByteBuffer for this whole ArrayFile;
    * the file need not (and probably should not) be open.
    * @return the MappedByteBuffer
    * @throws java.io.IOException  if an error occurs during mapping
    */
   public ByteBuffer mapRW() throws IOException {
      return mapRW(0, length());
   }
   
   // =========================== write via internal buffer  =============================
   
   
   /**
    * Writes a part of a given array to disk via this ArrayFile. 
    * If this ArrayFile is open for writing, write at the current file position.
    * Otherwise, replace the whole file by the given (part of) the array.
    * @param a      the int[] to write
    * @param start  position in array 'a' at which to start writing
    * @param items  number of array entries to write
    * @return       length of this ArrayFile after writing
    * @throws java.io.IOException  if any I/O error occurs
    */
   public long writeArray(final int[] a, int start, int items) throws IOException {
      final int factor = Integer.SIZE/8; // !!! factor depends on 'a'
      final boolean openclose = (channel==null);
      if (openclose) openW(); else flush(); // clear buffer!
      if (mode!=Mode.WRITE) throw new IOException("non-writable ArrayFile "+mode.toString());
      // internal buffer is now clear to write
      final int bufitems = bufsize / factor;
      final IntBuffer ib = internalBuffer.asIntBuffer(); // !!! type depends on 'a'
      while(items>0) {
         final int itemstowrite = (items<bufitems)? items : bufitems;
         ib.clear();
         ib.put(a, start, itemstowrite);
         int bytestowrite = itemstowrite * factor;         
         internalBuffer.position(0).limit(bytestowrite); 
         while (bytestowrite>0) bytestowrite -= channel.write(internalBuffer);
         assert(bytestowrite==0);
         start += itemstowrite; items -= itemstowrite;
      }
      internalBuffer.clear();
      final long p = channel.position();
      if (openclose) close();
      return p;
   }
   
   /**
    * Writes a whole given array to disk via this ArrayFile. 
    * If this ArrayFile is open for writing, write at the current file position.
    * Otherwise, replace the whole file by the given (part of) the array.
    * @param a     the int[] to write
    * @return      length of this ArrayFile after writing
    * @throws java.io.IOException  if any I/O error occurs
    */
   public long writeArray(final int[] a) throws IOException {
      return writeArray(a,0,a.length);
   }

   
   // TODO: copy the above code for short[] and long[]. For byte[], see below.

   /** (BYTE array:) 
    * write a part of a given array to disk via this ArrayFile. 
    * If this ArrayFile is open for writing, write at the current file position.
    * Otherwise, replace the whole file by the given (part of) the array.
    * @param a     the byte[] to write
    * @param start position in array 'a' at which to start writing
    * @param len   number of entries to write
    * @return      length of this ArrayFile after writing
    * @throws java.io.IOException  if any I/O error occurs
    */
   public long writeArray(final byte[] a, int start, int len) throws IOException {
      // bypass internal buffer for byte arrays by wrapping!
      final ByteBuffer b = ByteBuffer.wrap(a, start, len).order(ByteOrder.nativeOrder());
      return writeBuffer(b);
   }
   
   /**
    * Writes a whole byte array to disk via this array file.
    * If this ArrayFile is open for writing, write at the current file position.
    * Otherwise, replace the whole file by the given (part of) the array.
    * @param a
    * @return length of this ArrayFile after writing
    * @throws java.io.IOException if any I/O error occurs
    */
   public long writeArray(final byte[] a) throws IOException {
      return writeArray(a, 0, a.length);
   }

  /**
    * Writes the given ByteBuffer (between position and limit) to disk via this ArrayFile. 
    * If this ArrayFile is open for writing, write at the current file position.
    * Otherwise, replace the whole file by the given (part of) the array.
    * @param b     the ByteBuffer to write
    * @return      length of this ArrayFile after writing
    * @throws java.io.IOException  if any I/O error occurs
    */
   public long writeBuffer(final ByteBuffer b) throws IOException {
      // for ByteBuffers, the Byte order does not matter!
      final boolean openclose = (channel==null); // file is not open -> write complete file & close
      if (openclose) openW();
      channel.write(b);
      final long p = channel.position();
      if (openclose) close();
      return p;
   }
   
   public final ArrayFile writeLong(final long x) throws IOException {
      if (internalBuffer.position() >= bufsize-8) flush();
      internalBuffer.putLong(x);          
      return this;
   }

   public final ArrayFile writeInt(final int x) throws IOException {
      if (internalBuffer.position() >= bufsize-4) flush();
      internalBuffer.putInt(x);          
      return this;
   }
   
   public final ArrayFile writeShort(final short x) throws IOException {
      if (internalBuffer.position() >= bufsize-2) flush();
      internalBuffer.putShort(x);          
      return this;
   }
   
   public final ArrayFile writeByte(final byte x) throws IOException {
      if (internalBuffer.position() >= bufsize-1) flush();
      internalBuffer.put(x);          
      return this;
   }
   
   
   // ******************************** read methods *******************************

   
   /**
    * Reads a part of a file on disk via this ArrayFile into a part of an array,
    * a[start .. start+len-1].
    * If a is null, a sufficiently large new array is allocated.
    * If the size of the given array is smaller than (start+len), a runtime exception occurs.
    * If this ArrayFile is presently closed, it is opened, and closed when done.
    * We read 'len' items from the given position if the given position is &ge;= 0. 
    * If the given position is negative, read from the current position.
    * If len is negative, we read till the end.
    * @param a     the int[] to read
    * @param start position in array 'a' at which to start reading
    * @param nItems   number of entries to read. If negative, read the whole (remaining) file.
    * @param fpos  file index at which to start reading
    * @return      the int[]
    * @throws java.io.IOException  if any I/O error occurs
    */
   public int[] readArray(int[] a, int start, int nItems, final long fpos) throws IOException {
      final int factor = Integer.SIZE / 8; // depends on 'a'
      final boolean openclose = (channel==null);
      if (openclose) openR();
      if (mode!=Mode.READ) throw new IOException("ArrayFile not open for reading");
      if (fpos>=0) channel.position(factor*fpos);
      final IntBuffer ib = internalBuffer.asIntBuffer(); // type depends on 'a'
      if (nItems<0) nItems = (int)((channel.size()-channel.position())/factor);
      if (a==null) a = new int[start+nItems]; // type depends on 'a'
      while(nItems>0) {
         int bytestoread = (nItems*factor < bufsize)? nItems*factor : bufsize;
         final int itemstoread = bytestoread / factor;
         internalBuffer.position(0).limit(bytestoread);
         while ((bytestoread -= channel.read(internalBuffer))>0) {}
         ib.position(0).limit(itemstoread);
         ib.get(a, start, itemstoread);
         start += itemstoread; nItems -= itemstoread;
      }
      if (openclose) close();
      return a;
   }

   /**
    * Reads a file on disk via this ArrayFile into an array a[0 .. end].
    * If a is null, a sufficiently large new array is allocated.
    * If the size of the given array is too small, a runtime exception occurs.
    * If this ArrayFile is presently closed, it is opened, and closed when done.
    * We read from the current position (or the start) of the file to the end.
    * @param a     the int[] to read
    * @return      the int[]
    * @throws java.io.IOException  if any I/O error occurs
    */
   public int[] readArray(int[] a) throws IOException {
      return readArray(a,0,-1,-1);
   }
   
   //TODO: copy that code for short[] and long[]. For byte[] see below.
  
   
   /**
    * Reads a part of a file on disk via this ArrayFile into a part of an array,
    * a[start .. start+len-1].
    * If a is null, a sufficiently large new array is allocated.
    * If the size of the given array is smaller than (start+len), a runtime exception occurs.
    * If this ArrayFile is presently closed, it is opened, and closed when done.
    * We read 'len' items from the given file position 'fpos' if it is &ge;= 0. 
    * If the given file position is negative, we read from the current file position.
    * We read 'len' bytes.  If 'len' is negative, we read till the end.
    * @param a     the byte[] to read
    * @param start position in array 'a' at which to start reading
    * @param len   number of entries to read. If negative, read the whole (remaining) file.
    * @param fpos  file index at which to start reading
    * @return      the (given or newly allocated) byte[]
    * @throws java.io.IOException  if any I/O error occurs
    */
   public byte[] readArray(byte[] a, int start, int len, final long fpos) throws IOException {
      final boolean openclose = (channel==null); // file is not open -> open & close file
      if (openclose) openR();
      if (fpos>=0) channel.position(fpos);
      if (len<0) len = (int)((channel.size()-channel.position()));
      if (a==null) a = new byte[start+len];
      final ByteBuffer b = ByteBuffer.wrap(a, start, len).order(ByteOrder.nativeOrder());
      for(int read=0; read<len; read+=channel.read(b)) {}
      if (openclose) close();
      return a;
   }

   /**
    * Reads a file on disk via this ArrayFile into an array a[0 .. end].
    * If a is null, a sufficiently large new array is allocated.
    * If the size of the given array is too small, a runtime exception occurs.
    * If this ArrayFile is presently closed, it is opened, and closed when done.
    * We read from the current position (or the start) of the file to the end.
    * @param a     the byte[] to read
    * @return      the byte[]
    * @throws java.io.IOException  if any I/O error occurs
    */
   public byte[] readArray(byte[] a) throws IOException {
      return readArray(a,0,-1,-1);
   }
   
   /**
    * Reads a file on disk via this ArrayFile into a newly allocated
    * ByteBuffer that exactly fits the size of the file,
    * or the remainder of the file, if the file is already open.
    * @return  an array-backed ByteBuffer containing the file contents
    * @throws java.io.IOException
    */
   public ByteBuffer readArrayIntoNewBuffer() throws IOException {
      final boolean openclose = (channel==null); // file is not open -> open & close file
      if (openclose) openR();
      final int len = (int)((channel.size()-channel.position()));
      final ByteBuffer b = ByteBuffer.allocate(len).order(ByteOrder.nativeOrder());
      for(int read=0; read<len; read+=channel.read(b)) {}
      if (openclose) close();
      b.limit(b.capacity()).position(0);
      return b;
   }

   
   // ============ memory mapped read()s =======================================
   
   /**
    * Reads a part of a file on disk via this ArrayFile into a part of an array,
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
      final int factor = Integer.SIZE / 8;  // factor depends on type of 'a'
      final long size = this.length();
      if (size % factor != 0) throw new IOException("Length of '" + this.name + "' does not align");
      final long items = size/factor;   // items in file
      if (len<0) len = (int) (items - fpos);  // items to read
      final IntBuffer ib = this.mapR(fpos*factor, len*factor).order(ByteOrder.nativeOrder()).asIntBuffer();
      if (a==null) a = new int[start+len];    // type depends on 'a'
      ib.get(a, start, len); // was: for (int i = start; i < start+len; i++) a[i] = ib.get();
      return a;
   }

   /**
    * Reads a file on disk via this ArrayFile into an array a[0 .. end].
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
    * Reads a part of a file on disk via this ArrayFile into a part of an array,
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
      flush();
      final long items = this.length();       // items in file
      if (len<0) len = (int) (items - fpos);  // items to read
      final ByteBuffer ib = this.mapR(fpos,len).order(ByteOrder.nativeOrder());
      if (a==null) a = new byte[start+len]; // type depends on 'a'
      ib.get(a, start, len); // was:  for (int i = start; i < start+len; i++) a[i] = ib.get();
      return a;
   }

   /**
    * Reads a file on disk via this ArrayFile into an array a[0 .. end].
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
              

   /**
    * Counts the number of occurrences of each byte (0..255) in the file.
    * @param counts array where to store the counts. 
    *   If its size is different from 256, a new array is allocated. 
    * @return counts or a new long[256] containing the counts
    * @throws java.io.IOException 
    */
   public long[] byteCounts(long[] counts) throws IOException {
      if (counts == null || counts.length != 256) counts = new long[256];
      final ByteBuffer buf = mapR();
      final long ll = buf.capacity();
      for (long i = 0; i < ll; i++) {
         final byte b = buf.get();
         if (b < 0)  counts[b + 256]++;
         else        counts[b]++;
      }
      return counts;
   }

   /**
    * Counts the number of occurrences of each byte (0..255) in the file.
    * @return a new long[256] array containing the counts
    * @throws java.io.IOException 
    */
   public long[] byteCounts() throws IOException {
      return byteCounts(null);
   }

   /**
    * Prints the given count array to the specified PrintStream.
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

} // end class
//==========================================================================
