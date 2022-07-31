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

import java.util.Collection;

/**
 * An iteration to access a materialized {@link Collection} of BindingSets.
 *
 * @author Andreas Schwarte
 * @deprecated since 2.3 use {@link org.eclipse.rdf4j.repository.sparql.federation.CollectionIteration}
 */
@Deprecated
public class CollectionIteration<E, X extends Exception>
		extends org.eclipse.rdf4j.repository.sparql.federation.CollectionIteration<E, X> {

	public CollectionIteration(Collection<E> collection) {
		super(collection);
	}
}
