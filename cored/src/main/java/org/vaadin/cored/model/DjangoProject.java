package org.vaadin.cored.model;

import java.util.regex.Pattern;

import java.io.*;

import org.vaadin.aceeditor.collab.gwt.shared.Doc;
import org.vaadin.cored.DjangoClearDatabase;
import org.vaadin.cored.DjangoNewAppWindow;
import org.vaadin.cored.DjangoPortWindow;
import org.vaadin.cored.DjangoSQLapp;
import org.vaadin.cored.DjangoSyncDB;


import com.vaadin.data.validator.AbstractValidator;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Tree;
import com.vaadin.ui.Window;

public class DjangoProject extends Project { 
	

	public static class DjangoUtils {
		
		private static final Pattern validClass = Pattern.compile("^[A-Za-z1-9_]+$");
		
		public static boolean isValidDjangoClass(String s) {
			return validClass.matcher(s).matches();
		}

		@SuppressWarnings("serial")
		public static class DjangoClassNameValidator extends AbstractValidator {

			public DjangoClassNameValidator() {
				super("Class names should contain letters, numbers, _");
			}

			public boolean isValid(Object value) {
				return value instanceof String && isValidDjangoClass((String) value);
			}	
		}
	}
	
	protected DjangoProject(String name) {
		super(name,ProjectType.django);
	}
	//TODO: delete this?
	public class ReadStream implements Runnable {
	    String name;
	    InputStream is;
	    Thread thread;      
	    public ReadStream(String name, InputStream is) {
	        this.name = name;
	        this.is = is;
	    }       
	    public void start () {
	        thread = new Thread (this);
	        thread.start ();
	    }       
	    public void run () {
	        try {
	            InputStreamReader isr = new InputStreamReader (is);
	            BufferedReader br = new BufferedReader (isr);   
	            while (true) {
	                String s = br.readLine ();
	                if (s == null) break;
	                
	                System.out.println ("[" + name + "] " + s);
	            }
	            is.close ();    
	        } catch (Exception ex) {
	            System.out.println ("Problem reading stream " + name + "... :" + ex);
	            ex.printStackTrace ();
	        }
	    }
	}
	
	@Override
	protected void projectInitialized(boolean createSkeleton) {
		if (createSkeleton) {
			try {
				String cmds = "cmd /c django-admin.py startproject "+this.getName();
				Runtime rt = Runtime.getRuntime();
				Process proc = rt.exec(cmds,null,this.getProjectDir());
				ReadStream s1 = new ReadStream("stdin", proc.getInputStream ());
				ReadStream s2 = new ReadStream("stderr", proc.getErrorStream ());
				s1.start ();
				s2.start ();
				proc.waitFor();
				readFromDisk();
				} catch (Exception e) {  
					e.printStackTrace();  
				}
			System.out.println("project "+ this.getName() +" created");
		}
	}
	

	@Override
	public void fillTree(Tree tree) {
		String folderParentName;
		for (ProjectFile pf : super.getProjectFiles()) {
			if(pf.getParent().indexOf("\\")>0)	//it's a sub folder
			{
				tree.addItem(pf.getParent());
				folderParentName = pf.getParent().substring(0, pf.getParent().indexOf("\\"));
				tree.setParent(pf.getParent(),folderParentName);
				tree.setItemCaption(pf.getParent(),pf.getParent().substring((pf.getParent().indexOf("\\")+1),pf.getParent().length()));
			}
			else if(pf.getParent().indexOf(".")<0)	//it's a root folder
			{
				tree.addItem(pf.getParent());
				tree.setItemCaption(pf.getParent(), pf.getParent());
				tree.expandItem(pf.getParent());	//expand the root folder
			}
		}
		for (ProjectFile pf : super.getProjectFiles()) {
			if( pf.getName().endsWith(".py"))	//filter file type
			{
				tree.addItem(pf);
				tree.setParent(pf,pf.getParent());
				tree.setItemCaption(pf, pf.getName());
				tree.setChildrenAllowed(pf, false);
			}
		}
	}
	
	
	@SuppressWarnings("serial")
	private class NewPyFileWindow extends Window {
		TextField nameField = new TextField();
		private NewPyFileWindow(final DjangoProject p) {
			super("New Django File");
			HorizontalLayout h  = new HorizontalLayout();
			h.addComponent(new Label("Django file: "));
			h.addComponent(nameField);
			h.addComponent(new Label(".py"));
			Button b = new Button("Add");
			addComponent(h);
			addComponent(b);
			b.addListener(new ClickListener() {
				public void buttonClick(ClickEvent event) {
					String n = (String)nameField.getValue();
					if (!DjangoUtils.isValidDjangoClass(n)) {
						return;
					}
					p.addDoc(new ProjectFile(n+".py"), new Doc("# "+n+".py\n"));
					NewPyFileWindow.this.close();
				}
			});
		}
	}
	
	@SuppressWarnings("serial")
	@Override
	public void addMenuItem(final MenuBar menuBar) {
		MenuItem mi = menuBar.addItem("Django", null, null);
		final DjangoPortWindow runserverWindow = new DjangoPortWindow(DjangoProject.this);
		
		mi.addItem("Runserver", new Command() {
			public void menuSelected(MenuItem selectedItem) {
				//menuBar.getWindow().addWindow(new DjangoPortWindow(DjangoProject.this));
				menuBar.getWindow().addWindow(runserverWindow);
			}
		});
		mi.addItem("New app", new Command() {
			public void menuSelected(MenuItem selectedItem) {
				menuBar.getWindow().addWindow(new DjangoNewAppWindow(DjangoProject.this));
			}
		});
		mi.addItem("syncdb", new Command() {
			//this is the only one that saves the project changes
			//TODO: save button?
			public void menuSelected(MenuItem selectedItem) {
				menuBar.getWindow().addWindow(new DjangoSyncDB(DjangoProject.this));
				
				/*try {
					//project.writeToDisk();
					
					String cmds = "cmd /c python manage.py cleanup";
					System.out.println("projectDir=="+getProjectDir()+"getName=="+getName());
					File djangoDir = new File(getProjectDir()+"\\"+getName());
					System.out.println("djangoDir=="+djangoDir);
					Runtime rt = Runtime.getRuntime();
					Process proc = rt.exec(cmds,null,djangoDir);
					ReadStream s1 = new ReadStream("stdin", proc.getInputStream ());
					ReadStream s2 = new ReadStream("stderr", proc.getErrorStream ());
					s1.start ();
					s2.start ();
					//proc.waitFor();
					} catch (Exception e) {  
						e.printStackTrace();
					}*/
				
			}
		});
		mi.addItem("Run sql app", new Command() {
			public void menuSelected(MenuItem selectedItem) {
				menuBar.getWindow().addWindow(new DjangoSQLapp(DjangoProject.this));
			}
		});
		mi.addItem("Clear Database", new Command() {
			public void menuSelected(MenuItem selectedItem) {
				menuBar.getWindow().addWindow(new DjangoClearDatabase(DjangoProject.this));
			}
		});
	}


	@Override
	public Window createNewFileWindow() {
		return new NewPyFileWindow(this);
	}
	

}
