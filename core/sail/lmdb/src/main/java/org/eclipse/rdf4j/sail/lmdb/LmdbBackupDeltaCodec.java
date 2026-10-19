/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.TripleTerm;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

final class LmdbBackupDeltaCodec {

	private static final byte[] MAGIC = "R4J-LMDB-DELTA-1".getBytes(StandardCharsets.US_ASCII);
	private static final byte[] CHAINED_MAGIC = "R4J-LMDB-DELTA-2".getBytes(StandardCharsets.US_ASCII);

	record Journal(long previousTransactionId, List<Record> records) {
		Journal {
			records = List.copyOf(records);
		}
	}

	private static final byte OP_ADD = 1;
	private static final byte OP_REMOVE = 2;
	private static final byte OP_SET_NAMESPACE = 3;
	private static final byte OP_REMOVE_NAMESPACE = 4;
	private static final byte OP_CLEAR_NAMESPACES = 5;

	private static final byte VALUE_IRI = 1;
	private static final byte VALUE_BNODE = 2;
	private static final byte VALUE_LITERAL = 3;
	private static final byte VALUE_TRIPLE = 4;

	private LmdbBackupDeltaCodec() {
	}

	static void write(OutputStream outputStream, List<Record> records) throws IOException {
		writeJournal(outputStream, records, -1L);
	}

	static void write(OutputStream outputStream, List<Record> records, long previousTransactionId) throws IOException {
		if (previousTransactionId < 0L) {
			throw new IllegalArgumentException("Previous transaction ID must not be negative");
		}
		writeJournal(outputStream, records, previousTransactionId);
	}

	private static void writeJournal(OutputStream outputStream, List<Record> records, long previousTransactionId)
			throws IOException {
		try (DataOutputStream out = new DataOutputStream(outputStream)) {
			out.writeInt(MAGIC.length);
			out.write(previousTransactionId < 0 ? MAGIC : CHAINED_MAGIC);
			if (previousTransactionId >= 0) {
				out.writeLong(previousTransactionId);
			}
			out.writeInt(records.size());
			for (Record record : records) {
				writeRecord(out, record);
			}
		}
	}

	static void writeRecord(DataOutputStream out, Record record) throws IOException {
		if (record.isStatementOperation()) {
			out.writeByte(record.isAddition() ? OP_ADD : OP_REMOVE);
			out.writeBoolean(record.isExplicit());
			writeStatement(out, record.getStatement());
		} else {
			switch (record.getNamespaceOperation()) {
			case SET -> {
				out.writeByte(OP_SET_NAMESPACE);
				writeString(out, record.getNamespacePrefix());
				writeString(out, record.getNamespaceName());
			}
			case REMOVE -> {
				out.writeByte(OP_REMOVE_NAMESPACE);
				writeString(out, record.getNamespacePrefix());
			}
			case CLEAR -> out.writeByte(OP_CLEAR_NAMESPACES);
			}
		}
	}

	static List<Record> read(InputStream inputStream) throws IOException {
		return readJournal(inputStream).records();
	}

	static Journal readJournal(InputStream inputStream) throws IOException {
		return readJournal(inputStream, true);
	}

	static long validateJournal(InputStream inputStream) throws IOException {
		return readJournal(inputStream, false).previousTransactionId();
	}

	private static Journal readJournal(InputStream inputStream, boolean retainRecords) throws IOException {
		ValueFactory vf = SimpleValueFactory.getInstance();
		try (DataInputStream in = new DataInputStream(inputStream)) {
			if (in.readInt() != MAGIC.length) {
				throw new IOException("Unexpected LMDB delta log header length");
			}
			byte[] magic = new byte[MAGIC.length];
			in.readFully(magic);
			boolean chained = java.util.Arrays.equals(CHAINED_MAGIC, magic);
			if (!chained && !java.util.Arrays.equals(MAGIC, magic)) {
				throw new IOException("Unexpected LMDB delta log header");
			}
			long previousTransactionId = chained ? in.readLong() : -1L;
			if (chained && previousTransactionId < 0) {
				throw new IOException("Negative previous transaction ID in backup delta");
			}
			int recordCount = in.readInt();
			if (recordCount < 0) {
				throw new IOException("Negative record count in backup delta");
			}
			List<Record> records = retainRecords ? new ArrayList<>(recordCount) : List.of();
			for (int i = 0; i < recordCount; i++) {
				Record record = readRecord(in, vf);
				if (retainRecords) {
					records.add(record);
				}
			}
			return new Journal(previousTransactionId, records);
		}
	}

	static Record readRecord(DataInputStream in, ValueFactory vf) throws IOException {
		byte operation = in.readByte();
		return switch (operation) {
		case OP_ADD -> new Record(true, in.readBoolean(), readStatement(in, vf));
		case OP_REMOVE -> new Record(false, in.readBoolean(), readStatement(in, vf));
		case OP_SET_NAMESPACE -> new Record(NamespaceOperation.SET, readString(in), readString(in));
		case OP_REMOVE_NAMESPACE -> new Record(NamespaceOperation.REMOVE, readString(in), null);
		case OP_CLEAR_NAMESPACES -> new Record(NamespaceOperation.CLEAR, null, null);
		default -> throw new IOException("Unexpected operation marker in backup delta: " + operation);
		};
	}

