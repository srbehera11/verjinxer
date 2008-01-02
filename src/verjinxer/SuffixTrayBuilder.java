/*
 * SuffixTrayBuilder.java
 *
 * Created on May 2, 2007, 6:23 PM
 * TODO: include lcp in checking!
 * TODO: include manber myers method!
 */

package verjinxer;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.IntBuffer;
import java.util.Properties;
import java.util.Arrays;
import rahmann.sequenceanalysis.*;
import rahmann.util.*;
import rahmann.util.ArrayFile;
import static verjinxer.Globals.*;

/**
 *
 * @author Sven Rahmann
 */
public class SuffixTrayBuilder {
  
  private Globals g;
  
  /** Creates a new instance of SuffixTrayBuilder */
  public SuffixTrayBuilder(Globals gl) {
    g = gl;
  }
  
  /**
   * print help on usage
   */
  public void help() {
    g.logmsg("Usage:%n  %s suffix [options] Indexnames...%n", programname);
    g.logmsg("Builds the suffix tray (tree plus array) of a .seq file;%n");
    g.logmsg("writes %s, %s (incl. variants 1,1x,2,2x).%n", extpos, extlcp);
    g.logmsg("Options:%n");
    g.logmsg("  -m, --method  <id>    select construction method, where <id> is one of:%n");
    g.logmsg("      rah1up%n" +
        "      rah1down%n" +
        "      rah2min%n" +
        "      rah2both%n"
        );
    g.logmsg("  -l, --lcp[2|1]        build lcp array using int|short|byte%n");
    g.logmsg("  -c, --check           additionally check index integrity%n");
    g.logmsg("  -C, --onlycheck       ONLY check index integrity%n");
    g.logmsg("  -X, --notexternal     DON'T save memory at the cost of lower speed%n");
  }
  
  /** if run independently, call main */
  public static void main(String[] args) {
    new SuffixTrayBuilder(new Globals()).run(args);
  }
  
  
  boolean check     = false;
  boolean onlycheck = false;
  boolean external  = true;
  String method     = null;
  int dolcp         = 0;
  
  int asize        = -1;
  AlphabetMap amap = null;
  byte[] s         = null;   // text
  int n            = -1;     // length of text
  int[] lexfirst   = null;
  int[] lexlast    = null;
  int[] lexpred    = null;
  int[] lexsucc    = null;
  long steps       = 0;
  
  int lcp1x        = 0;      // lcp1 exceptions
  int lcp2x        = 0;      // lcp2 exceptions
  int maxlcp       = -1;
  
