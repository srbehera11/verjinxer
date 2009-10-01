package verjinxer.util;


/**
 * @author Markus Kemmerling
 */
public class WaveletTree {
   private BitArray[] bitVector; // one bit vector for each layer
   private int[][] delimiter; // One bit vector (one layer) is divided in several nodes.
                              // Each node contains a subsequence of the bitVector.
                              // The i-th node in the layer (from left to right) contains 
                              // the subsequence starting at delimiter[i](inclusive) and ending at delimiter[i+1](exclusive) within the bit vector of the layer.
                              // The last entry in delimiter points to the first invalid position of the corresponding bit vector.
   
   public WaveletTree(byte[] sequence, final byte minCharacter, final byte maxCharacter) {
      final int depth = (int) Math.ceil(MathUtils.log2(maxCharacter - minCharacter + 1));
      bitVector = new BitArray[depth];
      delimiter = new int[depth][];
      
      byte[][] subAlphabet = new byte[depth][];
      subAlphabet[0] = new byte[]{minCharacter, (byte)(maxCharacter+1)};
      delimiter[0] = new int[]{0, sequence.length};
      byte[] buffer = new byte[sequence.length];
      
      int i;
      for(i = 0; i < depth-1; i++) {
         bitVector[i] = new BitArray(sequence.length);
         subAlphabet[i+1] = new byte[(1<<i+1)+1];
         delimiter[i+1] = new int[(1<<i+1)+1];
         
         for(int n = 0; n < delimiter[i].length-1; n++) {
            final int delimiterValue = (subAlphabet[i][n] + subAlphabet[i][n+1]-1) / 2;
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
            delimiter[i+1][2*n + 1] = delimiter[i][n] + countZeros;
            delimiter[i+1][2*n + 2] = delimiter[i][n+1];
         }
         
         byte[] tmp = sequence;
         sequence = buffer;
         buffer = tmp;
      }
      
      if (i == depth - 1) {
         bitVector[i] = new BitArray(sequence.length);
         
         for(int n = 0; n < delimiter[i].length-1; n++) {
            final int delimiterValue = (subAlphabet[i][n] + subAlphabet[i][n+1]-1) / 2;
            
            for(int j = delimiter[i][n]; j < delimiter[i][n+1]; j++) {
               if (sequence[j] <= delimiterValue) {
                  bitVector[i].set(j, 0);
               } else {
                  bitVector[i].set(j, 1);
               }
            }
         }
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
