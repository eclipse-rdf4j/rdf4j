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
package org.eclipse.rdf4j.collection.factory.mapdb;

import java.util.AbstractMap;
import java.util.AbstractQueue;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

import org.eclipse.rdf4j.collection.factory.api.BindingSetKey;
import org.eclipse.rdf4j.collection.factory.api.CollectionFactory;
import org.eclipse.rdf4j.collection.factory.impl.DefaultCollectionFactory;
import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.MutableBindingSet;
import org.mapdb.DB;
import org.mapdb.DB.HashMapMaker;
import org.mapdb.DBException;
import org.mapdb.DBMaker;
import org.mapdb.DBMaker.Maker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.mapdb.serializer.SerializerJava;
import org.mapdb.serializer.SerializerLong;

public class MapDb3CollectionFactory implements CollectionFactory {
	// The size 16 seems like a nice starting value but others could well
	// be better.
	private static final int DEFAULT_SWITCH_TO_DISK_BASED_SET_AT_SIZE = 16;
	protected volatile DB db;
	protected volatile long colectionId = 0;
	protected final long iterationCacheSyncThreshold;
	private final CollectionFactory delegate;
	// The chances that someone would run a 32bit non "sun" vm are just miniscule
	// So I am not going to worry about this.
	private static final boolean ON_32_BIT_VM = "32".equals(System.getProperty("sun.arch.data.model"));

	private final class MapDb3BackedQueue<T> extends AbstractQueue<T> {
		private final Map<Long, T> m;
		private long tail;
		private long head;

		private MapDb3BackedQueue(Map<Long, T> m) {
			this.m = m;
		}

		@Override
		public boolean offer(T arg0) {
			m.put(tail++, arg0);
			if (tail % iterationCacheSyncThreshold == 0) {
				db.commit();
			}
			return true;
		}

		@Override
		public T peek() {
			return m.get(head);
		}

		@Override
		public T poll() {
			T r = m.remove(head++);
			if (head % iterationCacheSyncThreshold == 0) {
				db.commit();
			}
			return r;
		}

		@Override
		public Iterator<T> iterator() {
			return new Iterator<>() {
				long at = head;

				@Override
				public boolean hasNext() {
					return at < tail;
				}

				@Override
				public T next() {
					if (at >= tail) {
						throw new NoSuchElementException();
					}
					return m.get(at++);
				}

			};
		}

		@Override
		public int size() {
			return (int) (tail - head);
		}
	}

	protected static final class RDF4jMapDB3Exception extends RDF4JException {

		private static final long serialVersionUID = 1L;

		public RDF4jMapDB3Exception(String string, Exception e) {
			super(string, e);
		}

	}

	public MapDb3CollectionFactory(long iterationCacheSyncThreshold) {
		this(iterationCacheSyncThreshold, new DefaultCollectionFactory());
	}

	public MapDb3CollectionFactory(long iterationCacheSyncThreshold, CollectionFactory delegate) {

		this.iterationCacheSyncThreshold = iterationCacheSyncThreshold;
		this.delegate = delegate;
	}

	protected void init() {
		if (this.db == null) {
			synchronized (this) {
				if (this.db == null) {
					try {
						final Maker dbmaker = DBMaker.tempFileDB().closeOnJvmShutdown();
						// On 32 bit machines this may fail to often so guard it.
						if (!ON_32_BIT_VM) {
							// mmap is much faster than random access file.
							dbmaker.fileMmapEnable();
						}
						this.db = dbmaker.make();
					} catch (DBException e) {
						throw new RDF4jMapDB3Exception("could not initialize temp db", e);
					}
				}
			}
		}
	}

	@Override
	public <T> List<T> createList() {
		return delegate.createList();
	}

	@Override
	public List<Value> createValueList() {
		return delegate.createValueList();
	}

