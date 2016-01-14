/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene.config;

import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.eclipse.rdf4j.sail.lucene.config.AbstractLuceneSailConfig;

public class LuceneSailConfig extends AbstractLuceneSailConfig {

	/*--------------*
	 * Constructors *
	 *--------------*/

	public LuceneSailConfig() {
		super(LuceneSailFactory.SAIL_TYPE);
	}

	public LuceneSailConfig(SailImplConfig delegate) {
		super(LuceneSailFactory.SAIL_TYPE, delegate);
	}

	public LuceneSailConfig(String luceneDir) {
		super(LuceneSailFactory.SAIL_TYPE, luceneDir);
	}

	public LuceneSailConfig(String luceneDir, SailImplConfig delegate) {
		super(LuceneSailFactory.SAIL_TYPE, luceneDir, delegate);
	}
}
