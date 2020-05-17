package com.rationalagents.twbxless;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.joining;

@RestController
public class Controller {

	private final HyperService hyperService;

	public Controller(HyperService hyperService) {
		this.hyperService = hyperService;
	}

	@RequestMapping(value="filenames", method = RequestMethod.GET)
	public String getFilenames(@RequestParam String url) {
		return Csv.toCsv("filenames", hyperService.getFilenames(url));
	}

	@RequestMapping(value="data", method = RequestMethod.GET)
	public String getData(@RequestParam String url, @RequestParam String filename) {

		try {
			return Csv.toCsv(hyperService.getData(url, filename));
		} catch (DataException e) {
			return Csv.toCsv(e);
		}
	}

	/**
	 * Factored these out and grouped so I could write tests on what I had first, but
	 * these are issue #1.
	 */
	private static class Csv {
		static String toCsv(String singleHeader, List<String> singleColumn) {
			List<List<String>> response = new ArrayList<>();
			response.add(List.of(singleHeader));
			for (String row : singleColumn) {
				response.add(List.of(row));
			}
			return toCsv(response);
		}

		static String toCsv(DataException e) {
			return toCsv(e.getMessage(), e.getExtraData());
		}

		static String toCsv(List<List<String>> rows) {
			return rows.stream()
				.map(v -> String.join(",", v))
				.collect(joining("\n"));
		}
	}
}
