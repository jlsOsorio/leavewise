package org.altechego.leavewise;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LeavewiseApplication {

	public static void main(String[] args) {
		SpringApplication.run(LeavewiseApplication.class, args);
	}

}
