package verjinxer.sequenceanalysis;

import java.util.Vector;

/**
 * An Automaton that decides whether a query sequence matches a reference sequence.
 * Therefore it gets oppositely character by character as input.
 * 
 * @author Markus Kemmerling
 *
 */
public abstract class MatchAutomaton {
   
   // encodings for dinucleotides:
   // XY := encode(X)<<2 + encode(Y)
   private static final int AA = 0;
   private static final int AC = 1;
   private static final int AG = 2;
   private static final int AT = 3;
   private static final int CA = 4;
   private static final int CC = 5;
   private static final int CG = 6;
   private static final int CT = 7;
   private static final int GA = 8;
   private static final int GC = 9;
   private static final int GG = 10;
   private static final int GT = 11;
   private static final int TA = 12;
   private static final int TC = 13;
   private static final int TG = 14;
   private static final int TT = 15;
   
   /** Error state (not accepting sink)**/
   protected static final int ERROR = -1;
   
   /** Accepting state **/
   protected static final int ACCEPT = 0;
   
   /** Current state of the automaton**/
   private int state;

   /**
    * Sets the state of the automaton.
    * 
    * @param state
    *           The new state of the automaton.
    */
   protected final void setState(int state) {
      this.state = state;
   }
   
   /**
    * @param state
    * @return Whether the automaton has the given state.
    */
   protected final boolean hasState(int state) {
      return this.state == state;
   }

   /**
    * @return Whether the automaton accepts the input.
    */
   public boolean isAccepting() {
      return this.state == ACCEPT;
   }

   /**
    * @return Whether the automaton refuses the input.
    */
   public boolean isRefusing() {
      return this.state != ACCEPT;
   }

   /**
    * @return Whether the automaton is in a not accepting sink (does not and will never accept).
    */
   public boolean isErrorState() {
      return this.state == ERROR;
   }
   
   /**
    * Forces the automaton to make a step and go in the next state while reading the given input.
    * While making the step, the automaton checks whether the query character can match the
    * reference character. More precisely, the automaton observes whether both subsequences read
    * until now can each other or if a match is possible (whether it is a match may depend on the
    * following characters).
    * 
    * @param query
    *           Next character of the query sequence
    * @param reference
    *           Next character of the reference sequence
    * @return Whether the query and reference sequences read until now potentially match each other.
    *         That means, when `true` is returned it is not necessary a match because whether it is
    *         a match or not may depend on the following characters. But when false is returned,
    *         then the automaton got in an error state meaning that the subsequences do not match
    *         each other and can not be expanded to a match by reading more characters.
    */
   public abstract boolean step(byte query, byte reference);

   /**
    * Resets the automaton. That means, it will act like no input was read.
    */
   public abstract void reset();
   
   /**
    * 
    * @return
    */
   public static MatchAutomaton exactMatchAutomaton(Alphabet alphabet) {
      return new UnmodifiedMatch(alphabet);
   }
   
   public static MatchAutomaton bisulfitMatchAutomaton() {
      AutomatonCollection ac = new AutomatonCollection();
      ac.addSubautomaton(new UnmodifiedMatch(Alphabet.DNA()));
      ac.addSubautomaton(new CTmatch());
      ac.addSubautomaton(new GAmatch());
      return ac;
   }
   
   /**
    * An automaton that is composed of several subautomatons, that accepts when at least one subautomaton accepts.
    * @author Markus Kemmerling
    */
   private static class AutomatonCollection extends MatchAutomaton {

      private Vector<MatchAutomaton> subAutomatons = new Vector<MatchAutomaton>(3);
      
      public void addSubautomaton(MatchAutomaton automaton) {
         subAutomatons.add(automaton);
      }

      public boolean step(byte query, byte reference) {
         boolean b = false;
         for (MatchAutomaton automaton : subAutomatons) {
            // the automaton get in an error state, when all subautomatons get in an error state.
            b |= automaton.step(query, reference);
         }
         return b;
      }
      
      public boolean isAccepting() {
         for (MatchAutomaton automaton : subAutomatons) {
            if (automaton.isAccepting()) {
               //if one subautomaton accepts, then the whole automaton accepts
               return true;
            }
         }
         return false;
      }

      public boolean isRefusing() {
         // if all subautomatons refuses, then the whole automaton refuses.
         return !isAccepting();
      }

