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
package org.eclipse.rdf4j.federated.algebra;

import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.query.algebra.LeftJoin;

/**
 * Abstraction of {@link LeftJoin} to maintain the {@link QueryInfo}.
 *
 * @author Andreas Schwarte
 *
 */
public class FedXLeftJoin extends LeftJoin {

	private static final long serialVersionUID = 7444318847550719096L;

	protected transient final QueryInfo queryInfo;

	public FedXLeftJoin(LeftJoin leftJoin, QueryInfo queryInfo) {
		super(leftJoin.getLeftArg(), leftJoin.getRightArg(), leftJoin.getCondition());
		this.queryInfo = queryInfo;
	}

	public QueryInfo getQueryInfo() {
		return queryInfo;
	}
}
