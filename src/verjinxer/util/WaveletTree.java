package verjinxer.util;


/**
 * @author Markus Kemmerling
 */
public class WaveletTree implements IWaveletTree {
   private RankedBitArray[] bitVector; // one bit vector for each layer
   private int[][] delimiter; // One bit vector (one layer) is divided in several nodes.
                              // Each node contains a subsequence of the bitVector.
                              // The i-th node in the layer (from left to right) contains 
                              // the subsequence starting at delimiter[layer][i](inclusive) and ending at delimiter[layer][i+1](exclusive) within the bit vector of the layer.
                              // The last entry in delimiter points to the first invalid position of the corresponding bit vector.
   private byte[] leaves; // stores the character at a leave
   private int[][] nodeRank1Table; // stores for each bitVector the rank1 value from the beginning of the vector to the beginning of a node (nodeRank1Table[layer][node] = bitVector[layer].rank(1, delimiter[layer][node]);
   
   /** Lowest character within the sequence.  */
   private final byte minCharacter;
   
   /** Highest character within the sequence. */
   private final byte maxCharacter;
   
   /**
    * Creates a new Wavelet Tree for the given sequence.
    * @param sequence
    *           Sequence for that the Wavelet Tree is created (WARNING: will be changed while creation).
    * @param minCharacter
    *           Lowest character within the sequence.
    * @param maxCharacter
    *           Highest character within the sequence.
    */
   public WaveletTree(byte[] sequence, final byte minCharacter, final byte maxCharacter) {
      this.minCharacter = minCharacter;
      this.maxCharacter = maxCharacter > minCharacter ? maxCharacter : (byte) (maxCharacter + 1);
      // to prevent a tree with depth 0
      assert this.maxCharacter > this.minCharacter : String.format(
            "maxCharacter:%d, minCharacter:%d", this.maxCharacter, this.minCharacter);
      final int depth = (int) Math.ceil(MathUtils.log2(this.maxCharacter - this.minCharacter + 1));
      bitVector = new RankedBitArray[depth];
      delimiter = new int[depth][];
      
      byte[][] subAlphabet = new byte[depth][];
      subAlphabet[0] = new byte[]{this.minCharacter, (byte)(this.maxCharacter+1)};
      delimiter[0] = new int[]{0, sequence.length};
      byte[] buffer = new byte[sequence.length];
      
      int i;
      for(i = 0; i < depth-1; i++) {
         bitVector[i] = new RankedBitArray(sequence.length);
         subAlphabet[i+1] = new byte[(1<<i+1)+1];
         delimiter[i+1] = new int[(1<<i+1)+1];
         
         for(int n = 0; n < delimiter[i].length-1; n++) {
            final int delimiterValue = getDelimiterValue(subAlphabet[i][n] , subAlphabet[i][n+1]);
            subAlphabet[i+1][2*n] = subAlphabet[i][n];
            subAlphabet[i+1][2*n+1] = (byte)(delimiterValue+1);
            subAlphabet[i+1][2*n+2] = subAlphabet[i][n+1];
            
            int pos = delimiter[i][n];
            for(int j = delimiter[i][n]; j < delimiter[i][n+1]; j++) {
               if (sequence[j] <= delimiterValue) {
                  bitVector[i].set(j, 0);
                  buffer[pos] = sequence[j];
                  pos++;
               } else {
                  bitVector[i].set(j, 1);
               }
            }
            final int countZeros = pos-delimiter[i][n];
            pos = delimiter[i][n+1]-1;
            for(int j = delimiter[i][n+1]-1; j >= delimiter[i][n] ; j--) {
               if (sequence[j] > delimiterValue) {
                  buffer[pos] = sequence[j];
                  pos--;
               }
            }
            //delimiter[i+1][2*n] is set in previous pass
            assert delimiter[i+1][2*n] == delimiter[i][n];
            delimiter[i+1][2*n + 1] = delimiter[i][n] + countZeros;
            delimiter[i+1][2*n + 2] = delimiter[i][n+1];
         }
         
//         bitVector[i].preRankCalculation();
         
         byte[] tmp = sequence;
         sequence = buffer;
         buffer = tmp;
      }
      
      if (i == depth - 1) {
         bitVector[i] = new RankedBitArray(sequence.length);
         leaves = new byte[(1<<i+1)+1];
         
         for(int n = 0; n < delimiter[i].length-1; n++) {
            final int delimiterValue = getDelimiterValue(subAlphabet[i][n] , subAlphabet[i][n+1]);
            assert subAlphabet[i][n + 1] - subAlphabet[i][n] == 1
                  || subAlphabet[i][n + 1] - subAlphabet[i][n] == 2 : String.format(
                  "Range:[%d,%d]", subAlphabet[i][n], subAlphabet[i][n + 1]);
            
            for (int j = delimiter[i][n]; j < delimiter[i][n + 1]; j++) {
               if (sequence[j] <= delimiterValue) {
                  bitVector[i].set(j, 0);
                  assert sequence[j] == delimiterValue : String.format(
                        "Range:[%d,%d] Delimiter:%d Sequence[j]:%d", subAlphabet[i][n],
                        subAlphabet[i][n + 1], delimiterValue, sequence[j]);
                  leaves[2 * n] = sequence[j];
               } else {
                  bitVector[i].set(j, 1);
                  assert sequence[j] == delimiterValue + 1 : String.format(
                        "Range:[%d,%d] Delimiter:%d Sequence[j]:%d", subAlphabet[i][n],
                        subAlphabet[i][n + 1], delimiterValue, sequence[j]);
                  leaves[2 * n + 1] = sequence[j];
               }
            }
         }
         
//         bitVector[i].preRankCalculation();
      }
      
      buildNodeRank1Table();
      
   }
   