	private static void writeStatement(DataOutputStream out, Statement statement) throws IOException {
		writeValue(out, statement.getSubject());
		writeValue(out, statement.getPredicate());
		writeValue(out, statement.getObject());
		Resource context = statement.getContext();
		out.writeBoolean(context != null);
		if (context != null) {
			writeValue(out, context);
		}
	}

	private static Statement readStatement(DataInputStream in, ValueFactory vf) throws IOException {
		Resource subject = toResource(readValue(in, vf));
		IRI predicate = toIRI(readValue(in, vf));
		Value object = readValue(in, vf);
		boolean hasContext = in.readBoolean();
		Resource context = hasContext ? toResource(readValue(in, vf)) : null;
		return context == null ? vf.createStatement(subject, predicate, object)
				: vf.createStatement(subject, predicate, object, context);
	}

	private static void writeValue(DataOutputStream out, Value value) throws IOException {
		if (value instanceof IRI iri) {
			out.writeByte(VALUE_IRI);
			writeString(out, iri.stringValue());
			return;
		}
		if (value instanceof BNode bNode) {
			out.writeByte(VALUE_BNODE);
			writeString(out, bNode.getID());
			return;
		}
		if (value instanceof Literal literal) {
			out.writeByte(VALUE_LITERAL);
			writeString(out, literal.getLabel());
			writeString(out, literal.getDatatype().stringValue());
			String language = literal.getLanguage().orElse("");
			writeString(out, language);
			writeString(out, literal.getBaseDirection().toString());
			return;
		}
		if (value instanceof TripleTerm triple) {
			out.writeByte(VALUE_TRIPLE);
			writeValue(out, triple.getSubject());
			writeValue(out, triple.getPredicate());
			writeValue(out, triple.getObject());
			return;
		}
		throw new IOException("Unsupported value type for backup delta: " + value.getClass().getName());
	}

	private static Value readValue(DataInputStream in, ValueFactory vf) throws IOException {
		byte kind = in.readByte();
		return switch (kind) {
		case VALUE_IRI -> vf.createIRI(readString(in));
		case VALUE_BNODE -> vf.createBNode(readString(in));
		case VALUE_LITERAL -> {
			String label = readString(in);
			String datatype = readString(in);
			String language = readString(in);
			Literal.BaseDirection direction = Literal.BaseDirection.fromString(readString(in));
			if (language.isEmpty()) {
				yield vf.createLiteral(label, vf.createIRI(datatype));
			}
			if (direction == Literal.BaseDirection.NONE) {
				yield vf.createLiteral(label, language);
			}
			yield vf.createLiteral(label, language, direction);
		}
		case VALUE_TRIPLE -> vf.createTripleTerm(toResource(readValue(in, vf)), toIRI(readValue(in, vf)),
				readValue(in, vf));
		default -> throw new IOException("Unsupported value marker in backup delta: " + kind);
		};
	}

	private static void writeString(DataOutputStream out, String value) throws IOException {
		ByteBuffer encoded = StandardCharsets.UTF_8.encode(CharBuffer.wrap(value));
		out.writeInt(encoded.remaining());
		out.write(encoded.array(), 0, encoded.remaining());
	}

	private static String readString(DataInputStream in) throws IOException {
		int length = in.readInt();
		if (length < 0) {
			throw new IOException("Negative string length in backup delta: " + length);
		}
		byte[] bytes = new byte[length];
		in.readFully(bytes);
		return new String(bytes, StandardCharsets.UTF_8);
	}

	private static Resource toResource(Value value) throws IOException {
		if (value instanceof Resource resource) {
			return resource;
		}
		throw new IOException("Expected resource value but found: " + value.getClass().getName());
	}

	private static IRI toIRI(Value value) throws IOException {
		if (value instanceof IRI iri) {
			return iri;
		}
		throw new IOException("Expected IRI value but found: " + value.getClass().getName());
	}

	enum NamespaceOperation {
		SET,
		REMOVE,
		CLEAR
	}

	static final class Record {
		private final boolean addition;
		private final boolean explicit;
		private final Statement statement;
		private final NamespaceOperation namespaceOperation;
		private final String namespacePrefix;
		private final String namespaceName;

		Record(boolean addition, boolean explicit, Statement statement) {
			this.addition = addition;
			this.explicit = explicit;
			this.statement = statement;
			this.namespaceOperation = null;
			this.namespacePrefix = null;
			this.namespaceName = null;
		}

		Record(NamespaceOperation namespaceOperation, String namespacePrefix, String namespaceName) {
			this.addition = false;
			this.explicit = false;
			this.statement = null;
			this.namespaceOperation = namespaceOperation;
			this.namespacePrefix = namespacePrefix;
			this.namespaceName = namespaceName;
		}

		boolean isStatementOperation() {
			return statement != null;
		}

		boolean isAddition() {
			return addition;
		}

		boolean isExplicit() {
			return explicit;
		}

		Statement getStatement() {
			return statement;
		}

		boolean isNamespaceOperation() {
			return namespaceOperation != null;
		}

		NamespaceOperation getNamespaceOperation() {
			return namespaceOperation;
		}

		String getNamespacePrefix() {
			return namespacePrefix;
		}

		String getNamespaceName() {
			return namespaceName;
		}
	}
}
