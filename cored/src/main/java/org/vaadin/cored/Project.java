package org.vaadin.cored;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import org.vaadin.aceeditor.collab.DocDiff;
import org.vaadin.aceeditor.collab.gwt.shared.Doc;
import org.vaadin.chatbox.SharedChat;
import org.vaadin.chatbox.gwt.shared.Chat;
import org.vaadin.chatbox.gwt.shared.ChatDiff;
import org.vaadin.chatbox.gwt.shared.ChatLine;
import org.vaadin.diffsync.Shared;

public class Project {

	public interface DocListener {
		public void docCreated(ProjectFile file, long collaboratorId);
		public void docRemoved(ProjectFile file, long collaboratorId);
	}

	private LinkedList<DocListener> listeners = new LinkedList<DocListener>();

	private final String projName;

	private Map<ProjectFile, Shared<Doc, DocDiff>> files = new HashMap<ProjectFile, Shared<Doc, DocDiff>>();

	private SharedChat projectChat;
	private Team team = new Team();

	private File projectDir;

	private static File projectsRootDir;

	private HashMap<String, SharedChat> chatsByMarkerId = new HashMap<String, SharedChat>();

	
	private static HashMap<String, Project> allProjects = new HashMap<String, Project>();
	
//	private HierarchicalContainer container;

	protected Project(String name) {
		name = name.toLowerCase();
		this.projName = name;
		this.projectChat = new SharedChat();
		getProjectDir();
		readFromDisk();
	}
	
	protected static boolean addProjectIfNotExist(Project p) {
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

	public static void refreshFromDisk() {
		if (projectsRootDir == null || !projectsRootDir.exists()) {
			return;
		}

		for (File f : projectsRootDir.listFiles()) {
			String name = f.getName();
			if (!name.startsWith(".")) {
				// TODO: other kind of projects?
				VaadinProject.createProjectIfNotExist(name);
			}

		}
	}

//	public static Project createProjectIfNotExist(String name) {
//		return createProjectIfNotExist(name, null);
//	}
//
//	public static Project createProjectIfNotExist(String name,
//			VaadinProject.Type type) {
//		Project p;
//		synchronized (allProjects) {
//			p = allProjects.get(name);
//			if (p == null) {
//				p = new Project(name);
//			}
//			
//		}
//		return p;
//	}

	public void addListener(DocListener li) {
		synchronized (listeners) {
			listeners.add(li);
		}
	}

	public void removeListener(DocListener li) {
		synchronized (listeners) {
			listeners.remove(li);
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
			
			sharedDoc = new Shared<Doc, DocDiff>(doc);
			files.put(file, sharedDoc);
			fireDocCreated(file, collaboratorId);
		}

		decorateDoc(file, sharedDoc);
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
		MyFileUtils.writeFileToDisk(dest, doc.getText());
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

	private void readFromDisk() {
		System.out.println("readFromDisk");
		TreeSet<File> files = getFilesIn(getProjectDir());
		for (File f : files) {
			System.out.println("Found "+f);
			readFileFromDisk(f);
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

	// TODO: refactor?
	protected void decorateDoc(ProjectFile file, Shared<Doc, DocDiff> sharedDoc) {

	}

	private void fireDocCreated(ProjectFile file, long collaboratorId) {
		synchronized (listeners) {
			for (DocListener li : listeners) {
				li.docCreated(file, collaboratorId);
			}
		}
	}

	private void fireDocRemoved(ProjectFile file, long collaboratorId) {
		synchronized (listeners) {
			for (DocListener li : listeners) {
				li.docRemoved(file, collaboratorId);
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
			content = readFile(getLocationOfFile(file));
		} catch (IOException e) {
			return false;
		}

		synchronized (files) {
			if (files.containsKey(file)) {
				Shared<Doc, DocDiff> shared = files.get(file);
				if (file != null) {
					shared.setValue(new Doc(content), Shared.NO_COLLABORATOR_ID);
				}
			}
			else {
				if (EditorUtil.isEditableWithEditor(file)) {
					Shared<Doc, DocDiff> shared = new Shared<Doc, DocDiff>(new Doc(content));
					decorateDoc(file, shared);
					files.put(file, shared);
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

	public static String readFile(File f) throws IOException {
		StringBuilder text = new StringBuilder();
		String NL = System.getProperty("line.separator");
		Scanner scanner = new Scanner(new FileInputStream(f));
		try {
			while (scanner.hasNextLine()) {
				text.append(scanner.nextLine()).append(NL);
			}
		} finally {
			scanner.close();
		}
		return text.toString();
	}

	public void zip(File destZipFile) {
		try {
			MyFileUtils.zipDir(getProjectDir(), destZipFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}




}
