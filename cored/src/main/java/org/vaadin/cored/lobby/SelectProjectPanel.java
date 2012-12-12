package org.vaadin.cored.lobby;

import java.util.Collection;
import java.util.LinkedList;

import org.vaadin.cored.Icons;

import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.BaseTheme;

@SuppressWarnings("serial")
public class SelectProjectPanel extends Panel implements
		ItemClickListener {
	

	public interface Listener {
		void projectSelected(String projectName);
		void refreshRequested();
		void removeRequested(String projectName);
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
	
	private Button selectButton = new Button("Open");
	{
		selectButton.setWidth("100%");
		selectButton.setEnabled(false);
		selectButton.addListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				if (table.getValue() != null) {
					fireProjectSelected((String) table.getValue());
				}
			}
		});
		
	}
	
	private Button removeButton = new Button();
	{
		removeButton.setEnabled(false);
		removeButton.addListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				if (table.getValue() != null) {
					fireRemoveRequested((String) table.getValue());
				}
			}
		});
		removeButton.setIcon(Icons.CROSS_SCRIPT);
	}
	

	private Button refreshButton = new Button("Refresh");
	{
		refreshButton.setStyleName(BaseTheme.BUTTON_LINK);
		refreshButton.addListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				for (Listener li : listeners) {
					li.refreshRequested();
				}
			}
		});
	}
	
	public SelectProjectPanel(Collection<ProjectDescription> pds) {
		super("Open Project");
		setIcon(Icons.BOX_ARROW);
		
		VerticalLayout layout = new VerticalLayout();

		for (ProjectDescription pd : pds) {
			table.addItem(new Object[] { pd.name, pd.getCollaborators() }, pd.name);
		}
		
		layout.addComponent(refreshButton);
		layout.addComponent(table);
		HorizontalLayout hl = new HorizontalLayout();
		hl.setWidth("100%");
		hl.addComponent(removeButton);
		hl.addComponent(selectButton);
		hl.setExpandRatio(selectButton, 1);
		layout.addComponent(hl);
		addComponent(layout);
	}


	public void itemClick(ItemClickEvent event) {
		if (event.getItemId() != null) {
			if (event.isDoubleClick()) {
				fireProjectSelected((String) event.getItemId());
			} else {
				selectButton.setCaption("Open " + event.getItemId());
			}
		} else {
			selectButton.setCaption("");
		}
		selectButton.setEnabled(event.getItemId()!=null);
		removeButton.setEnabled(event.getItemId()!=null);
	}

	private void fireProjectSelected(String projectName) {
		for (Listener li : listeners) {
			li.projectSelected(projectName);
		}
	}
	
	private void fireRemoveRequested(String projectName) {
		for (Listener li : listeners) {
			li.removeRequested(projectName);
		}
	}

}
