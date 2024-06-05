/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.model.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

import javax.management.NotificationEmitter;
import javax.management.openmbean.CompositeData;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.management.GarbageCollectionNotificationInfo;
import com.sun.management.GcInfo;

@InternalUseOnly
public abstract class AbstractMemoryOverflowModel<T extends AbstractModel> extends AbstractModel {

	private static final long serialVersionUID = 4119844228099208169L;

	private static final Runtime RUNTIME = Runtime.getRuntime();

	/**
	 * The default batch size is 1024. This is the number of statements that will be written to disk at a time.
	 */
	@SuppressWarnings("StaticNonFinalField")
	public static int BATCH_SIZE = 1024;

	/**
	 * ms GC activity over the past second that triggers overflow to disk
	 */
	@SuppressWarnings("StaticNonFinalField")
	public static int MEMORY_THRESHOLD_HIGH = 300;

	/**
	 * ms GC activity over the past second that disables overflow to disk
	 */
	@SuppressWarnings("StaticNonFinalField")
	public static int MEMORY_THRESHOLD_MEDIUM = 200;

	/**
	 * ms GC activity over the past second that skips overflow to disk in anticipation of GC freeing up memory
	 */
	@SuppressWarnings("StaticNonFinalField")
	public static int MEMORY_THRESHOLD_LOW = 100;

	private static volatile boolean overflow;

	// To reduce the chance of OOM we will always overflow once we get close to running out of memory even if we think
	// we have space for one more block. The limit is currently set at 32 MB for small heaps and 128 MB for large heaps.
	/**
	 * The minimum amount of free memory before overflowing to disk. Defaults to 32 MB for heaps smaller than 1 GB and
	 * 128 MB for heaps larger than 1 GB.
	 */
	@SuppressWarnings("StaticNonFinalField")
	public static int MIN_AVAILABLE_MEM_BEFORE_OVERFLOWING = RUNTIME.maxMemory() >= 1024 * 1024 * 1024 ? 128 : 32;

	static final Logger logger = LoggerFactory.getLogger(AbstractMemoryOverflowModel.class);

	private volatile LinkedHashModel memory;

	protected transient volatile T disk;

	private final SimpleValueFactory vf = SimpleValueFactory.getInstance();

	private static final int[] GC_LOAD = new int[10];
	private static int prevBucket;

	private static volatile boolean highGcLoad = false;
	private static final Queue<GcInfo> gcInfos = new ConcurrentLinkedQueue<>();

	// if we are in a low memory situation and the GC load is low we will not overflow to disk size it is likely that
	// the GC will be able to free up enough memory
	private static volatile boolean lowMemLowGcSum = false;

	private final ReentrantLock lock = new ReentrantLock();

	static {
		List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
		for (GarbageCollectorMXBean gcBean : gcBeans) {
			NotificationEmitter emitter = (NotificationEmitter) gcBean;
			emitter.addNotificationListener((notification, o) -> {

				long currentBucket = (System.currentTimeMillis() / 100) % 10;
				while (currentBucket != prevBucket) {
					prevBucket = (prevBucket + 1) % 10;
					GC_LOAD[prevBucket] = 0;
				}

				while (true) {
					GcInfo poll = gcInfos.poll();
					if (poll == null) {
						break;
					}
					GC_LOAD[(int) currentBucket] += (int) poll.getDuration();
				}

				// extract garbage collection information from notification.
				GarbageCollectionNotificationInfo gcNotificationInfo = GarbageCollectionNotificationInfo
						.from((CompositeData) notification.getUserData());
				GcInfo gcInfo = gcNotificationInfo.getGcInfo();
				gcInfos.add(gcInfo);
				long gcSum = 0;
				long gcMax = 0;
				for (int i : GC_LOAD) {
					gcSum += i;
					if (i > gcMax) {
						gcMax = i;
					}
				}

				if (gcSum < gcMax * 1.3) {
					gcSum -= (gcMax / 2);
				}

				double v = mbFree();
				if (!highGcLoad && !lowMemLowGcSum && v < MIN_AVAILABLE_MEM_BEFORE_OVERFLOWING
						&& gcSum < MEMORY_THRESHOLD_LOW) {
					lowMemLowGcSum = true;
					return;
				}

				lowMemLowGcSum = false;

				if (!highGcLoad && v < 256
						&& (gcSum > MEMORY_THRESHOLD_HIGH || v < MIN_AVAILABLE_MEM_BEFORE_OVERFLOWING)) {
					logger.debug("High GC load detected. Free memory: {} MB, GC sum: {} ms in past 1000 ms", v, gcSum);
					highGcLoad = true;
				} else if ((v > 256 || gcSum < MEMORY_THRESHOLD_MEDIUM) && highGcLoad
						&& v > MIN_AVAILABLE_MEM_BEFORE_OVERFLOWING) {
					logger.debug("GC load back to normal. Free memory: {} MB, GC sum: {} ms in past 1000 ms", v, gcSum);
					highGcLoad = false;
				}

			}, null, null);
		}
	}

	private static double mbFree() {
		// maximum heap size the JVM can allocate
		long maxMemory = RUNTIME.maxMemory();

		// total currently allocated JVM memory
		long totalMemory = RUNTIME.totalMemory();

		// amount of memory free in the currently allocated JVM memory
		long freeMemory = RUNTIME.freeMemory();

		// estimated memory used
		long used = totalMemory - freeMemory;

		// amount of memory the JVM can still allocate from the OS (upper boundary is the max heap)
		long freeToAllocateMemory = maxMemory - used;

		return (freeToAllocateMemory / 1024.0 / 1024.0);

	}

	private volatile boolean closed;

	public AbstractMemoryOverflowModel() {
		memory = new LinkedHashModel();
	}

