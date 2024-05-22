/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.lmdb;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.management.NotificationEmitter;
import javax.management.openmbean.CompositeData;

import org.eclipse.rdf4j.common.io.FileUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.AbstractModel;
import org.eclipse.rdf4j.model.impl.FilteredModel;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.SailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.management.GarbageCollectionNotificationInfo;
import com.sun.management.GcInfo;

/**
 * Model implementation that stores in a {@link LinkedHashModel} until more than 10KB statements are added and the
 * estimated memory usage is more than the amount of free memory available. Once the threshold is cross this
 * implementation seamlessly changes to a disk based {@link SailSourceModel}.
 */
abstract class MemoryOverflowModel extends AbstractModel implements AutoCloseable {

	private static final long serialVersionUID = 4119844228099208169L;

	private static final Runtime RUNTIME = Runtime.getRuntime();

	private static final int LARGE_BLOCK = 5 * 1024;

	private static volatile boolean overflow;

	// To reduce the chance of OOM we will always overflow once we get close to running out of memory even if we think
	// we have space for one more block. The limit is currently set at 32 MB for small heaps and 128 MB for large heaps.
	private static final int MIN_AVAILABLE_MEM_BEFORE_OVERFLOWING = 32 * 1024 * 1024;

	final Logger logger = LoggerFactory.getLogger(MemoryOverflowModel.class);

	private volatile LinkedHashModel memory;

	private transient File dataDir;

	private transient LmdbSailStore store;

	private transient volatile SailSourceModel disk;

	private final boolean verifyAdditions;

	private final SimpleValueFactory vf = SimpleValueFactory.getInstance();

	private static volatile boolean highGcLoad = false;
	private static volatile long lastGcUpdate;
	private static volatile long gcSum;
	private static volatile List<GcInfo> gcInfos = new CopyOnWriteArrayList<>();
	static {
		List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
		for (GarbageCollectorMXBean gcBean : gcBeans) {
			NotificationEmitter emitter = (NotificationEmitter) gcBean;
			emitter.addNotificationListener((notification, o) -> {
				while (! gcInfos.isEmpty()) {
					if (System.currentTimeMillis() - gcInfos.get(0).getEndTime() > 5000) {
						gcSum -= gcInfos.remove(0).getDuration();
					} else {
						break;
					}
				}

				// extract garbage collection information from notification.
				GarbageCollectionNotificationInfo gcNotificationInfo = GarbageCollectionNotificationInfo.from((CompositeData) notification.getUserData());
				GcInfo gcInfo = gcNotificationInfo.getGcInfo();
				gcInfos.add(gcInfo);
				gcSum += gcInfo.getDuration();
				System.out.println("gcSum: " + gcSum);
				if (gcSum > 1000 || gcInfos.size() > 4) {
					highGcLoad = true;
					lastGcUpdate = System.currentTimeMillis();
				} else if (System.currentTimeMillis() - lastGcUpdate > 10000) {
					highGcLoad = false;
				}
			}, null, null);
		}
	}

	public MemoryOverflowModel(boolean verifyAdditions) {
		this.verifyAdditions = verifyAdditions;
		memory = new LinkedHashModel(LARGE_BLOCK);
	}

	public MemoryOverflowModel(Set<Namespace> namespaces, boolean verifyAdditions) {
		this.verifyAdditions = verifyAdditions;
		memory = new LinkedHashModel(namespaces, LARGE_BLOCK);
	}

	@Override
	public synchronized void closeIterator(Iterator<?> iter) {
		super.closeIterator(iter);
		if (disk != null) {
			disk.closeIterator(iter);
		}
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
		if (disk != null || c.size() <= 1024) {
			return getDelegate().addAll(c);
		} else {
			boolean ret = false;
			HashSet<Statement> buffer = new HashSet<>();
			for (Statement st : c) {
				buffer.add(st);
				if (buffer.size() >= 1024) {
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
				MemoryOverflowModel.this.removeTermIteration(iter, subj, pred, obj, contexts);
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

	protected abstract LmdbSailStore createSailStore(File dataDir) throws IOException, SailException;

	private Model getDelegate() {
		var memory = this.memory;
		if (memory != null) {
			return memory;
		} else {
			var disk = this.disk;
			if (disk != null) {
				return disk;
			}
			synchronized (this) {
				if (this.memory != null) {
					return this.memory;
				}
				if (this.disk != null) {
					return this.disk;
				}
				throw new IllegalStateException("MemoryOverflowModel is in an inconsistent state");
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
		if (disk == getDelegate()) {
			return;
		}

		if (highGcLoad) {
			logger.debug("syncing at {} triples due to gc load");
			overflowToDisk();
			System.gc();
		}
	}

	private synchronized void overflowToDisk() {
		overflow = true;
		if (memory == null) {
			assert disk != null;
			return;
		}

		try {
			LinkedHashModel memory = this.memory;
			this.memory = null;

			assert disk == null;
			dataDir = Files.createTempDirectory("model").toFile();
			logger.debug("memory overflow using temp directory {}", dataDir);
			store = createSailStore(dataDir);
			disk = new SailSourceModel(store, memory, verifyAdditions);
			logger.debug("overflow synced to disk");
		} catch (IOException | SailException e) {
			String path = dataDir != null ? dataDir.getAbsolutePath() : "(unknown)";
			logger.error("Error while writing to overflow directory " + path, e);
		}
	}

	@Override
	public void close() throws IOException {
		if (disk != null) {
			logger.debug("closing {}", dataDir);
			try {
				disk.close();
			} catch (Exception e) {
				logger.error(e.toString(), e);
			} finally {
				try {
					store.close();
				} catch (SailException e) {
					logger.error(e.toString(), e);
				} finally {
					FileUtil.deleteDir(dataDir);
					dataDir = null;
					store = null;
					disk = null;
				}
			}
		}
	}
}
