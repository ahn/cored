package org.vaadin.cored;

import com.vaadin.ui.ListSelect;
import com.vaadin.ui.Window;

@SuppressWarnings("serial")
public class VaadinJarWindow extends Window {

	private final ListSelect jarSelect;
	
	VaadinJarWindow(VaadinProject project) {
		super("Jar files of "+project.getName());
		getContent().setSizeFull();
		jarSelect = new ListSelect("Jars", project.getJarNames());
		addComponent(jarSelect);
		jarSelect.setSizeFull();
	}
	
}
