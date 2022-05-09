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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
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

	private final class FunctionImplementation implements Function<BindingSet, Integer>, Serializable {
		private final List<Function<BindingSet, Value>> getValues;

		private FunctionImplementation(List<Function<BindingSet, Value>> getValues) {
			this.getValues = getValues;
		}

		@Override
		public Integer apply(BindingSet bs) {
			int nextHash = 0;
			for (Function<BindingSet, Value> getValue : getValues) {
				Value value = getValue.apply(bs);
				if (value instanceof LmdbValue) {
					LmdbValue lv = (LmdbValue) value;
					if (lv.getValueStoreRevision() == rev) {
						nextHash ^= Long.hashCode(lv.getInternalID());
					} else {
						nextHash ^= hashUnkown(value);
					}
				} else if (value != null) {
					nextHash ^= hashUnkown(value);
				}
			}
			return nextHash;
		}
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
	public BindingSetKey createBindingSetKey(BindingSet bindingSet, List<Function<BindingSet, Value>> getValues,
			BiFunction<BindingSet, BindingSet, Boolean> equalsTest) {
		Function<BindingSet, Integer> hashMaker = lmdbAwareHashMaker(getValues);
		return new DefaultBindingSetKey(bindingSet, hashMaker, equalsTest);
	}

	private Function<BindingSet, Integer> lmdbAwareHashMaker(List<Function<BindingSet, Value>> getValues) {

		Function<BindingSet, Integer> hashFunction = new FunctionImplementation(getValues);
		return hashFunction;
	}

	private long hashUnkown(Value value) {
		long id;
		try {
			id = rev.getValueStore().getId(value, false);
		} catch (IOException e) {
			throw new LmdbValueStoreException(e);
		}
		if (id == LmdbValue.UNKNOWN_ID) {
			return value.hashCode();
		} else {
			return Long.hashCode(id);
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
