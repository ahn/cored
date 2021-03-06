package org.vaadin.cored.model;


import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.vaadin.aceeditor.collab.DocDiff;
import org.vaadin.aceeditor.collab.gwt.shared.Doc;
import org.vaadin.chatbox.SharedChat;
import org.vaadin.chatbox.gwt.shared.ChatDiff;
import org.vaadin.chatbox.gwt.shared.ChatLine;
import org.vaadin.cored.BuildComponent;
import org.vaadin.cored.LoggerTask;
import org.vaadin.cored.MyFileUtils;
import org.vaadin.cored.ProjectLog;
import org.vaadin.cored.PropertiesUtil;
import org.vaadin.cored.VaadinBuildComponent;
import org.vaadin.diffsync.Shared;
import org.vaadin.diffsync.Shared.Listener;

import com.vaadin.ui.MenuBar;
import com.vaadin.ui.Tree;
import com.vaadin.ui.Window;

//TODO: check synchronization

/**
 * An abstract cored project containing the project files (CoredDoc's) as well as other related things.
 * 
 * Must subclassed to create projects of various types, eg. Vaadin, Python, ...
 *
 */
public abstract class Project {

	private static final Pattern VALID_PROJECT_NAME = Pattern.compile("[a-z][a-z0-9]*");
	
	private static final String PROPERTY_FILE_NAME="project.properties";
	
	private boolean logging = false;
	private final ProjectLog log;
	
	public enum ProjectType {
		vaadin, vaadinAppEngine, vaadinOSGi, python, generic;
	}
	
	public interface DocListener {
		public void docCreated(ProjectFile file);
		public void docRemoved(ProjectFile file);
	}
	
	public interface DocValueChangeListener {
		public void docValueChanged(ProjectFile pf, Doc newValue);
	}

	/**
	 * Must fill the tree (com.vaadin.ui.Tree) with the files of this project.
	 * 
	 * It's a tree so the files can be put to a hierarchy by type or something like that.
	 * 
	 */
	abstract public void fillTree(Tree tree);
	
	/**
	 * Returns a Window (com.vaadin.ui.Window) for creating a new File.
	 * 
	 * The window must have the needed functionality to actually create the file.
	 * What kind of files to create depends on the project.
	 * 
	 */
	abstract public Window createNewFileWindow();
	
	/**
	 * 
	 */
	abstract protected boolean isEditableFile(ProjectFile pf);
	
	/**
	 * Called when the project has been initialized.
	 * 
	 * Override in subclass if needed. To for example create some default files in the project
	 * at the beginning or something.
	 */
	protected void projectInitialized(boolean createSkeleton) { }
	
	
	/**
	 * Called when the project directory has been created on the disk.
	 * 
	 * Can be overridden in subclass if it needs to for example read some of the contents of
	 * files to memory. 
	 */
	// TODO: is this really needed?
	protected void projectDirCreated() { }
	
	/**
	 * If the project has something it needs to add to the IDE menu,
	 * it can override this method and add item(s) to the menubar.
	 */
	public void addMenuItem(MenuBar menuBar) { }
	
	
	
	/**
	 * Returns a build component for the project.
	 * 
	 * Implement in subclass if the project needs to be built.
	 */
	public BuildComponent createBuildComponent() {
		return null;
	}
	
	// XXX Not sure if it makes sense to use WeakReference to store listeners.
	// The idea is that storing a listener here wouldn't prevent the listener
	// to be garbage-collected.
	private CopyOnWriteArrayList<WeakReference<DocListener>> docListeners = new CopyOnWriteArrayList<WeakReference<DocListener>>();
	private CopyOnWriteArrayList<WeakReference<DocValueChangeListener>> docValueChangeListeners = new CopyOnWriteArrayList<WeakReference<DocValueChangeListener>>();

	private final String projName;

	protected Map<ProjectFile, CoredDoc> files = new HashMap<ProjectFile, CoredDoc>();

	private SharedChat projectChat;
	private Team team;

	private File projectDir;

	private static volatile File projectsRootDir;

	protected ProjectType projectType;

	private static HashMap<String, Project> allProjects = new HashMap<String, Project>();

	private static volatile File logDir;

