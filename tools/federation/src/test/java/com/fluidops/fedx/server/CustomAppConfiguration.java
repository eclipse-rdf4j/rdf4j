package com.fluidops.fedx.server;

import java.io.IOException;

import org.eclipse.rdf4j.common.app.AppConfiguration;
import org.eclipse.rdf4j.common.app.logging.base.AbstractLogConfiguration;
import org.eclipse.rdf4j.common.logging.LogReader;

/**
 * A bean for configuring the RDF4J Http Server webapp. Makes sure to initialize
 * the logging framework properly.
 * 
 * See <i>build/test/rdf4j-server/WEB-INF/rdf4j-http-server-servlet.xml</i>
 * 
 * @author andreas.schwarte
 *
 */
public class CustomAppConfiguration extends AppConfiguration {

	@Override
	public void load() throws IOException {
		super.load();
		getProperties().put("feature.logging.impl",
				"com.fluidops.fedx.server.CustomAppConfiguration$FedXLoggingConfiguration");
	}

	public static class FedXLoggingConfiguration extends AbstractLogConfiguration {

		public FedXLoggingConfiguration() throws IOException {
			super();
		}

		@Override
		public LogReader getLogReader(String appender) {
			return null;
		}

		@Override
		public LogReader getDefaultLogReader() {
			return null;
		}

		@Override
		public void init() throws IOException {

		}

		@Override
		public void load() throws IOException {

		}

		@Override
		public void save() throws IOException {

		}

		@Override
		public void destroy() throws IOException {

		}

	}
}