      public boolean isErrorState() {
         for (MatchAutomaton automaton : subAutomatons) {
            // if subautomaton is not in an error state, then the whole automaton is not in an error state
            if (!automaton.isErrorState()) {
               return false;
            }
         }
         // if all subautomatons are in an error state, then the whole automaton is in an error state 
         return true;
      }
      
      public void reset() {
         for (MatchAutomaton automaton : subAutomatons) {
            // reset each subautomaton 
            automaton.reset();
         }
      }
   }

   /**
    * An automaton that calculates exact matches.
    * @author Markus Kemmerling
    */
   private static class UnmodifiedMatch extends MatchAutomaton {

      private final Alphabet alphabet;
      
      public UnmodifiedMatch(Alphabet alphabet) {
         this.alphabet = alphabet;
         setState(ACCEPT);
      }
      
      @Override
      public void reset() {
         setState(ACCEPT);
      }

      @Override
      public boolean step(byte query, byte reference) {
         if (isErrorState()) {
            return false;
         }
         
         if (alphabet.isSymbol(query) && query == reference) {
            // AA or CC or GG or TT
            return true;
         } else {
            setState(ERROR);
            return false;
         }
      }
      
   }
   
   /**
    * An automaton that accepts under the following conditions (left is the query and right is the reference): <br>
    * A matches A<br>
    * T matches T<br>
    * G matches G<br>
    * C matches T<br>
    * C matches C only before a G on both sides.
    * 
    * @author Markus Kemmerling
    */
   private static class CTmatch extends MatchAutomaton {
      private final int STATE_CC = 1;
      
      public CTmatch() {
         setState(ACCEPT);
      }
      
      @Override
      public void reset() {
         setState(ACCEPT);
      }

      @Override
      public boolean step(byte query, byte reference) {
         if (query < 0 || query >=4 || reference < 0 || reference >=4) {
            // not A, C, G, T
            setState(ERROR);
            return false;
         }
         
         final int merged = query << 2 | reference;

         if (hasState(ACCEPT)) {
            switch (merged) {
            case AA:
               ;
            case TT:
               ;
            case GG:
               ;
            case CT:
               // remains in accepting state
               return true;
            case CC:
               setState(STATE_CC);
               return false; // only okay when GG follows
            default:
               setState(ERROR);
               return false;
            }
         } else if (hasState(STATE_CC)) {
            if (merged == GG) {
               setState(ACCEPT);
               return true;
            } else {
               setState(ERROR);
               return false;
            }
         } else { // isErrorState()
            assert isErrorState();
            // remains in error state
            return false;
         }
      }
      
   }
   
   /**
    * An automaton that accepts under the following conditions (left is the query and right is the reference): <br>
    * A matches A<br>
    * T matches T<br>
    * C matches C<br>
    * G matches A<br>
    * G matches G only after a C on both sides.
    * 
    * @author Markus Kemmerling
    *
    */
   private static class GAmatch extends MatchAutomaton {
      private final int STATE_CC = 1;

      public GAmatch() {
         setState(ACCEPT);
      }
      
      @Override
      public void reset() {
         setState(ACCEPT);
      }

      @Override
      public boolean isAccepting() {
         return hasState(ACCEPT) || hasState(STATE_CC);
      }

      @Override
      public boolean isRefusing() {
         return ! isAccepting();
      }

      @Override
      public boolean step(byte query, byte reference) {
         if (query < 0 || query >=4 || reference < 0 || reference >=4) {
            // not A, C, G, T
            setState(ERROR);
            return false;
         }
         
         final int merged = query << 2 | reference;

         if (hasState(ACCEPT)) {
            switch (merged) {
            case AA:
               ;
            case TT:
               ;
            case GA:
               // remains in accepting state
               return true;
            case CC:
               setState(STATE_CC);
               return true;
            default:
               setState(ERROR);
               return false;
            }
         } else if (hasState(STATE_CC)) {
            switch (merged) {
            case AA:
               ;
            case TT:
               ;
            case GA:
               ;
            case GG:
               setState(ACCEPT);
               return true;
            case CC:
               // remains in CC state
               return true;
            default:
               setState(ERROR);
               return false;
            }
         } else { // isErrorState()
            assert isErrorState();
            // remains in error state
            return false;
         }
      }
      
   }
}