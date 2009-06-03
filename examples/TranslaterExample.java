import java.io.File;
import java.io.IOException;

import verjinxer.Globals;
import verjinxer.Project;
import verjinxer.Translater;
import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.util.FileUtils;

public class TranslaterExample {

   /**
    * @param args
    */
   public static void main(String[] args) throws IOException {
      if (args.length != 1) {
         System.out.println("one argument expected: name of a FASTA file");
         System.exit(1);
      }
      
      File[] files = new File[args.length];
      for (int i = 0; i < args.length; i++) {
         files[i] = new File(args[i]);
      }
      
      File fastaname = files[0];

      File projectname = FileUtils.removeExtension(fastaname);
      Project project = new Project(projectname);
      Translater translater = new Translater(new Globals(), Alphabet.DNA());
      
      // TODO the way to create a new project is very likely to change
      translater.translateFilesAndCreateProject(project, files);
      project.store();
   }
}
