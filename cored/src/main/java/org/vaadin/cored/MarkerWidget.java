package org.vaadin.cored;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import org.vaadin.aceeditor.SelectionChangeListener;
import org.vaadin.aceeditor.collab.CollabDocAceEditor;
import org.vaadin.aceeditor.collab.DocDiff;
import org.vaadin.aceeditor.collab.User;
import org.vaadin.aceeditor.gwt.shared.Marker;
import org.vaadin.cored.MarkerTab.MarkerTabListener;

import com.vaadin.ui.Accordion;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class MarkerWidget extends CustomComponent implements
		SelectionChangeListener, MarkerTabListener {

	private CollabDocAceEditor editor;

	private Button lockButton;
	private Button noteButton;

	private HashMap<String, MarkerTab> tabsByMarkerId = new HashMap<String, MarkerTab>();

	private final Project project;

	Accordion tabs = new Accordion();
	private User user;
	private MarkerTab selectedTab;

	public MarkerWidget(Project project) {
		super();
		
		
//		tabs.addListener(new TabSheet.SelectedTabChangeListener() {
//			public void selectedTabChange(SelectedTabChangeEvent event) {
//				MarkerTab tab = (MarkerTab)event.getTabSheet().getSelectedTab();
//				System.out.println("tabchange " + tab);
//				if (selectedTab!=null) {
//					selectedTab.setICEPush(null);
//				}
//				tab.setICEPush(MarkerWidget.this.ice);
//			}
//		});

		this.project = project;
		

		VerticalLayout layout = new VerticalLayout();
		layout.setSizeFull();

		layout.addComponent(createNewMarkerComponent());

		tabs.setCaption("Notes & Locks:");
		tabs.setSizeFull();
		layout.addComponent(tabs);
		layout.setExpandRatio(tabs, 1);

		this.setWidth("200px");
		this.setHeight("250px");

		super.setCompositionRoot(layout);
	}

	public void setUser(User user) {
		this.user = user;
		for (MarkerTab tab : tabsByMarkerId.values()) {
			tab.setUser(user);
		}
	}

	public void listenToEditor(CollabDocAceEditor editor) {
		if (this.editor != null) {
			this.editor.removeListener(this);
		}
		this.editor = editor;
		editor.addListener(this);
	}

//	@Override
	public void selectionChanged(int start, int end) {
		int smaller = Math.min(start, end);
		int bigger = Math.max(start, end);
		int touchesLocks = 0;
		int touchesNotes = 0;
		LinkedList<String> touchingMarkers = new LinkedList<String>();
		Map<String, Marker> markers = editor.getShadow().getMarkers();
		for (Entry<String, Marker> e : markers.entrySet()) {
			final Marker m = e.getValue();
			if (!isTabWorthyMarker(m)) {
				continue;
			}
			Component tab = tabsByMarkerId.get(e.getKey());
			if (tab == null) {
				tab = newTab(e.getKey(), m);
			}
			if (m.touches(smaller, bigger)) {
				touchingMarkers.add(e.getKey());
				if (m.getType() == Marker.Type.LOCK)
					touchesLocks++;
				if (m.getType() == Marker.Type.NOTE)
					touchesNotes++;
			}

		}

		LinkedList<String> removed = new LinkedList<String>();
		for (String mid : tabsByMarkerId.keySet()) {
			if (!markers.containsKey(mid)) {
				removed.add(mid);
			}
		}
		for (String mid : removed) {
			removeTab(mid);
		}

		boolean selected = editor.getUser() != null && start != end;
		noteButton.setEnabled(selected && touchesNotes == 0);
		lockButton.setEnabled(selected && touchesLocks == 0);

		if (touchingMarkers.size() > 0
				&& !touchingMarkers.contains(selectedTab)) {
			selectedTab = tabsByMarkerId.get(touchingMarkers.getFirst());
			tabs.setSelectedTab(selectedTab);
		}
	}

	private void removeTab(String mid) {
		tabs.removeComponent(tabsByMarkerId.get(mid));
		tabsByMarkerId.remove(mid);
	}

	protected boolean isTabWorthyMarker(Marker m) {
		return m.getType() == Marker.Type.NOTE
				|| (m.getType() == Marker.Type.LOCK && (m.getData() != null));
	}

	protected Component newTab(final String mid, Marker m) {
		MarkerTab tab = new MarkerTab(mid, m, user, project);
		tab.addListener(this);
		tabsByMarkerId.put(mid, tab);
		String title = tab.getTabTitle();
		if (title == null) {
			title = shorten(m.substringOf(editor.getShadow().getText()), 16);
		}
		tabs.addTab(tab, title, tab.getTabIcon());
		
		selectedTab = tab;
		return tab;
	}

	private static String shorten(String s, int len) {
		if (s.length() <= len) {
			return s;
		}
		return s.substring(0, len - 2) + "...";
	}

	private Component createNewMarkerComponent() {
		Panel panel = new Panel("Selection");
		panel.setContent(new HorizontalLayout());
		noteButton = new Button("Add Note");
		noteButton.setEnabled(false);
		noteButton.addListener(new ClickListener() {
//			@Override
			public void buttonClick(ClickEvent event) {
				editor.addMarkerToSelection(Marker.newNoteMarker(0, 0));
			}
		});
		panel.addComponent(noteButton);
		lockButton = new Button("Lock");
		lockButton.setEnabled(false);
		lockButton.addListener(new ClickListener() {
//			@Override
			public void buttonClick(ClickEvent event) {
				User user = editor.getUser();
				editor.addMarkerToSelection(Marker.newLockMarker(0, 0,
						user.getUserId(), "Locked for " + user.getName()));
			}
		});
		panel.addComponent(lockButton);
		return panel;
	}

//	@Override
	public void removeMarker(String markerId) {
		project.removeMarkerChat(markerId);
		DocDiff dd = DocDiff.removeMarker(markerId);
		editor.getShared().applyDiff(dd, 0);
	}

//	@Override
	public void scrollTo(String markerId) {
		editor.scrollToMarkerId(markerId);
	}

}
