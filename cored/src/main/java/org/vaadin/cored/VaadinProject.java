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
import org.vaadin.diffsync.DiffTaskExecPolicy;
import org.vaadin.diffsync.Shared;

import com.vaadin.data.validator.AbstractValidator;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Tree;
import com.vaadin.ui.Window;

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

	private final static File srcDir = new File("src");
	private final static File jarDir = new File("WebContent", new File("WEB-INF", "lib").getPath());

	private static String additionalClassPath;

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
	}
	
	synchronized private void updateJarsFromDisk() {

		File location = getLocationOfFile(jarDir);
		System.out.println("location: " + location);
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
	


	public String getPackageName() {
		return packageName;
	}
	
	public File getSourceDir() {
		return srcDir;
	}
	
	public String getApplicationClassName() {
		return appClassNameFromProjectName(getName());
	}
	
	public ProjectFile getApplicationFile() {
		return getFileOfClass(getApplicationClassName());
	}
	
	public ProjectFile getFileOfClass(String className) {
		return new ProjectFile(srcPackageDir, className+".java");
	}
	
	public void createSrcDoc(String pakkage, String name, String content) {
		File dir = new File(srcDir, dirFromPackage(pakkage).getPath());
		ProjectFile file = new ProjectFile(dir, name);
		Shared<Doc, DocDiff> doc = createDoc(file, content);
		decorateDoc(file, doc);
	}
	
	private static File dirFromPackage(String pakkage) {
		return new File(pakkage.replace(".", File.separator));
	}

	public TreeSet<ProjectFile> getSourceFiles() {
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
//			compiler.addToClasspath(getClasspathPath());
//			if (additionalClassPath!=null) {
//				for (String c : additionalClassPath.split(";")) { // XXX
//					compiler.addToClasspath(c);
//				}
//			}
		}
		return compiler;
	}
	
	public String getClasspathPath() {
		return getLocationOfFile(getSourceDir()).getAbsolutePath();
	}


	public String[] getExtendsClasses() {
		final String[] components = {
			"java.lang.Object",
			"com.vaadin.ui.Panel",
			"com.vaadin.ui.Window",
			"com.vaadin.ui.CustomComponent" };
		return components;
	}

	public String generateContent(String name, String base) {
		String content = "package "+getPackageName()+";\n\n"
				+ generateImports(base) + "\n\n"
				+ generateClass(name, base) + "\n";
		
		return content;
	}

	private String generateImports(String base) {
		if (base.equals("java.lang.Object")) {
			return "";
		}
		else {
			return "import "+base +";";
		}
	}
	

	private String generateClass(String name, String base) {
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

	public static void setAdditionalClassPath(String classPath) {
		additionalClassPath = classPath;
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
	}

	@Override
	public Window createNewFileWindow() {
		return new VaadinNewFileWindow(this);
	}
	
	@Override
	protected boolean isEditableFile(File f) {
		boolean editable = f.getName().endsWith(".java");;
		return editable;
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
	
//	@Override
//	protected void readFromDisk() {
//		System.out.println("readFromDisk");
//		TreeSet<File> files = getFilesIn(getProjectDir());
//		for (File f : files) {
//			if (!f.getName().equals(PROPERTY_FILE_NAME) && isEditableFile(f)){
//				System.out.println("Found "+f);
//				System.out.println("Fofo  " + readFileFromDisk(f));				
//			}
//		}
//	}
//
//	public List<String> getJars() {
//		// TODO Auto-generated method stub
//		return null;
//	}
//	
	public List<String> getJarNames() {
		LinkedList<String> jarNames = new LinkedList<String>();
		for (String j : classpathItems) {
			jarNames.add(new File(j).getName());
		}
		return jarNames;
	}
	
	synchronized private List<String> getClasspathItems() {
		return classpathItems;
	}

}
