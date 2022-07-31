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

package org.eclipse.rdf4j.common.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Utility methods for I/O working with Readers, Writers, InputStreams and OutputStreams.
 */
public class IOUtil {

	/**
	 * Read the contents as a string from the given (unbuffered) file.
	 *
	 * @param file file to read
	 * @return content as one single string
	 * @throws IOException
	 */
	public static String readString(File file) throws IOException {
		try (FileInputStream in = new FileInputStream(file)) {
			return readString(in);
		}
	}

	/**
	 * Read the contents of a (unbuffered) resource into one single string.
	 *
	 * @param url url to get the data from
	 * @return string
	 * @throws IOException
	 */
	public static String readString(URL url) throws IOException {
		try (Reader reader = urlToReader(url)) {
			return readString(reader);
		}
	}

	/**
	 * Read the contents of an (unbuffered) input stream into a single string.
	 *
	 * @param in input stream
	 * @return string
	 * @throws IOException
	 */
	public static String readString(InputStream in) throws IOException {
		return readString(new InputStreamReader(in));
	}

	/**
	 * Reads all characters from the supplied reader and returns them as a string.
	 *
	 * @param r The Reader supplying the characters
	 * @return A String containing all characters from the supplied reader.
	 * @throws IOException
	 */
	public static String readString(Reader r) throws IOException {
		return readFully(r).toString();
	}

	/**
	 * Reads a string of at most length <var>maxChars</var> from the supplied Reader.
	 *
	 * @param r        The Reader to read the string from.
	 * @param maxChars The maximum number of characters to read.
	 * @return A String of length <var>maxChars</var>, or less if the supplied Reader did not contain that much
	 *         characters.
	 * @throws IOException
	 */
	public static String readString(Reader r, int maxChars) throws IOException {
		char[] charBuf = new char[maxChars];
		int charsRead = readChars(r, charBuf);
		return new String(charBuf, 0, charsRead);
	}

	/**
	 * Read the contents of a (unbuffered) resource into a character array.
	 *
	 * @param url url to get the data from
	 * @return character array
	 * @throws IOException
	 */
	public static char[] readChars(URL url) throws IOException {
		try (Reader reader = urlToReader(url)) {
			return readChars(reader);
		}
	}

	/**
	 * Reads all characters from the supplied reader and returns them.
	 *
	 * @param r The Reader supplying the characters
	 * @return A character array containing all characters from the supplied reader.
	 * @throws IOException
	 */
	public static char[] readChars(Reader r) throws IOException {
		return readFully(r).toCharArray();
	}

	/**
	 * Fills the supplied character array with characters read from the specified Reader. This method will only stop
	 * reading when the character array has been filled completely, or the end of the stream has been reached.
	 *
	 * @param r         The Reader to read the characters from.
	 * @param charArray The character array to fill with characters.
	 * @return The number of characters written to the character array.
	 * @throws IOException
	 */
	public static int readChars(Reader r, char[] charArray) throws IOException {
		int totalCharsRead = 0;

		int charsRead = r.read(charArray);

		while (charsRead >= 0) {
			totalCharsRead += charsRead;

			if (totalCharsRead == charArray.length) {
				break;
			}

			charsRead = r.read(charArray, totalCharsRead, charArray.length - totalCharsRead);
		}

		return totalCharsRead;
	}

	/**
	 * Reads all bytes from the specified file and returns them as a byte array.
	 *
	 * @param file The file to read.
	 * @return A byte array containing all bytes from the specified file.
	 * @throws IOException              If an I/O error occurred while reading from the file.
	 * @throws IllegalArgumentException If the file size exceeds the maximum array length (larger than
	 *                                  {@link Integer#MAX_VALUE}.
	 */
	public static byte[] readBytes(File file) throws IOException {
		long fileSize = file.length();
		if (fileSize > Integer.MAX_VALUE) {
			throw new IllegalArgumentException(
					"File size exceeds maximum array length (" + fileSize + " > " + Integer.MAX_VALUE + ")");
		}

		try (FileInputStream in = new FileInputStream(file)) {
			return readBytes(in, (int) fileSize);
		}
	}

