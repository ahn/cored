package org.vaadin.cored;

import com.vaadin.terminal.ExternalResource;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.Window;

@SuppressWarnings("serial")
public class VaadinBuildResultWindow extends Window {

	static VaadinBuildResultWindow success(String appUrl) {
		return new VaadinBuildResultWindow(appUrl);
	}
	
	static VaadinBuildResultWindow failure() {
		return new VaadinBuildResultWindow();
	}
	
	private VaadinBuildResultWindow() {
		super("Build failed");
		setIcon(Icons.CROSS_CIRCLE);
		setWidth("300px");
		setHeight("300px");
	}
	
	private VaadinBuildResultWindow(String appUrl) {
		super("Build Succesful");
		setIcon(Icons.TICK_CIRCLE);
		setWidth("300px");
		setHeight("300px");
		addComponent(new Label("Deployed application to"));
		ExternalResource res = new ExternalResource(appUrl);
		Link link = new Link(appUrl, res);
		addComponent(link);
		addComponent(new Label("NOTE: It will take a moment for the app to become available."));
	}

}
