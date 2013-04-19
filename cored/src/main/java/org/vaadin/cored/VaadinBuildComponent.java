package org.vaadin.cored;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.helper.ProjectHelper2;
import org.vaadin.cored.model.Project;
import org.vaadin.cored.model.VaadinProject;
import org.vaadin.cored.model.Project.ProjectType;

import com.vaadin.ui.Button;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CustomComponent;

@SuppressWarnings("serial")
public class VaadinBuildComponent extends CustomComponent implements BuildComponent,
		ClickListener {
	
	private final File buildXml;
	
	private static File deployDir;
	private static String deployURL;
	private static String deployPort;

	private OptionGroup deployTypeGroup = new OptionGroup("Deploy to:",
			Arrays.asList(new String[] {"Tomcat","AppScale","CloudFoundry","OSGi"}));
	
	public static void setDeployDir(String dir) {
		deployDir = new File(dir);
		if (!deployDir.isDirectory()) {
			deployDir = null;
			throw new RuntimeException("No such dir: " + dir);
		}
	}

	public static void setDeployURL(String url) {
		deployURL = url;
	}
	
	public static void setDeployPort(String port) {
		deployPort = port;
	}
	
	private final VaadinProject project;
	private final Button buildButton = new Button("Deploy Application");
	private String warDeployName;

	public VaadinBuildComponent(VaadinProject project) {
		VerticalLayout la = new VerticalLayout();
		deployTypeGroup.select("Tomcat");
		buildButton.setIcon(Icons.PAPER_PLANE);

		la.addComponent(deployTypeGroup);
		la.addComponent(buildButton);
		setCompositionRoot(la);
				
		this.project = project;
		File prDir = project.getProjectDir();
		buildXml = new File(prDir, "build.xml");
	}

	@Override
	public void attach() {
		super.attach();
		buildButton.addListener(this);
	}
	
	public void build() {
		VaadinBuildResultWindow resultWindow;
		try {
			String appUrl = doTheBuild();
			if (appUrl.contains("fail")){
				resultWindow = VaadinBuildResultWindow.failure(appUrl);				
				project.log("App deployed failed: " + appUrl);
			}else{
				resultWindow = VaadinBuildResultWindow.success(appUrl);
				project.log("App deployed to " + appUrl);
			}
		}
		catch (IOException e) {
			e.printStackTrace();
			resultWindow = VaadinBuildResultWindow.failure("Build failed");
		}
		catch (BuildException e) {
			e.printStackTrace();
			resultWindow = VaadinBuildResultWindow.failure("Build failed");
		}
		resultWindow.center();
		getWindow().addWindow(resultWindow);
	}

	public void buttonClick(ClickEvent event) {
		build();
	}
	
	private String getAppUrl() {
		if (deployURL != null) {
			return deployURL + "/apps/" + project.getName() + "?debug&restartApplication";
		}
		else if (deployPort != null) {
			URL url = getWindow().getURL();
			String x = url.getProtocol()+"://"+url.getHost()+":"+deployPort;
			return x+"/apps/" + warDeployName + "?debug&restartApplication";
		}
		else {
			URL url = getWindow().getURL();
			String x = url.getProtocol()+"://"+url.getAuthority();
			return x+"/apps/" + warDeployName + "?debug&restartApplication";
		}
		
	}

	private String doTheBuild() throws IOException {
		synchronized (project) {
			project.writeToDisk();
			warDeployName = null;
	
			if (project.getProjectType().equals(Project.ProjectType.vaadin)){
				warDeployName = antBuildWar();
			}else if (project.getProjectType().equals(Project.ProjectType.vaadinOSGi)){
				warDeployName = antBuildOsgi();				
			}else if (project.getProjectType().equals(Project.ProjectType.vaadinAppEngine)){
				warDeployName = antBuildAppEngine();
			}
			
			//Experimental implementation for using the  *-PaaS API implemented by 
			//Mohamed Sellami, Telecom SudParis. 
			//Sami Yangui, Telecom SudParis. 
			//Mohamed Mohamed, Telecom SudParis. 
			//Samir Tata, Telecom SudParis. 
			//for deploying the applications
			String deployTo = (String)deployTypeGroup.getValue();
			
			if (deployTo.toLowerCase().equals("cloudfoundry")){
				String appName = warDeployName.replace(".war", "");
				String warName = warDeployName;
				String warLocation = deployDir.toString();
				String deployLocation = "/home/ubuntu/cored";
				String memory = "128";
//				String paasApiUrl = "http://cf-paas-api.cloudfoundry.com/rest/";
				String paasApiUrl = "http://localhost:80/cf-api/rest/";
//				String paasApiUrl = "http://jlautamaki.dy.fi:8080/cf-api/rest/";
				Date today = new Date();
				String date = new SimpleDateFormat("yyyy-MM-dd").format(today);
				return APIClient.depployApp(appName, warName, warLocation,deployLocation,date,paasApiUrl,memory);
			}else if (deployTo.toLowerCase().equals("appscale")){
	             String dirPath = project.getProjectDir().getAbsolutePath();
	             File file = new File(deployDir, "hellotest.tar.gz");
	             String tarGzPath = (file).getAbsolutePath();
	             if (createTarGZ(dirPath,tarGzPath,true)){
	            	 String deployLocation = "http://130.230.142.82:8090/upload";
	            	 return APIClient.formPost(deployLocation, file);
	             }
	             return "deployment failed :(";
			}else if (deployTo.toLowerCase().equals("osgi")){
				return "osgi compilation worked, but app not deployed!";
			}else if (deployTo.toLowerCase().equals("tomcat")){
				return getAppUrl();
			}
			return "deployment failed :(";
		}

	}

	/**
	 * 
	 * Returns deploy name, eg. myproj-252352352452
	 * 
	 * that'll be in deploy_url/apps/myproj-252352352452
	 */
	private String antBuildWar() {
		org.apache.tools.ant.Project antProj = new org.apache.tools.ant.Project();
		antProj.setBasedir(project.getProjectDir().getAbsolutePath()); // TODO: turha?
		antProj.init();
		
//		antProj.addBuildListener(new BuildListener() {
//
//			public void buildFinished(BuildEvent arg0) {
//				System.out.println("buildFinished " +arg0.getTask()+" "+ arg0.getMessage());
//				
//			}
//
//			public void buildStarted(BuildEvent arg0) {
//				System.out.println("buildStarted " +arg0.getTask()+" "+ arg0.getMessage());
//				
//			}
//
//			public void messageLogged(BuildEvent arg0) {
//				System.out.println("messageLogged " +arg0.getTask()+" "+ arg0.getMessage());
//				
//			}
//
//			public void targetFinished(BuildEvent arg0) {
//				System.out.println("targetFinished " +arg0.getTask()+" "+ arg0.getMessage());
//				
//			}
//
//			public void targetStarted(BuildEvent arg0) {
//				System.out.println("targetStarted " +arg0.getTask()+" "+ arg0.getMessage());
//				
//			}
//
//			public void taskFinished(BuildEvent arg0) {
//				System.out.println("taskFinished " +arg0.getTask()+" "+ arg0.getMessage());
//				
//			}
//
//			public void taskStarted(BuildEvent arg0) {
//				System.out.println("taskStarted " +arg0.getTask()+" "+ arg0.getMessage());
//			}
//			
//		});

		ProjectHelper ph = new ProjectHelper2();
		ph.parse(antProj, buildXml);

		//create name for war
		final String projectName = project.getName();
		final String deployName;
		final String deployWarName;

		boolean complexName = false;
		if (complexName){
			deployName = projectName + "-" +(new Date()).getTime();
			deployWarName = "apps#" + deployName + ".war";
		}else{
			deployName = projectName + "" +(new Date()).getTime();
			deployWarName = deployName + ".war";
		}
		
		File warFile = new File(deployDir, deployWarName);

		antProj.setProperty("destfile", warFile.getAbsolutePath());
		
		antProj.executeTarget("war");
		
		String[] prevWars = deployDir.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return !name.equals(deployWarName) && name.startsWith("apps#"+projectName+"-") && name.endsWith(".war");
			}
		});
		if (prevWars != null) {
			for (String s : prevWars) {
				new File(deployDir, s).delete();
			}
		}
		
		return deployWarName;
	}

	/**
	 * 
	 * Returns deploy name, eg. myproj-252352352452
	 * 
	 * that'll be in deploy_url/apps/myproj-252352352452
	 */
	private String antBuildAppEngine() {
		org.apache.tools.ant.Project antProj = new org.apache.tools.ant.Project();
		antProj.setBasedir(project.getProjectDir().getAbsolutePath()); // TODO: turha?
		antProj.init();
		
//		antProj.addBuildListener(new BuildListener() {
//
//			public void buildFinished(BuildEvent arg0) {
//				System.out.println("buildFinished " +arg0.getTask()+" "+ arg0.getMessage());
//				
//			}
//
//			public void buildStarted(BuildEvent arg0) {
//				System.out.println("buildStarted " +arg0.getTask()+" "+ arg0.getMessage());
//				
//			}
//
//			public void messageLogged(BuildEvent arg0) {
//				System.out.println("messageLogged " +arg0.getTask()+" "+ arg0.getMessage());
//				
//			}
//
//			public void targetFinished(BuildEvent arg0) {
//				System.out.println("targetFinished " +arg0.getTask()+" "+ arg0.getMessage());
//				
//			}
//
//			public void targetStarted(BuildEvent arg0) {
//				System.out.println("targetStarted " +arg0.getTask()+" "+ arg0.getMessage());
//				
//			}
//
//			public void taskFinished(BuildEvent arg0) {
//				System.out.println("taskFinished " +arg0.getTask()+" "+ arg0.getMessage());
//				
//			}
//
//			public void taskStarted(BuildEvent arg0) {
//				System.out.println("taskStarted " +arg0.getTask()+" "+ arg0.getMessage());
//			}
//			
//		});

		ProjectHelper ph = new ProjectHelper2();
		ph.parse(antProj, buildXml);

		//create name for war
		final String projectName = project.getName();
		final String deployName;
		final String deployWarName;

		boolean complexName = false;
		if (complexName){
			deployName = projectName + "-" +(new Date()).getTime();
			deployWarName = "apps#" + deployName + ".war";
		}else{
			deployName = projectName + "" +(new Date()).getTime();
			deployWarName = deployName + ".war";
		}
		
//		File warFile = new File(deployDir, deployWarName);

//		antProj.setProperty("destfile", warFile.getAbsolutePath());
		
		antProj.executeTarget("compile");
		
/*		String[] prevWars = deployDir.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return !name.equals(deployWarName) && name.startsWith("apps#"+projectName+"-") && name.endsWith(".war");
			}
		});
		if (prevWars != null) {
			for (String s : prevWars) {
				new File(deployDir, s).delete();
			}
		}*/
		
		return deployWarName;
	}

	
	private String antBuildOsgi() {
		org.apache.tools.ant.Project antProj = new org.apache.tools.ant.Project();
		antProj.setBasedir(project.getProjectDir().getAbsolutePath()); // TODO: turha?
		antProj.init();
		
		ProjectHelper ph = new ProjectHelper2();
		ph.parse(antProj, buildXml);

		//create name for war
		final String projectName = project.getName();
		final String deployName;
		final String deployWarName;

		boolean complexName = false;
		if (complexName){
			deployName = projectName + "-" +(new Date()).getTime();
			deployWarName = "apps#" + deployName + ".war";
		}else{
			deployName = projectName + "" +(new Date()).getTime();
			deployWarName = deployName + ".war";
		}
		
		File warFile = new File(deployDir, deployWarName);

		antProj.setProperty("destfile", warFile.getAbsolutePath());
		
		antProj.executeTarget("package-as-osgi");
		
		String[] prevWars = deployDir.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return !name.equals(deployWarName) && name.startsWith("apps#"+projectName+"-") && name.endsWith(".war");
			}
		});
		if (prevWars != null) {
			for (String s : prevWars) {
				new File(deployDir, s).delete();
			}
		}
		
		return deployWarName;
	}
	
	private boolean createTarGZ(String dirPath, String tarGzPath,boolean first) throws FileNotFoundException, IOException {
         FileOutputStream fOut = null;
         BufferedOutputStream bOut = null;
         GzipCompressorOutputStream gzOut = null;
         TarArchiveOutputStream tOut = null;
         try{
             System.out.println(new File(".").getAbsolutePath());
             fOut = new FileOutputStream(new File(tarGzPath));
             bOut = new BufferedOutputStream(fOut);
             gzOut = new GzipCompressorOutputStream(bOut);
             tOut = new TarArchiveOutputStream(gzOut);
             addFileToTarGz(tOut, dirPath, "",first);
         } finally {
             tOut.finish();
             tOut.close();
             gzOut.close();
             bOut.close();
             fOut.close();
         }
         return true;
     }


     private void addFileToTarGz(TarArchiveOutputStream tOut, String path, String base,boolean first) throws IOException {
         File f = new File(path);
         System.out.println(f.exists());
    	 String entryName="";
         if (!first){
        	 entryName = base + f.getName();
        	 TarArchiveEntry tarEntry = new TarArchiveEntry(f, entryName);
        	 tOut.putArchiveEntry(tarEntry);
         }
        	 
         if (f.isFile()) {
        	 if (!first){
        		 IOUtils.copy(new FileInputStream(f), tOut);
                 tOut.closeArchiveEntry();
        	 }
         } else {
        	 if (!first){
        		 tOut.closeArchiveEntry();
        	 }
        	 File[] children = f.listFiles();
             if (children != null){
                 for (File child : children) {
                     System.out.println(child.getName());
                     if (first){
                    	 addFileToTarGz(tOut, child.getAbsolutePath(), entryName,false);
                     }else{
                    	 addFileToTarGz(tOut, child.getAbsolutePath(), entryName + "/",false);
                     }
                 }
             }
         }
     }
}
