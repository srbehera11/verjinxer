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

// TODO maybe get...FileName methods should be removed?

/**
 * Metadata about a project. While primary data such as a suffix array, the alphabet, or the q-gram
 * index are stored in separate files, secondary data such as the number of sequences, the value of q,
 * whether an index is for bisulfite sequences, and so on is stored in a project file. This class 
 * provides a way to read and write that information.   
 */
public class ProjectInfo {
   Properties properties;
   final String projectname;
   final String projectfilename;

   public ProjectInfo(String projectname) {
      this.projectname = projectname;
      this.properties = new Properties();
      this.projectfilename = projectname + FileNameExtensions.prj;
   }

   /**
    * Loads this project from its associated file on disk. The actual file name is constructed by
    * appending the appropriate file name extension to the project name.
    */
   public void load() throws FileNotFoundException, IOException {
      BufferedReader projectfile = new BufferedReader(new FileReader(projectfilename));
      properties.load(projectfile);
      projectfile.close();
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
      PrintWriter projectfile = new PrintWriter(new BufferedWriter(new FileWriter(projectfilename)));
      properties.store(projectfile, null);
      projectfile.close();
   }

   public String getFileName() {
      return projectfilename;
   }

   public String getName() {
      return projectname;
   }
   
   public int getMaximumBucketSize() {
      return Integer.parseInt(properties.getProperty("qbckMax"));
   }

   public String getQPositionsFileName() {
      return projectname + FileNameExtensions.qpositions;
   }

   public String getQBucketsFileName() {
      return projectname + FileNameExtensions.qbuckets;
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
}
