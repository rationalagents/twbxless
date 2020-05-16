package com.rationalagents.twbxless;

import java.util.List;

public class DataException extends RuntimeException {

	private List<String> extraData;

	public DataException(String message, List<String> extraData) {
		super(message);
		this.extraData = extraData;
	}

	public List<String> getExtraData() {
		return extraData;
	}


}
