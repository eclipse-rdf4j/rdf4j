/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.ast;

public class SparqlFragment {

	String fragment;
	boolean filterCondition;
	boolean bgp;

	public SparqlFragment(String fragment) {
		this.fragment = fragment;
	}

	public static SparqlFragment filterCondition(String fragment) {
		SparqlFragment sparqlFragment = new SparqlFragment(fragment);
		sparqlFragment.filterCondition = true;
		return sparqlFragment;
	}

	public static SparqlFragment bgp(String fragment) {
		SparqlFragment sparqlFragment = new SparqlFragment(fragment);
		sparqlFragment.bgp = true;
		return sparqlFragment;
	}

	public String getFragment() {
		return fragment;
	}

	public boolean isFilterCondition() {
		return filterCondition;
	}

	public boolean isBgp() {
		return bgp;
	}
}
