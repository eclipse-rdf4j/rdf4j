/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.rdf4j.collection.factory.api.BindingSetKey;
import org.eclipse.rdf4j.collection.factory.impl.DefaultBindingSetKey;
import org.eclipse.rdf4j.collection.factory.mapdb.MapDbCollectionFactory;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;
import org.mapdb.Serializer;

/**
 * A CollectionFactory that tries to use LMDB core datastructures.
 */
public class LmdbCollectionFactory extends MapDbCollectionFactory {
	private final ValueStoreRevision rev;
	private final ValueStore valueFactory;

	public LmdbCollectionFactory(ValueStore valueFactory, long iterationCacheSyncThreshold) {
		super(iterationCacheSyncThreshold);
		this.valueFactory = valueFactory;
		this.rev = valueFactory.getRevision();
	}

	@Override
	public Set<Value> createValueSet() {
		if (iterationCacheSyncThreshold > 0) {
			init();
			MemoryTillSizeXSet<Value> set = new MemoryTillSizeXSet<>(colectionId++, new LmdbValueSet(rev));
			return new CommitingSet<Value>(set, iterationCacheSyncThreshold, db);
		} else {
			return new LmdbValueSet(rev);
		}
	}

	@Override
	public List<Value> createValueList() {
		return new LmdbValueList(rev);
	}

	@Override
	public Set<BindingSet> createSetOfBindingSets() {
		if (iterationCacheSyncThreshold > 0) {
			init();
			MemoryTillSizeXSet<BindingValues> set = new MemoryTillSizeXSet<>(colectionId++, new HashSet<>());
			// The idea of this converting set is that we serialize longs into the MapDB not
			// actually falling back to
			// the
			// serialization to SimpleLiterals etc.
			return new ConvertingSet(
					new CommitingSet<BindingValues>(set, iterationCacheSyncThreshold, db, new BindingSetSerializer()),
					this::bindingValuesToBindingSet, this::bindingSetToBindingValues);
		} else {
			return new HashSet<>();
		}
	}

	private class BindingSetSerializer implements Serializer<BindingValues> {
		private String[] names;

		@Override
		public void serialize(DataOutput out, BindingValues values) throws IOException {

			if (values instanceof BindingLongValues) {
				out.writeBoolean(true);
				serializeBindingLongValues(out, values);
			} else {
				out.writeBoolean(false);
				serializeBindingValues(out, values);
			}
		}

		private void serializeBindingLongValues(DataOutput out, BindingValues values) throws IOException {

			BindingLongValues blv = (BindingLongValues) values;
			out.writeInt(blv.ids.length);
			for (long l : blv.ids) {
				out.writeLong(l);
			}
			if (names == null) {
				this.names = Arrays.copyOf(blv.names, blv.names.length);
				for (int i = 0; i < names.length; i++) {
					out.writeInt(i);
				}
			} else {
				for (int i = 0; i < names.length; i++) {
					out.writeInt(indexOf(blv.names[i]));
				}
			}
		}

		private void serializeBindingValues(DataOutput out, BindingValues values) throws IOException {

			BindingRealValues blv = (BindingRealValues) values;
			out.writeInt(blv.values.length);
			for (Value v : blv.values) {
				serializeValue(out, v);
			}
			if (names == null) {
				this.names = Arrays.copyOf(blv.names, blv.names.length);
				for (int i = 0; i < names.length; i++) {
					out.writeInt(i);
				}
			} else {
				for (int i = 0; i < names.length; i++) {
					out.writeInt(indexOf(blv.names[i]));
				}
			}
		}

		private void serializeValue(DataOutput out, Value v) throws IOException {
			if (v instanceof IRI) {
				out.writeByte('i');
				out.writeUTF(v.stringValue());
			} else if (v instanceof Literal) {
				out.writeByte('l');
				Literal l = (Literal) v;
				out.writeUTF(v.stringValue());
				boolean hasLanguage = l.getLanguage().isPresent();
				out.writeBoolean(hasLanguage);
				if (hasLanguage) {
					out.writeUTF(l.getLanguage().get());
				} else {
					out.writeUTF(l.getDatatype().stringValue());
				}
			} else if (v instanceof BNode) {
				out.writeByte('b');
				out.writeUTF(v.stringValue());
			} else {
				out.writeByte('t');
				Triple t = (Triple) v;
				serializeValue(out, t.getSubject());
				serializeValue(out, t.getPredicate());
				serializeValue(out, t.getObject());
			}
		}

