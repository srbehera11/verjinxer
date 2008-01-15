/*
 * SuffixTrayBuilderTest.java
 * JUnit based test
 *
 * Created on 10. Mai 2007, 01:25
 */

package verjinxer;

import java.util.Locale;
import junit.framework.*;
import verjinxer.sequenceanalysis.*;
import verjinxer.util.*;
import static verjinxer.Globals.*;

/**
 *
 * @author Sven
 */
public class SuffixTrayBuilderTest extends TestCase {
  
  public SuffixTrayBuilderTest(String testName) {
    super(testName);
  }

  @Override
  protected void setUp() throws Exception {
  }

  @Override
  protected void tearDown() throws Exception {
  }


  /**
   * Test of main method, of class verjinxer.SuffixTrayBuilder.
   */
  public void testAll() {
    //doAllBinaryStrings();
    //doKlausStrings();
    doAAA();
  }
 
  
  
  public void doKlausStrings() {
    final int MAXONES = 25;
    Globals g = new Globals();
    g.quiet = true;
    SuffixTrayBuilder stb = new SuffixTrayBuilder(g);
    stb.amap = AlphabetMap.NUMERIC();
    byte[] t = null;
    int L, stp;
    boolean appendleft = false;
    StringBuilder s = new StringBuilder("1");
    StringBuilder ss;
    
    for(int i=1; i<MAXONES; i++) {
      int zeros = (1<<(i-1));
      if (appendleft) {
        ss = new StringBuilder(zeros+1+16);
        ss.append('1');
        for (int j=0; j<zeros; j++) ss.append('0');
        s.insert(0,ss);
        appendleft=false;
      } else {
        for (int j=0; j<zeros; j++) s.append('0');
        s.append('1');
        appendleft=true;
      }      
      L = s.length();
      s.append('$');
      t = s.toString().getBytes();
      s.delete(s.length()-1,s.length());
      assertTrue(t.length==L+1);
      assertTrue(s.length()==L);
      for(int j=0; j<L; j++) t[j]-=48;
      t[L]=-1;
      stb.s = t;
      stb.n = L+1;
      stb.steps = 0;
      stb.buildpos_rah2min();
      stp = (int) stb.steps -1; // -1 for $
      //result = stb.checkpos_rah1();
      //assertEquals(0, result);

      System.out.printf(Locale.US, "Ones: %d. Length: %d. Steps: %d. Steps/char: %.2f.%n", 
          i+1, L, stp, (double)stp/L, s.toString());
    }
  }
  
  
  public void doAllBinaryStrings() {
    final int asize =  2;
    final int minL  = 20;
    final int maxL  = 20;
    
    Globals g = new Globals();
    g.quiet = true;
    SuffixTrayBuilder stb = new SuffixTrayBuilder(g);
    stb.amap = AlphabetMap.NUMERIC();

    int result;
    long[] maxsteps = new long[maxL+1];
    long[] totalsteps = new long[maxL+1];
    String[] maxt  = new String[maxL+1];
    
    for (int L=minL; L<=maxL; L++) {
      System.out.printf("Length %d:%n", L);
      byte[] t = new byte[L+1];
      t[L]=-1;
      long cntr = 0;
      stb.s = t;
      stb.n = L+1;
      totalsteps[L]=0;
      //t[0]=1; // save half the work for rah2min(), since everything starts with 1
      
      while(true) {     
        // 1. process t
        stb.steps = 0;
        cntr++;
        stb.buildpos_rah1up();
        //result = stb.checkpos_rah1();
        //assertEquals(0, result);
        totalsteps[L]+=stb.steps;
        if (stb.steps>maxsteps[L]) {
          maxsteps[L]=stb.steps;
          maxt[L] = Strings.join("",t,0,L);
        }     
        // 2. advance t and quit if all done.
        int i;
        for (i=L-1; i>=0; i--) { t[i]++; if (t[i]==asize) t[i]=0; else break; }
        if (i<0) break;
      }
      //assertEquals(1L<<L, cntr);
      System.out.printf("  maxsteps=%d; text=%s%n", maxsteps[L], maxt[L]);
    }
    
    for (int L=minL; L<=maxL; L++) {
      System.out.printf(Locale.US, "%2d  %3d  (+%2d)  %.2f   %9d  %.4f%n", 
          L, maxsteps[L], maxsteps[L]-maxsteps[L-1], (double)maxsteps[L]/L, totalsteps[L], (double)totalsteps[L]/((1L<<L)*L) );
    }
    
  }

  
public void doAAA() {
    //final int asize =  2;
    final int minL  = 10;
    final int maxL  = 10;
    int result;
    
    Globals g = new Globals();
    g.quiet = true;
    SuffixTrayBuilder stb = new SuffixTrayBuilder(g);
    stb.amap = AlphabetMap.NUMERIC();
    
    for (int L=minL; L<=maxL; L++) {
      System.out.printf("Length %d:%n", L);
      byte[] t = new byte[L+2]; // initializes to zero
      t[L+1]=-1;
      t[L]=1; // a...ab$
      stb.s = t;
      stb.n = L+1;      
      stb.steps = 0;
      stb.buildpos_rah1up();
      result = stb.checkpos_rah1();
      assertEquals(0, result);
      System.out.printf("  steps=%d; text=%s%n", stb.steps, Strings.join("",t,0,L));
    }
        
  }
  
  

}
