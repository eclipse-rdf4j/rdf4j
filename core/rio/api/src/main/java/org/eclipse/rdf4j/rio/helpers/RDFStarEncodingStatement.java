/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.helpers;

import java.util.Objects;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

/**
 * Represents a {@link Statement} whose subject or object may be an RDF-star triple that will be encoded as a special
 * IRI value on {@link #getSubject()} and {@link #getObject()}.
 *
 * @author Pavel Mihaylov
 */
class RDFStarEncodingStatement implements Statement {
	private final Statement delegate;

	RDFStarEncodingStatement(Statement delegate) {
		this.delegate = delegate;
	}

	@Override
	public Resource getSubject() {
		return RDFStarUtil.toRDFEncodedValue(delegate.getSubject());
	}

	@Override
	public IRI getPredicate() {
		return delegate.getPredicate();
	}

	@Override
	public Value getObject() {
		return RDFStarUtil.toRDFEncodedValue(delegate.getObject());
	}

	@Override
	public Resource getContext() {
		return delegate.getContext();
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}

		if (other instanceof Statement) {
			Statement that = (Statement) other;

			return getObject().equals(that.getObject()) && getSubject().equals(that.getSubject())
					&& getPredicate().equals(that.getPredicate()) && Objects.equals(getContext(), that.getContext());
		}

		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(getSubject(), getPredicate(), getObject(), getContext());
	}
}
