package org.vaadin.cored;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.vaadin.aceeditor.collab.DocDiff;
import org.vaadin.aceeditor.collab.gwt.shared.Doc;
import org.vaadin.diffsync.Shared;

public class ProjectFile {
	public enum Type {
		DOC, BINARY;
	}

	private final Shared<Doc, DocDiff> doc;
	private final String binaryFileName;

	public ProjectFile(Shared<Doc, DocDiff> doc) {
		this.doc = doc;
		this.binaryFileName = null;
	}

	public ProjectFile(String binaryFileName) {
		this.doc = null;
		this.binaryFileName = binaryFileName;
	}

	public Type getType() {
		return doc != null ? Type.DOC : Type.BINARY;
	}

	public Shared<Doc, DocDiff> getDoc() {
		return doc;
	}

	public String getBinaryFileName() {
		return binaryFileName;
	}

	public void writeToFile(File f) throws IOException {
		if (doc == null) {
			return;
		}
		FileWriter fstream = new FileWriter(f);
		BufferedWriter out = new BufferedWriter(fstream);
		out.write(doc.getValue().getText());
		out.close();

	}

}
