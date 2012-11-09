package org.vaadin.cored;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;

public class ProjectLog {
	
	enum Type {
		EDIT,
		ADDMARKER,
		REMOVEMARKER,
		CHAT
	}
	
	private ArrayList<String> lines = new ArrayList<String>();
	
	public ProjectLog() {
		
	}
	
	public List<String> getLines() {
		return lines;
	}
	
	public void logEdit(User u, ProjectFile f) {
		lines.add(new Date().getTime() + ":EDIT:" + f.getName()+":"+u.getUserId());
	}
	
	public void logChat(String userId, String message) {
		lines.add(new Date().getTime() + ":CHAT:" + (userId==null?"":userId)+":"+message);
	}
	
	public void writeToFile(File f) throws IOException {
		FileUtils.writeLines(f, lines);
	}
}
