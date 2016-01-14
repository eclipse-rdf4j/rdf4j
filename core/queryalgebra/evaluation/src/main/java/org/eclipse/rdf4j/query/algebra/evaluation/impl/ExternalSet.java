/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import java.util.Collections;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.AbstractQueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

/**
 * @author James Leigh
 */
public abstract class ExternalSet extends AbstractQueryModelNode implements TupleExpr {

	private static final long serialVersionUID = 3903453394409442226L;

	public Set<String> getBindingNames() {
		return Collections.emptySet();
	}

	public Set<String> getAssuredBindingNames() {
		return Collections.emptySet();
	}

	public <X extends Exception> void visit(QueryModelVisitor<X> visitor)
		throws X
	{
		visitor.meetOther(this);
	}

	@Override
	public ExternalSet clone() {
		return (ExternalSet)super.clone();
	}

	public double cardinality() {
		return 1;
	}

	public abstract CloseableIteration<BindingSet, QueryEvaluationException> evaluate(BindingSet bindings)
		throws QueryEvaluationException;

}
