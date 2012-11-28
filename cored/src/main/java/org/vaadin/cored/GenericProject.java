package org.vaadin.cored;

import java.io.File;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.vaadin.aceeditor.collab.DocDiff;
import org.vaadin.aceeditor.collab.gwt.shared.Doc;
import org.vaadin.diffsync.Shared;

import com.vaadin.data.Validator;
import com.vaadin.data.validator.AbstractValidator;

public class GenericProject extends Project { 
	
	private final static File srcDir = new File("src");
	
	private final String packageName;
	
	private final File srcPackageDir;

	public static class ClassUtils {
		
		private static final Pattern validClass = Pattern.compile("^[A-Za-z1-9_.]+$");
		
		public static boolean isValidClass(String s) {
			System.out.println("isValidClass(\""+s+"\");");
			return validClass.matcher(s).matches();
		}

		@SuppressWarnings("serial")
		public static class ClassNameValidator extends AbstractValidator {

			public ClassNameValidator() {
				super("Class names should contain letters, numbers, _");
			}

			public boolean isValid(Object value) {
				return value instanceof String && isValidClass((String) value);
			}	
		}
	}

	
	public static Project createProjectIfNotExist(String name) {
		return createProjectIfNotExist(name, true);
	}

	public static GenericProject createProjectIfNotExist(String name, boolean createSkeleton) {
		GenericProject p = new GenericProject(name, createSkeleton);
		return addProjectIfNotExist(p) ? p : null;
	}
	
	protected GenericProject(String name, boolean createSkeleton) {
		super(name,ProjectType.generic, false);
		packageName = "fi.tut.cs.cored."+getName();
		srcPackageDir = new File(srcDir, ProjectFile.pathFromPackage(packageName));
				
		if (createSkeleton) {
			initApp();
		}
		else {
			readFromDisk();
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
		return new ProjectFile(srcPackageDir, className+this.getFileEnding());
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
	
	private void initApp() {
		String ske = createSkeletonCode(getPackageName(), getApplicationClassName());
		createDoc(getApplicationFile(), ske);
	}

	private static String appClassNameFromProjectName(String name) {
		return name.substring(0, 1).toUpperCase()
				+ name.substring(1).toLowerCase() + "Application";
	}

	private static String createSkeletonCode(String pakkage, String cls) {
		return "";
	}
	
	protected void decorateDoc(ProjectFile file, Shared<Doc, DocDiff> sharedDoc) {
	}

	@Override
	public String getProgrammingLanguage() {
		return "";
	}
	
	@Override
	public String getFileEnding() {
		return "";
	}

	@Override
	public Validator getClassNameValidator() {
		return new ClassUtils.ClassNameValidator();
	}
	@Override
	public String[] getExtendsClasses() {
		final String[] components = {};
		return components;
	}
	@Override
	public String generateContent(String name, String base) {
		String content = "";
		return content;
	}

}
