package org.vaadin.cored;

import org.vaadin.aceeditor.collab.DocDiff;
import org.vaadin.aceeditor.collab.SuggestibleCollabAceEditor;
import org.vaadin.aceeditor.collab.gwt.shared.Doc;
import org.vaadin.aceeditor.java.VaadinSuggester;
import org.vaadin.aceeditor.java.util.InMemoryCompiler;
import org.vaadin.diffsync.Shared;

import com.vaadin.terminal.ExternalResource;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class SingleFileView extends CustomComponent {

	private final VerticalLayout layout = new VerticalLayout();
	private final SuggestibleCollabAceEditor editor;
	
	public SingleFileView(ProjectFile file, Project project, User user, boolean inIde) {
		super();
		System.out.println("SingleFileView " + file + ", " + project);
		layout.setSizeFull();
		
		HorizontalLayout ho = new HorizontalLayout();
		if (!inIde) {
			String url = "#"+project.getName()+"/"+file.getName()+"!";
			Link link = new Link("<<<", new ExternalResource(url));
			link.setDescription("View project");
			ho.addComponent(link);
		}
		ho.addComponent(new Label(file.getName()));
		if (inIde) {
			String url = "#"+project.getName()+"/"+file.getName();
			Link link = new Link(">>>", new ExternalResource(url));
			link.setDescription("View in standalone window");
			ho.addComponent(link);
		}
		layout.addComponent(ho);
		
		editor = createEditor(file, project);
		editor.setSizeFull();
		editor.setEnabled(user != null);
		layout.addComponent(editor);
		layout.setExpandRatio(editor, 1);
		
		System.out.println("Sin " + editor);
		
		setSizeFull();
		setCompositionRoot(layout);
	}
	
	public SuggestibleCollabAceEditor getEditor() {
		return editor;
	}
	
	private static SuggestibleCollabAceEditor createEditor(ProjectFile file, Project project) {
		if (!EditorUtil.isEditableWithEditor(file)) {
			return null;
		}
		Shared<Doc, DocDiff> doc = project.getDoc(file);
		SuggestibleCollabAceEditor ed = EditorUtil.createEditorFor(doc, file);
		if (file.getName().endsWith(".java")) {
			InMemoryCompiler compiler = ((VaadinProject)project).getCompiler();
			ed.setSuggester(new VaadinSuggester(compiler),
						VaadinSuggester.DEFAULT_SHORTCUT);
		}
		return ed;
	}
	
}