	@Override
	public Set<BindingSet> createSetOfBindingSets(Supplier<MutableBindingSet> create,
			Function<String, Predicate<BindingSet>> getHas, Function<String, Function<BindingSet, Value>> getget,
			Function<String, BiConsumer<Value, MutableBindingSet>> getSet) {

		if (iterationCacheSyncThreshold > 0) {
			return new SyncThresholdAwareSet<>(delegate.createSet(), iterationCacheSyncThreshold, previousSet -> {
				init();
				Serializer<BindingSet> serializer = createBindingSetSerializer(create, getHas, getget, getSet);
				MemoryTillSizeXSet<BindingSet> set = new MemoryTillSizeXSet<>(colectionId++,
						delegate.createSetOfBindingSets(), serializer, DEFAULT_SWITCH_TO_DISK_BASED_SET_AT_SIZE);
				CommitingSet<BindingSet> bindingSets = new CommitingSet<>(set, iterationCacheSyncThreshold, db);
				bindingSets.addAll(previousSet);
				return bindingSets;
			});

		} else {
			return delegate.createSetOfBindingSets();
		}
	}

	@Override
	public <T> Set<T> createSet() {
		if (iterationCacheSyncThreshold > 0) {
			return new SyncThresholdAwareSet<>(delegate.createSet(), iterationCacheSyncThreshold, previousSet -> {
				init();
				Serializer<T> serializer = createAnySerializer();
				MemoryTillSizeXSet<T> set = new MemoryTillSizeXSet<>(colectionId++, delegate.createSet(), serializer,
						DEFAULT_SWITCH_TO_DISK_BASED_SET_AT_SIZE);
				CommitingSet<T> ts = new CommitingSet<>(set, iterationCacheSyncThreshold, db);
				ts.addAll(previousSet);
				return ts;
			});
		} else {
			return delegate.createSet();
		}
	}

	@Override
	public Set<Value> createValueSet() {
		if (iterationCacheSyncThreshold > 0) {
			return new SyncThresholdAwareSet<>(delegate.createValueSet(), iterationCacheSyncThreshold, previousSet -> {
				init();
				Serializer<Value> serializer = createValueSerializer();
				Set<Value> set = new MemoryTillSizeXSet<>(colectionId++, delegate.createValueSet(), serializer,
						DEFAULT_SWITCH_TO_DISK_BASED_SET_AT_SIZE);
				CommitingSet<Value> values = new CommitingSet<>(set, iterationCacheSyncThreshold, db);
				values.addAll(previousSet);
				return values;
			});
		} else {
			return delegate.createValueSet();
		}
	}

	@Override
	public <K, V> Map<K, V> createMap() {
		if (iterationCacheSyncThreshold > 0) {
			return new SyncThresholdAwareMap<>(delegate.createMap(), iterationCacheSyncThreshold, previousMap -> {
				Serializer<K> keySerializer = createAnySerializer();
				Serializer<V> valueSerializer = createAnySerializer();
				HashMapMaker<K, V> hashMap = db.hashMap(Long.toHexString(colectionId++), keySerializer,
						valueSerializer);
				HTreeMap<K, V> create = hashMap.create();
				CommitingMap<K, V> map = new CommitingMap<>(create, iterationCacheSyncThreshold, db);
				map.putAll(previousMap);
				return map;
			});
		} else {
			return delegate.createMap();
		}
	}

	@Override
	public <V> Map<Value, V> createValueKeyedMap() {
		if (iterationCacheSyncThreshold > 0) {
			return new SyncThresholdAwareMap<>(delegate.createValueKeyedMap(), iterationCacheSyncThreshold,
					previousMap -> {
						init();
						Serializer<Value> keySerializer = createValueSerializer();
						Serializer<V> valueSerializer = createAnySerializer();
						CommitingMap<Value, V> map = new CommitingMap<>(
								db.hashMap(Long.toHexString(colectionId++), keySerializer, valueSerializer).create(),
								iterationCacheSyncThreshold, db);
						map.putAll(previousMap);
						return map;
					});

		} else {
			return delegate.createValueKeyedMap();
		}
	}

	@Override
	public <T> Queue<T> createQueue() {
		if (iterationCacheSyncThreshold > 0) {
			return new SyncThresholdAwareQueue<>(delegate.createQueue(), iterationCacheSyncThreshold, prev -> {
				init();
				Serializer<T> s = createAnySerializer();
				Map<Long, T> m = db.hashMap(Long.toHexString(colectionId++), new SerializerLong(), s).create();

				Queue<T> ts = new MemoryTillSizeXQueue<>(delegate.createQueue(), 128, () -> new MapDb3BackedQueue<>(m));
				ts.addAll(prev);
				return ts;
			});
		} else {
			return delegate.createQueue();
		}
	}