	private LinkedList<Shared.Listener<Doc,DocDiff>> myDocValueChangeListeners = new LinkedList<Shared.Listener<Doc,DocDiff>>();
	
	protected Project(String name, ProjectType type) {
		name = name.toLowerCase();
		
		if (logDir!=null) {
			logging = true;
			log = new ProjectLog(new File(logDir, name+".log"));
		}
		else {
			logging = false;
			log = null;
		}
		
		team = new Team(this);
		
		projName = name;
		projectType = type;
		projectChat = new SharedChat();
		projectChat.addTask(new ChatLogTask(log));
				
		projectChat.applyDiff(ChatDiff.newLiveLine("Started project "+name));		
	}
	
	
	/**
	 * 
	 * @return added new project
	 */
	private static boolean addProjectIfNotExist(Project p) {
		synchronized (allProjects) {
			Project existing = allProjects.get(p.getName());
			if (existing != null) {
				return false;
			}
			else {
				allProjects.put(p.getName(), p);
				return true;
			}
		}
	}

	public static void setProjectsRootDir(String dir) {
		synchronized (allProjects) {
			projectsRootDir = new File(dir);
			projectsRootDir.mkdir(); // may already exist, that's ok.
		}
	}

	public static File getProjectsRootDir() {
		synchronized (allProjects) {
			return projectsRootDir;
		}
	}
	
	public static void setLogDir(File dir) {
		logDir = dir;
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
		synchronized (allProjects) {
			Project p = getProject(pn);
			if (p==null) {
				p = createProjectFromDisk(pn);
			}
			return p;
		}
	}
	
	static private Project createProjectFromDisk(String pn) {
		Project p = null;
		synchronized (allProjects) {
			for (File f : projectsRootDir.listFiles()) {
				if (f.isDirectory() && f.getName().equals(pn)) {
					ProjectType type = getProjectType(f);
					p = createProjectIfNotExist(pn, type, false);
					break;
				}
			}
		}
		if (p!=null) {
			p.readFromDisk();
			
			// XXX this should be somewhere else?
			if (p instanceof VaadinProject) {
				((VaadinProject)p).compileAll();
			}
		}
		return p;
	}

