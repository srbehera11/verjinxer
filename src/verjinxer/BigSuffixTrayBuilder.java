/*
 * BigSuffixTrayBuilder.java
 *
 * Created on 22.08.2008
 */

package verjinxer;

import static verjinxer.Globals.programname;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.LongBuffer;
import java.util.Arrays;

import com.spinn3r.log5j.Logger;

import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.util.ArrayFile;
import verjinxer.util.ArrayUtils;
import verjinxer.util.HugeByteArray;
import verjinxer.util.HugeLongArray;
import verjinxer.util.IllegalOptionException;
import verjinxer.util.Options;
import verjinxer.util.ProjectInfo;
import verjinxer.util.StringUtils;
import verjinxer.util.TicToc;

/**
 *
 * @author Sven Rahmann
 */
public class BigSuffixTrayBuilder {
  private static final Logger log = Globals.getLogger();
  private Globals g;
  
  /** Creates a new instance of SuffixTrayBuilder
   * @param gl global configuration structure
   */
  public BigSuffixTrayBuilder(Globals gl) {
    g = gl;
  }
  
  /**
   * print help on usage
   */
  public void help() {
    log.info("Usage:%n  %s bigsuffix [options] Indexnames...%n", programname);
    log.info("Builds the 64-bit suffix tray of a .seq file;%n");
    log.info("writes %s, %s (incl. variants 1,1x,2,2x).%n", FileNameExtensions.pos, FileNameExtensions.lcp);
    log.info("Options:%n");
    log.info("  -m, --method  <id>    select construction method, where <id> is one of:%n");
    log.info("      L%n" +
        "      R%n" +
        "      minLR%n" +
        "      bothLR%n" +
        "      bothLR2%n"
        );
    log.info("  -l, --lcp[2|1]        build lcp array using int|short|byte%n");
    log.info("  -c, --check           additionally check index integrity%n");
    log.info("  -C, --onlycheck       ONLY check index integrity%n");
    log.info("  -X, --notexternal     DON'T save memory at the cost of lower speed%n");
  }
  
  /** if run independently, call main
   * @param args command line arguments
   */
  public static void main(String[] args) {
    new SuffixTrayBuilder(new Globals()).run(args);
  }
  
  
  boolean check     = false;
  boolean onlycheck = false;
  boolean external  = true;
  String method     = null;
  int dolcp         = 0;
  
  int asize         = -1;
  Alphabet alphabet  = null;
  HugeByteArray s   = null;   // text
  long n            = -1;     // length of text
  long[] lexfirstpos= null;   // indexed by character
  long[] lexlastpos = null;   // indexed by character
  HugeLongArray lexprevpos    = null;
  HugeLongArray lexnextpos    = null;
  long steps       = 0;
  
  long lcp1x        = 0;      // lcp1 exceptions
  long lcp2x        = 0;      // lcp2 exceptions
  long lcp4x        = 0;      // lcp4 exceptions
  long maxlcp       = -1;
  