  /**
   * @param args the command line arguments
   */
  public int run(String[] args) {
    g.cmdname = "suffixtray";
    int returnvalue = 0;
    String action = "suffixtray \"" + Strings.join("\" \"",args)+ "\"";
    
    Options opt = new Options("c=check,C=onlycheck,X=notexternal=nox=noexternal,m=method:,l=lcp=lcp4,lcp1,lcp2");
    try {
      args = opt.parse(args);
    } catch (IllegalOptionException ex) { g.terminate("suffixtray: "+ex.toString()); }
    if (args.length==0) { help(); g.logmsg("suffixtray: no index given%n"); g.terminate(0); }
    
    // Determine external?, check?, onlycheck? options
    external = !(opt.isGiven("X"));
    check = (opt.isGiven("c"));
    onlycheck = (opt.isGiven("C"));
    dolcp = 0;
    if (opt.isGiven("l")) dolcp+=4;
    if (opt.isGiven("lcp2")) dolcp+=2;
    if (opt.isGiven("lcp1")) dolcp+=1;
    
    // Get indexname and di
    String indexname = args[0];
    String di        = g.dir + indexname;
    g.startplog(di+extlog);
    if (args.length>1) g.warnmsg("suffixtray: ignoring all arguments except first '%s'%n", args[0]);
    Properties prj = g.readProject(di+extprj);
    asize = Integer.parseInt(prj.getProperty("LargestSymbol")) + 1;

    // load alphabet map and text
    amap = g.readAlphabetMap(di+extalph);
    s = g.slurpByteArray(di+extseq);
    n = s.length;
    if (onlycheck) { returnvalue =  checkpos(di); g.stopplog(); return returnvalue; }
    prj.setProperty("SuffixAction", action);
    prj.setProperty("LastAction","suffixtray");
    prj.setProperty("AlphabetSize",Integer.toString(asize));    
    
    method = (opt.isGiven("m")? opt.get("m") : "rah1up"); // default method
    g.logmsg("suffixtray: constructing pos using method '%s'...%n", method);
    assert(amap.isSeparator(s[n-1])) : "last character in text needs to be a separator";
    TicToc timer = new TicToc();
    steps = 0;
    if (method.equals("rah1up")) buildpos_rah1up();
    else if (method.equals("rah1down")) buildpos_rah1down();
    else if (method.equals("rah2min")) buildpos_rah2min();
    else if (method.equals("rah2both")) buildpos_rah2both();
    else if (method.equals("rah2")) buildpos_rah2x();
    else g.terminate("suffixtray: Unsupported construction method '"+method+"'!");
    g.logmsg("suffixtray: pos completed after %.1f secs using %d steps (%.2f/char)%n",
        timer.tocs(), steps, (double)steps/n);
    prj.setProperty("SuffixTrayMethod",method);
    prj.setProperty("SuffixTraySteps",Long.toString(steps));    
    prj.setProperty("SuffixTrayStepsPerChar",Double.toString((double)steps/n));    
    
    if (check) {
      timer.tic();
      g.logmsg("suffixcheck: checking pos...%n");
      if (method.equals("rah1up")) returnvalue=checkpos_rah1();
      else if (method.equals("rah1down")) returnvalue=checkpos_rah1();
      else if (method.equals("rah2min")) returnvalue=checkpos_rah1();
      else if (method.equals("rah2both")) returnvalue=checkpos_rah1();
      else if (method.equals("rah2")) returnvalue=checkpos_rah2x();
      else g.terminate("suffixcheck: Unsupported construction method '"+method+"'!");
      if(returnvalue==0) g.logmsg("suffixcheck: pos looks OK!%n");
      g.logmsg("suffixcheck: done after %.1f secs%n", timer.tocs());
    }
    
    if (returnvalue==0) {
      timer.tic();
      String fpos = di+extpos;
      g.logmsg("suffixtray: writing '%s'...%n",fpos);
      if (method.equals("rah1up")) writepos_rah1(fpos);
      else if (method.equals("rah1down")) writepos_rah1(fpos);
      else if (method.equals("rah2min")) writepos_rah1(fpos);
      else if (method.equals("rah2both")) writepos_rah1(fpos);
      else if (method.equals("rah2")) writepos_rah2x(fpos);
      else g.terminate("suffixtray: Unsupported construction method '"+method+"'!");
      g.logmsg("suffixtray: writing took %.1f secs; done.%n", timer.tocs());
    }
    
    // do lcp if desired
    if (dolcp>0 && returnvalue==0) {
      timer.tic();
      String flcp = di+extlcp;
      g.logmsg("suffixtray: computing lcp array...%n");
      if (method.equals("rah1up")) lcp_rah1(flcp, dolcp);
      else if (method.equals("rah1down")) lcp_rah1(flcp, dolcp);
      else if (method.equals("rah2min")) lcp_rah1(flcp, dolcp);
      else if (method.equals("rah2both")) lcp_rah1(flcp, dolcp);
      else if (method.equals("rah2")) lcp_rah2x(flcp, dolcp);
      else g.terminate("suffixtray: Unsupported construction method '"+method+"'!");
      g.logmsg("suffixtray: lcp computation and writing took %.1f secs; done.%n", timer.tocs());
      prj.setProperty("lcp1Exceptions",Integer.toString(lcp1x));
      prj.setProperty("lcp2Exceptions",Integer.toString(lcp2x));
      prj.setProperty("lcp1Size",Integer.toString(n+8*lcp1x));
      prj.setProperty("lcp2Size",Integer.toString(2*n+8*lcp2x));
      prj.setProperty("lcp4Size",Integer.toString(4*n));
      prj.setProperty("lcpMax",Integer.toString(maxlcp));     
    }
    
    // write project data
    try { g.writeProject(prj, di+extprj); } 
    catch (IOException ex) { 
      g.warnmsg("qgram: could not write %s!%n", di+extprj); 
      g.terminate(1);
    }
    g.stopplog();
    return returnvalue;
  } // end run()
  
  
  /** builds suffix array 'pos' according to Rahmann's method
   * by walking up a partially constructed doubly linked suffix list.
   * Needs text 's' and its length 'n' correctly set.
   */
  public void buildpos_rah1up() {
    lexfirst = new int[256];
    lexlast  = new int[256];
    lexpred  = new int[n];
    lexsucc  = new int[n];
    Arrays.fill(lexfirst,-1);
    Arrays.fill(lexlast,-1);
    byte ch; int chi;
    
    for (int i=n-1; i>=0; i--) {
      // insert suffix starting at position i
      ch = s[i]; chi=ch+128;
      if (lexfirst[chi]==-1) {  // seeing character ch for the first time
        insertnew(chi,i);
        steps++;
      } else {                  // seeing character ch again
        assert(lexfirst[chi]>i);  assert(lexlast[chi]>i);
        if (amap.isSpecial(ch)) {    // special character: always inserted first
          insertasfirst(chi,i);
          steps++;
        } else {                     // symbol character: proceed normally
          int p = i+1;
          steps++;
          while(lexpred[p]!=-1 && s[(p=lexpred[p])-1]!=ch) {steps++; }
          p--;
          if (s[p]==ch && p!=i) {        // insert i after p, might be new last
            insertbetween(p,lexsucc[p],i);
            if (lexlast[chi]==p) lexlast[chi]=i;
          } else {                       // i is new first
            insertasfirst(chi,i);
          }
        } // end symbol character
      } // end seeing character ch again
      //DEBUG: showpos_rah1(String.format("List after step %d: [%d]%n", i, s[i]));
    } // end for i
  }
  
  
  /** builds suffix array 'pos' according to Rahmann's method
   * by walking DOWN a partially constructed doubly linked suffix list.
   * Needs text 's' and its length 'n' correctly set.
   */
  public void buildpos_rah1down() {
    lexfirst = new int[256];
    lexlast  = new int[256];
    lexpred  = new int[n];
    lexsucc  = new int[n];
    Arrays.fill(lexfirst,-1);
    Arrays.fill(lexlast,-1);
    byte ch; int chi;
    
    for (int i=n-1; i>=0; i--) {
      // insert suffix starting at position i
      ch = s[i]; chi=ch+128;
      if (lexfirst[chi]==-1) {  // seeing character ch for the first time
        insertnew(chi,i);
        steps++;
      } else {                  // seeing character ch again
        assert(lexfirst[chi]>i);  assert(lexlast[chi]>i);
        if (amap.isSpecial(ch)) {    // special character: always inserted first
          insertasfirst(chi,i);
          steps++;
        } else {                     // symbol character: proceed normally
          int p = i+1;
          steps++;
          while(lexsucc[p]!=-1 && s[(p=lexsucc[p])-1]!=ch) {steps++; }
          p--;
          if (s[p]==ch && p!=i) {        // insert i BEFORE p, might be new first
            insertbetween(lexpred[p],p,i);
            if (lexfirst[chi]==p) lexfirst[chi]=i;
          } else {                       // i is new LAST
            insertaslast(chi,i);
          }
        } // end symbol character
      } // end seeing character ch again
      //DEBUG:showpos_rah1(String.format("List after step %d: [%d]%n", i, s[i]));
    } // end for i
  }
  
  
  /** builds suffix array 'pos' according to Rahmann's method
   * by walking BIDIRECTIONALLY along a partially constructed doubly linked suffix list.
   * Needs text 's' and its length 'n' correctly set.
   */
  public void buildpos_rah2min() {
    lexfirst = new int[256];
    lexlast  = new int[256];
    lexpred  = new int[n];
    lexsucc  = new int[n];
    Arrays.fill(lexfirst,-1);
    Arrays.fill(lexlast,-1);
    byte ch; int chi;
    int pup, pdown;
    int lsp, lpp;
    int found=0;
    
    for (int i=n-1; i>=0; i--) {
      // insert suffix starting at position i
      ch = s[i]; chi=ch+128;
      if (lexfirst[chi]==-1) {  // seeing character ch for the first time
        insertnew(chi,i);
        steps++;
      } else {                  // seeing character ch again
        assert(lexfirst[chi]>i);  assert(lexlast[chi]>i);
        if (amap.isSpecial(ch)) {    // special character: always inserted first
          insertasfirst(chi,i);
          steps++;
        } else {                     // symbol character: proceed normally
          pup = pdown = i+1;
          for (found=0; found==0; ) {
            steps++;
            lpp = lexpred[pup];
            if (lpp==-1)              { found=1; break; } // new first
            if (s[(pup=lpp)-1]==ch)   { found=2; break; } // insert after pup
            steps++;
            lsp = lexsucc[pdown];
            if (lsp==-1)              { found=3; break; } // new last
            if (s[(pdown=lsp)-1]==ch) { found=4; break; } // insert before pdown
          }
          pup--; pdown--;
          switch (found) {
            case 1: insertasfirst(chi,i);  break;
            case 2: insertbetween(pup,lexsucc[pup],i); if (lexlast[chi]==pup) lexlast[chi]=i;  break;
            case 3: insertaslast(chi,i);   break;
            case 4: insertbetween(lexpred[pdown],pdown,i); if (lexfirst[chi]==pdown) lexfirst[chi]=i; break;
            default: g.terminate("suffixtray: internal error");
          } // end switch
        } // end symbol character
      } // end seeing character ch again
      //DEBUG:showpos_rah1(String.format("List after step %d: [%d]%n", i, s[i]));
    } // end for i
  }
  
