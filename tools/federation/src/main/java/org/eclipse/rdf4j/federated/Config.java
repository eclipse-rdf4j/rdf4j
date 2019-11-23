/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.rdf4j.federated.endpoint.Endpoint;
import org.eclipse.rdf4j.federated.endpoint.provider.ProviderUtil;
import org.eclipse.rdf4j.federated.exception.FedXException;
import org.eclipse.rdf4j.federated.exception.FedXRuntimeException;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration properties for FedX based on a properties file. Prior to using this configuration
 * {@link #initialize(File)} must be invoked with the location of the properties file.
 * 
 * @author Andreas Schwarte
 *
 */
public class Config {

	protected static final Logger log = LoggerFactory.getLogger(Config.class);

	private static Config instance = null;

	public static Config getConfig() {
		if (!isInitialized())
			throw new FedXRuntimeException("Config not initialized. Call Config.initialize() first.");
		return instance;
	}

	protected static void reset() {
		instance = null;
	}

	/**
	 * Initialize the configuration with default settings.
	 */
	public static void initialize() throws FedXException {
		initialize((File) null);
	}

	/**
	 * Initialize the configuration with the specified properties file.
	 * 
	 * @param fedxConfig the optional location of the properties file. If <code>null</code>, the default configuration
	 *                   is used.
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	public static void initialize(String fedxConfig) throws FedXException {
		File file = fedxConfig != null ? new File(fedxConfig) : null;
		initialize(file);
	}

	/**
	 * Initialize the configuration with the specified properties file.
	 * 
	 * @param fedxConfig the optional location of the properties file. If <code>null</code>, the default configuration
	 *                   is used.
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	public static synchronized void initialize(File fedxConfig) throws FedXException {
		if (isInitialized())
			throw new FedXRuntimeException("Config is already initialized.");
		instance = new Config();
		instance.init(fedxConfig);
	}

	static synchronized boolean isInitialized() {
		return instance != null;
	}

	private Properties props;

	private Config() {
		props = new Properties();
	}

	private void init(File configFile) throws FedXException {
		if (configFile == null) {
			log.debug("No configuration file specified. Using default config initialization.");
			return;
		}

		if (!configFile.isFile()) {
			throw new FedXException("FedX config file does not exist: " + configFile);
		}
		log.info("FedX Configuration initialized from file '" + configFile + "'.");
		try (FileInputStream in = new FileInputStream(configFile)) {
			props.load(in);
		} catch (IOException e) {
			throw new FedXException(
					"Failed to initialize FedX configuration with " + configFile + ": " + e.getMessage());
		}
	}

	public String getProperty(String propertyName) {
		return props.getProperty(propertyName);
	}

	public String getProperty(String propertyName, String def) {
		return props.getProperty(propertyName, def);
	}

	/**
	 * Flag to enable/disable JMX monitoring. Default=false
	 * 
	 * @return whether JMX is enabled
	 */
	public boolean isEnableJMX() {
		return Boolean.parseBoolean(props.getProperty("monitoring.enableJMX", "false"));
	}

	/**
	 * Whether to use a singleton {@link RepositoryConnection} per {@link Endpoint}. Default: false
	 * 
	 * If not set, a fresh {@link RepositoryConnection} is used for each triple store interaction.
	 * 
	 * @return indicator whether a singleton connection shall be used per endpoint
	 */
	public boolean useSingletonConnectionPerEndpoint() {
		return Boolean.parseBoolean(props.getProperty("endpoint.useSingletonConnection", "false"));
	}

	/**
	 * If enabled, repository connections are validated by
	 * {@link ProviderUtil#checkConnectionIfConfigured(org.eclipse.rdf4j.repository.Repository)} prior to adding the
	 * endpoint to the federation. If validation fails, an error is thrown to the user.
	 * 
	 * @return whether repository connections are validated
	 */
	public boolean isValidateRepositoryConnections() {
		return Boolean.parseBoolean(props.getProperty("validateRepositoryConnections", "false"));
	}

	/**
	 * The debug mode for worker scheduler, the scheduler prints usage stats regularly if enabled
	 * 
	 * @return whether the debug mode for the scheduler is enabled
	 */
	@Deprecated
	public boolean isDebugWorkerScheduler() {
		return Boolean.parseBoolean(props.getProperty("debugWorkerScheduler", "false"));
	}

	/**
	 * Set some property at runtime
	 * 
	 * @param key
	 * @param value
	 */
	public void set(String key, String value) {
		props.setProperty(key, value);
	}
}
