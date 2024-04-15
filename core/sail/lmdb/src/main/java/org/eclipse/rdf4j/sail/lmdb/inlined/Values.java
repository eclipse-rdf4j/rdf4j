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

import static org.eclipse.rdf4j.sail.lmdb.inlined.Booleans.*;
import static org.eclipse.rdf4j.sail.lmdb.inlined.Dates.*;
import static org.eclipse.rdf4j.sail.lmdb.inlined.Decimals.*;
import static org.eclipse.rdf4j.sail.lmdb.inlined.Integers.*;
import static org.eclipse.rdf4j.sail.lmdb.inlined.Strings.*;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.base.CoreDatatype.XSD;
import org.eclipse.rdf4j.sail.lmdb.ValueIds;

/**
 * Functions for inlining of values into long ids.
 */
public class Values {
	/**
	 * Maximum length of inlined values in bytes.
	 */
	static int MAX_LENGTH = 7;

	public static long packLiteral(Literal literal) {
		XSD xsdDataType = literal.getCoreDatatype().asXSDDatatypeOrNull();
		if (xsdDataType == null) {
			return 0L;
		}
		switch (xsdDataType) {
		case DECIMAL:
			return packDecimal(literal.decimalValue());
		case DOUBLE:
			return packDouble(literal.doubleValue());
		case FLOAT:
			return packFloat(literal.floatValue());
		case INTEGER:
			return packInteger(literal);
		case LONG:
			return packLong(literal);
		case INT:
			return packInt(literal);
		case SHORT:
			return packShort(literal);
		case BYTE:
			return packByte(literal);
		case UNSIGNED_LONG:
			return packUnsignedLong(literal);
		case UNSIGNED_INT:
			return packUnsignedInt(literal);
		case UNSIGNED_SHORT:
			return packUnsignedShort(literal);
		case UNSIGNED_BYTE:
			return packUnsignedByte(literal);
		case POSITIVE_INTEGER:
			return packPositiveInteger(literal);
		case NEGATIVE_INTEGER:
			return packNegativeInteger(literal);
		case NON_NEGATIVE_INTEGER:
			return packNonNegativeInteger(literal);
		case NON_POSITIVE_INTEGER:
			return packNonPositiveInteger(literal);
		case STRING:
			return packString(literal);
		case DATETIME:
			return packDateTime(literal);
		case DATETIMESTAMP:
			return packDateTimeStamp(literal);
		case DATE:
			return packDate(literal);
		case BOOLEAN:
			return packBoolean(literal);
		default:
			// unsupported type
			return 0L;
		}
	}

	public static Literal unpackLiteral(long value, ValueFactory valueFactory) {
		// special handling for double values
		if (isDouble(value)) {
			return unpackDouble(value, valueFactory);
		}

		int idType = ValueIds.getIdType(value);

		switch (idType) {
		case ValueIds.T_DECIMAL:
			return unpackDecimal(value, valueFactory);
		case ValueIds.T_FLOAT:
			return unpackFloat(value, valueFactory);
		case ValueIds.T_INTEGER:
			return unpackInteger(value, valueFactory);
		case ValueIds.T_LONG:
			return unpackLong(value, valueFactory);
		case ValueIds.T_INT:
			return unpackInt(value, valueFactory);
		case ValueIds.T_SHORT:
			return unpackShort(value, valueFactory);
		case ValueIds.T_BYTE:
			return unpackByte(value, valueFactory);
		case ValueIds.T_UNSIGNEDLONG:
			return unpackUnsignedLong(value, valueFactory);
		case ValueIds.T_UNSIGNEDINT:
			return unpackUnsignedInt(value, valueFactory);
		case ValueIds.T_UNSIGNEDSHORT:
			return unpackUnsignedShort(value, valueFactory);
		case ValueIds.T_UNSIGNEDBYTE:
			return unpackUnsignedByte(value, valueFactory);
		case ValueIds.T_POSITIVE_INTEGER:
			return unpackPositiveInteger(value, valueFactory);
		case ValueIds.T_NEGATIVE_INTEGER:
			return unpackNegativeInteger(value, valueFactory);
		case ValueIds.T_NON_NEGATIVE_INTEGER:
			return unpackNonNegativeInteger(value, valueFactory);
		case ValueIds.T_NON_POSITIVE_INTEGER:
			return unpackNonPositiveInteger(value, valueFactory);
		case ValueIds.T_SHORTSTRING:
			return unpackString(value, valueFactory);
		case ValueIds.T_DATETIME:
			return unpackDateTime(value, valueFactory);
		case ValueIds.T_DATETIMESTAMP:
			return unpackDateTimeStamp(value, valueFactory);
		case ValueIds.T_DATE:
			return unpackDate(value, valueFactory);
		case ValueIds.T_BOOLEAN:
			return unpackBoolean(value, valueFactory);
		default:
			throw new IllegalArgumentException("Invalid packed value with id type: " + idType);
		}
	}
}
