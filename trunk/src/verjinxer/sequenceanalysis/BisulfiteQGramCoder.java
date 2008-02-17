package verjinxer.sequenceanalysis;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * This class is a special QGramCoder that simulates bisulfite treatment on its input sequence.
 * It creates all possible q-grams. The alphabet size is fixed to four.
 * @author Marcel Martin
 */
public final class BisulfiteQGramCoder {
   
   public final QGramCoder coder; // underlying q-gram coder
   private HashSet<Integer> qcodes_bisulfite;    // qcodes for bisulfite treated strand
   private HashSet<Integer> qcodes_bisulfite_rc; // qcodes for rc of bisulfite treated rc (rc: reverse complement)
   private final ArrayList<byte[]> compatibleQGrams; // qgrams bis.-compatible to a given q-gram

   /**
    * Creates a new BisulfiteQGramCoder. The alphabet size is fixed to 4 (A, C, G, T).
    * @param q length of the q-grams coded by this instance
    */
   public BisulfiteQGramCoder(int q) {
      coder = new QGramCoder(q, ASIZE);
      qcodes_bisulfite = new HashSet<Integer>(1<<q);
      qcodes_bisulfite_rc = new HashSet<Integer>(1<<q);
      compatibleQGrams = new ArrayList<byte[]>(1<<(q+1));
      reset();
   }
   
   // (hardcoded) encoding for nucleotides
   public static final byte NUCLEOTIDE_A = 0;    // private enum Nucleotide { A, C, G, T; }
   public static final byte NUCLEOTIDE_C = 1;
   public static final byte NUCLEOTIDE_G = 2;
   public static final byte NUCLEOTIDE_T = 3;
   private static final int ASIZE = 4; // alphabet size
   

   
   /**
    * check wheter a given q-gram matches another given q-gram, 
    * under bisulfite replacement rules.
    * @param qgram given q-gram
    * @param i     starting position within qgram
    * @param s     another given q-gram
    * @param p     starting position within s
    * @return true iff qgram[i..i+q-1] can be derived from s[p..p+q-1] under bisulfite rules.
    */   
   public boolean areCompatible(final byte[] qgram, final int i, final byte[] s, final int p) {
      for (byte[] biqgram: compatibleQGrams(s,p)) 
        if (coder.areCompatible(qgram, i, biqgram, 0)) return true;
      return false; 
   }
   
   /**
    * 
    * @param s
    * @param p
    * @return
    */
   public ArrayList<byte[]> compatibleQGrams(final byte[] s, final int p) {
      throw new UnsupportedOperationException("Not yet implemented");
   }
   
   /**
    * 
    * @param qcode 
    * @return
    */
   public ArrayList<Integer> compatibleQCodes(final int qcode) {
      throw new UnsupportedOperationException("Not yet implemented");
   }

   // =============================================================================================
   
   private int qcode;                            // qcode for regular strand
   private byte previous_nucleotide = -1;
   
   private HashSet<Integer> updateCodes(Collection<Integer> qcodes, byte next) {
      HashSet<Integer> a = new HashSet<Integer>();
      for (int code : qcodes)
         a.add(coder.codeUpdate(code, next));
      return a;
   }

   private HashSet<Integer> updateCodes2(Collection<Integer> qcodes, byte next1, byte next2) {
      HashSet<Integer> a = new HashSet<Integer>();
      for (int code : qcodes) {
         a.add(coder.codeUpdate(code, next1));
         a.add(coder.codeUpdate(code, next2));
      }
      return a;
   }

