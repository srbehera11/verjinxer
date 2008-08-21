package verjinxer.sequenceanalysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * This class is a special QGramCoder that simulates bisulfite treatment on its input sequence.
 * It creates all possible q-grams. The alphabet size is fixed to four.
 * 
 * Make sure that different threads use different instances of a BisulfiteQGramCoder.
 * 
 * @author Marcel Martin
 * @author Sven Rahmann
 */
public final class BisulfiteQGramCoder {
   
   public final QGramCoder coder; // underlying q-gram coder
   public final int q;            // q-gram length
   private HashSet<Integer> qcodes_bisulfite;    // qcodes for bisulfite treated strand
   private HashSet<Integer> qcodes_bisulfite_rc; // qcodes for rc of bisulfite treated rc (rc: reverse complement)
   private final ArrayList<int[][]> compatibleQCodes; // qgrams bis.-compatible to a given q-gram
   private final ArrayList<Integer> fwdCompatibleQCodes;
   private final ArrayList<Integer> rrcCompatibleQCodes;
   private final byte[] tmpQGram;
   
   /**
    * Creates a new BisulfiteQGramCoder. The alphabet size is fixed to 4 (A, C, G, T).
    * @param q length of the q-grams coded by this instance
    */
   public BisulfiteQGramCoder(int q) {
      coder = new QGramCoder(q, ASIZE);
      this.q = q;
      qcodes_bisulfite = new HashSet<Integer>(1<<q);
      qcodes_bisulfite_rc = new HashSet<Integer>(1<<q);
      compatibleQCodes = new ArrayList<int[][]>(1<<(q+1));     
      computeCompatibleQCodes();
      final int zweiHochQHalbe = (1<<((q+1)/2)) + 2; // 2^ceil(q/2) plus some extra space
      fwdCompatibleQCodes = new ArrayList<Integer>(zweiHochQHalbe);
      rrcCompatibleQCodes = new ArrayList<Integer>(zweiHochQHalbe);
      tmpQGram = new byte[q];
      reset();
   }

   /** 
    * For each q-code, computes the q-codes that are
    * compatible under bisulfite treatment rules.
    * For each q-code, there are four different possible
    * sets of compatible q-codes, depending on whether
    * the corresponding q-gram is preceded by a C or
    * followed by a G. The four different combinations are
    * numbered as follows:
    * 0: no C | qgram | no G
    * 1: no C | qgram | G
    * 2: C    | qgram | no G
    * 3: C    | qgram | G
    * 
    * compatibleQCodes accordingly contains, at each position, a four-element
    * array of int arrays.
    */
   private void computeCompatibleQCodes() {
      compatibleQCodes.clear();
      for (int c = 0; c < coder.numberOfQGrams; ++c) {
         byte qgram[] = coder.qGram(c);
         
         // case 1: no C before q-gram, no G after q-gram
         reset();
         for (int i = 0; i < qgram.length-1; ++i) update(qgram[i], qgram[i+1]);
         update(qgram[qgram.length-1], NUCLEOTIDE_A);
         Collection<Integer> qcodesNoCNoG = getCodes();

         // case 2: no C before q-gram, but G after q-gram
         reset();
         for (int i = 0; i < qgram.length-1; ++i) update(qgram[i], qgram[i+1]);
         update(qgram[qgram.length-1], NUCLEOTIDE_G);
         Collection<Integer> qcodesNoCG = getCodes();

         // case 2: C before q-gram, no G after q-gram
         reset();
         update(NUCLEOTIDE_C, qgram[0]);
         for (int i = 0; i < qgram.length-1; ++i) update(qgram[i], qgram[i+1]);
         update(qgram[qgram.length-1], NUCLEOTIDE_A);
         Collection<Integer> qcodesCNoG = getCodes();
         
         // case 4: C before q-gram and G after q-gram
         reset();
         update(NUCLEOTIDE_C, qgram[0]);
         for (int i = 0; i < qgram.length-1; ++i) update(qgram[i], qgram[i+1]);
         update(qgram[qgram.length-1], NUCLEOTIDE_G);
         Collection<Integer> qcodesCG = getCodes();

         reset();

         // add found codes to compatibleQCodes
         int[][] tmp = new int[4][];
         tmp[0] = new int[qcodesNoCNoG.size()];
         tmp[1] = new int[qcodesNoCG.size()];
         tmp[2] = new int[qcodesCNoG.size()];
         tmp[3] = new int[qcodesCG.size()];
         
         int i = 0;
         for (int x : qcodesNoCNoG) {
            tmp[0][i++] = x;
         }
         i = 0;
         for (int x : qcodesNoCG) {
            tmp[1][i++] = x;
         }
         i = 0;
         for (int x : qcodesCNoG) {
            tmp[2][i++] = x;
         }
         i = 0;
         for (int x : qcodesCG) {
            tmp[3][i++] = x;
         }
         compatibleQCodes.add(tmp);
      }    
   }

   // (hardcoded) encoding for nucleotides
   public static final byte NUCLEOTIDE_A = 0;
   public static final byte NUCLEOTIDE_C = 1;
   public static final byte NUCLEOTIDE_G = 2;
   public static final byte NUCLEOTIDE_T = 3;
   private static final int ASIZE = 4; // alphabet size


