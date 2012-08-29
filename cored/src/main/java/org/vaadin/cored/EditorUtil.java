package org.vaadin.cored;

import org.vaadin.aceeditor.collab.SuggestibleCollabAceEditor;
import org.vaadin.aceeditor.collab.DocDiff;
import org.vaadin.aceeditor.collab.gwt.shared.Doc;
import org.vaadin.aceeditor.gwt.ace.AceMode;
import org.vaadin.aceeditor.java.VaadinSuggester;
import org.vaadin.aceeditor.java.util.InMemoryCompiler;
import org.vaadin.diffsync.Shared;

public class EditorUtil {

	public static boolean isEditableWithEditor(String filename) {
		int lastDot = filename.lastIndexOf(".");
		if (lastDot == -1) {
			return true; // ?
		}
		String end = filename.substring(lastDot + 1);
		return !end.equals("exe") && !end.equals("pdf"); // TODO: more?
	}

	public static SuggestibleCollabAceEditor createEditorFor(
			Shared<Doc, DocDiff> doc, String filename) {

		SuggestibleCollabAceEditor editor = new SuggestibleCollabAceEditor(doc);

		AceMode mode = AceMode.forFile(filename);
		if (mode != null) {
			editor.setMode(mode, createModeURL(mode));
		}

		if (filename.endsWith(".java")) {
			editor.setSuggester(new VaadinSuggester(new InMemoryCompiler()),
					VaadinSuggester.DEFAULT_SHORTCUT);
		}

		return editor;
	}

	private static String createModeURL(AceMode mode) {
		// TODO FIXME XXX ?????
		return "http://antti.virtuallypreinstalled.com/cored/VAADIN/widgetsets/org.vaadin.codeeditor.gwt.AceEditorWidgetset/ace/mode-"
				+ mode.toString() + ".js";
	}
}