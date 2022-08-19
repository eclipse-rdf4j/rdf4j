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
package org.eclipse.rdf4j.sail.shacl.ast;

public enum ValidationApproach {

	Transactional,
	SPARQL;

	public static final ValidationApproach MOST_COMPATIBLE = Transactional;

	/**
	 * The fastest validation approach for small transactions is usually Transactional validation, however some
	 * constraint components may find that it's going to be faster to validate the constraint using SPARQL validation
	 * against the entire base sail if it thinks that analyzing the transaction is going to be slower.
	 */
	public static ValidationApproach reducePreferred(ValidationApproach a, ValidationApproach b) {
		if (a == SPARQL) {
			return a;
		}
		if (b == SPARQL) {
			return b;
		}

		return a;
	}

	/**
	 * All constraint components must support Transactional validation, so unless both components support SPARQL
	 * validation then we return Transactional validation.
	 */
	public static ValidationApproach reduceCompatible(ValidationApproach a, ValidationApproach b) {
		if (a == MOST_COMPATIBLE) {
			return a;
		}
		if (b == MOST_COMPATIBLE) {
			return b;
		}

		return a;
	}
}
