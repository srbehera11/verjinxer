/*
 * AlphabetMap.java
 *
 * Created on December 12, 2006, 5:40 AM
 *
 */

package verjinxer.sequenceanalysis;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import verjinxer.util.ArrayFile;

/**
 *
 * @author Sven Rahmann
 */
public class AlphabetMap {
  
  static final int INVALID = 0;
  static final int NORMAL = 1;
  static final int WHITESPACE = 2;
  static final int WILDCARD = 4;
  static final int SEPARATOR = 8;
  
  private String[] initstrings;
  private byte[] myimage;
  private byte[] mypreimage;
  private byte[] modepreimage;
  private byte[] modeimage;
  private int mywhitespace = 128+WHITESPACE;
  private int mywildcard   = 128+WILDCARD;
  private int myseparator =  128+SEPARATOR;
  
  /** creates an alphabet map from the given text lines.
   * It is best to see the example alphabet maps for how to do this,
   * e.g., which strings create the DNA() map, etc.
   *@param lines  the text lines from which to create the alphabet map
   *@return  the created alphabet map
   */
  public AlphabetMap init(final String[] lines) {
    initstrings = lines;
    mywhitespace = 128+WHITESPACE;
    mywildcard   = 128+WILDCARD;
    myseparator  = 128+SEPARATOR;
    myimage = new byte[256];
    mypreimage = new byte[256];
    modepreimage = new byte[256];
    modeimage = new byte[256];
    
    int i=0;
    byte mode=1;
    for (String l : lines) {
      if(l.startsWith("##")) {
        String ll=l.substring(2);
        String istring;
        int icolon;
        if ((icolon=ll.indexOf(':'))>=0) {
          istring=ll.substring(icolon+1);
          if (istring.length()>0) i=Integer.decode(istring);
          ll=ll.substring(0,icolon);
        }
        int ii = (i<0)?i+256:i;
        ll = ll.toLowerCase();
        if(ll.startsWith("symbol")) 
          mode=1;
        else if(ll.startsWith("wildcard")) {
          mode=WILDCARD; modeimage[ii]=mode; mywildcard=i;
        } else if(ll.startsWith("separator")) {
          mode=SEPARATOR; modeimage[ii]=mode; myseparator=i; 
        } else if(ll.startsWith("whitespace")) {
          mode=WHITESPACE; modeimage[ii]=mode; mywhitespace=i;
          mypreimage[ii]=' ';
          for(char cc=0; cc<=32; cc++) if (Character.isWhitespace(cc)) {
            modepreimage[cc]=mode;
            myimage[cc]=(byte)i;
          }
        } else 
          throw new RuntimeException("Invalid annotation in alphabet map file: "+ll);
      } else {
        int ii = (i<0)?i+256:i;
        byte[] chs = l.getBytes();
        if (chs.length==0) { modeimage[i++]=mode; continue; }
        mypreimage[ii]=chs[0];
        modeimage[ii]=mode;
        for (int j=0; j<chs.length; j++) {
          int ch=chs[j]; if (ch<0) ch+=256;
          myimage[ch]=(byte)i;
          modepreimage[ch]=mode;
        }
        i++;
      }
    } // end for (l in lines)
    return this;
  } // end method 'init'
  
  
  /** Another method to iniialize an alphabet map. This method reads
   * all lines from a given text file into a String[] and creates the
   * alphabet map from these.
   * @param fname  name of the text file
   * @return  the created alphabet map
   * @throws java.io.IOException 
   */
  public AlphabetMap init(final String fname) throws IOException {
    ArrayList<String> lines = new ArrayList<String>();
    BufferedReader inf = new BufferedReader(new FileReader(fname));
    String s;
    while((s = inf.readLine())!=null) lines.add(s);
    inf.close();
    return this.init(lines.toArray(new String[0]));
  }
  
  
  /*********************** mode check methods ********************/
  
  /** checks whether a given character can be translated by this alphabet map
   * @param p the character
   * @return  true if p can be translated
   */
  public final boolean isPreValid(final int p) {
    return (modepreimage[(p<0)?(p+256):p]!=0);
  }
  
  public final boolean isValid(final int i) {
    return (modeimage[(i<0)?(i+256):i]!=0);
  }
  
  public final boolean isSymbol(final int i) {
    byte m = modeimage[(i<0)?(i+256):i];
    return m==NORMAL || m==WHITESPACE;
  }
  
  public final boolean isWildcard(final int i) {
    byte m = modeimage[(i<0)?(i+256):i];
    return m==WILDCARD;
  }
  
  public final boolean isSeparator(final int i) {
    byte m = modeimage[(i<0)?(i+256):i];
    return m==SEPARATOR;
  }
  
