package org.vaadin.cored;

import org.vaadin.aceeditor.collab.DocDiff;
import org.vaadin.aceeditor.collab.gwt.shared.Doc;
import org.vaadin.diffsync.DiffCalculator;

public class LoggerTask implements DiffCalculator<Doc, DocDiff> {

	private final Project project;
	private final ProjectLog log;
	private final ProjectFile file;
	
	public LoggerTask(Project project, ProjectFile file) {
		this.project = project;
		this.log = project.getLog();
		this.file = file;
	}
	
	public boolean needsToRunAfter(DocDiff diff, long byCollaboratorId) {
		log.logEdit(project.getTeam().getUserByCollabId(byCollaboratorId), file);
		return true;
	}

	public DocDiff calcDiff(Doc value) throws InterruptedException {
		return null;
	}

}
