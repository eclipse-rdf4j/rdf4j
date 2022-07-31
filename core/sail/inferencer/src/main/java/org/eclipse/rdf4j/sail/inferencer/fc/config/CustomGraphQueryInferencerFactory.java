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
package org.eclipse.rdf4j.sail.inferencer.fc.config;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.eclipse.rdf4j.sail.config.SailFactory;
import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.eclipse.rdf4j.sail.inferencer.fc.CustomGraphQueryInferencer;

/**
 * A {@link SailFactory} that creates a {@link CustomGraphQueryInferencer} based on RDF configuration data.
 *
 * @author Dale Visser
 */
public class CustomGraphQueryInferencerFactory implements SailFactory {

	/**
	 * The type of repositories that are created by this factory.
	 *
	 * @see SailFactory#getSailType()
	 */
	public static final String SAIL_TYPE = "openrdf:CustomGraphQueryInferencer";

	/**
	 * Returns the Sail's type: <var>openrdf:CustomGraphQueryInferencer</var>.
	 */
	@Override
	public String getSailType() {
		return SAIL_TYPE;
	}

	@Override
	public SailImplConfig getConfig() {
		return new CustomGraphQueryInferencerConfig();
	}

	@Override
	public Sail getSail(SailImplConfig config) throws SailConfigException {
		if (!SAIL_TYPE.equals(config.getType())) {
			throw new SailConfigException("Invalid Sail type: " + config.getType());
		}
		CustomGraphQueryInferencer sail = new CustomGraphQueryInferencer();
		if (config instanceof CustomGraphQueryInferencerConfig) {
			CustomGraphQueryInferencerConfig customConfig = (CustomGraphQueryInferencerConfig) config;
			try {
				sail.setFields(customConfig.getQueryLanguage(), customConfig.getRuleQuery(),
						customConfig.getMatcherQuery());
			} catch (RDF4JException e) {
				throw new SailConfigException("Problem occured parsing rule or matcher query text.", e);
			}
		}
		return sail;
	}
}
