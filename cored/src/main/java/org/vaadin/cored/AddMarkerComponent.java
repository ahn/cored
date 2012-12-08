package org.vaadin.cored;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class AddMarkerComponent extends VerticalLayout {

	private final EditorView parent; 
	private final Button lockButton = new Button("Lock Selection");
	private final TextField noteField = new TextField("Note:");
	private final Button noteButton = new Button("Add Note");
	
	public AddMarkerComponent(EditorView parent, boolean note, boolean lock) {
		this.parent = parent;
		
		lockButton.setIcon(Icons.LOCK);
		noteButton.setIcon(Icons.BALLOON);
		
		noteField.setWidth("100%");
		
		addComponent(noteField);
		addComponent(noteButton);
		addComponent(new Label("&nbsp;", Label.CONTENT_XHTML));
		
		addComponent(lockButton);
		
		noteButton.setEnabled(note);
		noteField.setEnabled(note);
		lockButton.setEnabled(lock);
	}
	
	@Override
	public void attach() {
		super.attach();
		
		lockButton.addListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				getWindow().setVisible(false);
				parent.addLock();
			}
		});
		
		noteButton.addListener(new ClickListener() {
			public void buttonClick(ClickEvent event) {
				String note = (String)noteField.getValue();
				if (note!=null && !note.isEmpty()) {
					getWindow().setVisible(false);
					parent.addNote(note);
				}
			}
		});
	}
	

}
