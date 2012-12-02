package org.vaadin.cored;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

public class Team {

	public interface TeamListener {
		public void teamChanged(String message);
	}

	private CopyOnWriteArrayList<TeamListener> listeners = new CopyOnWriteArrayList<TeamListener>();

	public void addListener(TeamListener li) {
		listeners.add(li);
	}

	public void removeListener(TeamListener li) {
		listeners.remove(li);
	}

	private HashMap<String, User> usersById = new HashMap<String, User>();
	
	private HashMap<String, User> allUsersById = new HashMap<String, User>();
	
	// contains all the users ever added to this team
	private HashMap<Long, User> allUsersByCollabId = new HashMap<Long, User>();

	
	private HashMap<String, String> filenamesByUserId = new HashMap<String, String>();
	
	private Project project;

	public Team(Project project) {
		this.project = project;
	}
	
	public void addUserCollabId(User user, long collabId) {
		System.out.println("addUserCollabId("+user.getName()+", "+collabId+")");
		synchronized (allUsersByCollabId) {
			allUsersByCollabId.put(collabId, user);
		}
	}

	public synchronized void addUser(User user) {
		synchronized (usersById) {
			if (!usersById.containsKey(user.getUserId())) {
				usersById.put(user.getUserId(), user);
				allUsersById.put(user.getUserId(), user);
				project.log(user.getName() + " joined");
				fireChange(null);
			}
		}
	}
	
	public void kickUser(User user) {
		kickUser(user, null);
	}

	public void kickUser(User user, String message) {
		synchronized (usersById) {
			if (usersById.containsKey(user.getUserId())) {
				usersById.remove(user.getUserId());
				if (message!=null) {
					project.log(user.getName() + " left ("+message+")");
				}
				else {
					project.log(user.getName() + " left");
				}
				fireChange(message);
			}
		}
	}
	
	public void kickAll(String message) {
		synchronized (usersById) {
			for (Entry<String, User> e : usersById.entrySet()) {
				kickUser(e.getValue(), message);
			}
		}
	}
	
	public void setUserFile(User user, String filename) {
		synchronized (usersById) {
			String prev = filenamesByUserId.put(user.getUserId(), filename);
			if (!filename.equals(prev)) {
				fireChange(null);
			}
		}
	}
	
	public String getUserFile(User user) {
		synchronized (usersById) {
			return filenamesByUserId.get(user.getUserId());
		}
	}

	private void fireChange(String message) {
		for (TeamListener li : listeners) {
			li.teamChanged(message);
		}
	}

	public Collection<User> getUsers() {
		synchronized (usersById) {
			return new HashSet<User>(usersById.values());
		}
	}

	public boolean hasUser(User user) {
		synchronized (usersById) {
			return usersById.containsKey(user.getUserId());
		}
	}
	
	public User getUserByCollabId(long collabId) {
		synchronized (allUsersByCollabId) {
			return allUsersByCollabId.get(collabId);
		}
	}

	public User getUserById(String userId) {
		return usersById.get(userId);
	}

	public User getUserByIdEvenIfKicked(String userId) {
		return allUsersById.get(userId);
	}

}
