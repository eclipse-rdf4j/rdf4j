/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.inlined;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.sail.lmdb.ValueIds;

public class Booleans {

	static long packBoolean(Literal literal) {
		return ValueIds.createId(ValueIds.T_BOOLEAN, literal.booleanValue() ? 1L : 0L);
	}

	static Literal unpackBoolean(long value, ValueFactory valueFactory) {
		return valueFactory.createLiteral(ValueIds.getValue(value) != 0);
	}
}
