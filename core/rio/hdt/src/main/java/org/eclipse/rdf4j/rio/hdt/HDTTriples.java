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
import java.util.HashMap;
import java.util.Map;

import java.util.zip.CheckedInputStream;
import java.util.zip.CheckedOutputStream;
import org.eclipse.rdf4j.common.io.UncloseableInputStream;
import org.eclipse.rdf4j.common.io.UncloseableOutputStream;

/**
 * HDT Triples Part.
 * 
 * This part starts with <code>$HDT</code>, followed by a byte indicating the type of the part, the NULL-terminated URI
 * string for the format, and optionally one or more <code>key=value;</code> properties.
 * 
 * These properties may include the order (SPO, SOP...), and the number of triples.
 * 
 * Then a <code>NULL</code> byte, followed by the 16-bit CRC (<code>$HDT</code> and <code>NULL</code> included)
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
class HDTTriples extends HDTPart {
	protected enum Order {
		UNKNOWN(0),
		SPO(1),
		SOP(2),
		PSO(3),
		POS(4),
		OSP(5),
		OPS(6);
		private final int value;

		protected int getValue() {
			return value;
		}

		/**
		 * Constructor
		 * 
		 * @param value integer value
		 */
		private Order(int value) {
			this.value = value;
		}
	}

	protected final static byte[] FORMAT_LIST = "<http://purl.org/HDT/hdt#triplesList>"
			.getBytes(StandardCharsets.US_ASCII);
	protected final static byte[] FORMAT_BITMAP = "<http://purl.org/HDT/hdt#triplesBitmap>"
			.getBytes(StandardCharsets.US_ASCII);
	protected final static String ORDER = "order";
	protected final static String NUM = "numTriples";

	private Order order;
	private int nrtriples;

	/**
	 * Return triple order
	 * 
	 * @return enum
	 */
	protected Order getOrder() {
		return order;
	}

	@Override
	protected void parse(InputStream is) throws IOException {
		// don't close CheckedInputStream, as it will close the underlying inputstream
		try (UncloseableInputStream uis = new UncloseableInputStream(is);
				CheckedInputStream cis = new CheckedInputStream(uis, new CRC16())) {

			checkControl(cis, HDTPart.Type.TRIPLES);
			checkFormat(cis, FORMAT_BITMAP);

			properties = readProperties(cis);

			int i = getIntegerProperty(properties, ORDER, "order");
			if (i != HDTTriples.Order.SPO.getValue()) {
				throw new UnsupportedOperationException(
						"Triples section: order " + Integer.toString(i) + ", but only SPO order is supported");
			}

			checkCRC(cis, is, 2);
		}
	}

	@Override
	protected void write(OutputStream os) throws IOException {
		// don't close CheckedOutputstream, as it will close the underlying outputstream
		try (UncloseableOutputStream uos = new UncloseableOutputStream(os);
				CheckedOutputStream cos = new CheckedOutputStream(uos, new CRC16())) {

			writeControl(cos, HDTPart.Type.TRIPLES);
			writeFormat(cos, FORMAT_BITMAP);

			Map<String, String> props = new HashMap<>();
			props.put(ORDER, String.valueOf(HDTTriples.Order.SPO.getValue()));
			writeProperties(cos, props);

			writeCRC(cos, os, 2);
		}
	}
}
