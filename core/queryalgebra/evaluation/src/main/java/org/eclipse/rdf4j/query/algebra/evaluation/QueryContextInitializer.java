/**
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.query.algebra.evaluation;

/**
 * Callback for configuring/customising a {@see QueryContext}.
 */
public interface QueryContextInitializer {

	/**
	 * Called after a QueryContext has begun.
	 */
	void init(QueryContext ctx);

	/**
	 * Called before a QueryContext has ended.
	 */
	void destroy(QueryContext ctx);
}
