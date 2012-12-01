package org.vaadin.cored;

import java.util.Collection;
import java.util.LinkedList;

import org.vaadin.cored.LoginPanel.LoggedInCollaboratorListener;
import org.vaadin.cored.lobby.CoredInfoComponent;
import org.vaadin.cored.lobby.CreateProjectPanel;
import org.vaadin.cored.lobby.CreateProjectPanel.ProjectCreatedListener;
import org.vaadin.cored.lobby.ProjectDescription;
import org.vaadin.cored.lobby.SelectProjectPanel;
import org.vaadin.cored.lobby.UploadProjectPanel;
import org.vaadin.cored.lobby.UploadProjectPanel.ProjectUploadListener;
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
public class CollabWindow extends Window implements SelectProjectPanel.Listener,
		FragmentChangedListener, LoggedInCollaboratorListener {

	private UriFragmentUtility urifu = new UriFragmentUtility();

	private VerticalLayout windowContent = new VerticalLayout();
	private VerticalLayout mainLayout = new VerticalLayout();

	private SelectProjectPanel projectSelecter;
	private CreateProjectPanel cpPanel;
	private UploadProjectPanel uploadPanel;

	private LoginPanel loginPanel;
	
	private CoredInfoComponent info = new CoredInfoComponent();

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

		clear();

		
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
		
		Collection<String> projectNames = Project.getProjectDirNames();
		LinkedList<ProjectDescription> pds = new LinkedList<ProjectDescription>();
		for (String pn : projectNames) {
			pds.add(new ProjectDescription(pn));
		}
		projectSelecter = new SelectProjectPanel(pds);
		projectSelecter.setWidth("80%");
		projectSelecter.addListener((SelectProjectPanel.Listener) this);
		
		
		HorizontalLayout hl = new HorizontalLayout();
		hl.setWidth("100%");
		hl.addComponent(projectSelecter);
		hl.setComponentAlignment(projectSelecter, Alignment.TOP_CENTER);
		
		VerticalLayout ve = new VerticalLayout();
		ve.setSizeFull();

		hl.addComponent(ve);
		
		cpPanel = new CreateProjectPanel();
		cpPanel.setWidth("80%");
		cpPanel.addListener(new ProjectCreatedListener() {
			public void projectCreated(Project p) {
				urifu.setFragment(p.getName());
			}
		});
		
		ve.addComponent(cpPanel);
		
		uploadPanel = new UploadProjectPanel();
		uploadPanel.setWidth("80%");
		uploadPanel.addListener(new ProjectUploadListener() {
			public void projectUploaded(Project p) {
				urifu.setFragment(p.getName());
			}
		});
		ve.addComponent(uploadPanel);
		
		
		
		mainLayout.addComponent(hl);
		mainLayout.setExpandRatio(hl, 1);
	}
	

	
	private void openProject(String projectName) {
		Project project= Project.getProjectTryDisk(projectName);
		if (project!=null){
			openProject(project);
		}else{
			showNotification("Could not open project "+projectName+" :(");
			urifu.setFragment("");
		}
	}

	private void openProject(Project project) {
		clear();
		
		for (ProjectFile pf : project.getProjectFiles()) {
			System.out.println("pf "+pf.getName());
		}
		

		IDE ide = new IDE(CoredApplication.getInstance().getCoredUser(),
				project);

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
