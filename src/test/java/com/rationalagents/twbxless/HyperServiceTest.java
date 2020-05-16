package com.rationalagents.twbxless;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@TestPropertySource(properties = {"HYPEREXEC=lib/hyper"})
public class HyperServiceTest {

	@Autowired
	HyperService service;

	@Test
	public void expectExceptionIfMissingFilename() {
		DataException thrown = assertThrows(DataException.class,
			() -> service.getData("https://public.tableau.com/workbooks/Example_15896654403480.twb", "DNE.hyper"));

		assertEquals("No file matching", thrown.getMessage());
		assertEquals(List.of("DNE.hyper"), thrown.getExtraData());
	}
}
