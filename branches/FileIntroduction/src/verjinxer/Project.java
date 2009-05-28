package verjinxer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import verjinxer.sequenceanalysis.Alphabet;
import verjinxer.sequenceanalysis.Sequences;
import verjinxer.util.FileTypes;

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
public class Project {
   Properties properties;
   final String projectName;
   final File projectFile;
   private File workingDirectory;
   
   /**
    * Creates a new project in memory only. Call load() and store() to synchronize with files.
    * 
    * @param projectName
    *           Name of the project inclusive the abstract path. The file name and working directory
    *           for the project is constructed from it.
    */
   public Project(File projectName) {
      if (projectName.getParentFile() != null) {
         setWorkingDirectory(projectName.getParentFile());
      } else {
         setWorkingDirectory(new File(System.getProperty("user.dir")));
      }
      
      this.projectName = projectName.getName();
      this.projectFile = makeFile(FileTypes.PRJ);

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
      BufferedReader projectFileReader = new BufferedReader(new FileReader(projectFile));
      properties.load(projectFileReader);
      projectFileReader.close();
   }
   
   /**
    * Creates a new ProjectInfo instance from a file. It constructs a new object and then calls
    * load() on it.
    * 
    * @param projectFile
    *           File composed of working directory and project name (no file extension).
    * @return a new ProjectInfo instance
    */
   public static Project createFromFile(File projectFile) throws FileNotFoundException,
         IOException {
      Project project = new Project(projectFile);
      project.load();
      return project;
   }

   /**
    * Writes a project file to disk. As for load(), the actual file name is constructed by appending
    * the appropriate extension to the project name.
    */
   public void store() throws IOException {
      PrintWriter projectFileWriter = new PrintWriter(new BufferedWriter(new FileWriter(projectFile)));
      properties.store(projectFileWriter, null);
      projectFileWriter.close();
   }
   
   /**
    * @return File where informations about the project are stored.
    */
   public File getFile() {
      return projectFile;
   }

   /**
    * @return The working directory of this project.
    */
   public File getWorkingDirectory() {
      return workingDirectory;
   }

   /**
    * Sets the working directory for this project. All Files will be created or searched for here.
    * The directory will be created if it does not exist.
    * 
    * @param workingDirectory
    */
   public void setWorkingDirectory(File workingDirectory) {
      this.workingDirectory = workingDirectory;
      if (!workingDirectory.exists()) {
         workingDirectory.mkdirs();
      }
      assert workingDirectory.exists();
      assert workingDirectory.isDirectory();
   }
   
   /**
    * Creates a File which name depends on the project name and the given file type. The files
    * parent is the working directory. It does not create a file on disc, so the returned file does
    * not necessarily exist.
    * 
    * @param fileType
    *           Type of the file to create.
    * @return The File.
    */
   public File makeFile(FileTypes fileType) {
      return new File(workingDirectory, projectName + fileType);
   }
   
   /**
    * Creates a File with the given name and the given type as extension which parent is the working
    * directory. It does not create a file on disc, so the returned file does not necessarily exist.
    * 
    * @param fileType
    *           Type of the file to create.
    * @param name
    *           Name of the file to create.
    * @return The File.
    */
   public File makeFile(FileTypes fileType, String name) {
      return new File(workingDirectory, name + fileType);
   }
   
   /**
    * Creates a File with the given name in the working directory. No Extensions are added to the
    * name. It does not create a file on disc, so the returned file does not necessarily exist.
    * 
    * @param name
    *           Name of the file to create.
    * @return The File.
    */
   public File makeFile(String name) {
      return new File(workingDirectory, name);
   }

   /**
    * Reads the sequences of this project from disc.
    * 
    * @return The sequences.
    */
   public Sequences readSequences() {
      try {
         return new Sequences(this);
      } catch (IOException e) {
         System.err.printf("%s: could not read sequence. Stop.%n", Globals.cmdname);
         System.exit(1);
         return null;
      }
   }

   /**
    * Reads the alphabet of this project from disc.
    * 
    * @return The alphabet.
    */
   public Alphabet readAlphabet() {
      return Globals.readAlphabet(makeFile(FileTypes.ALPHABET)); //TODO don't use Globals anywhere
   }

   public String getName() {
      return projectName;
   }

   public int getMaximumBucketSize() {
      return Integer.parseInt(properties.getProperty("qbckMax"));
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
    * public String getSequenceFileName() { return projectname + FileNameExtensions.seq; } public
    * String getRunSequenceFileName() { return projectname + FileNameExtensions.runseq; } public
    * String getRunLengthFileName() { return projectname + FileNameExtensions.runlen; }
    */
}
