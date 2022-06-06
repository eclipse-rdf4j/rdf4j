/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.query.parser.sparql;

import java.util.HashMap;
import java.util.Set;

public class SPARQLDataSet {

	private final HashMap<String, String> namedGraphs = new HashMap<>();

	private String defaultGraph;

	public SPARQLDataSet() {
	}

	public SPARQLDataSet(String defaultGraph) {
		this();
		setDefaultGraph(defaultGraph);
	}

	public void setDefaultGraph(String defaultGraph) {
		this.defaultGraph = defaultGraph;
	}

	public String getDefaultGraph() {
		return defaultGraph;
	}

	public void addNamedGraph(String graphName, String graphLocation) {
		namedGraphs.put(graphName, graphLocation);
	}

	public boolean hasNamedGraphs() {
		return (!namedGraphs.isEmpty());
	}

	public Set<String> getGraphNames() {
		return namedGraphs.keySet();
	}

	public String getGraphLocation(String graphName) {
		return namedGraphs.get(graphName);
	}
}
