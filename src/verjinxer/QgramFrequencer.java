/*
 * QgramFrequencer.java
 *
 * Created on 18. April 2007, 20:30
 *
 */

package verjinxer;

import static verjinxer.Globals.programname;

import java.io.IOException;
import java.util.Arrays;

import com.spinn3r.log5j.Logger;

import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.sequenceanalysis.InvalidSymbolException;
import verjinxer.sequenceanalysis.QGramCoder;
import verjinxer.sequenceanalysis.QGramFilter;
import verjinxer.util.IllegalOptionException;
import verjinxer.util.Options;
import verjinxer.util.ProjectInfo;
import verjinxer.util.Sortable;
import verjinxer.util.Sorter;
import verjinxer.util.TicToc;

/**
 *
 * @author Sven Rahmann
 */
public class QgramFrequencer {
  private static final Logger log = Globals.getLogger();
  private Globals g;
  
  /** creates a new instance of QgramFrequencer
   * @param gl  the globals object to use
   */
  public QgramFrequencer(Globals gl) {
    g = gl;
  }
  
  /** if run independently, call main
   * @param args  the command line arguments
   */
  public static void main(String[] args) {
    new QgramFrequencer(new Globals()).run(args);
  }
  
  /**
   * print help on usage
   */
  public void help() {
    log.info("Usage:  %s qfreq  [options]  indexname", programname);
    log.info("Finds the most frequent q-grams in a q-gram index");
    log.info("Options:");
    log.info("  -s, --sequences     base counts on number of sequences, not words");
    log.info("  -n  <n>|<'all'>     output at least the top n most frequent q-grams [30]");
    log.info("  -N  <N>             output at most N q-grams [all]");
    log.info("  -r, --reverse       output the LEAST instead of most frequent q-grams");
    log.info("  -p, --prefix  <p>   report only words starting with p, where |p|<=q");
    log.info("  -F, --filter  <c:d> filter q-grams by complexity <c> and delta <d>");
  }
  
  /* Variables */
  boolean countseq = false;
  byte[]  word     = null;
  boolean rev      = false;
  int asize;
  int q;
  Alphabet alphabet  = null;
  
  int Lcode = 0;
  int Hcode = 0;
  int len   = 0;
  int[] f   = null;
  int[] p   = null;
  int num   = 30;
  int NUM   = 0;
  
