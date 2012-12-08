package org.vaadin.cored;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.helper.ProjectHelper2;
import org.vaadin.cored.Project.DocListener;

import com.vaadin.terminal.ExternalResource;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class VaadinBuildComponent extends CustomComponent implements BuildComponent,
		ClickListener, DocListener {

	public enum DeployType {
		osgi, war
	};
	
	private static ThemeResource DEPLOY_ICON = new ThemeResource("icons/paper-plane.png");
	
	private final DeployType deployType;
	private static File deployDir;
	private final File buildXml;
	
	private static String deployURL;

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
		return deployDir != null;
	}

	private VerticalLayout layout = new VerticalLayout();

	private VaadinProject project;
	private Button buildButton = new Button("Deploy");
	{
		buildButton.setIcon(DEPLOY_ICON);
	}

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

	public VaadinBuildComponent(VaadinProject project, DeployType deployType) {
		this.project = project;
		this.deployType = deployType;
		File prDir = project.getProjectDir();
		buildXml = new File(prDir, "build.xml");
		
		draw();
		setCompositionRoot(layout);
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
			String appUrl = getAppUrl();
			appLink.setResource(new ExternalResource(appUrl));
			appLink.setVisible(true);
			appInfo.setVisible(true);
			User u = ((CoredApplication)getApplication()).getCoredUser();
			if (u==null) {
				project.log("App deployed to "+appUrl);
			}
			else {
				project.log(u.getName() + " deployed to " + appUrl);
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
	
	private String getAppUrl() {
		if (deployURL == null) {
			return "/apps/" + project.getName() + "?debug&restartApplication";
		}
		return deployURL + "/apps/" + project.getName() + "?debug&restartApplication";
	}

	private void build() throws IOException {

		project.writeToDisk();

		if (this.deployType.equals(DeployType.war)){
			antBuildWar();				
		}else if (this.deployType.equals(DeployType.osgi)){
			antBuildOsgi();				
		}

	}

	private void antBuildWar() {
		org.apache.tools.ant.Project antProj = new org.apache.tools.ant.Project();
		antProj.setBasedir(project.getProjectDir().getAbsolutePath()); // TODO: turha?
		antProj.init();
		
		antProj.addBuildListener(new BuildListener() {

			public void buildFinished(BuildEvent arg0) {
				System.out.println("buildFinished " +arg0.getTask()+" "+ arg0.getMessage());
				
			}

			public void buildStarted(BuildEvent arg0) {
				System.out.println("buildStarted " +arg0.getTask()+" "+ arg0.getMessage());
				
			}

			public void messageLogged(BuildEvent arg0) {
				System.out.println("messageLogged " +arg0.getTask()+" "+ arg0.getMessage());
				
			}

			public void targetFinished(BuildEvent arg0) {
				System.out.println("targetFinished " +arg0.getTask()+" "+ arg0.getMessage());
				
			}

			public void targetStarted(BuildEvent arg0) {
				System.out.println("targetStarted " +arg0.getTask()+" "+ arg0.getMessage());
				
			}

			public void taskFinished(BuildEvent arg0) {
				System.out.println("taskFinished " +arg0.getTask()+" "+ arg0.getMessage());
				
			}

			public void taskStarted(BuildEvent arg0) {
				System.out.println("taskStarted " +arg0.getTask()+" "+ arg0.getMessage());
			}
			
		});

		ProjectHelper ph = new ProjectHelper2();
		ph.parse(antProj, buildXml);

		File warFile = new File(deployDir, "apps#" + project.getName() + ".war");

		antProj.setProperty("destfile", warFile.getAbsolutePath());

		
		antProj.executeTarget("war");

	}

	private void antBuildOsgi() {
/*		org.apache.tools.ant.Project antProj = new org.apache.tools.ant.Project();
		antProj.setBasedir(buildTemplateDir.getAbsolutePath()); // TODO: turha?
		antProj.init();

		ProjectHelper ph = new ProjectHelper2();
		ph.parse(antProj, buildXml);

		File warFile = new File(deployDir, "apps#" + project.getName() + ".war");

		antProj.setProperty("destfile", warFile.getAbsolutePath());

		antProj.executeTarget("war");*/

		//from http://dev.vaadin.com/browser/svn/incubator/Arvue/arvue.com/src/com/arvue/frontend/ArvueEditor.java publish (and slightly modified)
		
        /*EditorModel m = ((EditorApplication) getApplication()).getModel();
        m.setClassName(model.getClassName());

        try {
            TextJavaGenerator txt = new TextJavaGenerator(m.getClassName());

            // TODO make it compile all classes in one go instead

            // Compile CustomComponent
            String className = m.getClassName();
            String simpleName = className
                    .substring(className.lastIndexOf(".") + 1);
            String res = CodeTools.compile(tmpDir, simpleName,
                    txt.generateClass(m));
            if (res != null) {
                getWindow().showNotification("Compilation failed", res,
                        Notification.TYPE_ERROR_MESSAGE);
                // TODO remove
                System.err.println(res);
                return;
            }

            // Compile Application class
            res = CodeTools.compile(tmpDir, CodeTools
                    .getApplicationSimpleName(ArvueEditor.this.appName),
                    CodeTools.generateApplicationJava(tmpDir,
                            ArvueEditor.this.appName, m.getClassName()));
            if (res != null) {
                getWindow().showNotification("Compilation failed", res,
                        Notification.TYPE_ERROR_MESSAGE);
                // TODO remove
                System.err.println(res);
                return;
            }
            // Write manifests etc
            CodeTools.writeManifest(tmpDir, ArvueEditor.this.appName,
                    ArvueEditor.this.version);

            CodeTools.writeOSGiXML(tmpDir, ArvueEditor.this.appName);

            // TODO
            File targetDir = new File(DEPLOY_DIR);
            File jar = CodeTools.packageJar(targetDir, tmpDir,
                    CodeTools.getPackageName(ArvueEditor.this.appName),
                    ArvueEditor.this.version);
            // CodeTools.deployJar(jar);
            getWindow().showNotification("Published", "To " + jar);

        } catch (IOException e) {
            // TODO
            getWindow().showNotification("Ouch!", e.getMessage());
        }
		*/
		
	}

}
