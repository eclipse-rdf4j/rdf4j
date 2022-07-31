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
 * Helper class for working with Strings as sequences of Unicode code points.
 *
 * @author Jeen Broekstra
 * @see CodePointIterator
 */
public class CodePointSequence implements Iterable<Integer> {

	private final String charSequence;

	public CodePointSequence(String charSequence) {
		this.charSequence = charSequence;
	}

	@Override
	public Iterator<Integer> iterator() {
		return new CodePointIterator(charSequence);
	}

	public int length() {
		return charSequence.codePointCount(0, charSequence.length());
	}

}
