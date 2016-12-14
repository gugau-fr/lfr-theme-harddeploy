package fr.gugau.lfr.tools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo( name = "live-deploy")
public class LiveDeploy extends AbstractMojo {
    
    private static final String WEBAPPS_PROP = "liferay.app.server.deploy.dir";
    private static final String SASS_DIR = "css";
    private static final String METHOD_COMPILE = "compile";
    private static final String METHOD_COPY = "copy";
    
    @Parameter(defaultValue="${project}", readonly=true, required=true)
    private MavenProject project;
    
    @Parameter
    private String serverDeployDir = null;
    
    @Parameter
    private String method = METHOD_COPY;
    
    @Parameter
    private String[] staticContentFiles = new String[]{"js", "images", "templates"};
    
    @Parameter
    private String[] sassSourceFiles = new String[]{"custom.css"};
    
    @Parameter
    private String sassCompilerCommand = "sass";
    @Parameter
    private String sassCompilerStyle = "expanded";
    @Parameter
    private String sassCompilerSourceMap = "none";
    
    @Override
    public void execute () throws MojoExecutionException {
        //Check parameters
        if (!METHOD_COMPILE.equals(method) && !METHOD_COPY.equals(method)) {
            throw new MojoExecutionException("Value '"+method+"' is invalid for parameter 'method'");
        }
        
        File themeDeployDir = getThemeDeployDir();
        
        //Content directory File object (where interesting sources are)
        File themeContentDir = new File(project.getBasedir().getAbsolutePath()
                +File.separator+"src"+File.separator+"main"+File.separator+"webapp");
        if (!themeContentDir.isDirectory()) {
            throw new MojoExecutionException("No content, no directory at '"+themeContentDir.getAbsolutePath()+"' exists");
        }
        
        //Copy static files
        for (String srcFileName : staticContentFiles) {
            copyResources(srcFileName, themeContentDir, themeDeployDir);
        }
        
        if (METHOD_COMPILE.equals(method)) {
            //Test SASS compiler is installed
            try {
                getLog().info( "Test if compiler is installed ..." );
                runSubprocess(null, sassCompilerCommand, "--version");
                getLog().info( "Success !" );
            } catch (IOException | InterruptedException ex) {
                throw new MojoExecutionException("Can't run sass compiler, is it installed ?", ex);
            }

            //Compile one by one SASS files
            for (String cssFile : sassSourceFiles) {
                try {
                    getLog().info( "Compile and copy '"+cssFile+"' SASS file..." );
                    runSubprocess(themeContentDir,
                            sassCompilerCommand, "--scss", "--compass", "--line-numbers", "--style", sassCompilerStyle, "--sourcemap="+sassCompilerSourceMap,
                            SASS_DIR+File.separator+cssFile,
                            themeDeployDir.getAbsolutePath()+File.separator+SASS_DIR+File.separator+cssFile);
                } catch (IOException | InterruptedException ex) {
                    throw new MojoExecutionException("Error while compiling SASS file '"+cssFile+"'", ex);
                }
            }
        } else {
            copyResources(SASS_DIR, themeContentDir, themeDeployDir);
            File deployedCssDir = new File(themeDeployDir.getAbsolutePath()+File.separator+SASS_DIR);
            for (String cssFile : sassSourceFiles) {
                //Evict main SASS files from cache in order to force Liferay to recompile it
                addDatedComment(new File(deployedCssDir.getAbsolutePath()+File.separator+cssFile));
            }
        }
    }
    
    /**
     * Add a dated comment at the end of the given file (using /* ... * /).
     * @param sassFile 
     */
    private void addDatedComment(File sassFile) throws MojoExecutionException{
        String sassCacheEvict = "\n/* live-deploy sass cache evict "+new Date().getTime()+" */";
        getLog().info("Evict file '"+sassFile.getName()+"' from SASS cache");
        try {
            FileWriter writer = new FileWriter(sassFile, true);
            writer.append(sassCacheEvict);
            writer.flush();
            writer.close();
        } catch (IOException ex) {
            throw new MojoExecutionException("Error while changing '"+sassFile.getName()+"' checksum");
        }
    }
    
    /**
     * Copy a file or directory
     * @param srcFileName the file/directory nam or subpath in themeContentDir
     * @param sourceDir
     * @param destinationDir
     * @return the created file or directory in themeDeployDir, null if no such file/directory in themeContentDir
     * @throws MojoExecutionException 
     */
    private File copyResources(String srcFileName, File sourceDir, File destinationDir) throws MojoExecutionException {
        File srcFile = new File(sourceDir.getAbsolutePath()+File.separator+srcFileName);
        if (srcFile.exists()) {
            getLog().info("Copying "+srcFile.getName()+"...");
            try {
                File destFile = new File(destinationDir.getAbsolutePath()+File.separator+srcFile.getName());
                if (srcFile.isDirectory()) {
                    FileUtils.copyDirectory(srcFile, destFile, false);
                } else {
                    FileUtils.copyFile(srcFile, destFile, false);
                }
                return destFile;
            } catch (IOException ex) {
                throw new MojoExecutionException("Error copying resource '"+srcFile.getName()+"'", ex);
            }
        } else {
            getLog().warn("No source file '"+srcFile.getName()+"' to copy");
            return null;
        }
    }
    
    /**
     * Run a command and redirect it's outputs to the current process's one.
     * @param directory
     * @param command
     * @throws InterruptedException
     * @throws IOException
     * @throws MojoExecutionException 
     */
    private void runSubprocess (File directory, String... command) throws InterruptedException, IOException, MojoExecutionException {
        ProcessBuilder pBuilder = new ProcessBuilder(command);
        
        if (directory != null) {
            pBuilder.directory(directory);
        }
        pBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
        pBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pBuilder.redirectInput(ProcessBuilder.Redirect.INHERIT);
        
        Process runningProcess = pBuilder.start();
        runningProcess.waitFor();
        if (runningProcess.exitValue() != 0) {
            throw new MojoExecutionException("Error with command '"+Arrays.toString(command)+"', status code "+runningProcess.exitValue());
        }
    }
    
    /**
     * Return the directory where the theme is deployed
     * @return
     * @throws MojoExecutionException 
     */
    private File getThemeDeployDir() throws MojoExecutionException {
        File themeDeployDir = null;
        if (serverDeployDir == null) {
            String webappsDirProp = project.getProperties().getProperty(WEBAPPS_PROP);
            if (webappsDirProp == null) {
                throw new MojoExecutionException("Property '"+WEBAPPS_PROP+"' is not defined, parameter serverDeployDir neither");
            }
            File webappsDir = new File(webappsDirProp);
            if (!webappsDir.isDirectory()) {
                throw new MojoExecutionException("Property '"+WEBAPPS_PROP+"' is not correct");
            }
            themeDeployDir = new File(webappsDir.getAbsolutePath()
                +File.separator
                +project.getArtifactId());
        } else {
            themeDeployDir = new File(serverDeployDir
                +File.separator
                +project.getArtifactId());
        }
        if (!themeDeployDir.isDirectory()) {
            throw new MojoExecutionException("Theme is not deployed at '"+themeDeployDir.getAbsolutePath()+"'");
        }
        return themeDeployDir;
    }
}
