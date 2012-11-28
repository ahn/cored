package org.vaadin.cored;

import org.vaadin.aceeditor.collab.DocDiff;
import org.vaadin.aceeditor.collab.gwt.shared.Doc;
import org.vaadin.diffsync.DiffTask;

public class LoggerTask implements DiffTask<Doc, DocDiff> {

	private final Project project;
	private final ProjectLog log;
//	private final ProjectFile file;
	
	public LoggerTask(Project project, ProjectFile file) {
		this.project = project;
		this.log = project.getLog();
//		this.file = file;
	}

	public DocDiff exec(Doc value, DocDiff diff, long collaboratorId) {
		User u = project.getTeam().getUserByCollabId(collaboratorId);
		log.logEdit(u, diff, collaboratorId);
		return null;
	}


}
