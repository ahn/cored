package org.vaadin.cored;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.CopyOnWriteArrayList;

import org.vaadin.aceeditor.collab.User;

public class Team {

	public interface TeamListener {
		public void teamChanged();
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
				fireChange();
			} else {
				users.put(user, n + 1);
			}
		}
	}

	public synchronized void removeOneUserInstance(User user) {
		synchronized (users) {
			Integer n = users.get(user);
			if (n != null) {
				if (n == 1) {
					users.remove(user);
					fireChange();
				} else {
					users.put(user, n - 1);
				}
			}
		}
	}

	public synchronized void kickUser(User user) {
		synchronized (users) {
			Integer n = users.get(user);
			if (n != null) {
				users.remove(user);
				fireChange();
			}
		}
	}

	private void fireChange() {
		for (TeamListener li : listeners) {
			li.teamChanged();
		}
	}

	public Collection<User> getUsers() {
		synchronized (users) {
			return new HashSet<User>(users.keySet());
		}
	}

	public boolean hasUser(User user) {
		synchronized (users) {

			System.err.println("hasUser " + user + " --- " + users);
			return users.containsKey(user);
		}
	}

}
