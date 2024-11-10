/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.nativerdf.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.eclipse.rdf4j.sail.nativerdf.ValueStoreRevision;

/**
 * CorruptLiteral is used when a NativeValue cannot be read from the ValueStore and if soft failure is enabled
 *
 * @see NativeStore#SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES .
 *
 * @author Håvard M. Ottestad
 */
public class CorruptLiteral extends CorruptValue implements Literal {

	private static final long serialVersionUID = -2510885288827542623L;

	private static final IRI CORRUPT = Values.iri("urn:corrupt");

	public CorruptLiteral(ValueStoreRevision revision, int internalID, byte[] data) {
		super(revision, internalID, data);
	}

	public String stringValue() {
		return "CorruptLiteral_with_ID_" + getInternalID();
	}

	@Override
	public String getLabel() {
		byte[] data = getData();
		try {
			if (data != null && data.length < 1024) {
				return "CorruptUnknownValue with ID " + getInternalID() + " with possible data: "
						+ new String(data, StandardCharsets.UTF_8);
			}
		} catch (Throwable ignored) {
		}
		return "CorruptUnknownValue_with_ID_" + getInternalID();
	}

	@Override
	public Optional<String> getLanguage() {
		return Optional.empty();
	}

	@Override
	public IRI getDatatype() {
		return CORRUPT;
	}

	@Override
	public boolean booleanValue() {
		return false;
	}

	@Override
	public byte byteValue() {
		return 0;
	}

	@Override
	public short shortValue() {
		return 0;
	}

	@Override
	public int intValue() {
		return 0;
	}

	@Override
	public long longValue() {
		return 0;
	}

	@Override
	public BigInteger integerValue() {
		return null;
	}

	@Override
	public BigDecimal decimalValue() {
		return null;
	}

	@Override
	public float floatValue() {
		return 0;
	}

	@Override
	public double doubleValue() {
		return 0;
	}

	@Override
	public XMLGregorianCalendar calendarValue() {
		return null;
	}

	@Override
	public CoreDatatype getCoreDatatype() {
		return null;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o instanceof CorruptLiteral && getInternalID() != NativeValue.UNKNOWN_ID) {
			CorruptLiteral otherCorruptValue = (CorruptLiteral) o;

			if (otherCorruptValue.getInternalID() != NativeValue.UNKNOWN_ID
					&& getValueStoreRevision().equals(otherCorruptValue.getValueStoreRevision())) {
				// CorruptValue is from the same revision of the same native store with both IDs set
				return getInternalID() == otherCorruptValue.getInternalID();
			}
		}

		return super.equals(o);
	}

}