  /**
   * @param args the command line arguments
   * @return zero on success, non-zero on failure
   */
  public int run(String[] args) {
    g.cmdname = "suffixtray";
    int returnvalue = 0;
    String action = "suffixtray \"" + StringUtils.join("\" \"",args)+ "\"";
    
    Options opt = new Options("c=check,C=onlycheck,X=notexternal=nox=noexternal,m=method:,l=lcp=lcp4,lcp1,lcp2");
    try {
      args = opt.parse(args);
    } catch (IllegalOptionException ex) { log.error("suffixtray: "+ex); g.terminate(1); }
    if (args.length==0) { help(); log.error("suffixtray: no index given%n"); g.terminate(0); }
    
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
    if (args.length > 1) log.warn("suffixtray: ignoring all arguments except first '%s'%n", args[0]);
    ProjectInfo project;
    String projectname = g.dir + indexname;
    try {
       project = ProjectInfo.createFromFile(projectname);
    } catch (IOException ex) {
       log.error("could not read project file: %s%n", ex);
       return 1;
    }
    g.startProjectLogging(project);
    asize = project.getIntProperty("LargestSymbol") + 1;

    // load alphabet map and text
    alphabet = g.readAlphabet(projectname+FileNameExtensions.alphabet);
    s = g.slurpHugeByteArray(projectname+FileNameExtensions.seq);
    n = s.length;
    if (onlycheck) { returnvalue =  checkpos(projectname); g.stopplog(); return returnvalue; }
    project.setProperty("SuffixAction", action);
    project.setProperty("LastAction", "suffixtray");
    project.setProperty("AlphabetSize", asize);    
    
    method = (opt.isGiven("m")? opt.get("m") : "L"); // default method
    log.info("suffixtray: constructing pos using method '%s'...%n", method);
    assert(alphabet.isSeparator(s.get(n-1))) : "last character in text needs to be a separator";
    TicToc timer = new TicToc();
    steps = 0;
    if (method.equals("L"))            buildpos_L();
    else if (method.equals("R"))       buildpos_R();
    else if (method.equals("minLR"))   buildpos_minLR(false);
    else if (method.equals("bothLR"))  buildpos_bothLR();
    else if (method.equals("bothLR2")) buildpos_bothLR2();
    else { log.error("suffixtray: Unsupported construction method '"+method+"'!"); return 1; }
    log.info("suffixtray: pos completed after %.1f secs using %d steps (%.2f/char)%n",
        timer.tocs(), steps, (double)steps/n);
    project.setProperty("SuffixTrayMethod", method);
    project.setProperty("SuffixTraySteps", steps);    
    project.setProperty("SuffixTrayStepsPerChar", (double)steps/n);    
    
    if (check) {
      timer.tic();
      log.info("suffixcheck: checking pos...%n");
      if (method.equals("L"))            returnvalue=checkpos_R();
      else if (method.equals("R"))       returnvalue=checkpos_R();
      else if (method.equals("minLR"))   returnvalue=checkpos_R();
      else if (method.equals("bothLR"))  returnvalue=checkpos_bothLR();
      else if (method.equals("bothLR2")) returnvalue=checkpos_R();
      else { log.error("suffixcheck: Unsupported construction method '"+method+"'!"); return 1; }
      if(returnvalue==0) log.info("suffixcheck: pos looks OK!%n");
      log.info("suffixcheck: done after %.1f secs%n", timer.tocs());
    }
    
    if (returnvalue==0) {
      timer.tic();
      String fpos = projectname+FileNameExtensions.pos;
      log.info("suffixtray: writing '%s'...%n",fpos);
      if (method.equals("L"))            writepos_R(fpos);
      else if (method.equals("R"))       writepos_R(fpos);
      else if (method.equals("minLR"))   writepos_R(fpos);
      else if (method.equals("bothLR"))  writepos_bothLR(fpos);
      else if (method.equals("bothLR2")) writepos_R(fpos);
      else { log.error("suffixtray: Unsupported construction method '"+method+"'!"); return 1; }
      log.info("suffixtray: writing took %.1f secs; done.%n", timer.tocs());
    }
    
    // do lcp if desired
    if (dolcp>0 && returnvalue==0) {
      timer.tic();
      String flcp = projectname+FileNameExtensions.lcp;
      log.info("suffixtray: computing lcp array...%n");
      if (method.equals("L"))            lcp_L(flcp, dolcp);
      else if (method.equals("R"))       lcp_L(flcp, dolcp);
      else if (method.equals("minLR"))   lcp_L(flcp, dolcp);
      else if (method.equals("bothLR"))  lcp_bothLR(flcp, dolcp);
      else if (method.equals("bothLR2")) lcp_L(flcp, dolcp);
      else { log.error("suffixtray: Unsupported construction method '"+method+"'!"); return 1;}
      log.info("suffixtray: lcp computation and writing took %.1f secs; done.%n", timer.tocs());
      project.setProperty("lcp1Exceptions", lcp1x);
      project.setProperty("lcp2Exceptions", lcp2x);
      project.setProperty("lcp1Size", n+8*lcp1x);
      project.setProperty("lcp2Size", 2*n+8*lcp2x);
      project.setProperty("lcp4Size", 4*n);
      project.setProperty("lcpMax", maxlcp);     
    }
    
    // write project data
    try { 
       project.store(); } 
    catch (IOException ex) { 
      log.error("suffix: could not write %s (%s)!%n", project.getFileName(), ex); 
      g.terminate(1);
    }
    g.stopplog();
    return returnvalue;
  } // end run()
  
  
  /** builds suffix array 'pos' 
   * by walking LEFT along a partially constructed doubly linked suffix list.
   * Needs text 's' and its length 'n' correctly set.
   */
  public void buildpos_L() {
    lexfirstpos = new long[256];
    lexlastpos  = new long[256];
    lexprevpos  = new HugeLongArray(n);
    lexnextpos  = new HugeLongArray(n);
    Arrays.fill(lexfirstpos,-1L);
    Arrays.fill(lexlastpos,-1L);
    byte ch; int chi;
    
    for (long p=n-1; p>=0; p--) {
      // insert suffix starting at position p
      ch = s.get(p); chi=ch+128;
      if (lexfirstpos[chi]==-1) {    // seeing character ch for the first time
        insertnew(chi,p);
        steps++;
      } else {                    // seeing character ch again
        assert(lexfirstpos[chi]>p);  
        assert(lexlastpos[chi]>p);
        if (alphabet.isSpecial(ch)) {   // special character: always inserted first
          insertasfirst(chi,p);
          steps++;
        } else {                    // symbol character: proceed normally
          long i = p+1;
          steps++;
          while(lexprevpos.get(i)!=-1 && s.get((i=lexprevpos.get(i))-1)!=ch) {steps++; }
          i--;
          if (s.get(i)==ch && i!=p) { // insert p after i, might be new last
            insertbetween(i,lexnextpos.get(i),p);
            if (lexlastpos[chi]==i) lexlastpos[chi]=p;
          } else {                  // p is new first
            insertasfirst(chi,p);
          }
        } // end symbol character
      } // end seeing character ch again
      // showpos_R(String.format("List after step %d: [%d]%n", p, s[p])); // DEBUG
    } // end for p
  }
  
  
  /** builds suffix array 'pos' 
   * by walking RIGHT in a partially constructed doubly linked suffix list.
   * Needs text 's' and its length 'n' correctly set.
   */
  public void buildpos_R() {
    lexfirstpos = new long[256];
    lexlastpos  = new long[256];
    lexprevpos  = new HugeLongArray(n);
    lexnextpos  = new HugeLongArray(n);
    Arrays.fill(lexfirstpos,-1L);
    Arrays.fill(lexlastpos,-1L);
    byte ch; int chi;
    
    for (long p=n-1; p>=0; p--) {
      // insert suffix starting at position p
      ch = s.get(p); chi=ch+128;
      if (lexfirstpos[chi]==-1) {  // seeing character ch for the first time
        insertnew(chi,p);
        steps++;
      } else {                  // seeing character ch again
        assert(lexfirstpos[chi]>p);  assert(lexlastpos[chi]>p);
        if (alphabet.isSpecial(ch)) {    // special character: always inserted first
          insertasfirst(chi,p);
          steps++;
        } else {                     // symbol character: proceed normally
          long i = p+1;
          steps++;
          while(lexnextpos.get(i)!=-1 && s.get((i=lexnextpos.get(i))-1)!=ch) {steps++; }
          i--;
          if (s.get(i)==ch && i!=p) {        // insert p BEFORE i, might be new first
            insertbetween(lexprevpos.get(i),i,p);
            if (lexfirstpos[chi]==i) lexfirstpos[chi]=p;
          } else {                       // p is new LAST
            insertaslast(chi,p);
          }
        } // end symbol character
      } // end seeing character ch again
      // showpos_R(String.format("List after step %d: [%d]%n", p, s[p])); // DEBUG
    } // end for p
  }
  
  
  /** builds suffix array 'pos' 
   * by walking BIDIRECTIONALLY along a partially constructed doubly linked suffix list
   * until the first matching character is found in EITHER direction.
   * Needs text 's' and its length 'n' correctly set.
   * @param show  write intermediate results to stdout?
   */
  public void buildpos_minLR(final boolean show) {
    lexfirstpos = new long[256];
    lexlastpos  = new long[256];
    lexprevpos  = new HugeLongArray(n);
    lexnextpos  = new HugeLongArray(n);
    Arrays.fill(lexfirstpos,-1L);
    Arrays.fill(lexlastpos,-1L);
    byte ch; int chi;
    long pup, pdown;
    long lsp, lpp;
    int found=0;
    
    for (long p=n-1; p>=0; p--) {
      // insert suffix starting at position p
      ch = s.get(p); chi=ch+128;
      if (lexfirstpos[chi]==-1) {  // seeing character ch for the first time
        insertnew(chi,p);
        steps++;
      } else {                  // seeing character ch again
        assert(lexfirstpos[chi]>p);  assert(lexlastpos[chi]>p);
        if (alphabet.isSpecial(ch)) {    // special character: always inserted first
          insertasfirst(chi,p);
          steps++;
        } else {                     // symbol character: proceed normally
          pup = pdown = p+1;
          for (found=0; found==0; ) {
            steps++;
            lpp = lexprevpos.get(pup);
            if (lpp==-1)                { found=1; break; } // new first
            if (s.get((pup=lpp)-1)==ch) { found=2; break; } // insert after pup
            steps++;
            lsp = lexnextpos.get(pdown);
            if (lsp==-1)                  { found=3; break; } // new last
            if (s.get((pdown=lsp)-1)==ch) { found=4; break; } // insert before pdown
          }
          pup--; pdown--;
          switch (found) {
            case 1: insertasfirst(chi,p);  
                    break;
            case 2: insertbetween(pup,lexnextpos.get(pup),p); 
                    if (lexlastpos[chi]==pup) lexlastpos[chi]=p;  
                    break;
            case 3: insertaslast(chi,p);   
                    break;
            case 4: insertbetween(lexprevpos.get(pdown),pdown,p); 
                    if (lexfirstpos[chi]==pdown) lexfirstpos[chi]=p; 
                    break;
            default: log.error("suffixtray: internal error"); g.terminate(1);
          } // end switch
        } // end symbol character
      } // end seeing character ch again
      if (show) showall_R(String.format("List after step %d: [%d]%n", p, s.get(p))); // DEBUG
    } // end for p
  }
  
  
  /** builds suffix array 'pos' 
   * by walking BOTH WAYS along a partially constructed doubly linked suffix list,
   * until the target character is found BOTH WAYS.
   * This implementation uses 2 integer arrays.
   * The SPACE SAVING technique (xor encoding) is not used here.
   * Use <code>buildpos_bothLR</code> for the space saving technique.
   * Needs text 's' and its length 'n' correctly set.
   */
  public void buildpos_bothLR2() {
    lexfirstpos = new long[256];
    lexlastpos  = new long[256];
    lexprevpos  = new HugeLongArray(n);
    lexnextpos  = new HugeLongArray(n);
    Arrays.fill(lexfirstpos,-1L);
    Arrays.fill(lexlastpos,-1L);
    byte ch; int chi;
    long pup, pdown;
    long lsp, lpp;
    int foundup=0;
    int founddown=0;
    
    for (long p=n-1; p>=0; p--) {
      // insert suffix starting at position p
      ch = s.get(p); chi=ch+128;
      if (lexfirstpos[chi]==-1) {  // seeing character ch for the first time
        insertnew(chi,p);
        steps++;
      } else {                  // seeing character ch again
        assert(lexfirstpos[chi]>p);  assert(lexlastpos[chi]>p);
        if (alphabet.isSpecial(ch)) {    // special character: always inserted first
          insertasfirst(chi,p);
          steps++;
        } else {                     // symbol character: proceed normally
          pup = pdown = p+1;
          for (founddown=0, foundup=0;  founddown==0 || foundup==0; ) {
            if (founddown==0) {
              steps++;
              lsp = lexnextpos.get(pdown);
              if (lsp==-1)                  { founddown=1; foundup=2; break; } // new last
              if (s.get((pdown=lsp)-1)==ch) { founddown=2; } // insert before pdown
            }
            if (foundup==0) {
              steps++;
              lpp = lexprevpos.get(pup);
              if (lpp==-1)                  { foundup=1; founddown=2; break; } // new first
              if (s.get((pup=lpp)-1)==ch)   { foundup=2; }   // insert after pup
            }
          }
          if (founddown==1) {       // new last
            pup=lexlastpos[chi];
            pdown=lexnextpos.get(pup);
            lexlastpos[chi]=p;
          } else if (foundup==1) {  // new first
            pdown=lexfirstpos[chi];
            pup=lexprevpos.get(pdown);
            lexfirstpos[chi]=p;
          } else {
            pup--; pdown--;         // normal insert at found position
          }
          insertbetween(pup,pdown,p);
        } // end symbol character
      } // end seeing character ch again
      // showpos_R(String.format("List after step %d: [%d]%n", p, s[p])); // DEBUG
    } // end for p
  }
  
