# SPARQL-Readonly

This code is to be used in a different (your) spring-boot project.

For example add the following to your pom otherwise setup like any other 
spring-boot application

```

<dependency>
	<groupId>org.eclipse.rdf4j</groupId>
	<artifactId>rdf4j-spring-boot-sparql-web</artifactId>
	<version>${rdf4j.version}</version>
</dependency>

```

In your spring-boot application

```java
package org.example;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@ComponentScan(basePackages = {"org.eclipse.rdf4j", "org.example"})
@Import(QueryResponder.class)
public class Server {
	@Bean
		MemoryStore store = ... ;//Configure progamatically your specific store
	public Repository getRepository() {
		SailRepository sailRepository = new SailRepository(store);
		sailRepository.init();
		return sailRepository;
	}

	public static void main(String[] args) {
		SpringApplication.run(Server.class, args);
	}
}

```

And that is it, you have a single `/sparql` api endpoint on your spring-boot application
that provides readonly access to the store you have configured.

This allows the usuall docker image build and more that spring-boot provides.
