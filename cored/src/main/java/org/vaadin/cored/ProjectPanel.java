package org.vaadin.cored;

import java.util.LinkedList;

import org.vaadin.cored.Project.DocListener;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.terminal.ExternalResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Tree;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

@SuppressWarnings("serial")
public class ProjectPanel extends Panel implements DocListener, Property.ValueChangeListener, ItemClickEvent.ItemClickListener {
	
	
	private final Project project;

	private VerticalLayout layout = new VerticalLayout();
	private final Button addButton = new Button("Add New");
	private final Button deleteButton = new Button("Delete");
	
	private Tree tree = new Tree();

	private Object selectedItemId;

	public ProjectPanel(final Project project) {
		super("Files");
		this.project = project;
		
		getContent().setSizeFull();
		
		setIcon(Icons.FOLDER_OPEN_DOCUMENT_TEXT);
		
		layout.addComponent(tree);
		tree.setSizeFull();
		layout.setExpandRatio(tree, 1);
		
		layout.setSizeFull();
		addComponent(layout);
		
		HorizontalLayout hl = new HorizontalLayout();
		
		addButton.setIcon(Icons.DOCUMENT_PLUS);
		addButton.addListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				Window win = project.createNewFileWindow();
				win.setWidth("400px");
				win.setHeight("300px");
				getWindow().addWindow(win);
			}
		});
		hl.addComponent(addButton);
		
		deleteButton.setIcon(Icons.CROSS_SCRIPT);
		deleteButton.setEnabled(false);
		deleteButton.addListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				if (canBeDeleted(selectedItemId)) {
					confirmDelete((ProjectFile)selectedItemId);
				}
			}
		});
		hl.addComponent(deleteButton);
		
		layout.addComponent(hl);
	}

	protected void confirmDelete(final ProjectFile pf) {
		final Window w = new Window("Delete "+pf.getName()+"?");
		final Window main = getWindow();
		w.setIcon(Icons.CROSS_SCRIPT);
		w.setModal(true);
		w.setResizable(false);
		w.addComponent(new Label("Delete "+pf.getName()+"?"));
		Button delete = new Button("Delete");
		delete.setIcon(Icons.CROSS_SCRIPT);
		delete.addListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				project.removeFile(pf);
				main.removeWindow(w);
			}
		});
		
		Button cancel = new Button("Cancel");
		cancel.addListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				main.removeWindow(w);
			}
		});
		
		HorizontalLayout ho = new HorizontalLayout();
		ho.addComponent(delete);
		ho.addComponent(cancel);
		w.addComponent(ho);
		
		main.addWindow(w);
	}

	@Override
	public void attach() {
		super.attach();
		refresh();
		
		tree.addListener((Property.ValueChangeListener)this);
		tree.addListener((ItemClickEvent.ItemClickListener)this);

		project.addListener(this);
	}

	@Override
	public void detach() {
		super.detach();
		project.removeListener(this);
		tree.removeListener((Property.ValueChangeListener)this);
		tree.removeListener((ItemClickEvent.ItemClickListener)this);
	}

	public void docCreated(ProjectFile file, long collaboratorId) {
		// "always synchronize on the application instance when accessing
		// Vaadin UI components or related data from another thread."
		// https://vaadin.com/forum/-/message_boards/view_message/1785789#_19_message_212956
		// Is this enough of synchronization?
		synchronized (getApplication()) {
			refresh();
		}
	}
	
	public void docRemoved(ProjectFile file, long collaboratorId) {
		// "always synchronize on the application instance when accessing
		// Vaadin UI components or related data from another thread."
		// https://vaadin.com/forum/-/message_boards/view_message/1785789#_19_message_212956
		// Is this enough of synchronization?
		synchronized (getApplication()) {
			refresh();
		}
	}
	
	private boolean canBeDeleted(Object obj) {
		if (obj instanceof ProjectFile) {
			ProjectFile pf = (ProjectFile)obj;
			return project.canBeDeleted(pf);
		}
		return false;
	}
	
	private void refresh() {
		tree.removeAllItems();
		project.fillTree(tree);
		
	}


	public interface FileSelectListener {
		public void fileSelected(ProjectFile file);
	}

	LinkedList<FileSelectListener> listeners = new LinkedList<FileSelectListener>();

	public void addListener(FileSelectListener li) {
		listeners.add(li);
	}
	
	public void removeListener(FileSelectListener li) {
		listeners.remove(li);
	}

	private void fireFileSelected(ProjectFile file) {
		for (FileSelectListener li : listeners) {
			li.fileSelected(file);
		}
	}

	public void itemClick(ItemClickEvent event) {
		if (!(event.getItemId() instanceof ProjectFile)) {
			return;
		}
		ProjectFile file = (ProjectFile) event.getItemId();
		if (event.getButton()==ItemClickEvent.BUTTON_MIDDLE) {
			String url = "#"+project.getName()+"/"+file.getName();
			getWindow().open(new ExternalResource(url), "_blank");
		}
		else if (event.isDoubleClick()) {
			fireFileSelected(file);
		}
	}

	public void valueChange(ValueChangeEvent event) {
		selectedItemId = event.getProperty().getValue();
		deleteButton.setEnabled(canBeDeleted(selectedItemId));
	}

}
