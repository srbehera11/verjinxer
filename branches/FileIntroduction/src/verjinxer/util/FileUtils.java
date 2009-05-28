package verjinxer.util;

import java.io.File;


/**
 * Utility class with static file functionality.
 * 
 * @author Markus Kemmerling
 */
public class FileUtils {
   
   /**
    * Creates a new File from the given file's path without its extension (/home/markus/test.ab ->
    * /home/markus/test).
    * 
    * @param file
    * @return The File.
    */
   public static File removeExtension(final File file) {
      return new File(removeExtension(file.getPath()));
   }

   /**
    * Creates a new String from the given path without its extension (/home/markus/test.ab ->
    * /home/markus/test).
    * 
    * @param path
    * @return The String.
    */
   public static String removeExtension(final String path) {
      int lastdot = path.lastIndexOf('.');
      if (lastdot >= 0) {
         return path.substring(0, lastdot); // substring creates a new String
      } else {
         return new String(path); // here a new String must be created cause of uniform treatment
      }
   }
   
   /**
    * Determines the type by the suffix of the name.<br>
    * *.fa -> FASTA<br>
    * *csfa -> CSFASTA<br>
    * otherwise -> TEXT
    * 
    * @param file
    * @return
    */
   //TODO maybe better use FileTypes.valueOf(String) [must be overridden for that circumstances]
   public static FileTypes determineFileType(final File file) {
      String name = file.getName();
      int suffixPosition = name.lastIndexOf(".");
      if (suffixPosition >= 0) {
         if (name.substring(suffixPosition + 1).startsWith("fa")) {
            return FileTypes.FASTA;
         } else if (name.substring(suffixPosition + 1).startsWith("csfa")) {
            return FileTypes.CSFASTA;
         }
      }
      // neither .fasta nor .csfasta detected
      return FileTypes.TEXT;
   }

}
