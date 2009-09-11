package verjinxer;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import org.junit.BeforeClass;
import org.junit.Test;

import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.sequenceanalysis.BWTIndex;
import verjinxer.sequenceanalysis.Sequences;
import verjinxer.sequenceanalysis.SuffixXorDLL;

public class BWTSearchTest {
   
   // a prefix of chrM
   private static final byte[] chrM = {2,0,3,1,0,1,0,2,2,3,1,3,0,3,1,0,1,1,1,3,0,3,3,0,0,1,1,0,1,3,1,0,1,2,2,2,0,2,1,3,1,3,1,1,0,3,2,1,0,3,3,3,2,2,3,0,3,3,3,3,1,2,3,1,3,2,2,2,2,2,2,3,2,3,2,1,0,1,2,1,2,0,3,0,2,1,0,3,3,2,1,2,0,2,0,1,2,1,3,2,2,0,2,1,1,2,2,0,2,1,0,1,1,1,3,0,3,2,3,1,2,1,0,2,3,0,3,1,3,2,3,1,3,3,3,2,0,3,3,1,1,3,2,1,1,3,1,0,3,3,1,3,0,3,3,0,3,3,3,0,3,1,2,1,0,1,1,3,0,1,2,3,3,1,0,0,3,0,3,3,0,1,0,2,2,1,2,0,0,1,0,3,0,1,1,3,0,1,3,0,0,0,2,3,2,3,2,3,3,0,0,3,3,0,0,3,3,0,0,3,2,1,3,3,2,3,0,2,2,0,1,0,3,0,0,3,0,0,3,0,0,1,0,0,3,3,2,0,0,3,2,3,1,3,2,1,0,1,0,2,1,1,2,1,3,3,3,1,1,0,1,0,1,0,2,0,1,0,3,1,0,3,0,0,1,0,0,0,0,0,0,3,3,3,1,1,0,1,1,0,0,0,1,1,1,1,1,1,1,1,3,1,1,1,1,1,1,2,1,3,3,1,3,2,2,1,1,0,1,0,2,1,0,1,3,3,0,0,0,1,0,1,0,3,1,3,1,3,2,1,1,0,0,0,1,1,1,1,0,0,0,0,0,1,0,0,0,2,0,0,1,1,1,3,0,0,1,0,1,1,0,2,1,1,3,0,0,1,1,0,2,0,3,3,3,1,0,0,0,3,3,3,3,0,3,1,3,3,3,0,2,2,1,2,2,3,0,3,2,1,0,1,3,3,3,3,0,0,1,0,2,3,1,0,1,1,1,1,1,1,0,0,1,3,0,0,1,0,1,0,3,3,0,3,3,3,3,1,1,1,1,3,1,1,1,0,1,3,1,1,1,0,3,0,1,3,0,1,3,0,0,3,1,3,1,0,3,1,0,0,3,0,1,0,0,1,1,1,1,1,2,1,1,1,0,3,1,1,3,0,1,1,1,0,2,1,0,1,0,1,0,1,0,1,0,1,1,2,1,3,2,1,3,0,0,1,1,1,1,0,3,0,1,1,1,1,2,0,0,1,1,0,0,1,1,0,0,0,1,1,1,1,0,0,0,2,0,1,0,1,1,1,1,1,1,0,1,0,2,3,3,3,0,3,2,3,0,2,1,3,3,0,1,1,3,1,1,3,1,0,0,0,2,1,0,0,3,0,1,0,1,3,2,0,0,0,0,3,2,3,3,3,0,2,0,1,2,2,2,1,3,1,0,1,0,3,1,0,1,1,1,1,0,3,0,0,0,1,0,0,0,3,0,2,2,3,3,3,2,2,3,1,1,3,0,2,1,1,3,3,3,1,3,0,3,3,0,2,1,3,1,3,3,0,2,3,0,0,2,0,3,3,0,1,0,1,0,3,2,1,0,0,2,1,0,3,1,1,1,1,2,3,3,1,1,0,2,3,2,0,2,3,3,1,0,1,1,1,3,1,3,0,0,0,3,1,0,1,1,0,1,2,0,3,1,0,0,0,0,2,2,2,0,1,0,0,2,1,0,3,1,0,0,2,1,0,1,2,1,0,2,1,0,0,3,2,1,0,2,1,3,1,0,0,0,0,1,2,1,3,3,0,2,1,1,3,0,2,1,1,0,1,0,1,1,1,1,1,0,1,2,2,2,0,0,0,1,0,2,1,0,2,3,2,0,3,3,0,0,1,1,3,3,3,0,2,1,0,0,3,0,0,0,1,2,0,0,0,2,3,3,3,0,0,1,3,0,0,2,1,3,0,3,0,1,3,0,0,1,1,1,1,0,2,2,2,3,3,2,2,3,1,0,0,3,3,3,1,2,3,2,1,1,0,2,1,1,0,1,1,2,1,2,2,3,1,0,1,0,1,2,0,3,3,0,0,1,1,1,0,0,2,3,1,0,0,3,0,2,0,0,2,1,1,2,2,1,2,3,0,0,0,2,0,2,3,2,3,3,3,3,0,2,0,3,1,0,1,1,1,1,1,3,1,1,1,1,0,0,3,0,0,0,2,1,3,0,0,0,0,1,3,1,0,1,1,3,2,0,2,3,3,2,3,0,0,0,0,0,0,1,3,1,1,0,2,3,3,2,0,1,0,1,0,0,0,0,3,0,2,0,1,3,0,1,2,0,0,0,2,3,2,2,1,3,3,3,0,0,1,0,3,0,3,1,3,2,0,0,1,0,1,0,1,0,0,3,0,2,1,3,0,0,2,0,1,1,1,0,0,0,1,3,2,2,2,0,3,3,0,2,0,3,0,1,1,1,1,0,1,3,0,3,2,1,3,3,0,2,1,1,1,3,0,0,0,1,1,3,1,0,0,1,0,2,3,3,0,0,0,3,1,0,0,1,0,0,0,0,1,3,2,1,3,1,2,1,1,0,2,0,0,1,0,1,3,0,1,2,0,2,1,1,0,1,0,2,1,3,3,0,0,0,0,1,3,1,0,0,0,2,2,0,1,1,3,2,2,1,2,2,3,2,1,3,3,1,0,3,0,3,1,1,1,3,1,3,0,2,0,2,2,0,2,1,1,3,2,3,3,1,3,2,3,0,0,3,1,2,0,3,0,0,0,1,1,1,1,2,0,3,1,0,0,1,1,3,1,0,1,1,0,1,1,3,1,3,3,2,1,3,1,0,2,1,1,3,0,3,0,3,0,1,1,2,1,1,0,3,1,3,3,1,0,2,1,0,0,0,1,1,1,3,2,0,3,2,0,0,2,2,1,3,0,1,0,0,0,2,3,0,0,2,1,2,1,0,0,2,3,0,1,1,1,0,1,2,3,0,0,0,2,0,1,2,3,3,0,2,2,3,1,0,0,2,2,3,2,3,0,2,1,1,1,0,3,2,0,2,2,3,2,2,1,0,0,2,0,0,0,3,2,2,2,1,3,0,1,0,3,3,3,3,1,3,0,1,1,1,1,0,2,0,0,0,0,1,3,0,1,2,0,3,0,2,1,1,1,3,3,0,3,2,0,0,0,1,3,3,0,0,2,2,2,3,1,2,0,0,2,2,3,2,2,0,3,3,3,0,2,1,0,2,3,0,0,0,1,3,2,0,2,0,2,3,0,2,0,2,3,2,1,3,3,0,2,3,3,2,0,0,1,0,2,2,2,1,1,1,3,2,0,0,2,1,2,1,2,3,0,1,0,1,0,1,1,2,1,1,1,2,3,1,0,1,1,1,3,1,1,3,1,0,0,2,3,0,3,0,1,3,3,1,0,0,0,2,2,0,1,0,3,3,3,0,0,1,3,0,0,0,0,1,1,1,1,3,0,1,2,1,0,3,3,3,0,3,0,3,0,2,0,2,2,0,2,0,1,0,0,2,3,1,2,3,0,0,1,0,3,2,2,3,0,0,2,3,2,3,0,1,3,2,2,0,0,0,2,3,2,1,0,1,3,3,2,2,0,1,2,0,0,1,1,0,2,0,2,3,2,3,0,2,1,3,3,0,0,1,0,1,0,0,0,2,1,0,1,1,1,0,0,1,3,3,0,1,0,1,3,3,0,2,2,0,2,0,3,3,3,1,0,0,1,3,3,0,0,1,3,3,2,0,1,1,2,1,3,1,3,2,0,2,1,3,0,0,0,1,1,3,0,2,1,1,1,1,0,0,0,1,1,1,0,1,3,1,1,0,1,1,3,3,0,1,3,0,1,1,0,2,0,1,0,0,1,1,3,3,0,2,1,1,0,0,0,1,1,0,3,3,3,0,1,1,1,0,0,0,3,0,0,0,2,3,0,3,0,2,2,1,2,0,3,0,2,0,0,0,3,3,2,0,0,0,1,1,3,2,2,1,2,1,0,0,3,0,2,0,3,0,3,0,2,3,0,1,1,2,1,0,0,2,2,2,0,0,0,2,0,3,2,0,0,0,0,0,3,3,0,3,0,0,1,1,0,0,2,1,0,3,0,0,3,0,3,0,2,1,0,0,2,2,0,1,3,0,0,1,1,1,1,3,0,3,0,1,1,3,3,1,3,2,1,0,3,0,0,3,2,0,0,3,3,0,0,1,3,0,2,0,0,0,3,0,0,1,3,3,3,2,1,0,0,2,2,0,2,0,2,1,1,0,0,0,2,1,3,0,0,2,0,1,1,1,1,1,2,0,0,0,1,1,0,2,0,1,2,0,2,1,3,0,1,1,3,0,0,2,0,0,1,0,2,1,3,0,0,0,0,2,0,2,1,0,1,0,1,1,1,2,3,1,3,0,3,2,3,0,2,1,0,0,0,0,3,0,2,3,2,2,2,0,0,2,0,3,3,3,0,3,0,2,2,3,0,2,0,2,2,1,2,0,1,0,0,0,1,1,3,0,1,1,2,0,2,1,1,3,2,2,3,2,0,3,0,2,1,3,2,2,3,3,2,3,1,1,0,0,2,0,3,0,2,0,0,3,1,3,3,0,2,3,3,1,0,0,1,3,3,3,0,0,0,3,3,3,2,1,1,1,0,1,0,2,0,0,1,1,1,3,1,3,0,0,0,3,1,1,1,1,3,3,2,3,0,0,0,3,3,3,0,0,1,3,2,3,3,0,2,3,1,1,0,0,0,2,0,2,2,0,0,1,0,2,1,3,1,3,3,3,2,2,0,1,0,1,3,0,2,2,0,0,0,0,0,0,1,1,3,3,2,3,0,2,0,2,0,2,0,2,3,0,0,0,0,0,0,3,3,3,0,0,1,0,1,1,1,0,3,0,2,3,0,2,2,1,1,3,0,0,0,0,2,1,0,2,1,1,0,1,1,0,0,3,3,0,0,2,0,0,0,2,1,2,3,3,1,0,0,2,1,3,1,0,0,1,0,1,1,1,0,1,3,0,1,1,3,0,0,0,0,0,0,3,1,1,1,0,0,0,1,0,3,0,3,0,0,1,3,2,0,0,1,3,1,1,3,1,0,1,0,1,1,1,0,0,3,3,2,2,0,1,1,0,0,3,1,3,0,3,1,0,1,1,1,3,0,3,0,2,0,0,2,0,0,1,3,0,0,3,2,3,3,0,2,3,0,3,0,0,2,3,0,0,1,0,3,2,0,0,0,0,1,0,3,3,1,3,1,1,3,1,1,2,1,0,3,0,0,2,1,1,3,2,1,2,3,1,0,2,0,3,1,0,0,0,0,1,0,1,3,2,0,0,1,3,2,0,1,0,0,3,3,0,0,1,0,2,1,1,1,0,0,3,0,3,1,3,0,1,0,0,3,1,0,0,1,1,0,0,1,0,0,2,3,1,0,3,3,0,3,3,0,1,1,1,3,1,0,1,3,2,3,1,0,0,1,1,1,0,0,1,0,1,0,2,2,1,0,3,2,1,3,1,0,3,0,0,2,2,0,0,0,2,2,3,3,0,0,0,0,0,0,0,2,3,0,0,0,0,2,2,0,0,1,3,1,2,2,1,0,0,0,1,1,3,3,0,1,1,1,1,2,1,1,3,2,3,3,3,0,1,1,0,0,0,0,0,1,0,3,1,0,1,1,3,1,3,0,2,1,0,3,1,0,1,1,0,2,3,0,3,3,0,2,0,2,2,1,0,1,1,2,1,1,3,2,1,1,1,0,2,3,2,0,1,0,1,0,3,2,3,3,3,0,0,1,2,2,1,1,2,1,2,2,3,0,1,1,1,3,0,0,1,1,2,3,2,1,0,0,0,2,2,3,0,2,1,0,3,0,0,3,1,0,1,3,3,2,3,3,1,1,3,3,0,0,0,3,0,2,2,2,0,1,1,3,2,3,0,3,2,0,0,3,2,2,1,3,1,1,0,1,2,0,2,2,2,3,3,1,0,2,1,3,2,3,1,3,1,3,3,0,1,3,3,3,3,0,0,1,1,0,2,3,2,0,0,0,3,3,2,0,1,1,3,2,1,1,1,2,3,2,0,0,2,0,2,2,1,2,2,2,1,0,3,2,0,1,0,1,0,2,1,0,0,2,0,1,2,0,2,0,0,2,0,1,1,1,3,0,3,2,2,0,2,1,3,3,3,0,0,3,3,3,0,3,3,0,0,3,2,1,0,0,0,1,0,2,3,0,1,1,3,0,0,1,0,0,0,1,1,1,0,1,0,2,2,3,1,1,3,0,0,0,1,3,0,1,1,0,0,0,1,1,3,2,1,0,3,3,0,0,0,0,0,3,3,3,1,2,2,3,3,2,2,2,2,1,2,0,1,1,3,1,2,2,0,2,1,0,2,0,0,1,1,1,0,0,1,1,3,1,1,2,0,2,1,0,2,3,0,1,0,3,2,1,3,0,0,2,0,1,3,3,1,0,1,1,0,2,3,1,0,0,0,2,1,2,0,0,1,3,0,1,3,0,3,0,1,3,1,0,0,3,3,2,0,3,1,1,0,0,3,0,0,1,3,3,2,0,1,1,0,0,1,2,2,0,0,1,0,0,2,3,3,0,1,1,1,3,0,2,2,2,0,3,0,0,1,0,2,1,2,1,0,0,3,1,1,3,0,3,3,1,3,0,2,0,2,3,1,1,0,3,0,3,1,0,0,1,0,0,3,0,2,2,2,3,3,3,0,1,2,0,1,1,3,1,2,0,3,2,3,3,2,2,0,3,1,0,2,2,0,1,0,3,1,1,1,2,0,3,2,2,3,2,1,0,2,1,1,2,1,3,0,3,3,0,0,0,2,2,3,3,1,2,3,3,3,2,3,3,1,0,0,1,2,0,3,3,0,0,0,2,3,1,1,3,0,1,2,3,2,0,3,1,3,2,0,2,3,3,1,0,2,0,1,1,2,2,0,2,3,0,0,3,1,1,0,2,2,3,1,2,2,3,3,3,1,3,0,3,1,3,0,1,3,3,1,0,0,0,3,3,1,1,3,1,1,1,3,2,3,0,1,2,0,0,0,2,2,0,1,0,0,2,0,2,0,0,0,3,0,0,2,2,1,1,3,0,1,3,3,1,0,1,0,0,0,2,1,2,1,1,3,3,1,1,1,1,1,2,3,0,0,0,3,2,0,3,0,3,1,0,3,1,3,1,0,0,1,3,3,0,2,3,0,3,3,0,3,0,1,1,1,0,1,0,1,1,1,0,1,1,1,0,0,2,0,0,1,0,2,2,2,3,3,3,2,3,3,0,0,2,0,3,2,2,1,0,2,0,2,1,1,1,2,2,3,0,0,3,1,2,1,0,3,0,0,0,0,1,3,3,0,0,0,0,1,3,3,3,0,1,0,2,3,1,0,2,0,2,2,3,3,1,0,0,3,3,1,1,3,1,3,3,1,3,3,0,0,1,0,0,1,0,3,0,1,1,1,0,3,2,2,1,1,0,0,1,1,3,1,1,3,0,1,3,1,1,3,1,0,3,3,2,3,0,1,1,1,0,3,3,1,3,0,0,3,1,2,1,0,0,3,2,2,1,0,3,3,1,1,3,0,0,3,2,1,3,3,0,1,1,2,0,0,1,2,0,0,0,0,0,3,3,1,3,0,2,2,1,3,0,3,0,3,0,1,0,0,1,3,0,1,2,1,0,0,0,2,2,1,1,1,1,0,0,1,2,3,3,2,3,0,2,2,1,1,1,1,3,0,1,2,2,2,1,3,0,1,3,0,1,0,0,1,1,1,3,3,1,2,1,3,2,0,1,2,1,1,0,3,0,0,0,0,1,3,1,3,3,1,0,1,1,0,0,0,2,0,2,1,1,1,1,3,0,0,0,0,1,1,1,2,1,1,0,1,0,3,1,3,0,1,1,0,3,1,0,1,1,1,3,1,3,0,1,0,3,1,0,1,1,2,1,1,1,1,2,0,1,1,3,3,0,2,1,3,1,3,1,0,1,1,0,3,1,2,1,3,1,3,3,1,3,0,1,3,0,3,2,0,0,1,1,1,1,1,1,3,1,1,1,1,0,3,0,1,1,1,0,0,1,1,1,1,1,3,2,2,3,1,0,0,1,1,3,1,0,0,1,1,3,0,2,2,1,1,3,1,1,3,0,3,3,3,0,3,3,1,3,0,2,1,1,0,1,1,3,1,3,0,2,1,1,3,0,2,1,1,2,3,3,3,0,1,3,1,0,0,3,1,1,3,1,3,2,0,3,1,0,2,2,2,3,2,0,2,1,0,3,1,0,0,0,1,3,1,0,0,0,1,3,0,1,2,1,1,1,3,2,0,3,1,2,2,1,2,1,0,1,3,2,1,2,0,2,1,0,2,3,0,2,1,1,1,0,0,0,1,0,0,3,1,3,1,0,3,0,3,2,0,0,2,3,1,0,1,1,1,3,0,2,1,1,0,3,1,0,3,3,1,3,0,1,3,0,3,1,0,0,1,0,3,3,0,1,3,0,0,3,0,0,2,3,2,2,1,3,1,1,3,3,3,0,0,1,1,3,1,3,1,1,0,1,1,1,3,3,0,3,1,0,1,0,0,1,0,1,0,0,2,0,0,1,0,1,1,3,1,3,2,0,3,3,0,1,3,1,1,3,2,1,1,0,3,1,0,3,2,0,1,1,1,3,3,2,2,1,1,0,3,0,0,3,0,3,2,0,3,3,3,0,3,1,3,1,1,0,1,0,1,3,0,2,1,0,2,0,2,0,1,1,0,0,1,1,2,0,0,1,1,1,1,1,3,3,1,2,0,1,1,3,3,2,1,1,2,0,0,2,2,2,2,0,2,3,1,1,2,0,0,1,3,0,2,3,1,3,1,0,2,2,1,3,3,1,0,0,1,0,3,1,2,0,0,3,0,1,2,1,1,2,1,0,2,2,1,1,1,1,3,3,1,2,1,1,1,3,0,3,3,1,3,3,1,0,3,0,2,1,1,2,0,0,3,0,1,0,1,0,0,0,1,0,3,3,0,3,3,0,3,0,0,3,0,0,0,1,0,1,1,1,3,1,0,1,1,0,1,3,0,1,0,0,3,1,3,3,1,1,3,0,2,2,0,0,1,0,0,1,0,3,0,3,2,0,1,2,1,0,1,3,1,3,1,1,1,1,3,2,0,0,1,3,1,3,0,1,0,1,0,0,1,0,3,0,3,3,3,3,2,3,1,0,1,1,0,0,2,0,1,1,1,3,0,1,3,3,1,3,0,0,1,1,3,1,1,1,3,2,3,3,1,3,3,0,3,2,0,0,3,3,1,2,0,0,1,0,2,1,0,3,0,1,1,1,1,1,2,0,3,3,1,1,2,1,3,0,1,2,0,1,1,0,0,1,3,1,0,3,0,1,0,1,1,3,1,1,3,0,3,2,0,0,0,0,0,0,1,3,3,1,1,3,0,1,1,0,1,3,1,0,1,1,1,3,0,2,1,0,3,3,0,1,3,3,0,3,0,3,2,0,3,0,3,2,3,1,3,1,1,0,3,0,1,1,1,0,3,3,0,1,0,0,3,1,3,1,1,0,2,1,0,3,3,1,1,1,1,1,3,1,0,0,0,1,1,3,0,0,2,0,0,0,3,0,3,2,3,1,3,2,0,3,0,0,0,0,2,0,2,3,3,0,1,3,3,3,2,0,3,0,2,0,2,3,0,0,0,3,0,0,3,0,2,2,0,2,1,3,3,0,0,0,1,1,1,1,1,3,3,0,3,3,3,1,3,0,2,2,0,1,3,0,3,2,0,2,0,0,3,1,2,0,0,1,1,1,0,3,1,1,1,3,2,0,2,0,0,3,1,1,0,0,0,0,3,3,1,3,1,1,2,3,2,1,1,0,1,1,3,0,3,1,0,1,0,1,1,1,1,0,3,1,1,3,0,0,0,2,3,0,0,2,2,3,1,0,2,1,3,0,0,0,3,0,0,2,1,3,0,3,1,2,2,2,1,1,1,0,3,0,1,1,1,1,2,0,0,0,0,3,2,3,3,2,2,3,3,0,3,0,1,1,1,3,3,1,1,1,2,3,0,1,3,0,0,3,3,0,0,3,1,1,1,1,3,2,2,1,1,1,0,0,1,1,1,2,3,1,0,3,1,3,0,1,3,1,3,0,1,1,0,3,1,3,3,3,2,1,0,2,2,1,0,1,0,1,3,1,0,3,1,0,1,0,2,1,2,1,3,0,0,2,1,3,1,2,1,0,1,3,2,0,3,3,3,3,3,3,0,1,1,3,2,0,2,3,0,2,2,1,1,3,0,2,0,0,0,3,0,0,0,1,0,3,2,1,3,0,2,1,3,3,3,3,0,3,3,1,1,0,2,3,3,1,3,0,0,1,1,0,0,0,0,0,0,0,3,0,0,0,1,1,1,3,1,2,3,3,1,1,0,1,0,2,0,0,2,1,3,2,1,1,0,3,1,0,0,2,3,0,3,3,3,1,1,3,1,0,1,2,1,0,0,2,1,0,0,1,1,2,1,0,3,1,1,0,3,0,0,3,1,1,3,3,1,3,0,0,3,0,2,1,3,0,3,1,1,3,1,3,3,1,0,0,1,0,0,3,0,3,0,1,3,1,3,1,1,2,2,0,1,0,0,3,2,0,0,1,1,0,3,0,0,1,1,0,0,3,0,1,3,0,1,1,0,0,3,1,0,0,3,0,1,3,1,0,3,1,0,3,3,0,0,3,0,0,3,1,0,3,0,0,3,2,2,1,3,0,3,0,2,1,0,0,3,0,0,0,0,1,3,0,2,2,0,0,3,0,2,1,1,1,1,1,3,3,3,1,0,1,3,3,1,3,2,0,2,3,1,1,1,0,2,0,2,2,3,3,0,1,1,1,0,0,2,2,1,0,1,1,1,1,3,1,3,2,0,1,0,3,1,1,2,2,1,1,3,2,1,3,3,1,3,3,1,3,1,0,1,0,3,2,0,1,0,0,0,0,0,1,3,0,2,1,1,1,1,1,0,3,1,3,1,0,0,3,1,0,3,0,3,0,1,1,0,0,0,3,1,3,1,3,1,1,1,3,1,0,1,3,0,0,0,1,2,3,0,0,2,1,1,3,3,1,3,1,1,3,1,0,1,3,1,3,1,3,1,0,0,3,1,3,3,0,3,1,1,0,3,1,0,3,0,2,1,0,2,2,1,0,2,3,3,2,0,2,2,3,2,2,0,3,3,0,0,0,1,1,0,0,0,1,1,1,0,2,1,3,0,1,2,1,0,0,0,0,3,1,3,3,0,2,1,0,3,0,1,3,1,1,3,1,0,0,3,3,0,1,1,1,0,1,0,3,0,2,2,0,3,2,0,0,3,0,0,3,0,2,1,0,2,3,3,1,3,0,1,1,2,3,0,1,0,0,1,1,1,3,0,0,1,0,3,0,0,1,1,0,3,3,1,3,3,0,0,3,3,3,0,0,1,3,0,3,3,3,0,3,0,3,3,0,3,1,1,3,0,0,1,3,0,1,3,0,1,1,2,1,0,3,3,1,1,3,0,1,3,0,1,3,1,0,0,1,3,3,0,0,0,1,3,1,1,0,2,1,0,1,1,0,1,2,0,1,1,1,3,0,1,3,0,1,3,0,3,1,3,1,2,1,0,1,1,3,2,0,0,0,1,0,0,2,1,3,0,0,1,0,3,2,0,1,3,0,0,1,0,1,1,1,3,3,0,0,3,3,1,1,0,3,1,1,0,1,1,1,3,1,1,3,1,3,1,1,1,3,0,2,2,0,2,2,1,1,3,2,1,1,1,1,1,2,1,3,0,0,1,1,2,2,1,3,3,3,3,3,2,1,1,1,0,0,0,3,2,2,2,1,1,0,3,3,0,3,1,2,0,0,2,0,0,3,3,1,0,1,0,0,0,0,0,0,1,0,0,3,0,2,1,1,3,1,0,3,1,0,3,1,1,1,1,0,1,1,0,3,1,0,3,0,2,1,1,0,1,1,0,3,1,0,1,1,1,3,1,1,3,3,0,0,1,1,3,1,3,0,1,3,3,1,3,0,1,1,3,0,1,2,1,1,3,0,0,3,1,3,0,1,3,1,1,0,1,1,3,1,0,0,3,1,0,1,0,1,3,0,1,3,1,1,1,1,0,3,0,3,1,3,0,0,1,0,0,1,2,3,0,0,0,0,0,3,0,0,0,0,3,2,0,1,0,2,3,3,3,2,0,0,1,0,3,0,1,0,0,0,0,1,1,1,0,1,1,1,1,0,3,3,1,1,3,1,1,1,1,0,1,0,1,3,1,0,3,1,2,1,1,1,3,3,0,1,1,0,1,2,1,3,0,1,3,1,1,3,0,1,1,3,0,3,1,3,1,1,1,1,3,3,3,3,0,3,0,1,3,0,0,3,0,0,3,1,3,3,0,3,0,2,0,0,0,3,3,3,0,2,2,3,3,0,0,0,3,0,1,0,2,0,1,1,0,0,2,0,2,1,1,3,3,1,0,0,0,2,1,1,1,3,1,0,2,3,0,0,2,3,3,2,1,0,0,3,0,1,3,3,0,0,3,3,3,1,3,2,1,0,0,1,0,2,1,3,0,0,2,2,0,1,3,2,1,0,0,0,0,1,1,1,1,0,1,3,1,3,2,1,0,3,1,0,0,1,3,2,0,0,1,2,1,0,0,0,3,1,0,2,1,1,0,1,3,3,3,0,0,3,3,0,0,2,1,3,0,0,2,1,1,1,3,3,0,1,3,0,2,0,1,1,0,0,3,2,2,2,0,1,3,3,0,0,0,1,1,1,0,1,0,0,0,1,0,1,3,3,0,2,3,3,0,0,1,0,2,1,3,0,0,2,1,0,1,1,1,3,0,0,3,1,0,0,1,3,2,2,1,3,3,1,0,0,3,1,3,0,1,3,3,1,3,1,1,1,2,1,1,2,1,1,2,2,2,0,0,0,0,0,0,2,2,1,2,2,2,0,2,0,0,2,1,1,1,1,2,2,1,0,2,2,3,3,3,2,0,0,2,1,3,2,1,3,3,1,3,3,1,2,0,0,3,3,3,2,1,0,0,3,3,1,0,0,3,0,3,2,0,0,0,0,3,1,0,1,1,3,1,2,2,0,2,1,3,2,2,3,0,0,0,0,0,2,0,2,2,1,1,3,0,0,1,1,1,1,3,2,3,1,3,3,3,0,2,0,3,3,3,0,1,0,2,3,1,1,0,0,3,2,1,3,3,1,0,1,3,1,0,2,1,1,0,3,3,3,3,0,1,1,3,1,0,1,1,1,1,1,0,1,3,2,0,3,2,3,3,1,2,1,1,2,0,1,1,2,3,3,2,0,1,3,0,3,3,1,3,1,3,0,1,0,0,0,1,1,0,1,0,0,0,2,0,1,0,3,3,2,2,0,0,1,0,1,3,0,3,0,1,1,3,0,3,3,0,3,3,1,2,2,1,2,1,0,3,2,0,2,1,3,2,2,0,2,3,1,1,3,0,2,2,1,0,1,0,2,1,3,1,3,0,0,2,1,1,3,1,1,3,3,0,3,3,1,2,0,2,1,1,2,0,2,1,3,2,2,2,1,1,0,2,1,1,0,2,2,1,0,0,1,1,3,3,1,3,0,2,2,3,0,0,1,2,0,1,1,0,1,0,3,1,3,0,1,0,0,1,2,3,3,0,3,1,2,3,1,0,1,0,2,1,1,1,0,3,2,1,0,3,3,3,2,3,0,0,3,0,0,3,1,3,3,1,3,3,1,0,3,0,2,3,0,0,3,0,1,1,1,0,3,1,0,3,0,0,3,1,2,2,0,2,2,1,3,3,3,2,2,1,0,0,1,3,2,0,1,3,0,2,3,3,1,1,1,1,3,0,0,3,0,0,3,1,2,2,3,2,1,1,1,1,1,2,0,3,0,3,2,2,1,2,3,3,3,1,1,1,1,2,1,0,3,0,0,0,1,0,0,1,0,3,0,0,2,1,3,3,1,3,2,0,1,3,1,3,3,0,1,1,3,1,1,1,3,1,3,1,3,1,1,3,0,1,3,1,1,3,2,1,3,1,2,1,0,3,1,3,2,1,3,0,3,0,2,3,2,2,0,2,2,1,1,2,2,0,2,1,0,2,2,0,0,1,0,2,2,3,3,2,0,0,1,0,2,3,1,3,0,1,1,1,3,1,1,1,3,3,0,2,1,0,2,2,2,0,0,1,3,0,1,3,1,1,1,0,1,1,1,3,2,2,0,2,1,1,3,1,1,2,3,0,2,0,1,1,3,0,0,1,1,0,3,1,3,3,1,3,1,1,3,3,0,1,0,1,1,3,0,2,1,0,2,2,3,2,3,1,3,1,1,3,1,3,0,3,1,3,3,0,2,2,2,2,1,1,0,3,1,0,0,3,3,3,1,0,3,1,0,1,0,0,1,0,0,3,3,0,3,1,0,0,3,0,3,0,0,0,0,1,1,1,1,1,3,2,1,1,0,3,0,0,1,1,1,0,0,3,0,1,1,0,0,0,1,2,1,1,1,1,3,1,3,3,1,2,3,1,3,2,0,3,1,1,2,3,1,1,3,0,0,3,1,0,1,0,2,1,0,2,3,1,1,3,0,1,3,3,1,3,1,1,3,0,3,1,3,1,3,1,1,1,0,2,3,1,1,3,0,2,1,3,2,1,3,2,2,1,0,3,1,0,1,3,0,3,0,1,3,0,1,3,0,0,1,0,2,0,1,1,2,1,0,0,1,1,3,1,0,0,1,0,1,1,0,1,1,3,3,1,3,3,1,2,0,1,1,1,1,2,1,1,2,2,0,2,2,0,2,2,0,2,0,1,1,1,1,0,3,3,1,3,0,3,0,1,1,0,0,1,0,1,1,3,0,3,3,1,3,2,0,3,3,3,3,3,1,2,2,3,1,0,1,1,1,3,2,0,0,2,3,3,3,0,3,0,3,3,1,3,3,0,3,1,1,3,0,1,1,0,2,2,1,3,3,1,2,2,0,0,3,0,0,3,1,3,1,1,1,0,3,0,3,3,2,3,0,0,1,3,3,0,1,3,0,1,3,1,1,2,2,0,0,0,0,0,0,0,2,0,0,1,1,0,3,3,3,2,2,0,3,0,1,0,3,0,2,2,3,0,3,2,2,3,1,3,2,0,2,1,3,0,3,2,0,3,0,3,1,0,0,3,3,2,2,1,3,3,1,1,3,0,2,2,2,3,3,3,0,3,1,2,3,2,3,2,0,2,1,0,1,0,1,1,0,3,0,3,0,3,3,3,0,1,0,2,3,0,2,2,0,0,3,0,2,0,1,2,3,0,2,0,1,0,1,0,1,2,0,2,1,0,3,0,3,3,3,1,0,1,1,3,1,1,2,1,3,0,1,1,0,3,0,0,3,1,0,3,1,2,1,3,0,3,1,1,1,1,0,1,1,2,2,1,2,3,1,0,0,0,2,3,0,3,3,3,0,2,1,3,2,0,1,3,1,2,1,1,0,1,0,1,3,1,1,0,1,2,2,0,0,2,1,0,0,3,0,3,2,0,0,0,3,2,0,3,1,3,2,1,3,2,1,0,2,3,2,1,3,1,3,2,0,2,1,1,1,3,0,2,2,0,3,3,1,0,3,1,3,3,3,1,3,3,3,3,1,0,1,1,2,3,0,2,2,3,2,2,1,1,3,2,0,1,3,2,2,1,0,3,3,2,3,0,3,3,0,2,1,0,0,0,1,3,1,0,3,1,0,1,3,0,2,0,1,0,3,1,2,3,0,1,3,0,1,0,1,2,0,1,0,1,2,3,0,1,3,0,1,2,3,3,2,3,0,2,1,3,1,0,1,3,3,1,1,0,1,3,0,3,2,3,1,1,3,0,3,1,0,0,3,0,2,2,0,2,1,3,2,3,0,3,3,3,2,1,1,0,3,1,0,3,0,2,2,0,2,2,1,3,3,1,0,3,3,1,0,1,3,2,0,3,3,3,1,1,1,1,3,0,3,3,1,3,1,0,2,2,1,3,0,1,0,1,1,1,3,0,2,0,1,1,0,0,0,1,1,3,0,1,2,1,1,0,0,0,0,3,1,1,0,3,3,3,1,0,1,3,0,3,1,0,3,0,3,3,1,0,3,1,2,2,1,2,3,0,0,0,3,1,3,0,0,1,3,3,3,1,3,3,1,1,1,0,1,0,0,1,0,1,3,3,3,1,3,1,2,2,1,1,3,0,3,1,1,2,2,0,0,3,2,1,1,1,1,2,0,1,2,3,3,0,1,3,1,2,2,0,1,3,0,1,1,1,1,2,0,3,2,1,0,3,0,1,0,1,1,0,1,0,3,2,0,0,0,1,0,3,1,1,3,0,3,1,0,3,1,3,2,3,0,2,2,1,3,1,0,3,3,1,0,3,3,3,1,3,1,3,0,0,1,0,2,1,0,2,3,0,0,3,0,3,3,0,0,3,0,0,3,3,3,3,1,0,3,2,0,3,3,3,2,0,2,0,0,2,1,1,3,3,1,2,1,3,3,1,2,0,0,2,1,2,0,0,0,0,2,3,1,1,3,0,0,3,0,2,3,0,2,0,0,2,0,0,1,1,1,3,1,1,0,3,0,0,0,1,1,3,2,2,0,2,3,2,0,1,3,0,3,0,3,2,2,0,3,2,1,1,1,1,1,1,0,1,1,1,3,0,1,1,0,1,0,1,0,3,3,1,2,0,0,2,0,0,1,1,1,2,3,0,3,0,1,0,3,0,0,0,0,3,1,3,0,2,0,1,0,0,0,0,0,0,2,2,0,0,2,2,0,0,3,1,2,0,0,1,1,1,1,1,1,0,0,0,2,1,3,2,2,3,3,3,1,0,0,2,1,1,0,0,1,1,1,1,0,3,2,2,1,1,3,1,1,0,3,2,0,1,3,3,3,3,3,1,0,0,0,0,0,2,2,3,0,3,3,0,2,0,0,0,0,0,1,1,0,3,3,3,1,0,3,0,0,1,3,3,3,2,3,1,0,0,0,2,3,3,0,0,0,3,3,0,3,0,2,2,1,3,0,0,0,3,1,1,3,0,3,0,3,0,3,1,3,3,0,0,3,2,2,1,0,1,0,3,2,1,0,2,1,2,1,0,0,2,3,0,2,2,3,1,3,0,1,0,0,2,0,1,2,1,3,0,1,3,3,1,1,1,1,3,0,3,1,0,3,0,2,0,0,2,0,2,1,3,3,0,3,1,0,1,1,3,3,3,1,0,3,2,0,3,1,0,1,2,1,1,1,3,1,0,3,0,0,3,1,0,3,3,3,3,1,1,3,3,0,3,1,3,2,1,3,3,1,1,3,0,2,3,1,1,3,2,3,0,3,2,1,1,1,3,3,3,3,1,1,3,0,0,1,0,1,3,1,0,1,0,0,1,0,0,0,0,1,3,0,0,1,3,0,0,3,0,1,3,0,0,1,0,3,1,3,1,0,2,0,1,2,1,3,1,0,2,2,0,0,0,3,0,2,0,0,0,1,1,2,3,1,3,2,0,0,1,3,0,3,1,1,3,2,1,1,1,2,1,1,0,3,1,0,3,1,1,3,0,2,3,1,1,3,1,0,3,1,2,1,1,1,3,1,1,1,0,3,1,1,1,3,0,1,2,1,0,3,1,1,3,3,3,0,1,0,3,0,0,1,0,2,0,1,2,0,2,2,3,1,0,0,1,2,0,3,1,1,1,3,1,1,1,3,3,0,1,1,0,3,1,0,0,0,3,1,0,0,3,3,2,2,1,1,0,1,1,0,0,3,2,2,3,0,1,3,2,0,0,1,1,3,0,1,2,0,2,3,0,1,0,1,1,2,0,1,3,0,1,2,2,1,2,2,0,1,3,0,0,3,1,3,3,1,0,0,1,3,1,1,3,0,1,0,3,0,1,3,3,1,1,1,1,1,0,3,3,0,3,3,1,1,3,0,2,0,0,1,1,0,2,2,1,2,0,1,1,3,2,1,2,0,1,3,1,1,3,3,2,0,1,2,3,3,2,0,1,0,0,3,1,2,0,2,3,0,2,3,0,1,3,1,1,1,2,0,3,3,2,0,0,2,1,1,1,1,1,0,3,3,1,2,3,0,3,0,0,3,0,0,3,3,0,1,0,3,1,0,1,0,0,2,0,1,2,3,1,3,3,2,1,0,1,3,1,0,3,2,0,2,1,3,2,3,1,1,1,1,0,1,0,3,3,0,2,2,1,3,3,0,0,0,0,0,1,0,2,0,3,2,1,0,0,3,3,1,1,1,2,2,0,1,2,3,1,3,0,0,0,1,1,0,0,0,1,1,0,1,3,3,3,1,0,1,1,2,1,3,0,1,0,1,2,0,1,1,2,2,2,2,2,3,0,3,0,1,3,0,1,2,2,3,1,0,0,3,2,1,3,1,3,2,0,0,0,3,1,3,2,3,2,2,0,2,1,0,0,0,1,1,0,1,0,2,3,3,3,1,0,3,2,1,1,1,0,3,1,2,3,1,1,3,0,2,0,0,3,3,0,0,3,3,1,1,1,1,3,0,0,0,0,0,3,1,3,3,3,2,0,0,0,3,0,2,2,2,1,1,1,2,3,0,3,3,3,0,1,1,1,3,0,3,0,2,1,0,1,1,1,1,1,3,1,3,0,1,1,1,1,1,3,1,3,0,2,0,2,1,1,1,0,1,3,2,3,0,0,0,2,1,3,0,0,1,3,3,0,2,1,0,3,3,0,0,1,1,3,3,3,3,0,0,2,3,3,0,0,0,2,0,3,3,0,0,2,0,2,0,0,1,1,0,0,1,0,1,1,3,1,3,3,3,0,1,0,2,3,2,0,0,0,3,2,1,1,1,1,0,0,1,3,0,0,0,3,0,1,3,0,1,1,2,3,0,3,2,2,1,1,1,0,1,1,0,3,0,0,3,3,0,1,1,1,1,1,0,3,0,1,3,1,1,3,3,0,1,0,1,3,0,3,3,1,1,3,1,0,3,1,0,1,1,1,0,0,1,3,0,0,0,0,0,3,0,3,3,0,0,0,1,0,1,0,0,0,1,3,0,1,1,0,1,1,3,0,1,1,3,1,1,1,3,1,0,1,1,0,0,0,2,1,1,1,0,3,0,0,0,0,0,3,0,0,0,0,0,0,3,3,0,3,0,0,1,0,0,0,1,1,1,3,2,0,2,0,0,1,1,0,0,0,0,3,2,0,0,1,2,0,0,0,0,3,1,3,2,3,3,1,2,1,3,3,1,0,3,3,1,0,3,3,2,1,1,1,1,1,0,1,0,0,3,1,1,3,0,2,2,1,1,3,0,1,1,1,2,1,1,2,1,0,2,3,0,1,3,2,0,3,1,0,3,3,1,3,0,3,3,3,1,1,1,1,1,3,1,3,0,3,3,2,0,3,1,1,1,1,0,1,1,3,1,1,0,0,0,3,0,3,1,3,1,0,3,1,0,0,1,0,0,1,1,2,0,1,3,0,0,3,1,0,1,1,0,1,1,1,0,0,1,0,0,3,2,0,1,3,0,0,3,1,0,0,0,1,3,0,0,1,1,3,1,0,0,0,0,1,0,0,0,3,2,0,3,0,2,1,1,0,3,0,1,0,1,0,0,1,0,1,3,0,0,0,2,2,0,1,2,0,0,1,1,3,2,0,3,1,3,1,3,3,0,3,0,1,3,0,2,3,0,3,1,1,3,3,0,0,3,1,0,3,3,3,3,3,0,3,3,2,1,1,0,1,0,0,1,3,0,0,1,1,3,1,1,3,1,2,2,0,1,3,1,1,3,2,1,1,3,1,0,1,3,1,0,3,3,3,0,1,0,1,1,0,0,1,1,0,1,1,1,0,0,1,3,0,3,1,3,0,3,0,0,0,1,1,3,0,2,1,1,0,3,2,2,1,1,0,3,1,1,1,1,3,3,0,3,2,0,2,1,2,2,2,1,2,1,0,2,3,2,0,3,3,0,3,0,2,2,1,3,3,3,1,2,1,3,1,3,0,0,2,0,3,3,0,0,0,0,0,3,2,1,1,1,3,0,2,1,1,1,0,1,3,3,1,3,3,0,1,1,0,1,0,0,2,2,1,0,1,0,1,1,3,0,1,0,1,1,1,1,3,3,0,3,1,1,1,1,0,3,0,1,3,0,2,3,3,0,3,3,0,3,1,2,0,0,0,1,1,0,3,1,0,2,1,1,3,0,1,3,1,0,3,3,1,0,0,1,1,0,0,3,0,2,1,1,1,3,2,2,1,1,2,3,0,1,2,1,1,3,0,0,1,1,2,1,3,0,0,1,0,3,3,0,1,3,2,1,0,2,2,1,1,0,1,1,3,0,1,3,1,0,3,2,1,0,1,1,3,0,0,3,3,2,2,0,0,2,1,2,1,1,0,1,1,1,3,0,2,1,0,0,3,0,3,1,0,0,1,1,0,3,3,0,0,1,1,3,3,1,1,1,3,1,3,0,1,0,1,3,3,0,3,1,0,3,1,3,3,1,0,1,0,0,3,3,1,3,0,0,3,3,1,3,0,1,3,2,0,1,3,0,3,1,1,3,0,2,0,0,0,3,1,2,1,3,2,3,1,2,1,1,3,3,0,0,3,1,1,0,0,2,1,1,3,0,1,2,3,3,3,3,1,0,1,0,1,3,3,1,3,0,2,3,0,0,2,1,1,3,1,3,0,1,1,3,2,1,0,1,2,0,1,0,0,1,0,1,0,3,0,0,3,2,0,1,1,1,0,1,1,0,0,3,1,0,1,0,3,2,1,1,3,0,3,1,0,3,0,3,0,2,3,0,0,0,0,1,1,1,0,2,1,1,1,0,3,2,0,1,1,1,1,3,0,0,1,0,2,2,2,2,1,1,1,3,1,3,1,0,2,1,1,1,3,1,1,3,0,0,3,2,0,1,1,3,1,1,2,2,1,1,3,0,2,1,1,0,3,2,3,2,0,3,3,3,1,0,1,3,3,1,1,0,1,3,1,1,0,3,0,0,1,2,1,3,1,1,3,1,0,3,0,1,3,0,2,2,1,1,3,0,1,3,0,0,1,1,0,0,1,0,1,0,1,3,0,0,1,1,0,3,0,3,0,1,1,0,0,3,2,2,3,2,2,1,2,1,2,0,3,2,3,0,0,1,0,1,2,0,2,0,0,0,2,1,0,1,0,3,0,1,1,0,0,2,2,1,1,0,1,1,0,1,0,1,0,1,1,0,1,1,3,2,3,1,1,0,0,0,0,0,2,2,1,1,3,3,1,2,0,3,0,1,2,2,2,0,3,0,0,3,1,1,3,0,3,3,3,0,3,3,0,1,1,3,1,0,2,0,0,2,3,3,3,3,3,3,3,1,3,3,1,2,1,0,2,2,0,3,3,3,3,3,1,3,2,0,2,1,1,3,3,3,3,0,1,1,0,1,3,1,1,0,2,1,1,3,0,2,1,1,1,1,3,0,1,1,1,1,1,1,0,0,1,3,0,2,2,0,2,2,2,1,0,1,3,2,2,1,1,1,1,1,0,0,1,0,2,2,1,0,3,1,0,1,1,1,1,2,1,3,0,0,0,3,1,1,1,1,3,0,2,0,0,2,3,1,1,1,0,1,3,1,1,3,0,0,0,1,0,1,0,3,1,1,2,3,0,3,3,0,1,3,1,2,1,0,3,1,0,2,2,0,2,3,0,3,1,0,0,3,1,0,1,1,3,2,0,2,1,3,1,0,1,1,0,3,0,2,3,1,3,0,0,3,0,2,0,0,0,0,1,0,0,1,1,2,0,0,0,1,1,0,0,0,3,0,0,3,3,1,0,0,2,1,0,1,3,2,1,3,3,0,3,3,0,1,0,0,3,3,3,3,0,1,3,2,2,2,3,1,3,1,3,0,3,3,3,3,0,1,1,1,3,1,1,3,0,1,0,0,2,1,1,3,1,0,2,0,2,3,0,1,3,3,1,2,0,2,3,1,3,1,1,1,3,3,1,0,1,1,0,3,3,3,1,1,2,0,1,2,2,1,0,3,1,3,0,1,2,2,1,3,1,0,0,1,0,3,3,3,3,3,3,2,3,0,2,1,1,0,1,0,2,2,1,3,3,1,1,0,1,2,2,0,1,3,3,1,0,1,2,3,1,0,3,3,0,3,3,2,2,1,3,1,0,0,1,3,3,3,1,1,3,1,0,1,3,0,3,1,3,2,1,3,3,1,0,3,1,1,2,1,1,0,0,1,3,0,0,3,0,3,3,3,1,0,1,3,3,3,0,1,0,3,1,1,0,0,0,1,0,3,1,0,1,3,3,3,2,2,1,3,3,1,2,0,0,2,1,1,2,1,1,2,1,1,3,2,0,3,0,1,3,2,2,1,0,3,3,3,3,2,3,0,2,0,3,2,3,2,2,3,3,3,2,0,1,3,0,3,3,3,1,3,2,3,0,3,2,3,1,3,1,1,0,3,1,3,0,3,3,2,0,3,2,0,2,2,2,3,1,3,3,0,1,3,1,3,3,3,3,0,2,3,0,3,0,0,0,3,0,2,3,0,1,1,2,3,3,0,0,1,3,3,1,1,0,0,3,3,0,0,1,3,0,2,3,3,3,3,2,0,1,0,0,1,0,3,3,1,0,0,0,0,0,0,2,0,2,3,0,0,3,0,0,0,1,3,3,1,2,1,1,3,3,0,0,3,3,3,3,0,0,3,0,0,3,1,0,0,1,0,1,1,1,3,1,1,3,0,2,1,1,3,3,0,1,3,0,1,3,0,0,3,0,0,3,3,0,3,3,0,1,0,3,3,3,3,2,0,1,3,0,1,1,0,1,0,0,1,3,1,0,0,1,2,2,1,3,0,1,0,3,0,2,0,0,0,0,0,3,1,1,0,1,1,1,1,3,3,0,1,2,0,2,3,2,1,2,2,1,3,3,1,2,0,1,1,1,3,0,3,0,3,1,1,1,1,1,2,1,1,1,2,1,2,3,-1};
   private static Sequences sequenceM;
   private static BWTIndex indexM;
   
