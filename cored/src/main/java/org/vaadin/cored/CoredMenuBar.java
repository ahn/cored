package org.vaadin.cored;

import java.io.File;
import java.io.IOException;

import org.vaadin.cored.model.Project;

import com.vaadin.terminal.FileResource;
import com.vaadin.ui.MenuBar;

@SuppressWarnings("serial")
public class CoredMenuBar extends MenuBar {
	
	private MenuItem projectItem;
	
	private MenuItem userItem;
	
	private final IDE ide;
	private final Project project;
	private final BuildComponent buildComponent;
	
	public CoredMenuBar() {
		this(null);
	}
	
	public CoredMenuBar(IDE ide) {
		super();
		this.ide = ide;
		this.project = ide.getProject();
		this.buildComponent = ide.getBuildComponent();
		setWidth("100%");
		
		if (project!=null){
			projectItem = addItem("Project", null);
			projectItem.setIcon(Icons.BOX);
			if (buildComponent!=null) {
				projectItem.addItem("Deploy", new BuildCommand()).setIcon(Icons.PAPER_PLANE);
			}
			projectItem.addItem("Download as zip", new DownloadCommand()).setIcon(Icons.BOX_ZIPPER);
//			projectItem.addItem("Project timeline", new StatsCommand()).setIcon(Icons.APPLICATION_WAVE);
			projectItem.addItem("Leave project", new CloseCommand());
		
			project.addMenuItem(this);
			
		}
		
		
		
		userItem = addItem("User", null);
		userItem.setIcon(Icons.USER_SILHOUETTE);
		userItem.addItem("Log out", new LogoutCommand());
		
	}
	
	private class DownloadCommand implements Command {
		public void menuSelected(MenuItem selectedItem) {
			File temp;
			try {
				temp = File.createTempFile("cored-"+project.getName(), ".zip");
				project.zip(temp);
				FileResource zip = new FileResource(temp, getApplication());
				getWindow().open(zip);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}
	
	private class BuildCommand implements Command {
		public void menuSelected(MenuItem selectedItem) {
			buildComponent.build();
		}
	}
	
	private class CloseCommand implements Command {
		public void menuSelected(MenuItem selectedItem) {
			ide.leaveProject();
		}
	}
	
	private class LogoutCommand implements Command {
		public void menuSelected(MenuItem selectedItem) {
			ide.logout();
		}
	}
	
	private class StatsCommand implements Command {
		public void menuSelected(MenuItem selectedItem) {
//			StatsWindow win = new StatsWindow(ide.getProject());
//			getWindow().addWindow(win);
		}
	}
}
