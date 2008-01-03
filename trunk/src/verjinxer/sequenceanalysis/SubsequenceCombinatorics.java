/*
 * SubsequenceCombinatorics.java
 *
 * Created on May 12, 2007, 11:49 AM
 *
 */

package verjinxer.sequenceanalysis;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;

/**
 * This class implements subsequence counting algorithms described the paper
 * <b>Algorithms for Subsequence Combinatorics</b> by Cees Elzinga (VU University,
 * Amsterdam), Sven Rahmann (TU Dortmund), and Hui Wang (University of Ulster).
 * See the paper for details.
 * @author Sven Rahmann
 */
public class SubsequenceCombinatorics {
  
  /** cannot create instances of this class */
  private SubsequenceCombinatorics() {
  }
  
  
  
  // ===================== Number of distinct subsequences =====================
  
  
  /** count the number of distinct subsequences of a given string t=t1..tn.
   * Note: The alphabet is restricted to 256 characters. 
   * Otherwise, a runtime exception will occur.
   *
   *@param t  the string
   *@return an array N[] of BigIntegers,
   *  with N[i] = number of distinct subsequences in t1..ti.
   */
  public static BigInteger[] countSubsequences(final String t) {
    return countSubsequences_Elzinga(t);
  }
  
  /** count the number of distinct subsequences of a given string t=t1..tn,
   * according to the length of the subsequence.
   * Note: The alphabet is restricted to 256 characters. 
   * Otherwise, a runtime exception will occur.
   *
   *@param t  the string
   *@return a matrix C[][] of BigIntegers, 
   *  with C[i][j] = number of distinct subsequences in t1..ti of length j.
   */
  public static BigInteger[][] countSubsequencesL(final String t) {
    return countSubsequencesL_Elzinga(t);
  }
  

  
  // Different algorithms for this problem follow.
  // The Elzinga algorithms are much more efficient.
  
  
  /** count the number of distinct subsequences of a given string t=t1..tn,
   * by Elzinga's efficient DP algorithm.
   * Note: The alphabet is restricted to 256 characters. 
   * Otherwise, a runtime exception will occur.
   * 
   *@param t  the string
   *@return N[i] = number of distinct subsequences in t1..ti
   */
  static BigInteger[] countSubsequences_Elzinga(final String t) {
    int[] L = new int[256]; // array of last indices of letters 0..255
    BigInteger[] N = new BigInteger[t.length()+1];
    N[0] = BigInteger.ONE;
    for (int i=1; i<=t.length(); i++) {
      char c = t.charAt(i-1);
      //System.out.printf("i=%d char=%c last=%d%n", i,c,L[c]);
      N[i] = N[i-1].multiply(BigInteger.valueOf(2));
      if (L[c]>0) N[i] = N[i].subtract(N[L[c]-1]);
      L[c] = i;
    }
    return N;
  }
  
  
  /** count the number of distinct subsequences of a given string t=t1..tn,
   * according to the length of the subsequence, using Elzinga's DP algorithm.
   * Note: The alphabet is restricted to 256 characters. 
   * Otherwise, a runtime exception will occur.
   *
   *@param t  the string
   *@return C[i][j] = number of distinct subsequences in t1...ti of length j
   */
  static BigInteger[][] countSubsequencesL_Elzinga(final String t) {
    final int n = t.length();
    int[] L = new int[256]; // array of last indices of letters 0..255
    BigInteger[][] C = new BigInteger[n+1][n+1];
    C[0][0] = BigInteger.ONE;  for(int j=1; j<=n; j++) C[0][j] = BigInteger.ZERO;
    for(int i=1; i<=n; i++) {
      char c = t.charAt(i-1);
      C[i][0] = BigInteger.ONE;
      for(int j=1; j<=n; j++) {
        C[i][j] = C[i-1][j].add(C[i-1][j-1]);
        if (L[c]>0) C[i][j] = C[i][j].subtract(C[L[c]-1][j-1]);
      }
      L[c]=i;
    }
    return C;
  }
  
  
  /** count the number of distinct subsequences of a given string,
   * by the algorithm of Rahmann's CPM'06 paper.
   *
   * A DP algorithm is used that actually computes the number of distinct
   * subsequences by length and then sums over all lengths.
   * Note: The alphabet is restricted to 256 characters. 
   * Otherwise, a runtime exception will occur.
   *
   *@param t  the string
   *@return N[i] = number of distinct subsequences in t1...ti
   */
  static BigInteger[] countSubsequences_Rahmann(final String t) {
    final int n = t.length();
    final int k = n;
    BigInteger[][] C = countSubsequencesL_Rahmann(t);
    BigInteger[] N = new BigInteger[n+1];
    for (int i=0; i<=n; i++) {
      N[i] = BigInteger.ZERO;
      for (int j=0; j<=k; j++) N[i] = N[i].add(C[i][j]);
    }
    return N;
  }
  
  
  /** count the number of distinct subsequences of a given string t=t1..tn,
   * according to the length of the subsequence, using Rahmann's CPM'06 algorithm.
   * Note: The alphabet is restricted to 256 characters. 
   * Otherwise, a runtime exception will occur.
   *
   *@param t  the string
   *@return C[i][j] = number of distinct subsequences in t1...ti of length j
   */
  static BigInteger[][] countSubsequencesL_Rahmann(final String t) {
    final int n = t.length();
    final int k = n;
    BigInteger[][] C = new BigInteger[n+1][k+1]; // C[i][j] #subseq of length j
    BigInteger[][] A = new BigInteger[k+1][256]; // A[j][c] current #subseq of length j that end with c
    C[0][0] = BigInteger.ONE;
    for(int j=1; j<=k; j++) C[0][j]=BigInteger.ZERO;
    for(int j=0; j<=k; j++) { for (int ch=0; ch<256; ch++) A[j][ch] = BigInteger.ZERO; }
    
    for (int i=1; i<=n; i++) {
      C[i][0] = BigInteger.ONE; // one subsequece of length 0
      final char c = t.charAt(i-1);
      for (int j=1; j<=k; j++) {
        A[j][c] = C[i-1][j-1];
        C[i][j] = BigInteger.ZERO;
        for (int ch=0; ch<256; ch++) C[i][j] = C[i][j].add(A[j][ch]);
      }
    }
    return C;
  }
  
  
  
