package org.vaadin.cored.lobby;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Properties;

import org.vaadin.cored.Icons;
import org.vaadin.cored.MyFileUtils;
import org.vaadin.cored.model.Project;
import org.vaadin.cored.model.Project.ProjectType;
import org.vaadin.cored.PropertiesUtil;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.FailedEvent;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Window.Notification;

@SuppressWarnings("serial")
public class UploadProjectPanel extends Panel implements Upload.Receiver,
		Upload.SucceededListener, Upload.FailedListener {

	public interface ProjectUploadListener {
		public void projectUploaded(Project p);
	}
	private LinkedList<ProjectUploadListener> listeners = new LinkedList<ProjectUploadListener>();
	public void addListener(ProjectUploadListener li) {
		listeners.add(li);
	}
	
	private Upload upload = new Upload("Upload project zip file", this);
	
	private File file;

	public UploadProjectPanel() {
		super("Upload Project");
		
		upload.addListener((Upload.SucceededListener) this);
		upload.addListener((Upload.FailedListener) this);
		
		setIcon(Icons.BOX_LABEL);
		
		addComponent(upload);
	}

	public OutputStream receiveUpload(String filename, String mimeType) {
		FileOutputStream fos = null;
		try {
			file = File.createTempFile("upload", ".zip");
			fos = new FileOutputStream(file);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return fos;
	}

	public void uploadFailed(FailedEvent event) {
		getWindow().showNotification("Upload failed: " + event.getReason(), Notification.TYPE_ERROR_MESSAGE);
	}

	public void uploadSucceeded(SucceededEvent event) {
		System.err.println("uploadSucceeded "+event.getFilename()+", "+event.getMIMEType());
		
		// Is this the right way to test if it's a zip file??
		if ("application/x-zip-compressed".equals(event.getMIMEType())) {
			File dir = unzip();
			if (dir!=null) {
				Properties props = projectProps(dir);
				if (props!=null) {
					createProject(props, dir);
				}
				else {
					getWindow().showNotification("The zip doesn't look like a CoRED project.", Notification.TYPE_ERROR_MESSAGE);
				}
			}
			else {
				getWindow().showNotification("Unzipping failed :(", Notification.TYPE_ERROR_MESSAGE);
			}
		}
		else {
			getWindow().showNotification("Not a zip file. The mimetype is " +event.getMIMEType(), Notification.TYPE_ERROR_MESSAGE);
		}
	}
	
	private void createProject(Properties props, File dir) {
		createProject(props.getProperty("PROJECT_NAME"),
				ProjectType.valueOf(props.getProperty("PROJECT_TYPE")), dir);
	}

	private void createProject(final String name, final ProjectType type, final File dir) {
		Project p = Project.createProjectIfNotExist(name, type, false);
		if (p==null) {
			final Window main = getWindow();
			final ConfirmResetDialog dia = new ConfirmResetDialog(name);
			dia.overwrite.addListener(new ClickListener() {
				public void buttonClick(ClickEvent event) {
					main.removeWindow(dia);
					Project p = Project.getProject(name);
					if (p==null) {
						unlucky();
					}
					else {
						p.resetFromDisk(dir);
						fireProjectUpload(p);
					}
				}
			});
//			dia.rename.addListener(new ClickListener() {
//				public void buttonClick(ClickEvent event) {
//					main.removeWindow(dia);
//					String newName = (String)dia.newName.getValue();
//					if (Project.isValidProjectName(newName)) {
//						createProject(newName, type, dir);
//					}
//					else {
//						main.showNotification("Not a valid project name.");
//					}
//				}
//			});
			main.addWindow(dia);
		}
		else {
			p.resetFromDisk(dir);
			fireProjectUpload(p);
		}
	}
	
	private void unlucky() {
		getWindow().showNotification("Something strange happened :(", Notification.TYPE_ERROR_MESSAGE);
	}

	private void fireProjectUpload(Project p) {
		for (ProjectUploadListener li : listeners) {
			li.projectUploaded(p);
		}
	}

	private File unzip() {
		File dir = null;
		try {
			dir = MyFileUtils.createTempDirectory();
			MyFileUtils.unzip(file, dir);
			System.err.println("Unzipped "+file+" to "+dir);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return dir;
	}
	
	
	private Properties projectProps(File dir) {
		File f = new File(dir, "project.properties");
		try {
			Properties props = PropertiesUtil.getProperties(f);
			if (props.containsKey("PROJECT_TYPE")) {
				System.out.println("Project: " + props.getProperty("PROJECT_TYPE") + ", " + props.getProperty("PROJECT_NAME"));
				return props;
			}
		} catch (IOException e) {
			
		}
		return null;
	}
	
	private class ConfirmResetDialog extends Window {
		
		Button cancel = new Button("Cancel");
		Button overwrite = new Button();
		
		// TODO: Allow renaming project if the name already exists.
		// For that, the packages, folders etc. must be renamed too.
		
		//TextField newName = new TextField("Use Another Name:");
		//Button rename = new Button("Create New");
		
		public ConfirmResetDialog(String name) {
			super(name +" already exists");
			setModal(true);
			setResizable(false);
			setWidth("300px");
			addComponent(new Label("Project called '"+name+"' already exists."));
			addComponent(new Label("What to do?"));
			HorizontalLayout ho = new HorizontalLayout();
			ho.addComponent(cancel);
			ho.addComponent(overwrite);
			overwrite.setCaption("Overwrite "+name);
			VerticalLayout ho2 = new VerticalLayout();
//			newName.setValue(name+"1");
//			ho2.addComponent(newName);
//			ho2.addComponent(rename);
			addComponent(ho);
			addComponent(ho2);
			cancel.addListener(new ClickListener() {
				public void buttonClick(ClickEvent event) {
					ConfirmResetDialog.this.close();
				}
			});
		}
	}
}
