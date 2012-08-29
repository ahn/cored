package org.vaadin.cored;

import java.util.List;

import org.vaadin.aceeditor.collab.DocDiff;
import org.vaadin.aceeditor.collab.SuggestibleCollabAceEditor;
import org.vaadin.aceeditor.collab.User;
import org.vaadin.aceeditor.collab.gwt.shared.Doc;
import org.vaadin.chatbox.ChatBox;
import org.vaadin.chatbox.SharedChat;
import org.vaadin.cored.ProjectPanel.FileSelectListener;
import org.vaadin.cored.Team.TeamListener;
import org.vaadin.diffsync.Shared;

import com.vaadin.terminal.ExternalResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.BaseTheme;

@SuppressWarnings("serial")
public class IDE extends VerticalLayout implements TeamListener {

	private VerticalLayout editorLayout = new VerticalLayout();
	{
		editorLayout.setSizeFull();
	}

	private final User user;
	private final Project project;
	private final BuildComponent buildComponent;

	private VerticalLayout rightBar = new VerticalLayout();
	private MarkerWidget mw;
	private ChatBox chat;
	private SharedChat sharedChat;

	private SuggestibleCollabAceEditor editor;

	private ProjectPanel projectPanel;

	private AddFilePanel addFilePanel;

	private TeamPanel teamPanel;

	public IDE(User user, Project project, BuildComponent buildComponent) {
		super();
		System.err.println("new IDE(" + user + "...)");

		this.user = user;
		this.project = project;
		project.getTeam().addUser(user);
		this.buildComponent = buildComponent;
		this.sharedChat = project.getProjectChat();

		setSizeFull();
		HorizontalLayout layout = new HorizontalLayout();
		layout.setSizeFull();
		addComponent(layout);

		rightBar.setWidth("200px");

		mw = new MarkerWidget(project);
		mw.setUser(user);

		rightBar.addComponent(mw);

		chat = new ChatBox(sharedChat);
		chat.setUser(user.getUserId(), user.getName(), user.getStyle());
		chat.setShowMyNick(false);
		chat.setCaption("Project-wide Chat:");
		chat.setPollInterval(0); // XXX?
		rightBar.addComponent(chat);

		HorizontalSplitPanel hsp = new HorizontalSplitPanel();
		hsp.addComponent(createLeftBar());
		hsp.addComponent(editorLayout);
		hsp.setSplitPosition(260, UNITS_PIXELS);

		layout.addComponent(hsp);
		layout.addComponent(rightBar);
		layout.setExpandRatio(hsp, 1);

		editFile(projectPanel.getSelectedFile());
	}

	@Override
	public void attach() {
		super.attach();

		project.getTeam().addListener(this);

		projectPanel.addListener(new FileSelectListener() {
//			@Override
			public void fileSelected(String name) {
				editFile(name);
			}
		});

		List<String> files = project.getFileNames();
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

	private void setEditorUser(User user) {
		if (editor != null) {
			editor.setEnabled(user != null);
			editor.setReadOnly(user == null);
			editor.setUser(user);
		}
	}

	private void editFile(String name) {
		if (name == null) {
			editorLayout.removeAllComponents();
		} else if (EditorUtil.isEditableWithEditor(name)) {
			Shared<Doc, DocDiff> doc = project.getDoc(name);
			if (doc != null) {
				editDoc(doc, name);
			} else {
				editorLayout.removeAllComponents();
			}
		} else {
			editorLayout.removeAllComponents();
			editorLayout.addComponent(new Label(name
					+ " can't be edited here :("));
		}
	}

	private void editDoc(Shared<Doc, DocDiff> doc, String filename) {
		editor = EditorUtil.createEditorFor(doc, filename);
		editor.setSizeFull();
		setEditorUser(user);
		editor.setPollInterval(0); // XXX
		mw.listenToEditor(editor);
		editorLayout.removeAllComponents();
		editorLayout.addComponent(editor);
	}

	private Component createLeftBar() {
		VerticalLayout leftBar = new VerticalLayout();

		leftBar.setWidth("100%");

		leftBar.addComponent(createLogoutPanel());
		leftBar.addComponent(createTeamPanel());
		leftBar.addComponent(createProjectPanel());
		leftBar.addComponent(createAddFilePanel());
		leftBar.addComponent(buildComponent);

		return leftBar;
	}

	private Component createLogoutPanel() {
		Panel p = new Panel("Logged in as " + user.getName());
		VerticalLayout la = new VerticalLayout();

		Button logout = new Button("Log Out");
		logout.setStyleName(BaseTheme.BUTTON_LINK);
		logout.addListener(new ClickListener() {
//			@Override
			public void buttonClick(ClickEvent event) {
				CoredApplication.getInstance().setCoredUser(null);
				leaveIDE();
			}
		});
		la.addComponent(logout);

		Button leave = new Button("Project Menu");
		leave.setStyleName(BaseTheme.BUTTON_LINK);
		leave.addListener(new ClickListener() {
//			@Override
			public void buttonClick(ClickEvent event) {
				leaveIDE();
			}
		});
		la.addComponent(leave);

		p.setContent(la);
		return p;
	}

	private Component createTeamPanel() {
		teamPanel = new TeamPanel(project.getTeam());
		return teamPanel;
	}

	private Component createProjectPanel() {
		projectPanel = new ProjectPanel(project);
		return projectPanel;
	}

	private Component createAddFilePanel() {
		addFilePanel = new AddFilePanel(project);
		return addFilePanel;
	}

//	@Override
	public void teamChanged() {
		System.err.println("teamChanged");
		// TODO
		if (!project.getTeam().hasUser(user)) {
			leaveIDE();
		}
	}

	private void leaveIDE() {
		getWindow().open(
				new ExternalResource(CoredApplication.getInstance().getURL()));
	}

}