	@Override
	public Queue<Value> createValueQueue() {
		if (iterationCacheSyncThreshold > 0) {
			return new SyncThresholdAwareQueue<>(delegate.createValueQueue(), iterationCacheSyncThreshold, prev -> {
				init();
				Serializer<Value> s = createValueSerializer();
				Map<Long, Value> m = db.hashMap(Long.toHexString(colectionId++), new SerializerLong(), s).create();
				MemoryTillSizeXQueue<Value> values = new MemoryTillSizeXQueue<>(delegate.createQueue(), 128,
						() -> new MapDb3BackedQueue<>(m));
				values.addAll(prev);
				return values;
			});

		} else {
			return delegate.createValueQueue();
		}
	}

	@Override
	@Experimental
	public Queue<BindingSet> createBindingSetQueue(Supplier<MutableBindingSet> create,
			Function<String, Predicate<BindingSet>> getHas, Function<String, Function<BindingSet, Value>> getget,
			Function<String, BiConsumer<Value, MutableBindingSet>> getSet) {
		if (iterationCacheSyncThreshold > 0) {
			return new SyncThresholdAwareQueue<>(delegate.createBindingSetQueue(), iterationCacheSyncThreshold,
					prev -> {
						init();
						Serializer<BindingSet> s = createBindingSetSerializer(create, getHas, getget, getSet);
						Map<Long, BindingSet> m = db.hashMap(Long.toHexString(colectionId++), new SerializerLong(), s)
								.create();
						MemoryTillSizeXQueue<BindingSet> bindingSets = new MemoryTillSizeXQueue<>(
								delegate.createBindingSetQueue(create, getHas, getget, getSet), 128,
								() -> new MapDb3BackedQueue<>(m));
						bindingSets.addAll(prev);
						return bindingSets;
					});
		} else {
			return delegate.createBindingSetQueue();
		}
	}

	@Override
	public void close() throws RDF4JException {
		if (db != null) {
			db.close();
		}
	}

	@Override
	public <E> Map<BindingSetKey, E> createGroupByMap() {
		if (iterationCacheSyncThreshold > 0) {
			return new SyncThresholdAwareMap<>(delegate.createGroupByMap(), iterationCacheSyncThreshold,
					previousMap -> {
						init();
						Serializer<BindingSetKey> keySerializer = createBindingSetKeySerializer();
						Serializer<E> valueSerializer = createAnySerializer();
						CommitingMap<BindingSetKey, E> map = new CommitingMap<>(
								db.hashMap(Long.toHexString(colectionId++), keySerializer, valueSerializer).create(),
								iterationCacheSyncThreshold, db);
						map.putAll(previousMap);
						return map;
					});

		} else {
			return delegate.createGroupByMap();
		}
	}

