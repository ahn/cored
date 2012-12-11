package org.vaadin.cored.lobby;

import java.util.Arrays;
import java.util.LinkedList;

import org.vaadin.cored.model.GenericProject;
import org.vaadin.cored.model.Project;
import org.vaadin.cored.model.PythonProject;
import org.vaadin.cored.model.VaadinProject;
import org.vaadin.cored.model.Project.ProjectType;

import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class CreateProjectPanel extends Panel {

	private OptionGroup projectTypeGroup = new OptionGroup("Project type:",
			Arrays.asList(new String[] {"Vaadin", "Python", "Generic"}));
	
	private static final ThemeResource ICON = new ThemeResource("icons/box--plus.png");
	
	private Button cnpButton;
	
	public interface ProjectCreatedListener {
		public void projectCreated(Project p);
	}
	private LinkedList<ProjectCreatedListener> listeners = new LinkedList<CreateProjectPanel.ProjectCreatedListener>();
	
	public CreateProjectPanel() {
		super("Create New Project");
		
		init();
	}
	
	private void init() {
		VerticalLayout la = new VerticalLayout();
		final TextField tf = new TextField("Project name: (lower-case letters and numbers)");
		tf.setImmediate(true);
		final CheckBox skBox = new CheckBox("Create Application Skeleton");
		skBox.setValue(true);
		la.addComponent(tf);
		
		tf.addListener(new TextChangeListener() {
			public void textChange(TextChangeEvent event) {
				cnpButton.setEnabled(Project.isValidProjectName(event.getText()));
			}});
		
		projectTypeGroup.select("Vaadin");
		la.addComponent(projectTypeGroup);
		
		la.addComponent(skBox);
		
		cnpButton = createNewProjectButton(tf, skBox);
		cnpButton.setWidth("100%");
		setIcon(ICON);
		la.addComponent(cnpButton);
		
		addComponent(la);
	}
	
	private Button createNewProjectButton(final TextField tf, final CheckBox skBox) {
		Button b = new Button("Create Project");
		b.setEnabled(false);

		b.addListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				String name = ((String)tf.getValue()).toLowerCase();
				if (Project.isValidProjectName(name)) {
					String typeStr = (String)projectTypeGroup.getValue();
					Project p = null;
					ProjectType type;
					if ("Vaadin".equals(typeStr)) {
						type = ProjectType.vaadin;
					}
					else if ("Python".equals(typeStr)) {
						type = ProjectType.python;
					}
					else {
						type = ProjectType.generic;
					}
					p = Project.createProjectIfNotExist(name, type, skBox.booleanValue());
					if (p!=null) {
						fireProjectCreated(p);
					}
					else {
						getWindow().showNotification("Project already exists with that name.");
					}
				} else {
					getWindow().showNotification("Not a valid project name.");
				}
			}
		});
		return b;
	}
	
	public void addListener(ProjectCreatedListener li) {
		listeners.add(li);
	}
		
	private void fireProjectCreated(Project p) {
		for (ProjectCreatedListener li : listeners) {
			li.projectCreated(p);
		}
	}


}
