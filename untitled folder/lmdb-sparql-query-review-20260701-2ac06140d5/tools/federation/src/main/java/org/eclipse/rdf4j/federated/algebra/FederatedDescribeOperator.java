/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.algebra;

import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.query.algebra.DescribeOperator;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

/**
 * Specialized {@link DescribeOperator} Node for maintaining {@link QueryInfo}.
 *
 * @author Andreas Schwarte
 *
 */
public class FederatedDescribeOperator extends DescribeOperator implements QueryRef {

	private static final long serialVersionUID = -5623698444166736806L;

	private final QueryInfo queryInfo;

	public FederatedDescribeOperator(TupleExpr arg, QueryInfo queryInfo) {
		super(arg);
		this.queryInfo = queryInfo;
	}

	@Override
	public QueryInfo getQueryInfo() {
		return queryInfo;
	}

}
