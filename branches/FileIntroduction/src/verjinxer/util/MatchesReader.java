package verjinxer.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class MatchesReader {

   private BufferedReader reader;

   public MatchesReader(String matchesFileName) throws IOException {
      reader = new BufferedReader(new FileReader(matchesFileName));
   }

   public Match readMatch() throws IOException, NumberFormatException {
      String line;
      line = reader.readLine();
      if (line == null) return null;
      String[] fields = line.split(" ");
      int queryNumber = Integer.parseInt(fields[0]);
      int queryPosition = Integer.parseInt(fields[1]);
      int referenceNumber = Integer.parseInt(fields[2]);
      int referencePosition = Integer.parseInt(fields[3]);
      int length = Integer.parseInt(fields[4]);
//      int diagonal = Integer.parseInt(fields[5]);
      return new Match(queryNumber, queryPosition, referenceNumber, referencePosition, length);
   }
}
