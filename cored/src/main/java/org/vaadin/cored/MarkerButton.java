package org.vaadin.cored;

import org.vaadin.aceeditor.gwt.shared.CommentMarkerData;
import org.vaadin.aceeditor.gwt.shared.LockMarkerData;
import org.vaadin.aceeditor.gwt.shared.Marker;

import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Button;

@SuppressWarnings("serial")
public class MarkerButton extends Button {

	public MarkerButton(Marker marker) {
		super();
		setStyleName("link");
		setIcon(iconFor(marker));
		setDescription(descriptionFor(marker));
	}
	
	private static ThemeResource iconFor(Marker m) {
		if (m.getType() == Marker.Type.COMMENT) {
			return Icons.BALLOON_32;
		}
		if (m.getType() == Marker.Type.LOCK) {
			return Icons.LOCK_32;
		}
		return null;
	}
	
	private static String descriptionFor(Marker m) {
		if (m.getType() == Marker.Type.COMMENT) {
			return ((CommentMarkerData)m.getData()).getComment();
		}
		if (m.getType() == Marker.Type.LOCK) {
			return ((LockMarkerData)m.getData()).getMessage();
		}
		return null;
	}
}
