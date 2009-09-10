package verjinxer;

import java.io.PrintWriter;

import verjinxer.sequenceanalysis.BWTIndex;

/**
 * @author Markus Kemmerling
 */
public class BWTSearch {
   /**
    * 
    * @param query
    * @param referenceIndex
    * @return
    */
   public static BWTSearch.BWTSearchResult find(byte[] query, BWTIndex referenceIndex) {
      return find(query,0,query.length,referenceIndex);
   }

   /**
    * 
    * @param query
    * @param begin
    * @param end
    * @param referenceIndex
    * @return
    */
   public static BWTSearch.BWTSearchResult find(final byte[] query, final int begin, int end,
         BWTIndex referenceIndex) {
      int start1, start2, end1, end2;
      start1 = 0;
      end1 = referenceIndex.size()-1;
      for(end--; end >= begin; end--) {
         start2 = referenceIndex.getFirstIndexPosition(query[end]);
         end2 = referenceIndex.getFirstIndexPosition((byte)(query[end]+1))-1;
         while(referenceIndex.getSuccedingIndexPosition(start2) < start1 && start2 <= end2) {
            start2++;
         }
         while(referenceIndex.getSuccedingIndexPosition(end2) > end2 && end2 >= start2) {
            end2--;
         }
         if(start2 > end2) {
            return new BWTSearchResult(0);
         }
         start1 = start2;
         end1 = end2;
      }
      
      return new BWTSearchResult(end1-start1+1);
   }

   /**
    * 
    * @author Markus Kemmerling
    */
   public static class BWTSearchResult {
      
      /** How often the query was found. */
      final int number;
      
      /**
       * @param number
       *           How often the query was found.
       */
      public BWTSearchResult(int number) {
         this.number = number;
      }

      /**
       * Prints the result to the given writer.
       * 
       * @param out
       */
      public void print(PrintWriter out) {
         out.printf("Querey was found %d times.", number);
      }
      
      public boolean equals(BWTSearchResult result) {
         return number == result.number;
      }

   }

}
