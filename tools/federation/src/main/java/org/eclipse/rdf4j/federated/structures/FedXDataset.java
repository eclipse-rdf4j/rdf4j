/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.structures;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.Dataset;

/**
 * Abstraction of a {@link Dataset} to provide additional information for the evaluation of a query.
 * <p>
 * Can be used to define the endpoints against which a given query shall be executed.
 * </p>
 * <p>
 * Example
 * </p>
 *
 * <pre>
 * TupleQuery tq = ...;
 * FedXDataset ds = new FedXDataset(tq.getDataset);
 * ds.addEndpoint("myEndpoint");
 * ds.addEndpoint("otherEndpoint");
 * tq.setDataset(ds)
 * TupleQueryResult res = tq.evaluate()
 * </pre>
 *
 * @author Andreas Schwarte
 *
 */
public class FedXDataset implements Dataset {

	protected final Dataset delegate;

	protected Set<String> endpoints = new HashSet<>();

	public FedXDataset(Dataset delegate) {
		super();
		this.delegate = delegate;
	}

	public void addEndpoint(String endpointId) {
		endpoints.add(endpointId);
	}

	public void addEndpoints(Collection<String> endpointIDs) {
		endpoints.addAll(endpointIDs);
	}

	public Set<String> getEndpoints() {
		return Collections.unmodifiableSet(endpoints);
	}

	@Override
	public Set<IRI> getDefaultGraphs() {
		if (delegate == null) {
			return Collections.emptySet();
		}
		return delegate.getDefaultGraphs();
	}

	@Override
	public IRI getDefaultInsertGraph() {
		if (delegate == null) {
			return null;
		}
		return delegate.getDefaultInsertGraph();
	}

	@Override
	public Set<IRI> getDefaultRemoveGraphs() {
		if (delegate == null) {
			return Collections.emptySet();
		}
		return delegate.getDefaultRemoveGraphs();
	}

	@Override
	public Set<IRI> getNamedGraphs() {
		if (delegate == null) {
			return Collections.emptySet();
		}
		return delegate.getNamedGraphs();
	}

}
