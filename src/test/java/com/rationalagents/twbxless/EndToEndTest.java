package com.rationalagents.twbxless;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A set of tests showing how twbxless works end-to-end. If these don't work, we don't have anything.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"HYPEREXEC=lib/hyper"})
public class EndToEndTest {

	private final String TWBX = "https://public.tableau.com/workbooks/Example_15896654403480.twb";

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	public void canGetFilenames() {
		assertEquals("filenames\nData/TableauTemp/TEMP_0kf7uk81qi1qyf18sg86d1m8pl9s.hyper",
			restTemplate.getForObject("http://localhost:" + port + "/filenames?url=" + TWBX, String.class));
	}

	@Test
	public void canGetData() {
		assertThat(
			restTemplate.getForObject("http://localhost:" + port + "/data?url=" + TWBX
				+ "&filename=Data/TableauTemp/TEMP_0kf7uk81qi1qyf18sg86d1m8pl9s.hyper", String.class))
			.contains("\"Date\",\"Animal Observed\",\"Animal\",\"Leg Count\"\n")
			.contains("2020-05-15,Frog,Frog,4\n");
	}
}
