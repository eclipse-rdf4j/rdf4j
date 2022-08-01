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
package org.eclipse.rdf4j.query;

import java.util.Map;

import org.eclipse.rdf4j.model.Statement;

/**
 * A representation of a query result as a sequence of {@link Statement} objects. Each query result consists of zero or
 * more Statements and additionaly carries information about relevant namespace declarations. Note: take care to always
 * close a GraphQueryResult after use to free any resources it keeps hold of.
 *
 * @author Jeen Broekstra
 */
public interface GraphQueryResult extends QueryResult<Statement> {

	/**
	 * Retrieves relevant namespaces from the query result. <br/>
	 * The contents of the Map may be modified after it is returned, as the initial return may be performed when the
	 * first RDF Statement is encountered.
	 *
	 * @return a Map&lt;String, String&gt; object containing (prefix, namespace) pairs.
	 * @throws QueryEvaluationException
	 */
	Map<String, String> getNamespaces() throws QueryEvaluationException;

}
