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
package org.eclipse.rdf4j.query.impl;

import java.io.Serializable;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.Dataset;

/**
 * @author james
 */
public class FallbackDataset implements Dataset, Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 5866540736738270376L;

	public static Dataset fallback(Dataset primary, Dataset fallback) {
		if (primary == null) {
			return fallback;
		}
		if (fallback == null) {
			return primary;
		}
		return new FallbackDataset(primary, fallback);
	}

	private final Dataset primary;

	private final Dataset fallback;

	private FallbackDataset(Dataset primary, Dataset secondary) {
		assert primary != null;
		assert secondary != null;
		this.primary = primary;
		this.fallback = secondary;
	}

	@Override
	public Set<IRI> getDefaultGraphs() {
		Set<IRI> set = primary.getDefaultGraphs();
		if (set == null || set.isEmpty()) {
			return fallback.getDefaultGraphs();
		}
		return set;
	}

	@Override
	public Set<IRI> getNamedGraphs() {
		Set<IRI> set = primary.getNamedGraphs();
		if (set == null || set.isEmpty()) {
			return fallback.getNamedGraphs();
		}
		return set;
	}

	@Override
	public IRI getDefaultInsertGraph() {
		IRI graph = primary.getDefaultInsertGraph();
		if (graph == null) {
			return fallback.getDefaultInsertGraph();
		}
		return graph;
	}

	@Override
	public Set<IRI> getDefaultRemoveGraphs() {
		Set<IRI> set = primary.getDefaultRemoveGraphs();
		if (set == null || set.isEmpty()) {
			return fallback.getDefaultRemoveGraphs();
		}
		return set;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (IRI uri : getDefaultRemoveGraphs()) {
			sb.append("DELETE FROM ");
			appendURI(sb, uri);
		}
		sb.append("INSERT INTO ");
		appendURI(sb, getDefaultInsertGraph());
		for (IRI uri : getDefaultGraphs()) {
			sb.append("FROM ");
			appendURI(sb, uri);
		}
		for (IRI uri : getNamedGraphs()) {
			sb.append("FROM NAMED ");
			appendURI(sb, uri);
		}
		return sb.toString();
	}

	private void appendURI(StringBuilder sb, IRI uri) {
		String str = uri.toString();
		if (str.length() > 50) {
			sb.append("<").append(str, 0, 19).append("..");
			sb.append(str, str.length() - 29, str.length()).append(">\n");
		} else {
			sb.append("<").append(uri).append(">\n");
		}
	}

}
