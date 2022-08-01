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
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.io.Serializable;
import java.util.Comparator;

import org.eclipse.rdf4j.query.BindingSet;

class SizeComparator implements Comparator<BindingSet>, Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public int compare(BindingSet o1, BindingSet o2) {
		return Integer.valueOf(o1.size()).compareTo(o2.size());
	}
}
