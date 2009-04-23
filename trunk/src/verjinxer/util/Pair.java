package verjinxer.util;

/**
 * Simple Record for pairs
 * @author Markus Kemmerling
 *
 * @param <T> Type of first value
 * @param <G> Type of second value
 */
public class Pair<T,G> {
   
   public final T fst; /** first value */
   public final G snd; /** second value */
   
   public Pair(final T fst, final G snd){
      this.fst = fst;
      this.snd = snd;
   }

}
