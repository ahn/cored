package org.vaadin.cored;

import java.util.Map.Entry;

import name.fraser.neil.plaintext.diff_match_patch.Diff;
import name.fraser.neil.plaintext.diff_match_patch.Operation;
import name.fraser.neil.plaintext.diff_match_patch.Patch;

import org.vaadin.aceeditor.collab.DocDiff;
import org.vaadin.aceeditor.collab.gwt.shared.Doc;
import org.vaadin.aceeditor.collab.gwt.shared.MarkerWithContext;
import org.vaadin.aceeditor.gwt.shared.Marker;
import org.vaadin.cored.model.Project;
import org.vaadin.cored.model.ProjectFile;
import org.vaadin.diffsync.DiffTask;
import org.vaadin.diffsync.Shared;

public class LoggerTask implements DiffTask<Doc, DocDiff> {

	private final ProjectLog log;
	private final ProjectFile file;
	
	public LoggerTask(Project project, ProjectFile file) {
		this.log = project.getLog();
		this.file = file;
	}

	public DocDiff exec(Doc value, DocDiff diff, long collaboratorId) {
		if (diff==null || collaboratorId==Shared.NO_COLLABORATOR_ID) {
			return null;
		}
		
			int len = value.getText().length();
			for (Patch p : diff.getTextDiff().getPatches()) {
				int start = p.start1;
				for (Diff d : p.diffs) {
					if (d.operation==Operation.EQUAL) {
						continue;
					}
					int ol = d.text==null ? 0 : d.text.length();
					log.logUserEdit(file, collaboratorId, d.operation, ol, start, len);
				}
			}
			for (Entry<String, MarkerWithContext> e : diff.getAddedMarkersAsUnmodifiable().entrySet()) {
				Marker m = e.getValue().getMarker();
//				if (m.getType()!=Marker.Type.NOTE && m.getType()!=Marker.Type.LOCK) {
//					continue;
//				}
				String markerId = e.getKey();
				log.logMarkerAdd(file, collaboratorId, markerId, m.getType(), m.getStart(), m.getEnd());
			}
			for (String markerId : diff.getRemovedMarkerIdsAsUnmodifiable()) {
				log.logMarkerRemove(file, collaboratorId, markerId);
			}
		
		return null;
	}

	public boolean needsToExec(Doc value, DocDiff diff, long collaboratorId) {
		return true;
	}


}
