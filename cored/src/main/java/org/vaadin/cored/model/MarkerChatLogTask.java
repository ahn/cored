package org.vaadin.cored.model;

import org.vaadin.chatbox.gwt.shared.Chat;
import org.vaadin.chatbox.gwt.shared.ChatDiff;
import org.vaadin.chatbox.gwt.shared.ChatLine;
import org.vaadin.cored.ProjectLog;
import org.vaadin.diffsync.DiffTask;

public class MarkerChatLogTask implements DiffTask<Chat, ChatDiff> {

	private final String markerId;
	private final ProjectLog log;
	
	public MarkerChatLogTask(String markerId, ProjectLog log) {
		this.markerId = markerId;
		this.log = log;
	}

	public ChatDiff exec(Chat value, ChatDiff diff, long collaboratorId) {
		if (diff==null || diff.getAddedLive().isEmpty()) {
			return null;
		}
		for (ChatLine li : diff.getAddedLive()) {
			if (li.getUserId()==null) {
				continue;
			}
			log.logMarkerChat(markerId, li.getUserId(), li.getText());
		}
		return null;
	}

}
