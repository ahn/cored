package org.vaadin.cored;

import org.vaadin.aceeditor.collab.SuggestibleCollabAceEditor;;

public interface EditorFactory {
	public SuggestibleCollabAceEditor getEditorForFilename(String name);
}
