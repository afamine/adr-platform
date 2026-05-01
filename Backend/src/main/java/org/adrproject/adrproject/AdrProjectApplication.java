package com.adrplatform.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.adrplatform")
@EnableJpaRepositories(basePackages = "com.adrplatform")
@EntityScan(basePackages = "com.adrplatform")
public class AdrProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(AdrProjectApplication.class, args);
	}

}
