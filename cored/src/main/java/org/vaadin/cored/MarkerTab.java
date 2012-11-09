package org.vaadin.cored;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.vaadin.aceeditor.gwt.shared.LockMarkerData;
import org.vaadin.aceeditor.gwt.shared.Marker;
import org.vaadin.chatbox.ChatBox;
import org.vaadin.chatbox.SharedChat;
import org.vaadin.chatbox.gwt.shared.ChatLine;

import com.vaadin.terminal.Resource;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.BaseTheme;

@SuppressWarnings("serial")
public class MarkerTab extends VerticalLayout {

	public final static ThemeResource LOCK_ICON = new ThemeResource(
			"icons/lock.png");
	public final static ThemeResource NOTE_ICON = new ThemeResource(
			"icons/light-bulb.png");

	public interface MarkerTabListener {
		void removeMarker(final String markerId);

		void scrollTo(final String markerId);
	}

	private ChatBox chatBox;

	final private String markerId;
	final private Marker marker;
	private User user;
	final private Project project;

	private HorizontalLayout buttonLayout = new HorizontalLayout();

	private Button scrollToButton;
	private Button removeButton;

	private SharedChat chat;

	public MarkerTab(String markerId, Marker marker, User user, Project project, String content) {
		super();
		this.markerId = markerId;
		this.marker = marker;
		this.user = user;
		this.project = project;
		initTab(content);
	}

	private void initTab(String content) {
		setSizeFull();
		addComponent(buttonLayout);
		buttonLayout.addComponent(createScrollToButton("_ScrollTo_"));
		if (marker.getType() == Marker.Type.LOCK) {
			initLockTab(content);
		} else if (marker.getType() == Marker.Type.NOTE) {
			initNoteTab(content);
		}
		if (chatBox != null) {
			setChatUser(chatBox, user);
		}
	}

	private void initLockTab(String content) {
		buttonLayout.addComponent(createRemoveButton("_Remove_"));
		User locker = User.getUser(((LockMarkerData) marker.getData())
				.getLockerId());
		removeButton.setVisible(locker == user);
		ChatLine line = new ChatLine(locker.getName() + " locked part of the file around '"+content+"'");
		addChatBox(line);
	}

	private void initNoteTab(String content) {
		buttonLayout.addComponent(createRemoveButton("_Remove_"));
		ChatLine line = new ChatLine("New note around '"+content+"'");
		addChatBox(line);
	}

	private Button createRemoveButton(String text) {
		removeButton = new Button(text);
		removeButton.setStyleName(BaseTheme.BUTTON_LINK);
		removeButton.addListener(new ClickListener() {
//			@Override
			public void buttonClick(ClickEvent event) {
				fireRemoveMarker();
			}
		});
		return removeButton;
	}

	private Button createScrollToButton(String text) {
		scrollToButton = new Button(text);
		scrollToButton.setStyleName(BaseTheme.BUTTON_LINK);
		scrollToButton.addListener(new ClickListener() {
//			@Override
			public void buttonClick(ClickEvent event) {
				fireScrollTo();
			}
		});
		return scrollToButton;
	}

	private void addChatBox(ChatLine firstLine) {
		List<ChatLine> lines;
		if (firstLine != null) {
			lines = Collections.singletonList(firstLine);
		} else {
			lines = Collections.emptyList();
		}
		chat = project.getMarkerChat(markerId, true, lines);
		chatBox = new ChatBox(chat);
		chatBox.setSizeFull();
		chatBox.setShowMyNick(false);
		addComponent(chatBox);
		setExpandRatio(chatBox, 1);
	}
	
//	private SharedChat.Listener<ChatDiff> chatListener;
//	public void setICEPush(final ICEPush ice) {
//		if (chatListener!=null) {
//			chat.removeListener(chatListener);
//		}
//		if (ice!=null) {
//			chatListener = new SharedChat.Listener<ChatDiff>() {
//				public void changed(ChatDiff diff, long collaboratorId) {
//					System.out.println("MarkerTab pushing! " + ice);
//					ice.push();
//				}
//			};
//			chat.addListener(chatListener);
//		}
//	}

	private LinkedList<MarkerTabListener> listeners = new LinkedList<MarkerTabListener>();

	public void addListener(MarkerTabListener li) {
		listeners.push(li);
	}

	private void fireRemoveMarker() {
		for (MarkerTabListener li : listeners) {
			li.removeMarker(markerId);
		}
	}

	private void fireScrollTo() {
		for (MarkerTabListener li : listeners) {
			li.scrollTo(markerId);
		}
	}


	public void setUser(User user) {
		if (user == this.user) {
			return;
		}
		if (chatBox != null) {
			setChatUser(chatBox, user);
		}
		if (marker.getType() == Marker.Type.LOCK) {
			User locker = User.getUser(((LockMarkerData) marker.getData())
					.getLockerId());
			removeButton.setVisible(locker == user);
		}
		this.user = user;
	}

	private static void setChatUser(ChatBox cb, User user) {
		if (user == null) {
			cb.setUser(null, null, null);
		} else {
			cb.setUser(user.getUserId(), user.getName(), user.getStyle());
		}
	}

	public String getTabTitle() {
		if (marker.getType() == Marker.Type.LOCK) {
			String lockerId = ((LockMarkerData) marker.getData()).getLockerId();
			return "Locked for " + User.getUser(lockerId).getName();
		}
		// if (marker.getType()==Marker.Type.NOTE) {
		// if (chat!=null) {
		// Chat cv = chat.getValue();
		// if (cv.getLines().isEmpty()) {
		// return "";
		// }
		// else {
		// return cv.getLines().get(cv.getLines().size()-1).toString();
		// }
		// }
		// }
		return null;
	}

	public Resource getTabIcon() {
		return iconFor(marker);
	}

	private static ThemeResource iconFor(Marker m) {
		if (m.getType() == Marker.Type.NOTE) {
			return NOTE_ICON;
		}
		if (m.getType() == Marker.Type.LOCK) {
			return LOCK_ICON;
		}
		return null;
	}
}
