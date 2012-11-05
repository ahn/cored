package org.vaadin.cored;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import org.vaadin.cored.Project.DocListener;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Component;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Tree;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class ProjectPanel extends Panel implements DocListener {

	private static final String NEW_FILE_ITEM_ID = "Add New...";

	private final Project project;

	private VerticalLayout layout = new VerticalLayout();

//	private Accordion acc = new Accordion();
	
	private Tree tree = new Tree();

	private boolean ignoreSelectedEvents;

	private HashMap<String, Component> tabsByFile = new HashMap<String, Component>();

	private Object selectedItemId;

	public ProjectPanel(Project project) {
		super("Project "+project.getName());
		if (project instanceof VaadinProject) {
			this.project = (VaadinProject) project;
		}else if(project instanceof PythonProject){
			this.project = (PythonProject) project;
		}else if(project instanceof GenericProject){
			this.project = (GenericProject) project;
		}else {
			throw new UnsupportedOperationException("ProjectPanel only supports VaadinProjects for now.");
		}
		
		layout.addComponent(tree);
		setContent(layout);
	}

	@Override
	public void attach() {
		super.attach();
		refresh();
		
		tree.addListener(new Property.ValueChangeListener() {
			
			public void valueChange(ValueChangeEvent event) {
				selectedItemId = event.getProperty().getValue();
			}
		});
		
		tree.addListener(new ItemClickEvent.ItemClickListener() {
			public void itemClick(ItemClickEvent event) {
				if (event.isDoubleClick() && selectedItemId instanceof ProjectFile) {
					tree.select(selectedItemId);
					fireFileSelected((ProjectFile) selectedItemId);
				}
				else if (event.isDoubleClick() && NEW_FILE_ITEM_ID.equals(selectedItemId)) {
					NewFileWindow win = new NewFileWindow(project);
					win.setWidth("400px");
					win.setHeight("400px");
					getWindow().addWindow(win);
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

//	public String getSelectedFile() {
//		Component sel = acc.getSelectedTab();
//		return sel == null ? null : acc.getTab(sel).getCaption();
//	}
//
//	public void setSelectedFile(String name) {
//		Component tab = tabsByFile.get(name);
//		if (tab != null) {
//			acc.setSelectedTab(tab);
//		}
//	}

//	@Override
	public void docCreated(ProjectFile file, long collaboratorId) {
		refresh();
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
		
		tree.addItem(NEW_FILE_ITEM_ID);
		tree.setItemIcon(NEW_FILE_ITEM_ID, PLUS_ICON);
		tree.setParent(NEW_FILE_ITEM_ID, project.getSourceDir());
		tree.setChildrenAllowed(NEW_FILE_ITEM_ID, false);
		
	}
	
	private static ThemeResource PLUS_ICON = new ThemeResource("icons/plus-white.png");
	
	private void addNth(Collection<List<File>> dirs, int n) {
		boolean added = false;
		for (List<File> dir : dirs) {
			if (dir.size()>n) {
				File dn = dir.get(n);
				tree.addItem(dn);
				tree.setItemCaption(dn, dn.getName());
				
				if (n > 0) {
					tree.setParent(dn, dir.get(n-1));
				}
				if (n==dir.size()-1) {
					tree.setChildrenAllowed(dn, false);
//					tree.setItemIcon(dn, res);
				}
				added = true;
			}
		}
		if (added) {
			addNth(dirs, n+1);
		}
	}


//	@Override
	public void docRemoved(ProjectFile file, long collaboratorId) {
//		refresh();
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
