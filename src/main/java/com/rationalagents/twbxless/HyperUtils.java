package com.rationalagents.twbxless;

import com.tableau.hyperapi.Result;
import com.tableau.hyperapi.ResultSchema;

public class HyperUtils {

	/**
	 * Gets next value from the Result, similar to {@link Result#getString(int)} for specified column in result schema,
	 * as a String appropriate for CSV output.
	 *
	 * For types I didn't expect to handle or know how to handle, the returned value will start with "?" then will
	 * indicate the unknown type.
	 *
	 * Unfortunately Tableau didn't seem to have a version of this, or I didn't find it.
	 *
	 * Might consider trying {@link Result#getObject(int)}.
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
