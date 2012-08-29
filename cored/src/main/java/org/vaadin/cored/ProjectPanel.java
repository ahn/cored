package org.vaadin.cored;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.vaadin.cored.Project.DocListener;

import com.vaadin.ui.Accordion;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TabSheet.SelectedTabChangeEvent;
import com.vaadin.ui.TabSheet.SelectedTabChangeListener;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.BaseTheme;

@SuppressWarnings("serial")
public class ProjectPanel extends Panel implements DocListener {

	private final Project project;

	private VerticalLayout layout = new VerticalLayout();

	private Accordion acc = new Accordion();

	private boolean ignoreSelectedEvents;

	private HashMap<String, Component> tabsByFile = new HashMap<String, Component>();

	public ProjectPanel(Project project) {
		super("Files of " + project.getName());
		this.project = project;
		layout.addComponent(acc);
		setContent(layout);
	}

	@Override
	public void attach() {
		super.attach();
		refresh();
		acc.addListener(new SelectedTabChangeListener() {
//			@Override
			public void selectedTabChange(SelectedTabChangeEvent event) {
				if (!ignoreSelectedEvents) {
					fireFileSelected(acc.getTab(acc.getSelectedTab())
							.getCaption()); // XXX
				}
			}
		});
		project.addListener(this);
	}

	@Override
	public void detach() {
		super.detach();
		project.removeListener(this);
	}

	public String getSelectedFile() {
		Component sel = acc.getSelectedTab();
		return sel == null ? null : acc.getTab(sel).getCaption();
	}

	public void setSelectedFile(String name) {
		Component tab = tabsByFile.get(name);
		if (tab != null) {
			acc.setSelectedTab(tab);
		}
	}

//	@Override
	public void docCreated(final String name, long collaboratorId) {
		refresh();
	}

	private void refresh() {
		tabsByFile.clear();
		String sel = getSelectedFile();
		boolean previousSelectStillExists = false;
		ignoreSelectedEvents = true;
		acc.removeAllComponents();
		List<String> files = project.getFileNames();
		for (final String dn : files) {
			if (dn.equals(sel)) {
				previousSelectStillExists = true;
			}
			Button removeButton = new Button("Remove");
			removeButton.setStyleName(BaseTheme.BUTTON_LINK);
			removeButton.addListener(new ClickListener() {
//				@Override
				public void buttonClick(ClickEvent event) {
					project.removeFile(dn);
					if (acc.getComponentCount() == 0) {
						fireFileSelected(null);
					}
				}
			});
			acc.addTab(removeButton, dn);
			tabsByFile.put(dn, removeButton);
		}

		// XXX

		if (previousSelectStillExists) {
			setSelectedFile(sel);
		} else {
			fireFileSelected(getSelectedFile());
		}

		ignoreSelectedEvents = false;
	}

//	@Override
	public void docRemoved(String name, long collaboratorId) {
		refresh();
	}

	public interface FileSelectListener {
		public void fileSelected(String name);
	}

	LinkedList<FileSelectListener> listeners = new LinkedList<FileSelectListener>();

	public void addListener(FileSelectListener li) {
		listeners.add(li);
	}

	private void fireFileSelected(String name) {
		for (FileSelectListener li : listeners) {
			li.fileSelected(name);
		}
	}

}
