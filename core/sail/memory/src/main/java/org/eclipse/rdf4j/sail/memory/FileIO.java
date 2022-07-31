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
package org.eclipse.rdf4j.sail.memory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.rio.helpers.RDFStarUtil;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.base.SailDataset;
import org.eclipse.rdf4j.sail.base.SailSink;
import org.eclipse.rdf4j.sail.memory.model.MemIRI;
import org.eclipse.rdf4j.sail.memory.model.MemResource;
import org.eclipse.rdf4j.sail.memory.model.MemValue;

/**
 * Functionality to read and write MemoryStore to/from a file.
 *
 * @author Arjohn Kampman
 */
class FileIO {

	/*-----------*
	 * Constants *
	 *-----------*/

	/** Magic number for Binary Memory Store Files */
	private static final byte[] MAGIC_NUMBER = new byte[] { 'B', 'M', 'S', 'F' };

	/** The version number of the current format. */
	// Version 1: initial version
	// Version 2: don't use read/writeUTF() to remove 64k limit on strings,
	// removed dummy "up-to-date status" boolean for namespace records
	// Version 3: introduced RDF-star triple record type
	private static final int BMSF_VERSION = 3;

	/* RECORD TYPES */
	public static final int NAMESPACE_MARKER = 1;

	public static final int EXPL_TRIPLE_MARKER = 2;

	public static final int EXPL_QUAD_MARKER = 3;

	public static final int INF_TRIPLE_MARKER = 4;

	public static final int INF_QUAD_MARKER = 5;

	public static final int URI_MARKER = 6;

	public static final int BNODE_MARKER = 7;

	public static final int PLAIN_LITERAL_MARKER = 8;

	public static final int LANG_LITERAL_MARKER = 9;

	public static final int DATATYPE_LITERAL_MARKER = 10;

	public static final int RDFSTAR_TRIPLE_MARKER = 11;

	public static final int EOF_MARKER = 127;

	/*-----------*
	 * Variables *
	 *-----------*/

	private final ValueFactory vf;

	private final CharsetEncoder charsetEncoder = StandardCharsets.UTF_8.newEncoder();

	private final CharsetDecoder charsetDecoder = StandardCharsets.UTF_8.newDecoder();

	private int formatVersion;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public FileIO(ValueFactory vf) {
		this.vf = vf;
	}

	/*---------*
	 * Methods *
	 *---------*/

	public synchronized void write(SailDataset explicit, SailDataset inferred, File syncFile, File dataFile)
			throws IOException, SailException {
		write(explicit, inferred, syncFile);

		// prefer atomic renameTo operations
		boolean renamed = syncFile.renameTo(dataFile);

		if (!renamed) {
			// tolerate renameTo that does not work if destination exists
			if (syncFile.exists() && dataFile.exists()) {
				dataFile.delete();
				renamed = syncFile.renameTo(dataFile);
			}
		}

		if (!renamed) {
			String path = syncFile.getAbsolutePath();
			String name = dataFile.getName();
			throw new IOException("Could not rename " + path + " to " + name);
		}
	}

	private void write(SailDataset explicit, SailDataset inferred, File dataFile) throws IOException, SailException {
		try (OutputStream out = Files.newOutputStream(dataFile.toPath())) {
			// Write header
			out.write(MAGIC_NUMBER);
			out.write(BMSF_VERSION);
			out.flush();
			// The rest of the data is GZIP-compressed
			try (DataOutputStream dataOut = new DataOutputStream(new GZIPOutputStream(out))) {
				writeNamespaces(explicit, dataOut);
				writeStatements(explicit, inferred, dataOut);

				dataOut.writeByte(EOF_MARKER);
			}
		}
	}

