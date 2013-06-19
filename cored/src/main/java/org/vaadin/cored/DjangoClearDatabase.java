package org.vaadin.cored;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.vaadin.cored.model.DjangoProject;

import com.vaadin.terminal.UserError;
import com.vaadin.ui.Label;
import com.vaadin.ui.Window;

@SuppressWarnings("serial")
public class DjangoClearDatabase extends Window {

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
		                Label l = new Label(s);
		                addComponent(l);
		                l.setComponentError(new UserError(s));
		            }
		            
		            is.close ();    
		        } catch (Exception ex) {
		            System.out.println ("Problem reading stream " + name + "... :" + ex);
		            ex.printStackTrace ();
		        }
		    }
		}
		
	public DjangoClearDatabase(DjangoProject project) {
		
		try {
			//project.writeToDisk();
			
			String cmds = "cmd /c python manage.py cleanup";
			System.out.println("projectDir=="+project.getProjectDir()+"getName=="+project.getName());
			File djangoDir = new File(project.getProjectDir()+"\\"+project.getName());
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
			}
			this.close();
		//TODO: close the window? or change it so is not a window at all (button)
	}
}
