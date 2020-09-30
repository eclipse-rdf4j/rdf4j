/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

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
				&& Objects.equals(getObject(), ((Statement) o).getObject())
				&& Objects.equals(getSubject(), ((Statement) o).getSubject())
				&& Objects.equals(getPredicate(), ((Statement) o).getPredicate())
				&& Objects.equals(getContext(), ((Statement) o).getContext());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(getSubject())
				^ Objects.hashCode(getPredicate())
				^ Objects.hashCode(getObject())
				^ Objects.hashCode(getContext());
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