  //===================== insertion methods =================================
  
  
  /** insert i between p1 and p2 (they must be neighbors with p1 &lt; p2)
   *@param p1  position after which to insert
   *@param p2  position before which to insert
   *@param i  what to insert
   */
  private final void insertbetween(final long p1, final long p2, final long i) {
    // before: ... p1, p2 ...
    // after:  ... p1, i, p2 ...
    assert(p1==-1 || lexnextpos.get(p1)==p2)
    : String.format("i=%d (of %d); p1=%d, p2=%d; s[i..i+1]=(%d,%d); lexsucc[p1]=%d %n",
        i,n,p1,p2,s.get(i),(i<n-1?s.get(i+1):999),lexnextpos.get(p1));
    assert(p2==-1 || lexprevpos.get(p2)==p1)
    : String.format("i=%d (of %d); p1=%d, p2=%d; s[i..i+1]=(%d,%d); lexpred[p2]=%d %n",
        i,n,p1,p2,s.get(i),(i<n-1?s.get(i+1):999),lexprevpos.get(p2));
    lexprevpos.set(i,p1);
    lexnextpos.set(i,p2);
    if (p2!=-1) lexprevpos.set(p2,i);
    if (p1!=-1) lexnextpos.set(p1,i);
  }
  
  
  /** insert the new first occurrence of chi before p=lexfirst[chi].
   *@param chi  integer representation of character to insert
   *@param i  what to insert
   */
  private final void insertasfirst(final int chi, final long i) {
    final long p = lexfirstpos[chi];
    assert(p!=-1);
    insertbetween(lexprevpos.get(p),p,i);
    lexfirstpos[chi]=i;
  }
  