  /** builds suffix array 'pos' according to Rahmann's method
   * by walking BOTH WAYS along a partially constructed doubly linked suffix list.
   * Needs text 's' and its length 'n' correctly set.
   */
  public void buildpos_rah2both() {
    lexfirst = new int[256];
    lexlast  = new int[256];
    lexpred  = new int[n];
    lexsucc  = new int[n];
    Arrays.fill(lexfirst,-1);
    Arrays.fill(lexlast,-1);
    byte ch; int chi;
    int pup, pdown;
    int lsp, lpp;
    int foundup=0;
    int founddown=0;
    
    for (int i=n-1; i>=0; i--) {
      // insert suffix starting at position i
      ch = s[i]; chi=ch+128;
      if (lexfirst[chi]==-1) {  // seeing character ch for the first time
        insertnew(chi,i);
        steps++;
      } else {                  // seeing character ch again
        assert(lexfirst[chi]>i);  assert(lexlast[chi]>i);
        if (amap.isSpecial(ch)) {    // special character: always inserted first
          insertasfirst(chi,i);
          steps++;
        } else {                     // symbol character: proceed normally
          pup = pdown = i+1;
          for (founddown=0, foundup=0;  founddown==0 || foundup==0; ) {
            if (founddown==0) {
              steps++;
              lsp = lexsucc[pdown];
              if (lsp==-1)              { founddown=1; foundup=2; break; } // new last
              if (s[(pdown=lsp)-1]==ch) { founddown=2; } // insert before pdown
            }
            if (foundup==0) {
              steps++;
              lpp = lexpred[pup];
              if (lpp==-1)              { foundup=1; founddown=2; break; } // new first
              if (s[(pup=lpp)-1]==ch)   { foundup=2; }   // insert after pup
            }
          }
          if (founddown==1) {       // new last
            pup=lexlast[chi];
            pdown=lexsucc[pup];
            lexlast[chi]=i;
          } else if (foundup==1) {  // new first
            pdown=lexfirst[chi];
            pup=lexpred[pdown];
            lexfirst[chi]=i;
          } else {
            pup--; pdown--;         // normal insert at found position
          }
          insertbetween(pup,pdown,i);
        } // end symbol character
      } // end seeing character ch again
      //DEBUG:showpos_rah1(String.format("List after step %d: [%d]%n", i, s[i]));
    } // end for i
  }
  
