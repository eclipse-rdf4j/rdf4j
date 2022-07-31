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

/**
 * Algebra construct representing an empty join.
 *
 * @author Andreas Schwarte
 *
 */
public class EmptyNJoin extends NTuple implements EmptyResult {

	private static final long serialVersionUID = -3895999439111284174L;

	public EmptyNJoin(NJoin njoin, QueryInfo queryInfo) {
		super(njoin.getArgs(), queryInfo);
	}

}
