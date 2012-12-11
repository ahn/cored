package org.vaadin.cored;

import org.vaadin.chatbox.ChatBox;
import org.vaadin.chatbox.SharedChat;
import org.vaadin.cored.model.User;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class MarkerChatBox extends CustomComponent {

	public MarkerChatBox(SharedChat shared, User user) {
		super();

		VerticalLayout layout = new VerticalLayout();
		layout.setSizeFull();

		ChatBox chat = new ChatBox(shared);
		chat.setUser(user.getUserId(), user.getName(), user.getStyle());
		chat.setShowMyNick(false);
		chat.setSizeFull();
		
		layout.addComponent(chat);

		setCompositionRoot(layout);
	}
}
