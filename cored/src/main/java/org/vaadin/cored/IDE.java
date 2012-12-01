package org.vaadin.cored;

import java.util.List;

import org.vaadin.aceeditor.collab.DocDiff;
import org.vaadin.aceeditor.collab.SuggestibleCollabAceEditor;
import org.vaadin.aceeditor.collab.gwt.shared.Doc;
import org.vaadin.aceeditor.java.VaadinSuggester;
import org.vaadin.aceeditor.java.util.InMemoryCompiler;
import org.vaadin.chatbox.ChatBox;
import org.vaadin.chatbox.SharedChat;
import org.vaadin.cored.ProjectPanel.FileSelectListener;
import org.vaadin.cored.Team.TeamListener;
import org.vaadin.diffsync.Shared;

import com.github.wolfie.refresher.Refresher;
import com.vaadin.terminal.ExternalResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

@SuppressWarnings("serial")
public class IDE extends VerticalLayout implements TeamListener {
	
	private VerticalLayout editorLayout = new VerticalLayout();
	{
		editorLayout.setSizeFull();
	}
	
	private Refresher refresher = new Refresher();

	private CoredMenuBar menuBar;
	
	private final User user;
	private final Project project;
	private final Component buildComponent;

	private VerticalLayout rightBar = new VerticalLayout();
	private MarkerWidget mw;
	private ChatBox chat;
	private SharedChat sharedChat;

	private SuggestibleCollabAceEditor editor;

	private ProjectPanel projectPanel;

	private TeamPanel teamPanel;

	public IDE(User user, Project project) {
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

		rightBar.setWidth("240px");
		rightBar.setHeight("100%");

		mw = new MarkerWidget(project);
		mw.setUser(user);
		mw.setWidth("90%");
		mw.setHeight("90%");
		rightBar.addComponent(mw);
		rightBar.setComponentAlignment(mw, Alignment.TOP_CENTER);

		chat = new ChatBox(sharedChat);
		chat.setUser(user.getUserId(), user.getName(), user.getStyle());
		chat.setShowMyNick(false);
		chat.setCaption("Project-wide Chat:");
		chat.setWidth("90%");
		chat.setHeight("90%");
		rightBar.addComponent(chat);
		rightBar.setComponentAlignment(chat, Alignment.TOP_CENTER);
		
		HorizontalSplitPanel hsp = new HorizontalSplitPanel();
		hsp.addComponent(createLeftBar());
		hsp.addComponent(editorLayout);
		hsp.setSplitPosition(260, UNITS_PIXELS);

		layout.addComponent(hsp);
		layout.addComponent(rightBar);
		layout.setExpandRatio(hsp, 1);
		
		refresher.setRefreshInterval(2000);
		addComponent(refresher);
	}
	
	public Project getProject() {
		return project;
	}

	@Override
	public void attach() {
		super.attach();

		project.getTeam().addListener(this);

		projectPanel.addListener(new FileSelectListener() {
//			@Override
			public void fileSelected(ProjectFile file) {
				editFile(file);
			}
		});

		List<ProjectFile> files = project.getProjectFiles();
		if (!files.isEmpty()) {
			editFile(files.get(0));
		}

	}

	@Override
	public void detach() {
		super.detach();
		System.err.println(this + " detach");
		project.getTeam().removeListener(this);
	}
	
//	private InMemoryCompiler getJavaCompiler() {
//		if (javaCompiler==null) {
//			javaCompiler = new InMemoryCompiler();
//			if (project instanceof VaadinProject) {
//				javaCompiler.appendClassPath(((VaadinProject)project).getClasspathPath());
//			}
//		}
//		return javaCompiler;
//	}

	private void setEditorUser(User user) {
		if (editor != null) {
			editor.setEnabled(user != null);
			editor.setReadOnly(user == null);
			if (user!=null) {
				editor.setUser(user.getUserId(), user.getStyle());
			}
			project.getTeam().addUserCollabId(user, editor.getCollaboratorId());
		}
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
		if (editor != null) {
			doc.removeListener(editor);
		}
		editor = EditorUtil.createEditorFor(doc, file);
		if (file.getName().endsWith(".java")) {
			InMemoryCompiler compiler = ((VaadinProject)project).getCompiler();
			editor.setSuggester(new VaadinSuggester(compiler),//, file.getFullJavaName()),
						VaadinSuggester.DEFAULT_SHORTCUT);
		}
		
		editor.setSizeFull();
		setEditorUser(user);
		mw.listenToEditor(editor);//, doc.getValue().getMarkers());
		editorLayout.removeAllComponents();
		editorLayout.addComponent(editor);
	}

	private Component createLeftBar() {
		VerticalLayout leftBar = new VerticalLayout();

		leftBar.setSizeFull();

		Component tp = createTeamPanel();
		leftBar.addComponent(tp);
		leftBar.setComponentAlignment(tp, Alignment.MIDDLE_CENTER);
		
		Component pp = createProjectPanel();
		leftBar.addComponent(pp);
		leftBar.setComponentAlignment(pp, Alignment.MIDDLE_CENTER);
		
		if (buildComponent!=null) {
			buildComponent.setWidth("90%");
			buildComponent.setHeight("90%");
			leftBar.addComponent(buildComponent);
			leftBar.setComponentAlignment(buildComponent, Alignment.MIDDLE_CENTER);
		}

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

}
