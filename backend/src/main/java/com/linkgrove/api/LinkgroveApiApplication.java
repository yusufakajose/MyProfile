package com.linkgrove.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class LinkgroveApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(LinkgroveApiApplication.class, args);
	}

}
