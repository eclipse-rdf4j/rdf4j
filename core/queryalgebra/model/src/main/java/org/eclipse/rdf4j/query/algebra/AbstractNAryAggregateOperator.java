/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

import java.util.List;
import java.util.Objects;

/**
 * Base class for n-ary aggregate operators.
 *
 * @author Nik Kozlov
 */
public abstract class AbstractNAryAggregateOperator extends NAryValueOperator implements AggregateOperator {

	private static final long serialVersionUID = 1L;

	private boolean distinct = false;

	protected AbstractNAryAggregateOperator(List<ValueExpr> args) {
		this(args, false);
	}

	protected AbstractNAryAggregateOperator(List<ValueExpr> args, boolean distinct) {
		super(args);
		this.distinct = distinct;
	}

	@Override
	public void setDistinct(boolean distinct) {
		this.distinct = distinct;
	}

	@Override
	public boolean isDistinct() {
		return this.distinct;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof AbstractNAryAggregateOperator)) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		AbstractNAryAggregateOperator that = (AbstractNAryAggregateOperator) o;
		return distinct == that.distinct;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), distinct);
	}

	@Override
	public AbstractNAryAggregateOperator clone() {
		return (AbstractNAryAggregateOperator) super.clone();
	}
}
