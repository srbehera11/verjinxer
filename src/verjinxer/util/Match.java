package verjinxer.util;

public class Match {
   private final int queryNumber;
   private final int queryPosition;
   private final int referenceNumber;
   private final int referencePosition;
   private final int length;

   public Match(int queryNumber, int queryPosition, int referenceNumber, int referencePosition,
         int length) {
      this.queryNumber = queryNumber;
      this.queryPosition = queryPosition;
      this.referenceNumber = referenceNumber;
      this.referencePosition = referencePosition;
      this.length = length;
   }

   public int getQueryNumber() {
      return queryNumber;
   }

   public int getQueryPosition() {
      return queryPosition;
   }

   public int getReferenceNumber() {
      return referenceNumber;
   }

   public int getReferencePosition() {
      return referencePosition;
   }

   public int getLength() {
      return length;
   }
   
   public int getDiagonal() {
      return referencePosition - queryPosition;
   }
}

