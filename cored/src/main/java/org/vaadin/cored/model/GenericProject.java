package org.vaadin.cored.model;


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
