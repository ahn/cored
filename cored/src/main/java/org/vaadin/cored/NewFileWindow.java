package org.vaadin.cored;

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
public class NewFileWindow extends Window {
		
//	private final Project project;

	public NewFileWindow(final Project project) {
		super("New " + project.getProgrammingLanguage() + " File");
//		this.project = project;
		
		setIcon(Icons.DOCUMENT_PLUS);
		
		final Form form = new Form();
		form.setSizeFull();
		
		TextField nameField = new TextField("Name", "");
		nameField.setRequired(true);
		nameField.addValidator(project.getClassNameValidator());
		form.addField("name", nameField);
		
		String[] extendsClasses = project.getExtendsClasses();
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
				System.out.println("valid");
				
				String name = (String) form.getField("name").getValue();
				
				Field baseGroup = form.getField("base");
				String base = "";
				if (baseGroup!=null){
					base = (String) baseGroup.getValue();
				}

				String content = project.generateContent(name, base);
				project.createDoc(project.getFileOfClass(name), content);
				
				NewFileWindow.this.close();
			}
		});
		
		form.getFooter().addComponent(addButton);
		
		addComponent(form);
	}	
}
