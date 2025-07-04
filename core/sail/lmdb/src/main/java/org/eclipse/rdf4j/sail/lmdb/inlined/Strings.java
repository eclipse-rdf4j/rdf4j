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

import java.nio.charset.StandardCharsets;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype.XSD;
import org.eclipse.rdf4j.sail.lmdb.ValueIds;

public class Strings {

	static long packString(Literal literal) {
		String label = literal.getLabel();
		if (label.length() > Values.MAX_LENGTH) {
			// in any case string is longer than maximum encodable length
			return 0L;
		}
		byte[] bytes = label.getBytes(StandardCharsets.UTF_8);
		int maxLength = Values.MAX_LENGTH - 1;
		if (bytes.length > maxLength) {
			// multi-byte string is longer than maximum encodable length
			return 0L;
		}

		return ValueIds.createId(ValueIds.T_SHORTSTRING, Bytes.packBytes(bytes) << 8 | bytes.length);
	}

	static Literal unpackString(long value, ValueFactory valueFactory) {
		value = ValueIds.getValue(value);
		int length = (int) (value & 0xFF);
		String strValue = new String(Bytes.unpackBytes(value >>> 8, length), StandardCharsets.UTF_8);
		return valueFactory.createLiteral(strValue, XSD.STRING);
	}
}