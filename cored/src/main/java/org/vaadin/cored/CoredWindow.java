package org.vaadin.cored;

import java.util.Collection;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.vaadin.cored.lobby.CoredInfoComponent;
import org.vaadin.cored.lobby.CreateProjectPanel;
import org.vaadin.cored.lobby.CreateProjectPanel.ProjectCreatedListener;
import org.vaadin.cored.lobby.LoginPanel;
import org.vaadin.cored.lobby.LoginPanel.LoggedInUserListener;
import org.vaadin.cored.lobby.ProjectDescription;
import org.vaadin.cored.lobby.SelectProjectPanel;
import org.vaadin.cored.lobby.UploadProjectPanel;
import org.vaadin.cored.lobby.UploadProjectPanel.ProjectUploadListener;
import org.vaadin.facebookauth.FacebookAuth;

import com.github.wolfie.refresher.Refresher;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UriFragmentUtility;
import com.vaadin.ui.UriFragmentUtility.FragmentChangedEvent;
import com.vaadin.ui.UriFragmentUtility.FragmentChangedListener;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.BaseTheme;

@SuppressWarnings("serial")
// http://dev.vaadin.com/ticket/2841
public class CoredWindow extends Window implements SelectProjectPanel.Listener,
		FragmentChangedListener, LoggedInUserListener {
	
	/**
	 * The URI fragment determines project + file:
	 * #proj
	 *     -> project called proj
	 * #proj/file.txt
	 *     -> file.txt of project proj in standalone view
	 * #proj/file.txt!
	 *     -> file.txt of project proj in project view (note the !)
	 */
	private static final Pattern FRAGMENT = Pattern.compile("^([^/]+)(?:/([^!]*))?(!?)$");

	private UriFragmentUtility urifu = new UriFragmentUtility();

	private VerticalLayout windowContent = new VerticalLayout();
	private VerticalLayout mainLayout = new VerticalLayout();

	private SelectProjectPanel projectSelecter;
	private CreateProjectPanel cpPanel;
	private UploadProjectPanel uploadPanel;

	private LoginPanel loginPanel;
	
	private CoredInfoComponent info = new CoredInfoComponent();
	
	private boolean ignoreNextFragmentChange = false;

	private User user;

	public CoredWindow(String facebookAppId) {
		super("CoRED");
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
		drawLobby();
	}

	private void drawLobby() {
		if (user == null) {
			showLoginPanel();
		} else {
			showProjectSelecter();
		}
	}

	private void showLoginPanel() {
		clear();
		mainLayout.addComponent(info);
		loginPanel.setLoggedInUser(user, false);
		mainLayout.addComponent(loginPanel);
		mainLayout.setExpandRatio(loginPanel, 1);

		mainLayout.addComponent(new Label("Some icons by <a href=\"http://p.yusukekamiyamane.com/\">Yusuke Kamiyamane</a>.", Label.CONTENT_XHTML));
	}

	private void clear() {
		mainLayout.removeAllComponents();
	}

	private void showProjectSelecter() {

		clear();

		Button logout = new Button("Log Out "+user.getName());
		logout.setStyleName(BaseTheme.BUTTON_LINK);
		logout.addListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				loggedInUserChanged(null);
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

	private void openProject(Project project, String filename) {
		clear();
		project.getTeam().addUser(user);
		Refresher ref = new Refresher();
		ref.setRefreshInterval(1000);
		mainLayout.addComponent(ref);
		IDE ide = new IDE(user, project, filename);
		mainLayout.addComponent(ide);
		mainLayout.setExpandRatio(ide, 10);
		setCaption(project.getName() + " - CoRED");
	}

	public void projectSelected(String projectName) {
		urifu.setFragment(projectName);
	}
	
	private void openSingleFile(Project project, String filename) {
		clear();
		project.getTeam().addUser(user);
		ProjectFile file = project.getProjectFile(filename);
		EditorView sfw = new EditorView(file, project, user, false);
		mainLayout.addComponent(sfw);
		mainLayout.setSizeFull();
		Refresher ref = new Refresher();
		ref.setRefreshInterval(1000);
		mainLayout.addComponent(ref);
		mainLayout.setExpandRatio(sfw, 10);
		setCaption(filename + " - " + project.getName() +" - CoRED");
	}

	public void fragmentChanged(FragmentChangedEvent source) {
		if (ignoreNextFragmentChange) {
			ignoreNextFragmentChange = false;
		}
		else {
			fragmentChanged(urifu.getFragment());
		}
	}
	
	public void fragmentChanged(String frag) {
		if (user == null) {
			showLoginPanel();
			return;
		}
		if (frag==null || frag.isEmpty()) {
			drawLobby();
			return;
		}
		
		Matcher x = FRAGMENT.matcher(urifu.getFragment());
		if (x.matches()) {
			Project project= Project.getProjectTryDisk(x.group(1));
			if (project==null) {
				showNotification("Could not open project "+x.group(1)+" :(");
				urifu.setFragment("");
			}
			else {
				String filename = x.group(2);
				boolean insideIde = !x.group(3).isEmpty();
				if (filename==null || filename.isEmpty()) {
					openProject(project, null);
				}
				else if (insideIde) {
					openProject(project, filename);
				} else {
					openSingleFile(project, filename);
				}
			}

		} else {
			urifu.setFragment("");
		}
	}

	public void refreshRequested() {
		showProjectSelecter();
	}

	public void loggedInUserChanged(User user) {
		this.user = user;
		fragmentChanged(urifu.getFragment());
	}

	public void setFragment(String frag) {
		ignoreNextFragmentChange = true;
		urifu.setFragment(frag);
	}
	
	public void close() {
		System.out.println("CoredWindow.close()");
		super.close();
	}
	
	public void logoutUser() {
		user = null;
		fragmentChanged(urifu.getFragment());
	}

}
