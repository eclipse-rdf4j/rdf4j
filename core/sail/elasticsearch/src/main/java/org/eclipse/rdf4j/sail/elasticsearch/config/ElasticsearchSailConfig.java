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
package org.eclipse.rdf4j.sail.elasticsearch.config;

import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.eclipse.rdf4j.sail.lucene.config.AbstractLuceneSailConfig;

public class ElasticsearchSailConfig extends AbstractLuceneSailConfig {

	/*--------------*
	 * Constructors *
	 *--------------*/

	public ElasticsearchSailConfig() {
		super(ElasticsearchSailFactory.SAIL_TYPE);
	}

	public ElasticsearchSailConfig(SailImplConfig delegate) {
		super(ElasticsearchSailFactory.SAIL_TYPE, delegate);
	}

	public ElasticsearchSailConfig(String luceneDir) {
		super(ElasticsearchSailFactory.SAIL_TYPE, luceneDir);
	}

	public ElasticsearchSailConfig(String luceneDir, SailImplConfig delegate) {
		super(ElasticsearchSailFactory.SAIL_TYPE, luceneDir, delegate);
	}
}
