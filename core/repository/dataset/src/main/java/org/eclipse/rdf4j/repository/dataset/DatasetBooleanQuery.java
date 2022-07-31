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
package org.eclipse.rdf4j.repository.dataset;

import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.sail.SailBooleanQuery;

/**
 * @author Arjohn Kampman
 */
class DatasetBooleanQuery extends DatasetQuery implements BooleanQuery {

	protected DatasetBooleanQuery(DatasetRepositoryConnection con, SailBooleanQuery sailQuery) {
		super(con, sailQuery);
	}

	@Override
	public boolean evaluate() throws QueryEvaluationException {
		con.loadDataset(sailQuery.getActiveDataset());
		return ((BooleanQuery) sailQuery).evaluate();
	}

}
