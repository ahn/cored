package org.vaadin.cored;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.vaadin.aceeditor.collab.DocDiff;
import org.vaadin.aceeditor.collab.gwt.shared.Doc;
import org.vaadin.cored.model.User;

public class ProjectLog {
	
	enum Type {
		EDIT_BY_USER,
		EDIT_BY_NO_USER,
		ADDMARKER,
		REMOVEMARKER,
		CHAT
	}
	
	public static abstract class LogItem {
		public final Date timestamp;
		private LogItem() {
			timestamp = new Date();
		}
		public String logString() {
			return timestamp + " " + logContentString();
		}
		abstract Type getType();
		abstract String logContentString();
	}
	
	public static class UserEditLogItem extends LogItem {
		public final User user;
		public final DocDiff diff;
		public final long collabId;
		private UserEditLogItem(User u, DocDiff d, long collabId) {
			user = u;
			diff = d;
			this.collabId = collabId;
		}
		@Override
		public String logContentString() {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public Type getType() {
			return Type.EDIT_BY_USER;
		}
	}
	
	public static class NonUserEditLogItem extends LogItem {
		public final DocDiff diff;
		public final long collabId;
		private NonUserEditLogItem(DocDiff d, long collabId) {
			diff = d;
			this.collabId = collabId;
		}
		@Override
		public String logContentString() {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public Type getType() {
			return Type.EDIT_BY_NO_USER;
		}
	}
	
	public static class ChatLogItem extends LogItem {
		public final User user;
		public final String message;
		private ChatLogItem(User user, String message) {
			this.user = user;
			this.message = message;
		}
		@Override
		public String logContentString() {
			// TODO Auto-generated method stub
			return null;
		}
		@Override
		public Type getType() {
			return Type.CHAT;
		}
	}
	
	private ArrayList<LogItem> log = new ArrayList<LogItem>();
	
	public ProjectLog() {
		
	}
	
	public ArrayList<LogItem> getLines() {
		return log;
	}
	
	public void logChat(User user, String message) {
		log.add(new ChatLogItem(user, message));
	}
	
	public void writeToFile(File f) throws IOException {
		StringBuilder sb = new StringBuilder();
		for (LogItem item : log) {
			sb.append(item.logString());
		}
		FileUtils.write(f, sb.toString());
	}

	public void logDiff(Doc value, DocDiff diff, long collaboratorId) {
		// TODO Auto-generated method stub
		
	}

	public void logEdit(User u, DocDiff diff, long collaboratorId) {
		if (u==null) {
			logNonUserEdit(diff, collaboratorId);
		}
		else {
			logUserEdit(u, diff, collaboratorId);
		}
	}

	private void logUserEdit(User u, DocDiff diff, long collaboratorId) {
		log.add(new UserEditLogItem(u, diff, collaboratorId));
	}

	private void logNonUserEdit(DocDiff diff, long collaboratorId) {
		log.add(new NonUserEditLogItem(diff, collaboratorId));
		
	}
}
