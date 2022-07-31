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

import org.eclipse.rdf4j.query.algebra.AbstractQueryModelNode;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;

/**
 * A structure representing a relevant source for some expression.
 *
 * @author Andreas Schwarte
 *
 */
public class StatementSource extends AbstractQueryModelNode {

	private static final long serialVersionUID = 1415552729436432653L;

	public enum StatementSourceType {
		LOCAL,
		REMOTE,
		REMOTE_POSSIBLY
	}

	protected String id;
	protected StatementSourceType type;

	public StatementSource(String name, StatementSourceType type) {
		super();
		this.id = name;
		this.type = type;
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor)
			throws X {
		visitor.meetOther(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		// no-op
	}

	@Override
	public String getSignature() {
		StringBuilder sb = new StringBuilder(64);

		sb.append(super.getSignature());

		sb.append(" (id=").append(id);

		sb.append(", type=").append(type);

		sb.append(")");

		return sb.toString();

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		StatementSource other = (StatementSource) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		if (type == null) {
			if (other.type != null) {
				return false;
			}
		} else if (!type.equals(other.type)) {
			return false;
		}
		return true;
	}

	public String getEndpointID() {
		return id;
	}

	public boolean isLocal() {
		return type == StatementSourceType.LOCAL;
	}

}