  public final boolean isSpecial(final int i) {
    byte m = modeimage[(i<0)?(i+256):i]; 
    return m == WILDCARD || m == SEPARATOR;
  }
  
  public final int smallestSymbol() {
    for(int i=0; i<256; i++)
      if (isSymbol(i)) return i;
    return -1;
  }
  
  public final int largestSymbol() {
    for(int i=255; i>=0; i--)
      if (isSymbol(i)) return i;
    return -1;
  }
  
  
  /******************** "show" methods ***********************************/
  
  /** print the image of the alphabet map to a PrintWriter
   * @param out  the PrintWriter
   */
  public void showImage(PrintWriter out) {
    for(int i=0; i<256; i++) {
      if (modeimage[i]==0) continue;
      out.printf("%d  %d  %d",i,modeimage[i],mypreimage[i]);
      out.println();
    }
    out.flush();
  }
  
  public void showImage() {
    this.showImage(new PrintWriter(System.out)); }
  
  
  public void showPreimage(PrintWriter out) {
    for(int i=0; i<256; i++) {
      if (modepreimage[i]==0) continue;
      out.printf("%d   %d  %d",i,modepreimage[i],myimage[i]);
      out.println();
    }
    out.flush();
  }
  
  public void showPreimage() {
    this.showPreimage(new PrintWriter(System.out)); }
  
  
  /** write source strings to a given PrintWriter
   * @param out the PrintWriter to write to
   */
  public void showSourceStrings(PrintWriter out) {
    for (String s : initstrings) {
      out.println(s);
    }
    out.flush();
  }
  
  /** write source strings to System.out */
  public void showSourceStrings() {
    this.showSourceStrings(new PrintWriter(System.out));
  }
  
  
  /** @return array of strings that define the alphabet map */
  public String[] asStrings() {
    return initstrings;
  }
  
  
  /************************** translation codes **************************/
   
  /** @param b  the character to translate
   * @return the code corresponding to character b
   * @throws verjinxer.sequenceanalysis.InvalidSymbolException 
   */
  public final byte code(byte b) throws InvalidSymbolException {
    int bb = (b<0)?b+256:b;
    if (modepreimage[bb]==0)
      throw new InvalidSymbolException("Symbol "+bb+" ("+(char)bb+") not in alphabet");
    return myimage[bb];
  }
  
  public final byte codeSeparator() throws InvalidSymbolException {
    if (myseparator<-128 || myseparator>127) throw new InvalidSymbolException();
    return (byte)myseparator;
  }
  
  public final byte codeWhitespace() throws InvalidSymbolException {
    if (mywhitespace<-128 || mywhitespace>127) throw new InvalidSymbolException();
    return (byte)mywhitespace;
  }
  
  public final byte codeWildcard()  throws InvalidSymbolException {
    if (mywildcard<-128 || mywildcard>127) throw new InvalidSymbolException();
    return (byte)mywildcard;
  }
  
  /** Computes the pre-image of a given code.
   * @param c  the code 
   * @return the charecter that corresponds to the pre-image of the given code under this alphabet map.
   * @throws verjinxer.sequenceanalysis.InvalidSymbolException if the code is not valid.
   */
  public final char preimage(byte c) throws InvalidSymbolException {
    int cc = (c<0)?c+256:c;
    if (modeimage[cc]==0)
      throw new InvalidSymbolException("Code "+cc+" not in alphabet");
    return (char)(mypreimage[cc]<0? mypreimage[cc]+256 : mypreimage[cc]);
  }
  
  /** Compute the pre-image of an array of given codes
   * @param a  the array of code values
   * @param offset  where to start computing pre-images in a
   * @param len  how many pre-images to compute
   * @return the string of concatenated pre-images of a[offset .. offset+len-1]
   * @throws verjinxer.sequenceanalysis.InvalidSymbolException if there is a problem
   */
  public final String preimage(byte[] a, int offset, int len) throws InvalidSymbolException {
    StringBuilder s = new StringBuilder(len);
    for (int i=0; i<len; i++) s.append(preimage(a[offset+i]));
    return s.toString();
  }
  
  /**************************** applyTo functions ***********************/
  
  /**
   * translate a string, and possibly append a separator at the end
   * @param s  the string
   * @param appendSeparator  set to true if you want to append a separator at the end
   * @return the translated byte array
   * @throws verjinxer.sequenceanalysis.InvalidSymbolException
   */
  public byte[] applyTo(final String s, final boolean appendSeparator)
  throws InvalidSymbolException {
    return applyTo(s,appendSeparator,false);
  }
  
