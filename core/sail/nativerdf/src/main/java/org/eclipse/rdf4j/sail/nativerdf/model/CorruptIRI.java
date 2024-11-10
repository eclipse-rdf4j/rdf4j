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

import org.apache.commons.codec.binary.Hex;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.eclipse.rdf4j.sail.nativerdf.ValueStoreRevision;

import com.google.common.net.UrlEscapers;

/**
 * CorruptIRI is used when a NativeValue cannot be read from the ValueStore and if soft failure is enabled
 *
 * @see NativeStore#SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES .
 *
 * @author Håvard M. Ottestad
 */
public class CorruptIRI extends CorruptValue implements IRI {

	private static final long serialVersionUID = -6995615243794525852L;
	private final String namespace;

	public CorruptIRI(ValueStoreRevision revision, int internalID, String namespace, byte[] data) {
		super(revision, internalID, data);
		this.namespace = namespace;
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

		return "CorruptIRI_with_ID_" + getInternalID();
	}

	@Override
	public String getNamespace() {
		if (namespace != null && !namespace.isEmpty()) {
			return namespace;
		}
		return "urn:CorruptIRI:";
	}

	@Override
	public String getLocalName() {
		byte[] data = getData();
		if (data != null && data.length < 1024) {
			try {
				String localName = new String(data, 5, data.length - 5, StandardCharsets.UTF_8);
				return "CORRUPT_" + UrlEscapers.urlPathSegmentEscaper().escape(localName);
			} catch (Throwable ignored) {
			}

			return "CORRUPT_" + Hex.encodeHexString(data);
		}

		return "CORRUPT";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o instanceof CorruptIRI && getInternalID() != NativeValue.UNKNOWN_ID) {
			CorruptIRI otherCorruptValue = (CorruptIRI) o;

			if (otherCorruptValue.getInternalID() != NativeValue.UNKNOWN_ID
					&& getValueStoreRevision().equals(otherCorruptValue.getValueStoreRevision())) {
				// CorruptValue is from the same revision of the same native store with both IDs set
				return getInternalID() == otherCorruptValue.getInternalID();
			}
		}

		return super.equals(o);
	}

}
