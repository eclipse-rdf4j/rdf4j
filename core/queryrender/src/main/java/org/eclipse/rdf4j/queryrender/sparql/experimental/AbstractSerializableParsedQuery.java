/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.queryrender.sparql.experimental;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.ExtensionElem;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.Slice;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;

import com.google.common.collect.Maps;

class AbstractSerializableParsedQuery {

	/**
	 * A map that maps all subquery projections within this query to their corresponding SerializableParsedTupleQuery
	 * instances.
	 */
	public Map<Projection, SerializableParsedTupleQuery> subQueriesByProjection = new HashMap<>();
	public TupleExpr whereClause = null;
	public Slice limit = null;
	public BindingSetAssignment bindings = null;
	public Map<String, ExtensionElem> extensionElements = Maps.newHashMap();
	public Dataset dataset = null;
	public Map<String, Var> nonAnonymousVars = Maps.newHashMap();

	public AbstractSerializableParsedQuery() {
		super();
	}

}
