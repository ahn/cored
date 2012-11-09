package org.vaadin.cored;

import org.vaadin.chatbox.ChatBox;
import org.vaadin.chatbox.SharedChat;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class MarkerChatBox extends CustomComponent {

	private VerticalLayout layout = new VerticalLayout();
	private SharedChat shared;
	private User user;
	private ChatBox chat;

	public MarkerChatBox(SharedChat shared, User user) {
		super();
		this.shared = shared;
		this.user = user;

		layout.setSizeFull();

		createChatBox();
		layout.addComponent(chat);

		setCompositionRoot(layout);
	}

	private void createChatBox() {
		chat = new ChatBox(shared);
		chat.setUser(user.getUserId(), user.getName(), user.getStyle());
		chat.setShowMyNick(false);
		chat.setSizeFull();
	}

}
