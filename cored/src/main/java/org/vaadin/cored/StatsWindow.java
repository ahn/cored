package org.vaadin.cored;

import java.util.Calendar;
import java.util.Date;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import org.vaadin.cored.ProjectLog.ChatLogItem;
import org.vaadin.cored.ProjectLog.LogItem;
import org.vaadin.cored.ProjectLog.UserEditLogItem;
import org.vaadin.cored.model.Project;
import org.vaadin.cored.model.User;

import com.vaadin.addon.timeline.Timeline;
import com.vaadin.addon.timeline.Timeline.ChartMode;
import com.vaadin.data.Container;
import com.vaadin.data.Container.Indexed;
import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.ui.Window;

@SuppressWarnings("serial")
public class StatsWindow extends Window {

	private Project project;
	private ProjectLog log;
	private Indexed markerCont = new IndexedContainer();
	{
		markerCont.addContainerProperty(Timeline.PropertyId.TIMESTAMP, Date.class, null);
		markerCont.addContainerProperty(Timeline.PropertyId.VALUE, String.class, "");
		markerCont.addContainerProperty(Timeline.PropertyId.CAPTION, String.class, "");
	}

	public StatsWindow(Project project) {
		super(project.getName()+" timeline");

		setWidth("400px");
		setHeight("300px");

		this.project = project;
		this.log = project.getLog();
		
		getContent().setSizeFull();
		
		tili();
	}
	
	private void tili() {
		Timeline timeline = new Timeline("Edits By User");
		timeline.setSizeFull();
		timeline.setChartMode(ChartMode.BAR);
		timeline.setChartModesVisible(false);
		timeline.setBrowserVisible(true);
		timeline.setVerticalGridLines(0, 1, 2, 5, 10, 20, 50);
		timeline.setZoomLevelsVisible(false);
		timeline.setDateSelectVisible(false);
		timeline.setGraphStacking(true);
		timeline.setGraphLegendVisible(true);
		timeline.setUniformBarThicknessEnabled(true);
		
		
		
		for (Entry<String, Indexed> e : createContainers().entrySet()) {
			Indexed cont = e.getValue();
			User user = project.getTeam().getUserByIdEvenIfKicked(e.getKey());
			System.out.println("User "+e.getKey() + " --- " + user);
			timeline.addGraphDataSource(cont, Timeline.PropertyId.TIMESTAMP, Timeline.PropertyId.VALUE);
			timeline.setGraphOutlineColor(cont, user.getColor());
			timeline.setGraphLegend(cont, user.getName());
			
			
		}
		
		timeline.setMarkerDataSource(markerCont, Timeline.PropertyId.TIMESTAMP, Timeline.PropertyId.CAPTION, Timeline.PropertyId.VALUE);
//		timeline.setEventDataSource(markerCont, Timeline.PropertyId.TIMESTAMP, Timeline.PropertyId.CAPTION);
		
		
		addComponent(timeline);
	}


	private TreeMap<String, Indexed> createContainers() {
		IndexedContainer container = new IndexedContainer();
		container.addContainerProperty(Timeline.PropertyId.TIMESTAMP,
				Date.class, null);
		container.addContainerProperty(Timeline.PropertyId.VALUE, Integer.class,
				0);
		
		Calendar cal = Calendar.getInstance();
		
		TreeMap<String, TreeMap<Date, Integer>> mm = new TreeMap<String, TreeMap<Date,Integer>>();
		
		TreeSet<Date> allDates = new TreeSet<Date>();
		
		for (LogItem li : log.getLines()) {
			Date timestamp = li.timestamp;
			
			ProjectLog.Type type = li.getType();
			if (type==ProjectLog.Type.EDIT_BY_USER) {
				UserEditLogItem item = (UserEditLogItem)li;
				
				
				TreeMap<Date, Integer> tm = mm.get(item.user.getUserId());
				if (tm==null) {
					tm = new TreeMap<Date, Integer>();
					mm.put(item.user.getUserId(), tm);
				}
				
				
				cal.setTime(timestamp);
				//cal.add(Calendar.SECOND, -(cal.get(Calendar.SECOND) % 10));
				cal.set(Calendar.SECOND, 30);
				cal.set(Calendar.MILLISECOND, 0);
				
				Date d = cal.getTime();
				allDates.add(d);
				if (tm.containsKey(d)) {
					tm.put(d, tm.get(d) + 1);
				} else {
					tm.put(d, 1);
				}
			}
			else if (type==ProjectLog.Type.CHAT) {
				ChatLogItem item = (ChatLogItem)li;
				if (item.user!=null) {
					Item i = markerCont.addItem(timestamp);
					i.getItemProperty(Timeline.PropertyId.TIMESTAMP).setValue(timestamp);
					i.getItemProperty(Timeline.PropertyId.VALUE).setValue(item.message);
					i.getItemProperty(Timeline.PropertyId.CAPTION).setValue(item.user.getName()+ ": "+item.message);
				}
			}
			
		}
		
		TreeMap<String, Container.Indexed> userConts = new TreeMap<String, Container.Indexed>();
		int offset = 0;
		for (Entry<String, TreeMap<Date, Integer>> e : mm.entrySet()) {
			System.out.println("Cont of " + e.getKey());
			Indexed c = cont(e.getValue(), allDates, offset++);
			userConts.put(e.getKey(), c);
		}
		
		return userConts;
	}
	
	private Container.Indexed cont(TreeMap<Date, Integer> m, TreeSet<Date> allDates, int offset) {
		IndexedContainer container = new IndexedContainer();
		container.addContainerProperty(Timeline.PropertyId.TIMESTAMP,Date.class, null);
		container.addContainerProperty(Timeline.PropertyId.CAPTION, String.class, "hehee");
		container.addContainerProperty(Timeline.PropertyId.VALUE, Integer.class, 0);

		Calendar cal = Calendar.getInstance();
		
		cal.setTime(allDates.first());
		cal.add(Calendar.MINUTE, -1);
		Date d0 = cal.getTime();
		Item item0 = container.addItem(d0);
		item0.getItemProperty(Timeline.PropertyId.TIMESTAMP).setValue(d0);
		item0.getItemProperty(Timeline.PropertyId.VALUE).setValue(0);
				
		for (Date d : allDates) {
			Integer n = m.get(d);
			if (n==null) {
				n = 0;
			}
			Item item = container.addItem(new Date(d.getTime()+offset*5000));
			item.getItemProperty(Timeline.PropertyId.TIMESTAMP).setValue(d);
			item.getItemProperty(Timeline.PropertyId.VALUE).setValue(n);
		}
		
		cal.setTime(allDates.last());
		cal.add(Calendar.MINUTE, 1);
		Date dx = cal.getTime();
		Item itemx = container.addItem(dx);
		itemx.getItemProperty(Timeline.PropertyId.TIMESTAMP).setValue(dx);
		itemx.getItemProperty(Timeline.PropertyId.VALUE).setValue(0);
		
		return container;
	}
}
