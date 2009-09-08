package verjinxer;

import java.io.File;
import java.io.IOException;

import verjinxer.sequenceanalysis.BigSuffixDLL;
import verjinxer.sequenceanalysis.BigSuffixXorDLL;
import verjinxer.sequenceanalysis.IBigSuffixDLL;
import verjinxer.util.ArrayFile;

public class BigSuffixTrayWriter {

   /**
    * Writes a given suffix list to disc.
    * 
    * @param suffixDLL
    *           The suffix list to store.
    * @param file
    *           Where to store the suffix list.
    * @param method
    *           How the suffix list shall be stored. Valid methods are 'L', 'R', 'minLR', 'bothLR'
    *           and 'bothLR2'.
    * @throws IOException
    *            when the suffix list can not be written to disc.
    * @throws IllegalArgumentException
    *            when the given method is not valid or when the given method does not suits to the
    *            type of suffixDLL.
    */
   public static void write(IBigSuffixDLL suffixDLL, File file, String method) throws IOException,
   IllegalArgumentException {
      if (method.equals("L")) {
         if (suffixDLL instanceof BigSuffixDLL) {
            writepos_R((BigSuffixDLL) suffixDLL, file);
         } else {
            throw new IllegalArgumentException("Method '" + method + "' only suits to type 'SuffixDLL'!");
         }
      } else if (method.equals("R")) {
         if (suffixDLL instanceof BigSuffixDLL) {
            writepos_R((BigSuffixDLL) suffixDLL, file);
         } else {
            throw new IllegalArgumentException("Method '" + method + "' only suits to type 'SuffixDLL'!");
         }
      } else if (method.equals("minLR")) {
         if (suffixDLL instanceof BigSuffixDLL) {
            writepos_R((BigSuffixDLL) suffixDLL, file);
         } else {
            throw new IllegalArgumentException("Method '" + method + "' only suits to type 'SuffixDLL'!");
         }
      } else if (method.equals("bothLR")) {
         if (suffixDLL instanceof BigSuffixXorDLL) {
            writepos_bothLR((BigSuffixXorDLL) suffixDLL, file);
         } else {
            throw new IllegalArgumentException("Method '" + method + "' only suits to type 'SuffixXorDLL'!");
         }
      } else if (method.equals("bothLR2")) {
         if (suffixDLL instanceof BigSuffixDLL) {
            writepos_R((BigSuffixDLL) suffixDLL, file);
         } else {
            throw new IllegalArgumentException("Method '" + method + "' only suits to type 'SuffixDLL'!");
         }
      } else {
         throw new IllegalArgumentException("Unsupported construction method '" + method + "'!");
      }
   }

   /**
    * Writes the given suffix list to disc.
    * 
    * @param suffixXorDLL
    *           The suffix list to store.
    * @param file
    *           Where to store the suffix list.
    * @throws IOException
    *            when the suffix list can not be written to disc.
    */
   private static void writepos_bothLR(BigSuffixXorDLL suffixXorDLL, File file) throws IOException {
      // this method is a copy of writepos_R(BigSuffixDLL, File)
      // the parameter is the only difference. the body is exactly the same
      // we don't use a single method with ISuffixDLL as parameter to get
      // a static linking with inlining instead of a polymorphy and dynamic linking.
      // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

      suffixXorDLL.resetToBegin();
      final ArrayFile f = new ArrayFile(file).openW();
      if (suffixXorDLL.getCurrentPosition() != -1) {
         f.writeLong(suffixXorDLL.getCurrentPosition());
         while (suffixXorDLL.hasNextUp()) {
            suffixXorDLL.nextUp();
            f.writeLong(suffixXorDLL.getCurrentPosition());
         }
         f.close();
      }
   }

   /**
    * Writes the given suffix list to disc.
    * 
    * @param suffixDLL
    *           The suffix list to store.
    * @param file
    *           Where to store the suffix list.
    * @throws IOException
    *            when the suffix list can not be written to disc.
    */
   private static void writepos_R(BigSuffixDLL suffixDLL, File file) throws IOException {
      // this method is a copy of writepos_bothLR(BigSuffixXorDLL, File).
      // the parameter is the only difference. the body is exactly the same
      // we don't use a single method with ISuffixDLL as parameter to get
      // a static linking with inlining instead of a polymorphy and dynamic linking.
      // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

      suffixDLL.resetToBegin();
      final ArrayFile f = new ArrayFile(file).openW();
      if (suffixDLL.getCurrentPosition() != -1) {
         f.writeLong(suffixDLL.getCurrentPosition());
         while (suffixDLL.hasNextUp()) {
            suffixDLL.nextUp();
            f.writeLong(suffixDLL.getCurrentPosition());
         }
         f.close();
      }
   }

}
