/*
 * QGramCoder.java
 *
 * Created on December 17, 2006, 3:43 PM
 *
 */

package rahmann.util;

import java.util.Arrays;
import java.util.BitSet;
import rahmann.sequenceanalysis.*;

/**
 * This class contains routines for coding strings of length q over the alphabet
 * {0,1,...,asize-1} as base-asize integers.
 * @author Sven Rahmann
 */
public final class QGramCoder
{
   
   /**
    * Creates a new instance of QGramCoder
    * @param q length of the q-grams coded by this instance
    * @param asize alphabet size of the q-grams coded by this instance
    */
   public QGramCoder(int q, int asize)
   {
      if (q<0 || asize<=0)
         throw new IllegalArgumentException("Need q>0; is "+q+". Need asize>=0; is "+asize+".");
      long power = 1;
      this.q = q;
      this.asize = asize;
      for(int i=1; i<=q-1; i++)
      {
         power*=asize;
         if (power<0 || power>Integer.MAX_VALUE)
            throw new IllegalArgumentException("Value asize^q exceeds maximum integer.");
      }
      if (power*asize<0 || power*asize>Integer.MAX_VALUE)
         throw new IllegalArgumentException("Value asize^q exceeds maximum integer.");
      mod = (int)power; // asize^(q-1)
   }
   
   private final int q;
   private final int asize;
   private final int mod;     // equals asize^(q-1):  q-1 q-2 ... 1 0
   
   /** @return the q-value of this coder */
   public int getq() { return q; }
   
   /** returns the qgram code >0 or -1 if illegal characters appear
    * @param qgram a byte array with the numbers to be interpreted as base-asize number
    */
   public final int code(final byte[] qgram)
   {
      return code(qgram, 0);
   }
   
   /** returns the qgram code >0 or -1 if illegal characters appear
    * @param qgram a byte array with the numbers to be interpreted as base-asize number
    * @param offset position to start looking in the qgram array
    */
   public final int code(final byte[] qgram, final int offset)
   {
      int c=0; int qi;
      for (int i=offset; i<q+offset; i++)
      {
        qi = qgram[i];
         if(qi<0 || qi>=asize) return(-1);
         c%=mod; c*=asize; c+=qi;
      }
      return c;
   }
   
   /** update the current code by shifting out the leftmost *most significant) 
    * character and introducing the next one at the right (least significant)
    * @param old  the old q-gram code
    * @param next the next byte to shift in
    */
   public final int codeUpdate(final int old, final byte next)
   {
      if (next<0 || next>=asize) return(-1);
      return (old%mod)*asize + next;
   }
   
   /** the number of q-grams encoded by this instance 
    * @return number of encoded q-grams 
    */
   public final int numberOfQGrams()
   {
      return mod*asize;
   }
   
   /** return the given q-gram code as q-gram in byte[]-form
    * @param qcode  the q-gram code
    * @param qgram  an existing byte[] of length>=q to store the q-gram;
    *   if null or too small,  a new byte[] is created.
    * @return  the existing or new byte[] containing the q-gram
    */
   public final byte[] qGram(int qcode, byte[] qgram)
   {
      if (qgram==null || qgram.length<q)  qgram=new byte[q];
      for (int p=q-1; p>=0; p--) { qgram[p]=(byte)(qcode%asize); qcode/=asize; }
      return qgram;
   }

   /** return the given q-gram code as q-gram in byte[]-form
    * @param qcode  the q-gram code
    * @return  a new byte[] containing the q-gram
    */
   public final byte[] qGram(int qcode)
   {
      return qGram(qcode, null);
   }

   /** return the given q-gram code as a String via alphabet map translation
    * @param qcode  the q-gram code
    * @param amap  the alphabet map for translation
    * @return  a new string of length q containing the q-gram text;
    *  null if invalid symbols are encountered
    */
  public String qGramString(final int qcode, final AlphabetMap amap) {
    try {
      byte[] qgram = qGram(qcode);
      StringBuilder s = new StringBuilder(q);
        for (int i=0; i<q; i++) s.append(amap.preimage(qgram[i]));
      return s.toString();
    } catch (InvalidSymbolException ex) {
    }
    return null;
  }

  /** create a low-complexity filter for the given instance;
   * this is a BitSet with the property that the c-th bit is 1 iff
   * the q-gram Q corresponding to code c is low-complexity.
   * Low-complexity means that at most numchar characters occur in Q
   * after removing one occurrence of the least frequent character for
   * delta times (see source code for details).
   * @param numchar  complexity threshold; 
   *   the q-gram is low-complexity if it consists of at most this many characters.
   * @param delta  before computing the number of characters, remove 
   *   the least frequent one for delta times.
   * @return the filter BitSet, low-complexity q-grams have their corresponding 
   *   bit set.
   */
  public BitSet createFilter(final int numchar, final int delta) {
    final int aq = mod*asize;
    final BitSet f = new BitSet(aq);
    if(numchar==0) return f;
    final int[]  freq  = new int[asize];
    final byte[] qgram = new byte[q];
    int nc, d, minfreq, mina;
    for (int c=0; c<aq; c++) {
      Arrays.fill(freq, 0); d=delta; nc=0;
      qGram(c, qgram);
      for(int i=0; i<q; i++) freq[qgram[i]]++;
      for(int a=0; a<asize; a++) if (freq[a]>0) nc++;
      if (nc<=numchar) { f.set(c); continue; }
      while(d>0) {
        minfreq = q+1;
        mina    = asize;
        for(int a=0; a<asize; a++) if (freq[a]>0 && freq[a]<minfreq) {minfreq=freq[a]; mina=a; }
        if (minfreq<=d) { freq[mina]=0; d-=minfreq; nc--; }
        else { freq[mina]-=d; d=0; }
      }
      if (nc<=numchar) f.set(c);
    }
    return f;
  }
  
  
  /** create a low-complexity filter for the given instance;
   * this is a BitSet with the property that the c-th bit is 1 iff
   * the q-gram Q corresponding to code c is low-complexity.
   * Low-complexity means that at most numchar characters occur in Q
   * after removing one occurrence of the least frequent character for
   * delta times (see source code for details).
   * @param filterparams  a string of the form "numchar:delta".
   *   numchar is the complexity threshold; 
   *   the q-gram is low-complexity if it consists of at most this many characters.
   *   delta is another parameter: before computing the number of characters, remove 
   *   the least frequent one for delta times.
   * @return the filter BitSet; low-complexity q-grams have their corresponding 
   *   bit set.
   */
  public BitSet createFilter(String filterparams) {
    if(filterparams==null) return createFilter(0,0);
    String[] fstring = filterparams.split(":");
    ffc=Integer.parseInt(fstring[0]);
    if (fstring.length>=2) ffd=Integer.parseInt(fstring[1]);
    return createFilter(ffc,ffd);  
  }
  
  private int ffc=0;  // filter complexity
  private int ffd=0;  // filter delta
  
  final public int getFilterComplexity() { return ffc; }
  final public int getFilterDelta()      { return ffd; }

}
