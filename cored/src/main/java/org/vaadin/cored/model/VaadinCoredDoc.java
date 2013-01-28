package org.vaadin.cored.model;

import org.vaadin.aceeditor.ErrorChecker;
import org.vaadin.aceeditor.collab.DocDiff;
import org.vaadin.aceeditor.collab.ErrorCheckTask;
import org.vaadin.aceeditor.collab.gwt.shared.Doc;
import org.vaadin.aceeditor.java.CompilerErrorChecker;
import org.vaadin.diffsync.DiffTaskExecPolicy;
import org.vaadin.diffsync.Shared;

public class VaadinCoredDoc extends CoredDoc {
	
	private final VaadinProject project;
	
	public VaadinCoredDoc(VaadinProject project, ProjectFile file, SharedDoc shared) {
		super(project.getProjectDir(), file, shared, project.getLog());
		this.project = project;
		
		System.out.println("Vaadin doc " + file);
		if (file.getName().endsWith(".java")) {
			
			ErrorCheckTask task = createErrorCheckTaskFor(file, getShared());
			
//			DocDiff d = task.exec(sharedDoc.getValue(), null, Shared.NO_COLLABORATOR_ID);
//			if (d!=null) {
//				sharedDoc.applyDiff(d);
//			}
			
			getShared().addAsyncTask(task, DiffTaskExecPolicy.LATEST_CANCEL_RUNNING);	
		}
	}
	
	public VaadinCoredDoc(VaadinProject project, ProjectFile file, Doc doc) {
		this(project, file, new SharedDoc(doc));
		
	}
	
	private ErrorCheckTask createErrorCheckTaskFor(ProjectFile file, Shared<Doc,DocDiff> doc) {
		String className = project.fullJavaNameOf(file.getName());
		ErrorChecker checker = new CompilerErrorChecker(project.getCompiler(), className);
		ErrorCheckTask task = new ErrorCheckTask(doc.newCollaboratorId(), checker);
		return task;
	}

}
