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
import org.vaadin.cored.model.CoredDoc;
import org.vaadin.cored.model.Project;
import org.vaadin.cored.model.ProjectFile;
import org.vaadin.cored.model.Team;
import org.vaadin.cored.model.User;
import org.vaadin.cored.model.VaadinProject;
import org.vaadin.diffsync.Shared;

import com.vaadin.terminal.ExternalResource;
import com.vaadin.terminal.gwt.server.WebApplicationContext;
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
public class EditorView extends CustomComponent implements SelectionChangeListener, CloseListener, Team.UserFileListener, Project.DocListener {

	private final VerticalLayout layout = new VerticalLayout();
	private final SuggestibleCollabAceEditor editor;
	private final CoredDoc cdoc;
	private final ProjectFile file;
	private final User user;
	private final Project project;
	private final VerticalLayout userLayout = new VerticalLayout();
	private final VerticalLayout markerLayout = new VerticalLayout();

	
	EditorPopupWindow popup;
	private int selMin;
	private int selMax;
	
	private Map<String, Marker> latestMarkers = new TreeMap<String, Marker>();
	private String activeMarker;
	
	public EditorView(ProjectFile file, Project project, User user, boolean inIde, int line) {
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

		cdoc = project.getDoc(file);
		
		
		
		editor = createEditor(file, project);
		editor.setSizeFull();
		editor.setEnabled(user != null);
		editor.setUser(user.getUserId(), user.getStyle());
		
		final int pos = org.vaadin.aceeditor.gwt.shared.Util.cursorPosFromLineCol(
				cdoc.getShared().getValue().getText(), line, 0, 1);
		editor.scrollToPosition(pos);
		ho.addComponent(editor);
		ho.setExpandRatio(editor, 1);
		
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
		project.getTeam().setUserFileOpen(file, user, editor.getCollaboratorId());
		getWindow().addListener(this);
		showUsers();
		editor.addListener(this);
		project.addListenerWeakRef(this);
		project.getTeam().addListener(this);
		
	}
	
	@Override
	public void detach() {
		super.detach();
		editor.removeListener(this);
		project.removeListenerWeakRef(this);
		project.getTeam().removeListener(this);
		project.getTeam().setUserFileClosed(file, user, editor.getCollaboratorId());
		if (popup != null) {
			getWindow().removeWindow(popup);
		}
	}
	
	private void showUsers() {
		userLayout.removeAllComponents();
		Collection<User> users = project.getTeam().getUsersByFile(file);
		for (User u : users) {
			userLayout.addComponent(new UserWidget(u, false));
		}
	}
	
	public SuggestibleCollabAceEditor getEditor() {
		return editor;
	}
	
	private SuggestibleCollabAceEditor createEditor(ProjectFile file, Project project) {
		SuggestibleCollabAceEditor ed = EditorUtil.createEditorFor(cdoc.getShared(), file);
		if (file.getName().endsWith(".java") && project instanceof VaadinProject) {
			InMemoryCompiler compiler = ((VaadinProject)project).getCompiler();
			String className = ((VaadinProject)project).getPackageName()+"."+file.getName().substring(0, file.getName().length()-5);
			ed.setSuggester(new VaadinSuggester(compiler, className), VaadinSuggester.DEFAULT_SHORTCUT);
		}
		return ed;
	}

	public void selectionChanged(int start, int end) {
		selMin = Math.min(start, end);
		selMax = Math.max(start, end);
		DocDiff diff = user.cursorDiff(selMin, selMax, editor.getShadow().getText());
		cdoc.getShared().applyDiff(diff, editor.getCollaboratorId());
		checkMarkers();
	}

	public void docCreated(ProjectFile file) {
		// nothing
	}

	public void docRemoved(ProjectFile file) {
		// TODO
		// "always synchronize on the application instance when accessing
		// Vaadin UI components or related data from another thread."
		// https://vaadin.com/forum/-/message_boards/view_message/1785789#_19_message_212956
		// Is this enough of synchronization?
//		synchronized (getApplication()) {
			if (this.file.equals(file)) {
				layout.removeAllComponents();
				getWindow().showNotification("File "+file.getName()+" was deleted");
			}
//		}

	}
	
	

	public void userFilesChanged(Set<User> users, Set<ProjectFile> files) {
		// TODO
		// "always synchronize on the application instance when accessing
		// Vaadin UI components or related data from another thread."
		// https://vaadin.com/forum/-/message_boards/view_message/1785789#_19_message_212956
		// Is this enough of synchronization?
//		synchronized (getApplication()) {
			if (files.contains(file)) {
				showUsers();
			}
			if (users.contains(user) && !project.getTeam().hasUser(user)) {
				layout.removeAllComponents();
				getWindow().showNotification(user.getName()+ " has left");
			}
//		}
	}
	
