/*
 * SuffixTrayBuilder.java
 *
 * Created on May 2, 2007, 6:23 PM
 * TODO: include lcp in checking!
 * TODO: include manber myers method!
 * TODO: space-efficient lcp not yet implemented
 */

package verjinxer;

import static verjinxer.Globals.programname;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.IntBuffer;
import java.util.Arrays;

import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.util.ArrayFile;
import verjinxer.util.ArrayUtils;
import verjinxer.util.IllegalOptionException;
import verjinxer.util.Options;
import verjinxer.util.ProjectInfo;
import verjinxer.util.StringUtils;
import verjinxer.util.TicToc;

/**
 *
 * @author Sven Rahmann
 */
public class SuffixTrayBuilder {
  
  private Globals g;
  
  /** Creates a new instance of SuffixTrayBuilder
   * @param gl global configuration structure
   */
  public SuffixTrayBuilder(Globals gl) {
    g = gl;
  }
  
  /**
   * print help on usage
   */
  public void help() {
    g.logmsg("Usage:%n  %s suffix [options] Indexnames...%n", programname);
    g.logmsg("Builds the suffix tray (tree plus array) of a .seq file;%n");
    g.logmsg("writes %s, %s (incl. variants 1,1x,2,2x).%n", FileNameExtensions.pos, FileNameExtensions.lcp);
    g.logmsg("Options:%n");
    g.logmsg("  -m, --method  <id>    select construction method, where <id> is one of:%n");
    g.logmsg("      L%n" +
        "      R%n" +
        "      minLR%n" +
        "      bothLR%n" +
        "      bothLR2%n"
        );
    g.logmsg("  -l, --lcp[2|1]        build lcp array using int|short|byte%n");
    g.logmsg("  -c, --check           additionally check index integrity%n");
    g.logmsg("  -C, --onlycheck       ONLY check index integrity%n");
    g.logmsg("  -X, --notexternal     DON'T save memory at the cost of lower speed%n");
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
  
  int asize        = -1;
  Alphabet alphabet = null;
  byte[] s         = null;   // text
  int n            = -1;     // length of text
  int[] lexfirstpos   = null;
  int[] lexlastpos    = null;
  int[] lexprevpos    = null;
  int[] lexnextpos    = null;
  long steps       = 0;
  
  int lcp1x        = 0;      // lcp1 exceptions
  int lcp2x        = 0;      // lcp2 exceptions
  int maxlcp       = -1;
  
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
    } catch (IllegalOptionException ex) { g.terminate("suffixtray: "+ex); }
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
    g.startplog(di+FileNameExtensions.log);
    if (args.length>1) g.warnmsg("suffixtray: ignoring all arguments except first '%s'%n", args[0]);
    
    ProjectInfo project;
    try {
       project = ProjectInfo.createFromFile(di);
    } catch (IOException e) {
       g.warnmsg("cannot read project file.%n");
       return 1;
    }
    asize = project.getIntProperty("LargestSymbol") + 1;

    // load alphabet map and text
    alphabet = g.readAlphabet(di+FileNameExtensions.alphabet);
    s = g.slurpByteArray(di+FileNameExtensions.seq);
    n = s.length;
    if (onlycheck) { returnvalue =  checkpos(di); g.stopplog(); return returnvalue; }
    project.setProperty("SuffixAction", action);
    project.setProperty("LastAction","suffixtray");
    project.setProperty("AlphabetSize",asize);    
    
    method = (opt.isGiven("m")? opt.get("m") : "L"); // default method
    g.logmsg("suffixtray: constructing pos using method '%s'...%n", method);
    assert(alphabet.isSeparator(s[n-1])) : "last character in text needs to be a separator";
    TicToc timer = new TicToc();
    steps = 0;
    if (method.equals("L"))            buildpos_L();
    else if (method.equals("R"))       buildpos_R();
    else if (method.equals("minLR"))   buildpos_minLR(false);
    else if (method.equals("bothLR"))  buildpos_bothLR();
    else if (method.equals("bothLR2")) buildpos_bothLR2();
    else g.terminate("suffixtray: Unsupported construction method '"+method+"'!");
    g.logmsg("suffixtray: pos completed after %.1f secs using %d steps (%.2f/char)%n",
        timer.tocs(), steps, (double)steps/n);
    project.setProperty("SuffixTrayMethod",method);
    project.setProperty("SuffixTraySteps",steps);    
    project.setProperty("SuffixTrayStepsPerChar",(double)steps/n);    
    