  //===================== insertion methods =================================
  
  
  /** insert i between p1 and p2 (they must be neighbors with p1<p2)
   *@param p1  position after which to insert
   *@param p2  position before which to insert
   *@param i  what to insert
   */
  private final void insertbetween(final int p1, final int p2, final int i) {
    // before: ... p1, p2 ...
    // after:  ... p1, i, p2 ...
    assert(p1==-1 || lexsucc[p1]==p2)
    : String.format("i=%d (of %d); p1=%d, p2=%d; s[i..i+1]=(%d,%d); lexsucc[p1]=%d %n",
        i,n,p1,p2,s[i],(i<n-1?s[i+1]:999),lexsucc[p1]);
    assert(p2==-1 || lexpred[p2]==p1)
    : String.format("i=%d (of %d); p1=%d, p2=%d; s[i..i+1]=(%d,%d); lexpred[p2]=%d %n",
        i,n,p1,p2,s[i],(i<n-1?s[i+1]:999),lexpred[p2]);
    lexpred[i] = p1;
    lexsucc[i] = p2;
    if (p2!=-1) lexpred[p2] = i;
    if (p1!=-1) lexsucc[p1] = i;
  }
  
  
  /** insert the new first occurrence of chi before p=lexfirst[chi].
   *@param chi  integer representation of character to insert
   *@param i  what to insert
   */
  private final void insertasfirst(final int chi, final int i) {
    final int p = lexfirst[chi];
    assert(p!=-1);
    insertbetween(lexpred[p],p,i);
    lexfirst[chi]=i;
  }
  
