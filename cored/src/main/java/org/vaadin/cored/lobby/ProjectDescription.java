package org.vaadin.cored.lobby;

import java.util.Collection;

import org.vaadin.cored.Project;
import org.vaadin.cored.User;

public class ProjectDescription {

	public final String name;
	
	public ProjectDescription(String name) {
		this.name = name;
	}

	public String getCollaborators() {
		Project p = Project.getProject(name);
		if (p==null) {
			return "";
		}
		else {
			return userString(p.getTeam().getUsers());
		}
	}
	
	private static String userString(Collection<User> users) {
		StringBuilder namesStr = new StringBuilder();
		boolean atLeastOne = false;
		for (User user : users) {
			if (atLeastOne) {
				namesStr.append(", ");
			}
			namesStr.append(user.getName());
			atLeastOne = true;
		}
		return namesStr.toString();
	}
}
