/* @formatter:off */
/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearchstore;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategyFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.UnknownSailTransactionStateException;
import org.eclipse.rdf4j.sail.base.SailSourceConnection;
import org.eclipse.rdf4j.sail.base.SailStore;

/**
 * @author Håvard Mikkelsen Ottestad
 */
public class ElasticsearchStoreConnection extends SailSourceConnection {

	ElasticsearchStore sail;

	protected ElasticsearchStoreConnection(ElasticsearchStore sail, SailStore store, FederatedServiceResolver resolver) {
		super(sail, store, resolver);
		this.sail = sail;
	}

	ElasticsearchStoreConnection(ElasticsearchStore sail, SailStore store,
										   EvaluationStrategyFactory evalStratFactory) {
		super(sail, store, evalStratFactory);
		this.sail = sail;
	}


	public ElasticsearchStore getSail() {
		return sail;
	}


	@Override
	protected void addStatementInternal(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
		//post statement added
	}

	@Override
	protected void removeStatementsInternal(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {

	}

	@Override
	protected IsolationLevel getTransactionIsolation() {
		return IsolationLevels.NONE;
	}



}
