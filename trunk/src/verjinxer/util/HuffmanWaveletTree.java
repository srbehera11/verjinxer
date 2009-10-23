package verjinxer.util;

/**
 * Huffman shaped Wavelet Tree
 * 
 * @author Markus Kemmerling
 */
public class HuffmanWaveletTree implements IWaveletTree {

   /**
    * The underlying Huffman Tree determining where to find a character.
    */
   private final HuffmanTree tree;

   /**
    * Bit Vector determining if a character at position 'i' is in the left (0) or right (1) subtree.
    */
   private final RankedBitArray bitVector;

   /**
    * The left and right subtree.
    */
   private final HuffmanWaveletTree left, right;

   /**
    * Creates a Huffman shaped Wavelet Tree for the given sequence.
    * 
    * @param sequence
    *           Sequence for that the tree is created.
    * @param characters
    *           The characters of the sequence.
    * @param frequencies
    *           How often each character occurs in the sequence.
    */
   public HuffmanWaveletTree(byte[] sequence, byte[] characters, int[] frequencies) {
      this(sequence, HuffmanTree.buildHuffmanTree(characters, frequencies));
   }

   /**
    * Creates a Huffman shaped Wavelet Tree for the given sequence.
    * 
    * @param sequence
    *           Sequence for that the tree is created.
    * @param tree
    *           Huffman Tree for the sequence determining the mapping between nodes and characters.
    */
   public HuffmanWaveletTree(byte[] sequence, final HuffmanTree tree) {
      this.tree = tree;
      if (tree.isLeaf() || tree.isEmpty()) {
         bitVector = null;
         left = right = null;
      } else {
         bitVector = new RankedBitArray(sequence.length);
         byte[] sequenceNew = new byte[sequence.length];
         final int delimiter = fillBitVector(sequence, sequenceNew, 0, sequence.length);
         assert tree.hasLeft();
         left = new HuffmanWaveletTree(sequenceNew, sequence, 0, delimiter, tree.getLeft());
         assert tree.hasRight();
         right = new HuffmanWaveletTree(sequenceNew, sequence, delimiter, sequence.length,
               tree.getRight());
      }
   }

   /**
    * Creates a Huffman shaped Wavelet Tree for a subsequence of sequenceOld. More precisely the
    * tree is created for sequenceOld[from, ..., to-1].
    * 
    * @param sequenceOld
    *           The Huffman shaped Wavelet Tree is created for a part of this sequence.
    * @param sequenceNew
    *           A buffer for calculations of the subtrees. It is only changed within the range
    *           sequenceNew[from, ..., to-1]. The rest will remain untouched.
    * @param from
    *           The beginning index of the subsequence (inclusive).
    * @param to
    *           The ending index of the subsequence (exclusive),
    * @param tree
    *           Huffman Tree for the complete sequenceOld determining the mapping between nodes and
    *           characters.
    */
   private HuffmanWaveletTree(byte[] sequenceOld, byte[] sequenceNew, int from, int to,
         final HuffmanTree tree) {
      this.tree = tree;
      if (tree.isLeaf() || tree.isEmpty()) {
         bitVector = null;
         left = right = null;
      } else {
         bitVector = new RankedBitArray(to - from);
         final int delimiter = fillBitVector(sequenceOld, sequenceNew, from, to);
         assert tree.hasLeft();
         left = new HuffmanWaveletTree(sequenceNew, sequenceOld, from, delimiter, tree.getLeft());
         assert tree.hasRight();
         right = new HuffmanWaveletTree(sequenceNew, sequenceOld, delimiter, to, tree.getRight());
      }
   }

   /**
    * Constructs {{@link #bitVector} for a subsequence of sequenceOld. More precisely the vector is
    * constructed for sequenceOld[from, ..., to-1].
    * 
    * @param sequenceOld
    *           The vector will be constructed for a part of this sequence.
    * @param sequenceNew
    *           A buffer for calculations of the subtrees. It is only changed within the range
    *           sequenceNew[from, ..., to-1]. The rest will remain untouched.
    * @param from
    *           The beginning index of the subsequence (inclusive).
    * @param to
    *           The ending index of the subsequence (exclusive),
    * @return The delimiting postion in sequenceNew where the left subtree ends and the right
    *         subtree begins. More precisely, let 'i' be the returned value, the left subtree will
    *         be build for sequenceNew[from, ..., i-1] and the right subtree forsequenceNew[i, ...,
    *         to-1].
    */
   private int fillBitVector(byte[] sequenceOld, byte[] sequenceNew, int from, int to) {
      // requires that the constructor for bitVector was invoked with the right parameter
      int posv = 0; // position in bitVector
      int posOld = from; // position in sequenceOld
      int posNewLeft = from; // position in sequenceNew to stores characters belonging into the left
      // subtree
      int posNewRight = to - 1; // position in sequenceNew to stores characters belonging into the
      // right subtree

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
      assert posNewLeft == posNewRight + 1;
      ArrayUtils.reverseArray(sequenceNew, posNewLeft, to);
      return posNewLeft;
   }

   @Override
   public byte getCharacter(int position) {
      if (bitVector == null) {
         // this is a leaf
         return tree.getCharacters()[0];
      } else {
         // this is a inner node
         final int bit = bitVector.get(position);
         // determine position of queried character in child
         // rank says, that the queried character is the i-th character in the child
         // the position of the i-th character is i-1 (counting begins at 0)
         position = bitVector.rank(bit, position + 1) - 1;
         if (bit == 0) {
            return left.getCharacter(position);
         } else {
            return right.getCharacter(position);
         }
      }
   }

   @Override
   public int rank(byte character, int prefixLength) {
      if (tree.isEmpty()) {
         return 0;
      } else if (bitVector == null) {
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
            prefixLength = bitVector.rank(1, prefixLength);
            return right.rank(character, prefixLength);
         }
      }
   }

}
