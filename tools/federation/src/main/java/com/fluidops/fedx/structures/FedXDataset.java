/*
 * Copyright (C) 2019 Veritas Technologies LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fluidops.fedx.structures;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.Dataset;

/**
 * Abstraction of a {@link Dataset} to provide additional information for the
 * evaluation of a query.
 * <p>
 * Can be used to define the endpoints against which a given query shall be
 * executed.</p>
 * <p>
 * Example</p>
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
public class FedXDataset implements Dataset{

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
		return delegate.getDefaultGraphs();
	}

	@Override
	public IRI getDefaultInsertGraph() {
		return delegate.getDefaultInsertGraph();
	}

	@Override
	public Set<IRI> getDefaultRemoveGraphs() {
		return delegate.getDefaultRemoveGraphs();
	}

	@Override
	public Set<IRI> getNamedGraphs() {
		return delegate.getNamedGraphs();
	}

}
