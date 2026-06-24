package org.eclipse.rdf4j.server;

import java.util.Objects;

import org.eclipse.rdf4j.common.app.AppConfiguration;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.lang.NonNull;

@Configuration
public class ServiceConfiguration {
	@NonNull
	@Bean(name = "commonAppConfig", initMethod = "init", destroyMethod = "destroy")
	public AppConfiguration commonAppConfig() {
		final AppConfiguration config = new AppConfiguration();
		config.setApplicationId("Server");
		config.setLongName("RDF4J Server");

		return config;
	}

	@NonNull
	@Autowired
	@Bean(name = "rdf4jRepositoryManager", initMethod = "init", destroyMethod = "shutDown")
	@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
	public RepositoryManager rdf4jRepositoryManager(
			@NonNull @Qualifier("commonAppConfig") final AppConfiguration appConfig) {
		Objects.requireNonNull(appConfig, "Application config was not properly initialized!");

		return new LocalRepositoryManager(appConfig.getDataDir());
	}

	@NonNull
	@Bean(name = "messageSource")
	public MessageSource messageSource() {
		final ResourceBundleMessageSource bundle = new ResourceBundleMessageSource();

		bundle.setBasenames(
				"org.eclipse.rdf4j.http.server.messages",
				"org.eclipse.rdf4j.common.webapp.system.messages",
				"org.eclipse.rdf4j.common.webapp.messages"
		);

		return bundle;
	}
}
