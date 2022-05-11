/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import java.io.IOException;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.rdf4j.collection.factory.api.BindingSetKey;
import org.eclipse.rdf4j.collection.factory.impl.DefaultBindingSetKey;
import org.eclipse.rdf4j.collection.factory.impl.DefaultCollectionFactory;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;

/**
 * A CollectionFactory that tries to use LMDB core datastructures.
 */
public class LmdbCollectionFactory extends DefaultCollectionFactory {
	private final ValueStoreRevision rev;

	public LmdbCollectionFactory(ValueStore valueFactory) {
		super();
		this.rev = valueFactory.getRevision();
	}

	@Override
	public Set<Value> createValueSet() {
		return new LmdbValueSet(rev);
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
//		List<Object> objects = new ArrayList<>(getValues.size());
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
}
