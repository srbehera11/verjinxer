/*
 * Options.java
 *
 * Created on December 8, 2006, 6:48 PM
 *
 */

package rahmann.util;

import java.util.HashMap;

/**
 *
 * @author Sven Rahmann
 */
public class Options
{
   /** maps option names to their canonical name */
   private HashMap<String,String> alias;
   
   /** specifies how many arguments an option takes (0 or 1) */
   private HashMap<String,Integer> argcount;
   
   /** after .parse(), this lists all given options and their values */
   private HashMap<String,String> options;

   /** create new options instance by specifying valid options.
    * Valid options are specified by a format string.
    * Example: 
    * <tt>Options o = new Options("show=s=display,D=debuglevel:");</tt>
    * This allows options -s and --show and --display without argument,
    * canonically represented by option name "show" (the first of the three).
    * It also allows options -D and --debuglevel with one argument,
    * accessbile by option name "D" (the first one).
    */
   public Options(String format)
   {
      alias = new HashMap<String,String>();
      argcount = new HashMap<String,Integer>();
      
      String[] opts = format.split(",");
      int oargs;
      for (String o : opts)
      {
         if (o.length()==0) continue;
         oargs = 0;
         StringBuilder oo = new StringBuilder(o);
         while (oo.charAt(oo.length()-1)==':')
         {
            oo.deleteCharAt(oo.length()-1);
            oargs++;
         }
         String[] aliases = oo.toString().split("=");
         String primary = aliases[0];
         argcount.put(primary,oargs);
         for (String a : aliases) alias.put(a,primary);
      }
   }
   
   /** parses command line arguments into options and true arguments.
    * After parsing, options and their argument values can be accessed
    * via <tt>.get(key)</tt> or <tt>.isGiven(key)</tt>.
    */
   public String[]
      parse(String[] args)
      throws IllegalOptionException
   {
      int i;
      String arg, olong, oshort, eq, key;
      boolean partial;
      options = new HashMap<String,String>();
      
      for (i=0; i<args.length; i++)
      {
         arg = args[i];
         key = null;
         eq = null;
         partial = false;
         
         if (arg.charAt(0)!='-') break;
         if (arg.equals("-")) break;
         if (arg.equals("--"))
         { i++; break; };
         
         int eqpos = arg.indexOf('=');
         if (eqpos>=0)
         {
            eq = arg.substring(eqpos+1);
            arg = arg.substring(0,eqpos);
         }
         
         if (arg.charAt(1)!='-')
         { // short option form. May contain multiple options.
            olong = arg.substring(1);
            oshort = arg.substring(1,2);
            if (alias.containsKey(olong)) key=alias.get(olong);
            else if (alias.containsKey(oshort))
            {
               key = alias.get(oshort);
               if(argcount.get(key)>0)
                  throw new IllegalOptionException("-"+oshort+" used with other options, but needs argument.");
               args[i] = (eq==null)? ("-"+arg.substring(2)):("-" + arg.substring(2)+"="+eq);
               partial = true;
            }
         }
         else
         { // long form.
            olong = arg.substring(2);
            if (alias.containsKey(olong)) key=alias.get(olong);
         }
         if (key==null) throw new IllegalOptionException(arg+ " is not a valid option");
         if (eq!=null)
         {
            if (argcount.get(key)==0) throw new IllegalOptionException(arg+" takes no argument");
            options.put(key,eq);
         }
         else
         {
            if (argcount.get(key)==0)
            {
               options.put(key,null);
               if (partial) i--;
            }
            else
            {
               if(i==args.length-1)
                  throw new IllegalOptionException(arg+" needs an argument");
               options.put(key,args[++i]);
            }
         }
      }
      
      String[] newargs = new String[args.length-i];
      System.arraycopy(args,i,newargs,0,args.length-i);
      return newargs;
   }
   
   /** returns the option argument for option 'key' */
   public final String get(String key)
   {
      String s = alias.get(key);
      if (s==null) return null;
      return options.get(s);
   }
   
   /** returns whether option 'key' has been given on the command line */
   public final boolean isGiven(String key)
   {
      String s = alias.get(key);
      if (s==null) return false;
      return options.containsKey(s);
   }
   
   /** returns true if 'key' is a valid option */
   public final boolean isValid(String key)
   {
      return alias.containsKey(key);
   }
   
   /** returns a HashMap of all given options */
   public final HashMap<String,String> getAll()
   {
      return options;
   }
   
   /** sets an option as if given on the command line */
   public void set(String key, String val)
   {
      alias.put(key,key);
      argcount.put(key, val==null?0:1);
      options.put(key,val);
   }
   
} // end class
