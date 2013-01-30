package org.vaadin.cored.model;


import org.vaadin.aceeditor.collab.gwt.shared.Doc;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
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
		return new NewFileWindow(this);
	}
	
	private static final String[] ILLEGAL_CHARACTERS = { "/", "\n", "\r", "\t", "\0", "\f", "`", "?", "*", "\\", "<", ">", "|", "\"", ":" };
	
	@SuppressWarnings("serial")
	private class NewFileWindow extends Window {
		TextField nameField = new TextField();
		private NewFileWindow(final GenericProject p) {
			super("New File");
			HorizontalLayout h  = new HorizontalLayout();
			h.addComponent(new Label("Filename: "));
			h.addComponent(nameField);
			Button b = new Button("Add");
			addComponent(h);
			addComponent(b);
			b.addListener(new ClickListener() {
				public void buttonClick(ClickEvent event) {
					String n = (String)nameField.getValue();
					if (n.isEmpty()) {
						return;
					}
					for (String c : ILLEGAL_CHARACTERS) {
						if (n.contains(c)) {
							return;
						}
					}
					p.addDoc(new ProjectFile(n), new Doc(""));
					NewFileWindow.this.close();
				}
			});
		}
	}
}