   /**
    * Takes all qcodes of the collection and adds copies to the collection. In the copies,
    * the last nucleotide has been replaced by new_nucleotide.
    * The last nucleotide is assumed to be old_nucleotide.  
    * @param qcodes
    * @param oldcode
    * @param newcode
    */
   /*	private HashSet<Integer> updateCodes3(Collection<Integer> qcodes, byte old_nucleotide, byte new_nucleotide) {
   // bisulfite codes: take the old codes (which end with 'T')
   // and create a copy with a 'C' in the end
   HashSet<Integer> a = new HashSet<Integer>();
   for (int code : qcodes) {
   a.add(code - old_nucleotide + new_nucleotide);
   }
   return a;
   }
    */
//  private long sizesum = 0;
//  private int count = 0;
   
   
   /** Updates q-gram codes.
    * @param next the next byte in the input.
    * @param after the byte following next in the input.
    * May be an invalid alphabet character if there is no regular character following.  
    */
   public void update(byte next, byte after) {
      // update qcode of unmodified sequence
      qcode = coder.codeUpdate(qcode, next);

      switch (next) {
         case NUCLEOTIDE_A:
         case NUCLEOTIDE_T:
            // if A or T is found, nothing special happens
            qcodes_bisulfite = updateCodes(qcodes_bisulfite, next);
            qcodes_bisulfite_rc = updateCodes(qcodes_bisulfite_rc, next);
            break;

         case NUCLEOTIDE_C:
            // if C is found, then what happens depends on the following nucleotide
            if (after == NUCLEOTIDE_G)
               qcodes_bisulfite = updateCodes2(qcodes_bisulfite, NUCLEOTIDE_C, NUCLEOTIDE_T);
            else
               qcodes_bisulfite = updateCodes(qcodes_bisulfite, NUCLEOTIDE_T);
            qcodes_bisulfite_rc = updateCodes(qcodes_bisulfite_rc, NUCLEOTIDE_C);
            break;

         case NUCLEOTIDE_G:
            qcodes_bisulfite = updateCodes(qcodes_bisulfite, NUCLEOTIDE_G);

            // if C is found, look at the previous nucleotide
            if (previous_nucleotide == NUCLEOTIDE_C)
               qcodes_bisulfite_rc = updateCodes2(qcodes_bisulfite_rc, NUCLEOTIDE_G, NUCLEOTIDE_A);
            else
               qcodes_bisulfite_rc = updateCodes(qcodes_bisulfite_rc, NUCLEOTIDE_A);
            break;

         default:
            // TODO something
            throw new IllegalArgumentException("expecting a valid alphabet character");
      }
      previous_nucleotide = next;
   /*assert(qcodes_bisulfite.contains(qcode));
   assert(qcodes_bisulfite_rc.contains(qcode));
   int size = getCodes().size();
   sizesum += size;
   count++;
   char c;
   AlphabetMap amap = AlphabetMap.DNA();
   try {
   c = amap.preimage(next);
   } catch (InvalidSymbolException e) {
   c = 'x';
   }
   System.out.format("next is %c. number of qcodes now %d. sum %d. count %d. avg %f%n", c, size, sizesum, count, ((double)sizesum)/count);
   for (int code : getCodes()) {
   System.out.println(coder.qGramString(code, amap));
   }*/
   }

   //public QGramCoder getCoder() {
   //   return coder;
   //}

   public void reset() {
      qcode = 0;
      qcodes_bisulfite.clear();
      qcodes_bisulfite_rc.clear();
      qcodes_bisulfite.add(0);         //TODO: why add 0 = AAAAAA?
      qcodes_bisulfite_rc.add(0);      //TODO: why?
      previous_nucleotide = -1;
   }

   /*
   public void update(byte next) {
   // regular qcode always gets updated
   qcode = coder.codeUpdate(qcode, next);
   switch (next) {
   case NUCLEOTIDE_A:
   case NUCLEOTIDE_T:
   // if A or T is found, nothing special happens
   qcodes_bisulfite = updateCodes(qcodes_bisulfite, next);
   qcodes_bisulfite_rc = updateCodes(qcodes_bisulfite_rc, next);
   break;
   case NUCLEOTIDE_C:
   qcodes_bisulfite = updateCodes(qcodes_bisulfite, NUCLEOTIDE_T);
   qcodes_bisulfite_rc = updateCodes(qcodes_bisulfite_rc, NUCLEOTIDE_C);
   break;
   case NUCLEOTIDE_G:
   if (previous_nucleotide == NUCLEOTIDE_C) {
   qcodes_bisulfite_rc = updateCodes2(qcodes_bisulfite_rc, NUCLEOTIDE_A, NUCLEOTIDE_G);
   qcodes_bisulfite = updateCodes3(qcodes_bisulfite, NUCLEOTIDE_T, NUCLEOTIDE_C);
   } else {
   qcodes_bisulfite = updateCodes(qcodes_bisulfite, NUCLEOTIDE_G);
   qcodes_bisulfite_rc = updateCodes(qcodes_bisulfite_rc, NUCLEOTIDE_A);
   }
   break;
   default:
   // TODO something
   System.exit(-1);
   }
   previous_nucleotide = next;
   }
    */
   public Collection<Integer> getCodes() {
      HashSet<Integer> codes = new HashSet<Integer>();
      codes.add(qcode);
      codes.addAll(qcodes_bisulfite);
      codes.addAll(qcodes_bisulfite_rc);

      return codes;
   }

   public static void main(String[] args) {
      AlphabetMap alphabetmap = AlphabetMap.DNA();
      QGramCoder coder = new QGramCoder(5, 4); // q=5
      InputStream in = System.in;
      //ByteBuffer buf = new 
      int c;
      int code = 0;
      try {
         while ((c = in.read()) != -1) {
            if (c == '\n')
               continue;
            code = coder.codeUpdate(code, alphabetmap.code((byte) c));
            System.out.println(code);
         }
      } catch (IOException e) {
         System.exit(1);
      } catch (InvalidSymbolException e) {
         System.err.println("invalid symbol");
         System.exit(1);
      }
   }
}
