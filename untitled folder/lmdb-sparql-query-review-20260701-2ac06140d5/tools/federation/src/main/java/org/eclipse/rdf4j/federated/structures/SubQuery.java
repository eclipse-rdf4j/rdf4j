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
package org.eclipse.rdf4j.federated.structures;

import java.io.Serializable;
import java.util.Arrays;

import org.eclipse.rdf4j.federated.util.FedXUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.StatementPattern;

public class SubQuery implements Serializable {

	private static final long serialVersionUID = 8968907794785828994L;

	protected final Resource subj;
	protected final IRI pred;
	protected final Value obj;
	/**
	 * the contexts, length zero array for triple mode query
	 */
	protected final Resource[] contexts;

	public SubQuery(Resource subj, IRI pred, Value obj, Resource... contexts) {
		super();
		this.subj = subj;
		this.pred = pred;
		this.obj = obj;
		this.contexts = contexts;
	}

	public SubQuery(StatementPattern stmt, Dataset dataset) {
		this((Resource) stmt.getSubjectVar().getValue(), (IRI) stmt.getPredicateVar().getValue(),
				stmt.getObjectVar().getValue(), FedXUtil.toContexts(stmt, dataset));
	}

	/**
	 *
	 * @return true if this subquery is unbound in all three positions
	 */
	public boolean isUnbound() {
		return subj == null && pred == null && obj == null;
	}

	public Resource subject() {
		return this.subj;
	}

	public IRI predicate() {
		return this.pred;
	}

	public Value object() {
		return this.obj;
	}

	public Resource[] contexts() {
		return this.contexts;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(contexts);
		result = prime * result + ((obj == null) ? 0 : obj.hashCode());
		result = prime * result + ((pred == null) ? 0 : pred.hashCode());
		result = prime * result + ((subj == null) ? 0 : subj.hashCode());
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
		SubQuery other = (SubQuery) obj;
		if (!Arrays.equals(contexts, other.contexts)) {
			return false;
		}
		if (this.obj == null) {
			if (other.obj != null) {
				return false;
			}
		} else if (!this.obj.equals(other.obj)) {
			return false;
		}
		if (pred == null) {
			if (other.pred != null) {
				return false;
			}
		} else if (!pred.equals(other.pred)) {
			return false;
		}
		if (subj == null) {
			if (other.subj != null) {
				return false;
			}
		} else if (!subj.equals(other.subj)) {
			return false;
		}
		return true;
	}

}