   @BeforeClass
   public static void setUpBeforeClass() throws Exception {
      sequenceM = Sequences.createEmptySequencesInMemory();
      sequenceM.addSequence(ByteBuffer.wrap(chrM));
      
      final SuffixTrayBuilder stb = new SuffixTrayBuilder(sequenceM, Alphabet.DNA());
      stb.build("bothLR"); //WARNING: change the method and you must change the type cast in the next line!
      assert (stb.getSuffixDLL() instanceof SuffixXorDLL);
      final SuffixXorDLL suffixDLL = (SuffixXorDLL)stb.getSuffixDLL(); // type cast is okay because I used method 'bothLR' to build the list
      indexM = BWTIndexBuilder.build(suffixDLL);
   }

   @Test
   public void testFind01() {
      final byte[] query = {3,2,2,3,0,3,3,3,3,1,2,3,1,3,2,2,2,2,2,2,3,2,3,2,1,0,1,2,1,2,0,3,0,2,1,0,3,3,2,1,2,0,2,0,1,2,1,3,2,2,0,2,1,1,2,2,0,2,1,0,1,1,1,3,0,3,2,3,1,2,1,0,2,3,0,3,1,3,2,3,1,3,3,3,2,0,3,3,1};
      BWTSearch.BWTSearchResult result = BWTSearch.find(query, indexM);
      final int number = 1;
      assertEquals(number, result.number);
      
   }

   @Test
   public void testFind02() {
      final byte[] query = {1,1,1,2,2};
      BWTSearch.BWTSearchResult result = BWTSearch.find(query, indexM);
      final int number = 3;
      assertEquals(number, result.number);
      
   }

}