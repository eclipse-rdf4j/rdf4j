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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.commons.codec.binary.Hex;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.eclipse.rdf4j.sail.nativerdf.ValueStoreRevision;

import com.google.common.net.UrlEscapers;

/**
 * CorruptIRIOrBNode is used when a NativeValue cannot be read from the ValueStore and if soft failure is enabled
 *
 * @see NativeStore#SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES .
 *
 * @author Håvard M. Ottestad
 */
public class CorruptIRIOrBNode extends CorruptValue implements IRI, BNode {

	private static final long serialVersionUID = 3709784393454516043L;

	public CorruptIRIOrBNode(ValueStoreRevision revision, int internalID, byte[] data) {
		super(revision, internalID, data);
	}

	@Override
	public String toString() {
		return stringValue();
	}

	public String stringValue() {
		try {
			return getNamespace() + ":" + getLocalName();
		} catch (Throwable ignored) {
		}

		return "CorruptIRIOrBNode_with_ID_" + getInternalID();
	}

	@Override
	public Type getType() {
		return IRI.super.getType();
	}

	@Override
	public String getNamespace() {
		return "urn:CorruptIRIOrBNode:";
	}

	@Override
	public String getLocalName() {
		byte[] data = getData();
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
				return "CORRUPT_ID_" + getInternalID() + "_all_" + data.length + "_data_bytes_are_0x00";
			}

			data = truncateData(data);

			// 1) Try full UTF-8 decode of the slice
			if (data.length > 0) {
				try {
					String utf8 = new String(data, StandardCharsets.UTF_8);
					// If replacement character is not present, we got a clean decode
					if (utf8.indexOf('\uFFFD') < 0) {
						return "CORRUPT_" + UrlEscapers.urlPathSegmentEscaper().escape(utf8);
					}
				} catch (Throwable ignored) {
					// fall through to recovery strategies
				}
			}

			// 2) Try to narrow down to a valid UTF-8 decodable substring (avoid replacement char)
			String recoveredUtf8 = null;
			int bestByteLen = 0;
			for (int start = 0; start < data.length; start++) {
				for (int end = data.length; end > start; end--) {
					int candidateLen = end - start;
					if (candidateLen <= bestByteLen) {
						break; // can't beat current best
					}
					try {
						String s = new String(data, start, candidateLen, StandardCharsets.UTF_8);
						if (s.indexOf('\uFFFD') < 0) {
							recoveredUtf8 = s;
							bestByteLen = candidateLen;
							break; // no need to try smaller end for this start
						}
					} catch (Throwable ignored) {
						// continue scanning
					}
				}
			}
			if (recoveredUtf8 != null && !recoveredUtf8.isEmpty()) {
				return "CORRUPT_" + UrlEscapers.urlPathSegmentEscaper().escape(recoveredUtf8);
			}

			// 3) Try ASCII: find the longest contiguous run of printable US-ASCII bytes and use that
			int bestAsciiStart = -1;
			int bestAsciiLen = 0;
			int i = 0;
			while (i < data.length) {
				// printable ASCII range 0x20 (space) to 0x7E (~)
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
				return "CORRUPT_" + UrlEscapers.urlPathSegmentEscaper().escape(ascii);
			}

			// 4) Fallback: hex-encode the entire raw data
			return "CORRUPT_" + Hex.encodeHexString(Arrays.copyOfRange(data, 0, data.length));
		}

		return "CORRUPT_ID_" + getInternalID();
	}

	@Override
	public String getID() {
		return "";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o instanceof CorruptIRIOrBNode && getInternalID() != NativeValue.UNKNOWN_ID) {
			CorruptIRIOrBNode otherCorruptValue = (CorruptIRIOrBNode) o;

			if (otherCorruptValue.getInternalID() != NativeValue.UNKNOWN_ID
					&& getValueStoreRevision().equals(otherCorruptValue.getValueStoreRevision())) {
				// CorruptValue is from the same revision of the same native store with both IDs set
				return getInternalID() == otherCorruptValue.getInternalID();
			}
		}

		return super.equals(o);
	}

}
