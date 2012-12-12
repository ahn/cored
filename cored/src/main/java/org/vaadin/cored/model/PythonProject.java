package org.vaadin.cored.model;

import java.util.regex.Pattern;


import com.vaadin.data.validator.AbstractValidator;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Tree;
import com.vaadin.ui.Window;

public class PythonProject extends Project { 
	

	public static class PythonUtils {
		
		private static final Pattern validClass = Pattern.compile("^[A-Za-z1-9_]+$");
		
		public static boolean isValidPythonClass(String s) {
			return validClass.matcher(s).matches();
		}

		@SuppressWarnings("serial")
		public static class PythonClassNameValidator extends AbstractValidator {

			public PythonClassNameValidator() {
				super("Class names should contain letters, numbers, _");
			}

			public boolean isValid(Object value) {
				return value instanceof String && isValidPythonClass((String) value);
			}	
		}
	}
	
	protected PythonProject(String name) {
		super(name,ProjectType.python);
	}
	
	@Override
	protected void projectInitialized(boolean createSkeleton) {
		if (createSkeleton) {
			String ske = createSkeletonCode();
			createDoc(new ProjectFile("main.py"), ske);
		}
	}


	private static String createSkeletonCode() {
		return "print \"Hello, World!\" \n";

	}

	@Override
	public void fillTree(Tree tree) {
		for (ProjectFile pf : super.getProjectFiles()) {
			tree.addItem(pf);
			tree.setItemCaption(pf, pf.getName());
			tree.setChildrenAllowed(pf, false);
		}
	}
	
	@SuppressWarnings("serial")
	private class NewPyFileWindow extends Window {
		TextField nameField = new TextField();
		private NewPyFileWindow(final PythonProject p) {
			super("New Python File");
			HorizontalLayout h  = new HorizontalLayout();
			h.addComponent(new Label("Python file: "));
			h.addComponent(nameField);
			h.addComponent(new Label(".py"));
			Button b = new Button("Add");
			addComponent(h);
			addComponent(b);
			b.addListener(new ClickListener() {
				public void buttonClick(ClickEvent event) {
					String n = (String)nameField.getValue();
					if (!PythonUtils.isValidPythonClass(n)) {
						return;
					}
					p.createDoc(new ProjectFile(n+".py"), "# "+n+".py\n");
					NewPyFileWindow.this.close();
				}
			});
		}
	}

	@Override
	public Window createNewFileWindow() {
		return new NewPyFileWindow(this);
	}
	

}
