package org.vaadin.cored;

import java.io.File;

public class ProjectFile extends File {

	public ProjectFile(File parent, String child) {
		super(parent, child);
	}
	
	public ProjectFile(String f) {
		super(f);
	}

	public ProjectFile(String parent, String child) {
		super(parent, child);
	}
	
//	public ProjectFile(File f) {
//		this(f.getAbsolutePath());
//	}

//public class ProjectFile implements Comparable<Object> {

//	private final File relDir;
//	private final String name;
//	
//	public ProjectFile(File file) {
//		String parent = file.getParent();
//		this.relDir = parent==null ? new File() : new File(parent);
//		this.name = file.getName();
//	}
//
//	public ProjectFile(File dir, String name) {
//		this.relDir = dir;
//		this.name = name;
//	}
//	
//	public File getDir() {
//		return relDir;
//	}
//	
//	public String getName() {
//		return name;
//	}
//	
//	public String getPath() {
//		return new File(getDir(), getName()).getPath();
//	}
//	
//	public String getPackage() {
//		return packageFromDir(relDir).substring(4); // XXX TEMP HACK!!!
//	}
//	
//	public static String packageFromDir(File dir) {
//		return dir.getPath().replace(File.separator, ".");
//	}
//	
//	public static File dirFromPackage(String pakkage) {
//		return new File(pakkage.replace(".", File.separator));
//	}
//	
//	public static String pathFromPackage(String pakkage) {
//		return dirFromPackage(pakkage).getPath();
//	}
//	
//	@Override
//	public boolean equals(Object other) {
//		if (other instanceof ProjectFile) {
//			ProjectFile opf = (ProjectFile)other;
//			return opf.getName().equals(getName()) && opf.getDir().equals(getDir());
//		}
//		return false;
//	}
//	
//	@Override
//	public int hashCode() {
//		return getName().hashCode();
//	}
//
//	public int compareTo(Object o) {
//		if (o instanceof ProjectFile) {
//			ProjectFile opf = (ProjectFile) o;
//			int cmp = getDir().compareTo(opf.getDir());
//			return cmp!=0 ? cmp : getName().compareTo(opf.getName());
//		}
//		else {
//			throw new ClassCastException("Cannot compare ProjectFile with "+o);
//		}
//	}
//
//	public String getFullJavaName() {
//		if (!getName().endsWith(".java")) {
//			return null;
//		}
//		return getPackage()+"."+getName().substring(0, getName().length()-5);
//	}
//
//
////	public void writeToFile(Doc doc) throws IOException {
////		if (doc == null) {
////			return;
////		}
////		FileWriter fstream = new FileWriter(getFullName());
////		BufferedWriter out = new BufferedWriter(fstream);
////		out.write(doc.getText());
////		out.close();
////
////	}

}
