package org.vaadin.cored;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.helper.ProjectHelper2;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CustomComponent;

@SuppressWarnings("serial")
public class VaadinBuildComponent extends CustomComponent implements BuildComponent,
		ClickListener {

	public enum DeployType {
		osgi, war
	};
	
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

	private final VaadinProject project;
	private final Button buildButton = new Button("Deploy Application");

	public VaadinBuildComponent(VaadinProject project, DeployType deployType) {
		buildButton.setIcon(Icons.PAPER_PLANE);
		setCompositionRoot(buildButton);
		
		this.project = project;
		this.deployType = deployType;
		File prDir = project.getProjectDir();
		buildXml = new File(prDir, "build.xml");
	}

	@Override
	public void attach() {
		super.attach();
		buildButton.addListener(this);
	}

	public void buttonClick(ClickEvent event) {
		VaadinBuildResultWindow resultWindow;
		try {
			build();
			String appUrl = getAppUrl();
			resultWindow = VaadinBuildResultWindow.success(appUrl);
			User user = ((CoredApplication)getApplication()).getCoredUser();
			project.log(user.getName() + " deployed to " + appUrl);
		}
		catch (IOException e) {
			e.printStackTrace();
			resultWindow = VaadinBuildResultWindow.failure();
		}
		catch (BuildException e) {
			e.printStackTrace();
			resultWindow = VaadinBuildResultWindow.failure();
		}
		resultWindow.center();
		getWindow().addWindow(resultWindow);
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
