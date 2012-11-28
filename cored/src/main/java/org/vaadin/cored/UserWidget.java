package org.vaadin.cored;

import com.vaadin.terminal.ExternalResource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.BaseTheme;

@SuppressWarnings("serial")
public class UserWidget extends HorizontalLayout {

	
	
	private Button kickButton = new Button("Kick");

	public UserWidget(User user) {
		super();
		setWidth("100%");
		setHeight("56px");
		
		setStyleName("v-userwidget " + user.getStyle());
		if (user instanceof FacebookUser) {
			FacebookUser fbUser = (FacebookUser) user;
			addFbImg(fbUser);
		} else {
			addGenericImg();
		}

		VerticalLayout vl = new VerticalLayout();
		//vl.setHeight("50px");
		Label ul = new Label(user.getName());
		ul.addStyleName("name-label");
		vl.addComponent(ul);
		kickButton.setStyleName(BaseTheme.BUTTON_LINK);
		vl.addComponent(kickButton);
		addComponent(vl);
		//setComponentAlignment(vl, Alignment.MIDDLE_CENTER);

		setExpandRatio(vl, 1);
	}

	public Button getKickButton() {
		return kickButton;
	}
	
	private void addGenericImg() {
		VerticalLayout ve = new VerticalLayout();
		ve.setWidth("50px");
		ve.setHeight("50px");
		Embedded emb = new Embedded(null, Icons.USER_SILHOUETTE);
		ve.addComponent(emb);
		ve.setComponentAlignment(emb, Alignment.MIDDLE_CENTER);
		ve.addStyleName("user-image");
		addComponent(ve);
	}

	private void addFbImg(FacebookUser fbUser) {
		ExternalResource ex = new ExternalResource(
				fbImgURL(fbUser.getFacebookId()));
		Embedded emb = new Embedded(null, ex);
		emb.setType(Embedded.TYPE_IMAGE);
		emb.setWidth("50px");
		emb.setHeight("50px");
		emb.addStyleName("user-image");
		addComponent(emb);
	}

	private static String fbImgURL(String fbId) {
		return "https://graph.facebook.com/" + fbId + "/picture";
	}
}
