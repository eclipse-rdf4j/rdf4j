/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail;

import org.eclipse.rdf4j.query.impl.AbstractParserQuery;
import org.eclipse.rdf4j.query.parser.ParsedQuery;

/**
 * @author Arjohn Kampman
 */
public abstract class SailConnectionQuery extends AbstractParserQuery {

	private final SailConnection con;

	protected SailConnectionQuery(ParsedQuery parsedQuery, SailConnection con) {
		super(parsedQuery);
		this.con = con;
	}

	protected SailConnection getSailConnection() {
		return con;
	}
}
