/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.query.parser.serql.ast;

import java.util.AbstractList;
import java.util.List;

/**
 * A list that wraps another list and casts its elements to a specific subtype of the list's element type.
 */
class CastingList<E> extends AbstractList<E> {

	protected List<? super E> _elements;

	public CastingList(List<? super E> elements) {
		_elements = elements;
	}

	@SuppressWarnings("unchecked")
	@Override
	public E get(int index) {
		return (E) _elements.get(index);
	}

	@Override
	public int size() {
		return _elements.size();
	}
}