  // ============== Common Subsequences of Two Strings =============
  
  
  /** count the number of distinct common subsequences in two given strings.
   * Note: The alphabet is restricted to 256 characters. 
   * Otherwise, a runtime exception will occur.
   *
   *@param x  first string
   *@param y  second string
   *@return a matrix N[][]
   *  with N[i][j] = number of distinct common subsequences of x1..xi and y1..yj
   */
  public static BigInteger[][] countCommonSubsequences(final String x, final String y) {
    final int m = x.length();
    final int n = y.length();
    BigInteger[][] N = new BigInteger[m+1][n+1];
    int Lx[] = new int[256]; // Lx[a] = last time a was seen in x
    int Ly[] = new int[256]; // Ly[a] = last time a was seen in y
    
    for (int i=0; i<=m; i++) {
      final char a = (i==0)? 0 : x.charAt(i-1);
      for (int j=0; j<=n; j++) {
        if (i==0 || j==0) { N[i][j] = BigInteger.ONE; continue; }
        BigInteger thisN = N[i-1][j];
        Ly[y.charAt(j-1)] = j;
        if(Ly[a]>0 && Ly[a]<=j) {
          thisN = thisN.add(N[i-1][Ly[a]-1]);
          if(Lx[a]>0) thisN = thisN.subtract(N[Lx[a]-1][Ly[a]-1]);
        }
        N[i][j] = thisN;
      }
      Lx[a]=i;
    }
    return N;
  }
  
  
  /** count the number of matching nonempty embeddings in two given strings x and y.
   * In other words, compute the number of nonempty common subsequences of x and y, weighted
   * by their number of joint embeddings.
   *@param x  first string
   *@param y  second string
   *@return a matrix M[][] with
   *    M[i][j] = number of matching embeddings of x1..xi and y1..yj.
   */
  public static BigInteger[][] countMatchingEmbeddings(final String x, final String y) {
    return countMatchingEmbeddings(x,y,false);
  }

  
  /** count the number of matching embeddings in two given strings x and y,
   * including possibly the empty embedding.
   *@param x  first string
   *@param y  second string
   *@param countEmpty  true if the empty embedding should be counted, false if not.
   *@return a matrix M[][] with
   *    M[i][j] = number of matching embeddings of x1..xi and y1..yj.
   */
  public static BigInteger[][] countMatchingEmbeddings(final String x, final String y, boolean countEmpty) {
    final int m = x.length();
    final int n = y.length();
    BigInteger[][] N = new BigInteger[m+1][n+1];
    
    for (int i=0; i<=m; i++) {
      for (int j=0; j<=n; j++) {
        if (i==0 || j==0) { N[i][j] = BigInteger.ZERO; continue; }
        BigInteger thisN = N[i-1][j].add(N[i][j-1]);
        if (x.charAt(i-1)==y.charAt(j-1))
          thisN = thisN.add(BigInteger.ONE);
        else
          thisN = thisN.subtract(N[i-1][j-1]);
        N[i][j] = thisN;
      }
    }
    
    if (countEmpty) { // add 1 to all table entries
      for (int i=0; i<=m; i++)
        for (int j=0; j<=n; j++)
          N[i][j] = N[i][j].add(BigInteger.ONE);
    }
    
    return N;
  }
  
  
  
