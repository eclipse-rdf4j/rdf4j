/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

import java.util.Collections;
import java.util.Set;

/**
 * A tuple expression that contains zero solutions.
 */
public class EmptySet extends AbstractQueryModelNode implements TupleExpr {

	@Override
	public Set<String> getBindingNames() {
		return getAssuredBindingNames();
	}

	@Override
	public Set<String> getAssuredBindingNames() {
		return Collections.emptySet();
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		// no-op
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof EmptySet;
	}

	@Override
	public int hashCode() {
		return "EmptySet".hashCode();
	}

	@Override
	public EmptySet clone() {
		return (EmptySet) super.clone();
	}
}
