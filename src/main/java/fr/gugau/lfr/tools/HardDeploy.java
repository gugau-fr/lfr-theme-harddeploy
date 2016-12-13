package fr.gugau.lfr.tools;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo( name = "hard-deploy")
public class HardDeploy extends AbstractMojo {
    
    private static final String WEBAPPS_PROP = "liferay.app.server.deploy.dir";
    private static final String SASS_DIR = "css";
    
    @Parameter(defaultValue="${project}", readonly=true, required=true)
    private MavenProject project;
    
    @Parameter
    private String serverDeployDir = null;
    
    @Parameter
    private String sassCommand = "sass";
    
    @Parameter
    private String[] sourceFilesCompiled = new String[]{"custom.css"};
    
    @Parameter
    private String[] staticContentCopied = new String[]{"js", "images", "templates"};
    
    @Parameter
    private String sassStyle = "expanded";
    
    @Parameter
    private String sassSourceMap = "none";
    
    @Override
    public void execute () throws MojoExecutionException {
        //Test SASS compiler is installed
        try {
            getLog().info( "Test if compiler is installed ..." );
            runAndLog(null, sassCommand, "--version");
            getLog().info( "Success !" );
        } catch (IOException | InterruptedException ex) {
            throw new MojoExecutionException("Can't run sass compiler, is it installed ?", ex);
        }
        
        //Build out dir File (themeDeployDir)
        File themeDeployDir = null;
        if (serverDeployDir == null) {
            String webappsDirProp = project.getProperties().getProperty(WEBAPPS_PROP);
            if (webappsDirProp == null) {
                throw new MojoExecutionException("Property '"+WEBAPPS_PROP+"' is not defined");
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
        
        //Build content directory (where intersting sources are)
        File themeContentDir = new File(project.getBasedir().getAbsolutePath()
                +File.separator+"src"+File.separator+"main"+File.separator+"webapp");
        if (!themeContentDir.isDirectory()) {
            throw new MojoExecutionException("No content, no directory at '"+themeContentDir.getAbsolutePath()+"' exists");
        }
        
        //Copy static files
        for (String srcFileName : staticContentCopied) {
            File srcFile = new File(themeContentDir.getAbsolutePath()+File.separator+srcFileName);
            if (srcFile.exists()) {
                getLog().info("Copying "+srcFile.getName()+"...");
                try {
                    File destFile = new File(themeDeployDir.getAbsolutePath()+File.separator+srcFile.getName());
                    if (srcFile.isDirectory()) {
                        FileUtils.copyDirectory(srcFile, destFile);
                    } else {
                        FileUtils.copyFile(srcFile, destFile);
                    }
                } catch (IOException ex) {
                    throw new MojoExecutionException("Error copying static resource '"+srcFile.getName()+"'", ex);
                }
            } else {
                getLog().info("Skipping "+srcFile.getName()+" copy.");
            }
        }
        
        //Compile one by one SASS files
        for (String cssFile : sourceFilesCompiled) {
            try {
                getLog().info( "Compile and copy '"+cssFile+"' SASS file..." );
                runAndLog(themeContentDir,
                        sassCommand, "--scss", "--compass", "--line-numbers", "--style", sassStyle, "--sourcemap="+sassSourceMap,
                        SASS_DIR+File.separator+cssFile,
                        themeDeployDir.getAbsolutePath()+File.separator+SASS_DIR+File.separator+cssFile);
            } catch (IOException | InterruptedException ex) {
                throw new MojoExecutionException("Error while compiling SASS file '"+cssFile+"'", ex);
            }
        }
    }
    
    private void runAndLog (File directory, String... command) throws InterruptedException, IOException, MojoExecutionException {
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
    
}
