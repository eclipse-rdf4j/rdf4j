/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.model.base;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

/**
 * An IRI implementation that interns the stringValue so that two objects can be compared by their stringValue
 * reference. Must only be used for IRIs that are effectively ´public static final´ and only for a very limited number
 * of objects because string interning affects the GC root set
 * (https://shipilev.net/jvm/anatomy-quarks/10-string-intern/).
 *
 */
@InternalUseOnly
public final class InternedIRI implements IRI {
	private static final long serialVersionUID = 169243429049169159L;

	private final String namespace;
	private final String localName;
	private final String stringValue;
	private final int hashCode;

	public InternedIRI(String namespace, String localName) {
		this.namespace = namespace;
		this.localName = localName;
		this.stringValue = namespace.concat(localName).intern();
		this.hashCode = stringValue.hashCode();
	}

	@Override
	public String stringValue() {
		return stringValue;
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
	public String toString() {
		return stringValue;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o instanceof InternedIRI) {
			// Because the stringValue is interned we can simply compare the reference.
			return stringValue == ((InternedIRI) o).stringValue;
		}

		if (o instanceof Value) {
			Value value = (Value) o;
			if (value.isIRI()) {
				return stringValue.equals(value.stringValue());
			}
		}

		return false;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeInt(hashCode);
		out.writeUTF(namespace);
		out.writeUTF(localName);
	}

	private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
		int hashCode = in.readInt();
		String namespace = in.readUTF();
		String localName = in.readUTF();

		// Deserialization in Java typically uses Unsafe to set final fields, we need to use reflection.
		writeToPrivateFinalField(namespace, "namespace");
		writeToPrivateFinalField(localName, "localName");
		writeToPrivateFinalField(hashCode, "hashCode");

		// The main reason we need a custom deserialization is that we need to intern the stringValue field.
		String stringValue = (namespace + localName).intern();
		writeToPrivateFinalField(stringValue, "stringValue");
		assert stringValue.hashCode() == hashCode;
	}

	private void writeToPrivateFinalField(String value, String fieldName) {
		try {
			Field field = InternedIRI.class.getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(this, value);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private void writeToPrivateFinalField(int value, String fieldName) {
		try {
			Field field = InternedIRI.class.getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(this, value);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

}
