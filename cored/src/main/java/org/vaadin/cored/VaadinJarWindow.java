package org.vaadin.cored;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.FailedEvent;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

@SuppressWarnings("serial")
public class VaadinJarWindow extends Window implements
		Upload.SucceededListener, Upload.FailedListener, Upload.Receiver, ClickListener {

	private final VaadinProject project;
	private ListSelect jarSelect;
	private final Button removeButton = new Button("Remove");
	private final Upload jarUpload = new Upload("Upload New Jar", this);
	private boolean uploadingJar;

	public VaadinJarWindow(VaadinProject project) {
		super("Jar files of " + project.getName());
		this.project = project;
		getContent().setSizeFull();
		setWidth("400px");
		setHeight("300px");
		draw();
	}
	
	private void draw() {
		getContent().removeAllComponents();
		VerticalLayout ve = new VerticalLayout();
		ve.setSizeFull();
		addComponent(ve);

		jarSelect = new ListSelect("Jars", project.getJarNames());
		jarSelect.setNullSelectionAllowed(false);
		ve.addComponent(jarSelect);
		jarSelect.setSizeFull();

		removeButton.addListener(this);
		
		removeButton.setWidth("80%");
		ve.addComponent(removeButton);
		jarUpload.setWidth("80%");
		ve.addComponent(jarUpload);

		ve.setExpandRatio(jarSelect, 1);

		jarUpload.addListener((Upload.SucceededListener) this);
		jarUpload.addListener((Upload.FailedListener) this);
	}
	
	// XXX receiveUpload can't return null or there'll be exception i can't catch???
	// using DummyStream instead of null, seems stupid but don't know if there's better way...
	private class DummyStream extends OutputStream {
		@Override
		public void write(int b) throws IOException {
			// do nothing
		}
	}

	public OutputStream receiveUpload(String filename, String mimeType) {
		uploadingJar = false;
		if (!filename.endsWith(".jar")) {
			getWindow().showNotification("Not a .jar file",
					Notification.TYPE_WARNING_MESSAGE);
			return new DummyStream();
		}
		File f = new File(project.getJarDirAbsolutePath(), filename);
		if (f.exists()) {
			getWindow().showNotification(
					"Jar file " + filename + " already exists",
					Notification.TYPE_WARNING_MESSAGE);
			return new DummyStream();
		}

		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(f);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		uploadingJar = true;
		return fos;
	}

	public void uploadFailed(FailedEvent event) {
		
	}

	public void uploadSucceeded(SucceededEvent event) {
		if (uploadingJar) {
			project.updateJarsFromDisk();
			draw();
		}
	}

	public void buttonClick(ClickEvent event) {
		String selected = (String)jarSelect.getValue();
		if (selected==null) {
			return;
		}
		
		if (project.removeJar(selected)) {
			draw();
		}
		else {
			getWindow().showNotification(
					"Can not remove " + selected,
					Notification.TYPE_WARNING_MESSAGE);
		}
	}

}