	/**
	 * Reads all bytes from the supplied input stream and returns them as a byte array.
	 *
	 * @param in The InputStream supplying the bytes.
	 * @return A byte array containing all bytes from the supplied input stream.
	 * @throws IOException
	 */
	public static byte[] readBytes(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
		transfer(in, out);
		return out.toByteArray();
	}

	/**
	 * Reads at most <var>maxBytes</var> bytes from the supplied input stream and returns them as a byte array.
	 *
	 * @param in       The InputStream supplying the bytes.
	 * @param maxBytes The maximum number of bytes to read from the input stream.
	 * @return A byte array of size <var>maxBytes</var> if the input stream can produce that amount of bytes, or a
	 *         smaller byte array containing all available bytes from the stream otherwise.
	 * @throws IOException
	 */
	public static byte[] readBytes(InputStream in, int maxBytes) throws IOException {
		byte[] result = new byte[maxBytes];

		int bytesRead = readBytes(in, result);

		if (bytesRead < maxBytes) {
			// Create smaller byte array
			byte[] tmp = new byte[bytesRead];
			System.arraycopy(result, 0, tmp, 0, bytesRead);
			result = tmp;
		}

		return result;
	}

	/**
	 * Fills the supplied byte array with bytes read from the specified InputStream. This method will only stop reading
	 * when the byte array has been filled completely, or the end of the stream has been reached.
	 *
	 * @param in        The InputStream to read the bytes from.
	 * @param byteArray The byte array to fill with bytes.
	 * @return The number of bytes written to the byte array.
	 * @throws IOException
	 */
	public static int readBytes(InputStream in, byte[] byteArray) throws IOException {
		int totalBytesRead = 0;

		int bytesRead = in.read(byteArray);

		while (bytesRead >= 0) {
			totalBytesRead += bytesRead;

			if (totalBytesRead == byteArray.length) {
				break;
			}

			bytesRead = in.read(byteArray, totalBytesRead, byteArray.length - totalBytesRead);
		}

		return totalBytesRead;
	}

	/**
	 * Read properties from the specified file.
	 *
	 * @param propsFile the file to read from
	 * @return Properties loaded from the specified file
	 * @throws IOException when the file could not be read properly
	 */
	public static Properties readProperties(File propsFile) throws IOException {
		return readProperties(propsFile, null);
	}

	/**
	 * Read properties from the specified file.
	 *
	 * @param propsFile the file to read from
	 * @param defaults  the default properties to use
	 * @return Properties loaded from the specified file
	 * @throws IOException when the file could not be read properly
	 */
	public static Properties readProperties(File propsFile, Properties defaults) throws IOException {
		return readProperties(new FileInputStream(propsFile), defaults);
	}

	/**
	 * Read properties from the specified InputStream.
	 *
	 * @param in the stream to read from. The stream will be closed by this method.
	 * @return Properties loaded from the specified stream. The stream will be closed by this method.
	 * @throws IOException when the stream could not be read properly
	 */
	public static Properties readProperties(InputStream in) throws IOException {
		return readProperties(in, null);
	}

	/**
	 * Read properties from the specified InputStream.
	 *
	 * @param in       the stream to read from. The stream will be closed by this method.
	 * @param defaults the default properties
	 * @return Properties loaded from the specified stream. The stream will be closed by this method.
	 * @throws IOException when the stream could not be read properly
	 */
	public static Properties readProperties(InputStream in, Properties defaults) throws IOException {
		Properties result = new Properties(defaults);
		try (in) {
			result.load(in);
		}
		return result;
	}

	/**
	 * Write the specified properties to the specified file.
	 *
	 * @param props           the properties to write
	 * @param file            the file to write to
	 * @param includeDefaults true when default values need to be included
	 * @throws IOException when the properties could not be written to the file properly
	 */
	public static void writeProperties(Properties props, File file, boolean includeDefaults) throws IOException {
		writeProperties(props, new FileOutputStream(file), includeDefaults);
	}

