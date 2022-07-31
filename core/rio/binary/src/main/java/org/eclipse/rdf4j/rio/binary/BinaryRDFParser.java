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

import static org.eclipse.rdf4j.common.io.IOUtil.readVarInt;
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
import static org.eclipse.rdf4j.rio.binary.BinaryRDFConstants.V1_STRING_CHARSET;
import static org.eclipse.rdf4j.rio.binary.BinaryRDFConstants.VALUE_DECL;
import static org.eclipse.rdf4j.rio.binary.BinaryRDFConstants.VALUE_REF;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFParser;

/**
 * @author Arjohn Kampman
 * @author Frens Jan Rumph
 */
public class BinaryRDFParser extends AbstractRDFParser {

	private Value[] declaredValues = new Value[16];

	private DataInputStream in;
	private int formatVersion;
	private Charset charset = StandardCharsets.UTF_8;

	private byte[] buf = new byte[1024];

	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.BINARY;
	}

	@Override
	public void parse(Reader reader, String baseURI) throws IOException, RDFParseException, RDFHandlerException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void parse(InputStream in, String baseURI) throws IOException, RDFParseException, RDFHandlerException {
		clear();

		try {
			if (in == null) {
				throw new IllegalArgumentException("Input stream must not be null");
			}

			this.in = new DataInputStream(new BufferedInputStream(in));

			// Check magic number
			byte[] magicNumber = IOUtil.readBytes(in, MAGIC_NUMBER.length);
			if (!Arrays.equals(magicNumber, MAGIC_NUMBER)) {
				reportFatalError("File does not contain a binary RDF document");
			}

			formatVersion = this.in.readInt();

			// Check format version (parser is backward-compatible with version 1 and version 2)
			if (formatVersion == FORMAT_V1) {
			} else if (formatVersion == FORMAT_V2) {
				charset = Charset.forName(readString());
			} else {
				reportFatalError("Incompatible format version: " + formatVersion);
			}

			if (rdfHandler != null) {
				rdfHandler.startRDF();
			}

			loop: while (true) {
				int recordType = this.in.readByte();

				switch (recordType) {
				case END_OF_DATA:
					break loop;
				case STATEMENT:
					readStatement();
					break;
				case VALUE_DECL:
					readValueDecl();
					break;
				case NAMESPACE_DECL:
					readNamespaceDecl();
					break;
				case COMMENT:
					readComment();
					break;
				default:
					reportFatalError("Invalid record type: " + recordType);
				}
			}
		} finally {
			clear();
		}

		if (rdfHandler != null) {
			rdfHandler.endRDF();
		}
	}

	private void readNamespaceDecl() throws IOException, RDFHandlerException {
		String prefix = readString();
		String namespace = readString();
		if (rdfHandler != null) {
			rdfHandler.handleNamespace(prefix, namespace);
		}
	}

	private void readComment() throws IOException, RDFHandlerException {
		String comment = readString();
		if (rdfHandler != null) {
			rdfHandler.handleComment(comment);
		}
	}

	private void readValueDecl() throws IOException, RDFParseException {
		int id = readId();
		Value v = readValue();

		if (id >= declaredValues.length) {
			// grow array
			Value[] newArray = new Value[2 * declaredValues.length];
			System.arraycopy(declaredValues, 0, newArray, 0, declaredValues.length);
			declaredValues = newArray;
		}

		declaredValues[id] = v;
	}

	private void readStatement() throws RDFParseException, IOException, RDFHandlerException {
		Value v = readValue();
		Resource subj = null;
		if (v instanceof Resource) {
			subj = (Resource) v;
		} else {
			reportFatalError("Invalid subject type: " + v);
		}

		v = readValue();
		IRI pred = null;
		if (v instanceof IRI) {
			pred = (IRI) v;
		} else {
			reportFatalError("Invalid predicate type: " + v);
		}

		Value obj = readValue();
		if (obj == null) {
			reportFatalError("Invalid object type: null");
		}

		v = readValue();
		Resource context = null;
		if (v == null || v instanceof Resource) {
			context = (Resource) v;
		} else {
			reportFatalError("Invalid context type: " + v);
		}

		Statement st = createStatement(subj, pred, obj, context);
		if (rdfHandler != null) {
			rdfHandler.handleStatement(st);
		}
	}

	private Value readValue() throws RDFParseException, IOException {
		byte valueType = in.readByte();
		switch (valueType) {
		case NULL_VALUE:
			return null;
		case VALUE_REF:
			return readValueRef();
		case URI_VALUE:
			return readURI();
		case BNODE_VALUE:
			return readBNode();
		case PLAIN_LITERAL_VALUE:
			return readPlainLiteral();
		case LANG_LITERAL_VALUE:
			return readLangLiteral();
		case DATATYPE_LITERAL_VALUE:
			return readDatatypeLiteral();
		case TRIPLE_VALUE:
			return readTriple();
		default:
			reportFatalError("Unknown value type: " + valueType);
			return null;
		}
	}

	private Value readValueRef() throws IOException, RDFParseException {
		int id = readId();
		return declaredValues[id];
	}

	private IRI readURI() throws IOException, RDFParseException {
		String uri = readString();
		return createURI(uri);
	}

	private Resource readBNode() throws IOException, RDFParseException {
		String bnodeID = readString();
		return createNode(bnodeID);
	}

	private Literal readPlainLiteral() throws IOException, RDFParseException {
		String label = readString();
		return createLiteral(label, null, null, -1, -1);
	}

	private Literal readLangLiteral() throws IOException, RDFParseException {
		String label = readString();
		String language = readString();
		return createLiteral(label, language, null, -1, -1);
	}

	private Literal readDatatypeLiteral() throws IOException, RDFParseException {
		String label = readString();
		String datatype = readString();
		IRI dtUri = createURI(datatype);
		return createLiteral(label, null, dtUri, -1, -1);
	}

	private Triple readTriple() throws IOException {
		Value subject = readValue();
		if (subject instanceof Resource) {
			Value predicate = readValue();
			if (predicate instanceof IRI) {
				Value object = readValue();

				return valueFactory.createTriple((Resource) subject, (IRI) predicate, object);
			}
		}

		reportFatalError("Invalid RDF-star triple value");
		return null;
	}

	private int readId() throws IOException {
		if (formatVersion == FORMAT_V1) {
			return in.readInt();
		} else {
			return readVarInt(in);
		}
	}

	private String readString() throws IOException {
		if (formatVersion == FORMAT_V1) {
			int stringLength = in.readInt();
			int stringBytes = stringLength << 1;
			byte[] bytes = readBytes(stringBytes);
			return new String(bytes, 0, stringBytes, V1_STRING_CHARSET);
		} else {
			int length = readVarInt(in);
			byte[] bytes = readBytes(length);
			return new String(bytes, 0, length, charset);
		}
	}

	protected byte[] readBytes(int length) throws IOException {
		if (buf == null || buf.length < length) {
			// Allocate what we need plus some extra space
			buf = new byte[length << 1];
		}

		in.readFully(buf, 0, length);
		return buf;
	}

}
