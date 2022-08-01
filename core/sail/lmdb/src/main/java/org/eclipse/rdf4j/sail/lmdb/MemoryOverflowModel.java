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
import java.nio.file.Files;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

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
import org.eclipse.rdf4j.sail.base.SailStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Model implementation that stores in a {@link LinkedHashModel} until more than 10KB statements are added and the
 * estimated memory usage is more than the amount of free memory available. Once the threshold is cross this
 * implementation seamlessly changes to a disk based {@link SailSourceModel}.
 *
 */
abstract class MemoryOverflowModel extends AbstractModel {

	private static final long serialVersionUID = 4119844228099208169L;

	private static final Runtime RUNTIME = Runtime.getRuntime();

	private static final int LARGE_BLOCK = 10000;

	// To reduce the chance of OOM we will always overflow once we get close to running out of memory even if we think
	// we have space for one more block. The limit is currently set at 32 MB
	private static final int MIN_AVAILABLE_MEM_BEFORE_OVERFLOWING = 32 * 1024 * 1024;

	final Logger logger = LoggerFactory.getLogger(MemoryOverflowModel.class);

	private LinkedHashModel memory;

	transient File dataDir;

	transient SailStore store;

	transient SailSourceModel disk;

	private long baseline = 0;

	private long maxBlockSize = 0;

	SimpleValueFactory vf = SimpleValueFactory.getInstance();

	public MemoryOverflowModel() {
		memory = new LinkedHashModel(LARGE_BLOCK);
	}

	public MemoryOverflowModel(Model model) {
		this(model.getNamespaces());
		addAll(model);
	}

	public MemoryOverflowModel(Set<Namespace> namespaces, Collection<? extends Statement> c) {
		this(namespaces);
		addAll(c);
	}

	public MemoryOverflowModel(Set<Namespace> namespaces) {
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

	protected abstract SailStore createSailStore(File dataDir) throws IOException, SailException;

	synchronized Model getDelegate() {
		if (disk == null) {
			return memory;
		}
		return disk;
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
		if (disk == null) {
			int size = size();
			if (size >= LARGE_BLOCK && size % LARGE_BLOCK == 0) {
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

				if (baseline > 0) {
					long blockSize = used - baseline;
					if (blockSize > maxBlockSize) {
						maxBlockSize = blockSize;
					}

					// Sync if either the estimated size of the next block is larger than remaining memory, or
					// if less than 15% of the heap is still free (this last condition to avoid GC overhead limit)
					if (freeToAllocateMemory < MIN_AVAILABLE_MEM_BEFORE_OVERFLOWING ||
							freeToAllocateMemory < Math.min(0.15 * maxMemory, maxBlockSize)) {
						logger.debug("syncing at {} triples. max block size: {}", size, maxBlockSize);
						overflowToDisk();
					}
				}
				baseline = used;
			}
		}
	}

	private synchronized void overflowToDisk() {
		try {
			assert disk == null;
			dataDir = Files.createTempDirectory("model").toFile();
			logger.debug("memory overflow using temp directory {}", dataDir);
			store = createSailStore(dataDir);
			disk = new SailSourceModel(store) {

				@Override
				protected void finalize() throws Throwable {
					logger.debug("finalizing {}", dataDir);
					if (disk == this) {
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
					super.finalize();
				}
			};
			disk.addAll(memory);
			memory = new LinkedHashModel(memory.getNamespaces(), LARGE_BLOCK);
			logger.debug("overflow synced to disk");
		} catch (IOException | SailException e) {
			String path = dataDir != null ? dataDir.getAbsolutePath() : "(unknown)";
			logger.error("Error while writing to overflow directory " + path, e);
		}
	}
}
