package org.vaadin.cored;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import org.vaadin.aceeditor.ErrorChecker;
import org.vaadin.aceeditor.collab.DocDiff;
import org.vaadin.aceeditor.collab.ErrorCheckTask;
import org.vaadin.aceeditor.collab.gwt.shared.Doc;
import org.vaadin.aceeditor.java.CompilerErrorChecker;
import org.vaadin.aceeditor.java.util.InMemoryCompiler;
import org.vaadin.chatbox.SharedChat;
import org.vaadin.chatbox.gwt.shared.Chat;
import org.vaadin.chatbox.gwt.shared.ChatDiff;
import org.vaadin.chatbox.gwt.shared.ChatLine;
import org.vaadin.cored.ProjectSkeletonUtil.SkeletonType;
import org.vaadin.diffsync.Shared;

public class Project {

	public interface DocListener {
		public void docCreated(String name, long collaboratorId);

		public void docRemoved(String name, long collaboratorId);
	}

	private LinkedList<DocListener> listeners = new LinkedList<DocListener>();

	private String projName;

	private Map<String, ProjectFile> files = new HashMap<String, ProjectFile>();

	private SharedChat projectChat;
	private Team team = new Team();

	private File projectDir;

	private static File projectsRootDir;

	private HashMap<String, SharedChat> chatsByMarkerId = new HashMap<String, SharedChat>();

	private static HashMap<String, Project> allProjects = new HashMap<String, Project>();

	private Project(String name) {
		this.projName = name;
		this.projectChat = new SharedChat();
		getProjectDir();
		readFromDisk();
		allProjects.put(name, this);
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
				createProjectIfNotExist(name);
			}

		}
	}

	public static Project createProjectIfNotExist(String name) {
		return createProjectIfNotExist(name, null);
	}

	public static Project createProjectIfNotExist(String name,
			SkeletonType skeletonType) {
		Project p;
		synchronized (allProjects) {
			p = allProjects.get(name);
			if (p == null) {
				p = new Project(name);
				if (skeletonType != null) {
					ProjectSkeletonUtil.applyProjectSkeletonTo(p, skeletonType);
				}
			}
		}
		return p;
	}

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

	public Shared<Doc, DocDiff> getDoc(String name) {
		synchronized (files) {
			ProjectFile pf = files.get(name);
			return pf != null ? pf.getDoc() : null;
		}
	}

	public List<String> getFileNames() {
		synchronized (files) {
			return new LinkedList<String>(files.keySet());
		}
	}

	public Shared<Doc, DocDiff> createDoc(String name, Doc doc,
			long collaboratorId) {
		Shared<Doc, DocDiff> sharedDoc;
		synchronized (files) {
			ProjectFile pf = files.get(name);
			if (pf != null) {
				if (pf.getType() == ProjectFile.Type.DOC) {
					pf.getDoc().setValue(doc, collaboratorId);
					return pf.getDoc();
				} else {
					removeFile(name);
				}
			}
			sharedDoc = new Shared<Doc, DocDiff>(doc);
			pf = new ProjectFile(sharedDoc);
			files.put(name, pf);
			fireDocCreated(name, collaboratorId);
		}

		decorateDoc(name, sharedDoc);
		return sharedDoc;
	}

	public Shared<Doc, DocDiff> createDoc(String filename, String content) {
		return createDoc(filename, new Doc(content), Shared.NO_COLLABORATOR_ID);
	}

	public Shared<Doc, DocDiff> createDoc(String filename) {
		return createDoc(filename, new Doc(), Shared.NO_COLLABORATOR_ID);
	}

	public void removeFile(String name) {
		synchronized (files) {
			ProjectFile pf = files.remove(name);
			if (pf != null) {
				getLocationOfFile(name).delete();
				fireDocRemoved(name, Shared.NO_COLLABORATOR_ID);
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
				for (Entry<String, ProjectFile> e : files.entrySet()) {
					e.getValue().writeToFile(getLocationOfFile(e.getKey()));
				}
			}
		}
	}

	public void addFile(String filename, File fromFile) {
		synchronized (projectDir) {
			boolean success = fromFile.renameTo(getLocationOfFile(filename));
			if (success) {
				readFileFromDisk(filename);
			}
		}
	}

	public void writeToDiskInNewDir(File dir) throws IOException {
		synchronized (files) {
			synchronized (projectDir) {
				for (Entry<String, ProjectFile> e : files.entrySet()) {
					File dest = new File(dir, e.getKey());
					e.getValue().writeToFile(dest);
				}
			}
		}
	}

	public void log(String text) {
		projectChat.applyDiff(ChatDiff.newLiveLine(new ChatLine(text)),
				Shared.NO_COLLABORATOR_ID);
	}

	private void readFromDisk() {
		File[] files = getProjectDir().listFiles();
		for (File f : files) {
			readFileFromDisk(f.getName());
		}
	}

	// TODO: refactor?
	private void decorateDoc(String filename, Shared<Doc, DocDiff> sharedDoc) {
		System.out.println("decoratedoc!!!");
		if (filename.endsWith(".java")) {
			ErrorChecker checker = new CompilerErrorChecker(
					new InMemoryCompiler());
			ErrorCheckTask task = new ErrorCheckTask(
					sharedDoc.newCollaboratorId(), checker);
			System.out.println("errorchecker!!!");
			sharedDoc.addAsyncTask(task, true);
		}
	}

	private void fireDocCreated(String name, long collaboratorId) {
		synchronized (listeners) {
			for (DocListener li : listeners) {
				li.docCreated(name, collaboratorId);
			}
		}
	}

	private void fireDocRemoved(String name, long collaboratorId) {
		synchronized (listeners) {
			for (DocListener li : listeners) {
				li.docRemoved(name, collaboratorId);
			}
		}
	}

	private File getLocationOfFile(String filename) {
		return new File(getProjectDir(), filename);
	}

	private boolean readFileFromDisk(String filename) {
		String content;
		try {
			content = readFile(getLocationOfFile(filename));
		} catch (IOException e) {
			return false;
		}

		synchronized (files) {
			ProjectFile pf = files.get(filename);
			if (EditorUtil.isEditableWithEditor(filename)) {
				Doc doc = new Doc(content);
				if (pf != null) {
					pf.getDoc().setValue(doc, Shared.NO_COLLABORATOR_ID);
				} else {
					Shared<Doc, DocDiff> shared = new Shared<Doc, DocDiff>(doc);
					decorateDoc(filename, shared);
					pf = new ProjectFile(shared);
					files.put(filename, pf);
					fireDocCreated(filename, Shared.NO_COLLABORATOR_ID);
				}
			} else {
				files.put(filename, new ProjectFile(filename));
				fireDocCreated(filename, Shared.NO_COLLABORATOR_ID);
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

}
