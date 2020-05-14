package com.rationalagents.hypersuck;

import com.tableau.hyperapi.Catalog;
import com.tableau.hyperapi.Connection;
import com.tableau.hyperapi.HyperProcess;
import com.tableau.hyperapi.Result;
import com.tableau.hyperapi.ResultSchema;
import com.tableau.hyperapi.TableName;
import com.tableau.hyperapi.Telemetry;
import com.tableau.hyperapi.SchemaName;
import com.tableau.hyperapi.SqlType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.util.stream.Collectors.joining;

@RestController
public class ApiController {

	private Logger logger = Logger.getLogger(ApiController.class.getName());

	@Value("${HYPERPATH}")
	private String hyperPath;

	@RequestMapping(value="data", method = RequestMethod.GET)
	public String getData(@RequestParam(defaultValue = "https://public.tableau.com/workbooks/DPHIdahoCOVID-19Dashboard_V2.twb") String twbxUrl,
												@RequestParam(defaultValue = "Data/Datasources/County (COVID State Dashboard.V1).hyper") String hyperFilename,
												HttpServletResponse response) throws IOException {

		File tempDir = createTempDir();
		try {
			String matchFilenameExtracted = null;

			try (ZipInputStream  zis  = new ZipInputStream(new URL(twbxUrl).openStream())) {
				ZipEntry ze = zis.getNextEntry();
				while (ze != null) {
					String fileName = ze.getName();
					if (fileName.endsWith(".hyper")) {

						if (fileName.endsWith(hyperFilename)) {
							String extractedFilename = tempDir.getAbsolutePath() + File.separator + fileName;
							extractFile(zis, extractedFilename);

							// Matched and extracted!
							matchFilenameExtracted = extractedFilename;
						}

					}
					ze = zis.getNextEntry();
				}
			}

			if (matchFilenameExtracted == null) {
				return "No .hyper file matching\n" + hyperFilename;
			}

			// Going from suck to Tableau!

			try (HyperProcess process = new HyperProcess(Path.of(hyperPath),
				Telemetry.DO_NOT_SEND_USAGE_DATA_TO_TABLEAU,
				"",
				// keep logs from filling container (https://help.tableau.com/current/api/hyper_api/en-us/reference/sql/loggingsettings.html);
				Map.of("log_config", ""))) {

				try (Connection connection = new Connection(process.getEndpoint(), matchFilenameExtracted)) {

					List<TableName> tableNames = connection.getCatalog().getTableNames(new SchemaName("Extract"));

					// I wrote the above/below thinking there were >1 tables in some files, but I never saw that to be
					// the case w/ Idaho DHW .hyper files. There was always just one table named "Extract", like the
					// schema named "Extract." This could be rewritten to filter down to one table if there is a case
					// where a .hyper has multiple tables. For now, we'll just return what names if != 1.
					if (tableNames.size() != 1) {
						return "Unexpected tables\n" + tableNames.stream()
							.map(v -> v.getName().toString())
							.collect(joining("\n"));
					}

					StringBuilder csv = new StringBuilder();

					for (TableName tableName : tableNames) {
						Result result = connection.executeQuery("SELECT * FROM " + tableName.toString());
						ResultSchema resultSchema = result.getSchema();
						List<ResultSchema.Column> columns = resultSchema.getColumns();

						// Headers
						csv.append(columns.stream()
							.map(v -> v.getName().toString())
							.collect(joining(",")));
						csv.append("\n");

						// Rows
						while (result.nextRow()) {
							csv.append(columns.stream()
								.map(v -> getCsvCell(v, result, resultSchema))
								.collect(joining(",")));
							csv.append("\n");
						}
					}

					response.setContentType("text/csv");

					String csvName = last(hyperFilename.split("/"))
						.replace(".hyper", "");
					response.setHeader("Content-Disposition","attachment; filename=\"" + csvName + ".csv\"");

					return csv.toString();
				}
			}
		}
		finally {
			deleteTempDir(tempDir);
		}
	}

	/**
	 * Creates a tempdir, ensuring it exists before proceeding.
	 * Do not forget to delete using {@link #deleteTempDir(File)}!
	 */
	public File createTempDir() {
		File tempDir = new File(System.getProperty("java.io.tmpdir") + "/hypersuck-" + UUID.randomUUID().toString());
		if (!tempDir.exists()) {
			tempDir.mkdir();
		}
		return tempDir;
	}

	/**
	 * Given zip input stream positioned an entry, extracts the entry as a file with filename (full path to the file.)
	 */
	private static void extractFile(ZipInputStream zis, String filename) throws IOException {
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
	 * Java can't delete directories unless they're empty, so delete every file, then delete directory
	 */
	private void deleteTempDir(File directory) {
		String[] files = directory.list();
		for(String file: files){
			new File(directory.getPath(),file).delete();
		}
		directory.delete();
	}

	/**
	 * Gets next value from the Result for specified column in result schema as a String, as appropriate
	 * for CSV writing. For types I didn't expect to handle or know how to handle, the returned value will
	 * start with "?" then will indicate the unknown type.
	 *
	 * Unfortunately Tableau didn't seem to have a version of this, or I didn't find it. They'll scold you
	 * if you getObject.
	 */
	private String getCsvCell(ResultSchema.Column column, Result result, ResultSchema schema) {
		int position = schema.getColumnPositionByName(column.getName()).getAsInt();
		if (column.getType().equals(SqlType.bigInt())) {
			return result.isNull(position) ? "" : Long.toString(result.getLong(position));
		} else if (column.getType().equals(SqlType.doublePrecision())) {
			return result.isNull(position) ? "" : Double.toString(result.getDouble(position));
		} else if (column.getType().equals(SqlType.text())) {
			return result.isNull(position) ? "" : result.getString(position);
		} else if (column.getType().equals(SqlType.date())) {
			return result.isNull(position) ? "" : result.getLocalDate(position).toString();
		} else if (column.getType().equals(SqlType.geography())) {
			// ??
			return result.isNull(position) ? "" : "?GEO";
		} else {
			return "?" + column.getType().toString();
		}
	}

	private static <T> T last(T[] list) {
		return list[list.length - 1];
	}
}
