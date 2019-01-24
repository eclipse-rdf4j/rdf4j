/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.impl;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.Dataset;

/**
 * A simple implementation of the {@link Dataset} interface.
 * 
 * @author Arjohn Kampman
 * @author James Leigh
 */
public class SimpleDataset implements Dataset, Serializable {

	private static final long serialVersionUID = 7841576172053060417L;

	private Set<IRI> defaultRemoveGraphs = new LinkedHashSet<>();

	private IRI defaultInsertGraph;

	private Set<IRI> defaultGraphs = new LinkedHashSet<>();

	private Set<IRI> namedGraphs = new LinkedHashSet<>();

	public SimpleDataset() {
	}

	@Override
	public Set<IRI> getDefaultRemoveGraphs() {
		return Collections.unmodifiableSet(defaultRemoveGraphs);
	}

	/**
	 * Adds a graph URI to the set of default remove graph URIs.
	 */
	public void addDefaultRemoveGraph(IRI graphURI) {
		defaultRemoveGraphs.add(graphURI);
	}

	/**
	 * Removes a graph URI from the set of default remove graph URIs.
	 * 
	 * @return <tt>true</tt> if the URI was removed from the set, <tt>false</tt> if the set did not contain
	 *         the URI.
	 */
	public boolean removeDefaultRemoveGraph(IRI graphURI) {
		return defaultRemoveGraphs.remove(graphURI);
	}

	/**
	 * @return Returns the default insert graph.
	 */
	@Override
	public IRI getDefaultInsertGraph() {
		return defaultInsertGraph;
	}

	/**
	 * @param defaultInsertGraph
	 *        The default insert graph to used.
	 */
	public void setDefaultInsertGraph(IRI defaultInsertGraph) {
		this.defaultInsertGraph = defaultInsertGraph;
	}

	@Override
	public Set<IRI> getDefaultGraphs() {
		return Collections.unmodifiableSet(defaultGraphs);
	}

	/**
	 * Adds a graph URI to the set of default graph URIs.
	 */
	public void addDefaultGraph(IRI graphURI) {
		defaultGraphs.add(graphURI);
	}

	/**
	 * Removes a graph URI from the set of default graph URIs.
	 * 
	 * @return <tt>true</tt> if the URI was removed from the set, <tt>false</tt> if the set did not contain
	 *         the URI.
	 */
	public boolean removeDefaultGraph(IRI graphURI) {
		return defaultGraphs.remove(graphURI);
	}

	/**
	 * Gets the (unmodifiable) set of named graph URIs.
	 */
	@Override
	public Set<IRI> getNamedGraphs() {
		return Collections.unmodifiableSet(namedGraphs);
	}

	/**
	 * Adds a graph URI to the set of named graph URIs.
	 */
	public void addNamedGraph(IRI graphURI) {
		namedGraphs.add(graphURI);
	}

	/**
	 * Removes a graph URI from the set of named graph URIs.
	 * 
	 * @return <tt>true</tt> if the URI was removed from the set, <tt>false</tt> if the set did not contain
	 *         the URI.
	 */
	public boolean removeNamedGraph(IRI graphURI) {
		return namedGraphs.remove(graphURI);
	}

	/**
	 * Removes all graph URIs (both default and named) from this dataset.
	 */
	public void clear() {
		defaultRemoveGraphs.clear();
		defaultInsertGraph = null;
		defaultGraphs.clear();
		namedGraphs.clear();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (IRI uri : getDefaultRemoveGraphs()) {
			sb.append("DELETE FROM ");
			appendURI(sb, uri);
		}
		if (getDefaultInsertGraph() != null) {
			sb.append("INSERT INTO ");
			appendURI(sb, getDefaultInsertGraph());
		}
		for (IRI uri : getDefaultGraphs()) {
			sb.append("USING ");
			appendURI(sb, uri);
		}
		for (IRI uri : getNamedGraphs()) {
			sb.append("USING NAMED ");
			appendURI(sb, uri);
		}
		if (getDefaultGraphs().isEmpty() && getNamedGraphs().isEmpty()) {
			sb.append("## empty dataset ##");
		}
		return sb.toString();
	}

	private void appendURI(StringBuilder sb, IRI uri) {
		String str = uri.toString();
		if (str.length() > 50) {
			sb.append("<").append(str, 0, 19).append("..");
			sb.append(str, str.length() - 29, str.length()).append(">\n");
		}
		else {
			sb.append("<").append(uri).append(">\n");
		}
	}
}
