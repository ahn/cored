package org.vaadin.cored;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.vaadin.aceeditor.ErrorChecker;
import org.vaadin.aceeditor.collab.DocDiff;
import org.vaadin.aceeditor.collab.ErrorCheckTask;
import org.vaadin.aceeditor.collab.gwt.shared.Doc;
import org.vaadin.aceeditor.java.util.InMemoryCompiler;
import org.vaadin.cored.VaadinBuildComponent.DeployType;
import org.vaadin.diffsync.DiffTaskExecPolicy;
import org.vaadin.diffsync.Shared;

import com.vaadin.data.validator.AbstractValidator;
import com.vaadin.ui.Component;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Tree;
import com.vaadin.ui.Window;

// TODO check synchronization

public class VaadinProject extends Project {	 
	public static class JavaUtils {
		
		private static final Pattern validClass = Pattern.compile("^[A-Z][A-Za-z1-9_]+$");
		
		public static boolean isValidJavaClass(String s) {
			System.out.println("isValidClass(\""+s+"\");");
			return validClass.matcher(s).matches();
		}

		@SuppressWarnings("serial")
		public static class JavaClassNameValidator extends AbstractValidator {

			public JavaClassNameValidator() {
				super("Class names should start with a capital letter and contain letters, numbers, _");
			}

			public boolean isValid(Object value) {
				return value instanceof String && isValidJavaClass((String) value);
			}	
		}
	}

	private final static ProjectFile srcDir = new ProjectFile("src");
	private final static ProjectFile jarDir = new ProjectFile("WebContent", new File("WEB-INF", "lib").getPath());
	private final static ProjectFile webXml = new ProjectFile("WebContent", new File("WEB-INF", "web.xml").getPath());

	private static File templateDir;
	
	private String packageName;
	
	private final LinkedList<String> classpathItems = new LinkedList<String>();
	
	private final File srcPackageDir;
	
	private InMemoryCompiler compiler;
	
	synchronized public static void setVaadinProjectTemplateDir(File dir) {
		templateDir  = dir;
	}
	
	synchronized private static File getVaadinProjectTemplateDir() {
		return templateDir;
	}
	
	
	@Override
	protected void projectDirCreated() {
		synchronized (getVaadinProjectTemplateDir()) {
			try {
				FileUtils.copyDirectory(getVaadinProjectTemplateDir(), getProjectDir());
			} catch (IOException e) {
				System.err.println("WARNING: could not initialize dir of vaadin project "+ getName());
			}
			updateJarsFromDisk();
		}
		
		try {
			createWebXml();
		} catch (IOException e) {
			System.err.println("WARNING: could not create web.xml file of " +getName());
		}
	}
	
	synchronized void updateJarsFromDisk() {

		File location = getLocationOfFile(jarDir);
		if (!location.isDirectory()) {
			return;
		}
		classpathItems.clear();
		classpathItems.add(getClasspathPath());
		for (File f : location.listFiles()) {
			classpathItems.add(f.getAbsolutePath());
		}
		getCompiler().setClasspath(classpathItems);

	}
	
	

	protected VaadinProject(String name) {
		super(name,ProjectType.vaadin);
		packageName = "fi.tut.cs.cored."+getName();
		srcPackageDir = new File(srcDir, dirFromPackage(packageName).getPath());
	}
	


	synchronized public String getPackageName() {
		return packageName;
	}
	
	synchronized public ProjectFile getSourceDir() {
		return srcDir;
	}
	
	synchronized public String getApplicationClassName() {
		return appClassNameFromProjectName(getName());
	}
	
	synchronized public ProjectFile getApplicationFile() {
		return getFileOfClass(getApplicationClassName());
	}
	
	synchronized public ProjectFile getFileOfClass(String className) {
		return new ProjectFile(srcPackageDir, className+".java");
	}
	
	synchronized public void createSrcDoc(String pakkage, String name, String content) {
		File dir = new File(srcDir, dirFromPackage(pakkage).getPath());
		ProjectFile file = new ProjectFile(dir, name);
		Shared<Doc, DocDiff> doc = createDoc(file, content);
		decorateDoc(file, doc);
	}
	
	private static File dirFromPackage(String pakkage) {
		return new File(pakkage.replace(".", File.separator));
	}

	synchronized public TreeSet<ProjectFile> getSourceFiles() {
		TreeSet<ProjectFile> srcFiles = new TreeSet<ProjectFile>();
		for (ProjectFile f : getProjectFiles()) {
			if (srcPackageDir.equals(f.getParentFile())) {
				srcFiles.add(f);
			}
		}
		return srcFiles;
	}
	
	@Override
	protected void projectInitialized(boolean createSkeleton) {
		if (createSkeleton) {
			String ske = createSkeletonCode(getPackageName(), getApplicationClassName());
			createDoc(getApplicationFile(), ske);
		}
		updateJarsFromDisk();
	}

