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
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
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
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
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
	public List<Value> createValueList() {
		return new LmdbValueList(rev);
	}

	@Override
	public Set<BindingSet> createSetOfBindingSets() {
		if (iterationCacheSyncThreshold > 0) {
			init();
			BindingSetSerializer bindingSetSerializer = new BindingSetSerializer(rev);
			MemoryTillSizeXSet<BindingSet> set = new MemoryTillSizeXSet<>(colectionId++, new HashSet<>(),
					bindingSetSerializer);
			return new CommitingSet<BindingSet>(set, iterationCacheSyncThreshold, db);
		} else {
			return new HashSet<>();
		}
	}

	// The idea of this converting set is that we serialize longs into the MapDB not
	// actually falling back to
	// the serialization to SimpleLiterals etc. unless we must
	private static class BindingSetSerializer implements Serializer<BindingSet>, Serializable {
		private static final long serialVersionUID = 1L;
		private final ObjectIntHashMap<String> namesToInt = new ObjectIntHashMap<>();
		private String[] names = new String[1];
		private transient ValueStoreRevision rev;

		public BindingSetSerializer(ValueStoreRevision rev) {
			super();
			this.rev = rev;
		}

		@Override
		public void serialize(DataOutput out, BindingSet values) throws IOException {
			out.writeInt(values.size());
			for (Binding b : values) {
				Value v = b.getValue();
				Object obj = convertToLongIfPossible(v, rev);
				if (obj instanceof Long) {
					out.writeBoolean(true);
					out.writeLong((Long) obj);
				} else {
					out.writeBoolean(false);
					serializeValue(out, v);
				}
				out.writeInt(indexOf(b.getName()));
			}
		}

		private void serializeValue(DataOutput out, Value v) throws IOException {
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

		private Value deserializeValue(DataInput in) throws IOException {
			byte t = in.readByte();
			if (t == 'i') {
				return rev.getValueStore().createIRI(in.readUTF());
			} else if (t == 'l') {
				return deserializeLiteral(in);
			} else if (t == 'b') {
				return rev.getValueStore().createBNode(in.readUTF());
			} else {

				Value subject = deserializeValue(in);
				Value predicate = deserializeValue(in);
				Value object = deserializeValue(in);
				return rev.getValueStore().createTriple((Resource) subject, (IRI) predicate, object);
			}
		}

		private void serializeLiteral(DataOutput out, Value v) throws IOException {
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

		private Value deserializeLiteral(DataInput in) throws IOException {
			String label = in.readUTF();
			int t = in.readByte();
			if (t == 1) {
				return rev.getValueStore().createLiteral(label, in.readUTF());
			} else if (t == 2) {
				return rev.getValueStore().createLiteral(label, CoreDatatype.GEO.values()[in.readByte()]);
			} else if (t == 3) {
				return rev.getValueStore().createLiteral(label, CoreDatatype.RDF.values()[in.readByte()]);
			} else if (t == 4) {
				return rev.getValueStore().createLiteral(label, CoreDatatype.XSD.values()[in.readByte()]);
			} else {
				return rev.getValueStore().createLiteral(label, rev.getValueStore().createIRI(in.readUTF()));
			}
		}

		private int indexOf(String name) {
			int ifAbsentPut = namesToInt.getIfAbsentPut(name, namesToInt.size());
			if (ifAbsentPut >= names.length) {
				names = Arrays.copyOf(names, ifAbsentPut + 1);
			}
			names[ifAbsentPut] = name;
			return ifAbsentPut;
		}

		@Override
		public BindingSet deserialize(DataInput in, int available) throws IOException {
			int size = in.readInt();
			QueryBindingSet qbs = new QueryBindingSet(size);
			for (int i = 0; i < size; i++) {
				boolean wasLong = in.readBoolean();
				Value v;
				if (wasLong) {
					v = rev.getValueStore().getLazyValue(in.readLong());
				} else {
					v = deserializeValue(in);
				}
				qbs.setBinding(names[in.readInt()], v);
			}
			return qbs;
		}

		@Override
		public int fixedSize() {
			return -1;
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
		List<Value> values = new ArrayList<>(getValues.size());
		for (int i = 0; i < getValues.size(); i++) {
			values.add(getValues.get(i).apply(bindingSet));
		}
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

	@Override
	public void close() throws RDF4JException {
		super.close();
	}
}
