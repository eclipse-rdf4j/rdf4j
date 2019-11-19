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

	public ElasticsearchSailStore(DataStructureInterface dataStructure, DataStructureInterface dataStructureInferred,
			NamespaceStore namespaceStore) {
		sailSource = new ElasticsearchSailSource(dataStructure, namespaceStore);
		sailSourceInferred = new ElasticsearchSailSource(dataStructureInferred, namespaceStore);
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

}
