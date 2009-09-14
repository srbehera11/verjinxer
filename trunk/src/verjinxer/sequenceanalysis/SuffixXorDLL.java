package verjinxer.sequenceanalysis;

import java.util.Arrays;

/**
 * This is a lightweight suffix double linked list. It uses xor coding to store the predecessor and
 * the successor of a list element in the same place and needs less memory then {@link SuffixDLL}.
 * 
 * @see SuffixDLL
 * @author Markus Kemmerling
 */
public class SuffixXorDLL implements ISuffixDLL {
   // encapsulated suffix array
   /**
    * Text/Sequence positions where the lexicographical first suffix that starts with a specific
    * character can be found.
    */
   private int[] lexfirstpos = new int[256];
   
   /**
    * Text/Sequence positions where the lexicographical last suffix that starts with a specific
    * character can be found.
    */
   private int[] lexlastpos = new int[256];

   /** 
    * Lexicographical predecessor and successor for a position (xor coding).
    */
   private int[] lexps = null; // lexps[pos] = (predecessor of pos) ^ (successor of pos)
   
   /** Capacity of this list */
   private final int capacity;
   
   /** The text/sequence associated with this suffix list */
   private final Sequences sequence;
   
   /** Alphabet of the associated text/sequence. */
   private final Alphabet alphabet;

   // internal state
   private int currentPosition = -1;
   private int predecessor = -1;
   private int successor = -1;

   /**
    * Constructor, initializes suffix list of given sequence.
    * 
    * @param sequence
    *           The associated sequence (text).
    * @param alphabet
    *           The alphabet of the sequence.
    */
   public SuffixXorDLL(Sequences sequence, Alphabet alphabet) {
      this.sequence = sequence;
      this.alphabet = alphabet;
      assert sequence.length() < Integer.MAX_VALUE;
      capacity = (int)sequence.length(); //normal sequences warp an array, so its length is int and not long
      lexps = new int[capacity];
      Arrays.fill(lexfirstpos, -1);
      Arrays.fill(lexlastpos, -1);
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#insertbetween(int, int, int)
    */
   public final void insertbetween(final int p1, final int p2, final int i) {
      // before: ... p1, p2 ...
      // after: ... p1, i, p2 ...
      lexps[i] = p1 ^ p2;
      if (p2 != -1) {
         lexps[p2] ^= p1 ^ i;
      }
      if (p1 != -1) {
         lexps[p1] ^= p2 ^ i;
      }
      // update internal state
      predecessor = p1;
      successor = p2;
      currentPosition = i;
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#insertnew(int, int)
    */
   public final void insertnew(final int chi, final int i) {
      int cp, cs, ip, is;
      assert (lexfirstpos[chi] == -1);
      assert (lexlastpos[chi] == -1);
      lexfirstpos[chi] = i;
      lexlastpos[chi] = i;
      for (cp = chi - 1; cp >= 0 && lexlastpos[cp] == -1; cp--) {
      }
      ip = (cp >= 0 ? lexlastpos[cp] : -1);
      for (cs = chi + 1; cs < 256 && lexfirstpos[cs] == -1; cs++) {
      }
      is = (cs < 256 ? lexfirstpos[cs] : -1);
      insertbetween(ip, is, i);
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#insertasfirst(int, int)
    */
   public final void insertasfirst(int chi, int i) {
      int cp, ip;
      assert (lexfirstpos[chi] != -1);
      assert (lexlastpos[chi] != -1);
      for (cp = chi - 1; cp >= 0 && lexlastpos[cp] == -1; cp--) {
      }
      ip = (cp >= 0 ? lexlastpos[cp] : -1);
      insertbetween(ip, lexfirstpos[chi], i);
      lexfirstpos[chi] = i;
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#insertaslast(int, int)
    */
   public final void insertaslast(int chi, int i) {
      int cs, is;
      assert (lexfirstpos[chi] != -1);
      assert (lexlastpos[chi] != -1);
      for (cs = chi + 1; cs < 256 && lexfirstpos[cs] == -1; cs++) {
      }
      is = (cs < 256 ? lexfirstpos[cs] : -1);
      insertbetween(lexlastpos[chi], is, i);
      lexlastpos[chi] = i;
   }

   // ////////////////////////////////////////////////////////////////////
   // ////////////////////getter for suffix array/////////////////////////
   // ////////////////////////////////////////////////////////////////////
   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#getFirstPos(int)
    */
   public int getFirstPos(int chi) {
      return lexfirstpos[chi];
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#getLastPos(int)
    */
   public int getLastPos(int chi) {
      return lexlastpos[chi];
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#getLowestCharacter()
    */
   public int getLowestCharacter() {
      int chi;
      for (chi = 0; chi < 256 && lexfirstpos[chi] == -1; chi++) {
      }
      return chi;
   }

   /**
    * This doulbe linked list uses xor coding to store the lexicographical predecessor and successor
    * of a suffix in the same place. This method returns an array where for each position in the text/sequence
    * the lexicographical previous/next suffix is stored.<br>
    * getLexPS(pos) = (predecessor of pos) ^ (successor of pos).
    * 
    * @param p
    * @return 
    */
   public int getLexPS(int p) {
      return lexps[p];
   }
   
   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#length()
    */
   public int capacity() {
      return capacity;
   }

   // ////////////////////////////////////////////////////////////////////
   // ////////////////////////internal state//////////////////////////////
   // ////////////////////////////////////////////////////////////////////
   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#getCurrentPosition()
    */
   public int getCurrentPosition() {
      return currentPosition;
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#getPredecessor()
    */
   public int getPredecessor() {
      return predecessor;
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#getSuccessor()
    */
   public int getSuccessor() {
      return successor;
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#resetToBegin()
    */
   public void resetToBegin() {
      int chi = getLowestCharacter();
      if (chi >= 0 && chi < 256) {
         currentPosition = lexfirstpos[chi];
         predecessor = -1;
         successor = lexps[currentPosition] ^ -1;
      } else {
         currentPosition = -1;
         predecessor = -1;
         successor = -1;
      }
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#hasNextUp()
    */
   public boolean hasNextUp() {
      return successor != -1;
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#nextUp()
    */
   public void nextUp() {
      predecessor = currentPosition;
      currentPosition = successor;
      successor = lexps[currentPosition] ^ predecessor;
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#hasNextDown()
    */
   public boolean hasNextDown() {
      return predecessor != -1;
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#nextDown()
    */
   public void nextDown() {
      successor = currentPosition;
      currentPosition = predecessor;
      predecessor = lexps[currentPosition] ^ successor;
   }
   
   // ////////////////////////////////////////////////////////////////////
   // /////////////////////associated sequence////////////////////////////
   // ////////////////////////////////////////////////////////////////////
   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#getSequence()
    */
   public Sequences getSequence() {
      return sequence;
   }
   
   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.ISuffixDLL#getAlphabet()
    */
   public Alphabet getAlphabet() {
      return alphabet;
   }
   // ////////////////////////////////////////////////////////////////////
   
   /**
    * @return A String containing the lexicographical ordered text positions separated with whitespaces. 
    */
   public String toString() {
      StringBuilder sb = new StringBuilder(capacity());
      int chi = getLowestCharacter();
      int pos, pred, tmp;
      if (chi >= 0 && chi < 256) {
         pos  = lexfirstpos[chi];
         pred = -1;
         while (pos != -1) {
            sb.append(pos);
            sb.append(" ");
            tmp = pos;
            pos = lexps[pos] ^ pred;
            pred = tmp;
         }
      }
      return sb.toString();
   }
}
