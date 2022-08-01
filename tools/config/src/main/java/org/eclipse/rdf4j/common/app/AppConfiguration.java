/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.common.app;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;

import org.eclipse.rdf4j.RDF4J;
import org.eclipse.rdf4j.common.app.config.Configuration;
import org.eclipse.rdf4j.common.app.logging.LogConfiguration;
import org.eclipse.rdf4j.common.app.net.ProxySettings;
import org.eclipse.rdf4j.common.app.util.ConfigurationUtil;
import org.eclipse.rdf4j.common.platform.PlatformFactory;

/**
 * @author Herko ter Horst
 */
public class AppConfiguration implements Configuration {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final String APP_CONFIG_FILE = "application.properties";
	private static final String DEFAULT_PREFIX = "RDF4J";
	private static final String DEFAULT_LOGGING = "org.eclipse.rdf4j.common.app.logging.logback.LogbackConfiguration";

	/*-----------*
	 * Variables *
	 *-----------*/

	private String applicationId;
	private String longName;
	private String fullName;
	private AppVersion version;

	private String[] commandLineArgs;

	private String dataDirName;
	private File dataDir;

	private LogConfiguration loggingConfiguration;
	private ProxySettings proxySettings;
	private Properties properties;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Create a new, uninitialized application configuration.
	 */
	public AppConfiguration() {
		super();
	}

	/**
	 * Create the application configuration.
	 *
	 * @param applicationId the ID of the application
	 */
	public AppConfiguration(final String applicationId) {
		this();
		setApplicationId(applicationId);
	}

	/**
	 * Create the application configuration.
	 *
	 * @param applicationId the ID of the application
	 * @param version       the application's version
	 */
	public AppConfiguration(final String applicationId, final AppVersion version) {
		this(applicationId);
		setVersion(version);
	}

	/**
	 * Create the application configuration.
	 *
	 * @param applicationId the ID of the application
	 * @param longName      the long name of the application
	 */
	public AppConfiguration(final String applicationId, final String longName) {
		this(applicationId);
		setLongName(longName);
	}

	/**
	 * Create the application configuration.
	 *
	 * @param applicationId the ID of the application
	 * @param longName      the long name of the application
	 * @param version       the application's version
	 */
	public AppConfiguration(final String applicationId, final String longName, final AppVersion version) {
		this(applicationId, version);
		setLongName(longName);
	}

	/*---------*
	 * Methods *
	 ----------*/

	@Override
	public void load() throws IOException {
		// load from resource
		properties = ConfigurationUtil.loadConfigurationProperties(APP_CONFIG_FILE, null);
		// load from properties file
		File f = Paths.get(getDataDir().toString(), Configuration.DIR, APP_CONFIG_FILE).toFile();
		properties = ConfigurationUtil.loadConfigurationProperties(f, properties);
	}

	@Override
	public void save() throws IOException {
		if (loggingConfiguration != null) {
			loggingConfiguration.save();
		}
		// save to properties file
		if (properties != null) {
			File f = Paths.get(getDataDir().toString(), Configuration.DIR, APP_CONFIG_FILE).toFile();
			ConfigurationUtil.saveConfigurationProperties(properties, f, false);
		}
		proxySettings.save();
	}

	@Override
	public void init() throws IOException {
		this.init(true);
	}

