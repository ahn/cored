package org.vaadin.cored.lobby;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class CoredInfoComponent extends CustomComponent {
	
	private VerticalLayout root = new VerticalLayout();
	
	public CoredInfoComponent() {
		super();
		root.addComponent(new Label("CoRED is an experimental real-time collaborative editor for creating <a href=\"http://vaadin.com\">Vaadin</a> and other kinds of web applications.", Label.CONTENT_XHTML));
		root.addComponent(new Label("It's being developed at <a href=\"http://www.tut.fi\">TUT</a>. Contact <a href=\"mailto:antti.h.nieminen@tut.fi\">Antti</a> or <a href=\"mailto:janne.lautamaki@tut.fi\">Janne</a> if you like to know more.<br />", Label.CONTENT_XHTML));
		root.addComponent(new Label("&nbsp;", Label.CONTENT_XHTML));
		root.addComponent(new Label("Some icons by <a href=\"http://p.yusukekamiyamane.com/\">Yusuke Kamiyamane</a>.", Label.CONTENT_XHTML));
		root.addComponent(new Label("&nbsp;", Label.CONTENT_XHTML));
		setCompositionRoot(root);
	}
}