	public void windowClose(CloseEvent e) {
		project.getTeam().setUserFileClosed(file, user, editor.getCollaboratorId());
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
				if (m.getType() == Marker.Type.COMMENT)
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
			if (popup!=null) {
				popup.setVisible(false);
			}
			activeMarker = null;
		}
	}
	
	
	private void showPopup(String title, Component content, int yCoord) {
		
		if (popup==null) {
			popup = new EditorPopupWindow();
			popup.addListener(new CloseListener() {
				public void windowClose(CloseEvent e) {
					popup = null;
				}
			});
			getWindow().addWindow(popup);
		}
		else {
			popup.removeAllComponents();
			popup.setVisible(true);
		}
		
		int bw = ((WebApplicationContext)getApplication().getContext()).getBrowser().getScreenWidth();
		
		popup.setPositionX(bw-250-80);
		popup.setPositionY(yCoord);
		popup.setWidth("250px");
		popup.setHeight("250px");
		popup.setVisible(true);
		popup.setCaption(title);
		
		popup.getContent().setSizeFull();
		popup.addComponent(content);
	}
	
	private void showMarkerPopup() {
		int[] coords = editor.getCursorCoords();
		showMarkerPopup(coords[1]);
	}
	
	private void showMarkerPopup(int yCoord) {
		final String markerId = activeMarker;
		SharedChat chat = project.getDoc(file).getMarkerChat(markerId);
		String firstLine = chat.getValue().getFrozenLines().get(0).getText();
		Marker m = latestMarkers.get(markerId);
		MarkerComponent mc = new MarkerComponent(m, user, chat);
		mc.addListener(new MarkerComponentListener() {
			public void removeMarker() {
				if (popup != null) {
					popup.setVisible(false);
				}
				activeMarker = null;
				latestMarkers.remove(markerId);
				updateMarkers(latestMarkers);
				removeMarkerById(markerId);
			}
		});
		showPopup(firstLine, mc, yCoord);
	}
	
	private void removeMarkerById(String markerId) {
		cdoc.getShared().applyDiff(DocDiff.removeMarker(markerId), editor.getCollaboratorId());
	}
	
	private void showAddMarkerPopup(boolean notingEnabled, boolean lockingEnabled) {
		AddMarkerComponent am = new AddMarkerComponent(this, notingEnabled, lockingEnabled);
		String title = editor.getShadow().getText().substring(selMin, selMax);
		showPopup(title, am, editor.getCursorCoords()[1]);
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
					activeMarker = markerId;
					showMarkerPopup(100);
					editor.scrollToMarkerId(markerId);
				}
			});
			markerLayout.addComponent(b);
		}
		latestMarkers = newMarkers;
	}

	private boolean isTabWorthyMarker(Marker m) {
		return m.getType() == Marker.Type.COMMENT
				|| (m.getType() == Marker.Type.LOCK && (m.getData() != null));
	}

	

	public void addLock() {
		Marker m = Marker.newLockMarker(selMin, selMax, user.getUserId(), "Locked for " + user.getName());
		ChatLine line = new ChatLine(user.getName() + " locked this part.");
		addMarker(m, line);
	}
	
	public void addNote(String note) {
		ChatLine line = new ChatLine(note, user.getUserId(), user.getName(), user.getStyle());
		Marker m = Marker.newCommentMarker(0, 0, note);
		addMarker(m, line);
	}
	
	private void addMarker(Marker m, ChatLine chatLine) {
		String markerId = editor.newItemId();
		Doc v = cdoc.getShared().getValue();
		// Adding the marker to the selection marker position, a bit of a hack.
		Marker sema = v.getMarkers().get(user.getSelectionMarkerId());
		if (sema==null) {
			getWindow().showNotification("Something went wrong marker-wise :( Please try again.");
			return;
		}
		String text = v.getText();
		int start = sema.getStart();
		int end = sema.getEnd();
		m = m.withNewPos(start, end);
		
		DocDiff d = DocDiff.addMarker(markerId, m, text);
		cdoc.getShared().applyDiff(d, editor.getCollaboratorId());
		
		if (chatLine!=null) {
			List<ChatLine> lines = Collections.singletonList(chatLine);
			project.getDoc(file).getMarkerChatCreateIfNotExist(markerId, lines);
		}
		
		latestMarkers.put(markerId, m);
		activeMarker = markerId;
		
		updateMarkers(latestMarkers);
		showMarkerPopup();
		editor.requestRepaint();
	}


	
}
