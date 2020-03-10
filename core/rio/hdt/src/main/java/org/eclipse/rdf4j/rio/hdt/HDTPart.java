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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CheckedInputStream;
import java.util.zip.CheckedOutputStream;

/**
 * Helper class for different HDT parts.
 * 
 * Each part starts with <code>$HDT</code>, followed by a byte indicating the type of the part.
 * 
 * Structure:
 * 
 * <pre>
 * +------+------+
 * | $HDT | type |
 * +------+------+
 * </pre>
 * 
 * @author Bart Hanssens
 */
abstract class HDTPart {
	protected enum Type {
		GLOBAL((byte) 1),
		HEADER((byte) 2),
		DICTIONARY((byte) 3),
		TRIPLES((byte) 4);
		private final byte value;

		/**
		 * Get value associated with this type
		 * 
		 * @return value 1,2 or 3
		 */
		protected byte getValue() {
			return value;
		}

		private Type(byte value) {
			this.value = value;
		}
	}

	protected final static byte[] COOKIE = "$HDT".getBytes(StandardCharsets.US_ASCII);

	// TODO: make configurable, buffer for reading object values
	private final static int BUFLEN = 1 * 1024 * 1024;
	// for debugging purposes
	protected final String name;
	protected final long pos;
	protected Map<String, String> properties;

	/**
	 * Parse from input stream
	 * 
	 * @param is
	 * @throws IOException
	 */
	protected abstract void parse(InputStream is) throws IOException;

	/**
	 * Write to output stream
	 * 
	 * @param is
	 * @throws IOException
	 */
	protected void write(OutputStream os) throws IOException {
	}

	/**
	 * Get properties, if any.
	 * 
	 * @return key,value map
	 */
	protected Map<String, String> getProperties() {
		return properties;
	}

	/**
	 * Constructor
	 * 
	 * @param name part name
	 * @param pos  starting position in input stream
	 */
	protected HDTPart(String name, long pos) {
		this.name = name;
		this.pos = pos;
	}

	/**
	 * Constructor
	 */
	protected HDTPart() {
		this("", -1);
	}

	/**
	 * Get a string for debugging purposes, containing the name and starting position of this part.
	 * 
	 * @return string
	 */
	protected String getDebugPartStr() {
		if (name == null || name.isEmpty()) {
			return "";
		}
		return (pos != -1) ? name + " (starts at byte " + pos + ")" : name;
	}

	/**
	 * Check start of part for <code>$HDT</code> and the byte indicating the type
	 * 
	 * @param is    input stream
	 * @param ctype control type
	 * @throws IOException
	 */
	protected static void checkControl(InputStream is, HDTPart.Type ctype) throws IOException {
		byte[] cookie = new byte[COOKIE.length];
		is.read(cookie);
		if (!Arrays.equals(cookie, COOKIE)) {
			throw new IOException("$HDT marker not found");
		}

		byte b = (byte) is.read();
		if (b != ctype.getValue()) {
			throw new IOException("Info type " + Long.toHexString(b) + ", but expected different control info type");
		}
	}

	/**
	 * Write start of part for <code>$HDT</code> and the byte indicating the type
	 * 
	 * @param os    output stream
	 * @param ctype control type
	 * @throws IOException
	 */
	protected static void writeControl(OutputStream os, HDTPart.Type ctype) throws IOException {
		os.write(COOKIE);
		os.write(ctype.getValue());
	}

	/**
	 * Check for <code>null</code> terminated format string.
	 * 
	 * @param is     input stream
	 * @param format
	 * @throws IOException
	 */
	protected static void checkFormat(InputStream is, byte[] format) throws IOException {
		byte[] b = new byte[format.length];
		is.read(b);
		if (!Arrays.equals(b, format)) {
			throw new IOException("Unknown format, expected " + new String(format, StandardCharsets.US_ASCII));
		}
		is.read(); // also read null byte
	}

	/**
	 * Write <code>null</code> terminated format string.
	 * 
	 * @param os     output stream
	 * @param format
	 * @throws IOException
	 */
	protected static void writeFormat(OutputStream os, byte[] format) throws IOException {
		os.write(format);
		os.write(0b00); // also write null byte
	}

	/**
	 * Read null terminated series of bytes
	 * 
	 * @param is input stream
	 * @return
	 * @throws IOException
	 */
	protected static byte[] readToNull(InputStream is) throws IOException {
		byte[] buf = new byte[BUFLEN];
		int len = 0;

		do {
			buf[len] = (byte) is.read();
		} while (buf[len] != 0b00 && ++len < BUFLEN);

		if (len == BUFLEN) {
			throw new IOException("Buffer for reading properties exceeded, max " + BUFLEN);
		}
		return Arrays.copyOf(buf, len);
	}

