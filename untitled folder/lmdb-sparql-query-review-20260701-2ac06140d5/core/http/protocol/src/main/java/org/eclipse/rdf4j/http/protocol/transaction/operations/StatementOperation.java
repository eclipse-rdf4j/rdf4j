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
package org.eclipse.rdf4j.http.protocol.transaction.operations;

import java.util.Objects;

import org.eclipse.rdf4j.common.lang.ObjectUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;

/**
 * A context operation with (optional) subject, predicate, object.
 *
 * @author Arjohn Kampman
 * @author Leo Sauermann
 */
public abstract class StatementOperation extends ContextOperation {

	private Resource subject;

	private IRI predicate;

	private Value object;

	protected StatementOperation(Resource... contexts) {
		super(contexts);
	}

	public Resource getSubject() {
		return subject;
	}

	public void setSubject(Resource subject) {
		this.subject = subject;
	}

	public IRI getPredicate() {
		return predicate;
	}

	public void setPredicate(IRI predicate) {
		this.predicate = predicate;
	}

	public Value getObject() {
		return object;
	}

	public void setObject(Value object) {
		this.object = object;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof StatementOperation) {
			StatementOperation o = (StatementOperation) other;

			return Objects.equals(getSubject(), o.getSubject())
					&& Objects.equals(getPredicate(), o.getPredicate())
					&& Objects.equals(getObject(), o.getObject()) && super.equals(other);
		}

		return false;
	}

	@Override
	public int hashCode() {
		int hashCode = ObjectUtil.nullHashCode(getSubject());
		hashCode = 31 * hashCode + ObjectUtil.nullHashCode(getPredicate());
		hashCode = 31 * hashCode + ObjectUtil.nullHashCode(getObject());
		hashCode = 31 * hashCode + super.hashCode();
		return hashCode;
	}
}
