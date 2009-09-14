package verjinxer;

import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.sequenceanalysis.ISuffixDLL;
import verjinxer.sequenceanalysis.Sequences;
import verjinxer.sequenceanalysis.SuffixDLL;
import verjinxer.sequenceanalysis.SuffixXorDLL;

/**
 * Class responsible for building a suffix array of a text/sequence.
 * 
 * @author Markus Kemmerling
 */
public class SuffixTrayBuilder {

   private SuffixXorDLL xordll;
   private SuffixDLL normaldll;
   
   /** Whether a normal suffix list was created.*/
   private boolean getNormalDLL = false;
   
   /** Number of steps needed to build the suffix tray. */
   private long steps;
   
   /** Text/Sequence for that a suffix tray shall be build. */
   private final Sequences sequence;
   
   /** Length of the Text/Sequence */
   private final int n;
   
   /** Alphabet of the Text/Sequence */
   private final Alphabet alphabet;

   /**
    * Creates a new building instance for the given sequence.<br>
    * It is necessary that the sequence has a special character which is greater or lower than all
    * normal characters as last element. If this is not the case, the resulting suffix list can be
    * wrong or the building process itself can fail with an exception.
    * 
    * @param sequence
    *           Text/Sequence for that a suffix tray shall be build.
    * @param alphabet
    *           Alphabet of sequence.
    */
   public SuffixTrayBuilder(Sequences sequence, Alphabet alphabet) {
      this.n = (int) sequence.length();
      assert (alphabet.isSeparator(sequence.array()[n - 1]));
      this.sequence = sequence;
      this.alphabet = alphabet;
   }

   /**
    * @return  Number of steps needed to build the suffix tray.
    */
   public long getSteps() {
      return steps;
   }

   /**
    * @return The builded suffix list or null, if the build method was not invoked before.
    */
   public ISuffixDLL getSuffixDLL() {
      return getNormalDLL ? normaldll : xordll;
   }

   /**
    * Builds the suffix list.
    * 
    * @param method
    *           How the suffix list shall be build. Valid methods are 'L', 'R', 'minLR', 'bothLR'
    *           and 'bothLR2'.
    * @throws IllegalArgumentException
    *            when the given method is not valid.
    */
   public void build(String method) throws IllegalArgumentException {
      if (method.equals("L")) {
         buildpos_L();
      } else if (method.equals("R")) {
         buildpos_R();
      } else if (method.equals("minLR")) {
         buildpos_minLR();
      } else if (method.equals("bothLR")) {
         buildpos_bothLR();
      } else if (method.equals("bothLR2")) {
         buildpos_bothLR2();
      } else {
         throw new IllegalArgumentException("Unsupported construction method '" + method + "'!");
      }
   }

   /**
    * Builds a suffix array by walking LEFT along a partially constructed doubly linked suffix
    * list.
    */
   private void buildpos_L() {
      normaldll = new SuffixDLL(sequence, alphabet);
      getNormalDLL = true;
      byte ch;
      int chi;

      for (int p = n - 1; p >= 0; p--) {
         // insert suffix starting at position p
         ch = sequence.array()[p];
         chi = ch + 128;
         if (normaldll.getFirstPos(chi) == -1) { // seeing character ch for the first time
            normaldll.insertnew(chi, p);
            steps++;
         } else { // seeing character ch again
            assert normaldll.getFirstPos(chi) > p;
            assert normaldll.getLastPos(chi) > p;
            if (alphabet.isSpecial(ch)) { // special character: always inserted first
               normaldll.insertasfirst(chi, p);
               steps++;
            } else { // symbol character: proceed normally
               int i = p + 1;
               steps++;
               while (normaldll.getLexPreviousPos(i) != -1
                     && sequence.array()[(i = normaldll.getLexPreviousPos(i)) - 1] != ch) {
                  steps++;
               }
               i--;
               if (sequence.array()[i] == ch && i != p) { // insert p after i, might be new last
                  if (normaldll.getLastPos(chi) == i) {
                     normaldll.insertaslast(chi, p);
                  } else {
                     normaldll.insertbetween(i, normaldll.getLexNextPos(i), p);
                  }
               } else { // p is new first
                  normaldll.insertasfirst(chi, p);
               }
            } // end symbol character
         } // end seeing character ch again
         // showpos_R(String.format("List after step %d: [%d]%n", p, sequence[p])); // DEBUG
      } // end for p
   }

