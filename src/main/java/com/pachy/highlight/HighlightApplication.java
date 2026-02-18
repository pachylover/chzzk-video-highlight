package com.pachy.highlight;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class HighlightApplication {

	public static void main(String[] args) {
		SpringApplication.run(HighlightApplication.class, args);
	}

}
