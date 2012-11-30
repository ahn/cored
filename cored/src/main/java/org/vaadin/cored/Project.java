package org.vaadin.cored;


import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.vaadin.aceeditor.collab.DocDiff;
import org.vaadin.aceeditor.collab.gwt.shared.Doc;
import org.vaadin.aceeditor.gwt.shared.LockMarkerData;
import org.vaadin.aceeditor.gwt.shared.Marker;
import org.vaadin.aceeditor.java.util.InMemoryCompiler;
import org.vaadin.chatbox.SharedChat;
import org.vaadin.chatbox.gwt.shared.Chat;
import org.vaadin.chatbox.gwt.shared.ChatDiff;
import org.vaadin.chatbox.gwt.shared.ChatLine;
import org.vaadin.cored.VaadinBuildComponent.DeployType;
import org.vaadin.diffsync.DiffTask;
import org.vaadin.diffsync.Shared;

import com.vaadin.data.Validator;

public abstract class Project {

	private static final Pattern VALID_PROJECT_NAME = Pattern.compile("[a-z][a-z0-9]*");
	
	private static final String PROPERTY_FILE_NAME="project.properties";
	
	private boolean logging = true;
	private ProjectLog log = new ProjectLog();
	
	public enum ProjectType {
		vaadin, python, generic;
	}
	
	public interface DocListener {
		public void docCreated(ProjectFile file, long collaboratorId);
		public void docRemoved(ProjectFile file, long collaboratorId);
	}
	
	public interface ProjectListener {
		public void projectReset();
	}

	public abstract File getSourceDir();
	public abstract ProjectFile getFileOfClass(String className);
	public abstract String getFileEnding();
	public abstract String getPackageName();
	public abstract String getProgrammingLanguage();
	public abstract TreeSet<ProjectFile> getSourceFiles();
	public abstract Validator getClassNameValidator();
	public abstract String[] getExtendsClasses();
	public abstract String generateContent(String name, String base);
	
	/**
	 * Adds things like error checkers and other things specific to project type to the doc.
	 */
	protected void decorateDoc(ProjectFile file, Shared<Doc, DocDiff> sharedDoc) {
		// Default implementation does nothing.
	}
	
	private LinkedList<DocListener> docListeners = new LinkedList<DocListener>();
	private LinkedList<ProjectListener> projectListeners = new LinkedList<ProjectListener>();

	private final String projName;

	private Map<ProjectFile, Shared<Doc, DocDiff>> files = new HashMap<ProjectFile, Shared<Doc, DocDiff>>();

	private SharedChat projectChat;
	private Team team;

	private File projectDir;

	private static File projectsRootDir;

	private HashMap<String, SharedChat> chatsByMarkerId = new HashMap<String, SharedChat>();

	private ProjectType projectType;

	
	private static HashMap<String, Project> allProjects = new HashMap<String, Project>();
	
	protected Project(String name,ProjectType type, boolean readFromDisk) {
		name = name.toLowerCase();
		
		this.projName = name;
		this.projectType = type;
		this.projectChat = new SharedChat();
		this.projectChat.addTask(new DiffTask<Chat, ChatDiff>() {
			public ChatDiff exec(Chat value, ChatDiff diff, long collaboratorId) {
				for (ChatLine li : diff.getAddedFrozen()) {
					User u = getTeam().getUserByIdEvenIfKicked(li.getUserId());
					if (li.getUserId()!=null) {
						log.logChat(u, li.getText());
					}
				}
				for (ChatLine li : diff.getAddedLive()) {
					User u = getTeam().getUserByIdEvenIfKicked(li.getUserId());
					if (li.getUserId()!=null) {
						log.logChat(u, li.getText());
					}
				}
				return null;
			}
		});
		
		if (readFromDisk) {
			readFromDisk();
		}
		writePropertiesFile();
		
		this.team = new Team(this);
	}
	
	
	
	protected static boolean addProjectIfNotExist(Project p) {
		synchronized (allProjects) {
			Project existing = allProjects.get(p.getName());
			if (existing != null) {
				return false;
			}
			else {
				try {
					p.writeToDisk();
				} catch (IOException e) {
					System.err.println("Could not write project '"+  p.getName() + "' to disk!");
				}
				allProjects.put(p.getName(), p);
				return true;
			}
		}
	}

