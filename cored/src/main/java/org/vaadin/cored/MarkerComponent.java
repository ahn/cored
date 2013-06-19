package org.vaadin.cored;

import java.util.LinkedList;

import org.vaadin.aceeditor.gwt.shared.LockMarkerData;
import org.vaadin.aceeditor.gwt.shared.Marker;
import org.vaadin.chatbox.ChatBox;
import org.vaadin.chatbox.SharedChat;
import org.vaadin.cored.model.User;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.BaseTheme;

@SuppressWarnings("serial")
public class MarkerComponent extends VerticalLayout {

	public interface MarkerComponentListener {
		void removeMarker();
	}

	private ChatBox chatBox;

	final private Marker marker;
	final private User user;
	final private SharedChat chat;

	private Button removeButton;

	
	public MarkerComponent(Marker marker, User user, SharedChat chat) {
		super();
		this.marker = marker;
		this.user = user;
		this.chat = chat;
		initThis();
		setSizeFull();
	}
	
	private void initThis() {
		if (marker.getType() == Marker.Type.LOCK) {
			initLockTab();
		} else if (marker.getType() == Marker.Type.COMMENT) {
			initNoteTab();
		}
		if (chatBox != null) {
			setChatUser(chatBox, user);
		}
	}

	private void initLockTab() {
		Button rb = createRemoveButton("Unlock");
		addComponent(rb);
		setComponentAlignment(rb, Alignment.TOP_RIGHT);
		User locker = User.getUser(((LockMarkerData) marker.getData())
				.getLockerId());
		removeButton.setVisible(locker == user);
		addChatBox();
	}

	private void initNoteTab() {
		Button rb = createRemoveButton("Remove note");
		addComponent(rb);
		setComponentAlignment(rb, Alignment.TOP_RIGHT);
		addChatBox();
	}

	private Button createRemoveButton(String text) {
		removeButton = new Button(text);
		removeButton.setIcon(Icons.CROSS_SCRIPT);
		removeButton.setStyleName(BaseTheme.BUTTON_LINK);
		removeButton.addListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				fireRemoveMarker();
			}
		});
		return removeButton;
	}



	private void addChatBox() {
		chatBox = new ChatBox(chat);
		chatBox.setSizeFull();
		chatBox.setShowMyNick(true);
		addComponent(chatBox);
		setExpandRatio(chatBox, 1);
	}

	private LinkedList<MarkerComponentListener> listeners = new LinkedList<MarkerComponentListener>();

	public void addListener(MarkerComponentListener li) {
		listeners.push(li);
	}

	private void fireRemoveMarker() {
		for (MarkerComponentListener li : listeners) {
			li.removeMarker();
		}
	}

	private static void setChatUser(ChatBox cb, User user) {
		if (user == null) {
			cb.setUser(null, null, null);
		} else {
			cb.setUser(user.getUserId(), user.getName(), user.getStyle());
		}
	}
}