  /** insert the new last occurrence of chi before p=lexfirst[chi].
   *@param chi  integer representation of character to insert
   *@param i  what to insert
   */
  private final void insertaslast(final int chi, final long i) {
    final long p = lexlastpos[chi];
    assert(p!=-1);
    insertbetween(p,lexnextpos.get(p),i);
    lexlastpos[chi]=i;
  }
  
  /** insert the first overall occurrence of a new character chi.
   *@param chi  integer representation of character to insert
   *@param i  what to insert
   */
  private final void insertnew(final int chi, final long i) {
    int  cp, cs;
    long ip, is;
    assert(lexfirstpos[chi]==-1);
    assert(lexlastpos[chi]==-1);
    lexfirstpos[chi]=i;
    lexlastpos[chi]=i;
    for(cp=chi-1; cp>=0 && lexlastpos[cp]==-1; cp--) {}
    ip = (cp>=0? lexlastpos[cp] : -1);
    for(cs=chi+1; cs<256 && lexfirstpos[cs]==-1; cs++) {}
    is = (cs<256? lexfirstpos[cs] : -1);
    // before: ... ip, is ...
    // after:  ... ip, i, is ...
    insertbetween(ip, is, i);
  }
  
 

  
// ========= SPACE SAVING construction ======================================
  
