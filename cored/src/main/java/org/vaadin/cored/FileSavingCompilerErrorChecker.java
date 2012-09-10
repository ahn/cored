package org.vaadin.cored;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

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
		Collection<Marker> errors = super.getErrors(source);
		if (errors==null || errors.isEmpty()) {
			System.out.println("Saving file to " + location);
			try {
				MyFileUtils.writeFileToDisk(location, source);
				System.out.println("saved!");
			} catch (IOException e) {
				System.out.println("saving failed!");
				e.printStackTrace();
			}
		}
		return errors;
	}

}
