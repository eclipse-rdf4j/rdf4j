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
import java.nio.charset.StandardCharsets;
import java.util.zip.CheckedInputStream;

import org.eclipse.rdf4j.common.io.UncloseableInputStream;

/**
 * HDT Dictionary Part.
 * 
 * This part starts with <code>$HDT</code>, followed by a byte indicating the type of the part, the NULL-terminated URI
 * string for the format, and optionally one or more <code>key=value;</code> properties.
 * 
 * Then a <code>NULL</code> byte, followed by the 16-bit CRC (<code>$HDT</code> and <code>NULL</code> included).
 * 
 * Structure:
 * 
 * <pre>
 * +------+------+-----+------+------------+------+-------+
 * | $HDT | type | URI | NULL | key=value; | NULL | CRC16 |
 * +------+------+-----+------+------------+------+-------+
 * </pre>
 * 
 * @author Bart Hanssens
 */
class HDTDictionary extends HDTPart {
	protected final static byte[] DICT_FORMAT = "<http://purl.org/HDT/hdt#dictionaryFour>"
			.getBytes(StandardCharsets.US_ASCII);
	protected final static String DICT_MAPPING = "mapping";
	protected final static String DICT_ELEMENTS = "elements";

	@Override
	protected void parse(InputStream is) throws IOException {
		// don't close CheckedInputStream, as it will close the underlying inputstream
		try (UncloseableInputStream uis = new UncloseableInputStream(is);
				CheckedInputStream cis = new CheckedInputStream(uis, new CRC16())) {

			checkControl(cis, HDTPart.Type.DICTIONARY);
			checkFormat(cis, DICT_FORMAT);

			properties = getProperties(cis);

			checkCRC(cis, is, 2);
		}
	}
}
