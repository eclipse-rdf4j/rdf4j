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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * GZip-related utilities.
 */
public class GZipUtil {

	/**
	 * GZIP header magic number bytes, like found in a gzipped files, which are encoded in Intel format (i&#x2e;e&#x2e;
	 * little endian).
	 */
	private final static byte MAGIC_NUMBER[] = { (byte) 0x1f, (byte) 0x8b };

	/**
	 * Check if a stream is a GZIP stream, by checking the first bytes of the stream.
	 *
	 * @param in input stream
	 * @return true if a stream is a GZIP stream
	 * @throws IOException
	 */
	public static boolean isGZipStream(InputStream in) throws IOException {
		in.mark(MAGIC_NUMBER.length);
		byte[] fileHeader = IOUtil.readBytes(in, MAGIC_NUMBER.length);
		in.reset();
		return Arrays.equals(MAGIC_NUMBER, fileHeader);
	}
}
