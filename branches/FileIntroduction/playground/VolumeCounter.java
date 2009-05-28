import java.io.FileInputStream;
import java.io.IOException;



public class VolumeCounter {

   public static void main(String args[]) throws IOException{
      FileInputStream sequence = new FileInputStream(args[0] + ".seq");
      
      int counter = 0;
      while( sequence.read() >= 0 )
         counter++;
      
      System.out.println("sequence: " + counter);
      sequence.close();
      
      FileInputStream qpos = new FileInputStream(args[0] + ".qpos");
      
      counter = 0;
      while( qpos.read() >= 0 )
         counter++;
      
      System.out.println("qpos: " + ((double)counter/4) );
      qpos.close();
      
   }
}
