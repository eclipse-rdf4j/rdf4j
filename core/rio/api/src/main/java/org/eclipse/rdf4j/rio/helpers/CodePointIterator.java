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
package org.eclipse.rdf4j.rio.helpers;

import java.util.Iterator;

/**
 * Helper class for quickly iterating over a String and receiving each character code point (taking care to handle
 * surrogate pairs correctly).
 *
 * @author Jeen Broekstra
 * @see CodePointSequence
 */
public class CodePointIterator implements Iterator<Integer> {

	private final String source;

	private int index = 0;

	public CodePointIterator(String source) {
		this.source = source;
	}

	@Override
	public boolean hasNext() {
		return index < source.length();
	}

	@Override
	public Integer next() {
		int codePoint = source.codePointAt(index);
		index += Character.charCount(codePoint);
		return codePoint;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

}
