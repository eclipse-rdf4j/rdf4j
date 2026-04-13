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
		return switch (xsdDataType) {
		case DECIMAL -> packDecimal(literal.decimalValue());
		case DOUBLE -> packDouble(literal.doubleValue());
		case FLOAT -> packFloat(literal.floatValue());
		case INTEGER -> packInteger(literal);
		case LONG -> packLong(literal);
		case INT -> packInt(literal);
		case SHORT -> packShort(literal);
		case BYTE -> packByte(literal);
		case UNSIGNED_LONG -> packUnsignedLong(literal);
		case UNSIGNED_INT -> packUnsignedInt(literal);
		case UNSIGNED_SHORT -> packUnsignedShort(literal);
		case UNSIGNED_BYTE -> packUnsignedByte(literal);
		case POSITIVE_INTEGER -> packPositiveInteger(literal);
		case NEGATIVE_INTEGER -> packNegativeInteger(literal);
		case NON_NEGATIVE_INTEGER -> packNonNegativeInteger(literal);
		case NON_POSITIVE_INTEGER -> packNonPositiveInteger(literal);
		case STRING -> packString(literal);
		case DATETIME -> packDateTime(literal);
		case DATETIMESTAMP -> packDateTimeStamp(literal);
		case DATE -> packDate(literal);
		case BOOLEAN -> packBoolean(literal);
		default ->
			// unsupported type
			0L;
		};
	}

	public static Literal unpackLiteral(long value, ValueFactory valueFactory) {
		int idType = ValueIds.getIdType(value);
		return switch (idType) {
		case ValueIds.T_DOUBLE -> unpackDouble(value, valueFactory);
		case ValueIds.T_DECIMAL -> unpackDecimal(value, valueFactory);
		case ValueIds.T_FLOAT -> unpackFloat(value, valueFactory);
		case ValueIds.T_INTEGER -> unpackInteger(value, valueFactory);
		case ValueIds.T_LONG -> unpackLong(value, valueFactory);
		case ValueIds.T_INT -> unpackInt(value, valueFactory);
		case ValueIds.T_SHORT -> unpackShort(value, valueFactory);
		case ValueIds.T_BYTE -> unpackByte(value, valueFactory);
		case ValueIds.T_UNSIGNEDLONG -> unpackUnsignedLong(value, valueFactory);
		case ValueIds.T_UNSIGNEDINT -> unpackUnsignedInt(value, valueFactory);
		case ValueIds.T_UNSIGNEDSHORT -> unpackUnsignedShort(value, valueFactory);
		case ValueIds.T_UNSIGNEDBYTE -> unpackUnsignedByte(value, valueFactory);
		case ValueIds.T_POSITIVE_INTEGER -> unpackPositiveInteger(value, valueFactory);
		case ValueIds.T_NEGATIVE_INTEGER -> unpackNegativeInteger(value, valueFactory);
		case ValueIds.T_NON_NEGATIVE_INTEGER -> unpackNonNegativeInteger(value, valueFactory);
		case ValueIds.T_NON_POSITIVE_INTEGER -> unpackNonPositiveInteger(value, valueFactory);
		case ValueIds.T_SHORTSTRING -> unpackString(value, valueFactory);
		case ValueIds.T_DATETIME -> unpackDateTime(value, valueFactory);
		case ValueIds.T_DATETIMESTAMP -> unpackDateTimeStamp(value, valueFactory);
		case ValueIds.T_DATE -> unpackDate(value, valueFactory);
		case ValueIds.T_BOOLEAN -> unpackBoolean(value, valueFactory);
		default -> throw new IllegalArgumentException("Invalid packed value " + value + " with id type: " + idType);
		};
	}
}
