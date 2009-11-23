package verjinxer.sequenceanalysis;


import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class MatchAutomatonTest {
   
   
   static byte A;
   static byte C;
   static byte G;
   static byte T;
   static byte sep;
   static byte wild;
   static byte lineend;
   

   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
      final Alphabet dna = Alphabet.DNA();
      A = dna.code((byte)'A');
      C = dna.code((byte)'C');
      G = dna.code((byte)'G');
      T = dna.code((byte)'T');
      sep = dna.codeSeparator();
      wild = dna.codeWildcard();
      lineend = dna.codeEndOfLine();
   }

   @AfterClass
   public static void tearDownAfterClass() throws Exception {
   }
   
   @Test
   public void test_CGA_TGA(){
      MatchAutomaton ac = MatchAutomaton.bisulfitMatchAutomaton();
      assertTrue(ac.step(C, T));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(G, G));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(A, A));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
   }
   
   @Test
   public void test_CGA_CGA(){
      MatchAutomaton ac = MatchAutomaton.bisulfitMatchAutomaton();
      assertTrue(ac.step(C, C));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(G, G));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(A, A));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
   }
   
   @Test
   public void test_CGCG_CGTG(){
      MatchAutomaton ac = MatchAutomaton.bisulfitMatchAutomaton();
      assertTrue(ac.step(C, C));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(G, G));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(C, T));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(G, G));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
   }
   
   @Test
   public void test_CG_CA(){
      MatchAutomaton ac = MatchAutomaton.bisulfitMatchAutomaton();
      assertTrue(ac.step(C, C));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(G, A));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
   }
   
   @Test
   public void test_CGCGC_CACGC(){
      MatchAutomaton ac = MatchAutomaton.bisulfitMatchAutomaton();
      assertTrue(ac.step(C, C));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(G, A));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(C, C));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(G, G));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(C, C));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
   }
   
   @Test
   public void test_ACGT_ACGT(){
      MatchAutomaton ac = MatchAutomaton.bisulfitMatchAutomaton();
      assertTrue(ac.step(A, A));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(C, C));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(G, G));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(T, T));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
   }
   
   @Test
   public void test_ACGT_ATGT(){
      MatchAutomaton ac = MatchAutomaton.bisulfitMatchAutomaton();
      assertTrue(ac.step(A, A));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(C, T));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(G, G));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(T, T));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
   }
   
   @Test
   public void test_CCGT_CCAT(){
      MatchAutomaton ac = MatchAutomaton.bisulfitMatchAutomaton();
      assertTrue(ac.step(C, C));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(C, C));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(G, A));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(T, T));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
   }
   
   @Test
   public void test_ACCCGTTA_ATTTGTTA(){
      MatchAutomaton ac = MatchAutomaton.bisulfitMatchAutomaton();
      assertTrue(ac.step(A, A));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(C, T));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(C, T));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(C, T));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(G, G));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(T, T));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(T, T));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(A, A));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
   }
   
   @Test
   public void test_CGC_TGC(){
      MatchAutomaton ac = MatchAutomaton.bisulfitMatchAutomaton();
      assertTrue(ac.step(C, T));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(G, G));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertFalse(ac.step(C, C));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertFalse(ac.isErrorState());
   }
   
   @Test
   public void test_CGA_CGT(){
      MatchAutomaton ac = MatchAutomaton.bisulfitMatchAutomaton();
      assertTrue(ac.step(C, C));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(G, G));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertFalse(ac.step(A, C));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
   }
   
   @Test
   public void test_CGCGCG_CGTGCA(){
      MatchAutomaton ac = MatchAutomaton.bisulfitMatchAutomaton();
      assertTrue(ac.step(C, C));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(G, G));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(C, T));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(G, G));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertFalse(ac.step(C, C));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(G, G));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertFalse(ac.step(C, C));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertFalse(ac.step(G, A));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
   }
   
   @Test
   public void test_ATGCCA_ATGTCT(){
      MatchAutomaton ac = MatchAutomaton.bisulfitMatchAutomaton();
      assertTrue(ac.step(A, A));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(T, T));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(G, G));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(C, T));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertFalse(ac.step(C, C));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertFalse(ac.step(A, T));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
   }
   
   // check wildcards, seperators and lineends
   
   @Test
   public void test_ATsepAT_ATsepAT(){
      MatchAutomaton ac = MatchAutomaton.bisulfitMatchAutomaton();
      assertTrue(ac.step(A, A));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(T, T));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertFalse(ac.step(sep, sep));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(A, A));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(T, T));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
   }
   
   @Test
   public void test_ATwildAT_ATwildAT(){
      MatchAutomaton ac = MatchAutomaton.bisulfitMatchAutomaton();
      assertTrue(ac.step(A, A));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(T, T));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertFalse(ac.step(wild, wild));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(A, A));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(T, T));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
   }
   
   @Test
   public void test_ATsepAT_ATTAT(){
      MatchAutomaton ac = MatchAutomaton.bisulfitMatchAutomaton();
      assertTrue(ac.step(A, A));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(T, T));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertFalse(ac.step(sep, T));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(A, A));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(T, T));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
   }
   

   
   @Test
   public void test_ATwildAT_ATTAT(){
      MatchAutomaton ac = MatchAutomaton.bisulfitMatchAutomaton();
      assertTrue(ac.step(A, A));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(T, T));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertFalse(ac.step(wild, T));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(A, A));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(T, T));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
   }
   
   @Test
   public void test_ATTAT_ATsepAT(){
      MatchAutomaton ac = MatchAutomaton.bisulfitMatchAutomaton();
      assertTrue(ac.step(A, A));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(T, T));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertFalse(ac.step(T, sep));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(A, A));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(T, T));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
   }
   

   
   @Test
   public void test_ATTAT_ATwildAT(){
      MatchAutomaton ac = MatchAutomaton.bisulfitMatchAutomaton();
      assertTrue(ac.step(A, A));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(T, T));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertFalse(ac.step(T, wild));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(A, A));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(T, T));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
   }
   
   @Test
   public void test_ATleAT_ATleAT(){
      MatchAutomaton ac = MatchAutomaton.bisulfitMatchAutomaton();
      assertTrue(ac.step(A, A));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(T, T));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertFalse(ac.step(lineend, lineend));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(A, A));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(T, T));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
   }
   
   @Test
   public void test_ATTAT_ATleAT(){
      MatchAutomaton ac = MatchAutomaton.bisulfitMatchAutomaton();
      assertTrue(ac.step(A, A));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(T, T));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertFalse(ac.step(T, lineend));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(A, A));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(T, T));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
   }
   
   @Test
   public void test_ATleAT_ATTAT(){
      MatchAutomaton ac = MatchAutomaton.bisulfitMatchAutomaton();
      assertTrue(ac.step(A, A));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(T, T));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertFalse(ac.step(lineend, T));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(A, A));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(T, T));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
   }
   
   //---------------------------------------
   // in CC state
   
   @Test
   public void test_ACsepAT_ACsepAT(){
      MatchAutomaton ac = MatchAutomaton.bisulfitMatchAutomaton();
      assertTrue(ac.step(A, A));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(C, C));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertFalse(ac.step(sep, sep));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(A, A));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(T, T));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
   }
   
   @Test
   public void test_ACwildAT_ACwildAT(){
      MatchAutomaton ac = MatchAutomaton.bisulfitMatchAutomaton();
      assertTrue(ac.step(A, A));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(C, C));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertFalse(ac.step(wild, wild));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(A, A));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(T, T));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
   }
   
   @Test
   public void test_ACsepAT_ACTAT(){
      MatchAutomaton ac = MatchAutomaton.bisulfitMatchAutomaton();
      assertTrue(ac.step(A, A));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(C, C));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertFalse(ac.step(sep, T));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(A, A));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(T, T));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
   }
   

   
   @Test
   public void test_ACwildAT_ACTAT(){
      MatchAutomaton ac = MatchAutomaton.bisulfitMatchAutomaton();
      assertTrue(ac.step(A, A));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(C, C));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertFalse(ac.step(wild, T));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(A, A));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(T, T));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
   }
   
   @Test
   public void test_ACTAT_ACsepAT(){
      MatchAutomaton ac = MatchAutomaton.bisulfitMatchAutomaton();
      assertTrue(ac.step(A, A));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(C, C));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertFalse(ac.step(T, sep));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(A, A));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(T, T));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
   }
   

   
   @Test
   public void test_ACTAT_ACwildAT(){
      MatchAutomaton ac = MatchAutomaton.bisulfitMatchAutomaton();
      assertTrue(ac.step(A, A));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(C, C));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertFalse(ac.step(T, wild));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(A, A));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(T, T));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
   }
   
   @Test
   public void test_ACleAT_ACleAT(){
      MatchAutomaton ac = MatchAutomaton.bisulfitMatchAutomaton();
      assertTrue(ac.step(A, A));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(C, C));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertFalse(ac.step(lineend, lineend));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(A, A));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(T, T));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
   }
   
   @Test
   public void test_ACTAT_ACleAT(){
      MatchAutomaton ac = MatchAutomaton.bisulfitMatchAutomaton();
      assertTrue(ac.step(A, A));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(C, C));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertFalse(ac.step(T, lineend));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(A, A));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(T, T));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
   }
   
   @Test
   public void test_ACleAT_ACTAT(){
      MatchAutomaton ac = MatchAutomaton.bisulfitMatchAutomaton();
      assertTrue(ac.step(A, A));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(C, C));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertFalse(ac.step(lineend, T));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(A, A));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(T, T));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
   }
   
   @Test
   public void testReset(){
      MatchAutomaton ac = MatchAutomaton.bisulfitMatchAutomaton();
      assertTrue(ac.step(A, A));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(C, C));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertFalse(ac.step(lineend, T));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(A, A));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(T, T));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      ac.reset();
      assertTrue(ac.step(A, A));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(C, C));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertFalse(ac.step(lineend, T));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(A, A));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      assertFalse(ac.step(T, T));
      assertFalse(ac.isAccepting());
      assertTrue(ac.isRefusing());
      assertTrue(ac.isErrorState());
      
      ac.reset();
      assertTrue(ac.step(C, C));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(G, G));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(C, T));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(G, G));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      ac.reset();
      assertTrue(ac.step(C, C));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(C, C));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(G, A));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
      
      assertTrue(ac.step(T, T));
      assertTrue(ac.isAccepting());
      assertFalse(ac.isRefusing());
      assertFalse(ac.isErrorState());
   }

}
