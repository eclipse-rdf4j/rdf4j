/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;

/**
 * An in-memory store for namespace prefix information that uses a file for persistence. Namespaces are encoded in the
 * file as records as follows:
 *
 * <pre>
 *   byte 1 - 2     : the length of the encoded namespace name
 *   byte 3 - A     : the UTF-8 encoded namespace name
 *   byte A+1 - A+2 : the length of the encoded namespace prefix
 *   byte A+3 - end : the UTF-8 encoded namespace prefix
 * </pre>
 *
 * @author Arjohn Kampman
 */
class NamespaceStore implements Iterable<SimpleNamespace> {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final String FILE_NAME = "namespaces.dat";

	/**
	 * Magic number "Native Namespace File" to detect whether the file is actually a namespace file. The first three
	 * bytes of the file should be equal to this magic number.
	 */
	private static final byte[] MAGIC_NUMBER = new byte[] { 'n', 'n', 'f' };

	/**
	 * File format version, stored as the fourth byte in namespace files.
	 */
	private static final byte FILE_FORMAT_VERSION = 1;

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The data file for this NamespaceStore.
	 */
	private final File file;

	/**
	 * Map storing namespace information by their prefix.
	 */
	private final Map<String, SimpleNamespace> namespacesMap;

	/**
	 * Flag indicating whether the contents of this NamespaceStore are different from what is stored on disk.
	 */
	private volatile boolean contentsChanged;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public NamespaceStore(File dataDir) throws IOException {
		file = new File(dataDir, FILE_NAME);

		namespacesMap = new LinkedHashMap<>(16);

		if (file.exists()) {
			readNamespacesFromFile();
		} else {
			// Make sure the file exists
			writeNamespacesToFile();
		}

		contentsChanged = false;
	}

	/*---------*
	 * Methods *
	 *---------*/

	public String getNamespace(String prefix) {
		String result = null;
		SimpleNamespace namespace = namespacesMap.get(prefix);
		if (namespace != null) {
			result = namespace.getName();
		}
		return result;
	}

	public void setNamespace(String prefix, String name) {
		SimpleNamespace ns = namespacesMap.get(prefix);

		if (ns != null) {
			if (!ns.getName().equals(name)) {
				ns.setName(name);
				contentsChanged = true;
			}
		} else {
			namespacesMap.put(prefix, new SimpleNamespace(prefix, name));
			contentsChanged = true;
		}
	}

	public void removeNamespace(String prefix) {
		SimpleNamespace ns = namespacesMap.remove(prefix);

		if (ns != null) {
			contentsChanged = true;
		}
	}

	@Override
	public Iterator<SimpleNamespace> iterator() {
		return namespacesMap.values().iterator();
	}

	public void clear() {
		if (!namespacesMap.isEmpty()) {
			namespacesMap.clear();
			contentsChanged = true;
		}
	}

	public void sync() throws IOException {
		if (contentsChanged) {
			// Flush the changes to disk
			writeNamespacesToFile();
			contentsChanged = false;
		}
	}

	public void close() {
	}

	/*----------*
	 * File I/O *
	 *----------*/

	private void writeNamespacesToFile() throws IOException {
		synchronized (file) {
			try (DataOutputStream out = new DataOutputStream(new FileOutputStream(file))) {
				out.write(MAGIC_NUMBER);
				out.writeByte(FILE_FORMAT_VERSION);

				for (Namespace ns : namespacesMap.values()) {
					out.writeUTF(ns.getName());
					out.writeUTF(ns.getPrefix());
				}
			}
		}
	}

	private void readNamespacesFromFile() throws IOException {
		synchronized (file) {
			try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
				byte[] magicNumber = IOUtil.readBytes(in, MAGIC_NUMBER.length);
				if (!Arrays.equals(magicNumber, MAGIC_NUMBER)) {
					throw new IOException("File doesn't contain compatible namespace data");
				}

				byte version = in.readByte();
				if (version > FILE_FORMAT_VERSION) {
					throw new IOException("Unable to read namespace file; it uses a newer file format");
				} else if (version != FILE_FORMAT_VERSION) {
					throw new IOException("Unable to read namespace file; invalid file format version: " + version);
				}

				while (true) {
					try {
						String name = in.readUTF();
						String prefix = in.readUTF();

						SimpleNamespace ns = new SimpleNamespace(prefix, name);
						namespacesMap.put(prefix, ns);
					} catch (EOFException e) {
						break;
					}
				}
			}
		}
	}

	/*-------------------*
	 * Debugging methods *
	 *-------------------*/

	public static void main(String[] args) throws Exception {
		NamespaceStore nsStore = new NamespaceStore(new File(args[0]));

		for (Namespace ns : nsStore) {
			System.out.println(ns.getPrefix() + " = " + ns.getName());
		}
	}
}
