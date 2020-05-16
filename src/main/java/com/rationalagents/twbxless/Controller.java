package com.rationalagents.twbxless;

import com.tableau.hyperapi.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.util.stream.Collectors.toList;

@RestController
public class Controller {

	@Value("${HYPEREXEC:/hyperapi/lib/hyper}")
	private String hyperExec;

	@RequestMapping(value="filenames", method = RequestMethod.GET)
	public String getFilenames(@RequestParam String url) throws IOException {
		StringBuilder csv = new StringBuilder();
		csv.append("filenames");
		csv.append("\n");

		try (ZipInputStream zis = new ZipInputStream(new URL(url).openStream())) {
			ZipEntry ze = zis.getNextEntry();
			while (ze != null) {
				String name = ze.getName();
				if (name.endsWith(".hyper")) {
					csv.append(name);
					csv.append("\n");
				}
				ze = zis.getNextEntry();
			}

			return csv.toString();
		}
	}

	@RequestMapping(value="data", method = RequestMethod.GET)
	public String getData(@RequestParam String url, @RequestParam String filename) throws IOException {

		String extractedFileName = null;

		try {
			try (ZipInputStream zis  = new ZipInputStream(new URL(url).openStream())) {
				ZipEntry ze = zis.getNextEntry();
				while (ze != null) {
					String name = ze.getName();
					if (name.endsWith(filename)) {
						extractedFileName = FileUtils.extractFile(zis);
					}
					ze = zis.getNextEntry();
				}
			}

			if (extractedFileName == null) {
				return "No .hyper file matching\n" + filename;
			}

			// Going from suck to Tableau!
			try (HyperProcess process = new HyperProcess(Path.of(hyperExec),
				Telemetry.DO_NOT_SEND_USAGE_DATA_TO_TABLEAU,
				"",
				// keep logs from filling container (https://help.tableau.com/current/api/hyper_api/en-us/reference/sql/loggingsettings.html);
				Map.of("log_config", ""))) {

				try (Connection connection = new Connection(process.getEndpoint(), extractedFileName)) {

					// We could support > table/schema here, see issue #4.
					List<String> tableNames = connection.getCatalog().getSchemaNames().stream()
						.flatMap(v -> connection.getCatalog().getTableNames(v).stream())
						.map(TableName::toString)// e.g. "Extract"."Extract"
						.collect(toList());
					if (tableNames.size() != 1) {
						return "Found multiple schemas/tables\n" + String.join("\n", tableNames);
					}

					String tableName = tableNames.get(0);
					Result result = connection.executeQuery("SELECT * FROM " + tableName);
					ResultSchema resultSchema = result.getSchema();
					List<ResultSchema.Column> columns = resultSchema.getColumns();

					StringBuilder csv = new StringBuilder();
					CsvUtils.appendRow(columns.stream().map(v -> v.getName().toString()), csv);
					while (result.nextRow()) {
						CsvUtils.appendRow(columns.stream().map(v -> HyperUtils.getString(v, result, resultSchema)), csv);
					}
					return csv.toString();
				}
			}
		}
		finally {
			if (extractedFileName != null) {
				Files.delete(Path.of(extractedFileName));
			}
		}
	}
}
