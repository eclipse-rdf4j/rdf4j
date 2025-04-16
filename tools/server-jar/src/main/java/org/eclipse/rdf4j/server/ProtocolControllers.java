package org.eclipse.rdf4j.server;

import org.eclipse.rdf4j.http.server.protocol.ProtocolController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

@Configuration
public class ProtocolControllers {
	@NonNull
	@Bean(name = "rdf4jProtocolController")
	public ProtocolController rdf4jProtocolController() {
		return new ProtocolController();
	}
}
