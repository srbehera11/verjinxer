package verjinxer.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import verjinxer.FileNameExtensions;
import verjinxer.Globals;
import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.sequenceanalysis.Sequences;

// TODO maybe get...FileName methods should be removed?

/**
 * Metadata about a project. While primary data such as a suffix array, the alphabet, or the q-gram
 * index are stored in separate files, secondary data such as the number of sequences, the value of
 * q, whether an index is for bisulfite sequences, and so on is stored in a project file. This class
 * provides a way to read and write that information.
 * 
 * @author Marcel Martin
 * 
 */
public class ProjectInfo {
   Properties properties;
   final String projectName;
   final String projectFileName;

   /**
    * Creates a new project in memory only. Call load() and store() to synchronize with files.
    * 
    * @param projectName
    *           Name of the project. The file name for the project is constructed from this string.
    */
   public ProjectInfo(String projectName) {
      this.projectName = projectName;
      this.projectFileName = Globals.dir + projectName + FileNameExtensions.prj;

      Properties defaults = new Properties();
      defaults.setProperty("BisulfiteIndex", "false");
      defaults.setProperty("RunIndex", "false");
      this.properties = new Properties(defaults);
   }

   /**
    * Loads this project from its associated file on disk. The actual file name is constructed by
    * appending the appropriate file name extension to the project name.
    */
   public void load() throws FileNotFoundException, IOException {
      BufferedReader projectFile = new BufferedReader(new FileReader(projectFileName));
      properties.load(projectFile);
      projectFile.close();
   }

   /**
    * Creates a new ProjectInfo instance from a file. It constructs a new object and then calls
    * load() on it.
    * 
    * @param projectname
    *           Name of the project.
    * @return a new ProjectInfo instance
    */
   public static ProjectInfo createFromFile(String projectname) throws FileNotFoundException,
         IOException {
      ProjectInfo project = new ProjectInfo(projectname);
      project.load();
      return project;
   }

   /**
    * Writes a project file to disk. As for load(), the actual file name is constructed by appending
    * the appropriate extension to the project name.
    */
   public void store() throws IOException {
      PrintWriter projectFile = new PrintWriter(new BufferedWriter(new FileWriter(projectFileName)));
      properties.store(projectFile, null);
      projectFile.close();
   }

   public String getFileName() {
      return projectFileName;
   }
   
   public String makeFileName(String extension) {
      //TODO stub
      //TODO extension shoud be enum FileType
      return "";
   }
   
   public Sequences readSequence(){
      return null; //TODO
   }
   
   public Alphabet readAlphabet(){
      return null; //TODO
   }
   

   public String getName() {
      return projectName;
   }

   public int getMaximumBucketSize() {
      return Integer.parseInt(properties.getProperty("qbckMax"));
   }

   public String getQPositionsFileName() {
      return Globals.dir + projectName + FileNameExtensions.qpositions;
   }

   public String getQBucketsFileName() {
      return Globals.dir + projectName + FileNameExtensions.qbuckets;
   }

   public boolean isBisulfiteIndex() {
      return getBooleanProperty("Bisulfite");
   }

   public void setBisulfiteIndex(boolean value) {
      setProperty("Bisulfite", value);
   }

   public void setRunIndex(boolean value) {
      setProperty("RunIndex", true);
   }

   public boolean isRunIndex() {
      return getBooleanProperty("RunIndex");
   }

   public int getStride() {
      return getIntProperty("Stride");
   }

   public void setStride(int stride) {
      setProperty("Stride", stride);
   }

   // TODO throws NumberFormatException?
   public int getIntProperty(final String name) {
      return Integer.parseInt(properties.getProperty(name));
   }

   // TODO throws NumberFormatException?
   public long getLongProperty(final String name) {
      return Long.parseLong(properties.getProperty(name));
   }

   // TODO throws NumberFormatException?
   public double getDoubleProperty(final String name) {
      return Double.parseDouble(properties.getProperty(name));
   }

   // TODO throws NumberFormatException?
   public boolean getBooleanProperty(final String name) {
      return Boolean.parseBoolean(properties.getProperty(name));
   }

   public void setProperty(final String name, String value) {
      properties.setProperty(name, value);
   }

   public void setProperty(final String name, long value) {
      properties.setProperty(name, Long.toString(value));
   }

   public void setProperty(final String name, int value) {
      properties.setProperty(name, Integer.toString(value));
   }

   public void setProperty(final String name, double value) {
      properties.setProperty(name, Double.toString(value));
   }

   public void setProperty(final String name, boolean value) {
      properties.setProperty(name, Boolean.toString(value));
   }
   /*
      public String getSequenceFileName() {
         return projectname + FileNameExtensions.seq;
      }
      
      public String getRunSequenceFileName() {
         return projectname + FileNameExtensions.runseq;
      }
      
      public String getRunLengthFileName() {
         return projectname + FileNameExtensions.runlen;
      }
   */
}
