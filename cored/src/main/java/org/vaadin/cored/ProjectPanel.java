package org.vaadin.cored;

import java.util.LinkedList;
import java.util.TreeSet;

import org.vaadin.cored.Project.DocListener;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.event.ItemClickEvent;
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
public class ProjectPanel extends Panel implements DocListener {
	
	
	private final Project project;

	private VerticalLayout layout = new VerticalLayout();
	private final Button addButton = new Button("Add New");
	private final Button deleteButton = new Button("Delete");
	
	private Tree tree = new Tree();

	private Object selectedItemId;

	public ProjectPanel(Project project) {
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
				NewFileWindow win = new NewFileWindow(ProjectPanel.this.project);
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
		
		tree.addListener(new Property.ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				selectedItemId = event.getProperty().getValue();
				deleteButton.setEnabled(canBeDeleted(selectedItemId));
			}
		});
		
		tree.addListener(new ItemClickEvent.ItemClickListener() {
			public void itemClick(ItemClickEvent event) {
				if (event.isDoubleClick() && selectedItemId instanceof ProjectFile) {
					tree.select(selectedItemId);
					fireFileSelected((ProjectFile) selectedItemId);
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

//	@Override
	public void docCreated(ProjectFile file, long collaboratorId) {
		refresh();
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
		
		TreeSet<ProjectFile> srcFiles = project.getSourceFiles();
		
		tree.addItem(project.getSourceDir());
		tree.setItemCaption(project.getSourceDir(), project.getProgrammingLanguage() + " Source Files");
		
		for (ProjectFile pf : srcFiles) {
			tree.addItem(pf);
			tree.setItemCaption(pf, pf.getName());
			tree.setChildrenAllowed(pf, false);
//			tree.setItemIcon(pf, res);
			tree.setParent(pf, project.getSourceDir());
		}
		
	}
	
	
	

//	@Override
	public void docRemoved(ProjectFile file, long collaboratorId) {
		refresh();
	}

	public interface FileSelectListener {
		public void fileSelected(ProjectFile file);
	}

	LinkedList<FileSelectListener> listeners = new LinkedList<FileSelectListener>();

	public void addListener(FileSelectListener li) {
		listeners.add(li);
	}

	private void fireFileSelected(ProjectFile file) {
		for (FileSelectListener li : listeners) {
			li.fileSelected(file);
		}
	}

}
