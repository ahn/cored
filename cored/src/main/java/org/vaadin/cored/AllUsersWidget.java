package org.vaadin.cored;

import java.util.Collection;

import org.vaadin.cored.Team.TeamListener;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;

@SuppressWarnings("serial")
public class AllUsersWidget extends CustomComponent implements TeamListener {
	
	private final HorizontalLayout layout = new HorizontalLayout();
	private final Team team;
	
	public AllUsersWidget(Team team) {
		super();
		this.team = team;
		layout.setSizeFull();
		setCompositionRoot(layout);
	}
	
	@Override
	public void attach() {
		super.attach();
		
		drawUsers();
		team.addListener(this);
	}

	private void drawUsers() {
		layout.removeAllComponents();
		Collection<User> users = team.getUsers();
		for (User u : users) {
			UserWidget uw = new UserWidget(u);
			layout.addComponent(uw);
			layout.setComponentAlignment(uw, Alignment.BOTTOM_CENTER);
		}
	}

	public void teamChanged(String message) {
		drawUsers();
	}

}
