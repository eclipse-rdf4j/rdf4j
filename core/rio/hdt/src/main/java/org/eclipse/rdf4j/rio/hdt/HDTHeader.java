/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.hdt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.zip.CheckedInputStream;
import java.util.zip.CheckedOutputStream;

import org.eclipse.rdf4j.common.io.UncloseableInputStream;
import org.eclipse.rdf4j.common.io.UncloseableOutputStream;

/**
 * HDT Header Part.
 * 
 * This part starts with <code>$HDT</code>, followed by a byte indicating the type of the part, the NULL-terminated
 * string for the format, and optionally one or more <code>key=value;</code> properties.
 * 
 * Then a <code>NULL</code> byte, followed by the 16-bit CRC (<code>$HDT</code> and <code>NULL</code> included).
 * 
 * Structure:
 * 
 * <pre>
 * +------+------+--------+------+------------+------+-------+
 * | $HDT | type | format | NULL | key=value; | NULL | CRC16 |
 * +------+------+--------+------+------------+------+-------+
 * </pre>
 * 
 * @author Bart Hanssens
 */
class HDTHeader extends HDTPart {

	protected final static byte[] HEADER_FORMAT = "ntriples".getBytes(StandardCharsets.US_ASCII);
	protected final static String HEADER_LENGTH = "length";

	private byte[] headerData;

	/**
	 * Get raw header data (byte array data stored as NTriples)
	 * 
	 * @return byte array
	 */
	protected byte[] getHeaderData() {
		return headerData;
	}

	/**
	 * Set raw header data (byte array data stored as NTriples)
	 * 
	 * @return byte array
	 */
	protected void setHeaderData(byte[] headerData) {
		this.headerData = headerData;
	}

	@Override
	protected void parse(InputStream is) throws IOException {
		// don't close CheckedInputStream, as it will close the underlying inputstream
		try (UncloseableInputStream uis = new UncloseableInputStream(is);
				CheckedInputStream cis = new CheckedInputStream(uis, new CRC16())) {

			checkControl(cis, HDTPart.Type.HEADER);
			checkFormat(cis, HEADER_FORMAT);
			properties = readProperties(cis);

			checkCRC(cis, is, 2);
		}
		int hlen = getIntegerProperty(properties, HEADER_LENGTH, "Header length");
		headerData = parseHeaderData(is, hlen);
	}

	@Override
	protected void write(OutputStream os) throws IOException {
		// don't close CheckedOutputStream, as it will close the underlying outputstream
		try (UncloseableOutputStream uos = new UncloseableOutputStream(os);
				CheckedOutputStream cos = new CheckedOutputStream(uos, new CRC16())) {

			String hlen = String.valueOf(headerData.length);

			writeControl(cos, HDTPart.Type.HEADER);
			writeFormat(cos, HEADER_FORMAT);
			writeProperties(cos, Collections.singletonMap(HEADER_LENGTH, hlen));

			writeCRC(cos, os, 2);
		}
		writeHeaderData(os);
	}

	/**
	 * Parse header data with metadata in NTriples format.
	 * 
	 * @param is  input stream
	 * @param len length
	 * @throws IOException
	 */
	private byte[] parseHeaderData(InputStream is, int len) throws IOException {
		byte b[] = new byte[len];
		is.read(b);
		return b;
	}

	/**
	 * Write header data with metadata in NTriples format.
	 * 
	 * @param os output stream
	 * @throws IOException
	 */
	private void writeHeaderData(OutputStream os) throws IOException {
		os.write(headerData);
	}
}
