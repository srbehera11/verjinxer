import java.io.IOException;

import verjinxer.Globals;
import verjinxer.Translater;
import verjinxer.TranslaterSubcommand;
import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.util.ProjectInfo;

public class TranslaterExample {

   /**
    * Removes a file name extension from a string. If no extension is found, the name is returned
    * unchanged.
    * 
    * @param name
    *           file name. For example, "hello.fa"
    * @return file name without extension. For example, "hello"
    */
   public static String extensionRemoved(String name) {
      int lastdot = name.lastIndexOf('.');
      if (lastdot >= 0) {
         return name.substring(0, lastdot);
      } else {
         return name;
      }
   }

   /**
    * @param args
    */
   public static void main(String[] args) throws IOException {
      if (args.length != 1) {
         System.out.println("one argument expected: name of a FASTA file");
         System.exit(1);
      }
      String fastaname = args[0];

      String projectname = extensionRemoved(fastaname);
      ProjectInfo project = new ProjectInfo(projectname);
      Translater translater = new Translater(new Globals(), Alphabet.DNA());
      
      // TODO the way to create a new project is very likely to change
      translater.createProject(project, args);
      project.store();
   }
}