  public byte[] applyTo(final String s, final boolean appendSeparator, final boolean separateByWildcard)
  throws InvalidSymbolException {
    int l = s.length();
    byte[] ba = new byte[(appendSeparator?l+1:l)];
    for (int i=0; i<l; i++) ba[i]=code((byte)s.charAt(i));
    if (appendSeparator)
      ba[l] = (separateByWildcard? codeWildcard() : codeSeparator());
    return ba;
  }
  
  /** translate byte array in place
   * @param s  the original byte array (modified during the process!)
   * @param setSeparator  set to true if you want to set the last byte in s to the separator
   * @throws verjinxer.sequenceanalysis.InvalidSymbolException 
   */
  public void applyTo(final byte[] s, final boolean setSeparator)
  throws InvalidSymbolException {
    applyTo(s,setSeparator,false);
  }
  
  /** translate byte array in place
   * @param s  the original byte array (modified during the process!)
   * @param setSeparator  set to true if you want to set the last byte in s to the separator
   * @param separateByWildcard  set to true if you want to use the wildcard code instead of the separator code
   *   at the end (only has and effect if setSeparator is true)
   * @throws verjinxer.sequenceanalysis.InvalidSymbolException
   */
  public void applyTo(final byte[] s, final boolean setSeparator, final boolean separateByWildcard)
  throws InvalidSymbolException {
    int l = s.length;
    int ll = l-(setSeparator?1:0);
    for (int i=0; i<ll; i++) s[i]=code(s[i]);
    if (setSeparator) s[ll] = (separateByWildcard? codeWildcard() : codeSeparator());
  }
  
  /** translate string into ByteBuffer (may be null -> reallocate)
   * @param sequence  the string to translate
   * @param buf the ByteBuffer to translate s into. This can be null, in which case
   *   a new byte buffer is allocated. A new buffer is also allocated if the given 
   *   buffer is too small to fit the translated string.
   * @param append  set to true to append a character (eg, a separator) at the end
   * @param appendwhat  specify the character to append
   * @return  either the target ByteBuffer buf, or a newly allocated buffer
   * @throws verjinxer.sequenceanalysis.InvalidSymbolException
   */
  public ByteBuffer applyTo(final String sequence, ByteBuffer buf, final boolean append, final byte appendwhat) 
  throws InvalidSymbolException {
    int ll = sequence.length();
    int req = ll + (append? 1:0);
    if (buf==null || buf.capacity()<req)  buf = ByteBuffer.allocateDirect(req+1024);
    buf.limit(buf.capacity());
    buf.position(0);
    for (int i=0; i<ll; i++) buf.put(code((byte)sequence.charAt(i)));
    if (append) buf.put(appendwhat);
    assert(buf.position()==req);
    buf.flip(); // prepare for writing
    return buf;
  }
  
  
  /*******************  special alphabet maps ******************/
  
  /** 
   * @return the standard DNA alphabet
   */
  public static final AlphabetMap DNA() {
    AlphabetMap a = new AlphabetMap();
    a.init(new String[] {
      "##symbols:0", "Aa","Cc","Gg","TtUu",
      "##wildcards", "XxNnWwRrKkYySsMmBbHhDdVv",
      "##wildcards", "#",
      "##separators:-1" });
    return a;
  }
  
  /**
   * @return  the standard complementary DNA alphabet
   */
  public static final AlphabetMap cDNA() {
    AlphabetMap a = new AlphabetMap();
    a.init(new String[] {
      "##symbols:0", "TtUu","Gg","Cc","Aa",
      "##wildcards", "XxNnWwRrKkYySsMmBbHhDdVv",
      "##wildcards", "#",
      "##separators:-1" });
    return a;
  }
  
  /**
   * @return a representation of the bisulfite-treated nonmethylated DNA alphabet
   */
  public static final AlphabetMap biDNA() {
    AlphabetMap a = new AlphabetMap();
    a.init(new String[] {
      "##symbols:0", "Aa","Zz","Gg","CcTtUu",
      "##wildcards", "XxNnWwRrKkYySsMmBbHhDdVv",
      "##wildcards", "#",
      "##separators:-1" });
    return a;
  }
  
  /** 
   * @return a representation of the complementary bisulfite-treated nonmethylated DNA alphabet
   */
  public static final AlphabetMap cbiDNA() {
    AlphabetMap a = new AlphabetMap();
    a.init(new String[] {
      "##symbols:0", "AaGg","Cc","Zz","TtUu",
      "##wildcards", "XxNnWwRrKkYySsMmBbHhDdVv",
      "##wildcards", "#",
      "##separators:-1" });
    return a;
  }

  /**
   * @return the numeric alphabet 0..9
   */
  public static final AlphabetMap NUMERIC() {
    AlphabetMap a = new AlphabetMap();
    a.init(new String[] {
      "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
      "##separators:-1" });
    return a;
  }
  
