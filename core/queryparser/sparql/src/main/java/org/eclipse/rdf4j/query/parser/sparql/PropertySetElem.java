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
package org.eclipse.rdf4j.query.parser.sparql;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.query.algebra.ValueConstant;

/**
 * @author Jeen
 *
 * @apiNote This feature is for internal use only: its existence, signature or behavior may change without warning from
 *          one release to the next.
 */
@InternalUseOnly
public class PropertySetElem {

	private boolean inverse;

	private ValueConstant predicate;

	/**
	 * @param inverse The inverse to set.
	 */
	public void setInverse(boolean inverse) {
		this.inverse = inverse;
	}

	/**
	 * @return Returns the inverse.
	 */
	public boolean isInverse() {
		return inverse;
	}

	/**
	 * @param predicate The predicate to set.
	 */
	public void setPredicate(ValueConstant predicate) {
		this.predicate = predicate;
	}

	/**
	 * @return Returns the predicate.
	 */
	public ValueConstant getPredicate() {
		return predicate;
	}
}