	public AbstractMemoryOverflowModel(Set<Namespace> namespaces) {
		memory = new LinkedHashModel(namespaces, 0);
	}

	@Override
	public synchronized Set<Namespace> getNamespaces() {
		return memory.getNamespaces();
	}

	@Override
	public synchronized Optional<Namespace> getNamespace(String prefix) {
		return memory.getNamespace(prefix);
	}

	@Override
	public synchronized Namespace setNamespace(String prefix, String name) {
		return memory.setNamespace(prefix, name);
	}

	@Override
	public void setNamespace(Namespace namespace) {
		memory.setNamespace(namespace);
	}

	@Override
	public synchronized Optional<Namespace> removeNamespace(String prefix) {
		return memory.removeNamespace(prefix);
	}

	@Override
	public boolean contains(Resource subj, IRI pred, Value obj, Resource... contexts) {
		return getDelegate().contains(subj, pred, obj, contexts);
	}

	@Override
	public boolean add(Resource subj, IRI pred, Value obj, Resource... contexts) {
		checkMemoryOverflow();
		return getDelegate().add(subj, pred, obj, contexts);
	}

	@Override
	public boolean add(Statement st) {
		checkMemoryOverflow();
		return getDelegate().add(st);
	}

	@Override
	public boolean addAll(Collection<? extends Statement> c) {
		checkMemoryOverflow();
		if (disk != null || c.size() <= BATCH_SIZE) {
			return getDelegate().addAll(c);
		} else {
			boolean ret = false;
			HashSet<Statement> buffer = new HashSet<>();
			for (Statement st : c) {
				buffer.add(st);
				if (buffer.size() >= BATCH_SIZE) {
					ret |= getDelegate().addAll(buffer);
					buffer.clear();
					checkMemoryOverflow();
				}
			}
			if (!buffer.isEmpty()) {
				ret |= getDelegate().addAll(buffer);
				buffer.clear();
			}

			return ret;

		}

	}

	@Override
	public boolean remove(Resource subj, IRI pred, Value obj, Resource... contexts) {
		return getDelegate().remove(subj, pred, obj, contexts);
	}

	@Override
	public int size() {
		return getDelegate().size();
	}

	@Override
	public Iterator<Statement> iterator() {
		return getDelegate().iterator();
	}

	@Override
	public boolean clear(Resource... contexts) {
		return getDelegate().clear(contexts);
	}

	@Override
	public Model filter(final Resource subj, final IRI pred, final Value obj, final Resource... contexts) {
		return new FilteredModel(this, subj, pred, obj, contexts) {

			private static final long serialVersionUID = -475666402618133101L;

			@Override
			public int size() {
				return getDelegate().filter(subj, pred, obj, contexts).size();
			}

			@Override
			public Iterator<Statement> iterator() {
				return getDelegate().filter(subj, pred, obj, contexts).iterator();
			}

			@Override
			protected void removeFilteredTermIteration(Iterator<Statement> iter, Resource subj, IRI pred, Value obj,
					Resource... contexts) {
				AbstractMemoryOverflowModel.this.removeTermIteration(iter, subj, pred, obj, contexts);
			}
		};
	}

	@Override
	public synchronized void removeTermIteration(Iterator<Statement> iter, Resource subj, IRI pred, Value obj,
			Resource... contexts) {
		if (disk == null) {
			memory.removeTermIteration(iter, subj, pred, obj, contexts);
		} else {
			disk.removeTermIteration(iter, subj, pred, obj, contexts);
		}
	}

	private Model getDelegate() {
		var memory = this.memory;
		if (memory != null) {
			return memory;
		} else {
			var disk = this.disk;
			if (disk != null) {
				return disk;
			}

			try {
				lock.lockInterruptibly();
				try {
					if (this.memory != null) {
						return this.memory;
					}
					if (this.disk != null) {
						return this.disk;
					}
					if (closed) {
						throw new IllegalStateException("MemoryOverflowModel is closed");
					}
					throw new IllegalStateException("MemoryOverflowModel is in an inconsistent state");
				} finally {
					lock.unlock();
				}

			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException(e);
			}

		}
	}

	private void writeObject(ObjectOutputStream s) throws IOException {
		// Write out any hidden serialization magic
		s.defaultWriteObject();
		// Write in size
		Model delegate = getDelegate();
		s.writeInt(delegate.size());
		// Write in all elements
		for (Statement st : delegate) {
			Resource subj = st.getSubject();
			IRI pred = st.getPredicate();
			Value obj = st.getObject();
			Resource ctx = st.getContext();
			s.writeObject(vf.createStatement(subj, pred, obj, ctx));
		}
	}

	private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
		// Read in any hidden serialization magic
		s.defaultReadObject();
		// Read in size
		int size = s.readInt();
		// Read in all elements
		for (int i = 0; i < size; i++) {
			add((Statement) s.readObject());
		}
	}

	private synchronized void checkMemoryOverflow() {
		try {
			lock.lockInterruptibly();
			try {

				if (disk == getDelegate()) {
					return;
				}

				if (overflow || highGcLoad) {
					logger.debug("Syncing triples to disk due to gc load");
					overflowToDisk();
					if (!highGcLoad) {
						overflow = false;
					}
				}
			} finally {
				lock.unlock();
			}

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}

	}

	private synchronized void overflowToDisk() {

		try {
			lock.lockInterruptibly();
			try {
				overflow = true;
				if (memory == null) {
					assert disk != null;
					return;
				}

				LinkedHashModel memory = this.memory;
				this.memory = null;
				overflowToDiskInner(memory);

				logger.debug("overflow synced to disk");
				System.gc();
			} finally {
				lock.unlock();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}

	}

	protected abstract void overflowToDiskInner(Model memory);
}
