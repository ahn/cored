package org.vaadin.cored;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.vaadin.cored.model.Project;
import org.vaadin.cored.model.VaadinProject;

public class PropertiesUtil {

	public static class CoredProperties {
		public final String projectsRootDir;
		public final String warBuildTemplateDir;
		public final String warDeployDir;
		public final String warDeployUrl;
		public final String facebookAppId;
		public final String classPath;

		private CoredProperties(String projectsRootDir,
				String warBuildTemplateDir, String warDeployDir,
				String warDeployUrl, String facebookAppId, String classPath) {
			this.projectsRootDir = projectsRootDir;
			this.warBuildTemplateDir = warBuildTemplateDir;
			this.warDeployDir = warDeployDir;
			this.warDeployUrl = warDeployUrl;
			this.facebookAppId = facebookAppId;
			this.classPath = classPath;
		}

		/**
		 * Only apply properties when none of the affected components have yet
		 * been created! Eg. in Application init().
		 */
		public void apply() {
			if (projectsRootDir != null) {
				Project.setProjectsRootDir(projectsRootDir);
			}

			if (warBuildTemplateDir != null) {
				VaadinProject.setVaadinProjectTemplateDir(new File(warBuildTemplateDir));
			}

			if (warDeployDir != null) {
				VaadinBuildComponent.setDeployDir(warDeployDir);
			}

			if (warDeployUrl != null) {
				VaadinBuildComponent.setDeployURL(warDeployUrl);
			}

			if (facebookAppId != null) {
				CoredApplication.setFacebookAppId(facebookAppId);
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
		
		System.out.println("props="+props);

		String rootDir = (String) props.get("PROJECTS_ROOT_DIR");
		if (rootDir == null) {
			throw new RuntimeException("PROJECTS_ROOT_DIR not defined!");
		}

		String warBuildTemplateDir = (String) props.get("WAR_BUILD_TEMPLATE_DIR");
		String warDeployDir = (String) props.get("WAR_DEPLOY_DIR");
		String warDeployUrl = (String) props.get("WAR_DEPLOY_URL");
		String fbAppId = (String) props.get("FACEBOOK_APP_ID");
		String classPath = (String) props.get("ADDITIONAL_CLASSPATH");

		return new CoredProperties(rootDir, warBuildTemplateDir, warDeployDir,
				warDeployUrl, fbAppId, classPath);
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
}
