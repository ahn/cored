package org.vaadin.cored;

import org.vaadin.aceeditor.collab.User;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

// FIXME: facebook login temporarily disabled

@SuppressWarnings("serial")
public class LoginPanel extends Panel {//implements LoginListener {

	// private VerticalLayout fbLoginLayout = new VerticalLayout();
//	private FacebookLogin fbLogin;
	// private VerticalLayout simpleLoginLayout = new VerticalLayout();
	private VerticalLayout loginLayout = new VerticalLayout();
	private TextField simpleLoginField;
	private String facebookAppId;
	private User user;
	private FacebookUser fbUser;
//	private FacebookLogin.LoginStatus fbLoginStatus = FacebookLogin.LoginStatus.UNKNOWN;

	private boolean wasLoggedOutOfFB = false;

	public LoginPanel() {
		this(null, "Login");
	}
	
	public LoginPanel(String facebookAppId) {
		this(facebookAppId, "Login");
	}
	
	public LoginPanel(String facebookAppId, String title) {
		super(title);
		this.facebookAppId = null; // facebookAppId; FIXME
		this.setSizeFull();
		loginLayout.setSizeFull();
		this.addComponent(loginLayout);
		drawLoggedOut();
	}

	private void drawLoggedOut() {
		loginLayout.removeAllComponents();
		if (facebookEnabled()) {
//			initFBLogin();
		} else {
			initSimpleLogin("Nick:");
		}
	}

	private void drawLoggedIn(User user) {
		loginLayout.removeAllComponents();
		loginLayout.addComponent(new Label("Logged in as " + user.getName()));
		Button logout = new Button("Log out");
		logout.addListener(new ClickListener() {
//			@Override
			public void buttonClick(ClickEvent event) {
				setLoggedInUser(null, true);
			}
		});
		loginLayout.addComponent(logout);
	}

//	private void initFBLogin() {
//		if (fbLoginStatus == FacebookLogin.LoginStatus.LOGGED_IN) {
//			final String fbId = fbLogin.getId();
//			final String fbName = fbLogin.getName();
//			loginLayout.addComponent(new Label("Facebook Login: " + fbName));
//			Button reFB = new Button("Login as " + fbName);
//			reFB.addListener(new ClickListener() {
//				/* @Override */
//				public void buttonClick(ClickEvent event) {
//					loginWithFB(fbId, fbName);
//				}
//			});
//			loginLayout.addComponent(reFB);
//			initSimpleLogin("Or type a nick:");
//		} else {
//			if (fbLogin == null) {
//				fbLogin = new FacebookLogin(facebookAppId);
//				fbLogin.setHeight("30px");
//				fbLogin.addListener(this);
//			}
//			loginLayout.addComponent(fbLogin);
//			initSimpleLogin("Or just type a nick:");
//		}
//	}

	private void initSimpleLogin(String nickText) {
		Button simpleLoginButton = new Button("Login");
		simpleLoginButton.addListener(new ClickListener() {
			/* @Override */
			public void buttonClick(ClickEvent event) {
				String nick = (String) simpleLoginField.getValue();
				if (!nick.isEmpty()) {
					setLoggedInUser(User.newUser(nick), true);
				}
			}
		});
		simpleLoginField = new TextField(nickText);
		loginLayout.addComponent(simpleLoginField);
		loginLayout.addComponent(simpleLoginButton);
	}

	private boolean facebookEnabled() {
		return facebookAppId != null;
	}

	/* @Override */
//	public void loginStatusChanged(LoginStatus newStatus) {
//		fbLoginStatus = newStatus;
//		if (newStatus == LoginStatus.LOGGED_IN) {
//			if (wasLoggedOutOfFB) {
//				loginWithFB(fbLogin.getId(), fbLogin.getName());
//			} else {
//				drawLoggedOut();
//			}
//		} else if (newStatus == LoginStatus.LOGGED_OUT) {
//			wasLoggedOutOfFB = true;
//			setLoggedInUser(null, true);
//		} else if (newStatus == LoginStatus.LOGIN_NOT_POSSIBLE) {
//			facebookAppId = null;
//			drawLoggedOut();
//		}
//
//	}

	private void loginWithFB(String fbId, String fbName) {
		if (fbUser == null || !fbUser.getFacebookId().equals(fbId)) {
			fbUser = FacebookUser.getFacebookUserOrCreate(fbId, fbName);
		}
		setLoggedInUser(fbUser, true);
	}

	interface LoggedInCollaboratorListener {
		void loggedInCollaboratorChanged(User user);
	}

	private LoggedInCollaboratorListener listener;

	public void setListener(LoggedInCollaboratorListener listener) {
		this.listener = listener;
	}

	private void fireLoggedInCollaboratorChanged(User user) {
		if (listener != null) {
			listener.loggedInCollaboratorChanged(user);
		}

	}

	public void setLoggedInUser(User user, boolean notifyListeners) {
		if (user == null && this.user != null) {
			this.user = null;
			drawLoggedOut();
			if (notifyListeners)
				fireLoggedInCollaboratorChanged(null);
		} else if (user != null
				&& (this.user == null || this.user.getUserId() != user
						.getUserId())) {
			this.user = user;
			if (user instanceof FacebookUser) {
				fbUser = (FacebookUser) user;
			}
			drawLoggedIn(user);
			if (notifyListeners)
				fireLoggedInCollaboratorChanged(this.user);
		}
	}

}
