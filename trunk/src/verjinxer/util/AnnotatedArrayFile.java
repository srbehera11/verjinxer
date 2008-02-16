

package verjinxer.util;
import java.util.ArrayList;

/**
 * This class provides an annotated ArrayFile.
 * @author Sven Rahmann
 */
public class AnnotatedArrayFile extends ArrayFile
{
   protected ArrayList<Info> info;
   
   /**
    * Creates a new instance of AnnotatedArrayFile
    * @param fname  the file name on disk of this AnnotatedArrayFile.
    */
   public AnnotatedArrayFile(String fname)
   {
      super(fname);
      info = new ArrayList<Info>(1024);
   }
   
   
   /** a record to store information about each sequence */
   public class Info
   {
      public String description;
      public int length;
      public int ssp;
      
      public Info(final String d, final int l, final int s)
      { description=d; length=l; ssp=s; }
   }
   
   
   public void addInfo(final String d, final int l, final int s)
   {
      info.add(new Info(d,l,s));
   }
   
   public void addInfo(final Info d)
   {
      info.add(d);
   }
   
   public Info getInfo(int i)
   {
      return info.get(i);
   }
   
   public String[] getDescriptions()
   {
      int size = info.size();
      String[] d = new String[size];
      for (int i=0; i<size; i++) d[i]=info.get(i).description;
      return d;
   }
   
   public int[] getSsps()
   {
      int size = info.size();
      int[] s = new int[size];
      for (int i=0; i<size; i++) s[i]=info.get(i).ssp;
      return s;
   }

   public int[] getLengths()
   {
      int size = info.size();
      int[] l = new int[size];
      for (int i=0; i<size; i++) l[i]=info.get(i).length;
      return l;
   }
   
   public int numElements()
   {
      return info.size();
   }

   void clear()
   {
      info = new ArrayList<Info>(1024);
   }
   
   
   
}
