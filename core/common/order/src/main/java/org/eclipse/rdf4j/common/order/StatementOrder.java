/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.common.order;

import java.util.Comparator;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

/**
 * An enum for the different orders in which statements can be ordered.
 */
@Experimental
public enum StatementOrder {

	S, // Subject
	P, // Predicate
	O, // Object
	C; // Context

	@Experimental
	public Comparator<Statement> getComparator(Comparator<Value> comparator) {
		switch (this) {
		case S:
			return (a, b) -> comparator.compare(a.getSubject(), b.getSubject());
		case P:
			return (a, b) -> comparator.compare(a.getPredicate(), b.getPredicate());
		case O:
			return (a, b) -> comparator.compare(a.getObject(), b.getObject());
		case C:
			return (a, b) -> comparator.compare(a.getContext(), b.getContext());
		}

		throw new IllegalStateException("Unknown StatementOrder: " + this);
	}

}
