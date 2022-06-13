/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.collection.factory.mapdb;

import java.io.IOError;
import java.util.AbstractMap;
import java.util.AbstractQueue;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.rdf4j.collection.factory.api.BindingSetKey;
import org.eclipse.rdf4j.collection.factory.api.CollectionFactory;
import org.eclipse.rdf4j.collection.factory.api.ValuePair;
import org.eclipse.rdf4j.collection.factory.impl.DefaultCollectionFactory;
import org.eclipse.rdf4j.collection.factory.impl.DefaultValuePair;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

public class MapDbCollectionFactory implements CollectionFactory {
	protected volatile DB db;
	protected volatile long colectionId = 0;
	protected final long iterationCacheSyncThreshold;
	private final CollectionFactory delegate;
//	private File tempFile;

	private static final class RDF4jMapDBException extends RDF4JException {

		private static final long serialVersionUID = 1L;

		public RDF4jMapDBException(String string, Throwable e) {
			super(string, e);
		}

	}

	public MapDbCollectionFactory(long iterationCacheSyncThreshold) {
		this(iterationCacheSyncThreshold, new DefaultCollectionFactory());
	}

	public MapDbCollectionFactory(long iterationCacheSyncThreshold, CollectionFactory delegate) {

		this.iterationCacheSyncThreshold = iterationCacheSyncThreshold;
		this.delegate = delegate;
	}

	protected void init() {
		if (this.db == null) {
			synchronized (this) {
				if (this.db == null) {
					try {
						this.db = DBMaker.newTempFileDB()
								.deleteFilesAfterClose()
								.closeOnJvmShutdown()
								.commitFileSyncDisable()
								.make();
					} catch (IOError e) {
						throw new RDF4jMapDBException("could not initialize temp db", e);
					}
				}
			}
		}
	}

	@Override
	public Set<BindingSet> createSetOfBindingSets(Supplier<MutableBindingSet> supplier,
			Function<String, BiConsumer<Value, MutableBindingSet>> valueSetter) {
		if (iterationCacheSyncThreshold > 0) {
			init();
			MemoryTillSizeXSet<BindingSet> set = new MemoryTillSizeXSet<>(colectionId++,
					delegate.createSetOfBindingSets(supplier, valueSetter));
			return new CommitingSet<>(set, iterationCacheSyncThreshold, db);
		} else {
			return delegate.createSetOfBindingSets(supplier, valueSetter);
		}
	}

	@Override
	public Set<Value> createValueSet() {
		if (iterationCacheSyncThreshold > 0) {
			init();
			Set<Value> set = new MemoryTillSizeXSet<>(colectionId++, delegate.createValueSet());
			return new CommitingSet<Value>(set, iterationCacheSyncThreshold, db);
		} else {
			return delegate.createValueSet();
		}
	}

	@Override
	public <V> Map<Value, V> createValueKeyedMap() {
		if (iterationCacheSyncThreshold > 0) {
			init();
			return new CommitingMap<>(db.createHashMap(Long.toHexString(colectionId++)).make(),
					iterationCacheSyncThreshold, db);
		} else {
			return delegate.createValueKeyedMap();
		}
	}

	@Override
	public void close() throws RDF4JException {
		if (db != null && !db.isClosed()) {
			db.close();
		}
	}

	@Override
	public <E> Map<BindingSetKey, E> createGroupByMap() {
		if (iterationCacheSyncThreshold > 0) {
			init();
			return new CommitingMap<>(db.createHashMap(Long.toHexString(colectionId++)).make(),
					iterationCacheSyncThreshold, db);
		} else {
			return delegate.createGroupByMap();
		}
	}

	@Override
	public BindingSetKey createBindingSetKey(BindingSet bindingSet, List<Function<BindingSet, Value>> getValues) {
		return delegate.createBindingSetKey(bindingSet, getValues);
	}

	protected static final class CommitingSet<T> extends AbstractSet<T> {
		private final Set<T> wrapped;
		private final long iterationCacheSyncThreshold;
		private final DB db;
		private long iterationCount;

		public CommitingSet(Set<T> wrapped, long iterationCacheSyncThreshold, DB db) {
			super();
			this.wrapped = wrapped;
			this.iterationCacheSyncThreshold = iterationCacheSyncThreshold;
			this.db = db;
		}

		@Override
		public boolean add(T e) {

			boolean res = wrapped.add(e);
			if (iterationCount++ % iterationCacheSyncThreshold == 0) {
				// write to disk every $iterationCacheSyncThreshold items
				db.commit();
			}
			return res;

		}

		@Override
		public boolean addAll(Collection<? extends T> c) {
			int preinsertSize = wrapped.size();
			boolean res = wrapped.addAll(c);
			int inserted = preinsertSize - c.size();
			if (inserted + iterationCount > iterationCacheSyncThreshold) {
				// write to disk every $iterationCacheSyncThreshold items
				db.commit();
				iterationCount = 0;
			} else {
				iterationCount += inserted;
			}
			return res;
		}

		@Override
		public Iterator<T> iterator() {
			return wrapped.iterator();
		}

		@Override
		public int size() {
			return wrapped.size();
		}
	}

	protected static final class CommitingMap<K, V> extends AbstractMap<K, V> {
		private final Map<K, V> wrapped;
		private final long iterationCacheSyncThreshold;
		private final DB db;
		private long iterationCount;

