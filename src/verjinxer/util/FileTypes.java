package verjinxer.util;

/**
 * @author Markus Kemmerling
 */
public enum FileTypes {
   SEQ("seq"),
   PRJ("prj"),
   LOG("log"),
   DESC("desc"),
   SELECT("select-filter"),
   SSP("ssp"),
   ALPHABET("alphabet"),
   QBUCKETS("qbck"),
   QPOSITIONS("qpos"),
   QFREQ("qfreq"),
   QSEQFREQ("qsfrq"),
   RUNSEQ("runseq"),
   RUNLEN("runlen"),
   RUN2POS("run2pos"),
   POS2RUN("pos2run"),
   POS("pos"),         // suffix array
   LCP("lcp"),         // lcp values
   QUALITIY("quality"),
   MAPPED("mapped"),
   NONMAPPABLE("nonmappable"),
   
   CUT("cut"), // Cutter
   QCOMPLEXITY("qcomplexity"), // MapperSubcommand
   ALLMAPPED("allmapped"), // MapperSubcommand
   REPEAT_FILTER("repeat-filter"), // MapperSubcommand
   TOOMANYHITS_FILTER("toomanyhits-filter"), // QGramMatcherSubcommand
   NUPROBES("nuprobes"), // NonUniqueProbeDesigner
   NUSTATS("nustats"), // NonUniqueProbeDesigner
   
   MAPBESTERROR("mapbesterror"), // MapperByAlignment
   MAPBESTHITS("mapbesthits"), // MapperByAlignment
   MAPALLHITS("mapallhits"), // MapperByAlignment
   
   SORTED_MATCHES("sorted-matches"), // QGramMatcherSubcommand
   MATCHES("matches"), // QGramMatcherSubcommand
   
   FASTA("fasta"),
   CSFASTA("csfasta"),
   TEXT("txt"), 
   
   CSFASTQ("csfastq"), // Sequence2Fastq
   FASTQ("fastq"),  // Sequence2Fastq
   DECSFASTQ("decsfastq"), // Sequence2Fastq
   
   BWT("bwt"),
   SAMPLEDPOS("spos"); // sampled suffix array
   
   private final String extension;

   private FileTypes(String extension) {
      this.extension = "." + extension;
   }

   public String toString() {
      return extension;
   }
   
}
