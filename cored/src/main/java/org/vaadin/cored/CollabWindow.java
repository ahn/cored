package org.vaadin.cored;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

import org.vaadin.aceeditor.collab.User;
import org.vaadin.cored.LoginPanel.LoggedInCollaboratorListener;
import org.vaadin.cored.ProjectSkeletonUtil.SkeletonType;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UriFragmentUtility;
import com.vaadin.ui.UriFragmentUtility.FragmentChangedEvent;
import com.vaadin.ui.UriFragmentUtility.FragmentChangedListener;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.BaseTheme;

@SuppressWarnings("serial")
// http://dev.vaadin.com/ticket/2841
public class CollabWindow extends Window implements ProjectSelecter.Listener,
		FragmentChangedListener, LoggedInCollaboratorListener {

	private UriFragmentUtility urifu = new UriFragmentUtility();

	private VerticalLayout windowContent = new VerticalLayout();
	private VerticalLayout mainLayout = new VerticalLayout();

	private ProjectSelecter projectSelecter;

	private LoginPanel loginPanel;
	
	private CoredInfoComponent info = new CoredInfoComponent();

	public CollabWindow(String facebookAppId) {
		super();
		loginPanel = new LoginPanel(facebookAppId, "Feel free to try out Cored by logging in (any nick is ok) and creating/joining a project.");
		mainLayout.setSizeFull();
		windowContent.setSizeFull();
		windowContent.addComponent(urifu);
		windowContent.addComponent(mainLayout);
		windowContent.setExpandRatio(mainLayout, 1);
		setContent(windowContent);
	}

	@Override
	public void attach() {
		super.attach();
		urifu.addListener(this);
		loginPanel.setListener(this);
		draw("");
	}

	private void draw(String frag) {
		if (CoredApplication.getInstance().getCoredUser() == null) {
			showLoginPanel();
		} else if (frag == null || frag.isEmpty()) {
			showProjectSelecter();
		} else {
			openProject(frag);
		}
	}

	private void showLoginPanel() {
		clear();
		mainLayout.addComponent(info);
		loginPanel.setLoggedInUser(CoredApplication.getInstance()
				.getCoredUser(), false);
		mainLayout.addComponent(loginPanel);
		mainLayout.setExpandRatio(loginPanel, 1);
	}

	private void clear() {
		mainLayout.removeAllComponents();
	}

	private void showProjectSelecter() {
		Project.refreshFromDisk();

		clear();
		mainLayout.addComponent(info);

		Button logout = new Button("Log Out");
		logout.setStyleName(BaseTheme.BUTTON_LINK);
		logout.addListener(new ClickListener() {
//			@Override
			public void buttonClick(ClickEvent event) {
				//CoredApplication.getInstance().setCoredUser(null);
				loggedInCollaboratorChanged(null);
			}
		});
		mainLayout.addComponent(logout);
		
		Collection<String> projectNames;
		HashMap<String, Collection<User>> projectColls = new HashMap<String, Collection<User>>();
		projectNames = Project.getProjectNames();
		for (String pn : projectNames) {
			// FIXME: sync
			Project p = Project.getProject(pn);
			if (p != null) {
				projectColls.put(pn, p.getTeam().getUsers());
			}
		}
		projectSelecter = new ProjectSelecter(projectNames);
		projectSelecter.addListener((ProjectSelecter.Listener) this);
		for (Entry<String, Collection<User>> e : projectColls.entrySet()) {
			projectSelecter.setProjectUsers(e.getKey(), e.getValue());
		}
		mainLayout.addComponent(projectSelecter);

		VerticalLayout la = new VerticalLayout();
		final TextField tf = new TextField("Project name:");
		final CheckBox skBox = new CheckBox("Create Application Skeleton");
		skBox.setValue(true);
		Button b = new Button("Create New Project");

		b.addListener(new ClickListener() {
//			@Override
			public void buttonClick(ClickEvent event) {
				String name = ((String)tf.getValue()).toLowerCase();
				if (isValidProjectName(name)) {
					SkeletonType skel = skBox.booleanValue() ? SkeletonType.VAADIN_APP
							: null;
					Project.createProjectIfNotExist(name, skel);
					urifu.setFragment(name);
				} else {
					getWindow().showNotification("Not a valid project name.");
				}
			}
		});
		la.addComponent(tf);
		la.addComponent(skBox);
		la.addComponent(b);
		mainLayout.addComponent(la);
		mainLayout.setExpandRatio(la, 1);
	}

	private void openProject(String projectName) {
		Project project;

		project = Project.createProjectIfNotExist(projectName);
		openProject(project);

	}

	private void openProject(Project project) {
		clear();

		IDE ide = new IDE(CoredApplication.getInstance().getCoredUser(),
				project, new WarBuildComponent(project));

		mainLayout.addComponent(ide);
		mainLayout.setExpandRatio(ide, 10);
	}

	/* @Override */
	public void projectSelected(String projectName) {
		urifu.setFragment(projectName);
	}

//	@Override
	public void fragmentChanged(FragmentChangedEvent source) {
		String frag = urifu.getFragment();

		String safr = sanitizeFragment(frag);
		if (safr == null || safr.isEmpty()) {
			draw("");
		} else if (!safr.equals(frag)) {
			urifu.setFragment(safr);
		} else {
			if (CoredApplication.getInstance().getCoredUser() != null) {
				openProject(safr);
			} else {
				showLoginPanel();
			}
		}
	}

	private static String sanitizeFragment(String frag) {
		if (frag == null) {
			return null;
		}
		return frag.toLowerCase().replaceAll("[^a-z]", "");
	}

	private static boolean isValidProjectName(String s) {
		return s != null && !s.isEmpty() && sanitizeFragment(s).equals(s);
	}

//	@Override
	public void refreshRequested() {
		showProjectSelecter();
	}

//	@Override
	public void loggedInCollaboratorChanged(User user) {
		CoredApplication.getInstance().setCoredUser(user);
		draw(urifu.getFragment());
	}

}