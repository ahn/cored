package org.vaadin.cored;

import java.util.Collection;
import java.util.LinkedList;

import org.vaadin.aceeditor.collab.User;

import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.themes.BaseTheme;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class ProjectSelecter extends VerticalLayout implements
		ItemClickListener, ClickListener {

	public interface Listener {
		void projectSelected(String projectName);

		void refreshRequested();
	}

	private LinkedList<Listener> listeners = new LinkedList<Listener>();

	public void addListener(Listener li) {
		listeners.add(li);
	}

	public void removeListener(Listener li) {
		listeners.remove(li);
	}

	private Table table = new Table();
	{
		table.setWidth("100%");
		table.addContainerProperty("Project name", String.class, null);
		table.addContainerProperty("Collaborators", String.class, null);
		table.setSelectable(true);
		table.addListener(this);
	}
	private Button button = new Button("Open");
	{
		button.setWidth("100%");
		button.setEnabled(table.getValue() != null);
		button.addListener(this);
	}

	private Button refreshButton = new Button("Refresh");
	{
		refreshButton.setStyleName(BaseTheme.BUTTON_LINK);
		refreshButton.addListener(new ClickListener() {
//			@Override
			public void buttonClick(ClickEvent event) {
				for (Listener li : listeners) {
					li.refreshRequested();
				}
			}
		});
	}

	public ProjectSelecter(Collection<String> projectNames) {
		super();

		setWidth("300px");

		for (String name : projectNames) {
			table.addItem(new Object[] { name, "" }, name);
		}
		HorizontalLayout hl = new HorizontalLayout();
		hl.setWidth("100%");
		Label title = new Label("Projects");
		hl.addComponent(title);
		hl.addComponent(refreshButton);
		hl.setExpandRatio(title, 1);
		addComponent(hl);
		addComponent(table);
		addComponent(button);
	}

	public void setProjectUsers(String projectName, Collection<User> users) {
		StringBuilder namesStr = new StringBuilder();
		boolean atLeastOne = false;
		for (User user : users) {
			if (atLeastOne) {
				namesStr.append(", ");
			}
			namesStr.append(user.getName());
			atLeastOne = true;
		}

		table.removeItem(projectName);
		table.addItem(
				new Object[] { projectName,
						(atLeastOne ? namesStr.toString() : "-") }, projectName);
	}

	/* @Override */
	public void itemClick(ItemClickEvent event) {
		if (event.getItemId() != null) {
			if (event.isDoubleClick()) {
				fireProjectSelected((String) event.getItemId());
			} else {
				button.setCaption("Open " + event.getItemId());
				button.setEnabled(true);
			}
		} else {
			button.setCaption("");
			button.setEnabled(false);
		}
	}

	private void fireProjectSelected(String projectName) {
		for (Listener li : listeners) {
			li.projectSelected(projectName);
		}
	}

	/* @Override */
	public void buttonClick(ClickEvent event) {
		if (table.getValue() != null) {
			fireProjectSelected((String) table.getValue());
		}

	}

}