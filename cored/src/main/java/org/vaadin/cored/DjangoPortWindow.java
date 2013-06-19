package org.vaadin.cored;



import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;

import org.vaadin.cored.DjangoNewAppWindow.ReadStream;
import org.vaadin.cored.model.DjangoProject;

import com.vaadin.data.Property;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Button.ClickListener;

@SuppressWarnings("serial")
public class DjangoPortWindow extends Window implements
ClickListener {
	
	/**
	 * 
	 */

	private final DjangoProject project;
	private Button runButton;
	private Button stopButton;
	private TextField portField;
	private String value;
	private File djangoDir;
	private Process proc = null;
	private Process procc = null;
	private Runtime rt,rtt;
	
	public DjangoPortWindow(DjangoProject project) {
		super("Port to run " + project.getName());
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
		
		portField = new TextField("Port number:");
		portField.setWidth("50");
		portField.focus();
		portField.addListener(new Property.ValueChangeListener() {
			public void valueChange(com.vaadin.data.Property.ValueChangeEvent event)	{
				value = (String) portField.getValue();
				portField.setValue(value);
			}
		});
		//portField.setEnabled(portField.isEnabled());
		addComponent(portField);
		
		runButton = new Button ("Runserver");		
		runButton.addListener(this);
		addComponent(runButton);
		stopButton = new Button ("Stop");		
		stopButton.addListener(this);
		
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
		if (event.getButton() == runButton)
		{
			try {
				String cmds = "cmd /c python manage.py runserver 0.0.0.0:"+portField.getValue();
				djangoDir = new File(project.getProjectDir()+"\\"+project.getName());
				rt = Runtime.getRuntime();
				proc = rt.exec(cmds,null,djangoDir);
				//proc.waitFor();    
				
				portField.setEnabled(false);
				removeComponent(runButton);
				addComponent(stopButton);
				//test to get process PID
				String cmdPid = "cmd /c wmic process get commandline,processid | find \"manage.py runserver 0.0.0.0:"+portField.getValue()+"\"";
				rtt = Runtime.getRuntime();
				procc = rtt.exec(cmdPid,null,djangoDir);
				System.out.println("-------->");
				ReadStream s1 = new ReadStream("stdin", procc.getInputStream ());
				ReadStream s2 = new ReadStream("stderr", procc.getErrorStream ());
				s1.start ();
				s2.start ();
				//proc.waitFor();
				
			} catch (Exception e) {  
				e.printStackTrace();
			}
		}
		if (event.getButton() == stopButton)
		{
			if(rt != null)
			{
				try {
					//wmic Path win32_process Where "CommandLine Like '%-jar selenium-server.jar%'" Call Terminate
					//TODO: try not to kill all of them
					String cmds = "cmd /c taskkill /im python.exe /f";
					//rt = Runtime.getRuntime();
					rt.exec(cmds,null,djangoDir);
					proc.waitFor();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				proc.destroy();
				portField.setEnabled(true);
				removeComponent(stopButton);
				addComponent(runButton);
			}
		}
	}

}
