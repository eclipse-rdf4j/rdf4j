package org.eclipse.rdf4j.server;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@Experimental
@SpringBootApplication
@PropertySource(
		value = "classpath:org/eclipse/rdf4j/http/server/application.properties", ignoreResourceNotFound = true, name = "org.springframework.context.support.PropertySourcesPlaceholderConfigurer"
)
public class Application {
	public static void main(final String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