    if (check) {
      timer.tic();
      g.logmsg("suffixcheck: checking pos...%n");
      if (method.equals("L"))            returnvalue=checkpos_R();
      else if (method.equals("R"))       returnvalue=checkpos_R();
      else if (method.equals("minLR"))   returnvalue=checkpos_R();
      else if (method.equals("bothLR"))  returnvalue=checkpos_bothLR();
      else if (method.equals("bothLR2")) returnvalue=checkpos_R();
      else g.terminate("suffixcheck: Unsupported construction method '"+method+"'!");
      if(returnvalue==0) g.logmsg("suffixcheck: pos looks OK!%n");
      g.logmsg("suffixcheck: done after %.1f secs%n", timer.tocs());
    }
    
    if (returnvalue==0) {
      timer.tic();
      String fpos = di+FileNameExtensions.pos;
      g.logmsg("suffixtray: writing '%s'...%n",fpos);
      if (method.equals("L"))            writepos_R(fpos);
      else if (method.equals("R"))       writepos_R(fpos);
      else if (method.equals("minLR"))   writepos_R(fpos);
      else if (method.equals("bothLR"))  writepos_bothLR(fpos);
      else if (method.equals("bothLR2")) writepos_R(fpos);
      else g.terminate("suffixtray: Unsupported construction method '"+method+"'!");
      g.logmsg("suffixtray: writing took %.1f secs; done.%n", timer.tocs());
    }
    
    // do lcp if desired
    if (dolcp>0 && returnvalue==0) {
      timer.tic();
      String flcp = di+FileNameExtensions.lcp;
      g.logmsg("suffixtray: computing lcp array...%n");
      if (method.equals("L"))            lcp_L(flcp, dolcp);
      else if (method.equals("R"))       lcp_L(flcp, dolcp);
      else if (method.equals("minLR"))   lcp_L(flcp, dolcp);
      else if (method.equals("bothLR"))  lcp_bothLR(flcp, dolcp);
      else if (method.equals("bothLR2")) lcp_L(flcp, dolcp);
      else g.terminate("suffixtray: Unsupported construction method '"+method+"'!");
      g.logmsg("suffixtray: lcp computation and writing took %.1f secs; done.%n", timer.tocs());
      project.setProperty("lcp1Exceptions",lcp1x);
      project.setProperty("lcp2Exceptions",lcp2x);
      project.setProperty("lcp1Size",n+8*lcp1x);
      project.setProperty("lcp2Size",2*n+8*lcp2x);
      project.setProperty("lcp4Size",4*n);
      project.setProperty("lcpMax",maxlcp);     
    }
    