	public synchronized void read(File dataFile, SailSink explicit, SailSink inferred)
			throws IOException, SailException {
		try (InputStream in = Files.newInputStream(dataFile.toPath())) {
			byte[] magicNumber = IOUtil.readBytes(in, MAGIC_NUMBER.length);
			if (!Arrays.equals(magicNumber, MAGIC_NUMBER)) {
				throw new IOException("File is not a binary MemoryStore file");
			}

			formatVersion = in.read();
			if (formatVersion > BMSF_VERSION || formatVersion < 1) {
				throw new IOException("Incompatible format version: " + formatVersion);
			}

			// The rest of the data is GZIP-compressed
			try (DataInputStream dataIn = new DataInputStream(new GZIPInputStream(in))) {
				int recordTypeMarker;
				while ((recordTypeMarker = dataIn.readByte()) != EOF_MARKER) {
					switch (recordTypeMarker) {
					case NAMESPACE_MARKER:
						readNamespace(dataIn, explicit);
						break;
					case EXPL_TRIPLE_MARKER:
						readStatement(false, true, dataIn, explicit, inferred);
						break;
					case EXPL_QUAD_MARKER:
						readStatement(true, true, dataIn, explicit, inferred);
						break;
					case INF_TRIPLE_MARKER:
						readStatement(false, false, dataIn, explicit, inferred);
						break;
					case INF_QUAD_MARKER:
						readStatement(true, false, dataIn, explicit, inferred);
						break;
					default:
						throw new IOException("Invalid record type marker: " + recordTypeMarker);
					}
				}
			}
		}
	}

	private void writeNamespaces(SailDataset store, DataOutputStream dataOut) throws IOException, SailException {
		try (CloseableIteration<? extends Namespace, SailException> iter = store.getNamespaces()) {
			while (iter.hasNext()) {
				Namespace ns = iter.next();
				dataOut.writeByte(NAMESPACE_MARKER);
				writeString(ns.getPrefix(), dataOut);
				writeString(ns.getName(), dataOut);
			}
		}
	}

	private void readNamespace(DataInputStream dataIn, SailSink store) throws IOException, SailException {
		String prefix = readString(dataIn);
		String name = readString(dataIn);

		if (formatVersion <= 1) {
			// the up-to-date status is no longer relevant
			dataIn.readBoolean();
		}

		store.setNamespace(prefix, name);
	}

	private void writeStatements(final SailDataset explicit, SailDataset inferred, DataOutputStream dataOut)
			throws IOException, SailException {
		// write explicit only statements
		writeStatement(explicit.getStatements(null, null, null), EXPL_TRIPLE_MARKER, EXPL_QUAD_MARKER, dataOut);
		// write inferred only statements
		writeStatement(inferred.getStatements(null, null, null), INF_TRIPLE_MARKER, INF_QUAD_MARKER, dataOut);
	}

	public void writeStatement(CloseableIteration<? extends Statement, SailException> stIter, int tripleMarker,
			int quadMarker, DataOutputStream dataOut) throws IOException, SailException {
		try (stIter) {
			while (stIter.hasNext()) {
				Statement st = stIter.next();
				Resource context = st.getContext();
				if (context == null) {
					dataOut.writeByte(tripleMarker);
				} else {
					dataOut.writeByte(quadMarker);
				}
				writeValue(st.getSubject(), dataOut);
				writeValue(st.getPredicate(), dataOut);
				writeValue(st.getObject(), dataOut);
				if (context != null) {
					writeValue(context, dataOut);
				}
			}
		}
	}

	private void readStatement(boolean hasContext, boolean isExplicit, DataInputStream dataIn, SailSink explicit,
			SailSink inferred) throws IOException, ClassCastException, SailException {
		MemResource memSubj = (MemResource) readValue(dataIn);
		MemIRI memPred = (MemIRI) readValue(dataIn);
		MemValue memObj = (MemValue) readValue(dataIn);
		MemResource memContext = null;
		if (hasContext) {
			memContext = (MemResource) readValue(dataIn);
		}

		if (isExplicit) {
			explicit.approve(memSubj, memPred, memObj, memContext);
		} else {
			inferred.approve(memSubj, memPred, memObj, memContext);
		}
	}

