/*
 * ArrayFile.java
 *
 * Created on December 12, 2006, 3:42 AM
 *
 */

package verjinxer.util;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import static java.nio.channels.FileChannel.*;
import verjinxer.sequenceanalysis.*;

/**
 *
 * @author Sven Rahmann
 */
public class ArrayFile
{
  
  private final int BUFSIZE = 512*1024; 
  
   /** filename of the array file */
   String name = null;
   /** channel of the array file, null if not open */
   private FileChannel channel = null;
   private DataOutputStream dataout = null;
   private DataInputStream datain = null;
   
   /**
    * Creates a new instance of ArrayFile
    */
   public ArrayFile(String filename)
   {
      name = filename;
   }
   
   /** returns the length of the ArrayFile */
   public long length()
   {
      return(new File(name).length());
   }
   
   /** returns the length of the ArrayFile, synonymous to length() */
   public long size()
   {
      return(length());
   }
   
   /** opens a channel to this ArrayFile for continued writing */
   public ArrayFile openW() throws IOException {
     if (channel!=null || dataout!=null || datain !=null)
       throw new IOException("ArrayFile already open");
     FileOutputStream fos = new FileOutputStream(name);
     channel = fos.getChannel();
     dataout = new DataOutputStream(new BufferedOutputStream(fos, BUFSIZE));
     return this;
   }

   /** opens a channel to this ArrayFile for continued reading */
   public ArrayFile openR() throws IOException {
     if (channel!=null || dataout!=null || datain !=null)
       throw new IOException("ArrayFile already open");
     FileInputStream fis = new FileInputStream(name);
     channel = fis.getChannel();
     datain = new DataInputStream(new BufferedInputStream(fis, BUFSIZE));
     return this;
   }
   
   /* closes the channel to this ArrayFile */
   public ArrayFile close() throws IOException {
     // it is imparative to close the DataXXXStream before the channel;
     // otherwise it might not be flushed!
     if (dataout!=null) { dataout.close(); }
     dataout=null;
     if (datain!=null) { datain.close(); }
     datain=null;
     if (channel!=null) { channel.close(); }
     channel=null;
     return this;
   }
   
   public DataOutputStream out() {
     return dataout;
   }
   
   public DataInputStream in() {
     return datain;
   }
   
   public FileChannel channel() {
      return channel;
   }

   /** returns a MappedByteBuffer for this array file,
   * or throws an IOException if this is not possible 
   */
  public MappedByteBuffer mapR() throws IOException {
    FileChannel fc = new FileInputStream(name).getChannel();
    long length = fc.size();
    MappedByteBuffer buf = fc.map(MapMode.READ_ONLY, 0, length);
    fc.close();
    return buf;
  }

  /** returns a MappedByteBuffer for a part of this array file,
   * or throws an IOException if this is not possible 
   */
  public MappedByteBuffer mapR(final long position, final long size) throws IOException {
    FileChannel fc = new FileInputStream(name).getChannel();
    MappedByteBuffer buf = fc.map(MapMode.READ_ONLY, position, size);
    fc.close();
    return buf;
  }

   
   //------------------------------------------------------------------------

   /** dumps a byte[] to this ArrayFile. 
    * @param array the array
    * @param append true to append to the end of an existing file
    */
   //public long put(byte[] array, boolean append) throws IOException
   //{
    //  return put(array, array.length, append);
   //}
   
   /** dumps a byte[] to this ArrayFile. 
    * @param array the array
    * @param len only write the first len entries of the array
    * @param append true to append to the end of an existing file
    */
  /*
   public long put(byte[] array, int len, boolean append) throws IOException
   {
      RandomAccessFile raf = new RandomAccessFile(name,"rw");
      FileChannel fc = raf.getChannel();
      if(!append) fc.truncate(array.length);
      long start = (append? fc.size() : 0);
      MappedByteBuffer buf = fc.map(MapMode.READ_WRITE, start, array.length);
      buf.put(array,0, len);
      //buf.force(); fc.force(true);
      long tlen = fc.size();
      fc.close(); raf.close();
      return tlen;
   }
 */
   /** dumps a byte[] to this ArrayFile, overwriting it */
   //public long write(byte[] array)  throws IOException 
   //{ return put(array,false); }
   
   /** dumps a byte[] to this ArrayFile, appending to it */
   //public long append(byte[] array) throws IOException 
   //{ return put(array,true);  }

   
   /** dumps an int[] to this ArrayFile.
    * @param array the array
    * @param append set this to true to append to the end of the file
    */
   //public long put(int[] array, boolean append) throws IOException
   //{
   //   return put(array,array.length,append);
  // }
   
