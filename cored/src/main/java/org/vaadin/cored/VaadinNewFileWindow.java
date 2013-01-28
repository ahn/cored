package org.vaadin.cored;

import org.vaadin.aceeditor.collab.gwt.shared.Doc;
import org.vaadin.cored.model.VaadinProject;
import org.vaadin.cored.model.VaadinProject.JavaUtils;

import com.vaadin.terminal.UserError;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Field;
import com.vaadin.ui.Form;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window;

@SuppressWarnings("serial")
public class VaadinNewFileWindow extends Window {

	public VaadinNewFileWindow(final VaadinProject project) {
		super("New Java Class");
		
		setIcon(Icons.DOCUMENT_PLUS);
		
		final Form form = new Form();
		form.setSizeFull();
		
		TextField nameField = new TextField("Name", "");
		nameField.setRequired(true);
		nameField.addValidator(new JavaUtils.JavaClassNameValidator());
		form.addField("name", nameField);
		
		String[] extendsClasses = getExtendsClasses();
		if (extendsClasses.length>0){
			OptionGroup og = new OptionGroup("extends");
			og.setNullSelectionAllowed(false);
			og.setRequired(true);

			for (String c : extendsClasses) {
				og.addItem(c);
				og.setItemCaption(c, c.substring(c.lastIndexOf(".")+1));
			}
			og.select(extendsClasses[0]);
			
			form.addField("base", og);
		}
			
		Button addButton = new Button("Add File");
		addButton.addListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				if (!form.isValid()) {
					form.setComponentError(new UserError("Please check the fields"));
					return;
				}
				
				String name = (String) form.getField("name").getValue();
				
				Field baseGroup = form.getField("base");
				String base = "";
				if (baseGroup!=null){
					base = (String) baseGroup.getValue();
				}

				String content = VaadinProject.generateContent(project.getPackageName(), name, base);
				project.addDoc(project.getFileOfClass(name), new Doc(content));
				
				VaadinNewFileWindow.this.close();
			}
		});
		
		form.getFooter().addComponent(addButton);
		
		addComponent(form);
		nameField.focus();
	}
	

	public static String[] getExtendsClasses() {
		final String[] components = {
			"java.lang.Object",
			"com.vaadin.ui.Panel",
			"com.vaadin.ui.Window",
			"com.vaadin.ui.CustomComponent" };
		return components;
	}
}
