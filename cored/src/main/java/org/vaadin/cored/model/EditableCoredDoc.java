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

public class EditableCoredDoc extends CoredDoc {


	private final SharedDoc shared;
	
	private final ProjectLog log;
	
	private HashMap<String,SharedChat> markerChats =
			new HashMap<String,SharedChat>();
	
	public EditableCoredDoc(File root, ProjectFile file, SharedDoc shared, ProjectLog log) {
		super(root, file);
		this.shared = shared;
		this.log = log;
	}
	
	public EditableCoredDoc(File root, ProjectFile file, Doc doc, ProjectLog log) {
		this(root, file, new SharedDoc(doc), log);
	}
	
	@Override
	public boolean isEditable() {
		return true;
	}
	
	public SharedDoc getShared() {
		return shared;
	}

	public void setValue(Doc doc) {
		getShared().setValue(doc, Shared.NO_COLLABORATOR_ID);
	}

	public void writeToDisk() throws IOException {
		if (isEditable()) {
			FileUtils.write(location, shared.getValue().getText());
		}
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
