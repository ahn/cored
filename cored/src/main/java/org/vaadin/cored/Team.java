package org.vaadin.cored;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;

public class Team {

	public interface TeamListener {
		public void teamChanged(String message);
	}

	private HashSet<TeamListener> listeners = new HashSet<TeamListener>();

	public void addListener(TeamListener li) {
		listeners.add(li);
	}

	public void removeListener(TeamListener li) {
		listeners.remove(li);
	}
	
	public interface UserFileListener {
		public void userFilesChanged(Set<User> users, Set<ProjectFile> files);
	}

	private HashSet<UserFileListener> ufListeners = new HashSet<UserFileListener>();

	public void addListener(UserFileListener li) {
		ufListeners.add(li);
	}

	public void removeListener(UserFileListener li) {
		ufListeners.remove(li);
	}

	private HashMap<String, User> usersById = new HashMap<String, User>();

	// contains all the users ever added to this team
	private HashMap<String, User> allUsersById = new HashMap<String, User>();

	// contains all the users ever added to this team
	private HashMap<Long, User> allUsersByCollabId = new HashMap<Long, User>();

	private HashMap<String, TreeSet<ProjectFile>> filesByUserId = new HashMap<String, TreeSet<ProjectFile>>();
	
	private HashMap<ProjectFile, TreeMap<User,TreeSet<Long>>> usersByFile
			= new HashMap<ProjectFile, TreeMap<User,TreeSet<Long>>>();

	private Project project;

	public Team(Project project) {
		this.project = project;
	}

	synchronized public void addUser(User user) {
		if (!usersById.containsKey(user.getUserId())) {
			usersById.put(user.getUserId(), user);
			allUsersById.put(user.getUserId(), user);
			project.log(user.getName() + " joined");
			fireChange(null);
		}
	}

	public void kickUser(User user) {
		kickUser(user, null);
	}

	synchronized public void kickUser(User user, String message) {
		if (usersById.containsKey(user.getUserId())) {
			usersById.remove(user.getUserId());
			if (message != null) {
				project.log(user.getName() + " left (" + message + ")");
			} else {
				project.log(user.getName() + " left");
			}
			fireChange(message);
		}
	}

	synchronized public void kickAll(String message) {
		for (Entry<String, User> e : usersById.entrySet()) {
			kickUser(e.getValue(), message);
		}
	}
	
	synchronized public Collection<User> getUsersByFile(ProjectFile f) {
		TreeMap<User, TreeSet<Long>> users = usersByFile.get(f);
		if (users==null) {
			return Collections.emptySet();
		}
		return Collections.unmodifiableCollection(users.keySet());
	}

	synchronized public void setUserFileOpen(ProjectFile file, User user, long collabId) {
		System.out.println("OPEN " + file + " --- " + user);
		boolean added = addFileUser(file, user, collabId);
		
		if (added) {
			fireUserFileChange(Collections.singleton(user), Collections.singleton(file));
		}
	}
	
	private boolean addFileUser(ProjectFile file, User user, long collabId) {
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
		}
		
		collabIds.add(collabId);
		
		return addedFileUser;
	}

	synchronized public void setUserFileClosed(ProjectFile file, User user, long collabId) {
		System.out.println("CLOSE " + file + " --- " + user);
		boolean removed = removeFileUser(file, user, collabId);
		
		if (removed) {
			fireUserFileChange(Collections.singleton(user), Collections.singleton(file));
		}
	}
	
	private boolean removeFileUser(ProjectFile file, User user, long collabId) {
		boolean removed = false;
		TreeMap<User, TreeSet<Long>> users = usersByFile.get(file);
		if (users!=null) {
			TreeSet<Long> collabIds = users.get(user);
			if (collabIds!=null) {
				collabIds.remove(collabId);
				if (collabIds.isEmpty()) {
					users.remove(user);
					removed = true;
				}
			}
			if (users.isEmpty()) {
				usersByFile.remove(file);
			}
		}
		return removed;
	}
	
	private boolean removeUserFile(User user, ProjectFile file) {
		TreeSet<ProjectFile> files = filesByUserId.get(user.getUserId());
		if (files!=null) {
			return files.remove(file);
		}
		return false;
	}

//	synchronized public void setUserFileClosedAll(User user) {
//		TreeSet<ProjectFile> removed = filesByUserId.remove(user.getUserId());
//		for (TreeSet<User> users : usersByFile.values()) {
//			users.remove(user);
//		}
//		if (removed!=null) {
//			fireUserFileChange(Collections.singleton(user), removed);
//		}
//	}

	// synchronized Collection<ProjectFile> getFilesOpenBy(User user) {
	// return filesByUserId.get(user.getUserId());
	// }

	// synchronized public Collection<User> getUsersOfFile(ProjectFile file) {
	// return filesByUserId.get(user.getUserId());
	// }

	private void fireChange(String message) {
		for (TeamListener li : listeners) {
			li.teamChanged(message);
		}
	}
	
	private void fireUserFileChange(Set<User> users, Set<ProjectFile> files) {
		for (UserFileListener li : ufListeners) {
			li.userFilesChanged(users, files);
		}
	}

	synchronized public Collection<User> getUsers() {
		return new HashSet<User>(usersById.values());
	}

	synchronized public boolean hasUser(User user) {
		return usersById.containsKey(user.getUserId());
	}

	synchronized public User getUserById(String userId) {
		return usersById.get(userId);
	}

	synchronized public User getUserByIdEvenIfKicked(String userId) {
		return allUsersById.get(userId);
	}

}
