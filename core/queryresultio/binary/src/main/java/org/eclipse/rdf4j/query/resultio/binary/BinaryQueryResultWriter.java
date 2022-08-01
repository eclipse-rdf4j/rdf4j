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
package org.eclipse.rdf4j.query.resultio.binary;

import static org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultConstants.BNODE_RECORD_MARKER;
import static org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultConstants.DATATYPE_LITERAL_RECORD_MARKER;
import static org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultConstants.EMPTY_ROW_RECORD_MARKER;
import static org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultConstants.ERROR_RECORD_MARKER;
import static org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultConstants.FORMAT_VERSION;
import static org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultConstants.LANG_LITERAL_RECORD_MARKER;
import static org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultConstants.MAGIC_NUMBER;
import static org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultConstants.MALFORMED_QUERY_ERROR;
import static org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultConstants.NAMESPACE_RECORD_MARKER;
import static org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultConstants.NULL_RECORD_MARKER;
import static org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultConstants.PLAIN_LITERAL_RECORD_MARKER;
import static org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultConstants.QNAME_RECORD_MARKER;
import static org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultConstants.QUERY_EVALUATION_ERROR;
import static org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultConstants.REPEAT_RECORD_MARKER;
import static org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultConstants.TABLE_END_RECORD_MARKER;
import static org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultConstants.TRIPLE_RECORD_MARKER;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.common.io.ByteSink;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.impl.ListBindingSet;
import org.eclipse.rdf4j.query.resultio.AbstractQueryResultWriter;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriter;
import org.eclipse.rdf4j.rio.RioSetting;

/**
 * Writer for the binary tuple result format. The format is explained in {@link BinaryQueryResultConstants}.
 *
 * @author Arjohn Kampman
 */
public class BinaryQueryResultWriter extends AbstractQueryResultWriter implements TupleQueryResultWriter, ByteSink {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The output stream to write the results table to.
	 */
	private final DataOutputStream out;

	private final CharsetEncoder charsetEncoder = StandardCharsets.UTF_8.newEncoder();

	/**
	 * Map containing the namespace IDs (Integer objects) that have been defined in the document, stored using the
	 * concerning namespace (Strings).
	 */
	private final Map<String, Integer> namespaceTable = new HashMap<>(32);

	private int nextNamespaceID;

	private BindingSet previousBindings;

	private List<String> bindingNames;

	private boolean documentStarted = false;

	protected boolean tupleVariablesFound = false;

	public BinaryQueryResultWriter(OutputStream out) {
		this.out = new DataOutputStream(out);
	}

	@Override
	public OutputStream getOutputStream() {
		return out;
	}

	public final TupleQueryResultFormat getTupleQueryResultFormat() {
		return TupleQueryResultFormat.BINARY;
	}

	@Override
	public final TupleQueryResultFormat getQueryResultFormat() {
		return getTupleQueryResultFormat();
	}

	@Override
	public Collection<RioSetting<?>> getSupportedSettings() {
		return Collections.emptyList();
	}

	@Override
	public void startDocument() throws TupleQueryResultHandlerException {
		documentStarted = true;
		try {
			out.write(MAGIC_NUMBER);
			out.writeInt(FORMAT_VERSION);
		} catch (IOException e) {
			throw new TupleQueryResultHandlerException(e);
		}
	}

	@Override
	public void startQueryResult(List<String> bindingNames) throws TupleQueryResultHandlerException {
		super.startQueryResult(bindingNames);

		tupleVariablesFound = true;

		if (!documentStarted) {
			startDocument();
		}

		// Copy supplied column headers list and make it unmodifiable
		bindingNames = new ArrayList<>(bindingNames);
		this.bindingNames = Collections.unmodifiableList(bindingNames);

		try {
			out.writeInt(this.bindingNames.size());

			for (String bindingName : this.bindingNames) {
				writeString(bindingName);
			}

			List<Value> nullTuple = Collections.nCopies(this.bindingNames.size(), (Value) null);
			previousBindings = new ListBindingSet(this.bindingNames, nullTuple);
			nextNamespaceID = 0;
		} catch (IOException e) {
			throw new TupleQueryResultHandlerException(e);
		}
	}

	@Override
	public void endQueryResult() throws TupleQueryResultHandlerException {
		if (!tupleVariablesFound) {
			throw new IllegalStateException("Could not end query result as startQueryResult was not called first.");
		}

		try {
			out.writeByte(TABLE_END_RECORD_MARKER);
			endDocument();
		} catch (IOException e) {
			throw new TupleQueryResultHandlerException(e);
		}
	}