    // write project data
    try { 
       project.store();
    } catch (IOException ex) { 
      g.warnmsg("suffix: could not write %s (%s)!%n", project.getFileName(), ex.toString()); 
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
    lexfirstpos = new int[256];
    lexlastpos  = new int[256];
    lexprevpos  = new int[n];
    lexnextpos  = new int[n];
    Arrays.fill(lexfirstpos,-1);
    Arrays.fill(lexlastpos,-1);
    byte ch; int chi;
    
    for (int p=n-1; p>=0; p--) {
      // insert suffix starting at position p
      ch = s[p]; chi=ch+128;
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
          int i = p+1;
          steps++;
          while(lexprevpos[i]!=-1 && s[(i=lexprevpos[i])-1]!=ch) {steps++; }
          i--;
          if (s[i]==ch && i!=p) { // insert p after i, might be new last
            insertbetween(i,lexnextpos[i],p);
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
    lexfirstpos = new int[256];
    lexlastpos  = new int[256];
    lexprevpos  = new int[n];
    lexnextpos  = new int[n];
    Arrays.fill(lexfirstpos,-1);
    Arrays.fill(lexlastpos,-1);
    byte ch; int chi;
    
    for (int p=n-1; p>=0; p--) {
      // insert suffix starting at position p
      ch = s[p]; chi=ch+128;
      if (lexfirstpos[chi]==-1) {  // seeing character ch for the first time
        insertnew(chi,p);
        steps++;
      } else {                  // seeing character ch again
        assert(lexfirstpos[chi]>p);  assert(lexlastpos[chi]>p);
        if (alphabet.isSpecial(ch)) {    // special character: always inserted first
          insertasfirst(chi,p);
          steps++;
        } else {                     // symbol character: proceed normally
          int i = p+1;
          steps++;
          while(lexnextpos[i]!=-1 && s[(i=lexnextpos[i])-1]!=ch) {steps++; }
          i--;
          if (s[i]==ch && i!=p) {        // insert p BEFORE i, might be new first
            insertbetween(lexprevpos[i],i,p);
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
    lexfirstpos = new int[256];
    lexlastpos  = new int[256];
    lexprevpos  = new int[n];
    lexnextpos  = new int[n];
    Arrays.fill(lexfirstpos,-1);
    Arrays.fill(lexlastpos,-1);
    byte ch; int chi;
    int pup, pdown;
    int lsp, lpp;
    int found=0;
    
    for (int p=n-1; p>=0; p--) {
      // insert suffix starting at position p
      ch = s[p]; chi=ch+128;
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
            lpp = lexprevpos[pup];
            if (lpp==-1)              { found=1; break; } // new first
            if (s[(pup=lpp)-1]==ch)   { found=2; break; } // insert after pup
            steps++;
            lsp = lexnextpos[pdown];
            if (lsp==-1)              { found=3; break; } // new last
            if (s[(pdown=lsp)-1]==ch) { found=4; break; } // insert before pdown
          }
          pup--; pdown--;
          switch (found) {
            case 1: insertasfirst(chi,p);  break;
            case 2: insertbetween(pup,lexnextpos[pup],p); if (lexlastpos[chi]==pup) lexlastpos[chi]=p;  break;
            case 3: insertaslast(chi,p);   break;
            case 4: insertbetween(lexprevpos[pdown],pdown,p); if (lexfirstpos[chi]==pdown) lexfirstpos[chi]=p; break;
            default: g.terminate("suffixtray: internal error");
          } // end switch
        } // end symbol character
      } // end seeing character ch again
      if (show) showall_R(String.format("List after step %d: [%d]%n", p, s[p])); // DEBUG
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
    lexfirstpos = new int[256];
    lexlastpos  = new int[256];
    lexprevpos  = new int[n];
    lexnextpos  = new int[n];
    Arrays.fill(lexfirstpos,-1);
    Arrays.fill(lexlastpos,-1);
    byte ch; int chi;
    int pup, pdown;
    int lsp, lpp;
    int foundup=0;
    int founddown=0;
    
    for (int p=n-1; p>=0; p--) {
      // insert suffix starting at position p
      ch = s[p]; chi=ch+128;
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
              lsp = lexnextpos[pdown];
              if (lsp==-1)              { founddown=1; foundup=2; break; } // new last
              if (s[(pdown=lsp)-1]==ch) { founddown=2; } // insert before pdown
            }
            if (foundup==0) {
              steps++;
              lpp = lexprevpos[pup];
              if (lpp==-1)              { foundup=1; founddown=2; break; } // new first
              if (s[(pup=lpp)-1]==ch)   { foundup=2; }   // insert after pup
            }
          }
          if (founddown==1) {       // new last
            pup=lexlastpos[chi];
            pdown=lexnextpos[pup];
            lexlastpos[chi]=p;
          } else if (foundup==1) {  // new first
            pdown=lexfirstpos[chi];
            pup=lexprevpos[pdown];
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
  
  
  /** insert i between p1 and p2 (they must be neighbors with p1<p2)
   *@param p1  position after which to insert
   *@param p2  position before which to insert
   *@param i  what to insert
   */
  private final void insertbetween(final int p1, final int p2, final int i) {
    // before: ... p1, p2 ...
    // after:  ... p1, i, p2 ...
    assert(p1==-1 || lexnextpos[p1]==p2)
    : String.format("i=%d (of %d); p1=%d, p2=%d; s[i..i+1]=(%d,%d); lexsucc[p1]=%d %n",
        i,n,p1,p2,s[i],(i<n-1?s[i+1]:999),lexnextpos[p1]);
    assert(p2==-1 || lexprevpos[p2]==p1)
    : String.format("i=%d (of %d); p1=%d, p2=%d; s[i..i+1]=(%d,%d); lexpred[p2]=%d %n",
        i,n,p1,p2,s[i],(i<n-1?s[i+1]:999),lexprevpos[p2]);
    lexprevpos[i] = p1;
    lexnextpos[i] = p2;
    if (p2!=-1) lexprevpos[p2] = i;
    if (p1!=-1) lexnextpos[p1] = i;
  }
  
  
  /** insert the new first occurrence of chi before p=lexfirst[chi].
   *@param chi  integer representation of character to insert
   *@param i  what to insert
   */
  private final void insertasfirst(final int chi, final int i) {
    final int p = lexfirstpos[chi];
    assert(p!=-1);
    insertbetween(lexprevpos[p],p,i);
    lexfirstpos[chi]=i;
  }
  
  /** insert the new last occurrence of chi before p=lexfirst[chi].
   *@param chi  integer representation of character to insert
   *@param i  what to insert
   */
  private final void insertaslast(final int chi, final int i) {
    final int p = lexlastpos[chi];
    assert(p!=-1);
    insertbetween(p,lexnextpos[p],i);
    lexlastpos[chi]=i;
  }
  
  /** insert the first overall occurrence of a new character chi.
   *@param chi  integer representation of character to insert
   *@param i  what to insert
   */
  private final void insertnew(final int chi, final int i) {
    int cp, cs, ip, is;
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
    
    private final void insertasfirstx(int chi, int i) {
      int cp, ip;
      assert(lexfirstpos[chi]!=-1);
      assert(lexlastpos[chi]!=-1);
      for(cp=chi-1; cp>=0 && lexlastpos[cp]==-1; cp--) {}
      ip = (cp>=0? lexlastpos[cp] : -1);
      insertbetweenx(ip,lexfirstpos[chi],i);
      lexfirstpos[chi]=i;      
    }

    private final void insertaslastx(int chi, int i) {
      int cs, is;
      assert(lexfirstpos[chi]!=-1);
      assert(lexlastpos[chi]!=-1);
      for(cs=chi+1; cs<256 && lexfirstpos[cs]==-1; cs++) {}
      is = (cs<256? lexfirstpos[cs] : -1);
      insertbetweenx(lexlastpos[chi],is,i);
      lexlastpos[chi]=i;      
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
 
  
  /** builds suffix array 'pos'
   * by walking BOTH WAYS along a partially constructed doubly linked suffix list,
   * until the target character is found BOTH WAYS.
   * This implementation uses the SPACE SAVING xor trick. 
   * Needs text 's' and its length 'n' correctly set.
   */
  public void buildpos_bothLR() {
    lexprevpos  = null;
    lexnextpos  = null;
    lexfirstpos = new int[256];
    lexlastpos  = new int[256];
    Arrays.fill(lexfirstpos,-1);
    Arrays.fill(lexlastpos,-1);
    dll = new LightDLL(n);
    byte ch; 
    int chi;
    
    for (int p=n-1; p>=0; p--) {
      // insert suffix starting at position p
      ch = s[p]; chi=ch+128;
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
  public final int scmp(final int i, final int j) {
    final int d = s[i]-s[j];
    if (d!=0 || alphabet.isSymbol(s[i])) return d;
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
  public final int suffixcmp(final int i, final int j) {
    if (i==j) return 0;
    int off, c;
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
  public final int suffixlcp(final int i, final int j, final int h) {
    if (i==j) return n-i+1;
    int off;
    for(off=h; scmp(i+off,j+off)==0; off++) {}
    return off;
  }
  
 
  
  /** check correctnes of a suffix array when lexnextpos is available;
   * output warning messages if errors are found.
   *@return  0 on success, 1 on sorting error, 2 on count error, 3 on both errors.
   */
  public int checkpos_R() {
    int chi, p, nextp, nn, comp;
    int returnvalue = 0;
    for (chi=0; chi<256 && lexfirstpos[chi]==-1; chi++)  {}
    if (chi>=256) {
      if(n==0) return 0;
      g.warnmsg("suffixcheck: no first character found, but |s|!=0.%n");
      return 2;
    }
    p = lexfirstpos[chi]; assert(p!=-1);
    nextp = lexnextpos[p]; nn=1;
    while (nextp!=-1) {
      //g.logmsg("  pos %d vs %d; text %d vs %d%n", p, nextp, s[p], s[nextp]);
      if (!((comp=suffixcmp(p,nextp))<0)) {
        g.warnmsg("suffixcheck: sorting error at ranks %d, %d; pos %d, %d; text %d, %d; cmp %d%n",
            nn-1,nn, p,nextp, s[p], s[nextp], comp);
        returnvalue=1;
      }
      p=nextp;
      nextp = lexnextpos[p];
      nn++;
    }
    if (nn!=n) {
      g.warnmsg("suffixcheck: missing some suffixes; have %d / %d.%n",nn,n);
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
    int chi, p, oldp, nextp, nn, comp;
    int returnvalue = 0;
    final int[] lexps = dll.lexps;
    for (chi=0; chi<256 && lexfirstpos[chi]==-1; chi++)  {}
    if (chi>=256) {
      if(n==0) return 0;
      g.warnmsg("suffixcheck: no first character found, but |s|!=0.%n");
      return 2;
    }
    nn=1;
    p = lexfirstpos[chi]; 
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
   *@param di  path and name of the index to check
   *@return  0 on success, 1 on sorting error, 2 on count error
   */
  public int checkpos(String di) {
    int returnvalue = 0;
    TicToc ctimer = new TicToc();
    g.logmsg("suffixcheck: checking pos...%n");
    ArrayFile fpos = null; 
    IntBuffer pos = null;
    try {
      fpos = new ArrayFile(di+FileNameExtensions.pos,0);
      pos = fpos.mapR().asIntBuffer();
    } catch (IOException ex) {
      g.terminate("suffixcheck: could not read .pos file; " + ex);
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
  
  
  /** write pos array to file when lexnextpos is available
   *@param fname  the full path and file name
   */
  private void writepos_R(String fname) {
    int chi, p;
    for (chi=0; chi<256 && lexfirstpos[chi]==-1; chi++)  {}
    try {
      final ArrayFile f = new ArrayFile(fname).openW();
      for (p=lexfirstpos[chi]; p!=-1; p=lexnextpos[p])
        f.writeInt(p);
      f.close();
    } catch (IOException ex) {
      g.warnmsg("suffixtray: error writing '%s': %s%n",fname, ex);
      g.terminate(1);
    }
  }
 
  /** write pos array to file after walk-bothLR using the xor trick.
   *@param fname  the full path and file name
   */
  private void writepos_bothLR(String fname) {
    int chi, p, oldp, tmp;
    final int[] lexps = dll.lexps;
    for (chi=0; chi<256 && lexfirstpos[chi]==-1; chi++)  {}
    try {
      final ArrayFile f = new ArrayFile(fname).openW();
      for (oldp=-1, p=lexfirstpos[chi];  p!=-1;  ) {
        f.writeInt(p);
        tmp = p;
        p = lexps[p] ^ oldp;
        oldp = tmp;
      }
      f.close();
    } catch (IOException ex) {
      g.warnmsg("suffixtray: error writing '%s': %s%n",fname, ex);
      g.terminate(1);
    }
  }
  
  /** write partial pos array to logfile during a construction
   * where lexnextpos[] is available.
   *@param header  a header string to print
   */
  private void showall_R(String header) {
    int chi, p;
    System.out.printf(header);
    for (chi=0; chi<256; chi++) {
      assert(lexfirstpos[chi]==-1 || lexlastpos[chi]!=-1);
      //if (lexfirstpos[chi]!=-1)
      //  System.out.printf("  char %d: lexfirst=%2d, lexlast=%2d%n", chi-128, lexfirstpos[chi], lexlastpos[chi]);
    }
    for (chi=0; chi<256 && lexfirstpos[chi]==-1; chi++)  {}
    for (p=lexfirstpos[chi]; p!=-1; p=lexnextpos[p])
      System.out.printf("  %2d: [%c] %s%n", p, (char)(p>0?s[p-1]+48:'$'), ArrayUtils.bytesToString(s,p));
  }
  
// ==================== lcp routines ==================================

  /** lcp computation according to Kasai et al.'s algorithm
   * when lexprevpos[] is available.
   *@param fname filename for lcp array
   *@param dolcp which lcp arrays to compute (0..7, any combination of 1+2+4)
   */
  private void lcp_L(String fname, int dolcp) {
    TicToc timer = new TicToc();
    int p, prev, h;
    h=0;
    for (p=0; p<n; p++) {
      prev = lexprevpos[p];
      if (prev!=-1)  h = suffixlcp(prev,p,h); 
      else {assert(h==0);}
      assert(h>=0);
      if(h>maxlcp) maxlcp=h;
      lexprevpos[p]=h;
      if (h>=255)   lcp1x++;
      if (h>=65535) lcp2x++;    
      if (h>0) h--;
    }
    g.logmsg("suffixtray: lcp computation took %.2f secs; writing...%n",timer.tocs());

    int chi, r;
    for (chi=0; chi<256 && lexfirstpos[chi]==-1; chi++)  {}
    ArrayFile f4=null, f2=null, f1=null, f2x=null, f1x=null;
    try {
      if ((dolcp&4)!=0) f4 = new ArrayFile(fname).openW();
      if ((dolcp&2)!=0) { f2 = new ArrayFile(fname+"2").openW(); f2x = new ArrayFile(fname+"2x",0).openW(); }
      if ((dolcp&1)!=0) { f1 = new ArrayFile(fname+"1").openW(); f1x = new ArrayFile(fname+"1x",0).openW(); }
      for (r=0, p=lexfirstpos[chi];   p!=-1;   p=lexnextpos[p], r++) {
        h = lexprevpos[p];
        assert(h>=0);
        if ((dolcp&4)!=0) { f4.writeInt(h); }
        if ((dolcp&2)!=0) {
          if (h>=65535) { f2.writeShort((short)-1); f2x.writeInt(r); f2x.writeInt(h); }
          else f2.writeShort((short)h);
        }
        if ((dolcp&1)!=0) {
          if (h>=255)   { f1.writeByte((byte)-1);  f1x.writeInt(r); f1x.writeInt(h); }
          else f1.writeByte((byte)h);
        } 
      }
      assert(r==n);
      if ((dolcp&4)!=0) f4.close();
      if ((dolcp&2)!=0) { f2.close(); f2x.close(); }
      if ((dolcp&1)!=0) { f1.close(); f1x.close(); }
    } catch (IOException ex) {
      g.warnmsg("suffixtray: error writing lcp file(s): %s%n", ex);
      g.terminate(1);
    }
  }

  /** lcp computation according to Kasai et al.'s algorithm
   * after walk-bothLR suffix array construction 
   * using the SPACE SAVING xor technique.
   *@param fname filename for lcp array
   *@param dolcp which lcp arrays to compute (1+2+4)
   */
  private void lcp_bothLR(String fname, int dolcp) {
    throw new UnsupportedOperationException("Not yet implemented");
  }
    
  
  
}