   /** dumps an int[] to this ArrayFile.
    * @param array  the array
    * @param len  only write the first len entries of the array
    * @param append  set this to true to append to the end of the file
    */
  /*
   public long put(int[] array, int len, boolean append) throws IOException
   {
      RandomAccessFile raf = new RandomAccessFile(name,"rw");
      FileChannel fc = raf.getChannel();
      if(!append) fc.truncate(4*len);
      long start = (append? fc.size() : 0);
      IntBuffer buf = fc.map(MapMode.READ_WRITE, start, 4*array.length).asIntBuffer();
      buf.put(array, 0, len);
      //fc.force(true);
      long tlen = fc.size();
      fc.close(); raf.close();
      return tlen;
   }
 */
   
   //public long putslice(int[] array, long start, int len) throws IOException
   //{
   //   return putslice_stream(array, start, len);
   //}
 
   /*
   public long putslice_map(int[] array, long start, int len) throws IOException
   {
      RandomAccessFile raf = new RandomAccessFile(name,"rw");
      FileChannel fc = raf.getChannel();
      
      boolean mapped = false; 
      int mapcount=0;
      MappedByteBuffer mbuf = null;
      while (!mapped && mapcount<3) {
         try { 
            mbuf = fc.map(MapMode.READ_WRITE, start, 4*len);  // sizeof(int)==4
            mapped = true;
         } catch (IOException ex) {
            System.gc();
            mapcount++;
         }
      }
      if (!mapped) throw new IOException("could not map "+name);
      
      IntBuffer buf = mbuf.asIntBuffer();
      buf.put(array, 0, len);
      mbuf.force();
      long tlen = fc.size();
      fc.close(); raf.close();
      buf=null; mbuf=null;
      return tlen;
   }

   
    public long putslice_raf(int[] array, long start, int len) throws IOException
   {
      RandomAccessFile raf = new RandomAccessFile(name,"rw");
      long newlen = start+4*len;
      if (raf.length()<newlen) raf.setLength(newlen);
      raf.seek(start);
      try {
         for (int i=0; i<len; i++) {
            raf.writeInt(array[i]);
            //if (i%1000000==0) System.err.printf("  ...%d%n",i);
         }
      } catch (IOException ex) {
         ex.printStackTrace(System.err);
      }
      long tlen = raf.length();
      raf.close();
      return tlen;
   }
    
   public long putslice_stream(int[] array, long start, int len) throws IOException
   {
      DataOutputStream out = new DataOutputStream(
          new BufferedOutputStream(new FileOutputStream(name, start!=0), BUFSIZE));
      for (int i=0; i<len; i++) out.writeInt(array[i]);
      out.close();
      return length();
   }
   
   
   public long putslice(ByteBuffer array, long start, int len) throws IOException
   {
     FileChannel fc = new FileOutputStream(name).getChannel();
     fc.position(start);
     fc.write(array);
     fc.truncate(start+len);
     fc.close();
     return start+len;
   }

    */
   
   /** write a ByteBuffer to this file's channel (which must be open)
    * @param b  the ByteBuffer
    * @return  the position in the channel (current file size in bytes)
    */
   public long dumpToChannel(ByteBuffer b) throws IOException {
     if (channel==null) throw new IOException("Channel not open");
     channel.write(b);
     return channel.position();
   }

   /** write an int[] to this file's channel (which must be open)
    * @param a  the array
    * @return  current file position (current file size in bytes)
    */
   public long dumpToChannel(int[] a, int start, int len) throws IOException {
     if (channel==null) throw new IOException("Channel not open");
     for (int i=start; i<start+len; i++) dataout.writeInt(a[i]);  
     return channel.position();
   }
   
   /** write a byte[] to this file's channel (which must be open)
    * @param a  the array
    * @return  current file position (current file size in bytes)
    */
   public long dumpToChannel(byte[] a, int start, int len) throws IOException {
     if (channel==null) throw new IOException("Channel not open");
     for (int i=start; i<start+len; i++) dataout.writeByte(a[i]);  
     return channel.position();
   }
     
   /** write an int[] to this ArrayFile, overwriting it.
    * The file needs to be closed prior to calling this method,
    * and will be closed after completion. Usually called as
    * new ArrayFile(filename).dump(a, 0, a.length);
    * @param a  the array
    * @param start  start dumping a at a[start]
    * @param len  dump len array elements
    */
   public void dump(int[] a, int start, int len) throws IOException {
     this.openW();
     for (int i=start; i<start+len; i++) dataout.writeInt(a[i]);
     this.close();
   }
   
