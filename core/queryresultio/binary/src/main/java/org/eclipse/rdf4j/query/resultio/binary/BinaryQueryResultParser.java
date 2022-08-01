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
import static org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultConstants.URI_RECORD_MARKER;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.impl.ListBindingSet;
import org.eclipse.rdf4j.query.resultio.AbstractTupleQueryResultParser;
import org.eclipse.rdf4j.query.resultio.QueryResultParseException;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;

/**
 * Reader for the binary tuple result format. The format is explained in {@link BinaryQueryResultConstants}.
 */
public class BinaryQueryResultParser extends AbstractTupleQueryResultParser {

	/*-----------*
	 * Variables *
	 *-----------*/

	private DataInputStream in;

	private int formatVersion;

	private final CharsetDecoder charsetDecoder = StandardCharsets.UTF_8.newDecoder();

	private String[] namespaceArray = new String[32];

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new parser for the binary query result format that will use an instance of {@link SimpleValueFactory}
	 * to create Value objects.
	 */
	public BinaryQueryResultParser() {
		super();
	}

	/**
	 * Creates a new parser for the binary query result format that will use the supplied ValueFactory to create Value
	 * objects.
	 */
	public BinaryQueryResultParser(ValueFactory valueFactory) {
		super(valueFactory);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public final TupleQueryResultFormat getTupleQueryResultFormat() {
		return TupleQueryResultFormat.BINARY;
	}

	@Override
	public synchronized void parse(InputStream in)
			throws IOException, QueryResultParseException, TupleQueryResultHandlerException {
		if (in == null) {
			throw new IllegalArgumentException("Input stream can not be 'null'");
		}

		this.in = new DataInputStream(in);

		// Check magic number
		byte[] magicNumber = IOUtil.readBytes(in, MAGIC_NUMBER.length);
		if (!Arrays.equals(magicNumber, MAGIC_NUMBER)) {
			throw new QueryResultParseException("File does not contain a binary RDF table result");
		}

		// Check format version (parser is backward-compatible with version 1 and
		// version 2)
		formatVersion = this.in.readInt();
		if (formatVersion > FORMAT_VERSION && formatVersion < 1) {
			throw new QueryResultParseException("Incompatible format version: " + formatVersion);
		}

		if (formatVersion == 2) {
			// read format version 2 FLAG byte (ordered and distinct flags) and
			// ignore them
			this.in.readByte();
		}

		// Read column headers
		int columnCount = this.in.readInt();
		if (columnCount < 0) {
			throw new QueryResultParseException("Illegal column count specified: " + columnCount);
		}

		List<String> columnHeaders = new ArrayList<>(columnCount);
		for (int i = 0; i < columnCount; i++) {
			columnHeaders.add(readString());
		}
		columnHeaders = Collections.unmodifiableList(columnHeaders);

		if (handler != null) {
			handler.startQueryResult(columnHeaders);
		}

		// Read value tuples
		List<Value> currentTuple = new ArrayList<>(columnCount);
		List<Value> previousTuple = Collections.nCopies(columnCount, (Value) null);

		int recordTypeMarker = this.in.readByte();

		while (recordTypeMarker != TABLE_END_RECORD_MARKER) {
			if (recordTypeMarker == ERROR_RECORD_MARKER) {
				processError();
			} else if (recordTypeMarker == NAMESPACE_RECORD_MARKER) {
				processNamespace();
			} else if (recordTypeMarker == EMPTY_ROW_RECORD_MARKER) {
				if (handler != null) {
					handler.handleSolution(EmptyBindingSet.getInstance());
				}
			} else {
				Value value = null;
				switch (recordTypeMarker) {
				case NULL_RECORD_MARKER:
					break; // do nothing
				case REPEAT_RECORD_MARKER:
					value = previousTuple.get(currentTuple.size());
					break;
				case QNAME_RECORD_MARKER:
					value = readQName();
					break;
				case URI_RECORD_MARKER:
					value = readURI();
					break;
				case BNODE_RECORD_MARKER:
					value = readBnode();
					break;
				case PLAIN_LITERAL_RECORD_MARKER:
				case LANG_LITERAL_RECORD_MARKER:
				case DATATYPE_LITERAL_RECORD_MARKER:
					value = readLiteral(recordTypeMarker);
					break;
				case TRIPLE_RECORD_MARKER:
					value = readTriple();
					break;
				default:
					throw new IOException("Unkown record type: " + recordTypeMarker);
				}

				currentTuple.add(value);

				if (currentTuple.size() == columnCount) {
					previousTuple = Collections.unmodifiableList(currentTuple);
					currentTuple = new ArrayList<>(columnCount);

					if (handler != null) {
						handler.handleSolution(new ListBindingSet(columnHeaders, previousTuple));
					}
				}
			}

			recordTypeMarker = this.in.readByte();
		}

		if (handler != null) {
			handler.endQueryResult();
		}
	}

	private void processError() throws IOException, QueryResultParseException {
		byte errTypeFlag = in.readByte();

		QueryErrorType errType;
		if (errTypeFlag == MALFORMED_QUERY_ERROR) {
			errType = QueryErrorType.MALFORMED_QUERY_ERROR;
		} else if (errTypeFlag == QUERY_EVALUATION_ERROR) {
			errType = QueryErrorType.QUERY_EVALUATION_ERROR;
		} else {
			throw new QueryResultParseException("Unkown error type: " + errTypeFlag);
		}

		String msg = readString();

		// FIXME: is this the right thing to do upon encountering an error?
		throw new QueryResultParseException(errType + ": " + msg);
	}

	private void processNamespace() throws IOException {
		int namespaceID = in.readInt();
		String namespace = readString();

		if (namespaceID >= namespaceArray.length) {
			int newSize = Math.max(namespaceID, namespaceArray.length * 2);
			String[] newArray = new String[newSize];
			System.arraycopy(namespaceArray, 0, newArray, 0, namespaceArray.length);
			namespaceArray = newArray;
		}

		namespaceArray[namespaceID] = namespace;
	}

	private IRI readQName() throws IOException {
		int nsID = in.readInt();
		String localName = readString();

		return valueFactory.createIRI(namespaceArray[nsID], localName);
	}

	private IRI readURI() throws IOException {
		String uri = readString();

		return valueFactory.createIRI(uri);
	}

	private BNode readBnode() throws IOException {
		String bnodeID = readString();
		return valueFactory.createBNode(bnodeID);
	}

	private Literal readLiteral(int recordTypeMarker) throws IOException, QueryResultParseException {
		String label = readString();

		if (recordTypeMarker == DATATYPE_LITERAL_RECORD_MARKER) {
			IRI datatype;

			int dtTypeMarker = in.readByte();
			switch (dtTypeMarker) {
			case QNAME_RECORD_MARKER:
				datatype = readQName();
				break;
			case URI_RECORD_MARKER:
				datatype = readURI();
				break;
			default:
				throw new QueryResultParseException("Illegal record type marker for literal's datatype");
			}

			return valueFactory.createLiteral(label, datatype);
		} else if (recordTypeMarker == LANG_LITERAL_RECORD_MARKER) {
			String language = readString();
			return valueFactory.createLiteral(label, language);
		} else {
			return valueFactory.createLiteral(label);
		}
	}

	private String readString() throws IOException {
		if (formatVersion == 1) {
			return readStringV1();
		} else {
			return readStringV2();
		}
	}

	/**
	 * Reads a string from the version 1 format, i.e. in Java's {@link DataInput#modified-utf-8 Modified UTF-8}.
	 */
	private String readStringV1() throws IOException {
		return in.readUTF();
	}

	/**
	 * Reads a string from the version 2 format. Strings are encoded as UTF-8 and are preceeded by a 32-bit integer
	 * (high byte first) specifying the length of the encoded string.
	 */
	private String readStringV2() throws IOException {
		int stringLength = in.readInt();
		byte[] encodedString = IOUtil.readBytes(in, stringLength);

		if (encodedString.length != stringLength) {
			throw new EOFException("Attempted to read " + stringLength + " bytes but no more than "
					+ encodedString.length + " were available");
		}

		ByteBuffer byteBuf = ByteBuffer.wrap(encodedString);
		CharBuffer charBuf = charsetDecoder.decode(byteBuf);

		return charBuf.toString();
	}

	private Triple readTriple() throws IOException {
		Value subject = readDirectValue();
		if (!(subject instanceof Resource)) {
			throw new IOException("Unexpected value type: " + subject);
		}

		Value predicate = readDirectValue();
		if (!(predicate instanceof IRI)) {
			throw new IOException("Unexpected value type: " + predicate);
		}

		Value object = readDirectValue();

		return valueFactory.createTriple((Resource) subject, (IRI) predicate, object);
	}

	private Value readDirectValue() throws IOException {
		int recordTypeMarker = this.in.readByte();

		switch (recordTypeMarker) {
		case NAMESPACE_RECORD_MARKER:
			processNamespace();
			return readDirectValue();
		case QNAME_RECORD_MARKER:
			return readQName();
		case URI_RECORD_MARKER:
			return readURI();
		case BNODE_RECORD_MARKER:
			return readBnode();
		case PLAIN_LITERAL_RECORD_MARKER:
		case LANG_LITERAL_RECORD_MARKER:
		case DATATYPE_LITERAL_RECORD_MARKER:
			return readLiteral(recordTypeMarker);
		case TRIPLE_RECORD_MARKER:
			return readTriple();
		default:
			throw new IOException("Unexpected record type: " + recordTypeMarker);
		}
	}
}
