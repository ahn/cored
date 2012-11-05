package org.vaadin.cored;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

import org.vaadin.aceeditor.collab.User;
import org.vaadin.cored.CreateProjectPanel.ProjectCreatedListener;
import org.vaadin.cored.LoginPanel.LoggedInCollaboratorListener;
import org.vaadin.cored.VaadinBuildComponent.DeployType;
import org.vaadin.facebookauth.FacebookAuth;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
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
	private CreateProjectPanel cpPanel;

	private LoginPanel loginPanel;
	
	private CoredInfoComponent info = new CoredInfoComponent();

	private final DeployType deployType= DeployType.war;

	public CollabWindow(String facebookAppId) {
		super();
		System.out.println("facebookAppId: "+facebookAppId);
		FacebookAuth fbAuth;
		if (facebookAppId != null) {
			fbAuth = new FacebookAuth(facebookAppId);
			windowContent.addComponent(fbAuth);
		}
		else {
			fbAuth = null;
		}
		loginPanel = new LoginPanel("Welcome to CoRED", fbAuth);
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

		
		Button logout = new Button("Log Out "+CoredApplication.getInstance().getCoredUser().getName());
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
		projectSelecter.setWidth("80%");
		projectSelecter.addListener((ProjectSelecter.Listener) this);
		for (Entry<String, Collection<User>> e : projectColls.entrySet()) {
			projectSelecter.setProjectUsers(e.getKey(), e.getValue());
		}
		
		HorizontalLayout hl = new HorizontalLayout();
		hl.addComponent(projectSelecter);
		hl.setComponentAlignment(projectSelecter, Alignment.TOP_CENTER);
		
		cpPanel = new CreateProjectPanel();
		cpPanel.setWidth("80%");
		hl.addComponent(cpPanel);
		hl.setComponentAlignment(cpPanel, Alignment.TOP_CENTER);
		
		cpPanel.addListener(new ProjectCreatedListener() {
			public void projectCreated(Project p) {
				urifu.setFragment(p.getName());
			}
		});
		
		hl.setWidth("100%");
		
		mainLayout.addComponent(hl);
		mainLayout.setExpandRatio(hl, 1);
	}
	

	
	private void openProject(String projectName) {
		Project project= Project.getProject(projectName);
		if (project!=null){
			openProject(Project.getProject(projectName));
		}else{
			urifu.setFragment("");
		}
	}

	private void openProject(Project project) {
		clear();
		
		BuildComponent bc = project.getBuildComponent(deployType);

		IDE ide = new IDE(CoredApplication.getInstance().getCoredUser(),
				project, bc);

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
		return frag.toLowerCase().replaceAll("[^a-z0-9]", "");
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
