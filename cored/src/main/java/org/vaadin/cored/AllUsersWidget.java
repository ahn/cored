package org.vaadin.cored;

import java.util.Collection;

import org.vaadin.cored.model.Team;
import org.vaadin.cored.model.User;
import org.vaadin.cored.model.Team.TeamListener;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Button.ClickEvent;

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
	
	@Override
	public void detach() {
		super.detach();
		team.removeListener(this);
	}

	private void drawUsers() {
		layout.removeAllComponents();
		Collection<User> users = team.getUsers();
		for (final User u : users) {
			UserWidget uw = new UserWidget(u, true);
			uw.getKickButton().addListener(new ClickListener() {
				public void buttonClick(ClickEvent event) {
					team.removeUser(u);
				}
			});
			layout.addComponent(uw);
			layout.setComponentAlignment(uw, Alignment.BOTTOM_CENTER);
		}
	}

	public void teamChanged() {
		// TODO
		// "always synchronize on the application instance when accessing
		// Vaadin UI components or related data from another thread."
		// https://vaadin.com/forum/-/message_boards/view_message/1785789#_19_message_212956
		// Is this enough of synchronization?
		synchronized(getApplication()) {
			drawUsers();
		}
	}

}