	private static String appClassNameFromProjectName(String name) {
		return name.substring(0, 1).toUpperCase()
				+ name.substring(1).toLowerCase() + "Application";
	}

	private static String createSkeletonCode(String pakkage, String cls) {
		return "package "+pakkage+";\n\nimport com.vaadin.ui.Window;\n"
				+ "import com.vaadin.ui.Label;\n\n" + "public class " + cls
				+ " extends com.vaadin.Application {\n\n"
				+ "    public void init() {\n"
				+ "        Window main = new Window(\"" + cls + "\");\n"
				+ "        setMainWindow(main);\n"
				+ "        main.addComponent(new Label(\"This is " + cls
				+ "\"));\n" + "    }\n\n" + "}\n";

	}
	
	
	protected void decorateDoc(ProjectFile file, Shared<Doc, DocDiff> sharedDoc) {
		String filename = file.getName();
		if (filename.endsWith(".java")) {
			ErrorChecker checker = new FileSavingCompilerErrorChecker(getCompiler(), fullJavaNameOf(filename), getLocationOfFile(file));
			ErrorCheckTask task = new ErrorCheckTask(
					sharedDoc.newCollaboratorId(), checker);
			sharedDoc.addAsyncTask(task, DiffTaskExecPolicy.LATEST_CANCEL_RUNNING);
		}
	}
	
	private String fullJavaNameOf(String name) {
		if (!name.endsWith(".java")) {
			return null;
		}
		return getPackageName()+"."+name.substring(0, name.length()-5);
	}
	
	synchronized public InMemoryCompiler getCompiler() {
		if (compiler == null) {
			compiler = new InMemoryCompiler(getPackageName());
		}
		return compiler;
	}
	
	synchronized public String getClasspathPath() {
		return getLocationOfFile(getSourceDir()).getAbsolutePath();
	}

	public static String generateContent(String packageName, String className, String base) {
		String content = "package "+packageName+";\n\n"
				+ generateImports(base) + "\n\n"
				+ generateClass(className, base) + "\n";
		
		return content;
	}

	private static String generateImports(String base) {
		if (base.equals("java.lang.Object")) {
			return "";
		}
		else {
			return "import "+base +";";
		}
	}
	

	private static String generateClass(String name, String base) {
		if (base.equals("java.lang.Object")) {
			return "public class "+name+" {\n"
					+ "    \n}\n";
		}
		else {
			return "public class "+name+" extends "+base.substring(base.lastIndexOf(".")+1)+" {\n"
					+ "    \n}\n";
		}
	}
	
	protected boolean canBeDeleted(ProjectFile file) {
		return !getApplicationFile().equals(file);
	}

	@Override
	public void fillTree(Tree tree) {
		tree.addItem(getSourceDir());
		tree.setItemCaption(getSourceDir(), "Java Source Files");
		
		for (ProjectFile pf : getSourceFiles()) {
			tree.addItem(pf);
			tree.setItemCaption(pf, pf.getName());
			tree.setChildrenAllowed(pf, false);
//			tree.setItemIcon(pf, res);
			tree.setParent(pf, getSourceDir());
		}
		tree.expandItem(getSourceDir());
	}

	@Override
	public Window createNewFileWindow() {
		return new VaadinNewFileWindow(this);
	}
	
	@Override
	protected boolean isEditableFile(File f) {
		return f.getName().endsWith(".java");
	}
	
	@SuppressWarnings("serial")
	@Override
	public void addMenuItem(final MenuBar menuBar) {
		MenuItem mi = menuBar.addItem("Java", null, null);
		mi.addItem("Jars", new Command() {
			public void menuSelected(MenuItem selectedItem) {
				menuBar.getWindow().addWindow(new VaadinJarWindow(VaadinProject.this));
			}
		});
	}

	synchronized public List<String> getJarNames() {
		LinkedList<String> jarNames = new LinkedList<String>();
		for (String j : classpathItems) {
			if (j.endsWith(".jar")) {
				jarNames.add(new File(j).getName());
			}
		}
		return jarNames;
	}
	

	synchronized public String getJarDirAbsolutePath() {
		return getLocationOfFile(jarDir).getAbsolutePath();
	}

	synchronized public boolean removeJar(String filename) {
		for (String s : classpathItems) {
			File jarFile = new File(s);
			if (jarFile.getName().equals(filename)) {
				boolean deleted = jarFile.delete();
				if (deleted) {
					updateJarsFromDisk();
				}
				return deleted;
			}
		}
		return false;
	}
	
	@Override
	public BuildComponent createBuildComponent() {
		return new VaadinBuildComponent(this, DeployType.war);
	}
	
	private void createWebXml() throws IOException {
		
		String appClass = getApplicationClassName();
		String pakkage = getPackageName();

		String fullClass = pakkage == null ? appClass : pakkage + "." + appClass;
		System.err.println("fullClass=" + fullClass);
		String wx = webXml(appClass, fullClass);
		
		File f = getLocationOfFile(webXml);
		FileUtils.write(f, wx);
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
