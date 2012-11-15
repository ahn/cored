package org.vaadin.cored;

import java.io.File;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.vaadin.aceeditor.ErrorChecker;
import org.vaadin.aceeditor.collab.DocDiff;
import org.vaadin.aceeditor.collab.ErrorCheckTask;
import org.vaadin.aceeditor.collab.gwt.shared.Doc;
import org.vaadin.aceeditor.java.util.InMemoryCompiler;
import org.vaadin.diffsync.Shared;

import com.vaadin.data.Validator;
import com.vaadin.data.validator.AbstractValidator;

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
	
	private final String packageName;
	
	private final File srcPackageDir;
	
	public static Project createProjectIfNotExist(String name) {
		return createProjectIfNotExist(name,true);
	}

	public static VaadinProject createProjectIfNotExist(String name,
			boolean createSkeleton) {
		VaadinProject p = new VaadinProject(name, createSkeleton);
		return addProjectIfNotExist(p) ? p : null;
	}
	
	protected VaadinProject(String name, boolean createSkeleton) {
		super(name,ProjectType.vaadin);
		packageName = "fi.tut.cs.cored."+getName();
		srcPackageDir = new File(srcDir, ProjectFile.pathFromPackage(packageName));
				
		if (createSkeleton) {
			initApp();
		}
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
		File dir = new File(srcDir, ProjectFile.dirFromPackage(pakkage).getPath());
		ProjectFile file = new ProjectFile(new File(dir, name));
		Shared<Doc, DocDiff> doc = createDoc(file, content);
		decorateDoc(file, doc);
	}
	
	public TreeSet<ProjectFile> getSourceFiles() {
		TreeSet<ProjectFile> srcFiles = new TreeSet<ProjectFile>();
		for (ProjectFile f : getProjectFiles()) {
			System.out.println("? " + f.getDir());
			if (f.getDir().equals(srcPackageDir)) {
				srcFiles.add(f);
			}
		}
		return srcFiles;
	}
	
	private void initApp() {
		String ske = createSkeletonCode(getPackageName(), getApplicationClassName());
		createDoc(getApplicationFile(), ske);
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
			InMemoryCompiler compiler = new InMemoryCompiler();
			compiler.appendClassPath(getLocationOfFile(getSourceDir()).getAbsolutePath());
			ErrorChecker checker = new FileSavingCompilerErrorChecker(compiler, getLocationOfFile(file));
			ErrorCheckTask task = new ErrorCheckTask(
					sharedDoc.newCollaboratorId(), checker);
			sharedDoc.addAsyncTask(task, true);
		}
	}
	@Override
	public String getProgrammingLanguage() {
		return "Java";
	}
	@Override
	public String getFileEnding() {
		return ".java";
	}

	@Override
	public Validator getClassNameValidator() {
		return new JavaUtils.JavaClassNameValidator();
	}

	@Override
	public String[] getExtendsClasses() {
		final String[] components = {
			"java.lang.Object",
			"com.vaadin.ui.Panel",
			"com.vaadin.ui.Window",
			"com.vaadin.ui.CustomComponent" };
		return components;
	}

	@Override
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
			return "class "+name+" {\n"
					+ "    \n}\n";
		}
		else {
			return "class "+name+" extends "+base.substring(base.lastIndexOf(".")+1)+" {\n"
					+ "    \n}\n";
		}
	}
	
	protected boolean canBeDeleted(ProjectFile file) {
		return !getApplicationFile().equals(file);
	}
	
}
