package org.vaadin.cored;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.vaadin.aceeditor.collab.DocDiff;
import org.vaadin.aceeditor.collab.gwt.shared.Doc;
import org.vaadin.aceeditor.gwt.shared.LockMarkerData;
import org.vaadin.aceeditor.gwt.shared.Marker;
import org.vaadin.diffsync.DiffCalculator;

/**
 * Cleans up old markers.
 * 
 */
public class MarkerCleanupTask implements DiffCalculator<Doc, DocDiff> {

	private HashMap<String, Date> markerDates = new HashMap<String, Date>();

	private Date nextCheckTime;

	private static final long EDIT_LIFETIME_MS = 60L * 1000L;
	private static final long LOCK_LIFETIME_MS = 5L * 60L * 1000L;

	private static final long MIN_CHECK_INTERVAL_MS = 60L * 1000L;

//	@Override
	public boolean needsToRunAfter(DocDiff diff, long byCollaboratorId) {
		return nextCheckTime == null || nextCheckTime.before(new Date());
	}

//	@Override
	public DocDiff calcDiff(Doc value) throws InterruptedException {
		Date now = new Date();
		final Date editCutoff = new Date(now.getTime() - EDIT_LIFETIME_MS);
		final Date lockCutoff = new Date(now.getTime() - LOCK_LIFETIME_MS);
		Date cutoff = null;
		HashSet<String> removeMarkers = new HashSet<String>();
		for (Entry<String, Marker> e : value.getMarkers().entrySet()) {
			Marker m = e.getValue();
			if (m.getType() == Marker.Type.EDIT) {
				cutoff = editCutoff;
			} else if (isUserLock(m)) {
				cutoff = lockCutoff;
			} else {
				continue;
			}
			Date d = markerDates.get(e.getKey());
			if (d == null) {
				markerDates.put(e.getKey(), new Date());
			} else if (d.before(cutoff)) {
				markerDates.remove(e.getKey());
			}
		}

		// TODO: clean up markerDates

		nextCheckTime = new Date(now.getTime() + MIN_CHECK_INTERVAL_MS);

		if (removeMarkers.isEmpty()) {
			return null;
		} else {
			return DocDiff.removeMarkers(removeMarkers);
		}
	}

	private static boolean isUserLock(Marker marker) {
		if (marker.getType() != Marker.Type.LOCK) {
			return false;
		}
		LockMarkerData data = (LockMarkerData) marker.getData();
		return data != null;
	}

}
