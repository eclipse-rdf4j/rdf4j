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
package org.eclipse.rdf4j.query.parser;

import org.eclipse.rdf4j.query.MalformedQueryException;

/**
 * An RDF query parser translate query strings in some query language to OpenRDF query models.
 */
public interface QueryParser {

	ParsedUpdate parseUpdate(String updateStr, String baseURI) throws MalformedQueryException;

	ParsedQuery parseQuery(String queryStr, String baseURI) throws MalformedQueryException;
}
