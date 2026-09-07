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

	private static final byte OP_ADD = 1;
	private static final byte OP_REMOVE = 2;

	private static final byte VALUE_IRI = 1;
	private static final byte VALUE_BNODE = 2;
	private static final byte VALUE_LITERAL = 3;
	private static final byte VALUE_TRIPLE = 4;

	private LmdbBackupDeltaCodec() {
	}

	static void write(OutputStream outputStream, List<Statement> additions, List<Statement> removals)
			throws IOException {
		try (DataOutputStream out = new DataOutputStream(outputStream)) {
			out.writeInt(MAGIC.length);
			out.write(MAGIC);
			out.writeInt(additions.size());
			for (Statement statement : additions) {
				out.writeByte(OP_ADD);
				writeStatement(out, statement);
			}
			out.writeInt(removals.size());
			for (Statement statement : removals) {
				out.writeByte(OP_REMOVE);
				writeStatement(out, statement);
			}
		}
	}

	static List<Record> read(InputStream inputStream) throws IOException {
		ValueFactory vf = SimpleValueFactory.getInstance();
		try (DataInputStream in = new DataInputStream(inputStream)) {
			byte[] magic = new byte[in.readInt()];
			in.readFully(magic);
			if (!java.util.Arrays.equals(MAGIC, magic)) {
				throw new IOException("Unexpected LMDB delta log header");
			}
			int additions = in.readInt();
			List<Record> records = new ArrayList<>(additions);
			for (int i = 0; i < additions; i++) {
				if (in.readByte() != OP_ADD) {
					throw new IOException("Unexpected operation marker in additions segment");
				}
				records.add(new Record(true, readStatement(in, vf)));
			}
			int removals = in.readInt();
			for (int i = 0; i < removals; i++) {
				if (in.readByte() != OP_REMOVE) {
					throw new IOException("Unexpected operation marker in removals segment");
				}
				records.add(new Record(false, readStatement(in, vf)));
			}
			return records;
		}
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
			out.writeUTF(iri.stringValue());
			return;
		}
		if (value instanceof BNode bNode) {
			out.writeByte(VALUE_BNODE);
			out.writeUTF(bNode.getID());
			return;
		}
		if (value instanceof Literal literal) {
			out.writeByte(VALUE_LITERAL);
			out.writeUTF(literal.getLabel());
			out.writeUTF(literal.getDatatype().stringValue());
			String language = literal.getLanguage().orElse("");
			out.writeUTF(language);
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
		case VALUE_IRI -> vf.createIRI(in.readUTF());
		case VALUE_BNODE -> vf.createBNode(in.readUTF());
		case VALUE_LITERAL -> {
			String label = in.readUTF();
			String datatype = in.readUTF();
			String language = in.readUTF();
			if (language.isEmpty()) {
				yield vf.createLiteral(label, vf.createIRI(datatype));
			}
			yield vf.createLiteral(label, language);
		}
		case VALUE_TRIPLE -> vf.createTripleTerm(toResource(readValue(in, vf)), toIRI(readValue(in, vf)),
				readValue(in, vf));
		default -> throw new IOException("Unsupported value marker in backup delta: " + kind);
		};
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

	static final class Record {
		private final boolean addition;
		private final Statement statement;

		Record(boolean addition, Statement statement) {
			this.addition = addition;
			this.statement = statement;
		}

		boolean isAddition() {
			return addition;
		}

		Statement getStatement() {
			return statement;
		}
	}
}
