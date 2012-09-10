package org.vaadin.cored;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.Receiver;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.Upload.SucceededListener;
import com.vaadin.ui.Window.Notification;

@SuppressWarnings("serial")
public class AddFilePanel extends Panel implements SucceededListener, Receiver,
		ClickListener {

	private VerticalLayout layout = new VerticalLayout();
	private TextField newFileField = new TextField();
	private Button newFileButton = new Button("Create File");
	private Upload upload = new Upload("Upload file", this);
	private File uploadingToFile;
	private Project project;

	public AddFilePanel(Project project) {
		super("Add File");
		this.project = project;
		HorizontalLayout hl = new HorizontalLayout();
		hl.addComponent(newFileField);
		hl.addComponent(newFileButton);
		layout.addComponent(hl);
		layout.addComponent(upload);
		setContent(layout);
	}

	@Override
	public void attach() {
		super.attach();
		newFileButton.addListener(this);
		upload.addListener(this);
	}

//	@Override
	public OutputStream receiveUpload(String filename, String mimeType) {
		try {
			uploadingToFile = File.createTempFile("temp", filename);
			System.err.println("UPLOADING TO " + uploadingToFile);
			return new FileOutputStream(uploadingToFile);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

//	@Override
	public void uploadSucceeded(SucceededEvent event) {
		if (uploadingToFile == null) {
			return;
		}

		// TODO XXX
//		project.addFile(new File(event.getFilename()), uploadingToFile);

		uploadingToFile = null;
	}

//	@Override
	public void buttonClick(ClickEvent event) {
		// TODO XXX
//		String filename = (String) newFileField.getValue();
//		if (properFilename(filename)) {
//			project.createDoc(filename);
//			newFileField.setValue("");
//		} else {
//			getWindow().showNotification("Not a valid filename",
//					Notification.TYPE_WARNING_MESSAGE);
//		}

	}

	private static boolean properFilename(String filename) {
		if (filename == null || filename.isEmpty()) {
			return false;
		}
		return filename.equals(filename.replaceAll("[^\\w._]", ""));
	}
}