  /**
   * @param args the command line arguments
   * @return zero on success, nonzero if there is a problem
   */
  public int run(String[] args) {
    TicToc gtimer = new TicToc();
    g.cmdname = "qfreq";
    int returnvalue = 0;
    Options opt = new Options("F=filter:,s=sequences,p=prefix:,n=number:,N=Number:,r=rev=reverse");
    try {
      args = opt.parse(args);
    } catch (IllegalOptionException ex) {
      log.error("qfreq: "+ex); return 1;
    }
    if (args.length==0) {
      help(); log.error("qfreq: no index given"); return 0;
    }
    
    // Get indexname and di
    String indexname = args[0];
    String di        = g.dir + indexname;
    g.startProjectLogging(di);
    if (args.length>1) log.warn("qfreq: ignoring all arguments except first '%s'", args[0]);
    
    // Determine options values
    rev      = (opt.isGiven("r"));
    countseq = (opt.isGiven("s"));
    String wordstring = (opt.isGiven("p")? opt.get("p") : null);
    
    // Read project data and determine asize, q; read alphabet map
    ProjectInfo project;
    try {
       project = ProjectInfo.createFromFile(di);
    } catch (IOException ex) {
       log.error("qfreq: cannot read project file.");
       return 1;
    }
    try {
      asize = project.getIntProperty("qAlphabetSize");
      q = project.getIntProperty("q");
    } catch (NumberFormatException ex) {
      log.error("qfreq: q-grams for index '%s' not found. (Re-create the q-gram index!)", di);
      return 1;
    }
    alphabet = g.readAlphabet(di+FileNameExtensions.alphabet);
    final QGramCoder coder = new QGramCoder(q,asize);
    final int aq = coder.numberOfQGrams;
    
    // Determine num and NUM
    if (opt.isGiven("n")) {
      if (opt.get("n").startsWith("a")) num = aq;
      else num = Integer.parseInt(opt.get("n"));
    }
    if (opt.isGiven("N")) {
      NUM = Integer.parseInt(opt.get("N"));
    } else NUM = aq;
    
    
    // Determine which q-gram codes to examine (Lcode, Hcode)
    Lcode = 0;
    Hcode = aq;
    byte[] qgram = null;
    if (wordstring!=null) {
      if (wordstring.length()>q) {
        log.warn("qfreq: only considering first %d characters of '%s': ", q, wordstring);
        wordstring = wordstring.substring(0,q);
        log.warn("%s", wordstring);
      }
      try {
        qgram = alphabet.applyTo(wordstring, false);
      } catch (InvalidSymbolException ex) {
        log.error("qfreq: given prefix is not a string over the alphabet.");
        return 1;
      }
      assert (qgram.length<=q) : "qgram array too big";
      if (qgram.length<q) qgram = Arrays.copyOf(qgram,q);
      assert (qgram.length==q) : "qgram array doesn't have length q";
      Lcode = coder.code(qgram);
      for (int i=wordstring.length(); i<q; i++) qgram[i]=(byte)(asize-1);
      Hcode = coder.code(qgram) + 1;
    }
    log.info("qfreq: considering q-grams %d..%d", Lcode, Hcode);
    
    // Read the correct array!
    if (countseq) f = g.slurpIntArray(di+FileNameExtensions.qseqfreq);
    else f = g.slurpIntArray(di+FileNameExtensions.qfreq);
    //TODO: read bck array if requested, also look at sequences bck-based!
    
    len = Hcode - Lcode;
    log.info("qfreq: all files read after %.1f sec; now sorting %d q-grams...", gtimer.tocs(),len);

    final QGramFilter filter = new QGramFilter(q, asize, opt.get("F"));
    log.info("qfreq: filtering out %d / %d q-grams", filter.cardinality(), aq);
    for (int i=0; i<aq; i++) if (filter.isFiltered(i)) f[i]=0;
    //for (int i = filter.nextSetBit(0); i >= 0; i = filter.nextSetBit(i+1)) f[i]=0;
    
    // sort frequencies, generate soring permutation
    log.info("qfreq: considering q-grams %d..%d", Lcode, Hcode);
    p = new int[len];
    for(int i=0; i<len; i++) p[i]=i+Lcode;
    
    TicToc timer = new TicToc();
    //new Sorter( rev? new ByReverseFrequency() : new ByFrequency() ).heapsort();
    //log.info("qfreq: heapsorting took %.1f sec", timer.tocs());
    //timer.tic();
    new Sorter( rev? new ByReverseFrequency() : new ByFrequency() ).quicksort();
    log.info("qfreq: quicksorting took %.1f sec", timer.tocs());
    
    // Output...
    if(num>len) num = len;
    if(NUM>len) NUM = len;
    int limitf = f[num+Lcode-1];
    if (!rev)  {for (num=0; f[num+Lcode]>=limitf && num<NUM; num++) {}}
    else       {for (num=0; f[num+Lcode]<=limitf && num<NUM; num++) {}}
    log.info("qfreq: showing top %d %d-grams:::", num,q);
    for (int i=0; i<num; i++) {
      System.out.printf("%s %9d%n", coder.qGramString(p[i],alphabet), f[i+Lcode]);
    }
    
    g.stopplog();
    return returnvalue;
  }
  
  
  /***************************************/
  /* Sorting as sub-classes of this one: */
  
  final class ByFrequency implements Sortable {
    public final int length() {
      return len;
    }
    
    public final int compare(final int i, final int j) {
      final int d = f[j+Lcode] - f[i+Lcode];
      if (d!=0) return d;
      return p[i]-p[j];
    }
    
    public final void swap(final int i, final int j) {
      int tmp = f[i+Lcode];
      f[i+Lcode] = f[j+Lcode];
      f[j+Lcode] = tmp;
      tmp = p[i];
      p[i] = p[j];
      p[j] = tmp;
    }
  }

  
  final class ByReverseFrequency implements Sortable {
    public final int length() {
      return len;
    }
    
    public final int compare(final int i, final int j) {
      final int d = f[i+Lcode] - f[j+Lcode];
      if (d!=0) return d;
      return p[i]-p[j];
    }
    
    public final void swap(final int i, final int j) {
      int tmp = f[i+Lcode];
      f[i+Lcode] = f[j+Lcode];
      f[j+Lcode] = tmp;
      tmp = p[i];
      p[i] = p[j];
      p[j] = tmp;
    }
  }

}
