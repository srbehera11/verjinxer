package verjinxer.sequenceanalysis;

import java.util.ArrayList;
import java.util.Collection;

public class MultiQGramCoder {
  private QGramCoder coder;
  private BisulfiteQGramCoder bicoder;
  private boolean bisulfite;
  private int qcode = 0;
  
  public final int asize;
  public final int q;
  public final int numberOfQGrams;
  
  public MultiQGramCoder(final int q, final int asize, final boolean bisulfite) {
    if (bisulfite) {
      if (asize != 4)
        throw new IllegalArgumentException("If bisulfite is true, asize must be 4.");
      bicoder = new BisulfiteQGramCoder(q);
      coder = bicoder.getCoder();
    } else {
      coder = new QGramCoder(q, asize);
    }
    this.bisulfite = bisulfite;
    this.asize = asize;
    this.q     = q;
    this.numberOfQGrams = coder.numberOfQGrams;
  }
    
  
  public void update(byte next, byte after) {
    if (bisulfite) {
      assert(0 <= next && next < asize);
      bicoder.update(next, after);
    }
    else {
      qcode = coder.codeUpdate(qcode, next);
      assert(qcode != -1);
    }
  }
  
  public void reset() {
    if (bisulfite)
      bicoder.reset();
    else
      qcode = 0;
  }
  
  /**
   * 
   * @return 
   */
  public Collection<Integer> getCodes() {
    if (bisulfite)
      return bicoder.getCodes();
    else {
      Collection<Integer> r = new ArrayList<Integer>();
      r.add(qcode);
      return r;
    }
  }

  
}