	public static void setProjectsRootDir(String dir) {
		projectsRootDir = new File(dir);
		projectsRootDir.mkdir(); // may already exist, that's ok.
	}

	public static File getProjectsRootDir() {
		return projectsRootDir;
	}

	public static List<String> getProjectNames() {
		synchronized (allProjects) {
			return new LinkedList<String>(allProjects.keySet());
		}
	}

	public static Project getProject(String pn) {
		synchronized (allProjects) {
			return allProjects.get(pn);
		}
	}
	
	public static Project getProjectTryDisk(String pn) {
		Project p = getProject(pn);
		if (p==null) {
			p = createProjectFromDisk(pn);
		}
		return p;
	}
	
	static private Project createProjectFromDisk(String pn) {
		synchronized(projectsRootDir) {
			for (File f : projectsRootDir.listFiles()) {
				if (f.isDirectory() && f.getName().equals(pn)) {
					ProjectType type = getProjectType(f);
					return createProjectIfNotExist(pn, type, false);
				}
			}
		}
		return null;
	}

	public static List<String> getProjectDirNames() {
		synchronized (projectsRootDir) {
			if (!projectsRootDir.exists()) {
				return Collections.emptyList();
			}

			LinkedList<String> li = new LinkedList<String>();
			for (File f : projectsRootDir.listFiles()) {
				if (f.isDirectory()) {
					li.add(f.getName());
				}
			}
			return li;
		}
	}
	
	public static Project createProjectIfNotExist(String name, ProjectType type, boolean createSkeleton) {
		if (ProjectType.python.equals(type)) {
			return PythonProject.createProjectIfNotExist(name, createSkeleton);}
		else if (ProjectType.vaadin.equals(type)){
			return VaadinProject.createProjectIfNotExist(name, createSkeleton);
		}else if (ProjectType.generic.equals(type)){
			return GenericProject.createProjectIfNotExist(name, createSkeleton);
		}
		return null;
	}

	
	private ProjectType getProjectType() {
		return this.projectType;
	}
	
	private static ProjectType getProjectType(File file) {
		class CoredPropertiesFilter implements FilenameFilter {
		    public boolean accept(File dir, String name) {
		        return (name.equals(PROPERTY_FILE_NAME));
		    }
		}
		if (file.isDirectory()){
			FilenameFilter filter = new CoredPropertiesFilter();
			File[] fs = file.listFiles(filter);
			try {
				if (fs.length==1){
					Properties properties = PropertiesUtil.getProperties(fs[0]);
					Object type = properties.get("PROJECT_TYPE");
					ProjectType projectType = ProjectType.valueOf(type.toString());
					return projectType;
				}
			} catch (Exception e) {
			}
		}
		return null;
	}
	
