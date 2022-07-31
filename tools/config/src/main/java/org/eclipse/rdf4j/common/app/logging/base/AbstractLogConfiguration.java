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
package org.eclipse.rdf4j.common.app.logging.base;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.rdf4j.common.app.AppConfiguration;
import org.eclipse.rdf4j.common.app.logging.LogConfiguration;
import org.eclipse.rdf4j.common.app.util.ConfigurationUtil;

/**
 * Base implementation of LogConfiguration.
 *
 * @author Herko ter Horst
 */
public abstract class AbstractLogConfiguration implements LogConfiguration {

	private static final String LOGGING_CONFIG_FILE = "logging.properties";
	private static final String PACKAGES_SLF4J_KEY = "packages.slf4j";
	private static final String PACKAGES_JUL_KEY = "packages.jul";

	private File baseDir;
	private File confDir;
	private File loggingDir;

	private boolean debugLoggingEnabled;

	private final Set<String> packages;

	private AppConfiguration config;

	/**
	 * Constructor
	 *
	 * @throws IOException
	 */
	protected AbstractLogConfiguration() throws IOException {
		debugLoggingEnabled = false;
		packages = new LinkedHashSet<>();
		initBase();
	}

	@Override
	public void setBaseDir(File baseDir) throws IOException {
		this.baseDir = baseDir;
		confDir = new File(baseDir, DIR);
		loggingDir = new File(baseDir, LOGGING_DIR);
		if (!loggingDir.mkdirs() && !loggingDir.canWrite()) {
			throw new IOException("Unable to create logging directory " + loggingDir.getAbsolutePath());
		}
	}

	@Override
	public File getBaseDir() {
		return this.baseDir;
	}

	@Override
	public File getConfDir() {
		return confDir;
	}

	@Override
	public File getLoggingDir() {
		return loggingDir;
	}

	/**
	 * Initialize logging, setting log levels and handlers.
	 *
	 * @throws IOException
	 */
	private void initBase() throws IOException {
		Properties loggingConfig = ConfigurationUtil.loadConfigurationProperties(LOGGING_CONFIG_FILE, null);

		String slf4jPackages = loggingConfig.getProperty(PACKAGES_SLF4J_KEY);

		if (slf4jPackages != null) {
			String[] slf4jPackageNames = slf4jPackages.split(",");

			for (String packageName : slf4jPackageNames) {
				packages.add(packageName);
			}
		}

		String julPackages = loggingConfig.getProperty(PACKAGES_JUL_KEY);

		if (julPackages != null) {
			String[] julPackageNames = julPackages.split(",");

			for (String packageName : julPackageNames) {
				packages.add(packageName);

				Logger logger = Logger.getLogger(packageName.trim());
				logger.setUseParentHandlers(false);
				logger.setLevel(Level.ALL);
				logger.addHandler(new LogConverterHandler());
			}
		}
	}

	@Override
	public boolean isDebugLoggingEnabled() {
		return debugLoggingEnabled;
	}

	@Override
	public void setDebugLoggingEnabled(boolean debugLoggingEnabled) {
		this.debugLoggingEnabled = debugLoggingEnabled;
	}

	/**
	 * Get packages as a set
	 *
	 * @return packages as set of string
	 */
	protected Set<String> getPackages() {
		return Collections.unmodifiableSet(packages);
	}

	@Override
	public AppConfiguration getAppConfiguration() {
		return this.config;
	}

	@Override
	public void setAppConfiguration(AppConfiguration config) {
		this.config = config;
	}
}