  /** insert the new last occurrence of chi before p=lexfirst[chi].
   *@param chi  integer representation of character to insert
   *@param i  what to insert
   */
  private final void insertaslast(final int chi, final int i) {
    final int p = lexlast[chi];
    assert(p!=-1);
    insertbetween(p,lexsucc[p],i);
    lexlast[chi]=i;
  }
  
  /** insert the first overall occurrence of a new character chi.
   *@param chi  integer representation of character to insert
   *@param i  what to insert
   */
  private final void insertnew(final int chi, final int i) {
    int cp, cs, ip, is;
    assert(lexfirst[chi]==-1);
    assert(lexlast[chi]==-1);
    lexfirst[chi]=i;
    lexlast[chi]=i;
    for(cp=chi-1; cp>=0 && lexlast[cp]==-1; cp--) {};
    ip = (cp>=0? lexlast[cp] : -1);
    for(cs=chi+1; cs<256 && lexfirst[cs]==-1; cs++) {};
    is = (cs<256? lexfirst[cs] : -1);
    // before: ... ip, is ...
    // after:  ... ip, i, is ...
    insertbetween(ip, is, i);
  }
  
 

  
// ========= SPACE SAVING construction ======================================
  
  /** lightweight doubly linked list with xor coding.
   * WARNING: uses lexfirst[] and lexlast[] from enclosing class!
   */
  final class LightDLL {
    private int pup, pdn, ppred, psucc;
    int[] lexps = null;
    
    /** constructor, initializes list of given capacity.
     *@param nn capacity
     */
    LightDLL(int nn) {
      lexps = new int[nn];
      pup=pdn=ppred=psucc=-1;
    }
    
    /** insert i between p1 and p2 (they must be neighbors with p1<p2)
     *@param p1  position after which to insert
     *@param p2  position before which to insert
     *@param i  what to insert
     */
    private final void insertbetweenx(final int p1, final int p2, final int i) {
      // before: ... p1, p2 ...
      // after:  ... p1, i, p2 ...
      lexps[i] = p1 ^ p2;
      if (p2!=-1) lexps[p2] ^= p1 ^ i;
      if (p1!=-1) lexps[p1] ^= p2 ^ i;
      ppred = p1;
      psucc = p2;
      pup = pdn = i;
    }
    
    /** insert the first overall occurrence of a new character chi.
     *@param chi  integer representation of character to insert
     *@param i  what to insert
     */
    private final void insertnewx(final int chi, final int i) {
      int cp, cs, ip, is;
      assert(lexfirst[chi]==-1);
      assert(lexlast[chi]==-1);
      lexfirst[chi]=i;
      lexlast[chi]=i;
      for(cp=chi-1; cp>=0 && lexlast[cp]==-1; cp--) {};
      ip = (cp>=0? lexlast[cp] : -1);
      for(cs=chi+1; cs<256 && lexfirst[cs]==-1; cs++) {};
      is = (cs<256? lexfirst[cs] : -1);
      insertbetweenx(ip, is, i);
    }
    
    private final void insertasfirstx(int chi, int i) {
      int cp, ip;
      assert(lexfirst[chi]!=-1);
      assert(lexlast[chi]!=-1);
      for(cp=chi-1; cp>=0 && lexlast[cp]==-1; cp--) {};
      ip = (cp>=0? lexlast[cp] : -1);
      insertbetweenx(ip,lexfirst[chi],i);
      lexfirst[chi]=i;      
    }

    private final void insertaslastx(int chi, int i) {
      int cs, is;
      assert(lexfirst[chi]!=-1);
      assert(lexlast[chi]!=-1);
      for(cs=chi+1; cs<256 && lexfirst[cs]==-1; cs++) {};
      is = (cs<256? lexfirst[cs] : -1);
      insertbetweenx(lexlast[chi],is,i);
      lexlast[chi]=i;      
    }
    
