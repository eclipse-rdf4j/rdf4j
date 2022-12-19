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
package org.eclipse.rdf4j.query.algebra;

/**
 * @author Jeen Broekstra
 * @deprecated Use {@link AbstractAggregateOperator} instead.
 */
@Deprecated(since = "2.0")
public abstract class AggregateOperatorBase extends AbstractAggregateOperator {

	private static final long serialVersionUID = 1L;

	protected AggregateOperatorBase(ValueExpr arg) {
		super(arg);
	}

}