	/**
	 * Initialize configuration and proxy settings, optionally load (logback) logging
	 *
	 * @param loadLogConfig load logging configuration
	 * @throws IOException
	 */
	public void init(final boolean loadLogConfig) throws IOException {
		if (longName == null) {
			setLongName(DEFAULT_PREFIX + " " + applicationId);
		}
		setFullName();
		configureDataDir();
		load();
		if (loadLogConfig) {
			try {
				loggingConfiguration = loadLogConfiguration();
				loggingConfiguration.setBaseDir(getDataDir());
				loggingConfiguration.setAppConfiguration(this);
				loggingConfiguration.init();
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		proxySettings = new ProxySettings(getDataDir());
		proxySettings.init();
		save();
	}

	@Override
	public void destroy() throws IOException {
		loggingConfiguration.destroy();
		// proxySettings.destroy();
	}

	/**
	 * Get the name of the application (e.g. "AutoFocus" or "Metadata Server").
	 *
	 * @return the name of the application
	 */
	public String getApplicationId() {
		return applicationId;
	}

	/**
	 * Set the application ID string
	 *
	 * @param applicationId string
	 */
	public final void setApplicationId(final String applicationId) {
		this.applicationId = applicationId;
	}

	/**
	 * Set the name of the data directory
	 *
	 * @param dataDirName
	 */
	public void setDataDirName(final String dataDirName) {
		this.dataDirName = dataDirName;
	}

	/**
	 * Get the long name of the application (e.g. "Aduna AutoFocus" or "OpenRDF Sesame Server").
	 *
	 * @return the long name of the application
	 */
	public String getLongName() {
		return longName;
	}

	/**
	 * Set the long name of the application.
	 *
	 * @param longName the new name
	 */
	public final void setLongName(final String longName) {
		this.longName = longName;
	}

	/**
	 * Get the full name of the application, which consists of the long name and the version number (e.g. "Aduna
	 * AutoFocus 4.0-beta1" or "OpenRDF Sesame Webclient 2.0")
	 *
	 * @return the full name of the application
	 */
	public String getFullName() {
		return fullName;
	}

	/**
	 * Set full name based upon long name and version
	 */
	private void setFullName() {
		this.fullName = longName;
		if (version != null) {
			fullName = fullName + " " + version.toString();
		}
	}

	/**
	 * Get the version of the application.
	 *
	 * @return the version of the application
	 */
	public AppVersion getVersion() {
		if (version == null) {
			version = AppVersion.parse(RDF4J.getVersion());
		}
		return version;
	}

	/**
	 * Set the version of the application.
	 *
	 * @param version the new version
	 */
	public final void setVersion(final AppVersion version) {
		this.version = version;
		this.fullName = longName + " " + version.toString();
	}

	/**
	 * Get the command line arguments of the application.
	 *
	 * @return A String array, as (typically) specified to the main method.
	 */
	public String[] getCommandLineArgs() {
		return (String[]) commandLineArgs.clone();
	}

	/**
	 * Set the command line arguments specified to the application.
	 *
	 * @param args A String array containing the arguments as specified to the main method.
	 */
	public void setCommandLineArgs(final String[] args) {
		this.commandLineArgs = (String[]) args.clone();
	}

	/**
	 * Get the data directory as File
	 *
	 * @return data directory
	 */
	public File getDataDir() {
		return dataDir;
	}

	/**
	 * Get logging configuration
	 *
	 * @return log configuration
	 */
	public LogConfiguration getLogConfiguration() {
		return loggingConfiguration;
	}

	/**
	 * Get proxy settings
	 *
	 * @return proxy settings
	 */
	public ProxySettings getProxySettings() {
		return proxySettings;
	}

	/**
	 * Set proxy settings
	 *
	 * @param proxySettings proxy settings
	 */
	public void setProxySettings(final ProxySettings proxySettings) {
		this.proxySettings = proxySettings;
	}

	/**
	 * Configure the data dir. Determination of the data dir might be deferred to Platform.
	 */
	private void configureDataDir() {
		if (dataDirName != null) {
			dataDirName = dataDirName.trim();
			if (!("".equals(dataDirName))) {
				final File dataDirCandidate = new File(dataDirName);
				dataDirCandidate.mkdirs();
				// change data directory if the previous code was successful
				dataDir = (dataDirCandidate.canRead() && dataDirCandidate.canWrite()) ? dataDirCandidate : dataDir;
			}
		}
		if (dataDir == null) {
			dataDir = PlatformFactory.getPlatform().getApplicationDataDir(applicationId);
		}
	}

	/**
	 * Load and instantiate the logging configuration.
	 *
	 * @return the logging configuration
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	private LogConfiguration loadLogConfiguration()
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		String classname = this.properties.getProperty("feature.logging.impl");
		if (classname == null) {
			classname = DEFAULT_LOGGING;
		}
		final Class<?> logImplClass = Class.forName(classname);
		final Object logImpl = logImplClass.newInstance();
		if (logImpl instanceof LogConfiguration) {
			return (LogConfiguration) logImpl;
		}
		throw new InstantiationException(classname + " is not valid LogConfiguration instance!");
	}

	/**
	 * Get the properties
	 *
	 * @return Returns the properties.
	 */
	public Properties getProperties() {
		return properties;
	}
}
