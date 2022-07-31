/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation;

import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;

/**
 * Stateful {@link EvaluationStatistics} providing information for evaluating a given query in
 * {@link FederationEvalStrategy#optimize(org.eclipse.rdf4j.query.algebra.TupleExpr, EvaluationStatistics, org.eclipse.rdf4j.query.BindingSet)}
 * <p>
 * The statistics are instantiated per query.
 * </p>
 *
 * @author Andreas Schwarte
 *
 */
public class FederationEvaluationStatistics extends EvaluationStatistics {

	private final QueryInfo queryInfo;

	private final Dataset dataset;

	public FederationEvaluationStatistics(QueryInfo queryInfo, Dataset dataset) {
		super();
		this.queryInfo = queryInfo;
		this.dataset = dataset;
	}

	public QueryInfo getQueryInfo() {
		return queryInfo;
	}

	public Dataset getDataset() {
		return dataset;
	}
}
