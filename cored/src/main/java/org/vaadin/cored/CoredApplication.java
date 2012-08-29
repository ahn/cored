package org.vaadin.cored;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.vaadin.aceeditor.collab.User;
import org.vaadin.cored.PropertiesUtil.CoredProperties;

import com.vaadin.Application;
import com.vaadin.terminal.gwt.server.HttpServletRequestListener;
import com.vaadin.ui.Window;

@SuppressWarnings("serial")
public class CoredApplication extends Application implements
		HttpServletRequestListener {

	private static ThreadLocal<CoredApplication> currentApplication = new ThreadLocal<CoredApplication>();

	private static String facebookAppId;

	private Window mainWindow;

	private User user;

	static {
		CoredProperties props = PropertiesUtil
				.getCoredProperties("cored.properties");
		props.apply();
	}

	@Override
	public void init() {
//		setTheme("cored");
		mainWindow = new CollabWindow(facebookAppId);
		setMainWindow(mainWindow);
	}

	@Override
	public Window getWindow(String name) {
		Window w = super.getWindow(name);
		if (w == null) {
			w = new CollabWindow(facebookAppId);
			w.setName(name);
			addWindow(w);
		}
		return w;
	}

	public static void setInstance(CoredApplication application) {
		currentApplication.set(application);
	}

//	@Override
	public void onRequestStart(HttpServletRequest request,
			HttpServletResponse response) {
		CoredApplication.setInstance(this);
	}

//	@Override
	public void onRequestEnd(HttpServletRequest request,
			HttpServletResponse response) {
		currentApplication.remove();
	}

	public static CoredApplication getInstance() {
		return currentApplication.get();
	}

	public void setCoredUser(User user) {
		this.user = user;
	}

	public User getCoredUser() {
		return user;
	}

	public static void setFacebookAppId(String facebookAppId) {
		CoredApplication.facebookAppId = facebookAppId;
	}
}
