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
package org.eclipse.rdf4j.rio.hdt;

/**
 * HDT Array factory.
 *
 * @author Bart Hanssens
 */
class HDTTriplesSectionFactory {

	static HDTTriplesSection parse(String str) {
		if (!str.equals(new String(HDTTriples.FORMAT_BITMAP))) {
			throw new UnsupportedOperationException(
					"Triples section: " + str + ", but only bitmap encoding is supported");
		}
		return new HDTTriplesSectionBitmap();
	}
}
