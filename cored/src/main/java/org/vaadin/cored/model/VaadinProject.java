package org.vaadin.cored.model;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import org.apache.commons.io.FileUtils;
import org.vaadin.aceeditor.ErrorChecker;
import org.vaadin.aceeditor.collab.DocDiff;
import org.vaadin.aceeditor.collab.ErrorCheckTask;
import org.vaadin.aceeditor.collab.gwt.shared.Doc;
import org.vaadin.aceeditor.gwt.shared.Marker;
import org.vaadin.aceeditor.java.CompilerErrorChecker;
import org.vaadin.aceeditor.java.util.InMemoryCompiler;
import org.vaadin.cored.BuildComponent;
import org.vaadin.cored.Icons;
import org.vaadin.cored.VaadinBuildComponent;
import org.vaadin.cored.VaadinBuildComponent.DeployType;
import org.vaadin.cored.VaadinJarWindow;
import org.vaadin.cored.VaadinNewFileWindow;
import org.vaadin.diffsync.DiffTaskExecPolicy;
import org.vaadin.diffsync.Shared;

import com.vaadin.data.validator.AbstractValidator;
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
	
	private final String packageName;
	
	private final LinkedList<String> classpathItems = new LinkedList<String>();
	
	private final ProjectFile srcPackageDir;
	
	private InMemoryCompiler compiler;
	
	synchronized public static void setVaadinProjectTemplateDir(File dir) {
		templateDir  = dir;
	}
	
	synchronized private static File getVaadinProjectTemplateDir() {
		return templateDir;
	}
	
	
	@Override
	protected void projectDirCreated() {
		try {
			FileUtils.copyDirectory(getVaadinProjectTemplateDir(), getProjectDir());
		} catch (IOException e) {
			System.err.println("WARNING: could not initialize dir of vaadin project "+ getName());
		}
		updateJarsFromDisk();
		
		try {
			createWebXml();
		} catch (IOException e) {
			System.err.println("WARNING: could not create web.xml file of " +getName());
		}
	}
	
	synchronized public void updateJarsFromDisk() {

		File location = getLocationOfFile(jarDir);
		if (!location.isDirectory()) {
			return;
		}
		classpathItems.clear();
		for (File f : location.listFiles()) {
			classpathItems.add(f.getAbsolutePath());
		}
		getCompiler().setClasspath(classpathItems);

	}
	
	
	protected VaadinProject(String name) {
		super(name,ProjectType.vaadin);
		packageName = "fi.tut.cs.cored."+getName();
		srcPackageDir = new ProjectFile(srcDir, dirFromPackage(packageName).getPath());
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
			addDoc(getApplicationFile(), new Doc(ske));
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
	
			ErrorCheckTask task = createErrorCheckTaskFor(file, sharedDoc);
			
//			DocDiff d = task.exec(sharedDoc.getValue(), null, Shared.NO_COLLABORATOR_ID);
//			if (d!=null) {
//				sharedDoc.applyDiff(d);
//			}
			
			sharedDoc.addAsyncTask(task, DiffTaskExecPolicy.LATEST_CANCEL_RUNNING);	
		}
	}
	
	private ErrorCheckTask createErrorCheckTaskFor(ProjectFile file, Shared<Doc,DocDiff> doc) {
		String className = fullJavaNameOf(file.getName());
		ErrorChecker checker = new CompilerErrorChecker(getCompiler(), className);//, getLocationOfClassFile(className));
		ErrorCheckTask task = new ErrorCheckTask(doc.newCollaboratorId(), checker);
		return task;
	}
	
	synchronized public void checkErrors(ProjectFile file) {
		if (!file.getName().endsWith(".java")) {
			return;
		}
		CoredDoc doc = getDoc(file);
		if (doc == null) {
			return;
		}
		SharedDoc shared = doc.getShared();
		DocDiff d = createErrorCheckTaskFor(file, shared)
				.exec(shared.getValue(), null, 0);
		if (d!=null) {
			shared.applyDiff(d);
		}
		
	}
	
	String fullJavaNameOf(String name) {
		if (!name.endsWith(".java")) {
			return null;
		}
		return getPackageName()+"."+name.substring(0, name.length()-5);
	}
	
	synchronized public InMemoryCompiler getCompiler() {
		if (compiler == null) {
			compiler = new InMemoryCompiler();
		}
		return compiler;
	}
	
	synchronized public String getProjectClasspathPath() {
		// Add / to the end so that URLClassLoader understands that this is a dir.
		// Slash / is correct in Windows too, I think.
		return getLocationOfFile(getSourceDir()).getAbsolutePath()+"/";
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
	
	
	public ProjectFile getJavaFileOfClass(String fullClassName) {
		return new ProjectFile(fullClassName.replace('.', File.separatorChar)+".java");
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

	@Override
	public void removeDoc(ProjectFile file) {
		super.removeDoc(file);
		if (file.getName().endsWith(".java")) {
			getCompiler().removeClass(fullClassNameOf(file));
		}
	}
	
	private static String fullClassNameOf(ProjectFile javaFile) {
		String n = javaFile.getPath().substring(4); // strip "src/"
		return n.replace(File.separatorChar, '.').substring(0, n.length()-5);
	}
	
	private static ProjectFile fromFullClassName(String cls) {
		return new ProjectFile("src"+File.separator+cls.replace('.', File.separatorChar)+".java");
	}

	public boolean canBeDeleted(ProjectFile file) {
		return !getApplicationFile().equals(file);
	}

	@Override
	public void fillTree(Tree tree) {
		tree.addItem(getSourceDir());
		tree.setItemCaption(getSourceDir(), "Java Source Files");
		
		for (ProjectFile pf : getSourceFiles()) {
			tree.addItem(pf);
			
			CoredDoc doc = getDoc(pf);
			if (doc != null) {
				if (doc.getShared().getValue().hasErrors()) {
					tree.setItemIcon(pf, Icons.CROSS_CIRCLE);
				} else {
					tree.setItemIcon(pf, Icons.TICK_SMALL);
				}
			}
			
			tree.setChildrenAllowed(pf, false);
			Collection<User> uf = getTeam().getUsersByFile(pf);
			if (uf.isEmpty()) {
				tree.setItemCaption(pf, pf.getName());
			}
			else {
				tree.setItemCaption(pf, pf.getName()+" ("+uf.size()+")");
			}
			tree.setParent(pf, getSourceDir());
		}
		tree.expandItem(getSourceDir());
	}

	@Override
	public Window createNewFileWindow() {
		return new VaadinNewFileWindow(this);
	}
	
	@Override
	protected CoredDoc addNewCoredDoc(ProjectFile file, Doc doc) {
		return addNewCoredDoc(new VaadinCoredDoc(this, file, doc));
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
		mi.addItem("Compile all", new Command() {
			public void menuSelected(MenuItem selectedItem) {
				compileAll();
				menuBar.getWindow().showNotification("Compiled");
			}
		});
	}

	// XXX: this compilation is a bit of a mess...
	protected final void compileAll() {
		Map<ProjectFile, CoredDoc> m;
		synchronized (this) {
			 m = new HashMap<ProjectFile, CoredDoc>(files);
		}
		
		HashMap<String,String> ss = new HashMap<String,String>();
		for (Entry<ProjectFile, CoredDoc> e : m.entrySet()) {
			ss.put(fullClassNameOf(e.getKey()), e.getValue().getShared().getValue().getText());
		}
		
		Map<String, List<Diagnostic<? extends JavaFileObject>>> xx = getCompiler().compileAll(ss);
		for (Entry<String, List<Diagnostic<? extends JavaFileObject>>> e : xx.entrySet()) {
			CoredDoc doc = getDoc(fromFullClassName(e.getKey()));
			if (doc==null) {
				continue;
			}
			SharedDoc shared = doc.getShared();
			final List<Diagnostic<? extends JavaFileObject>> errors = e.getValue();
			DocDiff d = new ErrorCheckTask(shared.newCollaboratorId(), new ErrorChecker() {
				public Collection<Marker> getErrors(String source) {
					return CompilerErrorChecker.errorsFromDiagnostics(errors);
				}
			}).exec(shared.getValue(), null, Shared.NO_COLLABORATOR_ID);
			shared.applyDiff(d);
		}
		
		
		
		
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
//			+ "<init-param>\n"
//			+ "<description>Application widgetset</description>\n"
//			+ "<param-name>widgetset</param-name>\n"
//			+ "<param-value>org.vaadin.aceeditor.gwt.AceEditorWidgetSet</param-value>\n"
//			+ "</init-param>\n"
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
