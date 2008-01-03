/*
 * Translater.java
 *
 * Created on 30. Januar 2007, 14:57
 *
 */

package verjinxer;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Properties;
import verjinxer.sequenceanalysis.*;
import verjinxer.util.*;
import static verjinxer.Globals.*;

/**
 * Application class to translate a set of text or FASTA files
 * @author Sven Rahmann
 */
public class Translater {
  
  private Globals g;
  
  public Translater(Globals gl) {
    g = gl;
  }
  
  /**
   * print help on usage and options
   */
  public void help() {
    g.logmsg("Usage:%n  %s translate [options] <TextAndFastaFiles...>%n",programname);
    g.logmsg("translates one or more text or FASTA files, using an alphabet map;%n");
    g.logmsg("creates %s, %s, %s, %s, %s;%n", extseq, extdesc, extalph, extssp, extprj);
    g.logmsg("with option -r, also creates %s, %s, %s, %s.%n", extrunseq, extrunlen, extrun2pos, extpos2run);
    g.logmsg("Options:%n");
    g.logmsg("  -i, --index <name>   name of index files [first filename]%n");
    g.logmsg("  -t, --trim           trim non-symbol characters at both ends%n");
    g.logmsg("  -a, --amap  <file>   filename of alphabet map%n");
    g.logmsg("  --dna                use standard DNA alphabet%n");
    g.logmsg("  --rconly             translate to reverse DNA complement%n");
    g.logmsg("  --dnarc     <desc>   combines --dna and --rconly;%n");
    g.logmsg("     if <desc> is empty or '#', concatenate rc with dna; otherwise,%n");
    g.logmsg("     generate new rc sequences and add <desc> to their headers.%n");
    g.logmsg("  --dnabi              translate to bisulfite-treated DNA%n");
    g.logmsg("  --protein            use standard protein alphabet%n");
    g.logmsg("  -r, --runs           additionally create run-related files%n");
  }
  
  
  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) {
    new Translater(new Globals()).run(args);
  }
  
  
  // Translater variables
  boolean trim=false;
  AlphabetMap amap=null, amap2=null;
  boolean separateRCByWildcard = false;
  boolean reverse = false;
  boolean addrc = false;
  boolean bisulfite = false;
  AnnotatedArrayFile out = null;
  String dnarcstring = null;
  
  
  
  public int run(String[] args) {
    TicToc gtimer = new TicToc();
    g.cmdname = "translate";
    Properties prj = new Properties();
    prj.setProperty("TranslateAction", "translate \"" + Strings.join("\" \"",args)+ "\"");
    
    Options opt = new Options("i=index=indexname:,t=trim,a=amap:,dna,rc=rconly,dnarc:,dnabi,protein,r=run=runs");
    try {
      args = opt.parse(args);
    } catch (IllegalOptionException e) {
      g.terminate(e.toString()); }
    
    if (args.length==0) {
      help();
      g.logmsg("translate: no files given%n");
      g.terminate(0);
    }
    prj.setProperty("NumberSourceFiles", Integer.toString(args.length));
    
    // determine trimming
    trim = opt.isGiven("t");
    prj.setProperty("TrimmedSequences", Boolean.toString(trim));
    
    // determine the name of the index
    String outname;
    if (opt.isGiven("i")) outname=opt.get("i");
    else { // take base name of first FASTA file
      outname = new File(args[0]).getName();
      int lastdot = outname.lastIndexOf('.');
      if (lastdot>=0) outname = outname.substring(0,lastdot);
    }
    outname = g.outdir + outname;
    g.startplog(outname + extlog, true);  // start new project log
    
    // determine the alphabet map(s)
    int givenmaps = 0;
    if (opt.isGiven("a")) givenmaps++;
    if (opt.isGiven("dna")) givenmaps++;
    if (opt.isGiven("rconly")) givenmaps++;
    if (opt.isGiven("dnarc")) givenmaps++;
    if (opt.isGiven("dnabi")) givenmaps++;
    if (opt.isGiven("protein")) givenmaps++;
    if (givenmaps>1) g.terminate("translate: use only one of {-a, --dna, --rconly, --dnarc, --protein}.");
    
    if (opt.isGiven("a")) amap = g.readAlphabetMap(g.dir+opt.get("a"));
    if (opt.isGiven("dna") || opt.isGiven("dnarc")) amap = AlphabetMap.DNA();
    if (opt.isGiven("rc")) { reverse = true; amap = AlphabetMap.cDNA(); }
    if (opt.isGiven("dnarc")) {
      amap2 = AlphabetMap.cDNA();
      addrc = true;
      dnarcstring = opt.get("dnarc");
      if (dnarcstring.equals("")) separateRCByWildcard = true;
      if (dnarcstring.startsWith("#")) separateRCByWildcard = true;
    }
    if (opt.isGiven("dnabi")) {
      bisulfite = true;
      amap  = AlphabetMap.DNA(); // do translation on-line
    }
    if (opt.isGiven("protein")) amap = AlphabetMap.Protein();
    if (amap==null) 
      g.terminate("translate: no alphabet map given; use one of {-a, --dna, --rconly, --dnarc, --protein}.");
    
    
    // determine the file types: FASTA or TEXT
    // FASTA 'f': First non-whitespace character is a '>''
    // TEXT  't': all others
    char[] filetype = new char[args.length];
    for(int i=0; i<args.length; i++) {
      String fname = g.dir + args[i];
      int ch = ' ';
      try {
        FileReader reader = new FileReader(fname);
        for(ch=reader.read(); ch!=-1 && Character.isWhitespace(ch); ch=reader.read());
        reader.close();
      } catch (Exception e) {
        g.terminate("translate: could not open sequence file '"+fname+"'; "+e.toString()); }
      if (ch=='>') filetype[i]='f';
      else filetype[i]='t';
    }
    
    // open the output file stream
    g.logmsg("translate: creating index '%s'...%n", outname);
    out = new AnnotatedArrayFile(outname + extseq);
    try {
      out.openW();
    } catch (IOException ex) {
      g.warnmsg("translate: could not create output file '%s'; %s",outname+extseq,ex.toString());
    }
    
    // process each file according to type
    for (int i=0; i<args.length; i++) {
      String fname = g.dir + args[i];
      g.logmsg("  processing '%s' (%c)...%n",fname,filetype[i]);
      if (bisulfite && filetype[i]=='f') processFastaB(fname);
      else if (filetype[i]=='f') processFasta(fname);
      else if (filetype[i]=='t') processText(fname);
      else g.terminate("translate: unsupported file type for file "+args[i]);
    }
    // DONE processing all files.
    try { out.close(); } catch (IOException ex) {};
    long totallength=out.length();
    if (totallength>=(2L*1024*1024*1024))
      g.warnmsg("translate: output length %d exceeds 2 GB limit!!%n", totallength);
    else if (totallength>=(2L*1024*1024*1024*127)/128) 
      g.warnmsg("translate: long output, %d is within 99% of 2GB limit!%n", totallength);
    prj.setProperty("Length",Long.toString(totallength));
    
    // Write the ssp array.
    g.dumpIntArray(outname+extssp, out.getSsps());
    prj.setProperty("NumberSequences",Integer.toString(out.getSsps().length));
    
    // Write sequence length statistics.
    long maxseqlen=0;
    long minseqlen=Long.MAX_VALUE;
    for (int seqlen : out.getLengths()) {
      if (seqlen>maxseqlen) maxseqlen=seqlen;
      if (seqlen<minseqlen) minseqlen=seqlen;
    }
    prj.setProperty("LongestSequence",  Long.toString(maxseqlen));
    prj.setProperty("ShortestSequence", Long.toString(minseqlen));
    
    // Write the descriptions
    PrintWriter descfile = null;
    try {
      descfile = new PrintWriter(outname+extdesc);
      for (String s : out.getDescriptions()) descfile.println(s);
      descfile.close();
    } catch (IOException ex) {
      g.warnmsg("translate: %s%s: %s%n",outname,extdesc,ex.toString());
      g.terminate(1);
    }
    
    // Write the alphabet and project file
    PrintWriter alfile = null;
    try {
      //alfile = new PrintWriter(outname + extamap);
      //amap.showImage(alfile);
      //alfile.close();
      alfile = new PrintWriter(outname + extalph);
      amap.showSourceStrings(alfile);
      alfile.close();
    } catch (IOException ex) {
      g.terminate("translate: could not write alphabet: " + ex.toString());
    }
    
    prj.setProperty("SmallestSymbol",Integer.toString(amap.smallestSymbol()));
    prj.setProperty("LargestSymbol",Integer.toString(amap.largestSymbol()));
    prj.setProperty("LastAction","translate");
    try {
      prj.setProperty("Separator", Byte.toString(amap.codeSeparator()));
    } catch (InvalidSymbolException ex) {
      prj.setProperty("Separator", "128"); // illegal byte code -> nothing
    }
    g.logmsg("translate: finished translation after %.1f secs.%n", gtimer.tocs());
    
    
    // compute runs
    if (opt.isGiven("r")) {
      g.logmsg("translate: computing runs...%n");
      long runs = 0;
      try {
        runs = computeRuns(outname);
      } catch (IOException ex) {
        g.terminate("translate: could not create run-related files; "+ex.toString());
      }
      prj.setProperty("Runs", Long.toString(runs));
    }
    
    // write project file
    try {
      g.writeProject(prj,outname+extprj); 
    }  catch (IOException ex) { 
      g.terminate("translate: could not write project file"); 
    }
    
    // that's all
    g.logmsg("translate: done; total time was %.1f secs.%n", gtimer.tocs());
    return 0;
  }
  
  
  /************* processing methods ******************************************************/
  
  private void processFasta(String fname) {
    FastaFile f = new FastaFile(fname);
    FastaSequence fseq = null;
    ByteBuffer tr = null;
    final int appendforward = (addrc && separateRCByWildcard)? 1 : 2;
    final int appendreverse = 2; // always append separator
    long lastbyte = 0;
    
    try { f.open(); } catch (Exception e) { g.warnmsg("translate: skipping '%s': %s%n",fname, e.toString()); return; }
    while(true) {
      try {
        fseq = f.read(); // reads one fasta sequence from file f
        if (fseq==null) break;
        tr = fseq.translateTo(tr, trim, amap, reverse, appendforward);
        lastbyte = out.dumpToChannel(tr);
        if (addrc) {
          if (!separateRCByWildcard)  out.addInfo(fseq.getHeader(), fseq.length(), (int)(lastbyte-1));
          tr = fseq.translateTo(tr, trim, amap2, true, appendreverse);
          lastbyte = out.dumpToChannel(tr);
          if (separateRCByWildcard) out.addInfo(fseq.getHeader(), 2*fseq.length()+1, (int)(lastbyte-1));
          else out.addInfo(fseq.getHeader()+" "+dnarcstring, fseq.length(), (int)(lastbyte-1));
        } else { // no reverse complement
          out.addInfo(fseq.getHeader(), fseq.length(), (int)(lastbyte-1)); }
      } catch (Exception e) {
        g.warnmsg("translate: %s%n",e.toString()); break; }
    }
    // close file
    try {  f.close(); } catch (Exception e) { g.warnmsg("translate: %s%n",e.toString()); }
  }

  
  private void processFastaB(String fname) {
    FastaFile f = new FastaFile(fname);
    FastaSequence fseq = null;
    ByteBuffer tr = null;
    long lastbyte = 0;
    
    try { f.open(); } catch (Exception e) { g.warnmsg("translate: skipping '%s': %s%n",fname, e.toString()); return; }
    while(true) {
      try {
        fseq = f.read(); // reads one fasta sequence from file f
        if (fseq==null) break;
        tr = fseq.translateDNABiTo(tr, trim, false, false, 1);  // nonmeth-bis-#
        lastbyte = out.dumpToChannel(tr);
        tr = fseq.translateDNABiTo(tr, trim, false, true,  2);  // nonmeth-cbis-$
        lastbyte = out.dumpToChannel(tr);
        out.addInfo(fseq.getHeader()+" /nonmeth-bis+cbis", 2*fseq.length()+1, (int)(lastbyte-1));
        tr = fseq.translateDNABiTo(tr, trim, true,  false, 1);  // meth-bis-#
        lastbyte = out.dumpToChannel(tr);
        tr = fseq.translateDNABiTo(tr, trim, true,  true,  2);  // meth-cbis-$
        lastbyte = out.dumpToChannel(tr);
        out.addInfo(fseq.getHeader()+" /meth-bis+cbis", 2*fseq.length()+1, (int)(lastbyte-1));
      } catch (Exception e) {
        g.warnmsg("translate: %s%n",e.toString()); break; }
    }
    // close file
    try {  f.close(); } catch (Exception e) { g.warnmsg("translate: %s%n",e.toString()); }
  }

  
  /** text file processing differs from fasta processing by the following:
   * -- reverse complement option is ignored, even for DNA text file;
   * -- each cr/lf is replaced by whitespace (error if the alphabet map does not allow whitespace);
   * -- description is simply the filename;
   * -- separator is appended (never the wildcard).
   */
  private void processText(String fname) {
    ByteBuffer tr = null;
    long lastbyte = 0;
    byte appender;
    int len=0;
    String current = null;
    BufferedReader br = null;
    
    try {
      br = new BufferedReader(new FileReader(fname), 512*1024);
      String next = br.readLine();
      while (next!=null) {
        current=next;
        next=br.readLine();
        try {
          appender = (next==null)? amap.codeSeparator() : amap.codeWhitespace();
          len += current.length() + (next==null? 0:1);
          tr = amap.applyTo(current, tr, true, appender);
        } catch (InvalidSymbolException ex) {
          throw new IOException(ex);
        }
      }
      lastbyte = out.dumpToChannel(tr);
      out.addInfo(fname, len, (int)(lastbyte-1));
    } catch(IOException ex) {
      g.warnmsg("translate: error translating '%s': %s%n",fname,ex.toString());
      g.terminate(1);
    } finally {
      try { if (br!=null) {br.close();} } catch(IOException e) {};
    }
  }
  
  
  /******************************* runs ***********************************/
  
  /** reads translated sequence and writes run-related files,
   * primarily using memory-mapped files */
  public long computeRuns(String fname) throws IOException {
    int run = -1;
    ByteBuffer seq = new ArrayFile(fname+extseq).mapR();
    ArrayFile rseq = new ArrayFile(fname+extrunseq).openW();
    ArrayFile rlen = new ArrayFile(fname+extrunlen).openW();
    ArrayFile  p2r = new ArrayFile(fname+extpos2run).openW();
    ArrayFile  r2p = new ArrayFile(fname+extrun2pos).openW();
    final int n = seq.limit();
    byte next;
    byte prev=-1;
    int start = 0;
    int len;
    for (int p=0; p<n; p++) {
      next = seq.get();
      if (next!=prev || p==0) {
        run++;
        prev=next;
        len=p-start;  assert(len>0 || p==0);  if(len>127) len=-1;
        if (p!=0) rlen.out().write(len);
        start=p;
        rseq.out().writeByte(next);
        r2p.out().writeInt(p);
      }
      p2r.out().writeInt(run);
    }
    run++;                 // number of runs
    len=n-start;  assert(len>0);  if(len>127) len=-1;
    rlen.out().write(len);
    r2p.out().writeInt(n); // write sentinel
    rseq.close(); rlen.close(); p2r.close(); r2p.close();
    seq=null;
    
    assert(4*run == r2p.length()-4) :
      String.format("n=%d, runs=%d, rseq=%d. 4*run=%d, run2pos=%d, pos2run=%d",
        n, run, rseq.length(), 4*run, r2p.length(), p2r.length());
    return run;
  }
  
// end class
}
