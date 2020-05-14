package com.rationalagents.hypersuck;

import com.tableau.hyperapi.Result;
import com.tableau.hyperapi.ResultSchema;
import com.tableau.hyperapi.SqlType;

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
		int position = schema.getColumnPositionByName(column.getName()).getAsInt();
		if (column.getType().equals(SqlType.bigInt())) {
			return result.isNull(position) ? "" : Long.toString(result.getLong(position));
		} else if (column.getType().equals(SqlType.doublePrecision())) {
			return result.isNull(position) ? "" : Double.toString(result.getDouble(position));
		} else if (column.getType().equals(SqlType.text())) {
			return result.isNull(position) ? "" : result.getString(position);
		} else if (column.getType().equals(SqlType.date())) {
			return result.isNull(position) ? "" : result.getLocalDate(position).toString();
		} else {
			// See README.md
			return result.isNull(position) ? "" : "TYPE?";
		}
	}
}
