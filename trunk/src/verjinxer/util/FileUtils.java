package verjinxer.util;

import java.io.File;


/**
 * Utility class with static file functionality.
 * 
 * @author Markus Kemmerling
 */
public class FileUtils {

   /**
    * Removes a file name extension from a string. If no extension is found, the name is returned
    * unchanged.
    * 
    * @param name
    *           file name. For example, "hello.fa"
    * @return file name without extension. For example, "hello"
    */
   public static String extensionRemoved(String name) {
      name = new File(name).getName(); // TODO is this necessary?
      int lastdot = name.lastIndexOf('.');
      if (lastdot >= 0) {
         return name.substring(0, lastdot);
      } else {
         return name;
      }
   }

   /**
    * Determines the type by the suffix of the name.<br>
    * *.fa -> FASTA<br>
    * *csfa -> CSFASTA<br>
    * otherwise -> TEXT
    * 
    * @param filename
    * @return
    */
   public static FileTypes determineFileType(final String filename) {
      int suffixPosition = filename.lastIndexOf(".");
      if (suffixPosition >= 0) {
         if (filename.substring(suffixPosition + 1).startsWith("fa")) {
            return FileTypes.FASTA;
         } else if (filename.substring(suffixPosition + 1).startsWith("csfa")) {
            return FileTypes.CSFASTA;
         }
      }
      // neither .fasta nor .csfasta detected
      return FileTypes.TEXT;
   }

}
