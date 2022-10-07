package com.rationalagents.twbxless;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipInputStream;

/**
 * Utilities related to needing to deal directly with filesystem/zip to use Hyper API, and to unzip
 * .twbx files to .hyper for that purpose (it works only with .hyper AFAIK.)
 */
public class FileUtils {

	/**
	 * Given {@link ZipInputStream} positioned on an entry, extracts the entry as a file,
	 * returning the the full path to the file.
	 */
	public static String extractFile(ZipInputStream zis) throws IOException {
		var file = File.createTempFile("twbxless-", "");
		var outputStream = new FileOutputStream(file);
		int len;
		byte[] buffer = new byte[1024];
		while ((len = zis.read(buffer)) > 0) {
			outputStream.write(buffer, 0, len);
		}
		outputStream.close();

		return file.getAbsolutePath();
	}

}
