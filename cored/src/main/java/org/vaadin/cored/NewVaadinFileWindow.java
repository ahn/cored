package org.vaadin.cored;

import com.vaadin.terminal.UserError;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Form;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window;

@SuppressWarnings("serial")
public class NewVaadinFileWindow extends Window {
	
	private static final String[] COMPONENTS = {
		"java.lang.Object",
		"com.vaadin.ui.Panel",
		"com.vaadin.ui.Window",
		"com.vaadin.ui.CustomComponent" };
	
	private final VaadinProject project;

	public NewVaadinFileWindow(final VaadinProject project) {
		super("New Java Class");
		this.project = project;
		
		final Form form = new Form();
		form.setSizeFull();
		
		TextField nameField = new TextField("Class Name", "");
		nameField.setRequired(true);
		nameField.addValidator(new JavaUtils.JavaClassNameValidator());
		form.addField("name", nameField);
		
		OptionGroup og = new OptionGroup("extends");
		og.setNullSelectionAllowed(false);
		og.setRequired(true);

		for (String c : COMPONENTS) {
			og.addItem(c);
			og.setItemCaption(c, c.substring(c.lastIndexOf(".")+1));
		}
		og.select("java.lang.Object");
		
		form.addField("base", og);
		
		Button addButton = new Button("Add File");
		addButton.addListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				System.out.println("click");
				if (!form.isValid()) {
					form.setComponentError(new UserError("Please check the fields"));
					return;
				}
				System.out.println("valid");
				
				String name = (String) form.getField("name").getValue();
				
				String base = (String) form.getField("base").getValue();
				String content = generateContent(name, base);
				project.createDoc(project.getFileOfClass(name), content);
				
				NewVaadinFileWindow.this.close();
			}
		});
		
		form.getFooter().addComponent(addButton);
		
		addComponent(form);
	}

	private String generateContent(String name, String base) {
		String content = "package "+project.getPackageName()+";\n\n"
				+ generateImports(base) + "\n\n"
				+ generateClass(name, base) + "\n";
		
		return content;
	}

	private String generateImports(String base) {
		if (base.equals("java.lang.Object")) {
			return "";
		}
		else {
			return "import "+base +";";
		}
	}
	

	private String generateClass(String name, String base) {
		if (base.equals("java.lang.Object")) {
			return "class "+name+" {\n"
					+ "    \n}\n";
		}
		else {
			return "class "+name+" extends "+base.substring(base.lastIndexOf(".")+1)+" {\n"
					+ "    \n}\n";
		}
	}
	
}
