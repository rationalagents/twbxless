package com.rationalagents.hypersuck;

import com.tableau.hyperapi.Connection;
import com.tableau.hyperapi.HyperProcess;
import com.tableau.hyperapi.Result;
import com.tableau.hyperapi.ResultSchema;
import com.tableau.hyperapi.TableName;
import com.tableau.hyperapi.Telemetry;
import com.tableau.hyperapi.SchemaName;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.util.stream.Collectors.joining;

@RestController
public class Controller {

	@Value("${HYPEREXEC:/hyperapi/lib/hyper}")
	private String hyperExec;

	@RequestMapping(value="filenames", method = RequestMethod.GET)
	public String getFilenames(@RequestParam String url) throws IOException {
		File tempDir = FileUtils.createTempDir();
		try {

			StringBuilder csv = new StringBuilder();
			csv.append("filenames");
			csv.append("\n");

			try (ZipInputStream zis = new ZipInputStream(new URL(url).openStream())) {
				ZipEntry ze = zis.getNextEntry();
				while (ze != null) {
					String fileName = ze.getName();
					if (fileName.endsWith(".hyper")) {
						csv.append(fileName);
						csv.append("\n");
					}
					ze = zis.getNextEntry();
				}

				return csv.toString();
			}
		}
		finally {
			FileUtils.deleteTempDir(tempDir);
		}
	}

	@RequestMapping(value="data", method = RequestMethod.GET)
	public String getData(@RequestParam String url, @RequestParam String filename) throws IOException {

		File tempDir = FileUtils.createTempDir();
		try {
			String matchFilenameExtracted = null;

			try (ZipInputStream zis  = new ZipInputStream(new URL(url).openStream())) {
				ZipEntry ze = zis.getNextEntry();
				while (ze != null) {
					String name = ze.getName();
					if (name.endsWith(".hyper")) {

						if (name.endsWith(filename)) {
							String extractedFilename = tempDir.getAbsolutePath() + File.separator + name;
							FileUtils.extractFile(zis, extractedFilename);

							// Matched and extracted!
							matchFilenameExtracted = extractedFilename;
						}

					}
					ze = zis.getNextEntry();
				}
			}

			if (matchFilenameExtracted == null) {
				return "No .hyper file matching\n" + filename;
			}

			// Going from suck to Tableau!
			try (HyperProcess process = new HyperProcess(Path.of(hyperExec),
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
						CsvUtils.appendRow(columns.stream().map(v -> v.getName().toString()), csv);

						// Rows
						while (result.nextRow()) {
							CsvUtils.appendRow(columns.stream().map(v -> HyperUtils.getString(v, result, resultSchema)), csv);
						}
					}

					return csv.toString();
				}
			}
		}
		finally {
			FileUtils.deleteTempDir(tempDir);
		}
	}
}
