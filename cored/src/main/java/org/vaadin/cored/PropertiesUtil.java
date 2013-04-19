package org.vaadin.cored;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;
import java.util.Properties;

import org.vaadin.cored.model.Project;
import org.vaadin.cored.model.Project.ProjectType;
import org.vaadin.cored.model.VaadinProject;

// TODO: reorganize, some kind of enum of possible properties maybe, etc...

public class PropertiesUtil {

	public static class CoredProperties {
		public static String projectsRootDir;
		public static String templateDirVaadin;
		public static String templateDirVaadinAppEngine;
		public static String templateDirVaadinOSGi;
		public static String warDeployDir;
		public static String warDeployUrl;
		public static String warDeployPort;
		public static String facebookAppId;
		public static String logDir;
		
		private CoredProperties(String projectsRootDir_,
				String templateDirVaadin_,String templateDirVaadinAppEngine_,String templateDirVaadinOSGi_, String warDeployDir_,
				String warDeployUrl_, String warDeployPort_, String facebookAppId_, String logDir_) {
			projectsRootDir = projectsRootDir_;
			templateDirVaadin = templateDirVaadin_;
			templateDirVaadinOSGi = templateDirVaadinOSGi_;
			templateDirVaadinAppEngine = templateDirVaadinAppEngine_;
			warDeployDir = warDeployDir_;
			warDeployUrl = warDeployUrl_;
			warDeployPort = warDeployPort_;
			facebookAppId = facebookAppId_;
			logDir = logDir_;
		}

		/**
		 * Only apply properties when none of the affected components have yet
		 * been created! Eg. in Application init().
		 * @throws Exception 
		 */
		public void apply() {
			if (projectsRootDir != null) {
				Project.setProjectsRootDir(projectsRootDir);
			}
		
			if (warDeployDir != null) {
				VaadinBuildComponent.setDeployDir(warDeployDir);
			}


			if (warDeployUrl != null) {
				VaadinBuildComponent.setDeployURL(warDeployUrl);
			}
			else if (warDeployPort != null) {
				VaadinBuildComponent.setDeployPort(warDeployPort);
			}

			if (facebookAppId != null) {
				CoredApplication.setFacebookAppId(facebookAppId);
			}
			
			if (logDir != null) {
				Project.setLogDir(new File(logDir));
			}
		}
	}

	public static CoredProperties getPropertiesFromClasspathFile(String filename) throws IOException {
		InputStream inputStream = PropertiesUtil.class.getClassLoader()
				.getResourceAsStream(filename);

		if (inputStream == null) {
			throw new IOException("Could not get InputStream of resource " + filename);
		}
		Properties properties = new Properties();
		properties.load(inputStream);
		return readProperties(properties);
	}

	private static CoredProperties readProperties(Properties props) {
		
		System.out.println("Properties:");
		for (Entry<Object, Object> e : props.entrySet()) {
			System.out.println("  "+e.getKey()+": " + e.getValue());
		}

		String rootDir = (String) props.get("PROJECTS_ROOT_DIR");
		if (rootDir == null) {
			throw new RuntimeException("PROJECTS_ROOT_DIR not defined!");
		}

		String templateDirs = (String) props.get("WAR_BUILD_TEMPLATES_DIR");
		String templateDirVaadin = templateDirs + (String) props.get("BUILD_TEMPLATE_VAADIN");
		String templateDirAppEngine = templateDirs + (String) props.get("BUILD_TEMPLATE_VAADIN_APP_ENGINE");
		String templateDirOSGi = templateDirs + (String) props.get("BUILD_TEMPLATE_VAADIN_OSGI");
		String warDeployDir = (String) props.get("WAR_DEPLOY_DIR");
		String warDeployUrl = (String) props.get("WAR_DEPLOY_URL");
		String warDeployPort = (String) props.get("WAR_DEPLOY_PORT");
		String fbAppId = (String) props.get("FACEBOOK_APP_ID");
		String logDir = (String) props.get("LOG_DIR");
		return new CoredProperties(rootDir, templateDirVaadin, templateDirAppEngine,templateDirOSGi, warDeployDir,
				warDeployUrl, warDeployPort, fbAppId, logDir);
	}
	
	public static Properties getProperties(File file) throws IOException {
		Properties props = new Properties();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			props.load(fis);
			return props;
		}
		finally {
			if (fis!=null) {
				fis.close();
			}
		}
	}

	public static CoredProperties getPropertiesFromFile(String filename) throws IOException {
		return readProperties(getProperties(new File(filename)));
	}

	public synchronized static String getTemplateDir(ProjectType projectType) {
		if (projectType.equals(Project.ProjectType.vaadin)){
			return CoredProperties.templateDirVaadin;
		}else if (projectType.equals(Project.ProjectType.vaadinOSGi)){
			return CoredProperties.templateDirVaadinOSGi;
		}else if (projectType.equals(Project.ProjectType.vaadinAppEngine)){
			return CoredProperties.templateDirVaadinAppEngine;
		}
		return null;
	}
}
