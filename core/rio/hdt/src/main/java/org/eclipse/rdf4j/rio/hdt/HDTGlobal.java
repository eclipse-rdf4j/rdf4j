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
 * HDT Global Part.
 * 
 * This part starts with <code>$HDT</code>, followed by a byte indicating the type of the part, the NULL-terminated URI
 * string for the format, and optionally one or more <code>key=value;</code> properties.
 * 
 * These properties may include a base URI, and the name of the software that produced the HDT file.
 * 
 * Then a <code>NULL</code> byte, followed by the 16-bit CRC (<code>$HDT</code> and <code>NULL</code> included)
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
class HDTGlobal extends HDTPart {
	protected final static byte[] GLOBAL_FORMAT = "<http://purl.org/HDT/hdt#HDTv1>".getBytes(StandardCharsets.US_ASCII);
	protected final static String GLOBAL_BASEURI = "BaseUri";
	protected final static String GLOBAL_SOFTWARE = "Software";

	@Override
	protected void parse(InputStream is) throws IOException {
		// don't close CheckedInputStream, as it will close the underlying inputstream
		try (UncloseableInputStream uis = new UncloseableInputStream(is);
				CheckedInputStream cis = new CheckedInputStream(uis, new CRC16())) {

			checkControl(cis, HDTPart.Type.GLOBAL);
			checkFormat(cis, GLOBAL_FORMAT);
			properties = readProperties(cis);

			checkCRC(cis, is, 2);
		}
	}
	
	@Override
	protected void write(OutputStream os) throws IOException {
		// don't close CheckedOutputStream, as it will close the underlying outputstream
		try (UncloseableOutputStream uos = new UncloseableOutputStream(os);
				CheckedOutputStream cos = new CheckedOutputStream(uos, new CRC16())) {

			writeControl(cos, HDTPart.Type.GLOBAL);
			writeFormat(cos, GLOBAL_FORMAT);
			writeProperties(cos, Collections.EMPTY_MAP);

			writeCRC(cos, os, 2);
		}
	}
}
