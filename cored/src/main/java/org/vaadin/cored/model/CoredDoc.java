package org.vaadin.cored.model;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.vaadin.aceeditor.collab.gwt.shared.Doc;


/**
 * A document in a cored project. 
 * 
 * The instances of this class can not be edited.
 * For editable documents, use EditableCoredDoc or one of its subclasses.
 *
 */
// TODO: this project/doc thing should be cleaned
// There's many project/doc classes whose responsibilities are not clear.
public class CoredDoc {
	
	protected final File projectRoot;
	protected final ProjectFile file;
	protected final File location;
	
	
	
	public static Doc fromDisk(File projectDir, ProjectFile file) throws IOException {
		File loc = new File(projectDir, file.getPath());
		return new Doc(FileUtils.readFileToString(loc));
	}
	
	public CoredDoc(File root, ProjectFile file) {
		this.projectRoot = root;
		this.file = file;
		this.location = new File(projectRoot, file.getPath());
	}
		
	
	public ProjectFile getProjectFile() {
		return file;
	}
	
	
	
//	public boolean writoDiskIfErrorFree() throws IOException {
//		if (!isEditable()) {
//			return false;
//		}
//		Doc doc = getShared().getValue();
//		if (doc.hasErrors()) {
//			return false;
//		}
//		writeToDisk();
//		return true;
//	}
	
	public void deleteFromDisk() {
		location.delete();
	}
	
}
