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
 * Base class for shared functionality of aggregate operators (e.g. DISTINCT setting)
 *
 * @author Jeen Broekstra
 */
public abstract class AbstractAggregateOperator extends UnaryValueOperator implements AggregateOperator {

	private static final long serialVersionUID = 4016064683034358205L;

	private boolean distinct = false;

	protected AbstractAggregateOperator(ValueExpr arg) {
		this(arg, false);
	}

	protected AbstractAggregateOperator(ValueExpr arg, boolean distinct) {
		super();
		if (arg != null) {
			setArg(arg);
		}
		setDistinct(distinct);
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
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}

		AbstractAggregateOperator that = (AbstractAggregateOperator) o;

		return distinct == that.distinct;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (distinct ? 1 : 0);
		return result;
	}

	@Override
	public AbstractAggregateOperator clone() {
		return (AbstractAggregateOperator) super.clone();
	}

}