	public static List<String> getProjectDirNames() {
		synchronized (allProjects) {
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
		Project p;
		if (ProjectType.python.equals(type)) {
			p = new PythonProject(name);
		}
		else if (ProjectType.vaadin.equals(type)||
				ProjectType.vaadinAppEngine.equals(type)||
				ProjectType.vaadinOSGi.equals(type)){
			p = new VaadinProject(name,type);
		}
		else if (ProjectType.generic.equals(type)){
			p = new GenericProject(name);
		}
		else {
			throw new IllegalStateException("???");
		}
		boolean addedNew = addProjectIfNotExist(p);
		if (addedNew) {
			p.initializeProject(createSkeleton);
			return p;
		}
		return null;
	}

	
	private void initializeProject(boolean createSkeleton) {
		if (!readFromDisk()) {
			writePropertiesFile();
			projectInitialized(createSkeleton);
		}
		projectInitialized(false);
	}
	
	public ProjectType getProjectType() {
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
		properties.setProperty("PROJECT_TYPE", getProjectType().toString());
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
	
	
	public void addListenerWeakRef(DocListener li) {
		docListeners.add(new WeakReference<DocListener>(li));
	}

	public void removeListenerWeakRef(DocListener li) {
		for (WeakReference<DocListener> ref : docListeners) {
			if (li.equals(ref.get())) {
				docListeners.remove(ref);
				break;
			}
		}
	}
	
	public void addListenerWeakRef(DocValueChangeListener li) {
		docValueChangeListeners.add(new WeakReference<DocValueChangeListener>(li));
	}

	public void removeListenerWeakRef(DocValueChangeListener li) {
		for (WeakReference<DocValueChangeListener> ref : docValueChangeListeners) {
			if (li.equals(ref.get())) {
				docValueChangeListeners.remove(ref);
				break;
			}
		}
	}

	public String getName() {
		return projName;
	}

	synchronized public CoredDoc getDoc(ProjectFile file) {
		return files.get(file);
	}

	synchronized public List<ProjectFile> getProjectFiles() {
		return new LinkedList<ProjectFile>(files.keySet());
	}
	
	synchronized public ProjectFile getProjectFile(String filename) {
		for (ProjectFile pf : files.keySet()) {
			if (pf.getName().equals(filename)) {
				return pf;
			}
		}
		return null;
	}
	
	public CoredDoc addDoc(ProjectFile file, Doc doc) {
		CoredDoc cd;
		if (files.containsKey(file)) {
			cd = files.get(file);
			if (cd instanceof EditableCoredDoc) {
				((EditableCoredDoc)cd).setValue(doc);
				return cd;
			}
		}
		else {
			cd = addNewCoredDoc(file, doc);
		}
		return cd;
	}
	
	final protected CoredDoc addNewCoredDoc(CoredDoc cd) {
		
		ProjectFile file = cd.getProjectFile();
		if (logging) {
			log.logNewFile(file);
		}
		
		if (cd instanceof EditableCoredDoc) {
			EditableCoredDoc ecd = (EditableCoredDoc)cd;
			try {
				ecd.writeToDisk();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (logging) {
				ecd.getShared().addTask(new LoggerTask(this,file));
			}
			files.put(file, cd);
			listenTo(file, ecd.getShared());
			
		}
		
		fireDocCreated(file);
		return cd;
	}
	
	/**
	 * 
	 * @param file
	 * @param doc
	 * @return
	 */
	protected CoredDoc addNewCoredDoc(ProjectFile file, Doc doc) {
		return addNewCoredDoc(new EditableCoredDoc(getProjectDir(), file, doc, log));
		
	}

	private void listenTo(final ProjectFile file, Shared<Doc, DocDiff> sharedDoc) {
		Listener<Doc, DocDiff> li = new Listener<Doc, DocDiff>() {
			public void changed(Doc newValue, DocDiff diff, long collaboratorId) {
				fireDocValueChanged(file, newValue);
			}
		};
		myDocValueChangeListeners.add(li);
		sharedDoc.addListenerWeakRef(li);;
	}
	
	
	public void removeDoc(ProjectFile file) {
		boolean removed = false;
		synchronized (this) {
			CoredDoc cd = files.get(file);
			if (cd!=null) {
				cd.deleteFromDisk();
				files.remove(file);
				removed = true;
			}
		}
		if (removed) {
			if (logging) {
				log.logRemoveFile(file);
			}
			fireDocRemoved(file);
		}
	}

	synchronized public SharedChat getProjectChat() {
		return projectChat;
	}

	synchronized public Team getTeam() {
		return team;
	}

	synchronized public void writeToDisk() throws IOException {
		for (Entry<ProjectFile, CoredDoc> e : files.entrySet()) {
			if (e.getValue() instanceof EditableCoredDoc) {
				((EditableCoredDoc)e.getValue()).writeToDisk();
			}
		}
	}
	
	
	public void log(String text) {
		getProjectChat().applyDiff(ChatDiff.newLiveLine(new ChatLine(text)));
	}

	private boolean readFromDisk() {
		TreeSet<File> files = getFilesIn(getProjectDir());
		boolean isProjectDir = false;
		for (File f : files) {
			if (f.getName().equals(PROPERTY_FILE_NAME)) {
				isProjectDir = true;
			}
			else {
				String rel = MyFileUtils.relativizePath(getProjectDir(), f);
				ProjectFile pf = new ProjectFile(rel);
				if (isEditableFile(pf)) {
					try {
						Doc doc = CoredDoc.fromDisk(getProjectDir(), pf);
						addNewCoredDoc(pf, doc);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			}
		}
		return isProjectDir;
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


	private void fireDocCreated(ProjectFile file) {
		boolean cleanup = false;
		for (WeakReference<DocListener> ref : docListeners) {
			DocListener li = ref.get();
			if (li == null) {
				cleanup = true;
			}
			else {
				li.docCreated(file);
			}
		}
		if (cleanup) {
			cleanupDocListeners();
		}
	}

	private void fireDocRemoved(ProjectFile file) {
		boolean cleanup = false;
		for (WeakReference<DocListener> ref : docListeners) {
			DocListener li = ref.get();
			if (li == null) {
				cleanup = true;
			}
			else {
				li.docRemoved(file);
			}
		}
		if (cleanup) {
			cleanupDocListeners();
		}
	}
	
	private void fireDocValueChanged(ProjectFile file, Doc newValue) {
		boolean cleanup = false;
		for (WeakReference<DocValueChangeListener> ref : docValueChangeListeners) {
			DocValueChangeListener li = ref.get();
			if (li == null) {
				cleanup = true;
			}
			else {
				li.docValueChanged(file, newValue);
			}
		}
		if (cleanup) {
			cleanupDocValueChangeListeners();
		}
	}
	
	private void cleanupDocListeners() {
		LinkedList<WeakReference<DocListener>> toBeRemoved = new LinkedList<WeakReference<DocListener>>();
		for (WeakReference<DocListener> ref : docListeners) {
			if (ref.get()==null) {
				toBeRemoved.add(ref);
			}
		}
		docListeners.removeAll(toBeRemoved);
	}
	
	private void cleanupDocValueChangeListeners() {
		LinkedList<WeakReference<DocValueChangeListener>> toBeRemoved = new LinkedList<WeakReference<DocValueChangeListener>>();
		for (WeakReference<DocValueChangeListener> ref : docValueChangeListeners) {
			if (ref.get()==null) {
				toBeRemoved.add(ref);
			}
		}
		docValueChangeListeners.removeAll(toBeRemoved);
	}
	
	final protected File getLocationOfFile(ProjectFile file) {
		return new File(getProjectDir(), file.getPath());
	}
	

	synchronized public File getProjectDir() {
		if (projectsRootDir == null) {
			return null;
		}
		if (projectDir == null) {
			File pf = new File(projectsRootDir, projName);
			if (pf.exists()) {
				projectDir = pf;
			}
			else if (pf.mkdir()) {
				projectDirCreated();
				projectDir = pf;
			}
		}
		return projectDir;
	}

	synchronized public void zip(File destZipFile) {
		try {
			synchronized(projectDir) {
				writeToDisk();
				MyFileUtils.zipDir(projectDir, destZipFile);
			}
		} catch (IOException e) {
			System.err.println("WARNING: zipping failed " + projectDir);
		}
	}

	public void resetFromDisk(File dir) {
		if (projectDir.equals(dir)) {
			return;
		}
		synchronized (this) {
			team.removeAllUsers();
			try {
				FileUtils.deleteDirectory(projectDir);
				FileUtils.copyDirectory(dir, projectDir);
				readFromDisk();
			} catch (IOException e) {
				System.err.println("WARNING: reseting project "+getName()+" failed");
			}
		}
	}
	
	public static boolean isValidProjectName(String s) {
		return s!=null && VALID_PROJECT_NAME.matcher(s).matches();
	}
	
	public static void kickFromAllProjects(User user) {
		synchronized(allProjects) {
			for (Project p : allProjects.values()) {
				p.getTeam().removeUser(user);
				p.removeLocksOf(user);
			}
		}
	}
	
	synchronized public void removeCursorMarkersOf(User user) {
		DocDiff diff = user.getRemoveMarkersDiff();
		for (CoredDoc cd : files.values()) {
			if (cd instanceof EditableCoredDoc) {
				((EditableCoredDoc)cd).getShared().applyDiff(diff);
			}
		}
	}
	
	synchronized public void removeLocksOf(User user) {
		for (CoredDoc cd : files.values()) {
			if (cd instanceof EditableCoredDoc) {
				((EditableCoredDoc)cd).removeLocksOf(user);
			}
			
		}
	}
	
	public boolean canBeDeleted(ProjectFile file) {
		return true;
	}
	
	synchronized public ProjectLog getLog() {
		return log;
	}
	
	public void remove() {
		synchronized (allProjects) {
			allProjects.remove(this);
		}
	}
	
	public static boolean removeProject(String projectName) {
		synchronized (allProjects) {
			Project p = allProjects.get(projectName);
			if (p != null) {
				allProjects.remove(p);
			}
			if (projectsRootDir != null) {
				File pf = new File(projectsRootDir, projectName);
				try {
					FileUtils.deleteDirectory(pf);
				} catch (IOException e) {
					System.err.println("WARNING: failed to delete dir " + pf);
					return false;
				}
			}
		}
		return true;
	}

	
	
}