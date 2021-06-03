# SPARQL-Readonly

This code is to be used in a different (your) spring-boot project.

For example add the following to your pom otherwise setup like any other 
spring-boot application

```

<dependency>
	<groupId>org.eclipse.rdf4j</groupId>
	<artifactId>rdf4j-http-server-readonly</artifactId>
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
@ComponentScan(basePackages = { "org.eclipse.rdf4j", "org.example" })
@Import(QueryResponder.class)
public class Server {
	@Bean
	public Repository getRepository(){
		MemoryStore store = ... ;//Configure progamatically your specific store
		SailRepository sailRepository = new SailRepository(store);
		sailRepository.init();
		retun sailRepository;
	}

	public static void main(String[] args) {
		SpringApplication.run(Server.class, args);
	}
}

```

And that is it, you have a single `/sparql` api endpoint on your spring-boot application
that provides readonly access to the store you have configured.

This allows the usuall docker image build etc.
