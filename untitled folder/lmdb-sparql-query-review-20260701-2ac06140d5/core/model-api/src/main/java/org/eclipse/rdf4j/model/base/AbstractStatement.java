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

package org.eclipse.rdf4j.model.base;

import java.util.Objects;

import org.eclipse.rdf4j.model.Statement;

/**
 * Base class for {@link Statement}, offering common functionality.
 *
 * @author Alessandro Bollini
 * @since 3.5.0
 */
public abstract class AbstractStatement implements Statement {

	private static final long serialVersionUID = 2087591563645988076L;

	@Override
	public boolean equals(Object o) {

		// We check object equality first since it's most likely to be different. In general the number of different
		// predicates and contexts in sets of statements are the smallest (and therefore most likely to be identical),
		// so these are checked last.

		return this == o || o instanceof Statement
				&& getObject().equals(((Statement) o).getObject())
				&& getSubject().equals(((Statement) o).getSubject())
				&& getPredicate().equals(((Statement) o).getPredicate())
				&& Objects.equals(getContext(), ((Statement) o).getContext());
	}

	@Override
	public int hashCode() {
		// Inlined Objects.hash(getSubject(), getPredicate(), getObject(), getContext()) to avoid array creationg
		int result = 1;
		result = 31 * result + (getSubject() == null ? 0 : getSubject().hashCode());
		result = 31 * result + (getPredicate() == null ? 0 : getPredicate().hashCode());
		result = 31 * result + (getObject() == null ? 0 : getObject().hashCode());
		result = 31 * result + (getContext() == null ? 0 : getContext().hashCode());
		return result;
	}

	@Override
	public String toString() {
		return "("
				+ getSubject()
				+ ", " + getPredicate()
				+ ", " + getObject()
				+ (getContext() == null ? "" : ", " + getContext())
				+ ")";
	}

}