   /**
    * Builds a suffix array by walking RIGHT in a partially constructed doubly linked suffix
    * list.
    */
   private void buildpos_R() {
      normaldll = new SuffixDLL(sequence, alphabet);
      getNormalDLL = true;
      byte ch;
      int chi;

      for (int p = n - 1; p >= 0; p--) {
         // insert suffix starting at position p
         ch = sequence.array()[p];
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
               int i = p + 1;
               steps++;
               while (normaldll.getLexNextPos(i) != -1
                     && sequence.array()[(i = normaldll.getLexNextPos(i)) - 1] != ch) {
                  steps++;
               }
               i--;
               if (sequence.array()[i] == ch && i != p) { // insert p BEFORE i, might be new first
                  if (normaldll.getFirstPos(chi) == i) {
                     normaldll.insertasfirst(chi, p);
                  } else {
                     normaldll.insertbetween(normaldll.getLexPreviousPos(i), i, p);
                  }
               } else { // p is new LAST
                  normaldll.insertaslast(chi, p);
               }
            } // end symbol character
         } // end seeing character ch again
         // showpos_R(String.format("List after step %d: [%d]%n", p, s[p])); // DEBUG
      } // end for p
   }

   /**
    * Builds a suffix array by walking BIDIRECTIONALLY along a partially constructed doubly
    * linked suffix list until the first matching character is found in EITHER direction.
    */
   private void buildpos_minLR() {
      normaldll = new SuffixDLL(sequence, alphabet);
      getNormalDLL = true;
      byte ch;
      int chi;
      int pup, pdown;
      int lsp, lpp;
      int found = 0;

      for (int p = n - 1; p >= 0; p--) {
         // insert suffix starting at position p
         ch = sequence.array()[p];
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
                  if (sequence.array()[(pup = lpp) - 1] == ch) {
                     found = 2;
                     break;
                  } // insert after pup
                  steps++;
                  lsp = normaldll.getLexNextPos(pdown);
                  if (lsp == -1) {
                     found = 3;
                     break;
                  } // new last
                  if (sequence.array()[(pdown = lsp) - 1] == ch) {
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
                  // TODO g.terminate("suffixtray: internal error");
               } // end switch
            } // end symbol character
         } // end seeing character ch again
      }
   }

   /**
    * Builds a suffix array by walking BOTH WAYS along a partially constructed doubly linked
    * suffix list, until the target character is found BOTH WAYS. This implementation uses 2 integer
    * arrays. The SPACE SAVING technique (xor encoding) is not used here. Use
    * <code>buildpos_bothLR</code> for the space saving technique.
    */
   private void buildpos_bothLR2() {
      normaldll = new SuffixDLL(sequence, alphabet);
      getNormalDLL = true;
      byte ch;
      int chi;
      int pup, pdown;
      int lsp, lpp;
      int foundup = 0;
      int founddown = 0;

      for (int p = n - 1; p >= 0; p--) {
         // insert suffix starting at position p
         ch = sequence.array()[p];
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
               for (founddown = 0, foundup = 0; founddown == 0 || foundup == 0;) {
                  if (founddown == 0) {
                     steps++;
                     lsp = normaldll.getLexNextPos(pdown);
                     if (lsp == -1) {
                        founddown = 1;
                        foundup = 2;
                        break;
                     } // new last
                     if (sequence.array()[(pdown = lsp) - 1] == ch) {
                        founddown = 2;
                     } // insert before pdown
                  }
                  if (foundup == 0) {
                     steps++;
                     lpp = normaldll.getLexPreviousPos(pup);
                     if (lpp == -1) {
                        foundup = 1;
                        founddown = 2;
                        break;
                     } // new first
                     if (sequence.array()[(pup = lpp) - 1] == ch) {
                        foundup = 2;
                     } // insert after pup
                  }
               }
               if (founddown == 1) { // new last
                  normaldll.insertaslast(chi, p);
               } else if (foundup == 1) { // new first
                  normaldll.insertasfirst(chi, p);
               } else {
                  pup--;
                  pdown--; // normal insert at found position
                  normaldll.insertbetween(pup, pdown, p);
               }
            } // end symbol character
         } // end seeing character ch again
         // showpos_R(String.format("List after step %d: [%d]%n", p, s[p])); // DEBUG
      } // end for p
   }

   /**
    * Builds suffix array by walking BOTH WAYS along a partially constructed doubly linked
    * suffix list, until the target character is found BOTH WAYS. This implementation uses the SPACE
    * SAVING xor trick.
    */
   private void buildpos_bothLR() {
      xordll = new SuffixXorDLL(sequence, alphabet);
      getNormalDLL = false;

      byte ch;
      int chi;

      for (int p = n - 1; p >= 0; p--) {
         // insert suffix starting at position p
         ch = sequence.array()[p];
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
            if (sequence.array()[qdn - 1] == ch) {
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
            if (sequence.array()[qup - 1] == ch) {
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