    private final void walkandinsert(int chi, int i) {
      final byte ch = (byte)(chi-128);
      int founddown, foundup, qdn, qup;
      // have pup, pdn from previous iteration!
      // now lexicographically (ppred < pup == pdn < psucc)
      for (founddown=0, foundup=0;  founddown==0 || foundup==0; ) {
        if (founddown==0) { // walk down
          steps++;
          qdn = lexps[pdn] ^ ppred;
          if (qdn==-1)      { insertaslastx(chi,i);  break; } // i is new last
          if (s[qdn-1]==ch) { founddown=2; } // insert before pdn
          ppred = pdn; pdn=qdn;
        }
        if (foundup==0) {   // walk up
          steps++;
          qup = lexps[pup] ^ psucc;
          if (qup==-1)      { insertasfirstx(chi,i); break; } // i is new first
          if (s[qup-1]==ch) { foundup=2; } // insert after pup
          psucc = pup; pup=qup;
        }
      }
      if (founddown!=0 && foundup!=0) {  // insert i at found position
        pup--; pdn--;    
        insertbetweenx(pup,pdn,i);
      }
    }  // end method walkandinsert
    
  } // end inner class
  
  LightDLL dll = null;
  
  /** builds suffix array 'pos' according to Rahmann's method
   * by walking BOTH WAYS along a partially constructed doubly linked suffix list,
   * using a SPACE SAVING trick.
   * Needs text 's' and its length 'n' correctly set.
   */
  public void buildpos_rah2x() {
    lexpred  = null;
    lexsucc  = null;
    lexfirst = new int[256];
    lexlast  = new int[256];
    Arrays.fill(lexfirst,-1);
    Arrays.fill(lexlast,-1);
    dll = new LightDLL(n);
    byte ch; 
    int chi;
    
    for (int i=n-1; i>=0; i--) {
      // insert suffix starting at position i
      ch = s[i]; chi=ch+128;
      if (lexfirst[chi]==-1) {  // seeing character ch for the first time
        dll.insertnewx(chi,i);
        steps++;
      } else {                  // seeing character ch again
        assert(lexfirst[chi]>i);  
        assert(lexlast[chi]>i);
        if (amap.isSpecial(ch)) {    // special character: always inserted first
          dll.insertasfirstx(chi,i);
          steps++;
        } else {                     // symbol character: proceed normally
          dll.walkandinsert(chi,i);
        } // end symbol character
      } // end seeing character ch again
      //DEBUG:showpos_rah1(String.format("List after step %d: [%d]%n", i, s[i]));
    } // end for i
  }

    
  
  // ==================== checking routines ====================================
  
  /** compare two characters of text s.
   * "Symbols" are compared according to their order in the alphabet map
   * "special" characters (wildcards, separators) are compared by position
   *@param i  first position
   *@param j  second position
   *@return any value < 0 iff s[i]<s[j], as specified by alphabet map,
   * zero(0) iff s[i]==s[j],
   * any value > 0 iff s[i]>s[j], as specified by alphabet map.
   */
  public final int scmp(final int i, final int j) {
    final int d = s[i]-s[j];
    if (d!=0 || amap.isSymbol(s[i])) return d;
    return i-j;
  }
  
  /** compare two suffixes of text s.
   * "Symbols" are compared according to their order in the alphabet map
   * "special" characters (wildcards, separators) are compared by position
   *@param i  first position
   *@param j  second position
   *@return any value < 0 iff suffix(i)<suffix(j) lexicographically,
   * zero (0) iff i==j
   * any value > 0 iff suffix(i)>suffix(j) lexicographically.
   */
  public final int suffixcmp(final int i, final int j) {
    if (i==j) return 0;
    int off, c;
    for(off=0; (c=scmp(i+off,j+off))==0; off++) {};
    // suffixes i and j disagree at offset off, thus have off characters in common
   return c;
  }
  
  /** find length of longest common prefix (lcp) of suffixes of text s,
   * given prior knowledge that lcp >= h.
   *@param i  first position
   *@param j  second position
   *@param h  minimum known lcp length
   *@return  lcp length
   */
  public final int suffixlcp(final int i, final int j, final int h) {
    if (i==j) return n-i+1;
    int off;
    for(off=h; scmp(i+off,j+off)==0; off++) {};
    return off;
  }
  
 
  
