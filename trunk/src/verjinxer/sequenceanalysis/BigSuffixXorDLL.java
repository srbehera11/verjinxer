package verjinxer.sequenceanalysis;

import java.util.Arrays;

import verjinxer.util.HugeByteArray;
import verjinxer.util.HugeLongArray;

/**
 * This is a lightweight huge suffix double linked list. It uses xor coding to store the predecessor and
 * the successor of a list element in the same place and needs less memory then {@link BigSuffixDLL}.
 * 
 * @see BigSuffixDLL
 * @author Markus Kemmerling
 */
public class BigSuffixXorDLL implements IBigSuffixDLL {
   // encapsulated suffix array
   /**
    * Text/Sequence positions where the lexicographical first suffix that starts with a specific
    * character can be found.
    */
   private long[] lexfirstpos = new long[256];
   
   /**
    * Text/Sequence positions where the lexicographical last suffix that starts with a specific
    * character can be found.
    */
   private long[] lexlastpos = new long[256];

   /** 
    * Lexicographical predecessor and successor for a position (xor coding).
    */
   private HugeLongArray lexps = null; // lexps[pos] = (predecessor of pos) ^ (successor of pos)
   
   /** Capacity of this list */
   private final long capacity;
   
   /** The text/sequence associated with this suffix list */
   private final HugeByteArray sequence;
   
   /** Alphabet of the associated text/sequence. */
   private final Alphabet alphabet;

   // internal state
   private long currentPosition = -1;
   private long predecessor = -1;
   private long successor = -1;

   /**
    * Constructor, initializes suffix list of given sequence.
    * 
    * @param sequence
    *           The associated sequence (text).
    * @param alphabet
    *           The alphabet of the sequence.
    */
   public BigSuffixXorDLL(HugeByteArray sequence, Alphabet alphabet) {
      this.sequence = sequence;
      this.alphabet = alphabet;
      capacity = sequence.length; //normal sequences warp an array, so its length is int and not long
      lexps = new HugeLongArray(capacity);
      Arrays.fill(lexfirstpos, -1);
      Arrays.fill(lexlastpos, -1);
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.IBigSuffixDLL#insertbetween(int, int, int)
    */
   public final void insertbetween(final long p1, final long p2, final long i) {
   // before: ... p1, p2 ...
      // after:  ... p1, i, p2 ...
      lexps.set(i, p1 ^ p2);
      if (p2!=-1L) {
         lexps.set(p2, lexps.get(p2) ^ p1 ^ i);
      }
      if (p1!=-1L) {
         lexps.set(p1, lexps.get(p1) ^ p2 ^ i);
      }
      // update internal state
      predecessor = p1;
      successor = p2;
      currentPosition = i;
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.IBigSuffixDLL#insertnew(int, int)
    */
   public final void insertnew(final int chi, final long i) {
      int cp, cs;
      long ip, is;
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
    * @see verjinxer.sequenceanalysis.IBigSuffixDLL#insertasfirst(int, int)
    */
   public final void insertasfirst(int chi, long i) {
      int cp;
      assert (lexfirstpos[chi] != -1);
      assert (lexlastpos[chi] != -1);
      for (cp = chi - 1; cp >= 0 && lexlastpos[cp] == -1; cp--) {
      }
      final long ip = (cp >= 0 ? lexlastpos[cp] : -1);
      insertbetween(ip, lexfirstpos[chi], i);
      lexfirstpos[chi] = i;
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.IBigSuffixDLL#insertaslast(int, int)
    */
   public final void insertaslast(int chi, long i) {
      int cs;
      assert (lexfirstpos[chi] != -1);
      assert (lexlastpos[chi] != -1);
      for (cs = chi + 1; cs < 256 && lexfirstpos[cs] == -1; cs++) {
      }
      final long is = (cs < 256 ? lexfirstpos[cs] : -1);
      insertbetween(lexlastpos[chi], is, i);
      lexlastpos[chi] = i;
   }

   // ////////////////////////////////////////////////////////////////////
   // ////////////////////getter for suffix array/////////////////////////
   // ////////////////////////////////////////////////////////////////////
   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.IBigSuffixDLL#getFirstPos(int)
    */
   public long getFirstPos(int chi) {
      return lexfirstpos[chi];
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.IBigSuffixDLL#getLastPos(int)
    */
   public long getLastPos(int chi) {
      return lexlastpos[chi];
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.IBigSuffixDLL#getLowestCharacter()
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
   public long getLexPS(long p) {
      return lexps.get(p);
   }
   
   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.IBigSuffixDLL#length()
    */
   public long capacity() {
      return capacity;
   }

   // ////////////////////////////////////////////////////////////////////
   // ////////////////////////internal state//////////////////////////////
   // ////////////////////////////////////////////////////////////////////
   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.IBigSuffixDLL#getCurrentPosition()
    */
   public long getCurrentPosition() {
      return currentPosition;
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.IBigSuffixDLL#getPredecessor()
    */
   public long getPredecessor() {
      return predecessor;
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.IBigSuffixDLL#getSuccessor()
    */
   public long getSuccessor() {
      return successor;
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.IBigSuffixDLL#resetToBegin()
    */
   public void resetToBegin() {
      int chi = getLowestCharacter();
      if (chi >= 0 && chi < 256) {
         currentPosition = lexfirstpos[chi];
         predecessor = -1;
         successor = lexps.get(currentPosition) ^ -1;
      } else {
         currentPosition = -1;
         predecessor = -1;
         successor = -1;
      }
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.IBigSuffixDLL#hasNextUp()
    */
   public boolean hasNextUp() {
      return successor != -1;
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.IBigSuffixDLL#nextUp()
    */
   public void nextUp() {
      predecessor = currentPosition;
      currentPosition = successor;
      successor = lexps.get(currentPosition) ^ predecessor;
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.IBigSuffixDLL#hasNextDown()
    */
   public boolean hasNextDown() {
      return predecessor != -1;
   }

   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.IBigSuffixDLL#nextDown()
    */
   public void nextDown() {
      successor = currentPosition;
      currentPosition = predecessor;
      predecessor = lexps.get(currentPosition) ^ successor;
   }
   
   // ////////////////////////////////////////////////////////////////////
   // /////////////////////associated sequence////////////////////////////
   // ////////////////////////////////////////////////////////////////////
   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.IBigSuffixDLL#getSequence()
    */
   public HugeByteArray getSequence() {
      return sequence;
   }
   
   /*
    * (non-Javadoc)
    * @see verjinxer.sequenceanalysis.IBigSuffixDLL#getAlphabet()
    */
   public Alphabet getAlphabet() {
      return alphabet;
   }
   // ////////////////////////////////////////////////////////////////////
}