	@Override
	public final BindingSetKey createBindingSetKey(BindingSet bindingSet, List<Function<BindingSet, Value>> getValues,
			ToIntFunction<BindingSet> hashOfBindingSetCalculator) {
		List<Value> values = new ArrayList<>(getValues.size());
		for (int i = 0; i < getValues.size(); i++) {
			values.add(getValues.get(i).apply(bindingSet));
		}
		return new MapDb3BindingSetKey(values, hashOfBindingSetCalculator.applyAsInt(bindingSet));
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
			boolean res = wrapped.addAll(c);
			if (iterationCount + c.size() % iterationCacheSyncThreshold == 0) {
				// write to disk every $iterationCacheSyncThreshold items
				db.commit();
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
	 * @param <V> of the contents of the set.
	 */
	protected class MemoryTillSizeXSet<V> extends AbstractSet<V> {
		private Set<V> wrapped;
		private final long setName;
		private final Serializer<V> valueSerializer;
		private final long switchToDiskAtSize;

		public MemoryTillSizeXSet(long setName, Set<V> wrapped, Serializer<V> valueSerializer, long switchToSize) {
			super();
			this.setName = setName;
			this.wrapped = wrapped;
			this.valueSerializer = valueSerializer;
			this.switchToDiskAtSize = switchToSize;
		}

		@Override
		public boolean add(V e) {
			if (wrapped instanceof HashSet && wrapped.size() > switchToDiskAtSize) {
				Set<V> disk = db.hashSet(Long.toHexString(setName), valueSerializer).create();
				disk.addAll(wrapped);
				wrapped = disk;
			}
			return wrapped.add(e);
		}

		@Override
		public boolean addAll(Collection<? extends V> arg0) {
			if (wrapped instanceof HashSet && arg0.size() > switchToDiskAtSize) {
				Set<V> disk = db.hashSet(Long.toHexString(setName), valueSerializer).create();
				disk.addAll(wrapped);
				wrapped = disk;
			}
			return wrapped.addAll(arg0);
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

	/**
	 * Only create a disk based set once the contents are large enough that it starts to pay off.
	 *
	 * @param <V> of the contents of the set.
	 */
	protected class MemoryTillSizeXQueue<V> extends AbstractQueue<V> {
		private Queue<V> wrapped;
		private final long switchToDiskAtSize;
		private final Supplier<Queue<V>> supplier;

		public MemoryTillSizeXQueue(Queue<V> wrapped, long switchToSize, Supplier<Queue<V>> supplier) {
			super();
			this.wrapped = wrapped;
			this.switchToDiskAtSize = switchToSize;
			this.supplier = supplier;
		}

		@Override
		public int size() {
			return wrapped.size();
		}

		@Override
		public boolean offer(V e) {
			if (!(wrapped instanceof MapDb3BackedQueue) && wrapped.size() > switchToDiskAtSize) {
				Queue<V> disk = supplier.get();
				disk.addAll(wrapped);
				wrapped = disk;
			}
			return wrapped.offer(e);
		}

		@Override
		public V peek() {
			return wrapped.peek();
		}

		@Override
		public V poll() {
			return wrapped.poll();
		}

		@Override
		public Iterator<V> iterator() {
			return wrapped.iterator();
		}

	}

	/**
	 * These methods should be overriding in case a store can deliver a better serialization protocol.
	 *
	 * @param getGet
	 * @param getHas
	 * @param create
	 */
	protected Serializer<BindingSet> createBindingSetSerializer(Supplier<MutableBindingSet> create,
			Function<String, Predicate<BindingSet>> getHas, Function<String, Function<BindingSet, Value>> getGet,
			Function<String, BiConsumer<Value, MutableBindingSet>> getSet) {
		return new BindingSetSerializer(createValueSerializer(), create, getHas, getGet, getSet);
	}

	protected <T> Serializer<T> createAnySerializer() {
		return new SerializerJava();
	}

	protected Serializer<Value> createValueSerializer() {
		return new ValueSerializer();
	}

	protected final Serializer<BindingSetKey> createBindingSetKeySerializer() {
		return new BindingSetKeySerializer(createValueSerializer());
	}

	private static class SyncThresholdAwareQueue<E> implements Queue<E> {

		private int estimatedSize = 0;
		private final long threshold;
		private final Function<Queue<E>, Queue<E>> createSyncingQueue;
		private Queue<E> wrapped;
		private boolean switched = false;

		public SyncThresholdAwareQueue(Queue<E> wrapped, long threshold,
				Function<Queue<E>, Queue<E>> createSyncingQueue) {
			this.wrapped = wrapped;
			this.threshold = threshold;
			this.createSyncingQueue = createSyncingQueue;
		}

		private void checkAndSwitch() {
			if (!switched && estimatedSize > threshold && wrapped.size() > threshold) {
				wrapped = createSyncingQueue.apply(wrapped);
				switched = true;
			}
		}

		@Override
		public boolean add(E e) {
			boolean add = wrapped.add(e);
			if (add) {
				estimatedSize++;
				checkAndSwitch();
			}
			return add;
		}

		@Override
		public boolean offer(E e) {
			boolean offer = wrapped.offer(e);
			if (offer) {
				estimatedSize++;
				checkAndSwitch();
			}
			return offer;
		}

		@Override
		public E remove() {
			estimatedSize--;
			return wrapped.remove();
		}

		@Override
		public E poll() {
			estimatedSize--;
			return wrapped.poll();
		}

		@Override
		public E element() {
			return wrapped.element();
		}

		@Override
		public E peek() {
			return wrapped.peek();
		}

		@Override
		public int size() {
			return wrapped.size();
		}

		@Override
		public boolean isEmpty() {
			return wrapped.isEmpty();
		}

		@Override
		public boolean contains(Object o) {
			return wrapped.contains(o);
		}

		@Override
		public Iterator<E> iterator() {
			return wrapped.iterator();
		}

		@Override
		public Object[] toArray() {
			return wrapped.toArray();
		}

		@Override
		public <T> T[] toArray(T[] a) {
			return wrapped.toArray(a);
		}

		@Override
		public <T> T[] toArray(IntFunction<T[]> generator) {
			return wrapped.toArray(generator);
		}

		@Override
		public boolean remove(Object o) {
			boolean remove = wrapped.remove(o);
			if (remove) {
				estimatedSize--;
			}
			return remove;
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			return wrapped.containsAll(c);
		}

		@Override
		public boolean addAll(Collection<? extends E> c) {
			estimatedSize += c.size();
			checkAndSwitch();
			return wrapped.addAll(c);
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			return wrapped.removeAll(c);
		}

		@Override
		public boolean removeIf(Predicate<? super E> filter) {
			return wrapped.removeIf(filter);
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			return wrapped.retainAll(c);
		}

		@Override
		public void clear() {
			estimatedSize = 0;
			wrapped.clear();
		}

		@Override
		public boolean equals(Object o) {
			return wrapped.equals(o);
		}

		@Override
		public int hashCode() {
			return wrapped.hashCode();
		}

		@Override
		public Spliterator<E> spliterator() {
			return wrapped.spliterator();
		}

		@Override
		public Stream<E> stream() {
			return wrapped.stream();
		}

		@Override
		public Stream<E> parallelStream() {
			return wrapped.parallelStream();
		}

		@Override
		public void forEach(Consumer<? super E> action) {
			wrapped.forEach(action);
		}
	}

	private static class SyncThresholdAwareSet<E> implements Set<E> {

		private int estimatedSize = 0;
		private final long threshold;
		private final Function<Set<E>, Set<E>> createSyncingSet;
		private Set<E> wrapped;
		private boolean switched = false;

		public SyncThresholdAwareSet(Set<E> wrapped, long threshold, Function<Set<E>, Set<E>> createSyncingSet) {
			this.wrapped = wrapped;
			this.threshold = threshold;
			this.createSyncingSet = createSyncingSet;
		}

		private void checkAndSwitch() {
			if (!switched && estimatedSize > threshold && wrapped.size() > threshold) {
				wrapped = createSyncingSet.apply(wrapped);
				switched = true;
			}
		}

		@Override
		public int size() {
			return wrapped.size();
		}

		@Override
		public boolean isEmpty() {
			return wrapped.isEmpty();
		}

		@Override
		public boolean contains(Object o) {
			return wrapped.contains(o);
		}

		@Override
		public Iterator<E> iterator() {
			return wrapped.iterator();
		}

		@Override
		public Object[] toArray() {
			return wrapped.toArray();
		}

		@Override
		public <T> T[] toArray(T[] a) {
			return wrapped.toArray(a);
		}

		@Override
		public boolean add(E e) {

			boolean add = wrapped.add(e);
			if (add) {
				estimatedSize++;
				checkAndSwitch();
			}
			return add;
		}

		@Override
		public boolean remove(Object o) {
			boolean remove = wrapped.remove(o);
			if (remove) {
				estimatedSize--;
			}
			return remove;
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			return wrapped.containsAll(c);
		}

		@Override
		public boolean addAll(Collection<? extends E> c) {
			estimatedSize += c.size();
			checkAndSwitch();
			return wrapped.addAll(c);
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			return wrapped.retainAll(c);
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			return wrapped.removeAll(c);
		}

		@Override
		public void clear() {
			estimatedSize = 0;
			wrapped.clear();
		}

		@Override
		public boolean equals(Object o) {
			return wrapped.equals(o);
		}

		@Override
		public int hashCode() {
			return wrapped.hashCode();
		}

		@Override
		public Spliterator<E> spliterator() {
			return wrapped.spliterator();
		}

		@Override
		public <T> T[] toArray(IntFunction<T[]> generator) {
			return wrapped.toArray(generator);
		}

		@Override
		public boolean removeIf(Predicate<? super E> filter) {
			return wrapped.removeIf(filter);
		}

		@Override
		public Stream<E> stream() {
			return wrapped.stream();
		}

		@Override
		public Stream<E> parallelStream() {
			return wrapped.parallelStream();
		}

		@Override
		public void forEach(Consumer<? super E> action) {
			wrapped.forEach(action);
		}
	}

	private static class SyncThresholdAwareMap<K, E> implements Map<K, E> {

		private int estimatedSize = 0;
		private final long threshold;
		private final Function<Map<K, E>, Map<K, E>> createSyncingMap;
		private Map<K, E> wrapped;
		private boolean switched = false;

		public SyncThresholdAwareMap(Map<K, E> wrapped, long threshold,
				Function<Map<K, E>, Map<K, E>> createSyncingMap) {
			this.wrapped = wrapped;
			this.threshold = threshold;
			this.createSyncingMap = createSyncingMap;
		}

		private void checkAndSwitch() {
			if (!switched && estimatedSize > threshold && wrapped.size() > threshold) {
				wrapped = createSyncingMap.apply(wrapped);
				switched = true;
			}
		}

		@Override
		public int size() {
			return wrapped.size();
		}

		@Override
		public boolean isEmpty() {
			return wrapped.isEmpty();
		}

		@Override
		public boolean containsKey(Object key) {
			return wrapped.containsKey(key);
		}

		@Override
		public boolean containsValue(Object value) {
			return wrapped.containsValue(value);
		}

		@Override
		public E get(Object key) {
			return wrapped.get(key);
		}

		@Override
		public E put(K key, E value) {
			E put = wrapped.put(key, value);
			if (put == null) {
				estimatedSize++;
				checkAndSwitch();
			}
			return put;
		}

		@Override
		public E remove(Object key) {
			E remove = wrapped.remove(key);
			if (remove != null) {
				estimatedSize--;
			}
			return remove;
		}

		@Override
		public void putAll(Map<? extends K, ? extends E> m) {
			estimatedSize += m.size();
			checkAndSwitch();
			wrapped.putAll(m);
		}

		@Override
		public void clear() {
			estimatedSize = 0;
			wrapped.clear();
		}

		@Override
		public Set<K> keySet() {
			return wrapped.keySet();
		}

		@Override
		public Collection<E> values() {
			return wrapped.values();
		}

		@Override
		public Set<Entry<K, E>> entrySet() {
			return wrapped.entrySet();
		}

		@Override
		public boolean equals(Object o) {
			return wrapped.equals(o);
		}

		@Override
		public int hashCode() {
			return wrapped.hashCode();
		}

		@Override
		public E getOrDefault(Object key, E defaultValue) {
			return wrapped.getOrDefault(key, defaultValue);
		}

		@Override
		public void forEach(BiConsumer<? super K, ? super E> action) {
			wrapped.forEach(action);
		}

		@Override
		public void replaceAll(BiFunction<? super K, ? super E, ? extends E> function) {
			wrapped.replaceAll(function);
		}

		@Override
		public E putIfAbsent(K key, E value) {
			return wrapped.putIfAbsent(key, value);
		}

		@Override
		public boolean remove(Object key, Object value) {
			boolean remove = wrapped.remove(key, value);
			if (remove) {
				estimatedSize--;
			}
			return remove;
		}

		@Override
		public boolean replace(K key, E oldValue, E newValue) {
			return wrapped.replace(key, oldValue, newValue);
		}

		@Override
		public E replace(K key, E value) {
			return wrapped.replace(key, value);
		}

		@Override
		public E computeIfAbsent(K key, Function<? super K, ? extends E> mappingFunction) {
			estimatedSize++;
			checkAndSwitch();
			return wrapped.computeIfAbsent(key, mappingFunction);
		}

		@Override
		public E computeIfPresent(K key, BiFunction<? super K, ? super E, ? extends E> remappingFunction) {
			return wrapped.computeIfPresent(key, remappingFunction);
		}

		@Override
		public E compute(K key, BiFunction<? super K, ? super E, ? extends E> remappingFunction) {
			estimatedSize++;
			checkAndSwitch();
			return wrapped.compute(key, remappingFunction);
		}

		@Override
		public E merge(K key, E value, BiFunction<? super E, ? super E, ? extends E> remappingFunction) {
			return wrapped.merge(key, value, remappingFunction);
		}

	}

}
