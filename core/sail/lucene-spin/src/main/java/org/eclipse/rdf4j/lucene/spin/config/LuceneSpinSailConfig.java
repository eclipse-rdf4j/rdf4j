/*******************************************************************************
 * Copyright (c) 2017 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.lucene.spin.config;

import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.eclipse.rdf4j.sail.lucene.config.AbstractLuceneSailConfig;

/**
 * @author jacek grzebyta
 */
public class LuceneSpinSailConfig extends AbstractLuceneSailConfig {

	public LuceneSpinSailConfig() {
		super(LuceneSpinSailFactory.SAIL_TYPE);
	}

	public LuceneSpinSailConfig(SailImplConfig delegate) {
		super(LuceneSpinSailFactory.SAIL_TYPE, delegate);
	}

	public LuceneSpinSailConfig(String luceneDir) {
		super(LuceneSpinSailFactory.SAIL_TYPE, luceneDir);
	}

	public LuceneSpinSailConfig(String luceneDir, SailImplConfig delegate) {
		super(LuceneSpinSailFactory.SAIL_TYPE, luceneDir, delegate);
	}

}
