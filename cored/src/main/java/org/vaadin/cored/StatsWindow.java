package org.vaadin.cored;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import com.vaadin.addon.timeline.Timeline;
import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.ui.Window;

@SuppressWarnings("serial")
public class StatsWindow extends Window {

	public StatsWindow() {
		Timeline timeline = new Timeline("My timeline");
		timeline.setWidth("400px");
		timeline.setHeight("300px");

		// Add the container
		timeline.addGraphDataSource(createContainer(), Timeline.PropertyId.TIMESTAMP, Timeline.PropertyId.VALUE);

		addComponent(timeline);

	}

	/**
	 * Creates a graph container with random data points
	 */
	private Container.Indexed createContainer() {

		// Create the container
		IndexedContainer container = new IndexedContainer();
		container.addContainerProperty(Timeline.PropertyId.TIMESTAMP,
				Date.class, null);
		container.addContainerProperty(Timeline.PropertyId.VALUE, Float.class,
				0);

		// Lets add a month of random data
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, -1);
		Date today = new Date();
		Random generator = new Random();

		while (cal.getTime().before(today)) {
			Item item = container.addItem(cal.getTime());
			item.getItemProperty(Timeline.PropertyId.TIMESTAMP).setValue(
					cal.getTime());
			item.getItemProperty(Timeline.PropertyId.VALUE).setValue(
					generator.nextFloat());
			cal.add(Calendar.DAY_OF_MONTH, 1);
		}

		return container;
	}

}