  // ==========  Distinct subsequences with span m  ======================
  
  /** count the number of distinct subsequences of a given string t=t1..tn,
   * that have a span of exactly m, for each m=0..n.
   * Note: The alphabet is restricted to 256 characters. 
   * Otherwise, a runtime exception will occur.
   * 
   *@param t  the string
   *@return an array T[] 
   *  with T[m] = number of distinct subsequences in t with span m
   */
  static BigInteger[] countSubsequencesSpan(final String t) {
    int[] F = new int[256]; // array of first indices of letters 0..255
    int[] L = new int[256]; // array of last indices of letters 0..255
    final int n = t.length();
    // Initialize the result array: T[m] will be the result for span m.
    BigInteger[] T = new BigInteger[n+1];
    T[0] = BigInteger.ONE;
    T[1] = BigInteger.ZERO;
    for(int m=2; m<=n; m++) T[m] = BigInteger.ZERO;
    // Compute F, L.
    for(int i=1; i<=n; i++) { 
      final char c = t.charAt(i-1);
      L[c] = i;
      if (F[c]==0) { F[c]=i; T[1] = T[1].add(BigInteger.ONE); }
    }
    for(char a=0; a<256; a++) {
      if (F[a]==0) continue;
      BigInteger[] N = countSubsequences(t.substring(F[a]+1-1)); // subtract one since indexing starts at zero
      for (char b=0; b<256; b++) {
        if (F[b]==0) continue;
        final int m=L[b]-F[a]+1;
        if (m>=2) T[m] = T[m].add(N[m-2]);
      }
    }
    return T;
  } 
   

  /** count the number of distinct subsequences of a given string t=t1..tn,
   * that have a span of exactly m, for each m=0..n, 
   * and a length of exactly k, for each k=0..n.
   * Note: The alphabet is restricted to 256 characters. 
   * Otherwise, a runtime exception will occur.
   * 
   *@param t  the string
   *@return a matrix T[][]
   *  with T[m][k] = number of distinct subsequences in t with span m
   *  and length k.
   */
  static BigInteger[][] countSubsequencesSpanL(final String t) {
    int[] F = new int[256]; // array of first indices of letters 0..255
    int[] L = new int[256]; // array of last indices of letters 0..255
    final int n = t.length();
    // Initialize the result array: T[m] will be the result for span m.
    BigInteger[][] T = new BigInteger[n+1][n+1];
    T[0][0] = BigInteger.ONE;
    for(int k=1; k<=n; k++) T[0][k] = BigInteger.ZERO; // span 0, length k
    for(int m=1; m<=n; m++) for(int k=0; k<=n; k++) T[m][k] = BigInteger.ZERO;

    // Compute F, L.
    for(int i=1; i<=n; i++) { 
      final char c = t.charAt(i-1); // (i-1) because indexing starts at 0 in t
      L[c] = i;
      // take care of the single-letter subsequences (span 1, length 1)
      if (F[c]==0) { F[c]=i; T[1][1] = T[1][1].add(BigInteger.ONE); } 
    }
    for(char a=0; a<256; a++) {
      if (F[a]==0) continue;
      BigInteger[][] N = countSubsequencesL(t.substring(F[a]+1-1)); // subtract one since indexing starts at zero
      for (char b=0; b<256; b++) {
        if (F[b]==0) continue;
        final int m=L[b]-F[a]+1; // the span of "ab"
        if (m>=2) 
          for(int k=2; k<N[0].length; k++)  T[m][k] = T[m][k].add(N[m-2][k-2]);
      }
    }
    return T;
  }

  
  // ================= NAIVE ===============================================
  // Here's a completely naive exponential-time way to count common subsequences,
  // using hashes. Run only on short strings, for testing purposes!
  
  
  
