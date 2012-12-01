package org.vaadin.cored;

import java.io.File;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.vaadin.aceeditor.collab.DocDiff;
import org.vaadin.aceeditor.collab.gwt.shared.Doc;
import org.vaadin.diffsync.Shared;

import com.vaadin.data.Validator;
import com.vaadin.data.validator.AbstractValidator;
import com.vaadin.ui.Tree;
import com.vaadin.ui.Window;

public class GenericProject extends Project { 
	
	protected GenericProject(String name) {
		super(name,ProjectType.generic);
	}

	@Override
	public void fillTree(Tree tree) {
		for (ProjectFile pf : super.getProjectFiles()) {
			tree.addItem(pf);
			tree.setItemCaption(pf, pf.getName());
			tree.setChildrenAllowed(pf, false);
		}
	}

	@Override
	public Window createNewFileWindow() {
		return new Window();
	}

}
