package org.vaadin.cored;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import org.vaadin.aceeditor.collab.User;

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

	private HashMap<User, Integer> users = new HashMap<User, Integer>();

	public Team() {

	}

	public synchronized void addUser(User user) {
		synchronized (users) {
			Integer n = users.get(user);
			if (n == null) {
				users.put(user, 1);
				fireChange(null);
			} else {
				users.put(user, n + 1);
			}
		}
	}

	public void removeOneUserInstance(User user) {
		synchronized (users) {
			Integer n = users.get(user);
			if (n != null) {
				if (n == 1) {
					users.remove(user);
					fireChange(null);
				} else {
					users.put(user, n - 1);
				}
			}
		}
	}
	
	public void kickUser(User user) {
		kickUser(user, null);
	}

	public void kickUser(User user, String message) {
		synchronized (users) {
			Integer n = users.get(user);
			if (n != null) {
				users.remove(user);
				fireChange(message);
			}
		}
	}
	
	public void kickAll(String message) {
		synchronized (users) {
			for (Entry<User, Integer> e : users.entrySet()) {
				if (e.getValue()!=null) {
					kickUser(e.getKey(), message);
				}
			}
		}
	}

	private void fireChange(String message) {
		for (TeamListener li : listeners) {
			li.teamChanged(message);
		}
	}

	public Collection<User> getUsers() {
		synchronized (users) {
			return new HashSet<User>(users.keySet());
		}
	}

	public boolean hasUser(User user) {
		synchronized (users) {
			return users.containsKey(user);
		}
	}

}
