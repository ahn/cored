package org.vaadin.cored;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.vaadin.cored.DjangoNewAppWindow.ReadStream;
import org.vaadin.cored.model.DjangoProject;

import com.vaadin.data.Property;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class DjangoSQLapp extends Window implements ClickListener {
	
	
	
	private final DjangoProject project;
	private TextField appName;
	private File djangoDir;
	private Process proc = null;
	private Runtime rt;
	private String value;
	private Button sqlAppButton;

	public DjangoSQLapp(DjangoProject project) {
		super("Name of the app");
		this.project = project;
		getContent().setSizeFull();
		setWidth("300px");
		setHeight("200px");
		draw();
	}
	
	private void draw() {

		getContent().removeAllComponents();
		VerticalLayout ve = new VerticalLayout();
		ve.setSizeFull();
		addComponent(ve);
		
		appName = new TextField("App name:");
		appName.setWidth("50");
		appName.focus();
		appName.addListener(new Property.ValueChangeListener() {
			public void valueChange(com.vaadin.data.Property.ValueChangeEvent event)	{
				value = (String) appName.getValue();
				appName.setValue(value);
			}
		});
		//portField.setEnabled(portField.isEnabled());
		addComponent(appName);
		
		sqlAppButton = new Button ("Run sql");		
		sqlAppButton.addListener(this);
		addComponent(sqlAppButton);		
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
		
	public void buttonClick(ClickEvent event) {
		try {
			project.writeToDisk();
			
			String cmds = "cmd /c python manage.py sql "+appName.getValue();
			djangoDir = new File(project.getProjectDir()+"\\"+project.getName());
			rt = Runtime.getRuntime();
			proc = rt.exec(cmds,null,djangoDir);
			ReadStream s1 = new ReadStream("stdin", proc.getInputStream ());
			ReadStream s2 = new ReadStream("stderr", proc.getErrorStream ());
			s1.start ();
			s2.start ();
			proc.waitFor();
			} catch (Exception e) {  
				e.printStackTrace();
			}
			this.close();
	}

}
