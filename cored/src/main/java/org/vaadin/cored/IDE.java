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
	private final Component buildComponent;

//	private VerticalLayout rightBar = new VerticalLayout();
//	private MarkerWidget mw;
	private ChatBox chat;
	private SharedChat sharedChat;

	private ProjectPanel projectPanel;

	private TeamPanel teamPanel;

	public IDE(User user, Project project, String initialFilename) {
		super();

		this.user = user;
		this.project = project;
		project.getTeam().addUser(user);
		this.buildComponent = project.createBuildComponent();
		this.sharedChat = project.getProjectChat();

		setSizeFull();
		
		HorizontalLayout layout = new HorizontalLayout();
		layout.setSizeFull();
		
		menuBar = new CoredMenuBar(this);
		
		addComponent(menuBar);
		
		addComponent(layout);
		setExpandRatio(layout, 1);
//
//		rightBar.setWidth("240px");
//		rightBar.setHeight("100%");
//
//		mw = new MarkerWidget(project);
//		mw.setUser(user);
//		mw.setWidth("90%");
//		mw.setHeight("90%");
//		rightBar.addComponent(mw);
//		rightBar.setComponentAlignment(mw, Alignment.TOP_CENTER);


		
		HorizontalSplitPanel hsp = new HorizontalSplitPanel();
		hsp.addComponent(createLeftBar());
		hsp.addComponent(editorLayout);
		editorLayout.setSizeFull();
		hsp.setSplitPosition(260, UNITS_PIXELS);

		layout.addComponent(hsp);
//		layout.addComponent(rightBar);
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
		
//		mw.listenToEditor(editorView.getEditor());
		
		editorLayout.removeAllComponents();
		editorLayout.addComponent(editorView);
		editorLayout.setExpandRatio(editorView, 1);
	}

	private Component createLeftBar() {
		VerticalLayout leftBar = new VerticalLayout();

		leftBar.setSizeFull();

//		Component tp = createTeamPanel();
//		leftBar.addComponent(tp);
//		leftBar.setComponentAlignment(tp, Alignment.MIDDLE_CENTER);
		

		Component pp = createProjectPanel();
		leftBar.addComponent(pp);
		leftBar.setComponentAlignment(pp, Alignment.MIDDLE_CENTER);
		leftBar.setExpandRatio(pp, 2);
		
		if (buildComponent!=null) {
			buildComponent.setWidth("90%");
			buildComponent.setHeight("90%");
			leftBar.addComponent(buildComponent);
			leftBar.setComponentAlignment(buildComponent, Alignment.MIDDLE_CENTER);
			leftBar.setExpandRatio(buildComponent, 1);
		}
		
		chat = new ChatBox(sharedChat);
		chat.setUser(user.getUserId(), user.getName(), user.getStyle());
		chat.setShowMyNick(false);
		chat.setCaption("Project-wide Chat:");
		chat.setWidth("90%");
		chat.setHeight("90%");
		leftBar.addComponent(chat);
		leftBar.setComponentAlignment(chat, Alignment.TOP_CENTER);
		leftBar.setExpandRatio(chat, 2);
		

		


		return leftBar;
	}

	private Component createTeamPanel() {
		teamPanel = new TeamPanel(project.getTeam());
		teamPanel.setWidth("90%");
		teamPanel.setHeight("90%");
		return teamPanel;
	}

	private Component createProjectPanel() {
		projectPanel = new ProjectPanel(project);
		projectPanel.setWidth("90%");
		projectPanel.setHeight("90%");
		return projectPanel;
	}

//	@Override
	public void teamChanged(String message) {
		System.err.println("teamChanged " + message);
		
		if (!project.getTeam().hasUser(user)) {
			leaveIDE();
		}
		if (message!=null) {
			getWindow().showNotification(message);
		}
	}

	public void leaveIDE() {
		System.out.println("leaveIDE");
		if (user!=null) {
			System.out.println("kicking user " + user.getName());
			project.getTeam().kickUser(user);
			project.removeLocksOf(user);
		}
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

}
