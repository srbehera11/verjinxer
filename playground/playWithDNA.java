import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.sequenceanalysis.InvalidSymbolException;


public class playWithDNA {

   public static void main (String args[]) throws InvalidSymbolException {
      Alphabet DNA = Alphabet.DNA();
      char[] ss = {'a', 'A', 'c','C','g','G','t','T','n','N','r','R','m','M'};
      
      System.out.printf("lenght is %d%n", ss.length);
      String[] code = new String[5];
      for(int i = 0; i < 5; i++)
         code[i] = "";
      for (char s: ss) {
         System.out.printf("%s is maped to %d.%n", s, DNA.code((byte)s));
         code[DNA.code((byte)s)] += s;
      }
      
      for(int i = 0; i < 5; i++) {
         for (int j = 0; j < 5; j++){
            System.out.printf("DNA2CS[%d][%d] = ; // %s%n", i,j, combine(code[i], code[j]));
         }
      }
   }
   
   private static String combine(String s1, String s2){
      String r = "";
      for(char c1: s1.toCharArray())
         for(char c2: s2.toCharArray())
            r += c1+""+c2+" ";
      
      return r;
   }
}
