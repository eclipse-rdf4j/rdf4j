/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.client;

import java.io.InputStream;
import java.nio.charset.Charset;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFParser;

/**
 * Provides concurrent access to statements as they are being parsed.
 * 
 * @author James Leigh
 * @deprecated Use {@link org.eclipse.rdf4j.query.impl.BackgroundGraphResult} instead.
 */
@Deprecated
public class BackgroundGraphResult extends org.eclipse.rdf4j.query.impl.BackgroundGraphResult {
	public BackgroundGraphResult(RDFParser parser, InputStream in, Charset charset, String baseURI) {
		this(new QueueCursor<Statement>(10), parser, in, charset, baseURI);
	}

	public BackgroundGraphResult(QueueCursor<Statement> queue, RDFParser parser, InputStream in, Charset charset,
			String baseURI) {
		super(queue, parser, in, charset, baseURI);
	}

}
