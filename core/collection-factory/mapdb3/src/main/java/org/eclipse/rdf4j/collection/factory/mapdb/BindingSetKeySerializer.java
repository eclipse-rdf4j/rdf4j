/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.collection.factory.mapdb;

import java.io.IOException;

import org.eclipse.rdf4j.collection.factory.api.BindingSetKey;
import org.eclipse.rdf4j.collection.factory.mapdb.MapDb3CollectionFactory.RDF4jMapDB3Exception;
import org.eclipse.rdf4j.model.Value;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;
import org.mapdb.serializer.SerializerBoolean;
import org.mapdb.serializer.SerializerIntegerPacked;

class BindingSetKeySerializer implements Serializer<BindingSetKey> {

	private final SerializerBoolean sb = new SerializerBoolean();
	private final SerializerIntegerPacked si = new SerializerIntegerPacked();
	private final Serializer<Value> vs;

	public BindingSetKeySerializer(Serializer<Value> vs) {
		this.vs = vs;
	}

	@Override
	public void serialize(DataOutput2 out, BindingSetKey value) throws IOException {
		if (value == null) {
			sb.serialize(out, true);
		} else {
			sb.serialize(out, false);
			try {
				MapDb3BindingSetKey k = (MapDb3BindingSetKey) value;
				si.serialize(out, k.hashCode());
				si.serialize(out, k.values.length);
				for (int i = 0; i < k.values.length; i++) {
					vs.serialize(out, k.values[i]);
				}
			} catch (ClassCastException e) {
				throw new RDF4jMapDB3Exception("Collection factory overriden in an non conformat way", e);
			}
		}

	}

	@Override
	public BindingSetKey deserialize(DataInput2 input, int available) throws IOException {
		if (sb.deserialize(input, available)) {
			return null;
		} else {
			int hashCode = si.deserialize(input, available);
			int length = si.deserialize(input, available);
			Value[] values = new Value[length];
			for (int i = 0; i < length; i++) {
				values[i] = vs.deserialize(input, available);
			}
			return new MapDb3BindingSetKey(values, hashCode);
		}
	}

}
