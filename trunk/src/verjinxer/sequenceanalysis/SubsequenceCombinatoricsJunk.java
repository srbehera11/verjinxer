/*
 * SubsequenceCombinatoricsJunk.java
 *
 * Created on November 8, 2007, 6:19 PM
 *
 */

package rahmann.sequenceanalysis;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import static rahmann.sequenceanalysis.SubsequenceCombinatorics.*;

/**
 * Junk from SubsequenceCombinatorics. Not for public use.
 * @author Sven Rahmann
 */
class SubsequenceCombinatoricsJunk {
  
  /** No instances can be created. */
  private SubsequenceCombinatoricsJunk() {
  }
  
    /** count the number of distinct subsequences of a given string t=t1..tn,
   * that have a span of at least m.
   * Note: The alphabet is restricted to 256 characters. 
   * Otherwise, a runtime exception will occur.
   * THIS CODE DOES NOT WORK!
   * 
   *@param t  the string
   *@param m  the desired minimum span
   *@return T[i] = number of distinct subsequences in t1..ti with span >= m
   */
  private static BigInteger[] countSubsequencesSpanERROR(final String t, final int m) {
    int[] L = new int[256]; // array of last indices of letters 0..255
    final int n = t.length();
    BigInteger[] N = countSubsequences(t);
    if(m==0) return N;      // m=0 is no restriction, so return "normal" counts
    BigInteger[] T = new BigInteger[n+1]; // T[n] will be the result
    for(int i=0; i<m; i++) { 
      T[i] = BigInteger.ZERO;
      if (i>0) L[t.charAt(i-1)] = i; 
    }
    if(m<3) throw new IllegalArgumentException("algorithm works only for m=0 or m>=3");
    for(int i=m; i<=n; i++) {               // considering length-i-prefix of t
      final char c = t.charAt(i-1);
      BigInteger thisT = T[i-1].add(N[i-1]);

      final String tt = t.substring(i-m+2-1,i-1); // t[i-m+2..i-1]
      assert(tt.length() == m-2);
      final BigInteger[] NN = countSubsequences(tt);
      assert(NN.length == m-2+1);
      thisT = thisT.subtract(NN[NN.length-1]);

      final String prefix = t.substring(0,i-m+1);
      assert(prefix.length() == i-m+1);
      final BigInteger NC[][] = countCommonSubsequences(prefix, tt);
      thisT = thisT.add(NC[NC.length-1][NN.length-1]);
      
      if (L[c]>0) thisT = thisT.subtract(T[L[c]-1]);
      L[c] = i;
      T[i] = thisT;
    }
    return T;
  }

  
  
  // ======================== Number of d-subsequences =============================
  
  /** Compute the number of d-subsequences of a given String t.
   * A d-subsequence is a subsequence whose positions in t must be at most
   * d positions apart. We let S_d(t) be the number of d-subsequences of t
   * and want to count |S_d(t)| =: C_d(t).
   *@param t the string
   *@param d the limit by which two adjacent positions of the subsequence may differ in t.
   * Give d=0 to count regular subsequences (for which d=infinity).
   * Give d=1 to count contiguous substrings.
   *@result a BigInteger array C[0..|t|] so that C[i] contains C_d(t1..ti)
   */
  
  public static BigInteger[] countDSubsequencesXX(final String t, int d) {
    //throw new RuntimeException("This code does not work correctly!");
    final int m = t.length();
    if (d==0) d=m;
    if (d<0) throw new IllegalArgumentException("Parameter d must be >=1 or zero (treated as infinity).");
    BigInteger[][] C_jd  = new BigInteger[m][d+1];
    BigInteger[][] Cij   = new BigInteger[m][m];
    BigInteger[]   Ci    = new BigInteger[m];
    boolean[][]    rij   = new boolean[m][m];
    
    // determine number of characters in each prefix
    // to initialize Ci[0..m-1] in O(m^2) time
    int nc=0;
    for(int i=0; i<m; i++) {
      char ti = t.charAt(i);
      boolean newcharacter=true;
      for(int j=i-1; j>=0; j--) if (t.charAt(j)==ti) { newcharacter=false; break; }
      if (newcharacter) nc++;
      Ci[i]=BigInteger.valueOf(1+nc);
      for (int j=0; j<=i; j++) Cij[i][j] = BigInteger.ZERO;
    }
    // initialize C_jd[][]
    for (int j=0; j<m; j++)
      for(int delta=1; delta<=d; delta++)
        C_jd[j][delta] = BigInteger.ZERO;
    
    // process each letter
    rij[0][0]=true;
    for(int i=1; i<m; i++) {
      char ti = t.charAt(i);
      // determine rij[i][*]
      rij[i][i]=true;
      for(int j=0; j<i; j++) rij[i][j] = (t.charAt(j)==ti)? false : rij[i-1][j];
      System.out.printf("%s (i=%d)%n", t.substring(0,i+1), i);
      for(int j=0; j<=i; j++)  System.out.printf("%d", rij[i][j]?1:0);
      System.out.println();
      // update Cijd
      for(int j=1; j<=i; j++) {
        char tj = t.charAt(j);
        for(int delta=1; delta<=d; delta++) {
          int fall=0;
          if(rij[i][j]) {
            C_jd[j][delta] = (rij[j-1][j-delta]?BigInteger.ONE:BigInteger.ZERO).add(Cij[j-1][j-delta]);
            fall=1;
          } else if (ti!=tj) {
            fall=2;
            // nothing to do!
          } else {
            assert(j<i);
            assert(ti==tj);
            assert(!rij[i][j]);
            if (i-(j-delta)<=d) {
              C_jd[j][delta] = BigInteger.ZERO;
              fall=3;
            } else {
              assert(i-(j-delta)>d);
              // nothing to do?!
              fall=4;
            }
          } // end three cases
          System.out.printf("  C_d[%d][%d][%d]=%s (case %d)%n", i,j,delta,C_jd[j][delta],fall);
          Cij[i][j] = Cij[i][j].add(C_jd[j][delta]);
        } // end for delta
        Ci[i] = Ci[i].add(Cij[i][j]);
      } // end for j
    } // end for i
    
    return Ci;
  }
  
