package verjinxer.util;

/**
 * @author Markus Kemmerling
 */
public class HuffmanWaveletTree implements IWaveletTree {
   
   private final HuffmanTree tree;
   private final RankedBitArray bitVector;
   private final HuffmanWaveletTree left, right;
   
   public HuffmanWaveletTree(byte[] sequence, byte[] characters, int[] frequencies) {
      this(sequence,HuffmanTree.buildHuffmanTree(characters, frequencies));
   }
   
   public HuffmanWaveletTree(byte[] sequence, final HuffmanTree tree) {
      this.tree = tree;
      if (tree.isLeaf()) {
         bitVector = null;
         left = right = null;
      } else {
         bitVector = new RankedBitArray(sequence.length);
         byte[] sequenceNew = new byte[sequence.length];
         final int delimiter = fillBitVector(sequence, sequenceNew, 0, sequence.length);
         assert tree.hasLeft();
         left = new HuffmanWaveletTree(sequenceNew, sequence, 0, delimiter, tree.getLeft());
         assert tree.hasRight();
         right = new HuffmanWaveletTree(sequenceNew, sequence, delimiter, sequence.length, tree.getRight());
      }
   }

   private HuffmanWaveletTree(byte[] sequenceOld, byte[] sequenceNew, int from, int to, final HuffmanTree tree) {
      this.tree = tree;
      if (tree.isLeaf()) {
         bitVector = null;
         left = right = null;
      } else {
         bitVector = new RankedBitArray(to-from);
         final int delimiter = fillBitVector(sequenceOld, sequenceNew, from, to);
         assert tree.hasLeft();
         left = new HuffmanWaveletTree(sequenceNew, sequenceOld, from, delimiter, tree.getLeft());
         assert tree.hasRight();
         right = new HuffmanWaveletTree(sequenceNew, sequenceOld, delimiter, to, tree.getRight());
      }
   }

   private int fillBitVector(byte[] sequenceOld, byte[] sequenceNew, int from, int to) {
      // requires that the constructor for bitVector was invoked with the right parameter
      int posv = 0; // position in bitVector
      int posOld = from; // position in sequenceOld
      int posNewLeft = from; // position in sequenceNew to stores characters belonging into the left subtree
      int posNewRight = to-1; // position in sequenceNew to stores characters belonging into the right subtree
      
      while (posOld < to) {
         if (tree.getLeft().contains(sequenceOld[posOld])) {
            // character contains in the left subtree
            // bits in bitVector are set to zero by default
            assert bitVector.get(posv) == 0;
            // bitVector.set(posv, 0);
            sequenceNew[posNewLeft] = sequenceOld[posOld];
            posNewLeft++;
         } else {
            // character contains in the right subtree
            assert tree.getRight().contains(sequenceOld[posOld]);
            bitVector.set(posv, 1);
            sequenceNew[posNewRight] = sequenceOld[posOld];
            posNewRight--;
         }
         posv++;
         posOld++;
      }
      assert posNewLeft == posNewRight+1;
      ArrayUtils.reverseArray(sequenceNew, posNewLeft, to);
      return posNewLeft;
   }

   @Override
   public byte getCharacter(int position) {
      if(bitVector == null) {
         // this is a leaf
         return tree.getCharacters()[0];
      } else {
         // this is a inner node
         final int bit = bitVector.get(position);
         // determine position of queried character in child
         // rank says, that the queried character is the i-th character in the child
         // the position of the i-th character is i-1 (counting begins at 0)
         position = bitVector.rank(bit, position+1) - 1;
         if (bit == 0) {
            return left.getCharacter(position);
         } else {
            return right.getCharacter(position);
         }
      }
   }

   @Override
   public int rank(byte character, int prefixLength) {
      if (bitVector == null) {
         // this is a leaf
         return prefixLength;
      } else {
         // this is a inner node
         if (tree.getLeft().contains(character)) {
            // character belongs to the left subtree
            prefixLength = bitVector.rank(0, prefixLength);
            return left.rank(character, prefixLength);
         } else {
            // character belongs to the right subtree
            prefixLength = bitVector.rank(0, prefixLength);
            return right.rank(character, prefixLength);
         }
      }
   }
   
}
