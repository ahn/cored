package org.vaadin.cored;

import org.vaadin.aceeditor.collab.DocDiff;
import org.vaadin.aceeditor.collab.gwt.shared.Doc;
import org.vaadin.chatbox.ChatBox;
import org.vaadin.chatbox.SharedChat;
import org.vaadin.cored.ProjectPanel.FileSelectListener;
import org.vaadin.cored.Team.TeamListener;
import org.vaadin.diffsync.Shared;

import com.vaadin.terminal.ExternalResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

@SuppressWarnings("serial")
public class IDE extends VerticalLayout implements TeamListener, FileSelectListener {
	
	private EditorView editorView;
	private VerticalLayout editorLayout = new VerticalLayout();

	private CoredMenuBar menuBar;
	
	private final User user;
	private final Project project;
	private final BuildComponent buildComponent;

	private ChatBox chat;
	private SharedChat sharedChat;

	private ProjectPanel projectPanel;

	public IDE(User user, Project project, String initialFilename) {
		super();

		this.user = user;
		this.project = project;
		this.buildComponent = project.createBuildComponent();
		this.sharedChat = project.getProjectChat();

		setSizeFull();
		
		HorizontalLayout layout = new HorizontalLayout();
		layout.setSizeFull();
		
		menuBar = new CoredMenuBar(this);
		
		addComponent(menuBar);
		
		addComponent(layout);
		setExpandRatio(layout, 1);

		HorizontalSplitPanel hsp = new HorizontalSplitPanel();
		hsp.addComponent(createLeftBar());
		hsp.addComponent(editorLayout);
		editorLayout.setSizeFull();
		hsp.setSplitPosition(260, UNITS_PIXELS);

		layout.addComponent(hsp);
		layout.setExpandRatio(hsp, 1);
		
		
		editFile(initialFilename);
	}
	
	public Project getProject() {
		return project;
	}

	@Override
	public void attach() {
		super.attach();
		
		project.getTeam().addListener(this);
		projectPanel.addListener(this);


	}

	@Override
	public void detach() {
		super.detach();
		System.err.println(this + " detach");
		project.getTeam().removeListener(this);
		projectPanel.removeListener(this);
	}
	
	private void editFile(String filename) {
		editFile(project.getProjectFile(filename));
	}

	private void editFile(ProjectFile file) {
		if (file == null) {
			editorLayout.removeAllComponents();
		} else if (EditorUtil.isEditableWithEditor(file)) {
			Shared<Doc, DocDiff> doc = project.getDoc(file);
			if (doc != null) {
				editDoc(doc, file);
			} else {
				editorLayout.removeAllComponents();
			}
		} else {
			editorLayout.removeAllComponents();
			editorLayout.addComponent(new Label(file.getName()
					+ " can't be edited here :("));
		}
	}

	private void editDoc(Shared<Doc, DocDiff> doc, ProjectFile file) {
		editorView = new EditorView(file, project, user, true);
		editorView.setSizeFull();
		
		editorLayout.removeAllComponents();
		editorLayout.addComponent(editorView);
		editorLayout.setExpandRatio(editorView, 1);
	}

	private Component createLeftBar() {
		VerticalLayout leftBar = new VerticalLayout();

		leftBar.setSizeFull();


		Component pp = createProjectPanel();
		leftBar.addComponent(pp);
		leftBar.setComponentAlignment(pp, Alignment.MIDDLE_CENTER);
		leftBar.setExpandRatio(pp, 2);
		

		if (buildComponent!=null) {
			buildComponent.setWidth("90%");
			buildComponent.setHeight("50px");
			leftBar.addComponent(buildComponent);
			leftBar.setComponentAlignment(buildComponent, Alignment.MIDDLE_CENTER);
		}
		
		AllUsersWidget auw = new AllUsersWidget(project.getTeam());
		auw.setWidth("90%");
		leftBar.addComponent(auw);
		leftBar.setComponentAlignment(auw, Alignment.MIDDLE_CENTER);
		
		chat = new ChatBox(sharedChat);
		chat.setUser(user.getUserId(), user.getName(), user.getStyle());
		chat.setShowMyNick(true);
		chat.setWidth("90%");
		chat.setHeight("100%");
		leftBar.addComponent(chat);
		leftBar.setComponentAlignment(chat, Alignment.TOP_CENTER);
		leftBar.setExpandRatio(chat, 2);

		return leftBar;
	}

	private Component createProjectPanel() {
		projectPanel = new ProjectPanel(project);
		projectPanel.setWidth("90%");
		projectPanel.setHeight("90%");
		return projectPanel;
	}

	public void teamChanged() {
		System.out.println("teamChanged");
		// "always synchronize on the application instance when accessing
		// Vaadin UI components or related data from another thread."
		// https://vaadin.com/forum/-/message_boards/view_message/1785789#_19_message_212956
		// Is this enough of synchronization?
		synchronized (getApplication()) {
			if (!project.getTeam().hasUser(user)) {
				leaveIDE();
			}
		}
	}
	
	public void leaveProject() {
		project.getTeam().removeUser(user);
	}

	private void leaveIDE() {
		System.out.println("leaveIDE");
		Window win = getWindow();
		CoredApplication app = CoredApplication.getInstance();
		if (win!=null && app!=null) {
			System.out.println("leaveIDE win.open " + app.getURL());
			win.open(new ExternalResource(app.getURL()));
		}
	}

	public void fileSelected(ProjectFile file) {
		((CoredWindow)getWindow()).setFragment(project.getName()+"/"+file.getName()+"!");
		editFile(file);
	}

	public void logout() {
		Project.kickFromAllProjects(user);
		((CoredWindow)getWindow()).logoutUser();
	}

	public BuildComponent getBuildComponent() {
		return buildComponent;
	}

}