  /** what is this? */
  private static int[][] subset;
  
  public static int countDSubsequences(final String t, int d) {
    ArrayList[][] s = naive(d, t); //naive Variante um die Subsequenzen zu berechen
    
    int count=0; //Anzahl der d-Subsequenzen
    for(int x=0;x<s.length;x++){
      for(int y=0;y<s[x].length;y++){
        Object[] a = s[x][y].toArray();
        for(int i=0;i<a.length;i++){
          //System.out.println(a[i].toString());
          count++;
        }
      }
    }
    return count;
  }
  
  
  private static ArrayList[][] naive(int d, String t) {
    int n = t.length();
    ArrayList[][] s = new ArrayList[n+1][n+1];
    for(int i = 0; i<s.length;i++) {
      for(int j = 0; j<s[i].length;j++){
        s[i][j]= new ArrayList();
      }
    }
    
   /*
    * Initialisierung des Arrays für die Teilmengen
    * Anzahl der Teilmengen (ohne leere Menge) = 2^(n)-2^(n-d-1), wenn (n-d-1)>=1
    * sonst 2^(n)-1;
    */
    if(n-d-1>0)
      subset = new int[(1<<n) - (1<<(n-d-1))][n];
    else subset = new int[(1<<n)-1][n];
    d_subsets(n, d);
    HashMap d_subsequence = subsequencen(t);
    
    //Speicherung in S(i,j)
    Set keySet= d_subsequence.keySet();
    Iterator it = keySet.iterator();
    Integer v;
    while(it.hasNext()){
      String next = it.next().toString();
      v = (Integer) d_subsequence.get(next);
      s[n][v.intValue()].add(next);
    }
    return s;
  }
  
  
  /**
   * Teilmengen der Menge {1..n} berechnen, abhängig von d
   * @param n
   * @param d
   */
  private static void d_subsets(int n, int d) {
    int pointer=0;
    if (n>1)  {
      d_subsets(n-1, d);
      if(n-d-1<=0) {
        int length = (1<<(n-1))-1; //Anzahl der schon im Array enthaltenen Teilmengen
        for(int x=0; x<length; x++) {
          for(int y=0; subset[x][y]!= 0; y++) {
            subset[x+length][y]=subset[x][y];
            pointer = y;
          }
          subset[x+length][pointer+1]= n;
        }
        subset[(1<<n)-2][0]= n;
      } else {
        int length = (1<<(n-1)) - (1<<(n-d-2));
        for(int x=0; x<length; x++) {
          for(int y=0; subset[x][y]!= 0; y++) {
            subset[x+length][y]=subset[x][y];
            pointer = y;
          }
          subset[x+length][pointer+1]= n;
        }
        subset[(1<<n) - (1<<(n-d-1)) - 2][0]= n;
      }
    } else { // n==1
      subset[0][0] = n;
    }
  }
  
  /**
   * Teilmengen in Subsequenzen von t überführen (ohne doppelte Subsequenzen)
   * @param t String
   * @return HashMap mit Subsequenzen als Key und die letzte Einbettung in t als Value
   */
  private static HashMap subsequencen(String t){
    String sequence = "";
    HashMap sequencen = new HashMap(1<<t.length());
    int j = 0;
    for(int x=0;x<subset.length;x++){
      for(int y=0;y<subset[x].length;y++){
        if(subset[x][y]!=0){
          j = subset[x][y];
          if(sequence=="")
            sequence = Character.toString(t.charAt(j-1));
          else sequence = sequence + t.charAt(j-1);
        }
      }
      Integer embed = new Integer(j);
      if(j!= 0){
        //überprüfen, ob Subsequenz schon vorhanden ist und vergleiche Einbettung
        if(sequencen.get(sequence)== null || embed.compareTo((Integer) sequencen.get(sequence))>0){
          sequencen.put(sequence, embed);
        }
      }
      sequence = "";
    }
    return sequencen;
  }
  
  
  
}
