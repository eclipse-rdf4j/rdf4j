/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.s3.config;

import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.eclipse.rdf4j.sail.config.SailFactory;
import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.eclipse.rdf4j.sail.s3.S3Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link SailFactory} that creates {@link S3Store}s based on RDF configuration data.
 */
public class S3StoreFactory implements SailFactory {

	private static final Logger logger = LoggerFactory.getLogger(S3StoreFactory.class);

	/**
	 * The type of repositories that are created by this factory.
	 *
	 * @see SailFactory#getSailType()
	 */
	public static final String SAIL_TYPE = "rdf4j:S3Store";

	/**
	 * Returns the Sail's type: <tt>rdf4j:S3Store</tt>.
	 */
	@Override
	public String getSailType() {
		return SAIL_TYPE;
	}

	@Override
	public SailImplConfig getConfig() {
		return new S3StoreConfig();
	}

	@Override
	public Sail getSail(SailImplConfig config) throws SailConfigException {
		if (!SAIL_TYPE.equals(config.getType())) {
			throw new SailConfigException("Invalid Sail type: " + config.getType());
		}

		if (config instanceof S3StoreConfig) {
			return new S3Store((S3StoreConfig) config);
		} else {
			logger.warn("Config is instance of {} is not S3StoreConfig.", config.getClass().getName());
			return new S3Store();
		}
	}
}