	@Override
	protected void handleSolutionImpl(BindingSet bindingSet) throws TupleQueryResultHandlerException {
		if (!tupleVariablesFound) {
			throw new IllegalStateException("Must call startQueryResult before handleSolution");
		}

		try {
			if (bindingSet.size() == 0) {
				writeEmptyRow();
			} else {
				for (String bindingName : bindingNames) {
					Value value = bindingSet.getValue(bindingName);

					if (value == null) {
						writeNull();
					} else if (value.equals(previousBindings.getValue(bindingName))) {
						writeRepeat();
					} else {
						writeValue(value);
					}
				}

				previousBindings = bindingSet;
			}
		} catch (IOException e) {
			throw new TupleQueryResultHandlerException(e);
		}
	}

	private void writeNull() throws IOException {
		out.writeByte(NULL_RECORD_MARKER);
	}

	private void writeRepeat() throws IOException {
		out.writeByte(REPEAT_RECORD_MARKER);
	}

	private void writeValue(Value value) throws IOException {
		if (value instanceof IRI) {
			writeQName((IRI) value);
		} else if (value instanceof BNode) {
			writeBNode((BNode) value);
		} else if (value instanceof Literal) {
			writeLiteral((Literal) value);
		} else if (value instanceof Triple) {
			writeTriple((Triple) value);
		} else {
			throw new TupleQueryResultHandlerException("Unknown Value object type: " + value.getClass());
		}
	}

	private void writeEmptyRow() throws IOException {
		out.writeByte(EMPTY_ROW_RECORD_MARKER);
	}

	@Override
	public void handleNamespace(String prefix, String uri) throws QueryResultHandlerException {
		// Binary format does not support explicit setting of namespace prefixes.
	}

	private void writeQName(IRI uri) throws IOException {
		// Check if the URI has a new namespace
		String namespace = uri.getNamespace();

		Integer nsID = namespaceTable.get(namespace);

		if (nsID == null) {
			// Generate a ID for this new namespace
			nsID = writeNamespace(namespace);
		}

		out.writeByte(QNAME_RECORD_MARKER);
		out.writeInt(nsID.intValue());
		writeString(uri.getLocalName());
	}

	private void writeBNode(BNode bnode) throws IOException {
		out.writeByte(BNODE_RECORD_MARKER);
		writeString(bnode.getID());
	}

	private void writeLiteral(Literal literal) throws IOException {
		String label = literal.getLabel();
		IRI datatype = literal.getDatatype();

		int marker;

		if (Literals.isLanguageLiteral(literal)) {
			marker = LANG_LITERAL_RECORD_MARKER;
		} else {
			String namespace = datatype.getNamespace();

			if (!namespaceTable.containsKey(namespace)) {
				// Assign an ID to this new namespace
				writeNamespace(namespace);
			}

			marker = DATATYPE_LITERAL_RECORD_MARKER;
		}

		out.writeByte(marker);
		writeString(label);

		if (Literals.isLanguageLiteral(literal)) {
			writeString(literal.getLanguage().get());
		} else {
			writeQName(datatype);
		}
	}

	private void writeTriple(Triple triple) throws IOException {
		out.writeByte(TRIPLE_RECORD_MARKER);
		writeValue(triple.getSubject());
		writeValue(triple.getPredicate());
		writeValue(triple.getObject());
	}

	/**
	 * Writes an error msg to the stream.
	 *
	 * @param errType The error type.
	 * @param msg     The error message.
	 * @throws IOException When the error could not be written to the stream.
	 */
	public void error(QueryErrorType errType, String msg) throws IOException {
		out.writeByte(ERROR_RECORD_MARKER);

		if (errType == QueryErrorType.MALFORMED_QUERY_ERROR) {
			out.writeByte(MALFORMED_QUERY_ERROR);
		} else {
			out.writeByte(QUERY_EVALUATION_ERROR);
		}

		writeString(msg);
	}

	private Integer writeNamespace(String namespace) throws IOException {
		out.writeByte(NAMESPACE_RECORD_MARKER);
		out.writeInt(nextNamespaceID);
		writeString(namespace);

		Integer result = new Integer(nextNamespaceID);
		namespaceTable.put(namespace, result);

		nextNamespaceID++;

		return result;
	}

	private void writeString(String s) throws IOException {
		ByteBuffer byteBuf = charsetEncoder.encode(CharBuffer.wrap(s));
		out.writeInt(byteBuf.remaining());
		out.write(byteBuf.array(), 0, byteBuf.remaining());
	}

	@Override
	public void handleStylesheet(String stylesheetUrl) throws QueryResultHandlerException {
		// Ignored by Binary Query Results format
	}

	@Override
	public void startHeader() throws QueryResultHandlerException {
		// Ignored by Binary Query Results format
	}

	@Override
	public void handleLinks(List<String> linkUrls) throws QueryResultHandlerException {
		// Ignored by Binary Query Results format
	}

	@Override
	public void endHeader() throws QueryResultHandlerException {
		// Ignored by Binary Query Results format
	}

	private void endDocument() throws IOException {
		out.flush();
		documentStarted = false;
	}

	@Override
	public void handleBoolean(boolean value) throws QueryResultHandlerException {
		throw new UnsupportedOperationException("Cannot handle boolean results");
	}
}
