package com.rationalagents.twbxless;

import java.util.Collection;
import java.util.List;

public class DataException extends RuntimeException {

	private final List<String> extraData;

	public DataException(String message, Collection<String> extraData) {
		super(message);
		this.extraData = List.copyOf(extraData);
	}

	public List<String> getExtraData() {
		return extraData;
	}
}
