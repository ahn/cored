package org.vaadin.cored;

import com.vaadin.terminal.ExternalResource;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.Window;

@SuppressWarnings("serial")
public class VaadinBuildResultWindow extends Window {

	static VaadinBuildResultWindow success(String appUrl) {
		ExternalResource res = new ExternalResource(appUrl);
		Link link = new Link(appUrl, res);
		return new VaadinBuildResultWindow(link);
	}
	
	static VaadinBuildResultWindow failure(String message) {
		return new VaadinBuildResultWindow(message);
	}
	
	private VaadinBuildResultWindow(String msg) {
		super("Build failed");
		setIcon(Icons.CROSS_CIRCLE);
		setWidth("300px");
		setHeight("300px");
		addComponent(new Label(msg));
	}
	
	private VaadinBuildResultWindow(Link link) {
		super("Build Succesful");
		setIcon(Icons.TICK_CIRCLE);
		setWidth("300px");
		setHeight("300px");
		addComponent(new Label("Deployed application to"));
		addComponent(link);
		addComponent(new Label("NOTE: It will take a moment for the app to become available."));
	}

}
