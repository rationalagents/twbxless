package com.rationalagents.twbxless;

import com.tableau.hyperapi.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.util.stream.Collectors.toList;

@Service
public class HyperService {

	Logger logger = LoggerFactory.getLogger(HyperService.class);

	@Value("${HYPEREXEC:/hyperapi/lib/hyper}")
	private String hyperExec;

	public List<String> getFilenames(String url ) {
		List<String> result = new ArrayList<>();

		try (ZipInputStream zis = new ZipInputStream(new URL(url).openStream())) {
			ZipEntry ze = zis.getNextEntry();
			while (ze != null) {
				String name = ze.getName();
				if (name.endsWith(".hyper")) {
					result.add(name);
				}
				ze = zis.getNextEntry();
			}

			return result;

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public List<List<String>> getData(String url, String fileName) {
		String extractedFileName = null;

		try {
			try (ZipInputStream zis  = new ZipInputStream(new URL(url).openStream())) {
				ZipEntry ze = zis.getNextEntry();
				while (ze != null) {
					String name = ze.getName();
					if (name.endsWith(fileName)) {
						extractedFileName = FileUtils.extractFile(zis);
					}
					ze = zis.getNextEntry();
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			if (extractedFileName == null) {
				throw new DataException("No .hyper file matching", List.of(fileName));
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
						throw new DataException("Found multiple schemas/tables", tableNames);
					}

					String tableName = tableNames.get(0);
					Result resultSet = connection.executeQuery("SELECT * FROM " + tableName);
					ResultSchema resultSchema = resultSet.getSchema();
					List<ResultSchema.Column> columns = resultSchema.getColumns();

					List<List<String>> result = new ArrayList<>();
					result.add(columns.stream().map(v -> v.getName().toString()).collect(toList()));
					while (resultSet.nextRow()) {
						result.add(columns.stream().map(v -> HyperUtils.getString(v, resultSet, resultSchema)).collect(Collectors.toList()));
					}
					return result;
				}
			}
		}
		finally {
			if (extractedFileName != null) {
				try {
					Files.delete(Path.of(extractedFileName));
				} catch (IOException e) {
					logger.warn("Didn't delete " + extractedFileName, e);
				}
			}
		}

	}


}
