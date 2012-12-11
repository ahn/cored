package org.vaadin.cored;

import java.util.HashMap;

import org.vaadin.cored.model.User;

public class FacebookUser extends User {

	// Guarded by users.
	private static HashMap<String, FacebookUser> fbUsers = new HashMap<String, FacebookUser>();

	private final String fbId;

	public static FacebookUser getFacebookUserOrCreate(String facebookId,
			String name) {
		synchronized (users) {
			FacebookUser fbUser = fbUsers.get(facebookId);
			if (fbUser == null) {
				fbUser = new FacebookUser(newUserId(), name, facebookId);
				users.put(fbUser.getUserId(), fbUser);
				fbUsers.put(facebookId, fbUser);
			}
			return fbUser;
		}

	}

	private FacebookUser(String id, String name, String facebookId) {
		super(id, name);
		this.fbId = facebookId;
	}

	public String getFacebookId() {
		return fbId;
	}

	public static FacebookUser getFacebookUserByFacebookId(String facebookId) {
		synchronized (users) {
			return fbUsers.get(facebookId);
		}
	}

}