		private Value derializeValue(DataInput in) throws IOException {
			byte t = in.readByte();
			if (t == 'i') {
				return valueFactory.createIRI(in.readUTF());
			} else if (t == 'l') {
				String label = in.readUTF();
				boolean hasLanguage = in.readBoolean();

				if (hasLanguage) {
					return valueFactory.createLiteral(label, in.readUTF());
				} else {
					return valueFactory.createLiteral(label, valueFactory.createIRI(in.readUTF()));
				}
			} else if (t == 'b') {
				return valueFactory.createBNode(in.readUTF());
			} else {

				Value subject = derializeValue(in);
				Value predicate = derializeValue(in);
				Value object = derializeValue(in);
				return valueFactory.createTriple((Resource) subject, (IRI) predicate, object);
			}
		}

		private int indexOf(String name) {
			for (int i = 0; i < names.length; i++) {
				if (name.equals(names[i])) {
					return i;
				}
			}
			names = Arrays.copyOf(names, names.length + 1);
			names[names.length - 1] = name;
			return names.length - 1;
		}

		@Override
		public BindingValues deserialize(DataInput in, int available) throws IOException {
			boolean wasLongValues = in.readBoolean();
			if (wasLongValues) {
				int valueLength = in.readInt();
				long[] values = new long[valueLength];
				for (int i = 0; i < valueLength; i++) {
					values[0] = in.readLong();
				}
				String[] savedNames = new String[valueLength];
				for (int i = 0; i < valueLength; i++) {
					savedNames[i] = names[in.readInt()];
				}
				return new BindingLongValues(savedNames, values);
			} else {
				int valueLength = in.readInt();
				Value[] values = new Value[valueLength];
				for (int i = 0; i < valueLength; i++) {
					values[0] = derializeValue(in);
				}
				String[] savedNames = new String[valueLength];
				for (int i = 0; i < valueLength; i++) {
					savedNames[i] = names[in.readInt()];
				}
				return new BindingRealValues(savedNames, values);
			}
		}

		@Override
		public int fixedSize() {
			return -1;
		}

	}

	private static class ConvertingSet extends AbstractSet<BindingSet> {
		private final Set<BindingValues> wrapped;
		private final Function<BindingValues, BindingSet> xToY;
		private final Function<BindingSet, BindingValues> yToX;

		public ConvertingSet(Set<BindingValues> wrapped, Function<BindingValues, BindingSet> xToY,
				Function<BindingSet, BindingValues> yToX) {
			super();
			this.wrapped = wrapped;
			this.xToY = xToY;
			this.yToX = yToX;
		}

		@Override
		public Iterator<BindingSet> iterator() {
			Iterator<BindingValues> wrapIter = wrapped.iterator();
			return new Iterator<BindingSet>() {

				@Override
				public boolean hasNext() {
					return wrapIter.hasNext();
				}

				@Override
				public BindingSet next() {
					return xToY.apply(wrapIter.next());
				}
			};
		}

		@Override
		public boolean add(BindingSet e) {

			return wrapped.add(yToX.apply(e));
		}

		@Override
		public boolean contains(Object o) {
			if (o instanceof BindingValues) {
				return wrapped.add(yToX.apply((BindingSet) o));
			}
			return false;
		}

		@Override
		public int size() {

			return wrapped.size();
		}

	}

	private interface BindingValues extends Serializable {

	}

	private static class BindingLongValues implements BindingValues {

		private static final long serialVersionUID = 1L;
		String[] names;
		long[] ids;

		public BindingLongValues(String[] names, long[] ids) {
			this.names = names;
			this.ids = ids;
		}

