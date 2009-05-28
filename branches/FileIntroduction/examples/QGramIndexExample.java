import java.io.IOException;

import verjinxer.Project;
import verjinxer.sequenceanalysis.QGramIndex;


public class QGramIndexExample {

   public static void main(String[] args) throws IOException {
      if (args.length != 1) {
         System.out.println("one argument expected: name of the project");
         System.exit(1);
      }
      String projectname = args[0];
      Project project = Project.createFromFile(projectname);
      QGramIndex qgramindex = new QGramIndex(project);
      
      System.out.printf("q-gram index read.%nq=%d%nmaximum bucket size is %d%n", qgramindex.q, qgramindex.getMaximumBucketSize());
      int qcode = 15;
      int positions[] = new int[qgramindex.getMaximumBucketSize()];
      qgramindex.getQGramPositions(qcode, positions);
      System.out.printf("%d positions for q-code %d:%n", qgramindex.getBucketSize(qcode), qcode);
      for (int i = 0; i < qgramindex.getBucketSize(qcode); ++i) {
         System.out.format("%d ", positions[i]);
      }
   }
}
