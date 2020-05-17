package com.rationalagents.twbxless;

import com.tableau.hyperapi.Result;
import com.tableau.hyperapi.ResultSchema;

public class HyperUtils {

	/**
	 * Gets next value from the Result, similar to {@link Result#getString(int)} for specified column in result schema,
	 * as a String appropriate for CSV output.
	 *
	 * For types that didn't seem appropriate to convert, the returned value will be "TYPE?"
	 */
	static String getString(ResultSchema.Column column, Result result, ResultSchema schema) {
		int position = schema.getColumnPositionByName(column.getName()).orElseThrow();

		if (result.isNull(position)) return "";

		return switch(column.getTypeTag()) {
			case TEXT, VARCHAR, CHAR, JSON,
				BOOL, OID, SMALL_INT, INT, BIG_INT, DOUBLE, NUMERIC,
				DATE, TIME, TIMESTAMP -> result.getObject(position).toString();
			// See README.md
			case UNSUPPORTED, BYTES, INTERVAL, TIMESTAMP_TZ, GEOGRAPHY-> "TYPE?";
		};
	}
}
