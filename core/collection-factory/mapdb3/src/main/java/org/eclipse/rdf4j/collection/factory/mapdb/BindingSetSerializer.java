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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;
import org.mapdb.serializer.SerializerBoolean;
import org.mapdb.serializer.SerializerIntegerPacked;

class BindingSetSerializer implements Serializer<BindingSet> {

	// Insertion order is very important
	private final LinkedHashSet<String> names = new LinkedHashSet<String>();
	private String[] namesInOrder = null;
	private final Serializer<Value> vs;
	private final SerializerBoolean sb = new SerializerBoolean();
	private final SerializerIntegerPacked si = new SerializerIntegerPacked();

	public BindingSetSerializer(Serializer<Value> valueSerializer) {
		vs = valueSerializer;
	}

	@Override
	public void serialize(DataOutput2 out, BindingSet bs) throws IOException {

		final Set<String> bindingNames = bs.getBindingNames();
		if (names.addAll(bindingNames)) {
			// new name found
			namesInOrder = new String[names.size()];
			int i = 0;
			final Iterator<String> nameI = names.iterator();
			while (nameI.hasNext()) {
				String name = nameI.next();
				namesInOrder[i++] = name;
			}
		}
		// all binding names where present
		int i = 0;
		final Iterator<String> nameI = names.iterator();
		while (nameI.hasNext()) {
			String name = nameI.next();
			if (bs.hasBinding(name)) {
				sb.serialize(out, true);
				si.serialize(out, i);
				vs.serialize(out, bs.getValue(name));
			}
			i++;
		}
		sb.serialize(out, false); // marks the end
	}

	@Override
	public BindingSet deserialize(DataInput2 input, int available) throws IOException {
		boolean hasMore = sb.deserialize(input, available);
		MapBindingSet bs = new MapBindingSet();
		while (hasMore) {
			int nextName = si.deserialize(input, available);
			Value nextValue = vs.deserialize(input, available);
			bs.setBinding(namesInOrder[nextName], nextValue);
			hasMore = sb.deserialize(input, available);
		}
		return bs;
	}

}
