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

package org.eclipse.rdf4j.rio.binary;

import static org.eclipse.rdf4j.common.io.IOUtil.writeVarInt;
import static org.eclipse.rdf4j.rio.binary.BinaryRDFConstants.BNODE_VALUE;
import static org.eclipse.rdf4j.rio.binary.BinaryRDFConstants.COMMENT;
import static org.eclipse.rdf4j.rio.binary.BinaryRDFConstants.DATATYPE_LITERAL_VALUE;
import static org.eclipse.rdf4j.rio.binary.BinaryRDFConstants.END_OF_DATA;
import static org.eclipse.rdf4j.rio.binary.BinaryRDFConstants.FORMAT_V1;
import static org.eclipse.rdf4j.rio.binary.BinaryRDFConstants.FORMAT_V2;
import static org.eclipse.rdf4j.rio.binary.BinaryRDFConstants.LANG_LITERAL_VALUE;
import static org.eclipse.rdf4j.rio.binary.BinaryRDFConstants.MAGIC_NUMBER;
import static org.eclipse.rdf4j.rio.binary.BinaryRDFConstants.NAMESPACE_DECL;
import static org.eclipse.rdf4j.rio.binary.BinaryRDFConstants.NULL_VALUE;
import static org.eclipse.rdf4j.rio.binary.BinaryRDFConstants.PLAIN_LITERAL_VALUE;
import static org.eclipse.rdf4j.rio.binary.BinaryRDFConstants.STATEMENT;
import static org.eclipse.rdf4j.rio.binary.BinaryRDFConstants.TRIPLE_VALUE;
import static org.eclipse.rdf4j.rio.binary.BinaryRDFConstants.URI_VALUE;
import static org.eclipse.rdf4j.rio.binary.BinaryRDFConstants.VALUE_REF;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import org.eclipse.rdf4j.common.io.ByteSink;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFWriter;
import org.eclipse.rdf4j.rio.helpers.BinaryRDFWriterSettings;

/**
 * A {@link RDFWriter} for the binary RDF format.
 *
 * @author Arjohn Kampman
 * @author Frens Jan Rumph
 */
public class BinaryRDFWriter extends AbstractRDFWriter implements ByteSink {

	private final Queue<Statement> statementQueue;

	private int bufferSize;

	private final Map<Value, ValueMeta> valueMeta;

	private int nextId = 0;

	private final Queue<Integer> idPool;

	private final DataOutputStream out;

	private int formatVersion;
	private Charset charset;
	private boolean recycleIds;

	public BinaryRDFWriter(OutputStream out) {
		this(out, 8192);
	}