  /** compute, in a naive and inefficient exponential-time way,
   * the number of distinct subsequences in a given string.
   * CAUTION: Run only on strings of length up to 20.
   *
   *@param x  the string
   *@return  the number of distinct subsequences
   */
  static BigInteger countSubsequences_Naive(final String x) {
    return countSubsequences_Naive(x,null,false,0,0);
  }
  
  
  
  /** compute, in a naive and inefficient way, the number of common distinct
   * subsequences in two given strings. If the second string is null,
   * count the number of distinct subsequences in the first string.
   * CAUTION: Run only on short strings, for testing purposes.
   *@param x  first string
   *@param y  second string or null
   *@param weighted   set to false to count each common subsequence once;
   *  set to true to count each subsequence according to its number of joint embeddings,
   *  i.e., the product of embeddings in each string.
   *@param minspan  minimum span of subsequences to consider (0: no restriction)
   *@param maxgap   maximal gap of subsequences to consider
   *@return  the number of distinct [common] subsequences of x [and y], [with span at least minspan]
   */
  static BigInteger countSubsequences_Naive(final String x, final String y, 
      final boolean weighted, final int minspan, final int maxgap) {

    // evaluate all embeddings of x [and y]
    HashMap<String,Integer> seenx = subsequenceHash(x,minspan,maxgap);
    if (y==null) return BigInteger.valueOf(seenx.size());
    HashMap<String,Integer> seeny = subsequenceHash(y,minspan,maxgap);
    
    BigInteger result = BigInteger.ZERO;
    if (!weighted) { // simple count
      int count=0;
      for(String s : seenx.keySet()) 
        if (seeny.containsKey(s)) count++;
      result = BigInteger.valueOf(count);
    } else {         // count embeddings
      for(String s : seenx.keySet())
        if (seeny.containsKey(s))
          result = result.add( BigInteger.valueOf(seenx.get(s)*seeny.get(s)) );
    }
    return result;
  }
 
  
  /** generate a hash that, for a given string t, contains each subsequence of t as a key,
   * and the number of embeddings [of given minimum span, with bounded gap] of this subsequence as value.
   *@param t  the string
   *@param minspan  the minimum span of the embedding (use 0 for no restrictions)
   *@param maxgap  the maximally allowed gap between two characters in an embedding (use 0 for infinity)
   *@return the hash
   */
  private static HashMap<String,Integer> subsequenceHash(final String t, 
      final int minspan, final int maxgap) {
    final int n = t.length();
    if (n>=31) throw new IllegalArgumentException("string too long");
    final int N = (1<<n);
    if (maxgap<0) throw new IllegalArgumentException("maxgap must be positive or zero(=infinity)");

    // evaluate all embeddings of t
    HashMap<String,Integer> seen = new HashMap<String,Integer>(N);
    for(int i=0; i<N; i++) {
      int span = 31-Integer.numberOfLeadingZeros(i) - Integer.numberOfTrailingZeros(i) + 1;
      if (span==-32) span=0; // catch the special case where all bits are zero
      if (span<minspan) continue; // skip this one, as minspan is violated
      int ii=i;
      final StringBuilder s = new StringBuilder(n);
      int lastk=-1, k;
      while((k=Integer.numberOfTrailingZeros(ii))<32) {
        final int gap = (maxgap==0 || lastk<0)? 0: (k - lastk);
        if (gap>maxgap) { lastk=n+1; break; }
        lastk = k;
        s.append(t.charAt(k));
        ii &= ~(1<<k);
      }
      if (lastk>n) continue; // skip this one, as maxgap is violated
      final String si = s.toString();
      seen.put(si, seen.containsKey(si)? seen.get(si)+1 : 1);
    }
    return seen;
  }

  
  
// ========== rho-generated and rho-restricted subsequences ===================

