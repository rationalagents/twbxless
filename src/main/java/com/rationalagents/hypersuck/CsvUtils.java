package com.rationalagents.hypersuck;

import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

public class CsvUtils {

	/**
	 * Collects stream as a new row of CSV
	 */
	public static void appendRow(Stream stream, StringBuilder csv) {
		csv.append(stream.collect(joining(",")));
		csv.append("\n");
	}
}