  /** check correctnes of a suffix array constructed with Rahmann's method;
   * output warning messages if errors are found.
   *@return  0 on success, 1 on sorting error, 2 on count error, 3 on both errors.
   */
  public int checkpos_rah1() {
    int chi, p, nextp, nn, comp;
    int returnvalue = 0;
    for (chi=0; chi<256 && lexfirst[chi]==-1; chi++)  {};
    if (chi>=256) {
      if(n==0) return 0;
      g.warnmsg("suffixcheck: no first character found, but |s|!=0.%n");
      return 2;
    }
    p = lexfirst[chi]; assert(p!=-1);
    nextp = lexsucc[p]; nn=1;
    while (nextp!=-1) {
      //g.logmsg("  pos %d vs %d; text %d vs %d%n", p, nextp, s[p], s[nextp]);
      if (!((comp=suffixcmp(p,nextp))<0)) {
        g.warnmsg("suffixcheck: sorting error at ranks %d, %d; pos %d, %d; text %d, %d; cmp %d%n",
            nn-1,nn, p,nextp, s[p], s[nextp], comp);
        returnvalue=1;
      }
      p=nextp;
      nextp = lexsucc[p];
      nn++;
    }
    if (nn!=n) {
      g.warnmsg("suffixcheck: missing some suffixes; have %d / %d.%n",nn,n);
      returnvalue += 2;
    }
    return returnvalue;
  }
  
  /** check correctnes of a suffix array constructed with Rahmann's method
   * using the SPACE SAVING technique.
   * output warning messages if errors are found.
   *@return  0 on success, 1 on sorting error, 2 on count error, 3 on both errors.
   */  
    public int checkpos_rah2x() {
    int chi, p, oldp, nextp, nn, comp;
    int returnvalue = 0;
    final int[] lexps = dll.lexps;
    for (chi=0; chi<256 && lexfirst[chi]==-1; chi++)  {};
    if (chi>=256) {
      if(n==0) return 0;
      g.warnmsg("suffixcheck: no first character found, but |s|!=0.%n");
      return 2;
    }
    nn=1;
    p = lexfirst[chi]; 
    assert(p!=-1);
    nextp = lexps[p] ^ -1; 
    while (nextp!=-1) {
      //g.logmsg("  pos %d vs %d; text %d vs %d%n", p, nextp, s[p], s[nextp]);
      if (!((comp=suffixcmp(p,nextp))<0)) {
        g.warnmsg("suffixcheck: sorting error at ranks %d, %d; pos %d, %d; text %d, %d; cmp %d%n",
            nn-1,nn, p,nextp, s[p], s[nextp], comp);
        returnvalue=1;
      }
      oldp=p;
      p=nextp;
      nextp = lexps[p] ^ oldp;
      nn++;
    }
    if (nn!=n) {
      g.warnmsg("suffixcheck: missing some suffixes; have %d / %d.%n",nn,n);
      returnvalue += 2;
    }
    return returnvalue;
  }
 
  /** check correctnes of a suffix array on disk
   * outputs warning messages if errors are found
   *@param index  path and name of the index to check
   *@return  0 on success, 1 on sorting error, 2 on count error
   */
  public int checkpos(String di) {
    ArrayFile fpos = new ArrayFile(di+extpos);
    IntBuffer pos = null;
    int returnvalue = 0;
    TicToc ctimer = new TicToc();
    g.logmsg("suffixcheck: checking pos...%n");
    try {
      pos = fpos.mapR().asIntBuffer();
    } catch (IOException ex) {
      g.terminate("suffixcheck: could not read .pos file");
    }
    int p = pos.get();
    int nextp, comp;
    int nn=1;
    while (true) {
      try {
        nextp = pos.get();
      } catch (BufferUnderflowException ex) {
        break;
      }
      if (!((comp=suffixcmp(p,nextp))<0)) {
        g.warnmsg("suffixcheck: sorting error at ranks %d, %d; pos %d, %d; text %d, %d; cmp %d%n",
            nn-1,nn, p,nextp, s[p], s[nextp], comp);
        returnvalue=1;
      }
      nn++; p=nextp;
    }
    if (nn!=n) {
      g.warnmsg("suffixcheck: missing some suffixes; have %d / %d.%n",nn,n);
      returnvalue += 2;
    }
    if (returnvalue==0) g.logmsg("suffixcheck: pos seems OK!%n");
    g.logmsg("suffixcheck: checking took %.2f secs.%n", ctimer.tocs());
    return returnvalue;      
  }  
  
  
// ==================== writing routines ==================================
  
  
  /** write pos array to file after Rahmann's construction
   *@param fname  the full path and file name
   */
  private void writepos_rah1(String fname) {
    int chi, p;
    for (chi=0; chi<256 && lexfirst[chi]==-1; chi++)  {};
    try {
      ArrayFile f = new ArrayFile(fname).openW();
      for (p=lexfirst[chi]; p!=-1; p=lexsucc[p])
        f.out().writeInt(p);
      f.close();
    } catch (IOException ex) {
      g.warnmsg("suffixtray: error writing '%s'!%n",fname);
      g.terminate(1);
    }
  }
 
