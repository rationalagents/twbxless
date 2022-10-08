package com.rationalagents.twbxless;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

@RestController
public class Controller {

	private final HyperService hyperService;

	public Controller(HyperService hyperService) {
		this.hyperService = hyperService;
	}

	@RequestMapping(value="datasources", method = RequestMethod.GET, produces="text/plain")
	public String getDataSources(@RequestParam String url) {
		var result = new ArrayList<List<String>>();
		result.add(List.of("name"));
		hyperService.getDataSources(url).forEach((k, v) -> result.add(List.of(k)));
		return Csv.toCsv(result);
	}

	@RequestMapping(value="data", method = RequestMethod.GET, produces="text/plain", params = {"url", "name"})
	public String getDataByName(@RequestParam String url, @RequestParam String name) {
		try {
			return Csv.toCsv(hyperService.getDataByName(url, name));
		} catch (DataException e) {
			return Csv.toCsv(e);
		}
	}

	/**
	 * Nicer here would be message converter
	 */
	private static class Csv {
		static String toCsv(String singleHeader, List<String> singleColumn) {
			var list = new ArrayList<List<String>>();
			list.add(List.of(singleHeader));
			singleColumn.forEach(v -> list.add(List.of(v)));
			return toCsv(list);
		}

		static String toCsv(DataException e) {
			return toCsv(e.getMessage(), e.getExtraData());
		}

		static String toCsv(List<List<String>> rows) {
			var writer = new StringWriter();
			var csvWriter = new CsvListWriter(writer, CsvPreference.STANDARD_PREFERENCE);

			try {
				for (List<String> row : rows) {
					csvWriter.write(row);
				}
				csvWriter.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return writer.toString();
		}
	}
}
