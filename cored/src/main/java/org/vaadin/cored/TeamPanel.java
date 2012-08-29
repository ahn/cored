package org.vaadin.cored;

import java.util.Collection;

import org.vaadin.aceeditor.collab.User;
import org.vaadin.cored.Team.TeamListener;

import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Panel;

@SuppressWarnings("serial")
public class TeamPanel extends Panel implements TeamListener {

	private final Team team;

	public TeamPanel(Team team) {
		super("Collaborators");
		this.team = team;
		draw();
	}

	private void draw() {
		Collection<User> newUsers = team.getUsers();
		this.removeAllComponents();

		for (final User u : newUsers) {
			UserWidget uw = new UserWidget(u);
			uw.getKickButton().addListener(new ClickListener() {
//				@Override
				public void buttonClick(ClickEvent event) {
					team.kickUser(u);
				}
			});
			uw.setWidth("90%");
			addComponent(uw);
		}
	}

	@Override
	public void attach() {
		super.attach();
		team.addListener(this);
	}

	@Override
	public void detach() {
		super.detach();
		team.removeListener(this);
	}

//	@Override
	public void teamChanged() {
		draw();
		requestRepaint();
	}

}