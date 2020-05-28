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
	public void expectExceptionDataIfWrongFilename() {
		DataException thrown = assertThrows(DataException.class,
			() -> service.getDataByFilename("https://public.tableau.com/workbooks/Example_15896654403480.twb", "DNE.hyper"));

		assertEquals("No file match", thrown.getMessage());
		assertEquals(List.of("DNE.hyper"), thrown.getExtraData());
	}

	@Test
	public void expectExceptionDataIfWrongName() {
		DataException thrown = assertThrows(DataException.class,
			() -> service.getDataByName("https://public.tableau.com/workbooks/Example_15896654403480.twb", "DNE"));

		assertEquals("No name match, valid names follow", thrown.getMessage());
		assertEquals(List.of("Data Source 1"), thrown.getExtraData());
	}

	@Test
	public void expectExceptionIfUrlOutsideAllowed() {
		RuntimeException thrown = assertThrows(RuntimeException.class,
			() -> service.getFilenames("https://badstuff.com"));

		assertEquals("url must start with https://public.tableau.com/", thrown.getMessage());

		thrown = assertThrows(RuntimeException.class,
			() -> service.getDataByFilename("https://badstuff.com", "doesntmatter"));

		assertEquals("url must start with https://public.tableau.com/", thrown.getMessage());
	}
}