	private void writeValue(Value value, DataOutputStream dataOut) throws IOException {
		if (value.isIRI()) {
			dataOut.writeByte(URI_MARKER);
			writeString(((IRI) value).stringValue(), dataOut);
		} else if (value.isBNode()) {
			dataOut.writeByte(BNODE_MARKER);
			writeString(((BNode) value).getID(), dataOut);
		} else if (value.isLiteral()) {
			Literal lit = (Literal) value;

			String label = lit.getLabel();
			IRI datatype = lit.getDatatype();

			if (Literals.isLanguageLiteral(lit)) {
				dataOut.writeByte(LANG_LITERAL_MARKER);
				writeString(label, dataOut);
				writeString(lit.getLanguage().get(), dataOut);
			} else {
				dataOut.writeByte(DATATYPE_LITERAL_MARKER);
				writeString(label, dataOut);
				writeValue(datatype, dataOut);
			}
		} else if (value.isTriple()) {
			dataOut.writeByte(RDFSTAR_TRIPLE_MARKER);
			writeValue(RDFStarUtil.toRDFEncodedValue(value), dataOut);
		} else {
			throw new IllegalArgumentException("unexpected value type: " + value.getClass());
		}
	}

	private Value readValue(DataInputStream dataIn) throws IOException, ClassCastException {
		int valueTypeMarker = dataIn.readByte();

		if (valueTypeMarker == URI_MARKER) {
			String uriString = readString(dataIn);
			return vf.createIRI(uriString);
		} else if (valueTypeMarker == BNODE_MARKER) {
			String bnodeID = readString(dataIn);
			return vf.createBNode(bnodeID);
		} else if (valueTypeMarker == PLAIN_LITERAL_MARKER) {
			String label = readString(dataIn);
			return vf.createLiteral(label);
		} else if (valueTypeMarker == LANG_LITERAL_MARKER) {
			String label = readString(dataIn);
			String language = readString(dataIn);
			return vf.createLiteral(label, language);
		} else if (valueTypeMarker == DATATYPE_LITERAL_MARKER) {
			String label = readString(dataIn);
			IRI datatype = (IRI) readValue(dataIn);
			return vf.createLiteral(label, datatype);
		} else if (valueTypeMarker == RDFSTAR_TRIPLE_MARKER) {
			IRI rdfStarEncodedTriple = (IRI) readValue(dataIn);
			return RDFStarUtil.fromRDFEncodedValue(rdfStarEncodedTriple);
		} else {
			throw new IOException("Invalid value type marker: " + valueTypeMarker);
		}
	}

	private void writeString(String s, DataOutputStream dataOut) throws IOException {
		ByteBuffer byteBuf = charsetEncoder.encode(CharBuffer.wrap(s));
		dataOut.writeInt(byteBuf.remaining());
		dataOut.write(byteBuf.array(), 0, byteBuf.remaining());
	}

	private String readString(DataInputStream dataIn) throws IOException {
		if (formatVersion == 1) {
			return readStringV1(dataIn);
		} else {
			return readStringV2(dataIn);
		}
	}

	/**
	 * Reads a string from the version 1 format, i.e. in Java's {@link DataInput#modified-utf-8 Modified UTF-8}.
	 */
	private String readStringV1(DataInputStream dataIn) throws IOException {
		return dataIn.readUTF();
	}

	/**
	 * Reads a string from the version 2 format. Strings are encoded as UTF-8 and are preceeded by a 32-bit integer
	 * (high byte first) specifying the length of the encoded string.
	 */
	private String readStringV2(DataInputStream dataIn) throws IOException {
		int stringLength = dataIn.readInt();
		byte[] encodedString = IOUtil.readBytes(dataIn, stringLength);

		if (encodedString.length != stringLength) {
			throw new EOFException("Attempted to read " + stringLength + " bytes but no more than "
					+ encodedString.length + " were available");
		}

		ByteBuffer byteBuf = ByteBuffer.wrap(encodedString);
		CharBuffer charBuf = charsetDecoder.decode(byteBuf);

		return charBuf.toString();
	}
}
