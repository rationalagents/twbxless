package com.rationalagents.twbxless;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.zip.ZipInputStream;

/**
 * Utilities related to needing to deal directly with filesystem/zip to use Hyper API, and to unzip
 * .twbx files to .hyper for that purpose (it works only with .hyper AFAIK.)
 */
public class FileUtils {
	/**
	 * Creates a temporary directory , ensuring it exists before proceeding.
	 *
	 * Don't forget to delete using {@link #deleteTempDir(File)}!
	 */
	public static File createTempDir() {
		File tempDir = new File(System.getProperty("java.io.tmpdir") + "/twbxless-" + UUID.randomUUID().toString());
		if (!tempDir.exists()) {
			tempDir.mkdir();
		}
		return tempDir;
	}

	/**
	 * Given {@link ZipInputStream} positioned on an entry, extracts the entry as a file with filename specified
	 * (a full path to the file.)
	 */
	static void extractFile(ZipInputStream zis, String filename) throws IOException {
		File newFile = new File(filename);
		new File(newFile.getParent()).mkdirs();
		FileOutputStream fos = new FileOutputStream(newFile);
		int len;
		byte[] buffer = new byte[1024];
		while ((len = zis.read(buffer)) > 0) {
			fos.write(buffer, 0, len);
		}
		fos.close();
	}

	/**
	 * Java can't delete directories unless they're empty. This deletes every file, then the directory.
	 */
	static void deleteTempDir(File directory) {
		String[] files = directory.list();
		for(String file: files){
			new File(directory.getPath(),file).delete();
		}
		directory.delete();
	}
}
