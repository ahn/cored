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


public class CoredDoc {
	
	private final File projectRoot;
	private final ProjectFile file;
	private final File location;
	private final SharedDoc shared;
	
	private final ProjectLog log;
	
	private HashMap<String,SharedChat> markerChats =
			new HashMap<String,SharedChat>();
	
	public static Doc fromDisk(File projectDir, ProjectFile file) throws IOException {
		File loc = new File(projectDir, file.getPath());
		return new Doc(FileUtils.readFileToString(loc));
	}
	
	public CoredDoc(File root, ProjectFile file, SharedDoc shared, ProjectLog log) {
		this.projectRoot = root;
		this.file = file;
		this.location = new File(projectRoot, file.getPath());
		this.shared = shared;
		this.log = log;
	}
	
	public CoredDoc(File root, ProjectFile file, Doc doc, ProjectLog log) {
		this(root, file, new SharedDoc(doc), log);
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
	
	public SharedChat getMarkerChat(String markerId) {
		return markerChats.get(markerId);
	}
	
	public SharedChat getMarkerChatCreateIfNotExist(String markerId, List<ChatLine> initial) {
		SharedChat chat = markerChats.get(markerId);
		if (chat == null) {
			chat = new SharedChat(new Chat(initial));
			if (log!=null) {
				for (ChatLine li : initial) {
					log.logMarkerChat(markerId, li.getUserId(), li.getText());
				}
				chat.addTask(new ChatLogTask(log, markerId));
			}
			markerChats.put(markerId, chat);
		}
		return chat;
	}
	
	synchronized public Shared<Chat, ChatDiff> removeMarkerChat(String markerId) {
		return markerChats.remove(markerId);
	}
	
	
	
	public void readFromDisk() throws IOException {
		
		String content = FileUtils.readFileToString(location);
		getShared().setValue(new Doc(content), Shared.NO_COLLABORATOR_ID);
		
	}

	public void deleteFromDisk() {
		location.delete();
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
