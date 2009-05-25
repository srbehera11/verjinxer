package verjinxer.util;

/**
 * @author Markus Kemmerling
 */
public enum FileTypes { //TODO replace FileNameExtension with this
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
   
   CUT("cut"), // Cutter
   ALLMAPPED("allmapped"), // MapperSubcommand
   REPEAT_FILTER("repeat-filter"), // MapperSubcommand
   TOOMANYHITS_FILTER("toomanyhits-filter"), // QGramMatcherSubcommand
   NUPROBES("nuprobes"), // NonUniqueProbeDesigner
   NUSTATS("nustats"), // NonUniqueProbeDesigner
   
   FASTA("fasta"),
   CSFASTA("csfasta"),
   TEXT("txt");
   
   private final String extension;

   private FileTypes(String extension) {
      this.extension = "." + extension;
   }

   public String toString() {
      return extension;
   }
   
}
