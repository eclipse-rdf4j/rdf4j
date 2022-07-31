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
package org.eclipse.rdf4j.sail.solr.config;

import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.eclipse.rdf4j.sail.lucene.config.AbstractLuceneSailConfig;

public class SolrSailConfig extends AbstractLuceneSailConfig {

	/*--------------*
	 * Constructors *
	 *--------------*/

	public SolrSailConfig() {
		super(SolrSailFactory.SAIL_TYPE);
	}

	public SolrSailConfig(SailImplConfig delegate) {
		super(SolrSailFactory.SAIL_TYPE, delegate);
	}

	public SolrSailConfig(String luceneDir) {
		super(SolrSailFactory.SAIL_TYPE, luceneDir);
	}

	public SolrSailConfig(String luceneDir, SailImplConfig delegate) {
		super(SolrSailFactory.SAIL_TYPE, luceneDir, delegate);
	}
}
