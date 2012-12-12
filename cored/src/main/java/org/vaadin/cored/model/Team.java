package org.vaadin.cored.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class Team {

	public interface TeamListener {
		public void teamChanged();
	}

	private HashSet<TeamListener> listeners = new HashSet<TeamListener>();

	synchronized public void addListener(TeamListener li) {
		listeners.add(li);
	}

	synchronized public void removeListener(TeamListener li) {
		listeners.remove(li);
	}
	
	public interface UserFileListener {
		public void userFilesChanged(Set<User> users, Set<ProjectFile> files);
	}

	private HashSet<UserFileListener> ufListeners = new HashSet<UserFileListener>();

	synchronized public void addListener(UserFileListener li) {
		ufListeners.add(li);
	}

	public void removeListener(UserFileListener li) {
		ufListeners.remove(li);
	}
	
	private HashSet<User> users = new HashSet<User>();

	// contains all the users ever added to this team
	private HashMap<String, User> allUsersById = new HashMap<String, User>();
	
	private TreeMap<User, TreeSet<ProjectFile>> userFiles = new TreeMap<User, TreeSet<ProjectFile>>();
	
	private HashMap<ProjectFile, TreeMap<User,TreeSet<Long>>> usersByFile
			= new HashMap<ProjectFile, TreeMap<User,TreeSet<Long>>>();

	private Project project;

	public Team(Project project) {
		this.project = project;
	}

	synchronized public void addUser(User user) {
		if (!users.contains(user)) {
			users.add(user);
			allUsersById.put(user.getUserId(), user);
			project.log(user.getName() + " joined");
			fireTeamChanged();
		}
	}


	synchronized public void removeUser(User user) {
		
		boolean removed = users.remove(user);
		if (!removed) {
			return;
		}
		
		HashSet<ProjectFile> files = new HashSet<ProjectFile>();
		for (Entry<ProjectFile, TreeMap<User, TreeSet<Long>>> e : usersByFile.entrySet()) {
			TreeSet<Long> collabIds = e.getValue().remove(user);
			if (collabIds!=null) {
				files.add(e.getKey());
			}
		}
		if (!files.isEmpty()) {
			project.removeCursorMarkersOf(user);
			fireUserFileChange(Collections.singleton(user), files);
		}
		
		fireTeamChanged();
		
		project.log(user.getName() + " left");
	}

	synchronized public void removeAllUsers() {
		for (User u : users) {
			removeUser(u);
		}
	}
	
	synchronized public Collection<User> getUsersByFile(ProjectFile f) {
		TreeMap<User, TreeSet<Long>> users = usersByFile.get(f);
		if (users==null) {
			return Collections.emptySet();
		}
		return new TreeSet<User>(users.keySet());
	}

	synchronized public void setUserFileOpen(ProjectFile file, User user, long collabId) {
		addFileUser(file, user, collabId);
	}
	
	private void addFileUser(ProjectFile file, User user, long collabId) {
		boolean addedFileUser = false;
		TreeMap<User, TreeSet<Long>> users = usersByFile.get(file);
		if (users==null) {
			users = new TreeMap<User, TreeSet<Long>>();
			usersByFile.put(file, users);
		}

		TreeSet<Long> collabIds = users.get(user);
		if (collabIds == null) {
			collabIds = new TreeSet<Long>();
			users.put(user, collabIds);
			addedFileUser = true;
			
			TreeSet<ProjectFile> ufs = userFiles.get(user);
			if (ufs==null) {
				ufs = new TreeSet<ProjectFile>();
				userFiles.put(user, ufs);
			}
			ufs.add(file);
		}
		
		collabIds.add(collabId);

		if (addedFileUser) {
			fireUserFileChange(Collections.singleton(user), Collections.singleton(file));
		}
	}

	synchronized public void setUserFileClosed(ProjectFile file, User user, long collabId) {
		removeFileUser(file, user, collabId);
	}
	
	private void removeFileUser(ProjectFile file, User user, long collabId) {
		boolean removedFileUser = false;
		TreeMap<User, TreeSet<Long>> users = usersByFile.get(file);
		if (users!=null) {
			TreeSet<Long> collabIds = users.get(user);
			if (collabIds!=null) {
				collabIds.remove(collabId);
				if (collabIds.isEmpty()) {
					users.remove(user);
					removedFileUser = true;
				}
			}
			if (users.isEmpty()) {
				usersByFile.remove(file);
			}
		}
		
		if (removedFileUser) {
			TreeSet<ProjectFile> ufs = userFiles.get(user);
			ufs.remove(file);
			if (ufs.isEmpty()) {
				userFiles.remove(user);
				project.removeCursorMarkersOf(user);
			}
		}
		
		if (removedFileUser) {
			fireUserFileChange(Collections.singleton(user), Collections.singleton(file));
		}
	}
	
	private void fireTeamChanged() {
		for (TeamListener li : listeners) {
			li.teamChanged();
		}
	}
	
	private void fireUserFileChange(Set<User> users, Set<ProjectFile> files) {
		for (UserFileListener li : ufListeners) {
			li.userFilesChanged(users, files);
		}
	}

	synchronized public Collection<User> getUsers() {
		return new TreeSet<User>(users);
	}

	synchronized public boolean hasUser(User user) {
		return users.contains(user);
	}

	synchronized public User getUserByIdEvenIfKicked(String userId) {
		return allUsersById.get(userId);
	}

}
