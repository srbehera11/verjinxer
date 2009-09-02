package verjinxer;

import java.io.File;
import java.io.IOException;

import verjinxer.sequenceanalysis.ISuffixDLL;
import verjinxer.sequenceanalysis.SuffixXorDLL;
import verjinxer.util.ArrayFile;

/**
 * @author Markus Kemmerling
 */
public class SuffixTrayWriter {

   public static void write(ISuffixDLL suffixDLL, File file, String method) throws IOException,
         IllegalArgumentException {
      if (method.equals("L")) {
         // buildpos_L();
         throw new UnsupportedOperationException("Method " + method + "is temporary not supported.");
      } else if (method.equals("R")) {
         // buildpos_R();
         throw new UnsupportedOperationException("Method " + method + "is temporary not supported.");
      } else if (method.equals("minLR")) {
         // buildpos_minLR(false);
         throw new UnsupportedOperationException("Method " + method + "is temporary not supported.");
      } else if (method.equals("bothLR")) {
         if (suffixDLL instanceof SuffixXorDLL) {
            writepos_bothLR((SuffixXorDLL) suffixDLL, file);
         } else {
            // TODO ???
         }
      } else if (method.equals("bothLR2")) {
         // buildpos_bothLR2();
         throw new UnsupportedOperationException("Method " + method + "is temporary not supported.");
      } else {
         throw new IllegalArgumentException("The Method " + method + " does not exist.");
      }
   }

   /**
    * write pos array to file after walk-bothLR using the xor trick.
    * 
    * @param suffixXorDLL
    * 
    * @param file
    *           the file
    */
   private static void writepos_bothLR(SuffixXorDLL suffixXorDLL, File file) throws IOException {
      suffixXorDLL.resetToBegin();
      final ArrayFile f = new ArrayFile(file).openW();
      if (suffixXorDLL.getCurrentPosition() != -1) {
         f.writeInt(suffixXorDLL.getCurrentPosition());
         while (suffixXorDLL.hasNextUp()) {
            suffixXorDLL.nextUp();
            f.writeInt(suffixXorDLL.getCurrentPosition());
         }
         f.close();
      }
   }

}
