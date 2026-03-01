/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.s3;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.AbstractValueFactory;
import org.eclipse.rdf4j.sail.s3.storage.ObjectStore;
import org.eclipse.rdf4j.sail.s3.storage.Varint;

/**
 * In-memory value store that maps RDF {@link Value} objects to long IDs and vice-versa. Uses {@link ConcurrentHashMap}
 * for thread-safe bidirectional lookup.
 */
class S3ValueStore extends AbstractValueFactory {

	static final long UNKNOWN_ID = -1;
	static final String VALUES_KEY = "values/current";

	private static final byte TYPE_IRI = 0;
	private static final byte TYPE_LITERAL = 1;
	private static final byte TYPE_BNODE = 2;

	private final ConcurrentHashMap<Value, Long> valueToId = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Long, Value> idToValue = new ConcurrentHashMap<>();
	private final AtomicLong nextId = new AtomicLong(1);

	/**
	 * Stores the supplied value and returns the ID assigned to it. If the value already exists, returns the existing
	 * ID.
	 *
	 * @param value the value to store
	 * @return the ID assigned to the value
	 */
	public long storeValue(Value value) {
		Long existing = valueToId.get(value);
		if (existing != null) {
			return existing;
		}
		long id = nextId.getAndIncrement();
		Long previous = valueToId.putIfAbsent(value, id);
		if (previous != null) {
			// another thread stored it first
			return previous;
		}
		idToValue.put(id, value);
		return id;
	}

	/**
	 * Gets the ID for the specified value.
	 *
	 * @param value a value
	 * @return the ID for the value, or {@link #UNKNOWN_ID} if not found
	 */
	public long getId(Value value) {
		Long id = valueToId.get(value);
		return id != null ? id : UNKNOWN_ID;
	}

	/**
	 * Gets the value for the specified ID.
	 *
	 * @param id a value ID
	 * @return the value, or {@code null} if not found
	 */
	public Value getValue(long id) {
		return idToValue.get(id);
	}

	/**
	 * Removes all stored values and resets the ID counter.
	 */
	public void clear() {
		valueToId.clear();
		idToValue.clear();
		nextId.set(1);
	}

	/**
	 * Returns the next value ID that would be assigned.
	 */
	long getNextId() {
		return nextId.get();
	}

	/**
	 * Serializes the value store to the object store.
	 */
	void serialize(ObjectStore objectStore) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(baos);
			ByteBuffer buf = ByteBuffer.allocate(9); // reusable scratch for varints

			writeVarint(out, buf, idToValue.size());

			for (Map.Entry<Long, Value> entry : idToValue.entrySet()) {
				writeVarint(out, buf, entry.getKey());
				Value val = entry.getValue();

				if (val instanceof IRI) {
					out.writeByte(TYPE_IRI);
					writeBytes(out, buf, val.stringValue().getBytes(StandardCharsets.UTF_8));
				} else if (val instanceof Literal) {
					out.writeByte(TYPE_LITERAL);
					Literal lit = (Literal) val;
					writeBytes(out, buf, lit.getLabel().getBytes(StandardCharsets.UTF_8));
					writeBytes(out, buf, lit.getDatatype().stringValue().getBytes(StandardCharsets.UTF_8));
					writeBytes(out, buf, lit.getLanguage().orElse("").getBytes(StandardCharsets.UTF_8));
				} else if (val instanceof BNode) {
					out.writeByte(TYPE_BNODE);
					writeBytes(out, buf, ((BNode) val).getID().getBytes(StandardCharsets.UTF_8));
				} else {
					throw new IllegalStateException("Unsupported value type: " + val.getClass());
				}
			}

			out.flush();
			objectStore.put(VALUES_KEY, baos.toByteArray());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Deserializes the value store from the object store.
	 */
	void deserialize(ObjectStore objectStore, long nextValueId) {
		byte[] data = objectStore.get(VALUES_KEY);
		if (data == null) {
			return;
		}

		try {
			ByteBuffer bb = ByteBuffer.wrap(data);
			long count = Varint.readUnsigned(bb);

			for (long i = 0; i < count; i++) {
				long id = Varint.readUnsigned(bb);
				int type = bb.get() & 0xFF;

				Value val;
				switch (type) {
				case TYPE_IRI: {
					int len = (int) Varint.readUnsigned(bb);
					byte[] payload = new byte[len];
					bb.get(payload);
					val = createIRI(new String(payload, StandardCharsets.UTF_8));
					break;
				}
				case TYPE_LITERAL: {
					int labelLen = (int) Varint.readUnsigned(bb);
					byte[] labelBytes = new byte[labelLen];
					bb.get(labelBytes);
					String label = new String(labelBytes, StandardCharsets.UTF_8);

					int dtLen = (int) Varint.readUnsigned(bb);
					byte[] dtBytes = new byte[dtLen];
					bb.get(dtBytes);
					String dt = new String(dtBytes, StandardCharsets.UTF_8);

					int langLen = (int) Varint.readUnsigned(bb);
					byte[] langBytes = new byte[langLen];
					bb.get(langBytes);
					String lang = new String(langBytes, StandardCharsets.UTF_8);

					IRI datatypeIRI = createIRI(dt);
					if (!lang.isEmpty()) {
						val = createLiteral(label, lang);
					} else {
						val = createLiteral(label, datatypeIRI);
					}
					break;
				}
				case TYPE_BNODE: {
					int len = (int) Varint.readUnsigned(bb);
					byte[] payload = new byte[len];
					bb.get(payload);
					val = createBNode(new String(payload, StandardCharsets.UTF_8));
					break;
				}
				default:
					throw new IllegalStateException("Unknown value type: " + type);
				}

				valueToId.put(val, id);
				idToValue.put(id, val);
			}

			nextId.set(nextValueId);
		} catch (Exception e) {
			throw new RuntimeException("Failed to deserialize value store", e);
		}
	}

	/**
	 * Closes the value store, releasing all resources.
	 */
	public void close() {
		clear();
	}

	private static void writeVarint(DataOutputStream out, ByteBuffer buf, long value) throws IOException {
		buf.clear();
		Varint.writeUnsigned(buf, value);
		out.write(buf.array(), 0, buf.position());
	}

	private static void writeBytes(DataOutputStream out, ByteBuffer buf, byte[] data) throws IOException {
		writeVarint(out, buf, data.length);
		out.write(data);
	}
}
