package org.vaadin.cored;

import java.io.File;

public class ProjectFile implements Comparable {

	private final File dir;
	private final String name;
	
	public ProjectFile(File file) {
		this.dir = new File(file.getParent());
		this.name = file.getName();
	}

	public ProjectFile(File dir, String name) {
		this.dir = dir;
		this.name = name;
	}
	
	public File getDir() {
		return dir;
	}
	
	public String getName() {
		return name;
	}
	
	public String getPath() {
		return new File(getDir(), getName()).getPath();
	}
	
	public String getPackage() {
		return packageFromDir(dir);
	}
	
	public static String packageFromDir(File dir) {
		return dir.getPath().replace(File.separator, ".");
	}
	
	public static File dirFromPackage(String pakkage) {
		return new File(pakkage.replace(".", File.separator));
	}
	
	public static String pathFromPackage(String pakkage) {
		return dirFromPackage(pakkage).getPath();
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof ProjectFile) {
			ProjectFile opf = (ProjectFile)other;
			return opf.getName().equals(getName()) && opf.getDir().equals(getDir());
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return getName().hashCode();
	}

	public int compareTo(Object o) {
		if (o instanceof ProjectFile) {
			ProjectFile opf = (ProjectFile) o;
			int cmp = getDir().compareTo(opf.getDir());
			return cmp!=0 ? cmp : getName().compareTo(opf.getName());
		}
		else {
			throw new ClassCastException("Cannot compare ProjectFile with "+o);
		}
	}


//	public void writeToFile(Doc doc) throws IOException {
//		if (doc == null) {
//			return;
//		}
//		FileWriter fstream = new FileWriter(getFullName());
//		BufferedWriter out = new BufferedWriter(fstream);
//		out.write(doc.getText());
//		out.close();
//
//	}

}
