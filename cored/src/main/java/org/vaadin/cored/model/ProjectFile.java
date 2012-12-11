package org.vaadin.cored.model;

import java.io.File;

/**
 * ProjectFile location is relative to project dir.
 * 
 *
 */
public class ProjectFile extends File {

	private static final long serialVersionUID = 4224816875397214821L;

	public ProjectFile(File parent, String child) {
		super(parent, child);
	}
	
	public ProjectFile(String f) {
		super(f);
	}

	public ProjectFile(String parent, String child) {
		super(parent, child);
	}
	
}
