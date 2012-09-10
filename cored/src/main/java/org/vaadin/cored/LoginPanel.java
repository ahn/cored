package org.vaadin.cored;

import org.vaadin.aceeditor.collab.User;
import org.vaadin.facebookauth.FacebookAuth;
import org.vaadin.facebookauth.FacebookAuth.LoginStatusListener;
import org.vaadin.facebookauth.FacebookLoginButton;
import org.vaadin.facebookauth.gwt.shared.LoginStatus;

import com.restfb.FacebookClient;
import com.restfb.DefaultFacebookClient;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class LoginPanel extends Panel implements LoginStatusListener {

	
	private FacebookLoginButton fbButton;

	private VerticalLayout loginLayout = new VerticalLayout();
	private TextField simpleLoginField;
	private FacebookAuth fbAuth;
	private User user;
	private FacebookUser fbUser;
	private LoginStatus fbLoginStatus = LoginStatus.UNKNOWN;

	private boolean wasLoggedOutOfFB = false;
	
	public LoginPanel(String title, FacebookAuth fbAuth) {
		super(title);
		this.fbAuth = fbAuth;
		this.setSizeFull();
		loginLayout.setSizeFull();
		this.addComponent(loginLayout);
		drawLoggedOut();
	}

	private void drawLoggedOut() {
		loginLayout.removeAllComponents();
		if (facebookEnabled()) {
			initFBLogin();
		} else {
			initSimpleLogin("Nick:");
		}
	}

	private void drawLoggedIn(User user) {
		loginLayout.removeAllComponents();
		loginLayout.addComponent(new Label("Logged in as " + user.getName()));
		Button logout = new Button("Log out");
		logout.addListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				setLoggedInUser(null, true);
			}
		});
		loginLayout.addComponent(logout);
	}

	private void initFBLogin() {
		if (fbLoginStatus == LoginStatus.CONNECTED) {
			final String fbId = fbAuth.getAuthResponse().userID;
			
			final String fbName = getFacebookUserName();
			
			loginLayout.addComponent(new Label("Use your Facebook account (" + fbName + ")"));
			Button reFB = new Button("Login as " + fbName);
			reFB.addListener(new ClickListener() {
				/* @Override */
				public void buttonClick(ClickEvent event) {
					loginWithFB(fbId, fbName);
				}
			});
			loginLayout.addComponent(reFB);
			initSimpleLogin("Or type a nick:");
		} else {
			if (fbButton == null) {
				fbButton = new FacebookLoginButton();
				fbAuth.addListener(this);
			}
			loginLayout.addComponent(new Label("Use your Facebook account:"));
			loginLayout.addComponent(fbButton);
			initSimpleLogin("Or just type a nick:");
		}
	}

	private void initSimpleLogin(String nickText) {
		Button simpleLoginButton = new Button("Login");
		simpleLoginButton.addListener(new ClickListener() {
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
		return fbAuth != null;
	}

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
	
	private String getFacebookUserId() {
		return fbAuth.getAuthResponse().userID;
	}
	
	private String getFacebookUserName() {
		FacebookClient client = new DefaultFacebookClient(fbAuth.getAuthResponse().accessToken);
		return client.fetchObject("me", com.restfb.types.User.class).getName();
	}

	public void loginStatusChanged(LoginStatus newStatus) {
		fbLoginStatus = newStatus;
		if (newStatus == LoginStatus.CONNECTED) {
			if (wasLoggedOutOfFB) {
				loginWithFB(getFacebookUserId(), getFacebookUserName());
			} else {
				drawLoggedOut();
			}
		} else if (newStatus == LoginStatus.LOGGED_OUT) {
			wasLoggedOutOfFB = true;
			setLoggedInUser(null, true);
		} else if (newStatus == LoginStatus.LOGIN_NOT_POSSIBLE) {
			fbAuth = null;
			drawLoggedOut();
		}

	}

}
