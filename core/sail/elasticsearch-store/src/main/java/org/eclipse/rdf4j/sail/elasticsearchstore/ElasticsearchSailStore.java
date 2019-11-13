/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearchstore;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.base.SailSource;
import org.eclipse.rdf4j.sail.base.SailStore;

/**
 * @author Håvard Mikkelsen Ottestad
 */
class ElasticsearchSailStore implements SailStore {

	private ElasticsearchSailSource sailSource;
	private ElasticsearchSailSource sailSourceInferred;

	private final MemNamespaceStore namespaceStore = new MemNamespaceStore();

	ElasticsearchSailStore(String hostname, int port, String index, ClientPool clientPool) {
		sailSource = new ElasticsearchSailSource(clientPool, new ElasticsearchDataStructure(hostname, port, index),
				namespaceStore);
		sailSourceInferred = new ElasticsearchSailSource(clientPool,
				new ElasticsearchDataStructure(hostname, port, index + "_inferred"), namespaceStore);
	}

	@Override
	public void close() throws SailException {

		sailSource.close();
		sailSourceInferred.close();
	}

	@Override
	public ValueFactory getValueFactory() {
		return SimpleValueFactory.getInstance();
	}

	@Override
	public EvaluationStatistics getEvaluationStatistics() {
		return new EvaluationStatistics() {
		};
	}

	@Override
	public SailSource getExplicitSailSource() {
		return sailSource;
	}

	@Override
	public SailSource getInferredSailSource() {
		return sailSourceInferred;
	}

	public void init() {
		sailSource.init();
		sailSourceInferred.init();
	}

	public void setElasticsearchScrollTimeout(int timeout) {
		sailSource.setElasticsearchScrollTimeout(timeout);
		sailSourceInferred.setElasticsearchScrollTimeout(timeout);
	}
}