  /** lightweight doubly linked list with xor coding.
   * NOTE: 
   * uses lexfirstpos[] and lexlastpos[] from enclosing class!
   * does not use lexprevpos[] or lexnextpos[]!
   */
  final class LightDLL {
    private long pup, pdn, ppred, psucc;
    HugeLongArray lexps = null;
    
    /** constructor, initializes list of given capacity.
     *@param nn capacity
     */
    LightDLL(final long nn) {
      lexps = new HugeLongArray(nn);
      pup=pdn=ppred=psucc=-1L;
    }
    
    /** insert i between p1 and p2 (they must be neighbors with p1 &lt; p2)
     *@param p1  position after which to insert
     *@param p2  position before which to insert
     *@param i  what to insert
     */
    private final void insertbetweenx(final long p1, final long p2, final long i) {
      // before: ... p1, p2 ...
      // after:  ... p1, i, p2 ...
      lexps.set(i, p1 ^ p2);
      if (p2!=-1L) lexps.set(p2, lexps.get(p2) ^ p1 ^ i);
      if (p1!=-1L) lexps.set(p1, lexps.get(p1) ^ p2 ^ i);
      ppred = p1;
      psucc = p2;
      pup = pdn = i;
    }
    
    /** insert the first overall occurrence of a new character chi.
     *@param chi  integer representation of character to insert
     *@param i  what to insert
     */
    private final void insertnewx(final int chi, final long i) {
      int cp, cs;
      long ip, is;
      assert(lexfirstpos[chi]==-1);
      assert(lexlastpos[chi]==-1);
      lexfirstpos[chi]=i;
      lexlastpos[chi]=i;
      for(cp=chi-1; cp>=0 && lexlastpos[cp]==-1; cp--) {}
      ip = (cp>=0? lexlastpos[cp] : -1);
      for(cs=chi+1; cs<256 && lexfirstpos[cs]==-1; cs++) {}
      is = (cs<256? lexfirstpos[cs] : -1);
      insertbetweenx(ip, is, i);
    }
    
    private final void insertasfirstx(final int chi, final long i) {
      int cp;
      assert(lexfirstpos[chi]!=-1);
      assert(lexlastpos[chi]!=-1);
      for(cp=chi-1; cp>=0 && lexlastpos[cp]==-1; cp--) {}
      final long ip = (cp>=0? lexlastpos[cp] : -1);
      insertbetweenx(ip,lexfirstpos[chi],i);
      lexfirstpos[chi]=i;      
    }

    private final void insertaslastx(final int chi, final long i) {
      int cs;
      assert(lexfirstpos[chi]!=-1);
      assert(lexlastpos[chi]!=-1);
      for(cs=chi+1; cs<256 && lexfirstpos[cs]==-1; cs++) {}
      final long is = (cs<256? lexfirstpos[cs] : -1);
      insertbetweenx(lexlastpos[chi],is,i);
      lexlastpos[chi]=i;      
    }
    
