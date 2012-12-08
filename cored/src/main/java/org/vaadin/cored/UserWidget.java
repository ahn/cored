package org.vaadin.cored;

import com.vaadin.terminal.ExternalResource;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.BaseTheme;

@SuppressWarnings("serial")
public class UserWidget extends VerticalLayout {

	
	
//	private Button kickButton = new Button("Kick");

	public UserWidget(User user) {
		super();
		setWidth("56px");
//		setHeight("56px");
		
		setStyleName("v-userwidget " + user.getStyle());
		
		Label nameLabel = new Label(user.getName());
		nameLabel.setWidth("50px");
		addComponent(nameLabel);
		
		AbstractComponent img;
		if (user instanceof FacebookUser) {
			FacebookUser fbUser = (FacebookUser) user;
			img = addFbImg(fbUser);
		} else {
			img = createGravatarImg(user);
		}
		addComponent(img);
		img.setDescription(user.getName());
		

//		VerticalLayout vl = new VerticalLayout();
		//vl.setHeight("50px");
//		Label ul = new Label(user.getName());
//		ul.addStyleName("name-label");
//		vl.addComponent(ul);
		
//		kickButton.setStyleName(BaseTheme.BUTTON_LINK);
//		vl.addComponent(kickButton);
//		addComponent(vl);
		//setComponentAlignment(vl, Alignment.MIDDLE_CENTER);

//		setExpandRatio(vl, 1);
	}

//	public Button getKickButton() {
//		return kickButton;
//	}
	
	private AbstractComponent addGenericImg() {
		VerticalLayout ve = new VerticalLayout();
		ve.setWidth("50px");
		ve.setHeight("50px");
		Embedded emb = new Embedded(null, Icons.USER_SILHOUETTE);
		ve.addComponent(emb);
		ve.setComponentAlignment(emb, Alignment.MIDDLE_CENTER);
		ve.addStyleName("user-image");
		return ve;
	}
	
	private AbstractComponent createGravatarImg(User user) {
		ExternalResource ex = new ExternalResource(GravatarUtil.gravatarUrlFor(user, 50));
		Embedded emb = new Embedded(null, ex);
		emb.setType(Embedded.TYPE_IMAGE);
		emb.setWidth("50px");
		emb.setHeight("50px");
		emb.addStyleName("user-image");
		return emb;
	}

	private AbstractComponent addFbImg(FacebookUser fbUser) {
		ExternalResource ex = new ExternalResource(
				fbImgURL(fbUser.getFacebookId()));
		Embedded emb = new Embedded(null, ex);
		emb.setType(Embedded.TYPE_IMAGE);
		emb.setWidth("50px");
		emb.setHeight("50px");
		emb.addStyleName("user-image");
		return emb;
	}

	private static String fbImgURL(String fbId) {
		return "https://graph.facebook.com/" + fbId + "/picture";
	}
}
