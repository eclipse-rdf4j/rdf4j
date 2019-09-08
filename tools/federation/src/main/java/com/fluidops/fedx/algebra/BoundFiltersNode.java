/*
 * Copyright (C) 2019 Veritas Technologies LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fluidops.fedx.algebra;

import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.AbstractQueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;

/**
 * A {@link QueryModelNode} for printing the bound filter vars (e.g. in a
 * {@link FedXStatementPattern})
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
	public String getSignature()
	{
		StringBuilder sb = new StringBuilder(64);
		sb.append("BoundFilters (");
		int i=0;
		for (Binding b : boundFilters) {
			sb.append(b.getName()).append("=").append(b.getValue());
			if (i++<boundFilters.size()-1) {
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
