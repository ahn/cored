package org.vaadin.cored;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.vaadin.aceeditor.SelectionChangeListener;
import org.vaadin.aceeditor.collab.DocDiff;
import org.vaadin.aceeditor.collab.SuggestibleCollabAceEditor;
import org.vaadin.aceeditor.collab.gwt.shared.Doc;
import org.vaadin.aceeditor.gwt.shared.Marker;
import org.vaadin.aceeditor.java.VaadinSuggester;
import org.vaadin.aceeditor.java.util.InMemoryCompiler;
import org.vaadin.chatbox.SharedChat;
import org.vaadin.chatbox.gwt.shared.ChatLine;
import org.vaadin.cored.MarkerComponent.MarkerComponentListener;
import org.vaadin.diffsync.Shared;

import com.vaadin.terminal.ExternalResource;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Link;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window.CloseEvent;
import com.vaadin.ui.Window.CloseListener;

@SuppressWarnings("serial")
public class EditorView extends CustomComponent implements SelectionChangeListener, CloseListener, Team.UserFileListener {

	private final VerticalLayout layout = new VerticalLayout();
	private final SuggestibleCollabAceEditor editor;
	private final Shared<Doc, DocDiff> doc;
	private final ProjectFile file;
	private final User user;
	private final Project project;
	private final VerticalLayout userLayout = new VerticalLayout();
	private final VerticalLayout markerLayout = new VerticalLayout();

	
	EditorPopupWindow popup = new EditorPopupWindow();
	private int selMin;
	private int selMax;
	
	private Map<String, Marker> latestMarkers = new TreeMap<String, Marker>();
	private String activeMarker;
	
	public EditorView(ProjectFile file, Project project, User user, boolean inIde) {
		super();
		this.file = file;
		this.project = project;
		this.user = user;
		layout.setSizeFull();
		
		HorizontalLayout ho = new HorizontalLayout();
		if (!inIde) {
			String url = "#"+project.getName()+"/"+file.getName()+"!";
			Link link = new Link("<<< " + file.getName(), new ExternalResource(url));
			link.setDescription("View project");
			layout.addComponent(link);
		}
		if (inIde) {
			String url = "#"+project.getName()+"/"+file.getName();
			Link link = new Link(file.getName()+" >>>", new ExternalResource(url));
			link.setDescription("View in standalone window");
			layout.addComponent(link);
		}
		
		layout.addComponent(ho);
		
		layout.setExpandRatio(ho, 1);
		ho.setSizeFull();

		doc = project.getDoc(file);
		
		editor = createEditor(file, project);
		editor.setSizeFull();
		editor.setEnabled(user != null);
		editor.setUser(user.getUserId(), user.getStyle());
		ho.addComponent(editor);
		ho.setExpandRatio(editor, 1);
		
		popup.setVisible(false);
		
		
		VerticalLayout rightBar = new VerticalLayout();
		rightBar.setWidth("64px");
		rightBar.addComponent(userLayout);
		rightBar.addComponent(markerLayout);
		
		ho.addComponent(rightBar);

		setSizeFull();
		setCompositionRoot(layout);
		
	
	}
	
	@Override
	public void attach() {
		super.attach();
		System.out.println("EV attach");
		editor.addListener(this);
		project.getTeam().setUserFileOpen(file, user, editor.getCollaboratorId());
		getWindow().addListener(this);
		showUsers();
		project.getTeam().addListener(this);
		getWindow().addWindow(popup);
	}
	
	@Override
	public void detach() {
		super.detach();
		System.out.println("EV detach");
		editor.removeListener(this);
		project.getTeam().removeListener(this);
		project.getTeam().setUserFileClosed(file, user, editor.getCollaboratorId());
		getWindow().removeWindow(popup);
	}
	
	private void showUsers() {
		userLayout.removeAllComponents();
		Collection<User> users = project.getTeam().getUsersByFile(file);
		for (User u : users) {
			userLayout.addComponent(new UserWidget(u));
		}
	}
	
	public SuggestibleCollabAceEditor getEditor() {
		return editor;
	}
	
	private SuggestibleCollabAceEditor createEditor(ProjectFile file, Project project) {
		SuggestibleCollabAceEditor ed = EditorUtil.createEditorFor(doc, file);
		if (file.getName().endsWith(".java")) {
			InMemoryCompiler compiler = ((VaadinProject)project).getCompiler();
			ed.setSuggester(new VaadinSuggester(compiler),
						VaadinSuggester.DEFAULT_SHORTCUT);
		}
		return ed;
	}

	public void selectionChanged(int start, int end) {
		// "always synchronize on the application instance when accessing
		// Vaadin UI components or related data from another thread."
		// https://vaadin.com/forum/-/message_boards/view_message/1785789#_19_message_212956
		// Is this enough of synchronization?
		synchronized (getApplication()) {
			selectionChangedAfterSync(start, end);
		}
	}
	
	private void selectionChangedAfterSync(int start, int end) {
		selMin = Math.min(start, end);
		selMax = Math.max(start, end);
		DocDiff diff = user.cursorDiff(selMin, selMax, editor.getShadow().getText());
		doc.applyDiff(diff);
		checkMarkers();
	}
	
