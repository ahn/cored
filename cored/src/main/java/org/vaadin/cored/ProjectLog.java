package org.vaadin.cored;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import name.fraser.neil.plaintext.diff_match_patch.Operation;

import org.apache.commons.io.FileUtils;
import org.vaadin.aceeditor.gwt.shared.Marker;
import org.vaadin.cored.model.ProjectFile;
import org.vaadin.cored.model.User;

public class ProjectLog {
	
	enum Type {
		EDIT,
		CHAT,
		MARKER_CHAT,
		MARKER_ADD,
		MARKER_REMOVE,
		OPEN_FILE,
		CLOSE_FILE,
		NEW_FILE,
		REMOVE_FILE
	}
	
	public static abstract class LogItem {
		public final Date timestamp;
		private LogItem() {
			timestamp = new Date();
		}
		public String logString() {
			return timestamp.getTime() + " " + getType() + " " + logContentString();
		}
		abstract Type getType();
		abstract String logContentString();
	}
	
	public static class UserEditLogItem extends LogItem {
		public final ProjectFile file;
		public final long collaboratorId;
		public final Operation op;
		public final int editLen;
		public final int editPos;
		public final int textLen;
		private UserEditLogItem(ProjectFile file, long collaboratorId, Operation op,
				int editLen, int editPos, int textLen) {
			this.file = file;
			this.collaboratorId = collaboratorId;
			this.op = op;
			this.editLen = editLen;
			this.editPos = editPos;
			this.textLen = textLen;
		}
		@Override
		public String logContentString() {
			return file.getName()+";"+collaboratorId+";"+
					op+";"+editLen+";"+editPos+";"+textLen;
		}
		@Override
		public Type getType() {
			return Type.EDIT;
		}
	}
	
	public static class ChatLogItem extends LogItem {

		public final String userId;
		public final String message;
		private ChatLogItem(String userId, String message) {
			this.userId = userId;
			this.message = message;
		}
		@Override
		public String logContentString() {
			return userId+";"+message;
		}
		@Override
		public Type getType() {
			return Type.CHAT;
		}
	}
	
	public static class MarkerChatLogItem extends LogItem {
		public final String markerId;
		public final String userId;
		public final String message;
		private MarkerChatLogItem(String markerId, String userId, String message) {
			this.markerId = markerId;
			this.userId = userId;
			this.message = message;
		}
		@Override
		public String logContentString() {
			return markerId+";"+userId+";"+message;
		}
		@Override
		public Type getType() {
			return Type.MARKER_CHAT;
		}
	}
	
	public static class MarkerAddLogItem extends LogItem {
		public final ProjectFile file;
		public final long collaboratorId;
		public final String markerId;
		public final Marker.Type type;
		public final int start;
		public final int end;
		private MarkerAddLogItem(ProjectFile file, long collaboratorId, String markerId, Marker.Type type,
				int start, int end) {
			this.file = file;
			this.collaboratorId = collaboratorId;
			this.markerId = markerId;
			this.type = type;
			this.start = start;
			this.end = end;
		}
		@Override
		Type getType() {
			return Type.MARKER_ADD;
		}
		@Override
		String logContentString() {
			return file.getName()+";"+collaboratorId+";"+markerId+";"+type+";"+start+";"+end+";";
		}
	}
	
	public static class MarkerRemoveLogItem extends LogItem {
		public final ProjectFile file;
		public final long collaboratorId;
		public final String markerId;
		private MarkerRemoveLogItem(ProjectFile file, long collaboratorId, String markerId) {
			this.file = file;
			this.collaboratorId = collaboratorId;
			this.markerId = markerId;
		}
		@Override
		Type getType() {
			return Type.MARKER_REMOVE;
		}
		@Override
		String logContentString() {
			return file.getName()+";"+collaboratorId+";"+markerId;
		}
	}
	
	public static class FileOpenLogItem extends LogItem {
		public final ProjectFile file;
		public final long collaboratorId;
		public final User user;
		private FileOpenLogItem(ProjectFile file, long collaboratorId, User user) {
			this.file = file;
			this.collaboratorId = collaboratorId;
			this.user = user;
		}
		@Override
		Type getType() {
			return Type.OPEN_FILE;
		}
		@Override
		String logContentString() {
			return file.getName()+";"+collaboratorId+";"+user.getUserId()+";"+user.getName();
		}
	}
	
	public static class FileCloseLogItem extends LogItem {
		public final ProjectFile file;
		public final long collaboratorId;
		public final User user;
		private FileCloseLogItem(ProjectFile file, long collaboratorId, User user) {
			this.file = file;
			this.collaboratorId = collaboratorId;
			this.user = user;
		}
		@Override
		Type getType() {
			return Type.CLOSE_FILE;
		}
		@Override
		String logContentString() {
			return file.getName()+";"+collaboratorId+";"+user.getUserId()+";"+user.getName();
		}
	}
	
	public static class FileNewLogItem extends LogItem {
		public final ProjectFile file;
		private FileNewLogItem(ProjectFile file) {
			this.file = file;
		}
		@Override
		Type getType() {
			return Type.NEW_FILE;
		}
		@Override
		String logContentString() {
			return file.getName();
		}
	}
	
	public static class FileRemoveLogItem extends LogItem {
		public final ProjectFile file;
		private FileRemoveLogItem(ProjectFile file) {
			this.file = file;
		}
		@Override
		Type getType() {
			return Type.REMOVE_FILE;
		}
		@Override
		String logContentString() {
			return file.getName();
		}
	}
	
	
	private final ArrayList<LogItem> log = new ArrayList<LogItem>();
	private final File file;
	
	public ProjectLog(File outputFile) {
		this.file = outputFile;
	}
	
	public void logChat(String userId, String message) {
		log(new ChatLogItem(userId, message));
	}
	
	public void logMarkerChat(String markerId, String userId, String message) {
		log(new MarkerChatLogItem(markerId, userId, message));
	}

	public void logUserEdit(ProjectFile file, long collaboratorId, Operation op, int editLen, int editPos,
			int fileLen) {
		log(new UserEditLogItem(file, collaboratorId, op, editLen, editPos, fileLen));
	}
	
	public void logMarkerAdd(ProjectFile file, long collaboratorId, String markerId, Marker.Type type, int start,
			int end) {
		log(new MarkerAddLogItem(file, collaboratorId, markerId, type, start, end));
		
	}
	
	public void logMarkerRemove(ProjectFile file, long collaboratorId, String markerId) {
		log(new MarkerRemoveLogItem(file, collaboratorId, markerId));
		
	}
	
	public void logOpenFile(ProjectFile f, long collabId, User user) {
		log(new FileOpenLogItem(f, collabId, user));	
	}
	
	public void logCloseFile(ProjectFile f, long collabId, User user) {
		log(new FileCloseLogItem(f, collabId, user));
	}
	
	public void logNewFile(ProjectFile pf) {
		log(new FileNewLogItem(pf));
	}
	
	public void logRemoveFile(ProjectFile pf) {
		log(new FileRemoveLogItem(pf));
	}

	synchronized private void log(LogItem item) {
		log.add(item);
		if (file!=null) {
			logToFile(item);
		}
	}
	
	private void logToFile(LogItem item) {
		try {
			FileUtils.writeLines(file, Collections.singletonList(item.logString()), true);
		} catch (IOException e) {
			System.err.println("WARNING: could not write log to "+file);
		}
	}

	

	

	

	
}
