package org.vaadin.cored;

import org.vaadin.aceeditor.collab.User;

import com.vaadin.terminal.ExternalResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.BaseTheme;

@SuppressWarnings("serial")
public class UserWidget extends HorizontalLayout {

	// private final User user;
	private Button kickButton = new Button("Kick");

	public UserWidget(User user) {
		super();
		setWidth("100%");
		setStyleName("v-userwidget " + user.getStyle());
		if (user instanceof FacebookUser) {
			FacebookUser fbUser = (FacebookUser) user;
			addFbImg(fbUser);
		} else {
			// TODO
		}

		VerticalLayout vl = new VerticalLayout();
		Label ul = new Label(user.getName());
		vl.addComponent(ul);
		kickButton.setStyleName(BaseTheme.BUTTON_LINK);
		vl.addComponent(kickButton);
		addComponent(vl);

		setExpandRatio(vl, 1);
	}

	public Button getKickButton() {
		return kickButton;
	}

	private void addFbImg(FacebookUser fbUser) {
		ExternalResource ex = new ExternalResource(
				fbImgURL(fbUser.getFacebookId()));
		Embedded emb = new Embedded(null, ex);
		emb.setType(Embedded.TYPE_IMAGE);
		emb.setWidth("50px");
		emb.setHeight("50px");
		addComponent(emb);
	}

	private static String fbImgURL(String fbId) {
		return "http://graph.facebook.com/" + fbId + "/picture";
	}
}