	private void checkMarkers() {
		
		int touchesLocks = 0;
		int touchesNotes = 0;
		LinkedList<String> touchingMarkers = new LinkedList<String>();
		Map<String, Marker> markers = editor.getShared().getValue().getMarkers();
		boolean redraw = false;
		for (Entry<String, Marker> e : markers.entrySet()) {
			final Marker m = e.getValue();
			if (!isTabWorthyMarker(m)) {
				continue;
			}
			if (!latestMarkers.containsKey(e.getKey())) {
				redraw = true;
			}
			if (m.touches(selMin, selMax)) {
				touchingMarkers.add(e.getKey());
				if (m.getType() == Marker.Type.LOCK)
					touchesLocks++;
				if (m.getType() == Marker.Type.NOTE)
					touchesNotes++;
			}
		}

		if (!redraw) {
			for (String mid : latestMarkers.keySet()) {
				if (!markers.containsKey(mid)) {
					redraw = true;
					break;
				}
			}
		}
		
		if (redraw) {
			updateMarkers(markers);
		}
		
		boolean selected = (selMin != selMax);
		boolean notingEnabled = selected && touchesNotes == 0;
		boolean lockingEnabled = selected && touchesLocks == 0;
		
		if (touchingMarkers.size() > 0) {
			if (!touchingMarkers.contains(activeMarker)) {
				activeMarker = touchingMarkers.getFirst();
				showMarkerPopup();
			}
		}
		else if (notingEnabled || lockingEnabled) {
			showAddMarkerPopup(notingEnabled, lockingEnabled);
		}
		else {
			popup.setVisible(false);
			activeMarker = null;
		}
	}
	
	private void showPopup(String title, Component content) {
		int[] coords = editor.getCursorCoords();
		popup.setPositionX(coords[0]+200);
		popup.setPositionY(coords[1]);
		popup.setWidth("250px");
		popup.setHeight("250px");
		popup.setVisible(true);
		popup.setCaption(title);
		popup.removeAllComponents();
		popup.getContent().setSizeFull();
		popup.addComponent(content);
	}
	
	private void showMarkerPopup() {
		final String markerId = activeMarker;
		String firstLine = project.getMarkerChat(file, activeMarker).getValue().getFrozenLines().get(0).getText();
		Marker m = latestMarkers.get(activeMarker);
		SharedChat chat = project.getMarkerChat(file, activeMarker);
		MarkerComponent mc = new MarkerComponent(m, user, chat);
		mc.addListener(new MarkerComponentListener() {
			public void removeMarker() {
				popup.setVisible(false);
				activeMarker = null;
				latestMarkers.remove(markerId);
				updateMarkers(latestMarkers);
				removeMarkerById(markerId);
			}
		});
		showPopup(firstLine, mc);
	}
	
	private void removeMarkerById(String markerId) {
		doc.applyDiff(DocDiff.removeMarker(markerId));
	}
	
	private void showAddMarkerPopup(boolean notingEnabled, boolean lockingEnabled) {
		AddMarkerComponent am = new AddMarkerComponent(this, notingEnabled, lockingEnabled);
		String title = editor.getShadow().getText().substring(selMin, selMax);
		showPopup(title, am);
	}

	private void updateMarkers(Map<String, Marker> markers) {
		markerLayout.removeAllComponents();
		HashMap<String, Marker> newMarkers = new HashMap<String, Marker>();
		TreeMap<Marker,String> ordered = new TreeMap<Marker,String>();
		for (Entry<String, Marker> e : markers.entrySet()) {
			if (isTabWorthyMarker(e.getValue())) {
				newMarkers.put(e.getKey(), e.getValue());
				ordered.put(e.getValue(), e.getKey());
			}
		}
		for (Entry<Marker, String> e : ordered.entrySet()) {
			final String markerId = e.getValue();
			MarkerButton b = new MarkerButton(e.getKey());
			b.addListener(new ClickListener() {
				public void buttonClick(ClickEvent event) {
					editor.scrollToMarkerId(markerId);
				}
			});
			markerLayout.addComponent(b);
		}
		latestMarkers = newMarkers;
	}

	private boolean isTabWorthyMarker(Marker m) {
		return m.getType() == Marker.Type.NOTE
				|| (m.getType() == Marker.Type.LOCK && (m.getData() != null));
	}

	synchronized public void windowClose(CloseEvent e) {
		project.getTeam().setUserFileClosed(file, user, editor.getCollaboratorId());
	}

	synchronized public void userFilesChanged(Set<User> users, Set<ProjectFile> files) {
		if (files.contains(file)) {
			showUsers();
		}
	}

	synchronized public void addLock() {
		Marker m = Marker.newLockMarker(selMin, selMax, user.getUserId(), "Locked for " + user.getName());
		String markerId = editor.newItemId();
		DocDiff d = DocDiff.addMarker(markerId, m, editor.getShadow().getText());
		editor.getShared().applyDiff(d);
		List<ChatLine> lines = Collections.singletonList(new ChatLine(user.getName() + " locked this part."));
		project.getMarkerChatCreateIfNotExist(file, markerId, lines);

		latestMarkers.put(markerId, m);
		activeMarker = markerId;
		updateMarkers(latestMarkers);
		showMarkerPopup();
	}
	
	synchronized public void addNote(String note) {
		Marker m = Marker.newNoteMarker(selMin, selMax);
		String markerId = editor.newItemId();
		DocDiff d = DocDiff.addMarker(markerId, m, editor.getShadow().getText());
		editor.getShared().applyDiff(d);
		ChatLine line = new ChatLine(note, user.getUserId(), user.getName(), user.getStyle());
		List<ChatLine> lines = Collections.singletonList(line);
		project.getMarkerChatCreateIfNotExist(file, markerId, lines);
		
		latestMarkers.put(markerId, m);
		activeMarker = markerId;
		updateMarkers(latestMarkers);
		showMarkerPopup();
	}
	
}
