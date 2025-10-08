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

import org.apache.commons.codec.binary.Hex;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.eclipse.rdf4j.sail.nativerdf.ValueStoreRevision;

/**
 * CorruptUnknownValue is used when a NativeValue cannot be read from the ValueStore and if soft failure is enabled
 *
 * @see NativeStore#SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES .
 *
 * @author Håvard M. Ottestad
 */
public class CorruptUnknownValue extends CorruptValue implements Literal {

	private static final long serialVersionUID = -6650510290226676279L;
	private final String hex;

	public CorruptUnknownValue(ValueStoreRevision revision, int internalID, byte[] data) {
		super(revision, internalID, data);
		var truncated = data;
		if (truncated.length > 1024) {
			truncated = new byte[1024];
			System.arraycopy(data, 0, truncated, 0, 1024);
		}
		this.hex = Hex.encodeHexString(truncated);
	}

	@Override
	public String getLabel() {
		byte[] data = getData();
		try {
			if (data != null && data.length > 0) {
				String prefix = "CorruptUnknownValue with ID " + getInternalID() + " with possible data: ";

				// truncate data to first 1024 bytes
				if (data.length > 1024) {
					byte[] truncated = new byte[1024];
					System.arraycopy(data, 0, truncated, 0, 1024);
					data = truncated;
				}


				// 1) Try full UTF-8 decode
				try {
					String utf8 = new String(data, StandardCharsets.UTF_8);
					if (utf8.indexOf('\uFFFD') < 0) {
						return prefix + utf8;
					}
				} catch (Throwable ignored) {
					// continue with recovery paths
				}

				// 2) Longest clean UTF-8 substring (no replacement char)
				String recoveredUtf8 = null;
				int bestLen = 0;
				for (int start = 0; start < data.length; start++) {
					for (int end = data.length; end > start; end--) {
						int len = end - start;
						if (len <= bestLen) {
							break; // can't beat best
						}
						try {
							String s = new String(data, start, len, StandardCharsets.UTF_8);
							if (s.indexOf('\uFFFD') < 0) {
								recoveredUtf8 = s;
								bestLen = len;
								break; // shorter end won't beat this start
							}
						} catch (Throwable ignored) {
							// keep scanning
						}
					}
				}
				if (recoveredUtf8 != null && !recoveredUtf8.isEmpty()) {
					return prefix + recoveredUtf8;
				}

				// 3) Longest contiguous printable ASCII run
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

				// 4) Fallback to hex of full data
				return prefix + Hex.encodeHexString(data);
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
		return XSD.STRING;
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

		if (o instanceof CorruptUnknownValue && getInternalID() != NativeValue.UNKNOWN_ID) {
			CorruptUnknownValue otherCorruptValue = (CorruptUnknownValue) o;

			if (otherCorruptValue.getInternalID() != NativeValue.UNKNOWN_ID
					&& getValueStoreRevision().equals(otherCorruptValue.getValueStoreRevision())) {
				// CorruptValue is from the same revision of the same native store with both IDs set
				return getInternalID() == otherCorruptValue.getInternalID();
			}
		}

		return super.equals(o);
	}

	@Override
	public String toString() {
		return getLabel();
	}

}
