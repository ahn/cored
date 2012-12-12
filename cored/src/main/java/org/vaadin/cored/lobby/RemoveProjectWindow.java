package org.vaadin.cored.lobby;

import org.vaadin.cored.CoredWindow;
import org.vaadin.cored.Icons;
import org.vaadin.cored.model.Project;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window;
import com.vaadin.ui.Button.ClickEvent;

@SuppressWarnings("serial")
public class RemoveProjectWindow extends Window {
	
	public RemoveProjectWindow(final String projectName) {
		super("Remove "+projectName+"?");
		setWidth("300px");
		setHeight("300px");
		center();
		
		addComponent(new Label("Are you sure you want to permanently remove the project?"));
		final TextField tf = new TextField("Type the project name for confirmation:");
		addComponent(tf);
		HorizontalLayout ho = new HorizontalLayout();
		Button removeButton = new Button("Remove");
		removeButton.setIcon(Icons.CROSS_SCRIPT);
		removeButton.addListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				if (projectName.equals(tf.getValue())) {
					boolean removed = Project.removeProject(projectName);
					if (removed) {
						getParent().showNotification("Removed project "+projectName);
						((CoredWindow)getParent()).refreshRequested();
					}
					else {
						getParent().showNotification("Failed to remove "+projectName+"!", Notification.TYPE_WARNING_MESSAGE);
					}
				}
				else {
					getParent().showNotification("Incorrect confirmation", Notification.TYPE_WARNING_MESSAGE);
				}
				close();
			}
		});
		ho.addComponent(removeButton);
		
		Button cancelButton = new Button("Cancel");
		cancelButton.addListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				close();				
			}
		});
		ho.addComponent(cancelButton);
		addComponent(ho);
	}

}
