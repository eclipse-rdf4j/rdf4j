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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;
import org.mapdb.serializer.SerializerBoolean;
import org.mapdb.serializer.SerializerIntegerPacked;

class BindingSetSerializer implements Serializer<BindingSet> {

	// Insertion order is very important
	private final LinkedHashSet<String> names = new LinkedHashSet<String>();
	private final List<Predicate<BindingSet>> has = new ArrayList<>();
	private final List<Function<BindingSet, Value>> get = new ArrayList<>();
	private final List<BiConsumer<Value, MutableBindingSet>> set = new ArrayList<>();
	private final Serializer<Value> vs;
	private final SerializerBoolean sb = new SerializerBoolean();
	private final SerializerIntegerPacked si = new SerializerIntegerPacked();
	private final Supplier<MutableBindingSet> create;
	private final Function<String, Predicate<BindingSet>> getHas;
	private final Function<String, Function<BindingSet, Value>> getGet;
	private final Function<String, BiConsumer<Value, MutableBindingSet>> getSet;

	public BindingSetSerializer(Serializer<Value> valueSerializer, Supplier<MutableBindingSet> create,
			Function<String, Predicate<BindingSet>> getHas, Function<String, Function<BindingSet, Value>> getGet,
			Function<String, BiConsumer<Value, MutableBindingSet>> getSet) {
		this.vs = valueSerializer;
		this.create = create;
		this.getHas = getHas;
		this.getGet = getGet;
		this.getSet = getSet;
	}

	@Override
	public void serialize(DataOutput2 out, BindingSet bs) throws IOException {

		final Set<String> bindingNames = bs.getBindingNames();
		if (names.addAll(bindingNames)) {
			// new name found
			int i = has.size();
			final Iterator<String> nameI = names.iterator();
			// Don't get setters that we already got.
			while (nameI.hasNext() && i > 0) {
				i--;
			}
			while (nameI.hasNext()) {
				String name = nameI.next();
				has.add(getHas.apply(name));
				get.add(getGet.apply(name));
				set.add(getSet.apply(name));
			}
		}
		// all binding names where present
		for (int i = 0; i < has.size(); i++) {
			if (has.get(i).test(bs)) {
				sb.serialize(out, true);
				si.serialize(out, i);
				vs.serialize(out, get.get(i).apply(bs));
			}
		}
		sb.serialize(out, false); // marks the end
	}

	@Override
	public BindingSet deserialize(DataInput2 input, int available) throws IOException {
		boolean hasMore = sb.deserialize(input, available);
		MutableBindingSet bs = create.get();
		while (hasMore) {
			int nextName = si.deserialize(input, available);
			Value nextValue = vs.deserialize(input, available);
			set.get(nextName).accept(nextValue, bs);
			hasMore = sb.deserialize(input, available);
		}
		return bs;
	}

}
