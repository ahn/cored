package org.vaadin.cored.model;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.vaadin.aceeditor.collab.DocDiff;
import org.vaadin.aceeditor.collab.gwt.shared.Doc;
import org.vaadin.aceeditor.gwt.shared.LockMarkerData;
import org.vaadin.aceeditor.gwt.shared.Marker;
import org.vaadin.diffsync.Shared;


public class CoredDoc {
	
	private final File projectRoot;
	private final ProjectFile file;
	private final File location;
	private final SharedDoc shared;
	
	public static Doc fromDisk(File projectDir, ProjectFile file) throws IOException {
		File loc = new File(projectDir, file.getPath());
		return new Doc(FileUtils.readFileToString(loc));
	}
	
	public CoredDoc(File root, ProjectFile file, SharedDoc shared) {
		this.projectRoot = root;
		this.file = file;
		this.location = new File(projectRoot, file.getPath());
		this.shared = shared;
	}
	
	public CoredDoc(File root, ProjectFile file, Doc doc) {
		this(root, file, new SharedDoc(doc));
	}
	
	public ProjectFile getProjectFile() {
		return file;
	}
	
	public SharedDoc getShared() {
		return shared;
	}

	public void setValue(Doc doc) {
		getShared().setValue(doc, Shared.NO_COLLABORATOR_ID);
	}

	public void writeToDisk() throws IOException {
		FileUtils.write(location, shared.getValue().getText());
	}
	
	public boolean writoDiskIfErrorFree() throws IOException {
		Doc doc = getShared().getValue();
		if (doc.hasErrors()) {
			return false;
		}
		writeToDisk();
		return true;
	}
	
	public void readFromDisk() throws IOException {
		
		String content = FileUtils.readFileToString(location);
		getShared().setValue(new Doc(content), Shared.NO_COLLABORATOR_ID);
		
	}

	public void delete() {
		
		// TODO: rm + remove marker chats
		
	}

	public void removeLocksOf(User user) {
		Doc doc = getShared().getValue();
		LinkedList<String> locksOf = new LinkedList<String>();
		for (Entry<String, Marker> e : doc.getMarkers().entrySet()) {
			Marker m = e.getValue();
			if (m.getType()==Marker.Type.LOCK &&
					user.getUserId().equals(((LockMarkerData)m.getData()).getLockerId())) {
				locksOf.add(e.getKey());
			}
		}
		if (!locksOf.isEmpty()) {
			getShared().applyDiff(DocDiff.removeMarkers(locksOf));
		}
	}
	
}
