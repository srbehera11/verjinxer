package verjinxer;

/** TODO remove dependency on Globals */
public interface Subcommand {
   void run(String[] args);
   void help();
}
