package com.rationalagents.twbxless;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(Application.class);

		// Be more conventional, supporting "PORT" over "server.port"/"SERVER_PORT". If not set default to 8080.
		// TODO: feature request for Spring Boot project?
		app.setDefaultProperties(Map.of(
			"server.port",
			System.getenv().containsKey("PORT") ? System.getenv("PORT") : "8080"));

		app.setBannerMode(Banner.Mode.OFF);
		app.run(args);
	}
}