  /** count the number of rho-generated rho-restricted sequences from a given
   * string x, for a given positive integer rho.
   *@param x    the string
   *@param rho  the restriction parameter
   *@return an array C[] with
   * C[i] = number of rho-generated rho-restricted sequences from x1..xi
   */
  public static BigInteger[] countGeneratedSequences(final String x, final int rho) {
    final int      n  = x.length();
    BigInteger[][] CL = countGeneratedSequencesL(x,rho, n*rho);
    BigInteger[]   C  = new BigInteger[n+1];
    for(int i=0; i<=n; i++) {
      BigInteger ci = BigInteger.ZERO;
      for(int k=0; k<=n*rho; k++)  ci = ci.add(CL[i][k]);
      C[i] = ci;
    }
    return C;
  }

  
  /** count the number of rho-generated rho-restricted sequences of a given
   *  length, from a given string x, for a given positive integer rho.
   *@param x    the string
   *@param rho  the restriction parameter
   *@param K    the maximum length for which a computation is necessary
   *@return a matrix C[0..|x|][0..K] with
   * C[i][k] = number of rho-generated rho-restricted sequences of length k from x1..xi
   */
  public static BigInteger[][] countGeneratedSequencesL(final String x, final int rho, final int K) {
    final int n = x.length();
    BigInteger[][] C = new BigInteger[n+1][K+1]; // C[i][k] result
    BigInteger[][] A = new BigInteger[K+1][256]; // A[k][c] current #subseq of length j that end with c
    C[0][0] = BigInteger.ONE;
    for(int k=1; k<=K; k++) C[0][k]=BigInteger.ZERO;
    for(int k=0; k<=K; k++) { for (int ch=0; ch<256; ch++) A[k][ch] = BigInteger.ZERO; }
    
    for (int i=1; i<=n; i++) {
      C[i][0] = BigInteger.ONE; // one subsequece of length 0
      final char c = x.charAt(i-1);
      // A[k][ch] unchanged for ch!=c, for all k
      // A[k][c]  must be computed,    for all k:
      for (int k=K; k>=1; k--) {
        C[i][k] = C[i-1][k].subtract(A[k][c]);
        A[k][c] = BigInteger.ZERO;
        final int R = (rho<k)? rho:k;
        for(int r=1; r<=R; r++) A[k][c] = A[k][c].add(C[i-1][k-r]).subtract(A[k-r][c]);
        C[i][k] = C[i][k].add(A[k][c]);
      }
    }
    return C;
  }
  
  
  
// ========== Distribution of length of longest increasing subsequence ========
  
  
  /** compute the distribution of the length of the longest increasing subsequence 
   * (the LIS-length distribution for short) in length-n sequences over the 
   * alphabet {1,...,K}, for given parameters K and N, where 0<=n<=N.
   *@param K alphabet size
   *@param N maximal length of sequences to consider
   *@return a matrix C[0..N][0..K] BigIntegers
   *   with C[n,l] =  number of K-ary sequences of length n with LIS-length = l.
   */
  public static BigInteger[][] countSequencesLIS(final int K, final int N) {
    // Uses a helper array D in each step,
    // where D[lambda] = # K=ary sequences of current length with configuration lambda
    
    // Compute zweihochK = 2^K
    if (K>=Integer.SIZE-1) throw new IllegalArgumentException("Parameter K too large.");
    final int zweihochK = (1<<K);
    
    // Initialize
    final BigInteger[][] C = new BigInteger[N+1][K+1];
    C[0][0] = BigInteger.ONE;
    for(int k=1; k<=K; k++) C[0][k] = BigInteger.ZERO;
    final BigInteger[] Dold = new BigInteger[zweihochK];
    Dold[0] = BigInteger.ONE;
    for(int lb=1; lb<zweihochK; lb++) Dold[lb] = BigInteger.ZERO;
    final BigInteger[] Dnew = new BigInteger[zweihochK];
    
    for(int n=1; n<=N; n++) {
      BigInteger Cn[] = C[n];
      for(int lb=0; lb<zweihochK; lb++) Dnew[lb] = BigInteger.ZERO;
      for(int lb=0; lb<zweihochK; lb++) {
        for(int k=0; k<K; k++) {
          final int conf = LISconfigupdate(lb,k);
          Dnew[conf] = Dnew[conf].add(Dold[lb]);
        }
      }
      for(int l=0; l<=K; l++) C[n][l] = BigInteger.ZERO;
      for(int lb=0; lb<zweihochK; lb++) {
        int l = Integer.bitCount(lb);
        Cn[l] = Cn[l].add(Dnew[lb]);
        Dold[lb] = Dnew[lb];
      }
    }
    return(C);
  }
  
  
  /** Compute the probability distribution of the length of the longest increasing subsequence
   *  (LIS-length for short) among sequences of length n over the alphabet {1,...,K},
   *  for each n=0...N, for given parameters K and N.
   *  It is assumed that the sequences follow an i.i.d. model with letter probabilties 
   *  given by a double array w=(w[0]...w[K-1]) with nonnegative entries.
   *
   *@param K alphabet size
   *@param N maximal length of sequences to consider
   *@param w letter probabilities, which must be nonnegative. If they do not sum to 1 initially,
   * they are automatically normalized. Trying to use a negative probability
   * results in an IllegalArgumentException. If a null reference
   * is passed for w, a uniform distribution is assumed.
   *@return matrix P[0..N][0..K] with
   * P[n][l] = probability in the iid model defined by w[] that a random sequences of length n has LIS-length = l
   */
  public static double[][] weightSequencesLIS(final int K, final int N, double[] w) {
    
    if (K>=Integer.SIZE-1) throw new IllegalArgumentException("Parameter K too large.");
    
    if (w==null) {
      w = new double[K];
      Arrays.fill(w, 1.0/K);
    }
    if (w.length != K) throw new IllegalArgumentException("Parameter w[] has wrong length.");
    
    double wsum=0.0;
    for(int k=0; k<K; k++) {
      wsum+=w[k];
      if (w[k]<0) throw new IllegalArgumentException("Entries of parameter w[] must be >=0.");
    }
    for(int k=0; k<K; k++) w[k]/=wsum;
    
    final int zweihochK = (1<<K);
    
    // Initialize
    double [][] P = new double[N+1][K+1];
    P[0][0] = 1.0;
    double[] Dold = new double[zweihochK];
    for(int lb=1; lb<zweihochK; lb++) Dold[lb] = 0.0;
    Dold[0] = 1.0;
    double[] Dnew = new double[zweihochK];
    
    for(int n=1; n<=N; n++) {
      double Pn[] = P[n];
      for(int lb=0; lb<zweihochK; lb++) Dnew[lb] = 0.0;
      for(int lb=0; lb<zweihochK; lb++) {
        for(int k=0; k<K; k++) {
          Dnew[LISconfigupdate(lb,k)] += w[k] * Dold[lb];
        }
      }
      for(int l=0; l<=K; l++) P[n][l] = 0.0;
      for(int lb=0; lb<zweihochK; lb++) {
        Pn[Integer.bitCount(lb)] += (Dold[lb] = Dnew[lb]);
      }
    }
    return(P);
  }
  
  
  private static int LISconfigupdate(int lb, int bit) {
    assert(bit<Integer.SIZE);
    // 1. If bit# 'bit' is already set in lb, do nothing
    int B = (1<<bit);
    if ((lb & B) != 0) return(lb);
    // 2. Set bit 'bit'
    // 3. Now find the next higher set bit and set it to zero.
    lb |= B;
    for(bit=bit+1; bit<Integer.SIZE; bit++) {
      B = (1<<bit);
      if ((lb & B) != 0) { lb &= ~B; break; }
    }
    return(lb);
  }
  
  
// ========== END OF CLASS =============================
  
}

