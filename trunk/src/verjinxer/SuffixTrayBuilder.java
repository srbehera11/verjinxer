package verjinxer;

import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.sequenceanalysis.ISuffixDLL;
import verjinxer.sequenceanalysis.Sequences;
import verjinxer.sequenceanalysis.SuffixDLL;
import verjinxer.sequenceanalysis.SuffixXorDLL;

/**
 * @author Markus Kemmerling
 */
public class SuffixTrayBuilder {

   private SuffixXorDLL xordll;
   private SuffixDLL normaldll;
   private boolean getNormalDLL = false;
   private long steps;
   private final byte[] sequence;
   private final int n;
   private final Alphabet alphabet;

   public SuffixTrayBuilder(Sequences sequence, Alphabet alphabet) {
      this.sequence = sequence.array();
      this.alphabet = alphabet;
      this.n = (int)sequence.length();
   }

   public long getSteps() {
      return steps;
   }

   public ISuffixDLL getSuffixDLL() {
      return getNormalDLL ? normaldll : xordll;
   }

   /**
    * 
    * @param method
    * @throws IllegalArgumentException
    */
   public void build(String method) throws  IllegalArgumentException{
      if (method.equals("L")) {
         //buildpos_L();
         throw new UnsupportedOperationException("Method " + method + "is temporary not supported.");
      } else if (method.equals("R")) {
         //buildpos_R();
         throw new UnsupportedOperationException("Method " + method + "is temporary not supported.");
      } else if (method.equals("minLR")) {
         buildpos_minLR();
      } else if (method.equals("bothLR")) {
         buildpos_bothLR();
      } else if (method.equals("bothLR2")) {
         //buildpos_bothLR2();
         throw new UnsupportedOperationException("Method " + method + "is temporary not supported.");
      } else {
         throw new IllegalArgumentException("The Method " + method + " does not exist.");
      }
   }

   private void buildpos_minLR() {
      normaldll = new SuffixDLL(n);
      getNormalDLL = true;
      byte ch;
      int chi;
      int pup, pdown;
      int lsp, lpp;
      int found = 0;

      for (int p = n - 1; p >= 0; p--) {
         // insert suffix starting at position p
         ch = sequence[p];
         chi = ch + 128;
         if (normaldll.getFirstPos(chi) == -1) { // seeing character ch for the first time
            normaldll.insertnew(chi, p);
            steps++;
         } else { // seeing character ch again
            assert (normaldll.getFirstPos(chi) > p);
            assert (normaldll.getLastPos(chi) > p);
            if (alphabet.isSpecial(ch)) { // special character: always inserted first
               normaldll.insertasfirst(chi, p);
               steps++;
            } else { // symbol character: proceed normally
               pup = pdown = p + 1;
               for (found = 0; found == 0;) {
                  steps++;
                  lpp = normaldll.getLexPreviousPos(pup);
                  if (lpp == -1) {
                     found = 1;
                     break;
                  } // new first
                  if (sequence[(pup = lpp) - 1] == ch) {
                     found = 2;
                     break;
                  } // insert after pup
                  steps++;
                  lsp = normaldll.getLexNextPos(pdown);
                  if (lsp == -1) {
                     found = 3;
                     break;
                  } // new last
                  if (sequence[(pdown = lsp) - 1] == ch) {
                     found = 4;
                     break;
                  } // insert before pdown
               }
               pup--;
               pdown--;
               switch (found) {
               case 1:
                  normaldll.insertasfirst(chi, p);
                  break;
               case 2:
                  if (normaldll.getLastPos(chi) == pup) {
                     normaldll.insertaslast(chi, p);
                  } else {
                     normaldll.insertbetween(pup, normaldll.getLexNextPos(pup), p);
                  }
                  break;
               case 3:
                  normaldll.insertaslast(chi, p);
                  break;
               case 4:
                  if (normaldll.getFirstPos(chi) == pdown) {
                     normaldll.insertasfirst(chi, p);
                  } else {
                     normaldll.insertbetween(normaldll.getLexPreviousPos(pdown), pdown, p);
                  }
                  break;
               default:
                  //TODO g.terminate("suffixtray: internal error");
               } // end switch
            } // end symbol character
         } // end seeing character ch again
      }
   }

   private void buildpos_bothLR() {
      xordll = new SuffixXorDLL(n);
      getNormalDLL =false;
      
      byte ch;
      int chi;

      for (int p = n - 1; p >= 0; p--) {
         // insert suffix starting at position p
         ch = sequence[p]; 
         chi = ch + 128;
         if (xordll.getFirstPos(chi) == -1) { // seeing character ch for the first time
            xordll.insertnew(chi, p);
            steps++;
         } else { // seeing character ch again
            assert (xordll.getFirstPos(chi) > p);
            assert (xordll.getLastPos(chi) > p);
            if (alphabet.isSpecial(ch)) { // special character: always inserted first
               xordll.insertasfirst(chi, p);
               steps++;
            } else { // symbol character: proceed normally
               walkandinsert(chi, p);
            } // end symbol character
         } // end seeing character ch again
         // showpos_R(String.format("List after step %d: [%d]%n", p, s[p])); // DEBUG
      } // end for p
   }

   private void walkandinsert(int chi, int p) {
      final byte ch = (byte) (chi - 128);
      int founddown, foundup, qdn, qup, pup, pdn, ppred, psucc;
      // get pup, pdn, ppred and psucc from SuffixDLL (internal state - is set while insertion)
      pup = pdn = xordll.getCurrentPosition();
      ppred = xordll.getPredecessor();
      psucc = xordll.getSuccessor();
      // now lexicographically (ppred < pup == pdn < psucc)
      for (founddown = 0, foundup = 0; founddown == 0 || foundup == 0;) {
         if (founddown == 0) { // walk down
            steps++;
            qdn = xordll.getLexPS(pdn) ^ ppred;
            if (qdn == -1) {
               xordll.insertaslast(chi, p);
               break;
            } // i is new last
            if (sequence[qdn - 1] == ch) {
               founddown = 2;
            } // insert before pdn
            ppred = pdn;
            pdn = qdn;
         }
         if (foundup == 0) { // walk up
            steps++;
            qup = xordll.getLexPS(pup) ^ psucc;
            if (qup == -1) {
               xordll.insertasfirst(chi, p);
               break;
            } // i is new first
            if (sequence[qup - 1] == ch) {
               foundup = 2;
            } // insert after pup
            psucc = pup;
            pup = qup;
         }
      }
      if (founddown != 0 && foundup != 0) { // insert i at found position
         pup--;
         pdn--;
         xordll.insertbetween(pup, pdn, p);
      }
   }

}
