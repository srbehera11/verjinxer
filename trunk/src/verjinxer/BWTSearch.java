package verjinxer;

import java.util.Arrays;

import verjinxer.sequenceanalysis.BWTIndex;

/**
 * @author Markus Kemmerling
 */
public class BWTSearch {
   /**
    * Searches for the query in the index.
    * 
    * @param query
    *           Sequence to search for.
    * @param referenceIndex
    *           Index of a sequence to search in.
    * @return The search result.
    */
   public static BWTSearch.BWTSearchResult find(byte[] query, BWTIndex referenceIndex) {
      return find(query, 0, query.length, referenceIndex);
   }

   /**
    * Searches for (a subrange of) the given query in the index.
    * 
    * @param query
    *           Sequence with a subrange to search for.
    * @param begin
    *           The beginning index of the subsequence (inclusive).
    * @param end
    *           The ending index of the subsequence (exclusive).
    * @param referenceIndex
    *           Index of a sequence to search in.
    * @return The search result.
    */
   public static BWTSearch.BWTSearchResult find(final byte[] query, final int begin, final int end,
         BWTIndex referenceIndex) {
      
      if (begin >= end) {
         // searches for the empty string
         // it is found everywhere
         int[] textPositions = new int[referenceIndex.size()+1];
         for(int i = 0; i < textPositions.length; i++) {
            textPositions[i] = i;
         }
         return new BWTSearchResult(textPositions);
      }
      
      int[] counter = count(query, begin, end, referenceIndex);
      assert counter[1] >= counter[0] : String.format("counter[0]=%d, counter[1]=%s", counter[0],
            counter[1]);
      int[] textPositions = new int[counter[1]-counter[0]];
      for(int i = counter[0], j = 0; i < counter[1]; i++) {
         textPositions[j++] = referenceIndex.map2text(i);
      }
      Arrays.sort(textPositions);
      return new BWTSearchResult(textPositions);
   }
   
   /**
    * Searches for (a subrange of) the given query in the index and returns the range within the index where to find it (to get the position original text/sequence, use #find  for each index position returned).
    * More precisely, if it returns the array 'a', the query can be found at position a[0], a[0]+1, ... , a[1]-1 within the index.
    * @param query
    *           Sequence with a subrange to search for.
    * @param begin
    *           The beginning index of the subsequence (inclusive).
    * @param end
    *           The ending index of the subsequence (exclusive).
    * @param referenceIndex
    *           Index of a sequence to search in.
    * @return An array 'a' of length 2 with a[0] is the first occurrence of the pattern in the index and a[1] is the position after the last occurrence.
    */
   private static int[] count(final byte[] query, final int begin, final int end,
         BWTIndex referenceIndex){
      
      int start1, end1; // index range for string query[i+1]...query[end-1]
      int start2, end2; // index range for string query[i]...query[end-1]
      int left, middle, right; // pointer for binary search

      // the empty string (query[end]) is covered by every character in the index
      start1 = 0;
      end1 = referenceIndex.size();

      for (int i = end - 1; i >= begin; i--) {
         start2 = referenceIndex.getFirstIndexPosition(query[i]);
         end2 = referenceIndex.getFirstIndexPosition((byte) (query[i] + 1)) - 1;
         
         //TODO break when searching wildcard

         // narrow the search space for both start2 and end2 with binary search
         // after this, the target value for start2 is between left and middle (inclusive)
         // and the target value for end2 is between middle and right (inclusive)
         left = start2;
         right = end2;
         middle = -1; // to ensure java initialization after while loop
         while (left <= right) {
            middle = (left + right) / 2;
            if (referenceIndex.getSuccedingIndexPosition(middle) < start1) {
               left = middle + 1;
            } else if (referenceIndex.getSuccedingIndexPosition(middle) > end1) {
               right = middle - 1;
            } else { // start1 <= referenceIndex.getSuccedingIndexPosition(middle) <= end1
               break;
            }
         }

         final int middle2 = middle;
         start2 = left;
         end2 = right;

         if (start2 > end2) {
            // there is no character 'query[end]' that has a successor within the range start1 and
            // end1
            start1 = start2;
            end1 = end2;
            break;
         }
         // else: start1 <= referenceIndex.getSuccedingIndexPosition(middle) <= end1

         // search target value of start2 (binary search)
         // it must be between the current values of start2 and middle2 (inclusive)
         assert (left == start2);
         right = middle2;
         while (left <= right) {
            middle = (left + right) / 2;
            if (referenceIndex.getSuccedingIndexPosition(middle) < start1) {
               left = middle + 1;
            } else if (referenceIndex.getSuccedingIndexPosition(middle) > start1) {
               right = middle - 1;
            } else { // referenceIndex.getSuccedingIndexPosition(middle) == start1
               left = middle;
               break;
            }
         }
         start2 = left; // final value for start2;

         // search target value of end2 (binary search)
         // it must be between middle2 and end2 (inclusive)
         left = middle2;
         right = end2;
         while (left <= right) {
            middle = (left + right) / 2;
            if (referenceIndex.getSuccedingIndexPosition(middle) < end1) {
               left = middle + 1;
            } else if (referenceIndex.getSuccedingIndexPosition(middle) > end1) {
               right = middle - 1;
            } else { // referenceIndex.getSuccedingIndexPosition(middle) == end1
               right = middle;
               break;
            }
         }
         end2 = right; // final value for end2;

         if (start2 > end2) {
            // there is no character 'query[end]' that has a successor within the range start1 and
            // end1
            start1 = start2;
            end1 = end2;
            break;
         }
         start1 = start2;
         end1 = end2;
      }
      
      return new int[]{start1, end1 + 1};
   }

   /**
    * 
    * @author Markus Kemmerling
    */
   public static class BWTSearchResult {
      
      /** How often the query was found. */
      public final int number;
      public final int positions[];
      
      /**
       * @param number
       *           How often the query was found.
       */
      public BWTSearchResult(final int positions[]) {
         this.number = positions.length;
         this.positions = positions;
      }

   }

}
