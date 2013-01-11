package org.vaadin.cored.model;

import org.vaadin.chatbox.gwt.shared.Chat;
import org.vaadin.chatbox.gwt.shared.ChatDiff;
import org.vaadin.chatbox.gwt.shared.ChatLine;
import org.vaadin.cored.ProjectLog;
import org.vaadin.diffsync.DiffTask;

public class ChatLogTask implements DiffTask<Chat, ChatDiff> {

	private final String markerId;
	private final ProjectLog log;
	
	public ChatLogTask(ProjectLog log) {
		this.markerId = null;
		this.log = log;
	}
	
	public ChatLogTask(ProjectLog log, String markerId) {
		this.markerId = markerId;
		this.log = log;
	}

	public ChatDiff exec(Chat value, ChatDiff diff, long collaboratorId) {
		
		for (ChatLine li : diff.getAddedLive()) {
			if (li.getUserId()==null) {
				continue;
			}
			if (markerId==null) {
				log.logChat(li.getUserId(), li.getText());
			}
			else {
				log.logMarkerChat(markerId, li.getUserId(), li.getText());
			}
		}
		return null;
	}

	public boolean needsToExec(Chat value, ChatDiff diff, long collaboratorId) {
		return diff!=null && !diff.getAddedLive().isEmpty();
	}

}