	/**
	 * Write the specified properties to the specified output stream.
	 *
	 * @param props           the properties to write
	 * @param out             the output stream to write to
	 * @param includeDefaults true if default values need to be included
	 * @throws IOException when the properties could not be written to the output stream properly
	 */
	public static void writeProperties(Properties props, OutputStream out, boolean includeDefaults) throws IOException {
		if (includeDefaults) {
			Properties all = new Properties();
			Enumeration<?> propNames = props.propertyNames();
			while (propNames.hasMoreElements()) {
				String propName = (String) propNames.nextElement();
				String propValue = props.getProperty(propName);
				all.put(propName, propValue);
			}
			props = all;
		}

		try (out) {
			props.store(out, null);
		}
	}

	/**
	 * Writes all data that can be read from the supplied InputStream to the specified file.
	 *
	 * @param in   An InputStream.
	 * @param file The file to write the data to.
	 * @throws IOException If an I/O error occurred.
	 */
	public static void writeStream(InputStream in, File file) throws IOException {
		try (FileOutputStream out = new FileOutputStream(file)) {
			transfer(in, out);
		}
	}

	/**
	 * Write the contents of a string (unbuffered) to a file
	 *
	 * @param contents string contents to write
	 * @param file     file to write to
	 * @throws IOException
	 */
	public static void writeString(String contents, File file) throws IOException {
		try (FileWriter out = new FileWriter(file)) {
			out.write(contents);
		}
	}

	/**
	 * Write the contents of a byte array (unbuffered) to a file.
	 *
	 * @param data data to write
	 * @param file file
	 * @throws IOException
	 */
	public static void writeBytes(byte[] data, File file) throws IOException {
		try (FileOutputStream out = new FileOutputStream(file)) {
			writeBytes(data, out);
		}
	}

	/**
	 * Write he contents of a byte array (unbuffered) to an output stream.
	 *
	 * @param data data to write
	 * @param out  file
	 * @throws IOException
	 */
	public static void writeBytes(byte[] data, OutputStream out) throws IOException {
		transfer(new ByteArrayInputStream(data), out);
	}

	/**
	 * Read the contents of a resource into a reader. Currently ignores HTTP Content-Encoding response header.
	 *
	 * @param url url
	 * @return reader
	 * @throws IOException
	 */
	public static Reader urlToReader(URL url) throws IOException {
		// FIXME: character encoding should be read from response headers or
		// should default to ISO-8859-1
		URLConnection con = url.openConnection();
		return new InputStreamReader(con.getInputStream());
	}

	/**
	 * Read into a character array writer.
	 *
	 * @param r input reader
	 * @return character array writer
	 * @throws IOException
	 */
	private static CharArrayWriter readFully(Reader r) throws IOException {
		CharArrayWriter result = new CharArrayWriter();
		char[] buf = new char[4096];
		int charsRead;

		while ((charsRead = r.read(buf)) != -1) {
			result.write(buf, 0, charsRead);
		}

		return result;
	}

	/**
	 * Transfers all bytes that can be read from <var>in</var> to <var>out</var>.
	 *
	 * @param in  The InputStream to read data from.
	 * @param out The OutputStream to write data to.
	 * @return The total number of bytes transfered.
	 * @throws IOException
	 */
	public static final long transfer(InputStream in, OutputStream out) throws IOException {
		long totalBytes = 0;
		int bytesInBuf;
		byte[] buf = new byte[4096];

		while ((bytesInBuf = in.read(buf)) != -1) {
			out.write(buf, 0, bytesInBuf);
			totalBytes += bytesInBuf;
		}

		return totalBytes;
	}

	/**
	 * Writes all bytes from an <var>InputStream</var> to a file.
	 *
	 * @param in   The <var>InputStream</var> containing the data to write to the file.
	 * @param file The file to write the data to.
	 * @return The total number of bytes written.
	 * @throws IOException If an I/O error occurred while trying to write the data to the file.
	 */
	public static final long transfer(InputStream in, File file) throws IOException {
		try (FileOutputStream out = new FileOutputStream(file)) {
			return transfer(in, out);
		}
	}