   private void buildNodeRank1Table() {
      nodeRank1Table = new int[bitVector.length][];
      for(int layer = 0; layer < bitVector.length; layer ++) {
         nodeRank1Table[layer] = new int[delimiter[layer].length-1];
         for(int node = 0; node < nodeRank1Table[layer].length; node++) {
            nodeRank1Table[layer][node] = bitVector[layer].rank(1, delimiter[layer][node]);
         }
      }
      
   }

   @Override
   public byte getCharacter(int position) {
      int node = 0;
      int layer;
      for(layer = 0; layer < bitVector.length - 1; layer++) {
         final int bit = getBit(layer, node, position);
         // determine position of queried character in child
         position = getRank(bit, layer, node, position + 1); // rank say, that the queried character is the i-th character in the child
         position = position - 1; // the position of the i-th character is i-1 (counting begins at 0)
         
         //determine left or right child
         if ( bit == 0) {
            node = 2 * node;
         } else {
            node = 2 * node + 1;
         }
      }
      
      if (layer == bitVector.length - 1) {
         final int bit = getBit(layer, node, position);
         if ( bit == 0) {
            return leaves[2 * node];
         } else {
            return leaves[2 * node + 1];
         }
      }
      
      throw new IllegalStateException(); // should not happen
   }
   
   @Override
   public int rank(byte character, int prefixLength) {
      int lowerBound = minCharacter;
      int upperBound = (byte)(maxCharacter+1);
      int node = 0;
      
      for(int layer = 0; layer < bitVector.length; layer++) {
         final int delimiterValue = getDelimiterValue(lowerBound, upperBound);
         if (character <= delimiterValue) {
            prefixLength = getRank(0, layer, node, prefixLength);
            
            //set child node and the corresponding subset of the alphabet  
            upperBound = delimiterValue + 1;
            node = 2 * node;
         } else {
            prefixLength = getRank(1, layer, node, prefixLength);

            //set child node and the corresponding subset of the alphabet  
            lowerBound = delimiterValue + 1;
            node = 2 * node + 1;
         }
      }
      
      return prefixLength;
   }
   
   /**
    * Returns the number of times the given bit (0 or 1) appears in the prefix of the BitArray belonging to the specified node.
    * @param bit
    *          The bit to count.
    * @param layer
    *          The layer of the tree.
    * @param node
    *          The node within the layer.
    * @param prefixLength
    *          The length of the prefix within the occurrence are returned.
    * @return The number of times the given bit (0 or 1) appears in the prefix of the BitArray belonging to the specified node.
    */
   private int getRank(int bit, int layer, int node, int prefixLength) {
      final int nodeBegin = delimiter[layer][node];
      final int tmp = bitVector[layer].rank(bit, nodeBegin + prefixLength);
      // tmp contains not only the number of occurrences within the actual node but within all node in the layer up to the actual node (from left to right).
      // to get only the occurrences within the actual node, the occurrences in all nodes on the left side must be subtracted.
      if (bit == 1) {
         return tmp - nodeRank1Table[layer][node];
      } else {
         return tmp - (nodeBegin - nodeRank1Table[layer][node]);
      }
   }
   
   /**
    * Returns the bit at the defined position within the specified node within the given layer.
    * @param layer
    *          The layer of the tree.
    * @param node
    *          The node within the layer.
    * @param position
    *          Position in the BitArray belonging to the specified node.
    * @return One bit as integer.
    */
   private int getBit(int layer, int node, int position) {
      final int nodeBegin = delimiter[layer][node];
      final int bit = bitVector[layer].get(nodeBegin + position);
      return bit;
   }
   
   /**
    * For a node, that represent a subset of the alphabet, it returns the character 'c' so that all characters lower or equal are in the left child and all character greater as 'c' are in the right child. 
    * @param lowerBound
    *          Lowest character (inclusive) of the alphabet subset.
    * @param upperBound
    *          Highest character (exclusive) of the alphabet subset.
    * @return Delimiter for the left and right have of of the ordered alphabet subset.
    */
   private int getDelimiterValue(final int lowerBound, final int upperBound) {
      // parameters and return value are int and not byte to avoid type castings
      assert upperBound >= lowerBound;
      if (lowerBound >= 0) {
         return (lowerBound + upperBound - 1) / 2;
      } else {
         final int shift = lowerBound * -1;
         final int delimiter = (lowerBound + shift + upperBound + shift - 1) / 2;
         return delimiter - shift;
//         final int delimiter = (lowerBound + upperBound - 1) / 2;
//         if ( (upperBound - lowerBound)%2 != 0) {
//            return delimiter;
//         } else {
//            return delimiter -1;
//         }
      }
   }
   
   /**
    * For debugging
    * @param args
    */
   public static void main(String args[]) {
      byte[] sequence = {0,1,0,0,1,2,1,2,0,2,1};
      byte min = 0;
      byte max = 2;
      WaveletTree wt = new WaveletTree(sequence, min, max);
      sequence = new byte[]{0,1,0,0,1,2,4,4,3,4,2,3};
      min = 0;
      max = 4;
      wt = new WaveletTree(sequence, min, max);
   }
   
}
