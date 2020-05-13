package com.rationalagents.hypersuck;

import com.tableau.hyperapi.*;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@RestController
public class ApiController {

	@RequestMapping(method = RequestMethod.GET)
	public String get(@RequestParam(defaultValue = "https://public.tableau.com/workbooks/DPHIdahoCOVID-19Dashboard_V2.twb") String twbxUrl,
										@RequestParam(defaultValue = "Data/Datasources/County (COVID State Dashboard.V1).hyper") String extractFilename) throws IOException {

		Logger logger = Logger.getLogger(ApiController.class.getName());

		File tempDir = new File(System.getProperty("java.io.tmpdir") + "/hypersuck-" + UUID.randomUUID().toString());
		try {
			if (!tempDir.exists()) {
				tempDir.mkdir();
			}

			List<String> matchingExtractedFilenames = new ArrayList<>();
			InputStream stream = new URL(twbxUrl).openStream();
			try (ZipInputStream  zis  = new ZipInputStream(stream)) {
				ZipEntry ze = zis.getNextEntry();
				while (ze != null) {
					String fileName = ze.getName();
					if (fileName.endsWith(extractFilename)) {
						logger.info("Found: " + extractFilename);

						File newFile = new File(tempDir.getAbsolutePath() + File.separator + fileName);

						matchingExtractedFilenames.add(newFile.getAbsolutePath());

						new File(newFile.getParent()).mkdirs();
						FileOutputStream fos = new FileOutputStream(newFile);
						int len;
						byte[] buffer = new byte[1024];
						while ((len = zis.read(buffer)) > 0) {
							fos.write(buffer, 0, len);
						}
						fos.close();
					} else if (fileName.endsWith(".hyper")) {
						// log the other non-matching .hyper files in the extract
						logger.info("Skipping: " + fileName);
					}
					ze = zis.getNextEntry();
				}
			}

			if (matchingExtractedFilenames.size() == 0) {
				return "No files matching\n" + extractFilename;
			}

			logger.info("Going from suck to Tableau!");

			// logging off https://help.tableau.com/current/api/hyper_api/en-us/reference/sql/loggingsettings.html));
			HyperProcess process = new HyperProcess(Telemetry.DO_NOT_SEND_USAGE_DATA_TO_TABLEAU, "", Map.of(
				"log_config", ""));

			StringBuilder csvBuilder = new StringBuilder();
			for (String fileName : matchingExtractedFilenames) {

				try (Connection connection = new Connection(process.getEndpoint(), fileName)) {

					Catalog catalog = connection.getCatalog();
					SchemaName extractSchema = new SchemaName("Extract");
					List<TableName> tableNames = catalog.getTableNames(extractSchema);

					// I wrote this thinking there were >1 tables in some files, but I never saw that to be the
					// case w/ the Idaho DHW .hyper files. So this could be rewritten if there was some table name
					// URL parameter. For now, we'll just return that there were != 1 table names as valid CSV.
					if (tableNames.size() != 1) {
						return "Tables in " + extractFilename + "\n" + tableNames.size();
					}

					for (TableName tableName : tableNames) {
						Result result = connection.executeQuery("SELECT * FROM " + tableName.toString());
						ResultSchema resultSchema = result.getSchema();

						List<ResultSchema.Column> columns = resultSchema.getColumns();

						csvBuilder.append(columns.stream().map(v -> v.getName().toString())
							.collect(Collectors.joining(",")));
						csvBuilder.append("\n");

						while (result.nextRow()) {
							csvBuilder.append(columns.stream().map(v -> getCsvCell(v, result, resultSchema))
								.collect(Collectors.joining(",")));
							csvBuilder.append("\n");
						}
					}
				}
			}

			// logger.info(builder.toString());
			return csvBuilder.toString();

		}
		finally {
			recursiveDeleteDir(tempDir);
		}

	}

	/**
	 * Java can't delete directories unless they're empty, so delete every file, then delete directory
	 */
	private void recursiveDeleteDir(File directory) {
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
}
