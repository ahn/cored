package org.vaadin.cored;

import java.io.File;
import java.util.TreeSet;

import org.vaadin.aceeditor.ErrorChecker;
import org.vaadin.aceeditor.collab.DocDiff;
import org.vaadin.aceeditor.collab.ErrorCheckTask;
import org.vaadin.aceeditor.collab.gwt.shared.Doc;
import org.vaadin.aceeditor.java.util.InMemoryCompiler;
import org.vaadin.diffsync.Shared;

public class VaadinProject extends Project {

	public enum Type {
		EMPTY, SKELETON
	}
	
	private final static File srcDir = new File("src");
	
	private final String packageName;
	
	private final File srcPackageDir;
	
	public static Project createProjectIfNotExist(String name) {
		return createProjectIfNotExist(name, null);
	}

	public static VaadinProject createProjectIfNotExist(String name,
			VaadinProject.Type type) {
		VaadinProject p = new VaadinProject(name, type);
		return addProjectIfNotExist(p) ? p : null;
	}
	
	protected VaadinProject(String name, Type type) {
		super(name);
		packageName = "fi.tut.cs.cored."+getName();
		srcPackageDir = new File(srcDir, ProjectFile.pathFromPackage(packageName));
				
		if (type==Type.SKELETON) {
			initVaadinApp();
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
		createDoc(file, content);
	}
	
	public TreeSet<ProjectFile> getSourceFiles() {
		TreeSet<ProjectFile> srcFiles = new TreeSet<ProjectFile>();
		for (ProjectFile f : getProjectFiles()) {
			if (f.getDir().equals(srcPackageDir)) {
				srcFiles.add(f);
			}
		}
		return srcFiles;
	}
	
	private void initVaadinApp() {
		String ske = createVaadinSkeletonCode(getPackageName(), getApplicationClassName());
		createDoc(getApplicationFile(), ske);
	}

	private static String appClassNameFromProjectName(String name) {
		return name.substring(0, 1).toUpperCase()
				+ name.substring(1).toLowerCase() + "Application";
	}

	private static String createVaadinSkeletonCode(String pakkage, String cls) {
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

}
