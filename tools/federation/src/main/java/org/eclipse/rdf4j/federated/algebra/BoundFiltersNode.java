/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.federated.algebra;

import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.AbstractQueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;

/**
 * A {@link QueryModelNode} for printing the bound filter vars (e.g. in a {@link FedXStatementPattern})
 *
 * @author Andreas Schwarte
 *
 */
public class BoundFiltersNode extends AbstractQueryModelNode {

	private static final long serialVersionUID = -757075347491900484L;

	private final BindingSet boundFilters;

	public BoundFiltersNode(BindingSet boundFilters) {
		super();
		this.boundFilters = boundFilters;
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meetOther(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		// no-op
	}

	@Override
	public String getSignature() {
		StringBuilder sb = new StringBuilder(64);
		sb.append("BoundFilters (");
		int i = 0;
		for (Binding b : boundFilters) {
			sb.append(b.getName()).append("=").append(b.getValue());
			if (i++ < boundFilters.size() - 1) {
				sb.append(", ");
			}
		}
		sb.append(")");
		return sb.toString();
	}

	public static <X extends Exception> void visit(QueryModelVisitor<X> visitor, BindingSet boundFilters) throws X {
		new BoundFiltersNode(boundFilters).visit(visitor);
	}

}
