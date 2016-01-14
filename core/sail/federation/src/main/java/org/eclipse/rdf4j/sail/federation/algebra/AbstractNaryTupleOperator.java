/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.federation.algebra;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.query.algebra.TupleExpr;

/**
 * An abstract superclass for n-ary tuple operators which have one or more
 * arguments.
 */
public abstract class AbstractNaryTupleOperator extends AbstractNaryOperator<TupleExpr> implements TupleExpr {

	private static final long serialVersionUID = 4703129201150946366L;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public AbstractNaryTupleOperator() {
		super();
	}

	/**
	 * Creates a new n-ary tuple operator.
	 */
	public AbstractNaryTupleOperator(TupleExpr... args) {
		super(args);
	}

	/**
	 * Creates a new n-ary tuple operator.
	 */
	public AbstractNaryTupleOperator(List<? extends TupleExpr> args) {
		super(args);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public Set<String> getBindingNames() {
		Set<String> bindingNames = new LinkedHashSet<String>(16);

		for (TupleExpr arg : getArgs()) {
			bindingNames.addAll(arg.getBindingNames());
		}

		return bindingNames;
	}

	public Set<String> getAssuredBindingNames() {
		Set<String> bindingNames = new LinkedHashSet<String>(16);

		for (TupleExpr arg : getArgs()) {
			bindingNames.addAll(arg.getAssuredBindingNames());
		}

		return bindingNames;
	}

	@Override
	public AbstractNaryTupleOperator clone() { // NOPMD
		return (AbstractNaryTupleOperator)super.clone();
	}
}