	/**
	 * Write null terminated series of bytes
	 * 
	 * @param os output stream
	 * @param b  byte buffer
	 * @return
	 * @throws IOException
	 */
	protected static void writeWithNull(OutputStream os, byte[] b) throws IOException {
		os.write(b);
		os.write(0b00);
	}

	/**
	 * Get the first position of the NULL byte within an array of bytes
	 * 
	 * @param b     byte array
	 * @param start position to start from
	 * @return position of first NULL byte
	 */
	protected static int countToNull(byte[] b, int start) throws IOException {
		for (int i = start; i < b.length; i++) {
			if (b[i] == 0b00) {
				return i;
			}
		}
		throw new IOException("No null byte found in buffer starting at byte " + start);
	}

	/**
	 * Get the properties from the input stream, reading at most BUFLEN bytes. The properties are encoded as a
	 * <code>key=value;</code> string and must be <code>null</code> terminated.
	 * 
	 * @param is input stream
	 * @return key,value map
	 * @throws IOException
	 */
	protected static Map<String, String> readProperties(InputStream is) throws IOException {
		return mapProperties(readToNull(is));
	}

	/**
	 * Set the properties for the output stream. The properties are encoded as a <code>key=value;</code> string and must
	 * be <code>null</code> terminated.
	 * 
	 * @param is   output stream
	 * @param prop key,value map
	 * @throws IOException
	 */
	protected static void writeProperties(OutputStream os, Map<String, String> props) throws IOException {
		writeWithNull(os, stringProperties(props));
	}

	/**
	 * Get properties as a key, value map
	 * 
	 * @param props
	 * @return
	 */
	protected static Map<String, String> mapProperties(byte[] props) {
		Map<String, String> map = new HashMap<>();
		if (props == null || props.length == 0) {
			return map;
		}

		String strs[] = new String(props, 0, props.length, StandardCharsets.US_ASCII).split(";");
		for (String str : strs) {
			String prop[] = str.split("=");
			if (prop.length == 2) {
				map.put(prop[0], prop[1]);
			}
		}
		return map;
	}

	/**
	 * Flatten a key, value map to a ';' separated string
	 * 
	 * @param props key,value map
	 * @return array of bytes
	 */
	protected static byte[] stringProperties(Map<String, String> props) {
		if (props == null || props.isEmpty()) {
			return new byte[0];
		}
		StringBuilder builder = new StringBuilder(props.size() * 16 * 2); // estimate
		props.forEach((k, v) -> {
			builder.append(k).append('=').append(v).append(';');
		});

		return builder.toString().getBytes(StandardCharsets.US_ASCII);
	}

	/**
	 * Get the positive integer value from a property map. Throw an exception when the property is missing, or less than
	 * 1.
	 * 
	 * @param props property map
	 * @param prop  name of the property
	 * @param name  display name of the property
	 * @return positive integer
	 * @throws IOException
	 */
	protected int getIntegerProperty(Map<String, String> props, String prop, String name) throws IOException {
		int len = 0;

		String str = props.getOrDefault(prop, "0");
		try {
			len = Integer.parseInt(str);
		} catch (NumberFormatException nfe) {
			throw new IOException(name + " is not an integer: " + str);
		}
		if (len < 1) {
			throw new IOException(name + " is less than 1: " + len);
		}
		return len;
	}

	/**
	 * Compare the calculated checksum to the expected one.
	 * 
	 * @param cis checked input stream
	 * @param is  (unchecked) input stream
	 * @param len number of bytes of the checksum
	 * @throws IOException
	 */
	protected static void checkCRC(CheckedInputStream cis, InputStream is, int len) throws IOException {
		long calc = cis.getChecksum().getValue();

		byte[] checksum = new byte[len];
		is.read(checksum);

		long expect = 0L;
		// little-endian to big-endian, e.g. HDT-It stores checksum 7635 as 0x35 0x76 (at least on x86)
		for (int i = len - 1; i >= 0; i--) {
			expect <<= 8;
			expect |= checksum[i] & 0xFF;
		}

		if (calc != expect) {
			throw new IOException("CRC does not match: calculated " +
					Long.toHexString(calc) + " instead of " + Long.toHexString(expect));
		}
	}

	/**
	 * Write the CRC
	 * 
	 * @param cos checked output stream
	 * @param os  (unchecked) output stream
	 * @param len number of bytes of the checksum
	 * @throws IOException
	 */
	protected static void writeCRC(CheckedOutputStream cos, OutputStream os, int len) throws IOException {
		long calc = cos.getChecksum().getValue();

		byte[] checksum = new byte[len];

		// big-endian to little-endian, e.g. HDT-It stores checksum 7635 as 0x35 0x76 (at least on x86)
		for (int i = 0; i < len; i++) {
			checksum[i] = (byte) (calc & 0xFF);
			calc >>= 8;
		}

		os.write(checksum);
	}
}