  /** write pos array to file after Rahmann's construction
   *@param fname  the full path and file name
   */
  private void writepos_rah2x(String fname) {
    int chi, p, oldp, tmp;
    final int[] lexps = dll.lexps;
    for (chi=0; chi<256 && lexfirst[chi]==-1; chi++)  {};
    try {
      ArrayFile f = new ArrayFile(fname).openW();
      for (oldp=-1, p=lexfirst[chi];  p!=-1;  ) {
        f.out().writeInt(p);
        tmp = p;
        p = lexps[p] ^ oldp;
        oldp = tmp;
      }
      f.close();
    } catch (IOException ex) {
      g.warnmsg("suffixtray: error writing '%s'!%n",fname);
      g.terminate(1);
    }
  }
  
  /** write partial pos array to logfile during Rahmann's construction
   *@param header  a header string to print
   */
  private void showpos_rah1(String header) {
    int chi, p;
    System.out.printf(header);
    for (chi=0; chi<256; chi++) {
      assert(lexfirst[chi]==-1 || lexlast[chi]!=-1);
      if (lexfirst[chi]!=-1)
        System.out.printf("  char %d: lexfirst=%2d, lexlast=%2d%n", chi-128, lexfirst[chi], lexlast[chi]);
    }
    for (chi=0; chi<256 && lexfirst[chi]==-1; chi++)  {};
    for (p=lexfirst[chi]; p!=-1; p=lexsucc[p])
      System.out.printf("  %2d: %d...%n", p, s[p]);
  }
  
// ==================== lcp routines ==================================

  /** lcp computation according to Kasai et al.'s algorithm
   * after Rahmann's suffix array construction.
   *@param fname filename for lcp array
   *@param dolcp which lcp arrays to compute (1+2+4)
   */
  private void lcp_rah1(String fname, int dolcp) {
    TicToc timer = new TicToc();
    int p, prev, h;
    h=0;
    for (p=0; p<n; p++) {
      prev = lexpred[p];
      if (prev!=-1)  h = suffixlcp(prev,p,h); 
      else {assert(h==0);}
      assert(h>=0);
      if(h>maxlcp) maxlcp=h;
      lexpred[p]=h;
      if (h>=255)   lcp1x++;
      if (h>=65535) lcp2x++;    
      if (h>0) h--;
    }
    g.logmsg("suffixtray: lcp computation took %.2f secs; writing...%n",timer.tocs());

    int chi, r;
    for (chi=0; chi<256 && lexfirst[chi]==-1; chi++)  {};
    ArrayFile f4=null, f2=null, f1=null, f2x=null, f1x=null;
    try {
      if ((dolcp&4)!=0) f4 = new ArrayFile(fname).openW();
      if ((dolcp&2)!=0) { f2 = new ArrayFile(fname+"2").openW(); f2x = new ArrayFile(fname+"2x").openW(); }
      if ((dolcp&1)!=0) { f1 = new ArrayFile(fname+"1").openW(); f1x = new ArrayFile(fname+"1x").openW(); }
      for (r=0, p=lexfirst[chi];   p!=-1;   p=lexsucc[p], r++) {
        h = lexpred[p];
        assert(h>=0);
        if ((dolcp&4)!=0) { f4.out().writeInt(h); }
        if ((dolcp&2)!=0) {
          if (h>=65535) { f2.out().writeShort(-1); f2x.out().writeInt(r); f2x.out().writeInt(h); }
          else f2.out().writeShort(h);
        }
        if ((dolcp&1)!=0) {
          if (h>=255)   { f1.out().writeByte(-1);  f1x.out().writeInt(r); f1x.out().writeInt(h); }
          else f1.out().writeByte(h);
        } 
      }
      assert(r==n);
      if ((dolcp&4)!=0) f4.close();
      if ((dolcp&2)!=0) { f2.close(); f2x.close(); }
      if ((dolcp&1)!=0) { f1.close(); f1x.close(); }
    } catch (IOException ex) {
      g.warnmsg("suffixtray: error writing lcp file(s): %s!%n",ex.toString());
      g.terminate(1);
    }
  }

  /** lcp computation according to Kasai et al.'s algorithm
   * after Rahmann's suffix array construction 
   * using the SPACE SAVING technique.
   *@param fname filename for lcp array
   *@param dolcp which lcp arrays to compute (1+2+4)
   */
  private void lcp_rah2x(String fname, int dolcp) {
    throw new UnsupportedOperationException("Not yet implemented");
  }
    
  
  
}
