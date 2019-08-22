/** *****************************************************************************
 * Copyright (c) 2017 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ****************************************************************************** */
package org.eclipse.rdf4j.lucene.spin.config;

import java.util.Properties;
import org.eclipse.rdf4j.lucene.spin.LuceneSpinSail;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.eclipse.rdf4j.sail.config.SailFactory;
import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.eclipse.rdf4j.sail.lucene.config.AbstractLuceneSailConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jacek grzebyta
 */
public class LuceneSpinSailFactory implements SailFactory {

	private static final Logger log = LoggerFactory.getLogger(LuceneSpinSailFactory.class);

	/**
	 * The type of repositories that are created by this factory.
	 *
	 * @see SailFactory#getSailType()
	 */
	public static final String SAIL_TYPE = "openrdf:LuceneSpinSail";

	/*
	 * Returns the Sail's type: <tt>openrdf:LuceneSpinSail</tt>.
	 */
	@Override
	public String getSailType() {
		return SAIL_TYPE;
	}

	@Override
	public SailImplConfig getConfig() {
		return new LuceneSpinSailConfig();
	}

	@Override
	public Sail getSail(SailImplConfig config) throws SailConfigException {
		if (!SAIL_TYPE.equals(config.getType())) {
			throw new SailConfigException("Invalid Sail type: " + config.getType());
		}

		LuceneSpinSail sail = new LuceneSpinSail();
		Properties params = sail.getParameters();

		// set up parameters
		params.setProperty(LuceneSail.INDEX_CLASS_KEY, LuceneSail.DEFAULT_INDEX_CLASS);

		if (config instanceof AbstractLuceneSailConfig) {
			AbstractLuceneSailConfig luceneConfig = (AbstractLuceneSailConfig) config;
			log.debug("Lucene indexDir: {}", luceneConfig.getIndexDir());
			params.setProperty(LuceneSail.LUCENE_DIR_KEY, luceneConfig.getIndexDir());
			for (String key : luceneConfig.getParameterNames()) {
				params.setProperty(key, luceneConfig.getParameter(key));
			}
		}

		return sail;
	}

}