		public CommitingMap(Map<K, V> wrapped, long iterationCacheSyncThreshold, DB db) {
			super();
			this.wrapped = wrapped;
			this.iterationCacheSyncThreshold = iterationCacheSyncThreshold;
			this.db = db;
		}

		@Override
		public V put(K k, V v) {

			V res = wrapped.put(k, v);
			if (iterationCount++ % iterationCacheSyncThreshold == 0) {
				// write to disk every $iterationCacheSyncThreshold items
				db.commit();
			}
			return res;

		}

		@Override
		public int size() {
			return wrapped.size();
		}

		@Override
		public Set<Entry<K, V>> entrySet() {
			return wrapped.entrySet();
		}
	}

	/**
	 * Only create a disk based set once the contents are large enough that it starts to pay off.
	 *
	 * @param <T> of the contents of the set.
	 */
	public class MemoryTillSizeXSet<V> extends AbstractSet<V> {
		private Set<V> wrapped;
		private final long setName;
		private final Serializer<V> serializer;
		private boolean disk;

		@SuppressWarnings("unchecked")
		public MemoryTillSizeXSet(long setName, Set<V> wrapped) {
			this(setName, wrapped, db.getDefaultSerializer());
		}

		public MemoryTillSizeXSet(long setName, Set<V> wrapped, Serializer<V> serializer) {
			super();
			this.setName = setName;
			this.wrapped = wrapped;
			this.serializer = serializer;
		}

		@Override
		public boolean add(V e) {
			if (wrapped instanceof HashSet && wrapped.size() > iterationCacheSyncThreshold && !disk) {
				Set<V> toReplace = makeDiskBasedSet();
				toReplace.addAll(wrapped);
				wrapped = toReplace;
				disk = true;
			}
			return wrapped.add(e);
		}

		@Override
		public boolean addAll(Collection<? extends V> arg0) {
			if (wrapped instanceof HashSet && arg0.size() > iterationCacheSyncThreshold && !disk) {
				Set<V> toReplace = makeDiskBasedSet();
				toReplace.addAll(wrapped);
				wrapped = toReplace;
				disk = true;
			}
			return wrapped.addAll(arg0);
		}

		private Set<V> makeDiskBasedSet() {
			return db.createHashSet(Long.toHexString(setName)).serializer(serializer).make();
		}

		@Override
		public void clear() {
			wrapped.clear();
		}

		@Override
		public boolean contains(Object o) {
			return wrapped.contains(o);
		}

		@Override
		public boolean containsAll(Collection<?> arg0) {
			return wrapped.containsAll(arg0);
		}

		@Override
		public boolean isEmpty() {
			return wrapped.isEmpty();
		}

		@Override
		public boolean remove(Object o) {
			return wrapped.remove(o);
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			return wrapped.retainAll(c);
		}

		@Override
		public Object[] toArray() {
			return wrapped.toArray();
		}

		@Override
		public <T> T[] toArray(T[] arg0) {
			return wrapped.toArray(arg0);
		}

		@Override
		public Iterator<V> iterator() {
			return wrapped.iterator();
		}

		@Override
		public int size() {
			return wrapped.size();
		}

	}

	@Override
	public ValuePair createValuePair(Value start, Value end) {
		return new DefaultValuePair(start, end);
	}

	@Override
	public Set<ValuePair> createValuePairSet() {
		if (iterationCacheSyncThreshold > 0) {
			init();
			Set<ValuePair> set = new MemoryTillSizeXSet<>(colectionId++, delegate.createValuePairSet());
			return new CommitingSet<ValuePair>(set, iterationCacheSyncThreshold, db);
		} else {
			return delegate.createValuePairSet();
		}
	}

	@Override
	public Queue<ValuePair> createValuePairQueue() {
		if (iterationCacheSyncThreshold > 0) {
			init();
			return new MemoryTillSizeXQueue(colectionId++, delegate.createValuePairQueue());
		} else {
			return delegate.createValuePairQueue();
		}
	}

	public class MemoryTillSizeXQueue extends AbstractQueue<ValuePair> {
		private Queue<ValuePair> wrapped;
		private final long setName;
		private boolean disk = false;
		private Serializer<ValuePair> serializer;

		@SuppressWarnings("unchecked")
		public MemoryTillSizeXQueue(long setName, Queue<ValuePair> wrapped) {
			this(setName, wrapped, db.getDefaultSerializer());
		}

		public MemoryTillSizeXQueue(long setName, Queue<ValuePair> wrapped, Serializer<ValuePair> serializer) {
			super();
			this.setName = setName;
			this.wrapped = wrapped;
			this.serializer = serializer;
		}

		@Override
		public boolean offer(ValuePair e) {
			boolean offer = wrapped.offer(e);
			if (offer && wrapped.size() > iterationCacheSyncThreshold && !disk) {
				disk = true;
				Queue<ValuePair> toReplace = db.createQueue(Long.toHexString(setName), serializer, false);
				toReplace.addAll(wrapped);
				wrapped = toReplace;
			}
			return offer;
		}

		@Override
		public ValuePair poll() {
			return wrapped.poll();
		}

		@Override
		public ValuePair peek() {
			return wrapped.peek();
		}

		@Override
		public Iterator<ValuePair> iterator() {
			return wrapped.iterator();
		}

		@Override
		public int size() {
			return wrapped.size();
		}

	}
}