	public BinaryRDFWriter(OutputStream out, int bufferSize) {
		this.out = new DataOutputStream(new BufferedOutputStream(out));
		this.statementQueue = new ArrayDeque<>(bufferSize);
		this.valueMeta = new HashMap<>(bufferSize * 3);
		this.idPool = new ArrayDeque<>(bufferSize);
		this.bufferSize = bufferSize;
	}

	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.BINARY;
	}

	@Override
	public Collection<RioSetting<?>> getSupportedSettings() {
		Set<RioSetting<?>> result = new HashSet<>(super.getSupportedSettings());
		result.add(BinaryRDFWriterSettings.VERSION);
		result.add(BinaryRDFWriterSettings.BUFFER_SIZE);
		result.add(BinaryRDFWriterSettings.CHARSET);
		result.add(BinaryRDFWriterSettings.RECYCLE_IDS);
		return result;
	}

	@Override
	public OutputStream getOutputStream() {
		return out;
	}

	@Override
	public void startRDF() throws RDFHandlerException {
		super.startRDF();

		handleWriterConfig();

		try {
			out.write(MAGIC_NUMBER);
			out.writeInt(formatVersion);

			if (formatVersion != FORMAT_V1) {
				byte[] charsetBytes = charset.toString().getBytes(charset);
				writeInt(charsetBytes.length);
				out.write(charsetBytes);
			}
		} catch (IOException e) {
			throw new RDFHandlerException(e);
		}
	}

	private void handleWriterConfig() {
		WriterConfig config = getWriterConfig();

		formatVersion = Math.toIntExact(config.get(BinaryRDFWriterSettings.VERSION));
		if (formatVersion == FORMAT_V1) {
			charset = StandardCharsets.UTF_16BE;
		} else if (formatVersion == FORMAT_V2) {
			charset = Charset.forName(config.get(BinaryRDFWriterSettings.CHARSET));
		} else {
			throw new IllegalArgumentException("Unsupported binary RDF version: " + formatVersion);
		}

		if (config.isSet(BinaryRDFWriterSettings.BUFFER_SIZE)) {
			bufferSize = Math.toIntExact(config.get(BinaryRDFWriterSettings.BUFFER_SIZE));
		}

		recycleIds = config.get(BinaryRDFWriterSettings.RECYCLE_IDS);
	}

	@Override
	public void endRDF() throws RDFHandlerException {
		checkWritingStarted();
		try {
			while (!statementQueue.isEmpty()) {
				writeStatement();
			}
			out.writeByte(END_OF_DATA);
			out.flush();
		} catch (IOException e) {
			throw new RDFHandlerException(e);
		}
	}

	@Override
	public void handleNamespace(String prefix, String uri) throws RDFHandlerException {
		checkWritingStarted();
		try {
			out.writeByte(NAMESPACE_DECL);
			writeString(prefix);
			writeString(uri);
		} catch (IOException e) {
			throw new RDFHandlerException(e);
		}
	}

	@Override
	public void handleComment(String comment) throws RDFHandlerException {
		checkWritingStarted();
		try {
			out.writeByte(COMMENT);
			writeString(comment);
		} catch (IOException e) {
			throw new RDFHandlerException(e);
		}
	}

	@Override
	protected void consumeStatement(Statement st) {
		statementQueue.add(st);
		incValueFreq(st.getSubject());
		incValueFreq(st.getPredicate());
		incValueFreq(st.getObject());
		incValueFreq(st.getContext());

		if (statementQueue.size() < bufferSize) {
			// postpone statement writing until queue is filled
			return;
		}

		// Process the first statement from the queue
		try {
			writeStatement();
		} catch (IOException e) {
			throw new RDFHandlerException(e);
		}
	}

	/** Writes the first statement from the statement queue */
	private void writeStatement() throws RDFHandlerException, IOException {
		Statement st = statementQueue.remove();

		out.writeByte(STATEMENT);
		writeValueOrId(st.getSubject());
		writeValueOrId(st.getPredicate());
		writeValueOrId(st.getObject());
		writeValueOrId(st.getContext());
	}

	private void incValueFreq(Value v) {
		if (v == null) {
			return;
		}

		ValueMeta meta = valueMeta.get(v);
		if (meta == null) {
			valueMeta.put(v, new ValueMeta(1));
		} else {
			meta.frequency++;
			if (meta.frequency == 2 && !meta.hasId()) {
				assignId(v, meta);
			}
		}
	}

	private void assignId(Value v, ValueMeta meta) {
		Integer id = idPool.poll();
		if (id == null) {
			id = nextId++; // get then increment
		}

		meta.id = id;

		try {
			out.writeByte(BinaryRDFConstants.VALUE_DECL);
			writeInt(id);
			writeValue(v);
		} catch (IOException e) {
			throw new RDFHandlerException(e);
		}
	}

	private void writeValueOrId(Value value) throws RDFHandlerException, IOException {
		if (value == null) {
			out.writeByte(NULL_VALUE);
		} else {
			ValueMeta meta = valueMeta.get(value);

			if (meta.hasId()) {
				out.writeByte(VALUE_REF);
				writeInt(meta.id);
			} else {
				writeValue(value);
			}

			meta.frequency--;

			if (meta.frequency == 0) {
				if (!meta.hasId()) {
					valueMeta.remove(value);
				} else if (recycleIds) {
					valueMeta.remove(value);
					idPool.add(meta.id);
				}
				// else keep value and id
			}
		}
	}

	private void writeValue(Value value) throws RDFHandlerException, IOException {
		if (value instanceof IRI) {
			writeURI((IRI) value);
		} else if (value instanceof BNode) {
			writeBNode((BNode) value);
		} else if (value instanceof Literal) {
			writeLiteral((Literal) value);
		} else if (value instanceof Triple) {
			writeTriple((Triple) value);
		} else {
			throw new RDFHandlerException("Unknown Value object type: " + value.getClass());
		}
	}

	private void writeURI(IRI uri) throws IOException {
		out.writeByte(URI_VALUE);
		writeString(uri.toString());
	}

	private void writeBNode(BNode bnode) throws IOException {
		out.writeByte(BNODE_VALUE);
		writeString(bnode.getID());
	}

	private void writeLiteral(Literal literal) throws IOException {
		String label = literal.getLabel();
		IRI datatype = literal.getDatatype();
		Optional<String> language = literal.getLanguage();

		if (language.isPresent()) {
			out.writeByte(LANG_LITERAL_VALUE);
			writeString(label);
			writeString(language.get());
		} else if (datatype.equals(XSD.STRING)) {
			out.writeByte(PLAIN_LITERAL_VALUE);
			writeString(label);
		} else {
			out.writeByte(DATATYPE_LITERAL_VALUE);
			writeString(label);
			writeString(datatype.toString());
		}
	}

	private void writeTriple(Triple triple) throws IOException {
		out.writeByte(TRIPLE_VALUE);
		writeValue(triple.getSubject());
		writeValue(triple.getPredicate());
		writeValue(triple.getObject());
	}

	private void writeString(String s) throws IOException {
		byte[] bytes = s.getBytes(charset);

		if (formatVersion == FORMAT_V1) {
			writeInt(s.length());
		} else {
			writeInt(bytes.length);
		}

		out.write(bytes);
	}

	private void writeInt(int i) throws IOException {
		if (formatVersion == FORMAT_V1) {
			out.writeInt(i);
		} else {
			writeVarInt(out, i);
		}
	}

	/**
	 * Holds the frequency of a value within the current {@link #statementQueue} as well as an identifier if any has
	 * been assigned.
	 */
	private static class ValueMeta {

		private long frequency;
		private int id = -1;

		public ValueMeta(long frequency) {
			this.frequency = frequency;
		}

		private boolean hasId() {
			return id != -1;
		}
	}

}
