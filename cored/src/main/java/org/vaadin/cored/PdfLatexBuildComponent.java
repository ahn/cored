package org.vaadin.cored;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;

import org.vaadin.cored.Project.DocListener;

import com.vaadin.terminal.FileResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.NativeSelect;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window.Notification;

@SuppressWarnings("serial")
public class PdfLatexBuildComponent extends Panel implements BuildComponent,
		ClickListener, DocListener {

	private static final String BUILD_SCRIPT = "/home/dev/bin/buildtex.sh";

	private VerticalLayout layout = new VerticalLayout();

	private Project project;
	private NativeSelect buildSelect;
	private Button buildButton = new Button("Create PDF");

	private Link pdfLink = new Link();
	private Label errorLabel = new Label();
	private Link errorButton = new Link("Build Error! Log here.", null);
	{
		errorLabel.setContentMode(Label.CONTENT_PREFORMATTED);
		errorLabel.setWidth("100%");
		errorLabel.setVisible(false);
		errorButton.setVisible(false);
	}

	public PdfLatexBuildComponent(Project project) {
		super("pdflatex");
		this.project = project;
		setContent(layout);
		draw();
	}

	private void draw() {
		layout.removeAllComponents();

		LinkedList<String> texFiles = new LinkedList<String>();
		for (String fn : project.getFileNames()) {
			if (fn.endsWith(".tex")) {
				texFiles.add(fn.substring(0, fn.length() - 4));
			}
		}

		buildSelect = new NativeSelect("Tex file", texFiles);
		buildSelect.setNullSelectionAllowed(false);
		if (!texFiles.isEmpty()) {
			buildSelect.select(texFiles.get(0));
		}

		layout.addComponent(buildSelect);
		layout.addComponent(buildButton);
		layout.addComponent(pdfLink);
		layout.addComponent(errorButton);
		layout.addComponent(errorLabel);
		buildButton.setEnabled(!texFiles.isEmpty());
	}

	@Override
	public void attach() {
		super.attach();
		project.addListener(this);
		buildButton.addListener(this);
	}

//	@Override
	public void buttonClick(ClickEvent event) {
		System.err.println("buttonClick");
		build();
	}

	private void build() {
		pdfLink.setVisible(false);
		errorButton.setVisible(false);
		errorLabel.setVisible(false);

		File temp;
		try {
			temp = File.createTempFile("textemp", "");
			temp.delete();
			temp.mkdir();
			project.writeToDiskInNewDir(temp);
		} catch (IOException e) {
			getWindow().showNotification("Error while writing to disk :(",
					Notification.TYPE_ERROR_MESSAGE);
			return;
		}

		int exitCode = 0;

		String mm = (String) buildSelect.getValue();
		try {
			Process child = Runtime.getRuntime().exec(
					BUILD_SCRIPT + " " + temp + " " + mm);
			exitCode = waitForExitCode(child);
		} catch (IOException e) {
			exitCode = -999;
		}

		if (exitCode == 0) {
			File pdf = new File(temp, mm + ".pdf");
			pdfLink.setCaption("View " + mm + ".pdf");
			FileResource pdfr = new FileResource(pdf, getApplication());
			pdfr.setCacheTime(0);
			pdfLink.setResource(pdfr);
			pdfLink.setVisible(true);
		} else if (exitCode == -999) {
			getWindow().showNotification(
					"Build environment not properly set up :(",
					Notification.TYPE_ERROR_MESSAGE);
		} else {
			File log = new File(temp, mm + ".log");
			try {
				FileResource lr = new FileResource(log, getApplication());
				lr.setCacheTime(0);
				errorButton.setResource(lr);
				errorButton.setVisible(true);
				String content = Project.readFile(log);
				errorLabel.setValue(errorFromLog(content));
				errorLabel.setVisible(true);
			} catch (IOException e) {
				getWindow().showNotification("Unknown build error :(",
						Notification.TYPE_ERROR_MESSAGE);
			}

		}
	}

	private static String errorFromLog(String log) {
		// FIXME
		StringBuilder sb = new StringBuilder();
		int i = log.indexOf("\n!");
		while (i != -1) {
			int k = log.indexOf("\nl.", i + 1);
			if (k == -1) {
				sb.append(log.substring(i));
				break;
			}
			int h = log.indexOf("\n", k + 1);
			if (h == -1) {
				sb.append(log.substring(i));
				break;
			} else {
				sb.append(log.substring(i, h));
			}
			i = log.indexOf("\n!", h);
			sb.append("\n\n");
		}
		return sb.toString();
	}

	private static int waitForExitCode(Process process) {
		Date endTime = new Date(new Date().getTime() + 5000L);
		while (!endTime.before(new Date())) {
			try {
				return process.exitValue();
			} catch (IllegalThreadStateException itse) {
				//
			}
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				// TODO
			}
		}
		return -999;
	}

//	@Override
	public void docCreated(String name, long collaboratorId) {
		draw();
	}

//	@Override
	public void docRemoved(String name, long collaboratorId) {
		draw();
	}
}
