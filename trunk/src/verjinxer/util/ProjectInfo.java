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

public class ProjectInfo {
   Properties properties;
   final String projectname;
   final String projectfilename;

   public ProjectInfo(String projectname) {
      this.projectname = projectname;
      this.properties = new Properties();
      this.projectfilename = projectname + FileNameExtensions.prj;
   }

//   public void setProjectName(String projectname) {
//      this.projectname = projectname;
//   }

   /**
    */
   public void load() throws FileNotFoundException, IOException {
      BufferedReader projectfile = new BufferedReader(new FileReader(projectfilename));
      properties.load(projectfile);
      projectfile.close();
//         warnmsg("%s: could not read project file [%s]%n", cmdname, ex.toString());
//         terminate(1);
   }

   public static ProjectInfo createFromFile(String projectname) throws FileNotFoundException, IOException {
      ProjectInfo project = new ProjectInfo(projectname);
      project.load();
      return project;
   }
   
   /* write prj file */
   public void store() throws IOException {
      PrintWriter projectfile = new PrintWriter(new BufferedWriter(new FileWriter(projectfilename)));
      properties.store(projectfile, null);
      projectfile.close();
//         warnmsg("%s: %s%n", cmdname, ex.toString());
//         throw new IOException();
   }

   public String getFilename() {
      return projectfilename;
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