	/**
	 * Transfers all characters that can be read from <var>in</var> to <var>out</var> .
	 *
	 * @param in  The Reader to read characters from.
	 * @param out The Writer to write characters to.
	 * @return The total number of characters transfered.
	 * @throws IOException
	 */
	public static final long transfer(Reader in, Writer out) throws IOException {
		long totalChars = 0;
		int charsInBuf;
		char[] buf = new char[4096];

		while ((charsInBuf = in.read(buf)) != -1) {
			out.write(buf, 0, charsInBuf);
			totalChars += charsInBuf;
		}

		return totalChars;
	}

	/**
	 * Writes all characters from a <var>Reader</var> to a file using the default character encoding.
	 *
	 * @param reader The <var>Reader</var> containing the data to write to the file.
	 * @param file   The file to write the data to.
	 * @return The total number of characters written.
	 * @throws IOException If an I/O error occurred while trying to write the data to the file.
	 * @see java.io.FileWriter
	 */
	public static final long transfer(Reader reader, File file) throws IOException {
		try (FileWriter writer = new FileWriter(file)) {
			return transfer(reader, writer);
		}
	}

	/**
	 * <p>
	 * Write an variable length (non-negative) integer.
	 * </p>
	 * <p>
	 * The variable length integer encoding (also know as <i>varint</i> and <i>vint</i>) writes non-negative / unsigned
	 * integers in a minimal number of bytes (binary octets). The encoding uses the most significant bit is used to
	 * indicate whether another varint byte follows. The remaining 7 bits of every octet are used to store the binary
	 * representation of the actual integer value. Note that the least significant bytes of the integer value are stored
	 * before more significate bytes.
	 * </p>
	 * <p>
	 * The table below shows a few examples of decimals encoded both as a 32-bit integer and as variable length binary:
	 *
	 * <pre>
	 * {@code
	 * decimal |           32-bit integer            |          variable length binary
	 *       0 | 00000000 00000000 00000000 00000000 |                   00000000   (1 byte)
	 *       1 | 00000000 00000000 00000000 00000001 |                   00000001   (1 byte)
	 *      42 | 00000000 00000000 00000000 00101010 |                   00101010   (1 byte)
	 *     421 | 00000000 00000000 00000001 10100101 |          10100101 00000011   (2 bytes)
	 *  100000 | 00000000 00000001 10000110 10100000 | 10100000 10001101 00000110   (3 bytes)
	 * }
	 * </pre>
	 * </p>
	 *
	 * @param out   The {@link OutputStream} to write to.
	 * @param value The {@code int} value to write.
	 * @throws IOException If an error occurred while writing the integer.
	 */
	public static void writeVarInt(OutputStream out, int value) throws IOException {
		if (value < 0) {
			throw new IllegalArgumentException("Unable to write negative variable length integer");
		}

		while (value > 127) {
			out.write(value & 0b01111111 | 0b10000000);
			value >>>= 7;
		}

		out.write(value);
	}

	public static void main(String[] args) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		writeVarInt(baos, 100000);
		byte[] bytes = baos.toByteArray();
		for (byte b : bytes) {
			System.out.print(" ");
			System.out.print(Integer.toBinaryString(0xff & b));
		}

		System.out.println();

		ByteArrayInputStream bis = new ByteArrayInputStream(
				new byte[] { (byte) 0b10100000, (byte) 0b10001101, (byte) 0b00000110 });
		System.out.println("read: " + readVarInt(bis));
	}

	/**
	 * Read an variable length integer. See {@link #writeVarInt(OutputStream, int)} for encoding details.
	 *
	 * @param in The {@link InputStream} to read from.
	 * @return The integer read.
	 * @throws IOException If an error occurred while reading the integer.
	 */
	public static int readVarInt(InputStream in) throws IOException {
		byte b = readByte(in);
		int v = b & 0b01111111;
		for (int i = 7; (b & 0b10000000) != 0; i += 7) {
			b = readByte(in);
			v |= (b & 0b01111111) << i;
		}
		return v;
	}

	private static byte readByte(InputStream in) throws IOException {
		int read = in.read();
		if (read == -1) {
			throw new EOFException();
		}
		return (byte) read;
	}
}
