/*
 * AlphabetMap.java
 *
 * Created on December 12, 2006, 5:40 AM
 *
 */

package verjinxer.sequenceanalysis;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 *
 * @author Sven Rahmann
 */
public class AlphabetMap {
  private String[] initstrings;
  private byte[] myimage;
  private byte[] mypreimage;
  private byte[] modepreimage;
  private byte[] modeimage;
  private int mywhitespace = 128+2;
  private int mywildcard   = 128+4;
  private int myseparator =  128+8;

  
  /** creates an alphabet map from the given text lines.
   * It is best to see the example alphabet maps for how to do this,
   * e.g., which strings create the DNA() map, etc.
   *@param lines  the text lines from which to create the alphabet map
   *@returns  the created alphabet map
   */
  public AlphabetMap init(final String[] lines) {
    initstrings = lines;
    mywhitespace = 128+2;
    mywildcard   = 128+4;
    myseparator  = 128+8;
    myimage = new byte[256];
    mypreimage = new byte[256];
    modepreimage = new byte[256];
    modeimage = new byte[256];
    
    int i=0;
    byte mode=1; // 0: invalid, 1: normal characters, 2: whitespace, 4: wildcards, 8: separators,
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
          mode=4; modeimage[ii]=mode; mywildcard=i;
        } else if(ll.startsWith("separator")) {
          mode=8; modeimage[ii]=mode; myseparator=i; 
        } else if(ll.startsWith("whitespace")) {
          mode=2; modeimage[ii]=mode; mywhitespace=i;
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
   * @returns the created alphabet map
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
  
  /** checks whether a given character can be translated by this alphabet map */
  public final boolean isPreValid(final int p) {
    return (modepreimage[(p<0)?(p+256):p]!=0);
  }
  
  public final boolean isValid(final int i) {
    return (modeimage[(i<0)?(i+256):i]!=0);
  }
  
  public final boolean isSymbol(final int i) {
    byte m = modeimage[(i<0)?(i+256):i];
    return (m==1 || m==2);
  }
  
  public final boolean isWildcard(final int i) {
    byte m = modeimage[(i<0)?(i+256):i];
    return (m==4);
  }
  
  public final boolean isSeparator(final int i) {
    byte m = modeimage[(i<0)?(i+256):i];
    return (m==8);
  }
  
  public final boolean isSpecial(final int i) {
    return (modeimage[(i<0)?(i+256):i]>2); // wildcard or separator
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
  
  /** print the image of the alphabet map to a PrintWriter */
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
  
  
  /************************** translation codes ****************************/
  
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
   * @returns  the charecter that corresponds to the pre-image of the given code under this alphabet map.
   * This method throws an InvalidSymbolException if the code is not valid.
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
   * @returns the string of concatenated pre-images of a[offset .. offset+len-1]
   * This method throws an InvalidSymbolException on failure.
   */
  public final String preimage(byte[] a, int offset, int len) throws InvalidSymbolException {
    StringBuilder s = new StringBuilder(len);
    for (int i=0; i<len; i++) s.append(preimage(a[offset+i]));
    return s.toString();
  }
  
  /**************************** applyTo functions ***********************/
  
  
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
  
  /** translate byte array in place */
  public void applyTo(byte[] s, final boolean setSeparator)
  throws InvalidSymbolException {
    applyTo(s,setSeparator,false);
  }
  
  /** translate byte array in place */
  public void applyTo(byte[] s, final boolean setSeparator, final boolean separateByWildcard)
  throws InvalidSymbolException {
    int l = s.length;
    int ll = l-(setSeparator?1:0);
    for (int i=0; i<ll; i++) s[i]=code(s[i]);
    if (setSeparator) s[ll] = (separateByWildcard? codeWildcard() : codeSeparator());
  }
  
  /** translate string into ByteBuffer (may be null -> reallocate) */
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
  
  /** static DNA alphabet */
  public static final AlphabetMap DNA() {
    AlphabetMap a = new AlphabetMap();
    a.init(new String[] {
      "##symbols:0", "Aa","Cc","Gg","TtUu",
      "##wildcards", "XxNnWwRrKkYySsMmBbHhDdVv",
      "##wildcards", "#",
      "##separators:-1" });
    return a;
  }
  
  /** static complementary DNA alphabet */
  public static final AlphabetMap cDNA() {
    AlphabetMap a = new AlphabetMap();
    a.init(new String[] {
      "##symbols:0", "TtUu","Gg","Cc","Aa",
      "##wildcards", "XxNnWwRrKkYySsMmBbHhDdVv",
      "##wildcards", "#",
      "##separators:-1" });
    return a;
  }
  
  /** static bisulfite-treated nonmethylated DNA alphabet */
  public static final AlphabetMap biDNA() {
    AlphabetMap a = new AlphabetMap();
    a.init(new String[] {
      "##symbols:0", "Aa","Zz","Gg","CcTtUu",
      "##wildcards", "XxNnWwRrKkYySsMmBbHhDdVv",
      "##wildcards", "#",
      "##separators:-1" });
    return a;
  }
  
  /** static complementary bisulfite-treated nonmethylated DNA alphabet */
  public static final AlphabetMap cbiDNA() {
    AlphabetMap a = new AlphabetMap();
    a.init(new String[] {
      "##symbols:0", "AaGg","Cc","Zz","TtUu",
      "##wildcards", "XxNnWwRrKkYySsMmBbHhDdVv",
      "##wildcards", "#",
      "##separators:-1" });
    return a;
  }

  public static final AlphabetMap NUMERIC() {
    AlphabetMap a = new AlphabetMap();
    a.init(new String[] {
      "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
      "##separators:-1" });
    return a;
  }
  
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

// end class  
}

