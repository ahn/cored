package org.vaadin.cored;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;

import org.vaadin.aceeditor.collab.DocDiff;
import org.vaadin.aceeditor.gwt.shared.Marker;

public class User implements Comparable<User> {

	private static final Color LIGHTGREEN = new Color(144,238,144);
	private static final Color LIGHTSALMON = new Color(255,160,122);
	private static final Color LIGHTBLUE = new Color(173,216,230);	
	private static final Color LIGHTPINK = new Color(255,182,193);
	
	private static final Color[] COLORS = {LIGHTGREEN, LIGHTSALMON, LIGHTBLUE, LIGHTPINK};
	
	
	protected static HashMap<String, User> users = new HashMap<String, User>();
	private static Integer latestUserId = 0;

	private final String userId;
	private final String name;
	private final String email;
	

	protected static String newUserId() {
		synchronized (latestUserId) {
			return "" + (++latestUserId);
		}
	}

	public static User getUser(String userId) {
		synchronized (users) {
			return users.get(userId);
		}
	}

	public static User newUser(String name, String email) {
		User user = new User(newUserId(), name, email);
		synchronized (users) {
			users.put(user.getUserId(), user);
		}
		return user;
	}

	protected User(String userId, String name) {
		this(userId, name, null);
	}
	
	protected User(String userId, String name, String email) {
		this.userId = userId;
		this.name = name;
		this.email = email;
	}

	public String getUserId() {
		return userId;
	}

	public String getName() {
		return name;
	}
	
	public String getEmail() {
		return email;
	}

	public String getStyle() {
		return getStyle(userId);
	}

	public static String getStyle(String userId) {
		return "of-user-" + getStyleNumber(userId);
	}

	private static int getStyleNumber(String userId) {
		return userId.hashCode() % 4;
	}
	
	public Color getColor() {
		return getColor(userId);
	}
	
	public static Color getColor(String userId) {
		return COLORS[getStyleNumber(userId)];
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof User) {
			return ((User) other).getUserId().equals(userId);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return userId.hashCode();
	}
	
	public String getCursorMarkerId() {
		return getUserId()+"-cursor";
	}
	
	public String getSelectionMarkerId() {
		return getUserId()+"-selection";
	}
	
	public DocDiff cursorDiff(int start, int end, String text) {
		if (start==end) {
			end++;
			Marker m = Marker.newCollaboratorAceMarker(start, end, "acemarker-1 "+getStyle()+" cursor", "text", false, getUserId());
			return DocDiff.replaceMarker(getSelectionMarkerId(), getCursorMarkerId(), m, text);
		}
		else {
			Marker m = Marker.newCollaboratorAceMarker(start, end, "acemarker-1 "+getStyle(), "line", false, getUserId());
			return DocDiff.replaceMarker(getCursorMarkerId(), getSelectionMarkerId(), m, text);
		}
		
	}

	public int compareTo(User o) {
		return name.compareTo(o.name);
	}
	
	@Override
	public String toString() {
		return "["+userId+"] "+name;
	}

	public DocDiff getRemoveMarkersDiff() {
		return DocDiff.removeMarkers(Arrays.asList(new String[]{getCursorMarkerId(), getSelectionMarkerId()}));
		
	}

}
