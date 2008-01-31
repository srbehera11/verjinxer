package verjinxer.util;

import java.util.ArrayList;
import java.util.Collection;

public class MultiQGramCoder {
  private QGramCoder coder;
  private BisulfiteQGramCoder bicoder;
  private boolean bisulfite;
  private int qcode = 0;
  private int asize;
  
  public MultiQGramCoder(int q, int asize, boolean bisulfite) {
    if (bisulfite) {
      if (asize != 4)
        throw new IllegalArgumentException("If bisulfite is true, asize must be 4.");
      bicoder = new BisulfiteQGramCoder(q);
      coder = bicoder.getCoder();
      this.bisulfite = bisulfite;
      this.asize = asize;
    } else {
      coder = new QGramCoder(q, asize);
    }
  }
  
  public int numberOfQGrams() {
    return coder.numberOfQGrams();
  }
  
  public int getq() {
    return coder.getq();
  }
  
  public int getAsize() {
    return coder.getAsize();
  }
  
  public void update(byte next) {
    if (bisulfite) {
      assert(0 <= next && next < asize);
      bicoder.update(next);
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
