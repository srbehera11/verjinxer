import java.io.File;
import java.io.IOException;

import verjinxer.Globals;
import verjinxer.Project;
import verjinxer.QGramIndexer;


public class QGramIndexerExample {

   /**
    * @param args
    */
   public static void main(String[] args) throws IOException {
      if (args.length != 1) {
         System.out.println("one argument expected: name of the project");
         System.exit(1);
      }
      File projectname = new File(args[0]);
      Project project = Project.createFromFile(projectname);
      int q = 0; // q=0 means that a good value for q is chosen automatically
      QGramIndexer qgramindexer = new QGramIndexer(new Globals(), project, q);
      qgramindexer.generateAndWriteIndex();
   }
}
