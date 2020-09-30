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
import org.eclipse.rdf4j.model.Triple;

public abstract class AbstractTriple implements Triple {

	private static final long serialVersionUID = 2661609986803671844L;

	@Override
	public String stringValue() {
		return "<<" + getSubject() + " " + getPredicate() + " " + getObject() + ">>";
	}

	@Override
	public boolean equals(Object o) {

		// We check object equality first since it's most likely to be different. In general the number of different
		// predicates and contexts in sets of statements are the smallest (and therefore most likely to be identical),
		// so these are checked last.

		return this == o || o instanceof Triple
				&& Objects.equals(getObject(), ((Triple) o).getObject())
				&& Objects.equals(getSubject(), ((Triple) o).getSubject())
				&& Objects.equals(getPredicate(), ((Triple) o).getPredicate());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(getSubject())
				^ Objects.hashCode(getPredicate())
				^ Objects.hashCode(getObject());
	}

	@Override
	public String toString() {
		return stringValue();
	}

}