	private boolean writePropertiesFile() {		
		Properties properties=new Properties();
		properties.setProperty("PROJECT_NAME", getName());
		properties.setProperty("PROJECT_TYPE", projectType.toString());
		File tagFile=new File(getProjectDir(),PROPERTY_FILE_NAME);
		FileOutputStream fstream = null;
		try {
			if(!tagFile.exists()){
					tagFile.createNewFile();
			}
			fstream = new FileOutputStream(tagFile.getAbsolutePath());
			properties.store(fstream, null);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			if (fstream!=null) {
				try {
					fstream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return true;
	}
	
	
	public void addListener(DocListener li) {
		synchronized (docListeners) {
			docListeners.add(li);
		}
	}

	public void removeListener(DocListener li) {
		synchronized (docListeners) {
			docListeners.remove(li);
		}
	}

	public String getName() {
		return projName;
	}

	public Shared<Doc, DocDiff> getDoc(ProjectFile file) {
		synchronized (files) {
			return files.get(file);
		}
	}

	public List<ProjectFile> getProjectFiles() {
		synchronized (files) {
			return new LinkedList<ProjectFile>(files.keySet());
		}
	}

	public Shared<Doc, DocDiff> createDoc(ProjectFile file, Doc doc,
			long collaboratorId) {
		Shared<Doc, DocDiff> sharedDoc;
		synchronized (files) {
			if (files.containsKey(file)) {
				sharedDoc = files.get(file);
				if (sharedDoc != null) {
					sharedDoc.setValue(doc, collaboratorId);
				}
				else {
					removeFile(file);
				}
			}
			
			sharedDoc = addNewSharedDoc(file, doc);
			
			try {
				writeFileToDisk(file, doc);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			fireDocCreated(file, collaboratorId);
		}

		return sharedDoc;
	}
	
	private Shared<Doc, DocDiff> addNewSharedDoc(ProjectFile file, Doc doc) {
		Shared<Doc, DocDiff> sharedDoc = new Shared<Doc, DocDiff>(doc);
		decorateDoc(file, sharedDoc);
		if (logging) {
			sharedDoc.addTask(new LoggerTask(this,file));
		}
		files.put(file, sharedDoc);
		return sharedDoc;
	}

	public Shared<Doc, DocDiff> createDoc(ProjectFile file, String content) {
		return createDoc(file, new Doc(content), Shared.NO_COLLABORATOR_ID);
	}

	public Shared<Doc, DocDiff> createDoc(ProjectFile file) {
		return createDoc(file, new Doc(), Shared.NO_COLLABORATOR_ID);
	}

	public void removeFile(ProjectFile file) {
		synchronized (files) {
			if (files.containsKey(file)) {
				getLocationOfFile(file).delete();
				files.remove(file);
				fireDocRemoved(file, Shared.NO_COLLABORATOR_ID);
			}
		}
	}

	public boolean hasFile(String name) {
		synchronized (files) {
			return files.containsKey(name);
		}
	}

	public SharedChat getProjectChat() {
		return projectChat;
	}

	public SharedChat getMarkerChat(String markerId,
			boolean createIfNotExist, List<ChatLine> lines) {
		synchronized (chatsByMarkerId) {
			SharedChat chat = chatsByMarkerId.get(markerId);
			if (chat == null && createIfNotExist) {
				Chat ch = (lines == null ? Chat.EMPTY_CHAT : new Chat(lines));
				chat = new SharedChat(ch);
				chatsByMarkerId.put(markerId, chat);
			}
			return chat;
		}
	}

	public Shared<Chat, ChatDiff> removeMarkerChat(String markerId) {
		synchronized (chatsByMarkerId) {
			return chatsByMarkerId.remove(markerId);
		}
	}

	public Team getTeam() {
		return team;
	}

	public void writeToDisk() throws IOException {
		synchronized (files) {
			synchronized (projectDir) {
				for (Entry<ProjectFile, Shared<Doc, DocDiff>> e : files.entrySet()) {
					if (e.getValue()!=null) {
						writeFileToDisk(e.getKey(), e.getValue().getValue());
					}
				}
			}
		}
	}

	private void writeFileToDisk(ProjectFile file, Doc doc) throws IOException {
		writeFileToDisk(file, doc, getLocationOfFile(file));
	}
	
	private void writeFileToDisk(ProjectFile file, Doc doc, File dest) throws IOException {
		if (doc == null) {
			return;
		}
		FileUtils.write(dest, doc.getText());
	}

	public void addFile(ProjectFile toFile, File fromFile) {
		synchronized (projectDir) {
			boolean success = fromFile.renameTo(getLocationOfFile(toFile));
			if (success) {
				readFileFromDisk(toFile);
			}
		}
	}

	public void writeSourceFilesTo(File dir) throws IOException {
		synchronized (files) {
			synchronized (projectDir) {
				for (Entry<ProjectFile, Shared<Doc, DocDiff>> e : files.entrySet()) {
					File dest = new File(dir, e.getKey().getPath());
					writeFileToDisk(e.getKey(), e.getValue().getValue(), dest);
				}
			}
		}
	}

	public void log(String text) {
		projectChat.applyDiff(ChatDiff.newLiveLine(new ChatLine(text)),
				Shared.NO_COLLABORATOR_ID);
	}

	protected void readFromDisk() {
		System.out.println("readFromDisk");
		TreeSet<File> files = getFilesIn(getProjectDir());
		for (File f : files) {
			if (!f.getName().equals(PROPERTY_FILE_NAME)){
				System.out.println("Found "+f);
				readFileFromDisk(f);				
			}
		}
	}
	
	private TreeSet<File> getFilesIn(File dir) {
		TreeSet<File> files = new TreeSet<File>();
		for (File f : dir.listFiles()) {
			if (f.isDirectory()) {
				files.addAll(getFilesIn(f));
			}
			else {
				files.add(f);
			}
		}
		return files;
	}


	private void fireDocCreated(ProjectFile file, long collaboratorId) {
		synchronized (docListeners) {
			for (DocListener li : docListeners) {
				li.docCreated(file, collaboratorId);
			}
		}
	}

	private void fireDocRemoved(ProjectFile file, long collaboratorId) {
		synchronized (docListeners) {
			for (DocListener li : docListeners) {
				li.docRemoved(file, collaboratorId);
			}
		}
	}

	private void fireProjectReset() {
		synchronized (projectListeners) {
			for (ProjectListener li : projectListeners) {
				li.projectReset();
			}
		}
	}
	
	protected File getLocationOfFile(ProjectFile file) {
		return new File(getProjectDir(), file.getPath());
	}
	
	protected File getLocationOfFile(File file) {
		return new File(getProjectDir(), file.getPath());
	}

	private boolean readFileFromDisk(File fileFullPath) {
		File rel = MyFileUtils.relativize(getProjectDir(), fileFullPath);
		return readFileFromDisk(new ProjectFile(rel));
	}
	
	private boolean readFileFromDisk(ProjectFile file) {
		String content;
		try {
			content = FileUtils.readFileToString(getLocationOfFile(file));
		} catch (IOException e) {
			return false;
		}

		synchronized (files) {
			if (files.containsKey(file)) {
				Shared<Doc, DocDiff> shared = files.get(file);
				shared.setValue(new Doc(content), Shared.NO_COLLABORATOR_ID);
			}
			else {
				if (EditorUtil.isEditableWithEditor(file)) {
					addNewSharedDoc(file, new Doc(content));
				}
				fireDocCreated(file, Shared.NO_COLLABORATOR_ID);
			}
		}
		
		return true;
	}

	private File getProjectDir() {
		synchronized (projectsRootDir) {
			if (projectsRootDir == null) {
				return null;
			}
			if (projectDir == null) {
				File pf = new File(projectsRootDir, projName);
				if (pf.exists() || pf.mkdir()) {
					projectDir = pf;
				}
			}
			return projectDir;
		}
	}



	public void zip(File destZipFile) {
		try {
			synchronized(projectDir) {
				writeToDisk();
				MyFileUtils.zipDir(projectDir, destZipFile);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public BuildComponent getBuildComponent(DeployType deployType) {
		if (getProjectType().equals(ProjectType.vaadin)){
			return new VaadinBuildComponent((VaadinProject) this, deployType);	
		}else{
			return null;
		}
	}
	

	public void resetFromDisk(File dir) {
		synchronized (projectDir) {
			if (projectDir.equals(dir)) {
				return;
			}
			team.kickAll("Resetting project!");
			fireProjectReset();
			synchronized (files) {
				try {
					for (File f : projectDir.listFiles()) {
						System.err.println("file "+f);
					}
					FileUtils.deleteDirectory(projectDir);
					FileUtils.copyDirectory(dir, projectDir);
					readFromDisk();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	public static boolean isValidProjectName(String s) {
		return s!=null && VALID_PROJECT_NAME.matcher(s).matches();
	}
	
	public static void kickFromAllProjects(User user, String message) {
		synchronized(allProjects) {
			for (Project p : allProjects.values()) {
				p.getTeam().kickUser(user, message);
				p.removeLocksOf(user);
			}
		}
	}
	
	public void removeLocksOf(User user) {
		synchronized (files) {
			for (Shared<Doc, DocDiff> doc : files.values()) {
				for (Entry<String, Marker> e : doc.getValue().getMarkers().entrySet()) {
					Marker m = e.getValue();
					if (m.getType()==Marker.Type.LOCK &&
							user.getUserId().equals(((LockMarkerData)m.getData()).getLockerId())) {
						doc.applyDiff(DocDiff.removeMarker(e.getKey()));
					}
					
				}
			}
		}
		
	}
	protected boolean canBeDeleted(ProjectFile file) {
		return true;
	}
	
	public ProjectLog getLog() {
		return log;
	}
	
	
}