package verjinxer;

/**
 * All subcommands must implement the Subcommand interface.
 * 
 * @author Marcel Martin
 * 
 * TODO rename to 'Module'?
 */
public interface Subcommand {

   /**
    * Runs this subcommand.
    * 
    * @param args
    *           the command-line parameters
    * 
    */
   int run(String[] args);

   /** Prints help for this Subcommand */
   void help();
}
