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
import java.util.Arrays;
import java.util.Optional;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.codec.binary.Hex;
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

	@Override
	public String stringValue() {
		return "CorruptLiteral_with_ID_" + getInternalID() + ": " + getLabel();
	}

	@Override
	public String getLabel() {
		byte[] data = getData();
		try {
			if (data != null && data.length > 0) {
				// check if all bytes are zero
				boolean allZero = true;
				for (byte b : data) {
					if (b != 0) {
						allZero = false;
						break;
					}
				}

				if (allZero) {
					return "All " + data.length + " data bytes are 0x00";
				}

				String prefix = this.getClass().getSimpleName() + " with ID " + getInternalID()
						+ " with possible data: ";

				data = truncateData(data);

				// 1) Try full UTF-8 decode of the slice
				try {
					String utf8 = new String(data, StandardCharsets.UTF_8);
					if (utf8.indexOf('\uFFFD') < 0) {
						return prefix + utf8;
					}
				} catch (Throwable ignored) {
				}

				// 2) Longest clean UTF-8 substring
				String recoveredUtf8 = null;
				int bestLen = 0;
				for (int start = 0; start < data.length; start++) {
					for (int end = data.length; end > start; end--) {
						int len = end - start;
						if (len <= bestLen) {
							break;
						}
						try {
							String s = new String(data, start, len, StandardCharsets.UTF_8);
							if (s.indexOf('\uFFFD') < 0) {
								recoveredUtf8 = s;
								bestLen = len;
								break;
							}
						} catch (Throwable ignored) {
						}
					}
				}
				if (recoveredUtf8 != null && !recoveredUtf8.isEmpty()) {
					return prefix + recoveredUtf8;
				}

				// 3) Longest contiguous printable ASCII run in slice
				int bestAsciiStart = -1;
				int bestAsciiLen = 0;
				int i = 0;
				while (i < data.length) {
					if (data[i] >= 0x20 && data[i] <= 0x7E) {
						int runStart = i;
						while (i < data.length && data[i] >= 0x20 && data[i] <= 0x7E) {
							i++;
						}
						int runLen = i - runStart;
						if (runLen > bestAsciiLen) {
							bestAsciiLen = runLen;
							bestAsciiStart = runStart;
						}
					} else {
						i++;
					}
				}
				if (bestAsciiLen > 0) {
					String ascii = new String(data, bestAsciiStart, bestAsciiLen, StandardCharsets.US_ASCII);
					return prefix + ascii;
				}

				// 4) Fallback: hex encode only up to sentinel data.length
				return prefix + Hex.encodeHexString(Arrays.copyOfRange(data, 0, data.length));
			}
		} catch (Throwable ignored) {
		}
		return this.getClass().getSimpleName() + " with ID " + getInternalID();
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