   /**
    * Checks whether a given q-gram matches another given q-gram, 
    * under bisulfite replacement rules.
    * @param qgram given q-gram (in index, bis.-treated)
    * @param i     starting position within qgram
    * @param s     another given q-gram (original, in text)
    * @param p     starting position within s
    * @return true iff qgram[i..i+q-1] can be derived from s[p..p+q-1] under bisulfite rules.
    */   
   public boolean areCompatible(final byte[] qgram, final int i, final byte[] s, final int p) {
      int biCode = coder.code(qgram, i);
      int type = 0;
      if (p > 0 && s[p-1] == NUCLEOTIDE_C) type = 2;
      if (p+coder.q < s.length && s[p+coder.q] == NUCLEOTIDE_G) type += 1;
      
      for (int code : compatibleQCodes.get(coder.code(s, p))[type]) {
         if (code == biCode) return true;
      }
      return false; 
   }
   
   /**
    * Returns 
    * @param s
    * @param p
    * @return
    */
   /*public ArrayList<byte[]> compatibleQGrams(final byte[] s, final int p) {
      throw new UnsupportedOperationException("Not yet implemented");
   }*/
   
   /**
    * For a given q-gram code (with alphabet size 4),
    * generate all q-gram codes that can arise from it by bisulfite treatment.
    * (v1) all Cs -> T.
    * (v2) all Cs -> T, except CG remains CG;
    *      If there is a C after the last position, we need more information.
    * (v3) all Gs -> A.
    * (v4) all Gs -> A, except CG remains CG;
    *     If there is a G before the first q-gram position, we need more information.
    * @param qcode           the original q-gram code.
    * @param reverse         if true, produce revcomp-bisulfite-revcomp instead of forward-bisulfite q-gram codes
    * @param specialBorder   indicate whether there is a C after the last (if reverse, a G before the first) position
    * @return a list of bisulfite-treated q-gram codes for the given q-gram code.
    */
   public ArrayList<Integer> bisulfiteQCodes(final int qcode, final boolean reverse, final boolean specialBorder) {
      ArrayList<Integer> result = null;
      int loneReactions = 0;
      coder.qGram(qcode, tmpQGram); // get the q-gram into tmpQGram (which is pre-allocated)
 
      if (!reverse) {  // forward
         result = fwdCompatibleQCodes;
         result.clear();
         if (qcode<0) return result;
         result.add(0);
         for(int p=0; p<q; p++) {
            final byte ch = tmpQGram[p];
            final byte chbi = (ch==NUCLEOTIDE_C)? NUCLEOTIDE_T : ch;
            final boolean GFollows = (p<q-1 && tmpQGram[p+1]==NUCLEOTIDE_G) || (p==q-1 && specialBorder);
            final boolean isCG = (ch==NUCLEOTIDE_C && GFollows);
            final int L = result.size();
            for (int l=0; l<L; l++) {
               final int shifted = result.get(l)*4;
               result.set(l,  shifted + chbi);
               if (isCG) result.add(shifted+NUCLEOTIDE_C);
            }
            if (ch==NUCLEOTIDE_C && !GFollows) loneReactions++;
         }
      } else {         // reverse
         result = rrcCompatibleQCodes;
         result.clear();
         if (qcode<0) return result;
         result.add(0);
         for(int p=0; p<q; p++) {
            final byte ch = tmpQGram[p];
            final byte chbi = (ch==NUCLEOTIDE_G)? NUCLEOTIDE_A : ch;
            final boolean CBefore = (p>0 && tmpQGram[p-1]==NUCLEOTIDE_C) || (p==0 && specialBorder);
            final boolean isCG = (ch==NUCLEOTIDE_G && CBefore);
            final int L = result.size();
            for (int l=0; l<L; l++) {
               final int shifted = result.get(l)*4;
               result.set(l, shifted + chbi);
               if (isCG) result.add(shifted+NUCLEOTIDE_G);
            }
            if (ch==NUCLEOTIDE_G && !CBefore) loneReactions++;
         }
      } // end else (reverse case)
      if (loneReactions==0) { 
         // no lone reactions: we have re-generated the original q-code, too.
         // the original q-code must be the last one in the list -> remove it.
        final int last = result.size()-1;
        assert result.get(last) == qcode; // last inserted must be equal to original
        result.remove(last);
      }
      return result;
   }

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
    * Updates q-gram codes.
    * @param next the next byte in the input
    * @param after the byte following next in the input.
    * May be an invalid alphabet character if there is no regular character following.
    * @throws IllegalArgumentException if next is not a regular alphabet symbol.
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
            throw new IllegalArgumentException("expecting a valid alphabet character");
      }
      previous_nucleotide = next;
   }

   public void reset() {
      qcode = 0;
      qcodes_bisulfite.clear();
      qcodes_bisulfite_rc.clear();
      qcodes_bisulfite.add(0);         //TODO: why add 0 = AAAAAA?
      qcodes_bisulfite_rc.add(0);      //TODO: why?
      previous_nucleotide = -1;
   }

   public Collection<Integer> getCodes() {
      HashSet<Integer> codes = new HashSet<Integer>();
      codes.add(qcode);
      codes.addAll(qcodes_bisulfite);
      codes.addAll(qcodes_bisulfite_rc);

      return codes;
   }

}
