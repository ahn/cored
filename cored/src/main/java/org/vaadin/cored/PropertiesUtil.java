package org.vaadin.cored;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesUtil {

	public static class CoredProperties {
		public final String projectsRootDir;
		public final String warBuildTemplateDir;
		public final String warDeployDir;
		public final String warDeployUrl;
		public final String facebookAppId;

		private CoredProperties(String projectsRootDir,
				String warBuildTemplateDir, String warDeployDir,
				String warDeployUrl, String facebookAppId) {
			this.projectsRootDir = projectsRootDir;
			this.warBuildTemplateDir = warBuildTemplateDir;
			this.warDeployDir = warDeployDir;
			this.warDeployUrl = warDeployUrl;
			this.facebookAppId = facebookAppId;
		}

		/**
		 * Only apply properties when none of the affected components have yet
		 * been created! Eg. in Application static {} block.
		 */
		public void apply() {
			if (projectsRootDir != null) {
				Project.setProjectsRootDir(projectsRootDir);
			}

			if (warBuildTemplateDir != null) {
				WarBuildComponent.setBuildTemplateDir(warBuildTemplateDir);
			}

			if (warDeployDir != null) {
				WarBuildComponent.setDeployDir(warDeployDir);
			}

			if (warDeployUrl != null) {
				WarBuildComponent.setDeployURL(warDeployUrl);
			}

			if (facebookAppId != null) {
				CoredApplication.setFacebookAppId(facebookAppId);
			}
		}
	}

	public static Properties getProperties(String filename) {
		InputStream inputStream = PropertiesUtil.class.getClassLoader()
				.getResourceAsStream(filename);

		if (inputStream == null) {
			return null;
		}
		Properties properties = new Properties();
		try {
			properties.load(inputStream);
		} catch (IOException e) {
			throw new RuntimeException("File '" + filename
					+ "' could not be found in classpath!");

		}
		return properties;
	}

	public static CoredProperties getCoredProperties(String filename) {
		Properties props = getProperties(filename);
		System.out.println("props="+props);

		String rootDir = (String) props.get("PROJECTS_ROOT_DIR");
		if (rootDir == null) {
			throw new RuntimeException("PROJECTS_ROOT_DIR not defined in '"
					+ filename + "'!");
		}

		String warBuildTemplateDir = (String) props
				.get("WAR_BUILD_TEMPLATE_DIR");
		String warDeployDir = (String) props.get("WAR_DEPLOY_DIR");
		String warDeployUrl = (String) props.get("WAR_DEPLOY_URL");
		String fbAppId = (String) props.get("FACEBOOK_APP_ID");

		return new CoredProperties(rootDir, warBuildTemplateDir, warDeployDir,
				warDeployUrl, fbAppId);
	}
}
