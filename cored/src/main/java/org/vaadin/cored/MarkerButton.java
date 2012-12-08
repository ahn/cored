package org.vaadin.cored;

import org.vaadin.aceeditor.gwt.shared.Marker;

import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Button;

@SuppressWarnings("serial")
public class MarkerButton extends Button {
	
	public MarkerButton(Marker marker) {
		super();
		setStyleName("link");
        setIcon(iconFor(marker));
	}
	
	private static ThemeResource iconFor(Marker m) {
		if (m.getType() == Marker.Type.NOTE) {
			return Icons.BALLOON_32;
		}
		if (m.getType() == Marker.Type.LOCK) {
			return Icons.LOCK_32;
		}
		return null;
	}
}