   /** write a byte[] to this ArrayFile, overwriting it.
    * The file needs to be closed prior to calling this method,
    * and will be closed after completion. Usually called as
    * new ArrayFile(filename).dump(a, 0, a.length);
    * @param a  the array
    * @param start  start dumping a at a[start]
    * @param len  dump len array elements
    */
   public void dump(byte[] a, int start, int len) throws IOException {
     this.openW();
     for (int i=start; i<start+len; i++) dataout.writeByte(a[i]);
     this.close();
   }
   
   /******************************************************************************************************/
     
   /** dumps a byte[] to this ArrayFile, overwriting it */
   //public long write(int[] array)  throws IOException 
   //{ return put(array,false); }
   
   /** dumps a byte[] to this ArrayFile, appending to it */
   //public long append(int[] array) throws IOException 
   //{ return put(array,true);  }

   
   /********************************* slurp-in methods ************************/
  
   
   /** slurps part of this ArrayFile into either an existing or a new byte[].
    * @param array  null or an existing byte[]
    * If the given array is null or has too small length, a new array is allocated and returned.
    * If the given array has sufficient length, its first positions are filled with the data of the file.
    * @param startindex  specifies at which byte in the file to start reading (incl.)
    * @param stopindex   specifies at which byte in the file to stop reading (excl.); use -1 to slurp the whole file.
    * @return  the array containing the file's contents.
    */
   public byte[] slurp(byte[] array, long startindex, long stopindex) throws IOException
   {
      long thislen = length();
      if (stopindex<0) stopindex=thislen;
      long readlen = stopindex - startindex;
      if (startindex>stopindex || stopindex>thislen)
        throw new IllegalArgumentException("Illegal startindex or stopindex");
      if (readlen>Integer.MAX_VALUE)
        throw new IllegalArgumentException("Range too long");
      int len = (int)readlen;
      if(array==null || array.length<len) array = new byte[len];
      FileChannel fc = new FileInputStream(name).getChannel();
      MappedByteBuffer bb = fc.map(MapMode.READ_ONLY, startindex, readlen);
      fc.close();
      for (int i=0; i<len; i++) array[i] = bb.get();
      return array;
    }

   /** slurps this ArrayFile into either an existing or a new byte[]
    * @param array  null or an existing byte[]
    * If the given array is null or has incorrect length, a new array is allocated and returned.
    * If the given array has the correct length, it is filled with the data of the file.
    * @return  the array containing the file contents
    */
   public byte[] slurp(byte[] array) throws IOException
   {
      int len = (int)length();
      try {
        if(array==null || array.length!=len) array = new byte[len];
      } catch(OutOfMemoryError ex) {
        throw new IOException(String.format("cannot slurp '%s': allocatable byte[] is %.0fM, requested %.0fM%n", 
            this.name, ArrayUtils.largestAllocatable()/1E6, len/1E6), ex);
      }
//      DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(name), BUFSIZE));
      MappedByteBuffer bb = this.mapR();
      for (int i=0; i<len; i++) array[i] = bb.get();
      return array;
    }
   
   
   /** slurps this ArrayFile into either an existing or a new int[].
    * @param array  null or an existing int[]
    * If the given array is null or has incorrect length, a new array is allocated and returned.
    * If the given array has the correct length, it is filled with the data of the file.
    * @return  the array containing the file contents
    */
   public int[] slurp(int[] array) throws IOException
   {
      long size = length();
      if (size%4!=0) throw new IOException("File size of '"+this.name+"' not divisible by 4");
      int len = (int)(size/4L);
      if(array==null || array.length!=len) array = new int[len];
      IntBuffer ib = this.mapR().asIntBuffer();
      for (int i=0; i<len; i++) array[i] = ib.get();
      return array;
   }

   /** slurps this ArrayFile into either an existing or a new int[].
    * This method differs from the slurp method only by the fact that the existing array
    * may be larger than the correct length without being re-allocated.
    * @param array  null or an existing int[]
    * If the given array is null or too short, a new array is allocated and returned.
    * If the given array has sufficient length, a prefix of it is filled with the data of the file.
    * @return  the array containing the file contents in a prefix
    */
   public int[] slurpIntoPrefix(int[] array) throws IOException
   {
      long size = length();
      if (size%4!=0) throw new IOException("File size not divisible by 4");
      int len = (int)(size/4L);
      if(array==null || array.length<len) array = new int[len];
      IntBuffer ib = this.mapR().asIntBuffer();
      for (int i=0; i<len; i++) array[i] = ib.get();
      return array;
   }
   
  
   // -------------------------------------------------------------------------
   
   /** counts the number of occurrences of each byte (0..255) in the file.
    * @return long[256], new array of longs containing the counts
    */
   public long[] byteCounts() throws IOException
   {
      return byteCounts(null);
   }
  
