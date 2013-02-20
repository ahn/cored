package org.vaadin.cored.model;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.vaadin.aceeditor.collab.DocDiff;
import org.vaadin.aceeditor.collab.gwt.shared.Doc;
import org.vaadin.aceeditor.gwt.shared.LockMarkerData;
import org.vaadin.aceeditor.gwt.shared.Marker;
import org.vaadin.chatbox.SharedChat;
import org.vaadin.chatbox.gwt.shared.Chat;
import org.vaadin.chatbox.gwt.shared.ChatDiff;
import org.vaadin.chatbox.gwt.shared.ChatLine;
import org.vaadin.cored.ProjectLog;
import org.vaadin.diffsync.Shared;


/**
 * A document containing the Shared Doc as well as some related things such as marker chats.
 * 
 * Can be subclassed to create for example VaadinCoredDoc.
 * 
 *
 */
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
	
	/**
	 * 
	 * Override in subclass if needed
	 */
	protected boolean isEditable() {
		return false;
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
