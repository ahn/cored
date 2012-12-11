package org.vaadin.cored;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.vaadin.cored.PropertiesUtil.CoredProperties;
import org.vaadin.cored.model.Project;
import org.vaadin.cored.model.User;

import com.vaadin.Application;
import com.vaadin.terminal.gwt.server.HttpServletRequestListener;
import com.vaadin.ui.Window;

@SuppressWarnings("serial")
public class CoredApplication extends Application implements
		HttpServletRequestListener {

	private static ThreadLocal<CoredApplication> currentApplication = new ThreadLocal<CoredApplication>();

	private static String facebookAppId;

	private Window mainWindow;
	
	private static void readProps() {
		CoredProperties props;
		Map<String, String> env = System.getenv();
		try {
			if (env.containsKey("CORED_PROPERTIES_FILE")) {
				String filename = env.get("CORED_PROPERTIES_FILE");
				System.err.println("Reading CoRED properties from file: "
						+ filename);
				props = PropertiesUtil.getPropertiesFromFile(filename);
			} else {
				String cbFile = "cored.properties";
				System.err.println("Reading CoRED properties from classpath: "
						+ cbFile);
				props = PropertiesUtil.getPropertiesFromClasspathFile(cbFile);
			}
			props.apply();
		} catch (IOException e) {
			System.err.println("ERROR: Failed to read properties file!");
			System.exit(1);
		}
	}
	

	@Override
	public void init() {
		System.err.println("moi");
		readProps();
		setTheme("cored");
		mainWindow = new CoredWindow(facebookAppId);
		setMainWindow(mainWindow);
	}

	@Override
	public Window getWindow(String name) {
		Window w = super.getWindow(name);
		if (w == null) {
			w = new CoredWindow(facebookAppId);
			w.setName(name);
			addWindow(w);
		}
		return w;
	}

	public static void setInstance(CoredApplication application) {
		currentApplication.set(application);
	}

	public void onRequestStart(HttpServletRequest request,
			HttpServletResponse response) {
		CoredApplication.setInstance(this);
	}

	public void onRequestEnd(HttpServletRequest request,
			HttpServletResponse response) {
		currentApplication.remove();
	}

	public static CoredApplication getInstance() {
		return currentApplication.get();
	}

	public static void setFacebookAppId(String facebookAppId) {
		CoredApplication.facebookAppId = facebookAppId;
	}
	
	@Override
	public void close() {
		User u = (User)getUser();
		if (u!=null) {
			System.err.println("Kicking "+u.getName()+" from all projects.");
			Project.kickFromAllProjects(u);
		}
		super.close();
	}
}
