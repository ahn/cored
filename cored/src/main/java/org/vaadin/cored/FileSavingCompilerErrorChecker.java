package org.vaadin.cored;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;

import org.vaadin.aceeditor.gwt.shared.Marker;
import org.vaadin.aceeditor.java.CompilerErrorChecker;
import org.vaadin.aceeditor.java.util.InMemoryCompiler;

public class FileSavingCompilerErrorChecker extends CompilerErrorChecker {

	private File location;

	public FileSavingCompilerErrorChecker(InMemoryCompiler compiler, File location) {
		super(compiler);
		this.location = location;
	}
	
	@Override
	public Collection<Marker> getErrors(String source) {
		System.out.println("Checking errors!" + (new Date()));
		Collection<Marker> errors = super.getErrors(source);
		if (errors==null || errors.isEmpty()) {
			try {
				MyFileUtils.writeFileToDisk(location, source);
			} catch (IOException e) {
				System.err.println("saving failed!");
				e.printStackTrace();
			}
		}
		System.out.println("Checked! " + (new Date()));
		return errors;
	}

}