   /** counts the number of occurrences of each byte (0..255) in the file.
    * @param counts array where to store the counts. 
    *   If its size is different from 256, a new array is allocated. 
    * @return counts or a new long[256] containing the counts
    */ 
   public long[] byteCounts(long[] counts) throws IOException
   {
      if (counts == null || counts.length!=256) counts = new long[256];
      FileChannel fc = new FileInputStream(name).getChannel();
      long length = fc.size();
      MappedByteBuffer buf = fc.map(MapMode.READ_ONLY, 0, length);
      
      byte b;
      for(long i=0; i<length; i++)
      {
         b=buf.get();
         if(b<0) counts[b+256]++;
         else counts[b]++;
      }
      fc.close();
      return counts;
   }
   
   /** prints the given count array to the specified PrintStream.
    * This is just a convenience method for testing/debugging.
    * @param out the stream to print to
    * @param counts the array containing counts
    */
   public static void showCounts(PrintStream out, long[] counts)
   {
      for(int i=0; i<counts.length; i++)
      {
         if (counts[i]>0)
         {
            char ch=(char)i;
            out.printf("%3d %c %d",i,Character.isISOControl(ch)?'?':ch,counts[i]);
            out.println();
         }
      }
   }

   /** prints the given count array to stdout.
    * This is just a convenience method for testing/debugging.
    * @param counts the array containing the counts
    */
   public static final void showCounts(long[] counts)
   {
      showCounts(System.out, counts);
   }
   
   
   //-------------------------------------------------------------------------
   
   /** indicates whether this ArrayFile can be translated by the given AlphabetMap
    * @param amap the alphabet map
    * @return true or false
    */
   public boolean isTranslatableBy(AlphabetMap amap) throws IOException
   {
      long[] counts = this.byteCounts();
      for(int i=0; i<counts.length; i++)
      {
         if (counts[i]>0 && !amap.isPreValid(i)) return false;
      }
      return true;
   }
   
   
   public void translateToFile(String outname, AlphabetMap amap, boolean appendSeparator)
   throws IOException, InvalidSymbolException
   {
      FileChannel fcin = new FileInputStream(name).getChannel();
      long length = fcin.size();
      long ll = appendSeparator? length+1 : length;
      MappedByteBuffer in = fcin.map(MapMode.READ_ONLY, 0, length);
      
      FileChannel fcout = new RandomAccessFile(outname,"rw").getChannel();
      MappedByteBuffer out = fcout.map(MapMode.READ_WRITE,0, ll);
      
      for (long i=0; i<length; i++) out.put(amap.code(in.get()));
      if (appendSeparator) out.put(amap.codeSeparator());
      fcin.close();
      fcout.close();
   }
   
   
   public byte[] translateToArray(AlphabetMap amap, boolean appendSeparator)
   throws IOException, InvalidSymbolException
   {
      FileChannel fc = new FileInputStream(name).getChannel();
      long length = fc.size();
      int ll = (int)length + (appendSeparator? 1: 0);
      MappedByteBuffer buf = fc.map(MapMode.READ_ONLY, 0, length);
      byte[] translation = new byte[ll];
      
      for(int i=0; i<length; i++) translation[i] = amap.code(buf.get());
      fc.close();
      if (appendSeparator) translation[ll-1] = amap.codeSeparator();
      return translation;
   }
   
   // -------------------------------------------------------------------------
   
   
   /** writes this ArrayFile to disk by translating a given String via an AlphabetMap
    *@param s the string to be translated
    *@param amap the alphabet map for translation
    *@append whether to append to an existing file
    *@writeSeparator whether to append the separator to the end of the translated string
    */
   public void writeTranslatedFromString(String s, AlphabetMap amap,
      boolean append, boolean writeSeparator)
      throws IOException, InvalidSymbolException
   {
      RandomAccessFile f = new RandomAccessFile(name,"rw");
      FileChannel fcout  = f.getChannel();
      int slen = s.length();
      long flen = fcout.size();
      long start  = append? flen: 0;
      if (flen>0 && append && writeSeparator)
      {
         f.seek(flen-1);
         if (f.readByte()!=amap.codeSeparator())
         { f.writeByte(amap.codeSeparator()); flen++; }
      }
      long newlen = append? (flen+slen) : slen;
      if (writeSeparator) newlen++;
      f.seek(newlen-1); f.writeByte(0); f.seek(start);
      MappedByteBuffer buf = fcout.map(MapMode.READ_WRITE, start, newlen-start);
      for(int i=0; i<slen; i++) buf.put(amap.code((byte)s.charAt(i)));
      if (writeSeparator) buf.put(amap.codeSeparator());
      fcout.close();
      f.close();
   }
   
}