		public BindingSet convertBack(ValueStoreRevision rev) {
			QueryBindingSet queryBindingSet = new QueryBindingSet(ids.length);
			for (int i = 0; i < ids.length; i++) {
				try {
					String name = names[i];
					Value value = rev.getValueStore().getLazyValue(ids[i]);
					queryBindingSet.addBinding(name, value);
				} catch (IOException e) {
					throw new LmdbValueStoreException(e);
				}
			}
			return queryBindingSet;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(ids);
			result = prime * result + Arrays.hashCode(names);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof BindingLongValues))
				return false;
			BindingLongValues other = (BindingLongValues) obj;
			return Arrays.equals(ids, other.ids) && Arrays.equals(names, other.names);
		}
	}

	private static class BindingRealValues implements BindingValues {
		private static final long serialVersionUID = 1L;

		public BindingRealValues(String[] names, Value[] values) {
			this.names = names;
			this.values = values;
		}

		String[] names;
		Value[] values;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(names);
			result = prime * result + Arrays.hashCode(values);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof BindingRealValues))
				return false;
			BindingRealValues other = (BindingRealValues) obj;
			return Arrays.equals(names, other.names) && Arrays.equals(values, other.values);
		}

		public BindingSet convertBack() {
			QueryBindingSet queryBindingSet = new QueryBindingSet(values.length);
			for (int i = 0; i < values.length; i++) {

				String name = names[i];
				Value value = values[i];
				queryBindingSet.addBinding(name, value);

			}
			return queryBindingSet;
		}

	}

	private int hash(BindingSet bs, List<Function<BindingSet, Value>> getValues) {
		int size = getValues.size();
		int[] hashes = new int[size];
		for (int i = 0; i < size; i++) {
			Function<BindingSet, Value> getValue = getValues.get(i);
			Value value = getValue.apply(bs);
			if (value instanceof LmdbValue) {
				LmdbValue lv = (LmdbValue) value;
				if (lv.getValueStoreRevision().equals(rev)) {
					long id = lv.getInternalID();
					if (id == LmdbValue.UNKNOWN_ID) {
						hashes[i] = value.hashCode();
					} else {
						hashes[i] = (int) id;
					}
				} else {
					hashes[i] = hashUnkown(value);
				}
			} else if (value != null) {
				hashes[i] = hashUnkown(value);
			}
		}
		return Arrays.hashCode(hashes);
	}

	private Object convertToLongIfPossible(Value value) {
		if (value instanceof LmdbValue) {
			LmdbValue lv = (LmdbValue) value;
			if (lv.getValueStoreRevision().equals(rev)) {
				long id = lv.getInternalID();
				if (id == LmdbValue.UNKNOWN_ID) {
					return value;
				} else {
					return Long.valueOf(id);
				}
			}
		}
		return value;
	}

	private static class LmdbValueStoreException extends RDF4JException {

		public LmdbValueStoreException(IOException e) {
			super(e);
		}

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

	}

	@Override
	public BindingSetKey createBindingSetKey(BindingSet bindingSet, List<Function<BindingSet, Value>> getValues) {
		int hash = hash(bindingSet, getValues);
		List<Value> values = new ArrayList<>(getValues.size());
		for (int i = 0; i < getValues.size(); i++) {
			values.add(getValues.get(i).apply(bindingSet));
		}
		boolean allLong = true;
		long[] ids = new long[values.size()];
		for (int i = 0; i < values.size(); i++) {
			Value val = values.get(i);
			Object obj = convertToLongIfPossible(val);
			if (!(obj instanceof Long)) {
				allLong = false;
				break;
			} else {
				ids[i] = (Long) obj;
			}
		}
		if (allLong) {
			return new LmdbValueBindingSetKey(ids, hash);
		} else {
			return new DefaultBindingSetKey(values, hash);
		}
	}

	private static class LmdbValueBindingSetKey implements BindingSetKey, Serializable {

		private static final long serialVersionUID = 1;

		private final long[] values;

		private final int hash;

		public LmdbValueBindingSetKey(long[] values, int hash) {
			this.values = values;
			this.hash = hash;
		}

		@Override
		public int hashCode() {
			return hash;
		}

		@Override
		public boolean equals(Object other) {
			if (other instanceof LmdbValueBindingSetKey && other.hashCode() == hash) {
				return Arrays.equals(values, ((LmdbValueBindingSetKey) other).values);
			}
			return false;
		}
	}

	private int hashUnkown(Value value) {
		long id;
		try {
			id = rev.getValueStore().getId(value, false);
		} catch (IOException e) {
			throw new LmdbValueStoreException(e);
		}
		if (id == LmdbValue.UNKNOWN_ID) {
			return value.hashCode();
		} else {
			return (int) id;
		}
	}

	private static class LmdbValueSet extends AbstractSet<Value> {
		private final ValueStoreRevision rev;
		private final Set<Long> storeKnownValues = new HashSet<>();
		private final Set<Value> notKnownToStoreValues = new HashSet<>();

		public LmdbValueSet(ValueStoreRevision rev) {
			super();
			this.rev = rev;
		}

		@Override
		public boolean add(Value v) {
			if (v instanceof LmdbValue) {
				LmdbValue lv = (LmdbValue) v;
				if (lv.getValueStoreRevision() == rev) {
					return storeKnownValues.add(lv.getInternalID());
				}
			}

			long id;
			try {
				id = rev.getValueStore().getId(v, false);
			} catch (IOException e) {
				throw new LmdbValueStoreException(e);
			}
			if (id == LmdbValue.UNKNOWN_ID) {
				return notKnownToStoreValues.add(v);
			} else {
				return storeKnownValues.add(id);
			}
		}

		@Override
		public Iterator<Value> iterator() {
			Iterator<Long> knowns = storeKnownValues.iterator();
			Iterator<Value> notKnowns = notKnownToStoreValues.iterator();
			return new Iterator<Value>() {

				@Override
				public boolean hasNext() {

					return knowns.hasNext() || notKnowns.hasNext();
				}

				@Override
				public Value next() {
					if (knowns.hasNext()) {
						try {
							return rev.getValueStore().getLazyValue(knowns.next());
						} catch (IOException e) {
							throw new LmdbValueStoreException(e);
						}
					} else {
						return notKnowns.next();
					}
				}

			};
		}

		@Override
		public int size() {
			return notKnownToStoreValues.size() + storeKnownValues.size();
		}
	}

	private static class LmdbValueList extends AbstractList<Value> {
		private final ValueStoreRevision rev;
		private final List<Object> values = new ArrayList<>();

		public LmdbValueList(ValueStoreRevision rev) {
			super();
			this.rev = rev;
		}

		@Override
		public boolean add(Value v) {
			if (v instanceof LmdbValue) {
				LmdbValue lv = (LmdbValue) v;
				if (lv.getValueStoreRevision() == rev) {
					return values.add(lv.getInternalID());
				}
			}

			long id;
			try {
				id = rev.getValueStore().getId(v, false);
			} catch (IOException e) {
				throw new LmdbValueStoreException(e);
			}
			if (id == LmdbValue.UNKNOWN_ID) {
				return values.add(v);
			} else {
				return values.add(id);
			}
		}

		@Override
		public int size() {
			return values.size();
		}

		@Override
		public Value get(int arg0) {
			Object o = values.get(arg0);
			if (o instanceof Value) {
				return (Value) o;
			} else if (o instanceof Value) {
				try {
					return rev.getValueStore().getLazyValue((Long) o);
				} catch (IOException e) {
					throw new LmdbValueStoreException(e);
				}
			}
			return null;
		}
	}

	private BindingSet bindingValuesToBindingSet(BindingValues bv) {
		if (bv instanceof BindingLongValues) {
			BindingLongValues blv = (BindingLongValues) bv;
			return blv.convertBack(rev);
		} else if (bv instanceof BindingRealValues) {
			BindingRealValues bvr = (BindingRealValues) bv;
			return bvr.convertBack();
		} else {
			return null;
		}
	}

	private BindingValues bindingSetToBindingValues(BindingSet bs) {
		String[] names = bs.getBindingNames().toArray(new String[0]);
		Value[] values = new Value[names.length];
		for (int i = 0; i < names.length; i++) {
			String name = names[i];
			values[i] = bs.getValue(name);
		}
		boolean allLong = true;
		long[] ids = new long[values.length];
		for (int i = 0; i < values.length; i++) {
			Value val = values[i];
			Object obj = convertToLongIfPossible(val);
			if (!(obj instanceof Long)) {
				allLong = false;
				break;
			} else {
				ids[i] = (Long) obj;
			}
		}
		if (allLong) {
			return new BindingLongValues(names, ids);
		} else {
			return new BindingRealValues(names, values);
		}

	}

	@Override
	public void close() throws RDF4JException {
		super.close();
	}
}