  /**
   * @return  the standard protein alphebet
   */
  public static final AlphabetMap Protein() {
    AlphabetMap a = new AlphabetMap();
    a.init(new String[] {
      "##symbols:0", "Aa", "Cc", "Dd", "Ee", "Ff", "Gg", "Hh", "Ii", "Kk", 
        "Ll", "Mm", "Nn", "Pp", "Qq", "Rr", "Ss", "Tt", "Vv", "Ww", "Yy",
      "##wildcards", "BbXxZz",
      "##wildcards", "#",
      "##separators:-1" });
    return a;
  }
  
  /**************************************************************/


   /** indicates whether a given file can be translated by this AlphabetMap
    * @param fname  the file name
    * @return true if the ArrayFile can be translated, false otherwise
    * @throws java.io.IOException   if an IO error occurs
    */
   public boolean isApplicableToFile(final String fname) throws IOException {
      final ArrayFile arf = new ArrayFile(fname, 0);
      final long[] counts = arf.byteCounts();
      for (int i = 0; i < counts.length; i++)
         if (counts[i] > 0 && !this.isPreValid(i)) return false;
      return true;
   }

   /** translate one byte file to another, applying this alphabet map 
    * @param inname   name of input file
    * @param outname  name of output file
    * @param appendSeparator  set true if you want to append a separator at the end
    * @throws java.io.IOException  if an IO exception occurs
    * @throws verjinxer.sequenceanalysis.InvalidSymbolException if a symbol in the input file cannot be translated
    */
   public void translateFileToFile(final String inname, final String outname, final boolean appendSeparator)
           throws IOException, InvalidSymbolException {
      final ArrayFile afin = new ArrayFile(inname, 0);
      final ByteBuffer in = afin.mapR();
      final long length = afin.length();
      final long ll = appendSeparator ? length + 1 : length;
      final ArrayFile afout = new ArrayFile(outname, 0);
      final ByteBuffer out = afout.mapRW();
      for (long i = 0; i < length; i++)  out.put(this.code(in.get()));
      if (appendSeparator) out.put(this.codeSeparator());
   }

   /**
    * translate a byte file to a byte array, applying this alphabet map 
    * @param inname   name of input file
    * @param translation  the array where to store the translated string (must have large enough size).
    *    If null or too small, a new array with sufficient size is allocated.
    * @param appendSeparator  set true if you want to append a separator at the end
    * @return  the newly allocated translated byte array, or 'translation'
    * @throws java.io.IOException
    * @throws verjinxer.sequenceanalysis.InvalidSymbolException
    */
   public byte[] translateFileToByteArray(final String inname, byte[] translation, final boolean appendSeparator)
           throws IOException, InvalidSymbolException {
      final ArrayFile afin = new ArrayFile(inname, 0);
      final ByteBuffer buf = afin.mapR();
      final long length = afin.length();
      final int ll = (int) length + (appendSeparator ? 1 : 0);
      if (translation==null || translation.length<ll) translation = new byte[ll];
      for (int i = 0; i < length; i++) translation[i] = this.code(buf.get());
      if (appendSeparator) translation[ll - 1] = this.codeSeparator();
      return translation;
   }

   /** translate a string to a given file, possibly appending to the end of the file.
    *@param s the string to be translated
    *@param fname  the file name of the translated file
    *@param append whether to append to an existing file
    *@param writeSeparator whether to append the separator to the end of the translated string.
    *  If both append and writeSeparator are true, and the existing file does not end with a separator,
    *  a separator is appended prior to appending the translated string and the final separator.
    * @throws java.io.IOException
    * @throws verjinxer.sequenceanalysis.InvalidSymbolException 
    */
   public void translateStringToFile(final String s, final String fname, final boolean append, final boolean writeSeparator)
           throws IOException, InvalidSymbolException {
      final int slen = s.length();
      RandomAccessFile f = new RandomAccessFile(fname, "rw");
      FileChannel fcout = f.getChannel();
      long flen = fcout.size();
      long start = append ? flen : 0;
      if (flen > 0 && append && writeSeparator) {
         f.seek(flen - 1);
         if (f.readByte() != this.codeSeparator()) {
            f.writeByte(this.codeSeparator());
            flen++;
         }
      }
      long newlen = append ? (flen + slen) : slen;
      if (writeSeparator) newlen++;
      f.seek(newlen - 1);
      f.writeByte(0);
      f.seek(start);
      MappedByteBuffer buf = fcout.map(MapMode.READ_WRITE, start, newlen - start);
      for (int i = 0; i < slen; i++) buf.put(this.code((byte) s.charAt(i)));
      if (writeSeparator) buf.put(this.codeSeparator());
      fcout.close();
      f.close();
   }

// end class  
}
