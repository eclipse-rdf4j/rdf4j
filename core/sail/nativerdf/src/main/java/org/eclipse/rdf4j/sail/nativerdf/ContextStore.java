/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.sail.SailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An in-memory index for context information that uses a file for persistence.
 * <p>
 * The context index file has an 8-byte header consisting of:
 *
 * <pre>
 * 	byte 1-3         : the magic number marker
 *  byte 4           : the file format version
 *  byte 5-8         : the number of mapped contexts contained in the file, as an int.
 * </pre>
 *
 * Each context is encoded in the file as a record, as follows:
 *
 * <pre>
 *   byte 1 - 8      : the number of statements in the content, as a long.
 *   byte 9          : boolean flag indicating the type of context identifier (1 = IRI, 0 = blank node)
 *   byte 10 - 11    : the length of the encoded context identifier
 *   byte 12 - A     : the UTF-8 encoded the encoded context identifer
 * </pre>
 *
 * @author Jeen Broekstra
 *
 */
class ContextStore implements Iterable<Resource> {

	static final Logger logger = LoggerFactory.getLogger(ContextStore.class);

	private static final String FILE_NAME = "contexts.dat";

	/**
	 * Magic number "Native Context File" to detect whether the file is actually a context file. The first three bytes
	 * of the file should be equal to this magic number.
	 */
	private static final byte[] MAGIC_NUMBER = new byte[] { 'n', 'c', 'f' };

	/**
	 * File format version, stored as the fourth byte in context files.
	 */
	private static final byte FILE_FORMAT_VERSION = 1;

	/**
	 * The data file for this {@link ContextStore}.
	 */
	private final File file;

	private final Map<Resource, Long> contextInfoMap;

	/**
	 * Flag indicating whether the contents of this {@link ContextStore} are different from what is stored on disk.
	 */
	private volatile boolean contentsChanged;

	private final ValueFactory valueFactory;

	private final NativeSailStore store;

	ContextStore(NativeSailStore store, File dataDir) throws IOException {
		Objects.requireNonNull(store);
		Objects.requireNonNull(dataDir);

		this.file = new File(dataDir, FILE_NAME);
		this.valueFactory = store.getValueFactory();
		this.store = store;

		contextInfoMap = new HashMap<>(16);

		try {
			readContextsFromFile();
		} catch (FileNotFoundException fe) {
			logger.debug("context index has not been created yet: " + fe.getMessage());
			initializeContextCache();
			writeContextsToFile();
			logger.debug("context index construction complete");
		} catch (IOException ioe) {
			logger.info("could not read context index: " + ioe.getMessage(), ioe);
			logger.debug("attempting reconstruction from store (this may take a while)");
			initializeContextCache();
			writeContextsToFile();
			logger.info("context index reconstruction complete");
		}
	}

	/**
	 * Increase the size of the context. If the context was not yet known, it is created with a size of 1.
	 *
	 * @param context the context identifier.
	 */
	void increment(Resource context) {
		contextInfoMap.merge(context, 1L, (size, one) -> size + one);
		contentsChanged = true;
	}

	/**
	 * Decrease the size of the context by the given amount. If the size reaches zero, the context is removed.
	 *
	 * @param context the context identifier.
	 * @param amount  the number by which to decrease the size
	 */
	void decrementBy(Resource context, long amount) {
		contextInfoMap.computeIfPresent(context, (c, size) -> size <= amount ? null : size - amount);
		contentsChanged = true;
	}

	@Override
	public Iterator<Resource> iterator() {
		return contextInfoMap.keySet().iterator();
	}

	void clear() {
		if (!contextInfoMap.isEmpty()) {
			contextInfoMap.clear();
			contentsChanged = true;
		}
	}

	void close() {
	}

	void sync() throws IOException {
		if (contentsChanged) {
			// Flush the changes to disk
			writeContextsToFile();
			contentsChanged = false;
		}
	}

	private void writeContextsToFile() throws IOException {
		synchronized (file) {
			try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
				out.write(MAGIC_NUMBER);
				out.writeByte(FILE_FORMAT_VERSION);
				out.writeInt(contextInfoMap.size());
				for (Entry<Resource, Long> entry : contextInfoMap.entrySet()) {
					out.writeLong(entry.getValue());
					out.writeBoolean(entry.getKey() instanceof IRI);
					out.writeUTF(entry.getKey().stringValue());
				}
			}
		}
	}

	private void initializeContextCache() throws IOException {
		logger.debug("initializing context cache");
		try (CloseableIteration<Resource, SailException> contextIter = store.getContexts()) {
			while (contextIter.hasNext()) {
				increment(contextIter.next());
			}
		}
	}

	private void readContextsFromFile() throws IOException {
		synchronized (file) {
			if (!file.exists()) {
				throw new FileNotFoundException("context index file " + file + " does not exist");
			}

			try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
				byte[] magicNumber = IOUtil.readBytes(in, MAGIC_NUMBER.length);
				if (!Arrays.equals(magicNumber, MAGIC_NUMBER)) {
					throw new IOException("File doesn't contain compatible context data");
				}

				byte version = in.readByte();
				if (version > FILE_FORMAT_VERSION) {
					throw new IOException("Unable to read context file; it uses a newer file format");
				} else if (version != FILE_FORMAT_VERSION) {
					throw new IOException("Unable to read context file; invalid file format version: " + version);
				}

				final int size = in.readInt();

				while (true) {
					try {
						long contextSize = in.readLong();
						boolean isIRI = in.readBoolean();
						String contextId = in.readUTF();

						Resource context = isIRI ? valueFactory.createIRI(contextId)
								: valueFactory.createBNode(contextId);
						contextInfoMap.put(context, contextSize);
					} catch (EOFException e) {
						break;
					} catch (IllegalArgumentException e) {
						throw new IOException("unable to parse context identifier: ", e);
					}
				}

				if (contextInfoMap.size() != size) {
					throw new IOException("Unable to read context file; size checksum validation failed");
				}
			}
		}
	}

}
