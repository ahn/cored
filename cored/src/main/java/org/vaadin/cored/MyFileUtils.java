package org.vaadin.cored;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Deque;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MyFileUtils {

	public static void writeFileToDisk(File f, String content)
			throws IOException {
		f.getParentFile().mkdirs();
		FileWriter fstream = new FileWriter(f);
		BufferedWriter out = new BufferedWriter(fstream);
		out.write(content);
		out.close();
	}

	public static File relativize(File base, File child) {
		return new File(base.toURI().relativize(child.toURI()).getPath());
	}

	// http://www.java-examples.com/create-zip-file-directory-recursively-using-zipoutputstream-example
	public static void zipDir(File dir, File zipFile) throws IOException {
		FileOutputStream fout = new FileOutputStream(zipFile);
		ZipOutputStream zout = new ZipOutputStream(fout);
		addDirToZip(dir, dir, zout);
		zout.close();
	}

	private static void addDirToZip(File base, File dir, ZipOutputStream zout)
			throws IOException {
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				zout.putNextEntry(new ZipEntry(MyFileUtils
						.relativize(base, file).getPath() + File.separator));
				addDirToZip(base, file, zout);
			} else {
				byte[] buffer = new byte[1024];
				FileInputStream fin = new FileInputStream(file);
				zout.putNextEntry(new ZipEntry(MyFileUtils.relativize(base, file).getPath()));
				int length;
				while ((length = fin.read(buffer)) > 0) {
					zout.write(buffer, 0, length);
				}
				zout.closeEntry();
				fin.close();
			}
		}
	}

	// http://stackoverflow.com/questions/1399126/java-util-zip-recreating-directory-structure
	public static void zip(File directory, File zipfile) throws IOException {
		URI base = directory.toURI();
		Deque<File> queue = new LinkedList<File>();
		queue.push(directory);
		OutputStream out = new FileOutputStream(zipfile);
		Closeable res = out;
		try {
			ZipOutputStream zout = new ZipOutputStream(out);
			res = zout;
			while (!queue.isEmpty()) {
				directory = queue.pop();
				for (File kid : directory.listFiles()) {
					String name = base.relativize(kid.toURI()).getPath();
					if (kid.isDirectory()) {
						queue.push(kid);
						name = name.endsWith("/") ? name : name + "/";
						zout.putNextEntry(new ZipEntry(name));
					} else {
						zout.putNextEntry(new ZipEntry(name));
						copy(kid, zout);
						zout.closeEntry();
					}
				}
			}
		} finally {
			res.close();
			out.close();
		}
	}

	private static void copy(InputStream in, OutputStream out)
			throws IOException {
		byte[] buffer = new byte[1024];
		while (true) {
			int readCount = in.read(buffer);
			if (readCount < 0) {
				break;
			}
			out.write(buffer, 0, readCount);
		}
	}

	private static void copy(File file, OutputStream out) throws IOException {
		InputStream in = new FileInputStream(file);
		try {
			copy(in, out);
		} finally {
			in.close();
		}
	}

	private static void copy(InputStream in, File file) throws IOException {
		OutputStream out = new FileOutputStream(file);
		try {
			copy(in, out);
		} finally {
			out.close();
		}
	}
}
