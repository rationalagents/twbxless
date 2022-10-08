package com.rationalagents.twbxless;

import com.tableau.hyperapi.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.zip.ZipInputStream;

import static java.util.stream.Collectors.toList;

@Service
public class HyperService {

	Logger logger = LoggerFactory.getLogger(HyperService.class);

	@Value("${HYPEREXEC:/hyperapi/lib/hyper}")
	String hyperExec;

	@Value("${URLPREFIX:https://public.tableau.com/}")
	String urlPrefix;

	public List<String> getFilenames(String url) {
		var result = new ArrayList<String>();
		extract(url, (name) -> {
			if (name.endsWith(".hyper")) result.add(name);
			return false;
		});
		return result;
	}

	/**
	 * Returns map of datasource name to filename. If no name, just uses filename.
	 */
	public Map<String,String> getDataSources(String url) {
		var extractedFileName = extract(url, (name) -> name.endsWith(".twb"));
		if (extractedFileName == null) {
			throw new RuntimeException("No .twb file in " + url);
		}

		try {
			var result = new HashMap<String, String>();

			try (var stream =  new FileInputStream(extractedFileName)) {
				var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
				var extracts = (NodeList)XPathFactory.newInstance().newXPath()
					.evaluate("//extract/connection[@dbname]" , doc, XPathConstants.NODESET);

				for (int i = 0; i < extracts.getLength(); i++) {
					var extractNode = extracts.item(i);
					var filename = getValueOrNull(extractNode, "dbname");
					var caption = getValueOrNull(extractNode.getParentNode().getParentNode(), "caption");
					result.put(caption == null ? filename : caption, filename);
				}

				return result;
			} catch (IOException | ParserConfigurationException | SAXException | XPathExpressionException e) {
				throw new RuntimeException(e);
			}
		} finally {
			tryDeleteFile(extractedFileName);
		}
	}

	private String getValueOrNull(Node node, String attributeName) {
		var attribute = node.getAttributes().getNamedItem(attributeName);
		return attribute == null ? null : attribute.getNodeValue();
	}

	public List<List<String>> getDataByName(String url, String name) {
		var dataSources = getDataSources(url);

		if (!dataSources.containsKey(name)) {
			throw new DataException("No name match, valid names follow" , dataSources.keySet());
		}

		return getDataByFilename(url, dataSources.get(name));
	}

	/**
	 * Returns data from the Hyper-formatted file specified by fileName, stored within .twbx file at
	 * provided url.
	 */
	public List<List<String>> getDataByFilename(String url, String fileName) {

		// For simplification and easier testing - in case paths in .twbx do change on save - we check
		// for ends-with match only.
		var extractedFileName = extract(url, (file) -> file.endsWith(fileName));

		if (extractedFileName == null) {
			throw new DataException("No file match", List.of(fileName));
		}

		try {
			try (var process = new HyperProcess(Path.of(hyperExec),
				Telemetry.DO_NOT_SEND_USAGE_DATA_TO_TABLEAU,
				"",
				// keep logs from filling container (https://help.tableau.com/current/api/hyper_api/en-us/reference/sql/loggingsettings.html);
				Map.of("log_config", ""))) {

				try (var connection = new Connection(process.getEndpoint(), extractedFileName)) {

					// We could support > table/schema here, see issue #4.
					var tableNames = connection.getCatalog().getSchemaNames().stream()
						.flatMap(v -> connection.getCatalog().getTableNames(v).stream())
						.map(TableName::toString)// e.g. "Extract"."Extract"
						.collect(toList());
					if (tableNames.size() != 1) {
						throw new DataException("Found ambiguous number of schemas/tables", tableNames);
					}

					var tableName = tableNames.get(0);
					var resultSet = connection.executeQuery("SELECT * FROM " + tableName);
					var resultSchema = resultSet.getSchema();
					var columns = resultSchema.getColumns();

					var result = new ArrayList<List<String>>();
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
			tryDeleteFile(extractedFileName);
		}
	}

	/**
	 * Retrieve .twbx file at URL, then pass each file name within to tester.
	 * If tester would like to extract it, return true, otherwise continues.
	 * Returns the extracted temp filename, or null if tester never returned true.
	 */
	public String extract(String url, Predicate<String> tester) {
		throwIfWrongPrefix(url);

		try (var zis = new ZipInputStream(new URL(url).openStream())) {
			var ze = zis.getNextEntry();
			while (ze != null) {
				if (tester.test(ze.getName())) {
					return FileUtils.extractFile(zis);
				}
				ze = zis.getNextEntry();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return null;
	}

	/**
	 * Throws if URL does not start with the configured prefix. This is to prevent
	 * the service from being used to crawl things its not meant for.
	 */
	private void throwIfWrongPrefix(String url) {
		if (!url.startsWith(urlPrefix)) {
			throw new RuntimeException("url must start with " + urlPrefix);
		}
	}

	/**
	 * If fileName is not null, try to delete the file. warns in log if didn't delete.
	 */
	private void tryDeleteFile(String fileName) {
		if (fileName != null) {
			try {
				Files.delete(Path.of(fileName));
			} catch (IOException e) {
				logger.warn("Didn't delete " + fileName, e);
			}
		}
	}

}
