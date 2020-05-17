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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.util.stream.Collectors.toList;

@Service
public class HyperService {

	Logger logger = LoggerFactory.getLogger(HyperService.class);

	@Value("${HYPEREXEC:/hyperapi/lib/hyper}")
	String hyperExec;

	@Value("${URLPREFIX:https://public.tableau.com/}")
	String urlPrefix;

	public void throwIfWrongPrefix(String url) {
		if (!url.startsWith(urlPrefix)) throw new RuntimeException("url must start with " + urlPrefix);
	}

	public List<String> getFilenames(String url) {
		throwIfWrongPrefix(url);

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

	/**
	 * Returns data for specified filename within URL.
	 *
	 * For convenience (because filenames seem pseudo-random, trying to work out an alternative),
	 * it goes with first ends-with match.
	 */
	public List<List<String>> getData(String url, String fileName) {
		throwIfWrongPrefix(url);

		String extractedFileName = null;

		try {
			try (ZipInputStream zis  = new ZipInputStream(new URL(url).openStream())) {
				ZipEntry ze = zis.getNextEntry();
				while (ze != null) {
					String name = ze.getName();
					if (name.endsWith(".hyper") && name.endsWith(fileName)) /* allow ends-with to match */ {
						extractedFileName = FileUtils.extractFile(zis);
						ze = null; // exit
					} else {
						ze = zis.getNextEntry();
					}
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			if (extractedFileName == null) {
				throw new DataException("No file matching", List.of(fileName));
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
						throw new DataException("Found ambiguous number of schemas/tables", tableNames);
					}

					String tableName = tableNames.get(0);
					Result resultSet = connection.executeQuery("SELECT * FROM " + tableName);
					ResultSchema resultSchema = resultSet.getSchema();
					List<ResultSchema.Column> columns = resultSchema.getColumns();

					List<List<String>> result = new ArrayList<>();
					result.add(columns.stream()
						.map(v -> v.getName().getUnescaped())
						.collect(toList()));
					while (resultSet.nextRow()) {
						result.add(columns.stream()
							.map(v -> HyperUtils.getString(v, resultSet, resultSchema))
							.collect(toList()));
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
