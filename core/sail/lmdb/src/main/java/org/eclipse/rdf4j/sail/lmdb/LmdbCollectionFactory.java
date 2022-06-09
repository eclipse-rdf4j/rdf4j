/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.eclipse.rdf4j.collection.factory.impl.DefaultCollectionFactory.extractValues;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.eclipse.rdf4j.collection.factory.api.BindingSetKey;
import org.eclipse.rdf4j.collection.factory.api.ValuePair;
import org.eclipse.rdf4j.collection.factory.impl.DefaultBindingSetKey;
import org.eclipse.rdf4j.collection.factory.impl.DefaultValuePair;
import org.eclipse.rdf4j.collection.factory.mapdb.MapDbCollectionFactory;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;
import org.mapdb.Serializer;

/**
 * A CollectionFactory that tries to use LMDB core datastructures.
 */
public class LmdbCollectionFactory extends MapDbCollectionFactory {
	private final ValueStoreRevision rev;

	public LmdbCollectionFactory(ValueStore valueFactory, long iterationCacheSyncThreshold) {
		super(iterationCacheSyncThreshold);
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
	public Set<BindingSet> createSetOfBindingSets(Supplier<MutableBindingSet> supplier,
			Function<String, BiConsumer<Value, MutableBindingSet>> valueSetter) {
		if (iterationCacheSyncThreshold > 0) {
			init();
			BindingSetSerializer bindingSetSerializer = new BindingSetSerializer(rev, supplier, valueSetter);
			MemoryTillSizeXSet<BindingSet> set = new MemoryTillSizeXSet<>(colectionId++, new HashSet<>(),
					bindingSetSerializer);
			return new CommitingSet<BindingSet>(set, iterationCacheSyncThreshold, db);
		} else {
			return new HashSet<>();
		}
	}

	@Override
	public Set<ValuePair> createValuePairSet() {
		if (iterationCacheSyncThreshold > 0) {
			init();
			ValuePairSetSerializer bindingSetSerializer = new ValuePairSetSerializer(rev);
			MemoryTillSizeXSet<ValuePair> set = new MemoryTillSizeXSet<>(colectionId++, new LmdbValuePairSet(rev),
					bindingSetSerializer);
			return new CommitingSet<ValuePair>(set, iterationCacheSyncThreshold, db);
		} else {
			return new LmdbValuePairSet(rev);
		}
	}

	// The idea of this converting set is that we serialize longs into the MapDB not
	// actually falling back to
	// the serialization to SimpleLiterals etc. unless we must
	private static class BindingSetSerializer implements Serializer<BindingSet>, Serializable {
		private static final long serialVersionUID = 1L;
		private final ObjectIntHashMap<String> namesToInt = new ObjectIntHashMap<>();
		@SuppressWarnings("unchecked")
		private transient BiConsumer<Value, MutableBindingSet>[] names = new BiConsumer[1];
		private transient ValueStoreRevision rev;
		private transient Supplier<MutableBindingSet> supplier;
		private transient Function<String, BiConsumer<Value, MutableBindingSet>> setter;

		public BindingSetSerializer(ValueStoreRevision rev, Supplier<MutableBindingSet> supplier,
				Function<String, BiConsumer<Value, MutableBindingSet>> valueSetter) {
			super();
			this.rev = rev;
			this.supplier = supplier;
			this.setter = valueSetter;
		}

		@Override
		public void serialize(DataOutput out, BindingSet values) throws IOException {
			out.writeInt(values.size());
			for (Binding b : values) {
				serializeValueOrLong(out, b.getValue(), rev);
				out.writeInt(indexOf(b.getName()));
			}
		}

		private int indexOf(String name) {
			int ifAbsentPut = namesToInt.getIfAbsentPut(name, namesToInt.size());
			if (ifAbsentPut >= names.length) {
				names = Arrays.copyOf(names, ifAbsentPut + 1);
			}
			names[ifAbsentPut] = setter.apply(name);
			return ifAbsentPut;
		}

		@Override
		public BindingSet deserialize(DataInput in, int available) throws IOException {
			int size = in.readInt();
			MutableBindingSet qbs = supplier.get();
			for (int i = 0; i < size; i++) {
				Value v = deserializeValueOrLong(in, rev);
				names[in.readInt()].accept(v, qbs);
			}
			return qbs;
		}

		@Override
		public int fixedSize() {
			return -1;
		}

	}

	private static Value deserializeValueOrLong(DataInput in, ValueStoreRevision rev) throws IOException {
		boolean wasLong = in.readBoolean();
		if (wasLong) {
			return rev.getValueStore().getLazyValue(in.readLong());
		} else {
			return deserializeValue(in, rev.getValueStore());
		}
	}

	private static void serializeValueOrLong(DataOutput out, Value v, ValueStoreRevision rev) throws IOException {
		Object obj = convertToLongIfPossible(v, rev);
		if (obj instanceof Long) {
			out.writeBoolean(true);
			out.writeLong((Long) obj);
		} else {
			out.writeBoolean(false);
			serializeValue(out, v);
		}
	}

	// The idea of this converting set is that we serialize longs into the MapDB not
	// actually falling back to
	// the serialization to SimpleLiterals etc. unless we must
	private static class ValuePairSetSerializer implements Serializer<ValuePair>, Serializable {
		private static final long serialVersionUID = 1L;
		private transient ValueStoreRevision rev;

		public ValuePairSetSerializer(ValueStoreRevision rev) {
			super();
			this.rev = rev;
		}

		@Override
		public void serialize(DataOutput out, ValuePair vp) throws IOException {
			serializeValueOrLong(out, vp.getStartValue(), rev);
			serializeValueOrLong(out, vp.getEndValue(), rev);
		}

		@Override
		public ValuePair deserialize(DataInput in, int available) throws IOException {

			Value start = deserializeValueOrLong(in, rev);
			Value end = deserializeValueOrLong(in, rev);
			return new DefaultValuePair(start, end);
		}

		@Override
		public int fixedSize() {
			return -1;
		}

	}

	private static void serializeValue(DataOutput out, Value v) throws IOException {
		if (v instanceof IRI) {
			out.writeByte('i');
			out.writeUTF(v.stringValue());
		} else if (v instanceof Literal) {
			out.writeByte('l');
			serializeLiteral(out, v);
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

	private static Value deserializeValue(DataInput in, ValueStore vs) throws IOException {
		byte t = in.readByte();
		if (t == 'i') {
			return vs.createIRI(in.readUTF());
		} else if (t == 'l') {
			return deserializeLiteral(in, vs);
		} else if (t == 'b') {
			return vs.createBNode(in.readUTF());
		} else {

			Value subject = deserializeValue(in, vs);
			Value predicate = deserializeValue(in, vs);
			Value object = deserializeValue(in, vs);
			return vs.createTriple((Resource) subject, (IRI) predicate, object);
		}
	}

	private static void serializeLiteral(DataOutput out, Value v) throws IOException {
		Literal l = (Literal) v;
		out.writeUTF(v.stringValue());
		boolean hasLanguage = l.getLanguage().isPresent();

		if (hasLanguage) {
			out.writeByte(1);
			out.writeUTF(l.getLanguage().get());
		} else {
			CoreDatatype cdt = l.getCoreDatatype();
			if (cdt.isGEODatatype()) {
				out.writeByte(2);
				out.writeByte(cdt.asGEODatatype().get().ordinal());
			} else if (cdt.isRDFDatatype()) {
				out.writeByte(3);
				out.writeByte(cdt.asRDFDatatype().get().ordinal());
			} else if (cdt.isXSDDatatype()) {
				out.writeByte(4);
				out.writeByte(cdt.asXSDDatatype().get().ordinal());
			} else {
				out.writeByte(5);
				out.writeUTF(l.getDatatype().stringValue());
			}
		}
	}

	private static Value deserializeLiteral(DataInput in, ValueStore vs) throws IOException {
		String label = in.readUTF();
		int t = in.readByte();
		if (t == 1) {
			return vs.createLiteral(label, in.readUTF());
		} else if (t == 2) {
			return vs.createLiteral(label, CoreDatatype.GEO.values()[in.readByte()]);
		} else if (t == 3) {
			return vs.createLiteral(label, CoreDatatype.RDF.values()[in.readByte()]);
		} else if (t == 4) {
			return vs.createLiteral(label, CoreDatatype.XSD.values()[in.readByte()]);
		} else {
			return vs.createLiteral(label, vs.createIRI(in.readUTF()));
		}
	}

	private int hash(BindingSet bs, List<Function<BindingSet, Value>> getValues) {
		int hash = 1;
		for (int i = 0; i < getValues.size(); i++) {
			Function<BindingSet, Value> getValue = getValues.get(i);
			Value value = getValue.apply(bs);
			if (value instanceof LmdbValue) {
				LmdbValue lv = (LmdbValue) value;
				if (lv.getValueStoreRevision().equals(rev)) {
					long id = lv.getInternalID();
					if (id == LmdbValue.UNKNOWN_ID) {
						hash = 31 * hash + value.hashCode();
					} else {
						hash = 31 * hash + Long.hashCode(id);
					}
				} else {
					hash = 31 * hash + hashUnkown(value);
				}
			} else if (value != null) {
				hash = 31 * hash + hashUnkown(value);
			}
		}
		return hash;
	}

	private static Object convertToLongIfPossible(Value value, ValueStoreRevision rev) {
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

		List<Value> values = extractValues(bindingSet, getValues);

		long[] ids = new long[values.size()];
		boolean allLong = tryToFillWithValueStoreLong(values, ids);
		if (allLong) {
			return new LmdbValueBindingSetKey(ids, hash);
		} else {
			return new DefaultBindingSetKey(values, hash);
		}
	}

	private boolean tryToFillWithValueStoreLong(List<Value> values, long[] ids) {
		boolean allLong = true;
		for (int i = 0; i < values.size(); i++) {
			Value val = values.get(i);
			Object obj = convertToLongIfPossible(val, rev);
			if (obj instanceof Long) {
				ids[i] = (Long) obj;
			} else {
				return false;
			}
		}
		return allLong;
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
		private final LongHashSet storeKnownValues = new LongHashSet();
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
		public boolean contains(Object o) {
			if (o instanceof LmdbValue) {
				LmdbValue lv = (LmdbValue) o;
				if (lv.getValueStoreRevision() == rev) {
					return storeKnownValues.add(lv.getInternalID());
				}
			} else if (o instanceof Value) {
				Value v = (Value) o;
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
			return false;
		}

		@Override
		public Iterator<Value> iterator() {
			LongIterator knowns = storeKnownValues.longIterator();
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

	@Override
	public Queue<ValuePair> createValuePairQueue() {
		if (iterationCacheSyncThreshold > 0) {
			init();
			return new MemoryTillSizeXQueue(colectionId++, new ArrayDeque<ValuePair>(),
					new ValuePairSetSerializer(rev));
		} else {
			return new ArrayDeque<ValuePair>();
		}
	}

	private static class LmdbValuePairSet extends AbstractSet<ValuePair> {
		private final ValueStoreRevision rev;
		private final LongHashSet storeKnownValues = new LongHashSet();
		private final Set<ValuePair> notKnownToStoreValues = new HashSet<>();

		public LmdbValuePairSet(ValueStoreRevision rev) {
			super();
			this.rev = rev;
		}

		@Override
		public boolean add(ValuePair v) {
			long start = idOf(v.getStartValue());
			long end = idOf(v.getEndValue());
			if (twoValuesCouldFitInOneLong(start, end)) {
				long vp = start << 32 | (end & 0x0000ffff);
				return storeKnownValues.add(vp);
			} else {
				return notKnownToStoreValues.add(v);
			}
		}

		private boolean twoValuesCouldFitInOneLong(long start, long end) {
			return start != LmdbValue.UNKNOWN_ID && end != LmdbValue.UNKNOWN_ID && start < Integer.MAX_VALUE
					&& end < Integer.MAX_VALUE;
		}

		@Override
		public boolean contains(Object o) {
			if (o instanceof ValuePair) {
				ValuePair v = (ValuePair) o;
				long start = idOf(v.getStartValue());
				long end = idOf(v.getEndValue());
				if (twoValuesCouldFitInOneLong(start, end)) {
					long vp = start << 32 | (end & 0x0000ffff);
					return storeKnownValues.contains(vp);
				} else {
					return notKnownToStoreValues.contains(v);
				}
			}
			return false;
		}

		private long idOf(Value o) {
			if (o instanceof LmdbValue) {
				LmdbValue lv = (LmdbValue) o;
				if (lv.getValueStoreRevision() == rev) {
					return lv.getInternalID();
				}
			} else if (o instanceof Value) {
				Value v = (Value) o;
				long id;
				try {
					id = rev.getValueStore().getId(v, false);
				} catch (IOException e) {
					throw new LmdbValueStoreException(e);
				}
				return id;
			}
			return LmdbValue.UNKNOWN_ID;
		}

		@Override
		public Iterator<ValuePair> iterator() {
			LongIterator knowns = storeKnownValues.longIterator();
			Iterator<ValuePair> notKnowns = notKnownToStoreValues.iterator();
			return new Iterator<ValuePair>() {

				@Override
				public boolean hasNext() {

					return knowns.hasNext() || notKnowns.hasNext();
				}

				@Override
				public ValuePair next() {
					if (knowns.hasNext()) {
						try {
							long vp = knowns.next();
							long start = vp >>> 32;
							long end = (long) ((int) vp);

							Value vs = rev.getValueStore().getLazyValue(start);
							Value ve = rev.getValueStore().getLazyValue(end);
							return new DefaultValuePair(vs, ve);
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
}
