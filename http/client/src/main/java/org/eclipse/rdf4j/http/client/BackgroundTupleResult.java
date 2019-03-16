/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.client;

import java.io.InputStream;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultParser;

/**
 * Provides concurrent access to tuple results as they are being parsed.
 * 
 * @author James Leigh
 * @deprecated Use {@link org.eclipse.rdf4j.query.resultio.helpers.BackgroundTupleResult} instead.
 */
@Deprecated
public class BackgroundTupleResult extends org.eclipse.rdf4j.query.resultio.helpers.BackgroundTupleResult {
	public BackgroundTupleResult(TupleQueryResultParser parser, InputStream in) {
		this(new QueueCursor<BindingSet>(10), parser, in);
	}

	public BackgroundTupleResult(QueueCursor<BindingSet> queue, TupleQueryResultParser parser, InputStream in) {
		super(queue, parser, in);
	}

}
