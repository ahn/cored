package org.vaadin.cored;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.helper.ProjectHelper2;
import org.vaadin.cored.Project.DocListener;

import com.vaadin.terminal.ExternalResource;
import com.vaadin.terminal.Resource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.NativeSelect;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class VaadinBuildComponent extends Panel implements BuildComponent,
		ClickListener, DocListener {

	private static final Pattern rePackage = Pattern
			.compile("package\\s+([^;]+);");

	private static File buildTemplateDir;
	private static File deployDir;
	private static File webContent;
	private static File webInf;
	private static File webXmlFile;
	private static File buildXml;
	private static String deployURL;

	public static void setBuildTemplateDir(String dir) {
		buildTemplateDir = new File(dir);
		if (!buildTemplateDir.isDirectory()) {
			buildTemplateDir = null;
			throw new RuntimeException("No such dir: " + dir);
		}
		webContent = new File(buildTemplateDir, "WebContent");
		webInf = new File(webContent, "WEB-INF");
		webXmlFile = new File(webInf, "web.xml");
		buildXml = new File(buildTemplateDir, "build.xml");
	}

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

	private static boolean isDeployEnvSet() {
		return buildTemplateDir != null && deployDir != null;
	}

	private VerticalLayout layout = new VerticalLayout();

	private VaadinProject project;
	private Button buildButton = new Button("Deploy");

	private Link appLink = new Link("App Deployed! Click here to open.", null);
	private Label appInfo = new Label("(It may take a few seconds for the app to be available.)");
	private Label errorLabel = new Label();
	private Link errorButton = new Link("Build Error! Log here.", null);
	{
		errorLabel.setContentMode(Label.CONTENT_PREFORMATTED);
		errorLabel.setWidth("100%");
		errorLabel.setVisible(false);
		errorButton.setVisible(false);
	}

	public VaadinBuildComponent(VaadinProject project) {
		super("Deploy App");
		this.project = project;
		setContent(layout);
		draw();
	}

	private void draw() {
		layout.removeAllComponents();

		LinkedList<String> javaFiles = new LinkedList<String>();
		for (ProjectFile f : project.getProjectFiles()) {
			String name = f.getName();
			if (name.endsWith(".java")) {
				javaFiles.add(name.substring(0, name.length() - 5));
			}
		}

		layout.addComponent(buildButton);
		layout.addComponent(appLink);
		layout.addComponent(appInfo);
		appLink.setVisible(false);
		appInfo.setVisible(false);
		layout.addComponent(errorButton);
		layout.addComponent(errorLabel);
		buildButton.setEnabled(isDeployEnvSet() && !javaFiles.isEmpty());
	}

	@Override
	public void attach() {
		super.attach();
		project.addListener(this);
		buildButton.addListener(this);
	}

//	@Override
	public void docCreated(ProjectFile file, long collaboratorId) {
		draw();
	}

//	@Override
	public void docRemoved(ProjectFile file, long collaboratorId) {
		draw();
	}

//	@Override
	public void buttonClick(ClickEvent event) {
		appLink.setVisible(false);
		appInfo.setVisible(false);
		errorLabel.setVisible(false);
		try {
			build();
			if (deployURL != null) {
				appLink.setResource(appResource());
				appLink.setVisible(true);
				appInfo.setVisible(true);
			} else {
				errorLabel.setValue("Deployed");
				errorLabel.setVisible(true);
			}
		} catch (IOException e) {
			e.printStackTrace();
			errorLabel.setValue("Build failed!");
			errorLabel.setVisible(true);
		} catch (BuildException e) {
			e.printStackTrace();
			errorLabel.setValue("Build failed!");
			errorLabel.setVisible(true);
		}
	}

	private Resource appResource() {
		return new ExternalResource(deployURL + "/apps/" + project.getName()
				+ "?debug&restartApplication");
	}

	private void build() throws IOException {
		synchronized (buildTemplateDir) {

			project.writeToDisk();

			writeSrcToDisk();

			deleteDirectory(new File(new File(buildTemplateDir, "build"),
					"classes"));

			createWebXml(project.getApplicationClassName(), project.getPackageName());

			antBuildWar();
		}
	}

	private void writeSrcToDisk() throws IOException {
		File srcDir = new File(buildTemplateDir, "src");
		srcDir.mkdir();
		deleteDirectory(srcDir);
		srcDir.mkdir();
		project.writeSourceFilesTo(srcDir);
	}

	private void createWebXml(String appClass, String pakkage) throws IOException {

		String fullClass = pakkage == null ? appClass : pakkage + "." + appClass;
		System.err.println("fullClass=" + fullClass);
		String wx = webXml(appClass, fullClass);

		webXmlFile.delete();

		BufferedWriter out = new BufferedWriter(new FileWriter(webXmlFile));
		out.write(wx);
		out.close();

	}

	private void antBuildWar() {
		org.apache.tools.ant.Project antProj = new org.apache.tools.ant.Project();
		antProj.setBasedir(buildTemplateDir.getAbsolutePath()); // TODO: turha?
		antProj.init();

		ProjectHelper ph = new ProjectHelper2();
		ph.parse(antProj, buildXml);

		File warFile = new File(deployDir, "apps#" + project.getName() + ".war");

		antProj.setProperty("destfile", warFile.getAbsolutePath());

		antProj.executeTarget("war");

	}

	private static boolean deleteDirectory(File path) {
		if (path.exists()) {
			File[] files = path.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteDirectory(files[i]);
				} else {
					files[i].delete();
				}
			}
		}
		return (path.delete());
	}

	private static String webXml(String servletName, String appClass) {
		return String
				.format(WEB_XML_FORMAT, servletName, appClass, servletName);
	}

	private static final String WEB_XML_FORMAT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<web-app id=\"WebApp_ID\" version=\"2.4\" xmlns=\"http://java.sun.com/xml/ns/j2ee\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd\">\n"
			+ "<display-name>Delme</display-name>\n"
			+ "<context-param>\n"
			+ "<description>\n"
			+ "Vaadin production mode</description>\n"
			+ "<param-name>productionMode</param-name>\n"
			+ "<param-value>false</param-value>\n"
			+ "</context-param>\n"
			+ "<servlet>\n"
			+ "<servlet-name>%s</servlet-name>\n"
			+ "<servlet-class>com.vaadin.terminal.gwt.server.ApplicationServlet</servlet-class>\n"
			+ "<init-param>\n"
			+ "<description>\n"
			+ "Vaadin application class to start</description>\n"
			+ "<param-name>application</param-name>\n"
			+ "<param-value>%s</param-value>\n"
			+ "</init-param>\n"
			+ "</servlet>\n"
			+ "<servlet-mapping>\n"
			+ "<servlet-name>%s</servlet-name>\n"
			+ "<url-pattern>/*</url-pattern>\n"
			+ "</servlet-mapping>\n"
			+ "<welcome-file-list>\n"
			+ "<welcome-file>index.html</welcome-file>\n"
			+ "<welcome-file>index.htm</welcome-file>\n"
			+ "<welcome-file>index.jsp</welcome-file>\n"
			+ "<welcome-file>default.html</welcome-file>\n"
			+ "<welcome-file>default.htm</welcome-file>\n"
			+ "<welcome-file>default.jsp</welcome-file>\n"
			+ "</welcome-file-list>\n" + "</web-app>\n";

}
