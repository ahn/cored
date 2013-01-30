package org.vaadin.cored.model;

import org.vaadin.aceeditor.collab.DocDiff;
import org.vaadin.aceeditor.collab.gwt.shared.Doc;
import org.vaadin.diffsync.Shared;

public class SharedDoc extends Shared<Doc, DocDiff> {

	public SharedDoc(Doc value) {
		super(value);
	}

}
