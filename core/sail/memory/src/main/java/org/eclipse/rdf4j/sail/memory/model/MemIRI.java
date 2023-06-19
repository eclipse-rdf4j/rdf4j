/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory.model;

import java.lang.ref.SoftReference;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

/**
 * A MemoryStore-specific implementation of URI that stores separated namespace and local name information to enable
 * reuse of namespace String objects (reducing memory usage) and that gives it node properties.
 */
public class MemIRI extends MemResource implements IRI {

	private static final long serialVersionUID = 9118488004995852467L;

	/*------------*
	 * Attributes *
	 *------------*/

	/**
	 * The URI's namespace.
	 */
	private final String namespace;

	/**
	 * The URI's local name.
	 */
	private final String localName;

	/**
	 * The object that created this MemURI.
	 */
	transient private final Object creator;

	/**
	 * The MemURI's hash code, 0 if not yet initialized.
	 */
	private volatile int hashCode = 0;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new MemURI for a URI.
	 *
	 * @param creator   The object that is creating this MemURI.
	 * @param namespace namespace part of URI.
	 * @param localName localname part of URI.
	 */
	public MemIRI(Object creator, String namespace, String localName) {
		this.creator = creator;
		this.namespace = namespace;
		this.localName = localName;
	}

	/*---------*
	 * Methods *
	 *---------*/

	transient SoftReference<String> toStringCache = null;

	@Override
	public String toString() {
		String result;
		if (toStringCache == null) {
			result = namespace + localName;
			toStringCache = new SoftReference<>(result);
		} else {
			result = toStringCache.get();
			if (result == null) {
				result = namespace + localName;
				toStringCache = new SoftReference<>(result);
			}
		}
		return result;
	}

	@Override
	public String stringValue() {
		return toString();
	}

	@Override
	public String getNamespace() {
		return namespace;
	}

	@Override
	public String getLocalName() {
		return localName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null) {
			return false;
		}

		if (o.getClass() == MemIRI.class) {
			MemIRI oMemIRI = (MemIRI) o;
			if (oMemIRI.creator == creator) {
				// two different MemIRI from the same MemoryStore can not be equal.
				return false;
			}
			return namespace.length() == oMemIRI.namespace.length() &&
					localName.length() == oMemIRI.localName.length() &&
					namespace.equals(oMemIRI.namespace) &&
					localName.equals(oMemIRI.localName);

		}

		if (o instanceof Value) {
			Value oValue = (Value) o;
			if (oValue.isIRI()) {
				String oStr = oValue.stringValue();

				if (toStringCache != null) {
					String stringValue = toStringCache.get();
					if (stringValue != null) {
						return stringValue.equals(oStr);
					}
				}

				return namespace.length() + localName.length() == oStr.length() &&
						oStr.endsWith(localName) &&
						oStr.startsWith(namespace);
			}
		}

		return false;
	}

	@Override
	public int hashCode() {
		if (hashCode == 0) {
			hashCode = stringValue().hashCode();
		}

		return hashCode;
	}

	@Override
	public Object getCreator() {
		return creator;
	}

	@Override
	public boolean hasStatements() {
		return !subjectStatements.isEmpty() || !predicateStatements.isEmpty() || !objectStatements.isEmpty()
				|| !contextStatements.isEmpty();
	}

}