    private final void walkandinsert(final int chi, final long i) {
      final byte ch = (byte)(chi-128);
      int founddown, foundup;
      long qdn, qup;
      // have pup, pdn from previous iteration!
      // now lexicographically (ppred < pup == pdn < psucc)
      for (founddown=0, foundup=0;  founddown==0 || foundup==0; ) {
        if (founddown==0) { // walk down
          steps++;
          qdn = lexps.get(pdn) ^ ppred;
          if (qdn==-1L)         { insertaslastx(chi,i);  break; } // i is new last
          if (s.get(qdn-1)==ch) { founddown=2; } // insert before pdn
          ppred = pdn; pdn=qdn;
        }
        if (foundup==0) {   // walk up
          steps++;
          qup = lexps.get(pup) ^ psucc;
          if (qup==-1L)         { insertasfirstx(chi,i); break; } // i is new first
          if (s.get(qup-1)==ch) { foundup=2; } // insert after pup
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
 
  
  /** builds suffix array 'pos'
   * by walking BOTH WAYS along a partially constructed doubly linked suffix list,
   * until the target character is found BOTH WAYS.
   * This implementation uses the SPACE SAVING xor trick. 
   * Needs text 's' and its length 'n' correctly set.
   */
  public void buildpos_bothLR() {
    lexprevpos  = null;
    lexnextpos  = null;
    lexfirstpos = new long[256];
    lexlastpos  = new long[256];
    Arrays.fill(lexfirstpos,-1L);
    Arrays.fill(lexlastpos,-1L);
    dll = new LightDLL(n);
    byte ch; 
    int chi;
    
    for (long p=n-1; p>=0; p--) {
      // insert suffix starting at position p
      ch = s.get(p); chi=ch+128;
      if (lexfirstpos[chi]==-1) {  // seeing character ch for the first time
        dll.insertnewx(chi,p);
        steps++;
      } else {                  // seeing character ch again
        assert(lexfirstpos[chi]>p);  
        assert(lexlastpos[chi]>p);
        if (alphabet.isSpecial(ch)) {    // special character: always inserted first
          dll.insertasfirstx(chi,p);
          steps++;
        } else {                     // symbol character: proceed normally
          dll.walkandinsert(chi,p);
        } // end symbol character
      } // end seeing character ch again
      //showpos_R(String.format("List after step %d: [%d]%n", p, s[p])); // DEBUG
    } // end for p
  }

    
  
  // ==================== checking routines ====================================
  
  /** compare two characters of text s.
   * "Symbols" are compared according to their order in the alphabet map
   * "special" characters (wildcards, separators) are compared by position
   *@param i  first position
   *@param j  second position
   *@return any value &lt; 0 iff s[i]&lt;s[j], as specified by alphabet map,
   * zero(0) iff s[i]==s[j],
   * any value &gt; 0 iff s[i]&gt;s[j], as specified by alphabet map.
   */
  public final long scmp(final long i, final long j) {
    final byte si = s.get(i);
    final int d = si - s.get(j);
    if (d!=0 || alphabet.isSymbol(si)) return d;
    return i-j;
  }
  
  /** compare two suffixes of text s.
   * "Symbols" are compared according to their order in the alphabet map
   * "special" characters (wildcards, separators) are compared by position
   *@param i  first position
   *@param j  second position
   *@return any value &lt; 0 iff suffix(i)&lt;suffix(j) lexicographically,
   * zero (0) iff i==j
   * any value &gt; 0 iff suffix(i)&gt;suffix(j) lexicographically.
   */
  public final long suffixcmp(final long i, final long j) {
    if (i==j) return 0;
    long off, c;
    for(off=0; (c=scmp(i+off,j+off))==0; off++) {}
    // suffixes i and j disagree at offset off, thus have off characters in common
   return c;
  }
  
  /** find length of longest common prefix (lcp) of suffixes of text s,
   * given prior knowledge that lcp &gt;= h.
   *@param i  first position
   *@param j  second position
   *@param h  minimum known lcp length
   *@return  lcp length
   */
  public final long suffixlcp(final long i, final long j, final long h) {
    if (i==j) return n-i+1;
    long off;
    for(off=h; scmp(i+off,j+off)==0; off++) {}
    return off;
  }
  
 
  
  /** check correctnes of a suffix array when lexnextpos is available;
   * output warning messages if errors are found.
   *@return  0 on success, 1 on sorting error, 2 on count error, 3 on both errors.
   */
  public int checkpos_R() {
    int chi;
    long p, nextp, nn, comp;
    int returnvalue = 0;
    for (chi=0; chi<256 && lexfirstpos[chi]==-1; chi++)  {}
    if (chi>=256) {
      if(n==0) return 0;
      log.error("suffixcheck: no first character found, but |s|!=0.%n");
      return 2;
    }
    p = lexfirstpos[chi]; assert(p!=-1);
    nextp = lexnextpos.get(p); nn=1;
    while (nextp!=-1) {
      //g.logmsg("  pos %d vs %d; text %d vs %d%n", p, nextp, s[p], s[nextp]);
      if (!((comp=suffixcmp(p,nextp))<0)) {
        log.error("suffixcheck: sorting error at ranks %d, %d; pos %d, %d; text %d, %d; cmp %d%n",
            nn-1,nn, p,nextp, s.get(p), s.get(nextp), comp);
        returnvalue=1;
      }
      p=nextp;
      nextp = lexnextpos.get(p);
      nn++;
    }
    if (nn!=n) {
      log.error("suffixcheck: missing some suffixes; have %d / %d.%n",nn,n);
      returnvalue += 2;
    }
    return returnvalue;
  }
  
  /** check correctnes of a suffix array constructed with Walk-bothLR
   * using the SPACE SAVING xor technique;
   * output warning messages if errors are found.
   *@return  0 on success, 1 on sorting error, 2 on count error, 3 on both errors.
   */  
    public int checkpos_bothLR() {
    int returnvalue = 0;
    int chi;
    long p, oldp, nextp, nn, comp;
    final HugeLongArray lexps = dll.lexps;
    for (chi=0; chi<256 && lexfirstpos[chi]==-1; chi++)  {}
    if (chi>=256) {
      if(n==0) return 0;
      log.error("suffixcheck: no first character found, but |s|!=0.%n");
      return 2;
    }
    nn=1;
    p = lexfirstpos[chi]; 
    assert(p!=-1);
    nextp = lexps.get(p) ^ -1L; 
    while (nextp!=-1) {
      //g.logmsg("  pos %d vs %d; text %d vs %d%n", p, nextp, s[p], s[nextp]);
      if (!((comp=suffixcmp(p,nextp))<0)) {
        log.error("suffixcheck: sorting error at ranks %d, %d; pos %d, %d; text %d, %d; cmp %d%n",
            nn-1,nn, p,nextp, s.get(p), s.get(nextp), comp);
        returnvalue=1;
      }
      oldp=p;
      p=nextp;
      nextp = lexps.get(p) ^ oldp;
      nn++;
    }
    if (nn!=n) {
      log.error("suffixcheck: missing some suffixes; have %d / %d.%n",nn,n);
      returnvalue += 2;
    }
    return returnvalue;
  }
 
  /** check correctnes of a suffix array on disk
   * outputs warning messages if errors are found
   *@param di  path and name of the index to check
   *@return  0 on success, 1 on sorting error, 2 on count error
   */
  public int checkpos(String di) {
    int returnvalue = 0;
    TicToc ctimer = new TicToc();
    log.info("suffixcheck: checking pos...%n");
    ArrayFile fpos = null; 
    LongBuffer pos = null;
    try {
      fpos = new ArrayFile(di+FileNameExtensions.pos,0);
      pos = fpos.mapR().asLongBuffer();
    } catch (IOException ex) {
      log.error("suffixcheck: could not read .pos file; " + ex);
      g.terminate(1);
    }
    long p = pos.get();
    long nextp, comp;
    long nn=1;
    while (true) {
      try {
        nextp = pos.get();
      } catch (BufferUnderflowException ex) {
        break;
      }
      if (!((comp=suffixcmp(p,nextp))<0)) {
        log.error("suffixcheck: sorting error at ranks %d, %d; pos %d, %d; text %d, %d; cmp %d%n",
            nn-1,nn, p,nextp, s.get(p), s.get(nextp), comp);
        returnvalue=1;
      }
      nn++; p=nextp;
    }
    if (nn!=n) {
      log.error("suffixcheck: missing some suffixes; have %d / %d.%n",nn,n);
      returnvalue += 2;
    }
    if (returnvalue==0) log.info("suffixcheck: pos seems OK!%n");
    log.info("suffixcheck: checking took %.2f secs.%n", ctimer.tocs());
    return returnvalue;      
  }  
  
  
// ==================== writing routines ==================================
  
  
  /** write pos array to file when lexnextpos is available
   *@param fname  the full path and file name
   */
  private void writepos_R(final String fname) {
    int chi;
    long p;
    for (chi=0; chi<256 && lexfirstpos[chi]==-1; chi++)  {}
    try {
      final ArrayFile f = new ArrayFile(fname).openW();
      for (p=lexfirstpos[chi]; p!=-1; p=lexnextpos.get(p))
        f.writeLong(p);
      f.close();
    } catch (IOException ex) {
      log.error("suffixtray: error writing '%s': %s%n", fname, ex);
      g.terminate(1);
    }
  }
 
  /** write pos array to file after walk-bothLR using the xor trick.
   *@param fname  the full path and file name
   */
  private void writepos_bothLR(final String fname) {
    int chi;
    long p, oldp, tmp;
    final HugeLongArray lexps = dll.lexps;
    for (chi=0; chi<256 && lexfirstpos[chi]==-1; chi++)  {}
    try {
      final ArrayFile f = new ArrayFile(fname).openW();
      for (oldp=-1, p=lexfirstpos[chi];  p!=-1;  ) {
        f.writeLong(p);
        tmp = p;
        p = lexps.get(p) ^ oldp;
        oldp = tmp;
      }
      f.close();
    } catch (IOException ex) {
      log.error("suffixtray: error writing '%s': %s%n",fname, ex);
      g.terminate(1);
    }
  }
  
  /** write partial pos array to logfile during a construction
   * where lexnextpos[] is available.
   *@param header  a header string to print
   */
  private void showall_R(final String header) {
    int  chi;
    long p;
    System.out.printf(header);
    for (chi=0; chi<256; chi++) {
      assert(lexfirstpos[chi]==-1 || lexlastpos[chi]!=-1);
      //if (lexfirstpos[chi]!=-1)
      //  System.out.printf("  char %d: lexfirst=%2d, lexlast=%2d%n", chi-128, lexfirstpos[chi], lexlastpos[chi]);
    }
    for (chi=0; chi<256 && lexfirstpos[chi]==-1; chi++)  {}
    for (p=lexfirstpos[chi]; p!=-1; p=lexnextpos.get(p))
      System.out.printf("  %2d: [%c] %s%n", p, (char)(p>0?s.get(p-1)+48:'$'), ArrayUtils.bytesToString(s,p));
  }
  
// ==================== lcp routines ==================================

  /** lcp computation according to Kasai et al.'s algorithm
   * when lexprevpos[] is available.
   *@param fname filename for lcp array
   *@param dolcp which lcp arrays to compute (0..15, any combination of 1+2+4+8)
   */
  private void lcp_L(String fname, int dolcp) {
    TicToc timer = new TicToc();
    long p, prev, h;
    h=0;
    for (p=0; p<n; p++) {
      prev = lexprevpos.get(p);
      if (prev!=-1)  h = suffixlcp(prev,p,h); 
      else {assert(h==0);}
      assert(h>=0);
      if(h>maxlcp) maxlcp=h;
      lexprevpos.set(p,h);
      if (h>=255)        lcp1x++;
      if (h>=65535)      lcp2x++; 
      if (h>=(1L<<32)-1) lcp4x++;
      if (h>0) h--;
    }
    log.info("suffixtray: lcp computation took %.2f secs; writing...%n",timer.tocs());

    int chi;
    long r;
    for (chi=0; chi<256 && lexfirstpos[chi]==-1; chi++)  {}
    ArrayFile f8=null, f4=null, f2=null, f1=null, f4x=null, f2x=null, f1x=null;
    try {
      if ((dolcp&8)!=0) f8 = new ArrayFile(fname+"8").openW();
      if ((dolcp&4)!=0) { f4 = new ArrayFile(fname+"4").openW(); f4x = new ArrayFile(fname+"4x",0).openW(); }
      if ((dolcp&2)!=0) { f2 = new ArrayFile(fname+"2").openW(); f2x = new ArrayFile(fname+"2x",0).openW(); }
      if ((dolcp&1)!=0) { f1 = new ArrayFile(fname+"1").openW(); f1x = new ArrayFile(fname+"1x",0).openW(); }
      for (r=0, p=lexfirstpos[chi];   p!=-1;   p=lexnextpos.get(p), r++) {
        h = lexprevpos.get(p);
        assert(h>=0);
        if ((dolcp&8)!=0) { f8.writeLong(h); }
        if ((dolcp&4)!=0) {
          if (h>=(1L<<32)-1) { f4.writeInt((int)-1); f4x.writeLong(r); f4x.writeLong(h); }
          else f4.writeInt((int)h);
        }
        if ((dolcp&2)!=0) {
          if (h>=65535) { f2.writeShort((short)-1); f2x.writeLong(r); f2x.writeLong(h); }
          else f2.writeShort((short)h);
        }
        if ((dolcp&1)!=0) {
          if (h>=255)   { f1.writeByte((byte)-1);  f1x.writeLong(r); f1x.writeLong(h); }
          else f1.writeByte((byte)h);
        } 
      }
      assert(r==n);
      if ((dolcp&8)!=0) f8.close();
      if ((dolcp&4)!=0) { f4.close(); f4x.close(); }
      if ((dolcp&2)!=0) { f2.close(); f2x.close(); }
      if ((dolcp&1)!=0) { f1.close(); f1x.close(); }
    } catch (IOException ex) {
      log.error("suffixtray: error writing lcp file(s): %s", ex);
      g.terminate(1);
    }
  }

  /** lcp computation according to Kasai et al.'s algorithm
   * after walk-bothLR suffix array construction 
   * using the SPACE SAVING xor technique.
   *@param fname filename for lcp array
   *@param dolcp which lcp arrays to compute (1+2+4+8)
   */
  private void lcp_bothLR(String fname, int dolcp) {
    throw new UnsupportedOperationException("Not yet implemented");
  }
    
  
  
}
