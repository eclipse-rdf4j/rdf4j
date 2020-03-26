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

/**
 * HDT Array factory.
 * 
 * @author Bart Hanssens
 */
class HDTArrayFactory {
	/**
	 * Parse array from input stream
	 * 
	 * @param is input stream
	 * @return array
	 * @throws IOException
	 */
	protected static HDTArray parse(InputStream is) throws IOException {
		int dtype = is.read();
		if (dtype != HDTArray.Type.LOG64.getValue()) {
			throw new UnsupportedOperationException("Array section: encoding " + Long.toHexString(dtype) +
					", but only Log64 encoding is supported");
		}
		return new HDTArrayLog64();
	}

	/**
	 * Create new array
	 * 
	 * @param os    output stream
	 * @param dtype array type
	 * @return array
	 * @throws IOException
	 */
	protected static HDTArray create(HDTArray.Type dtype) throws IOException {
		if (dtype != HDTArray.Type.LOG64) {
			throw new UnsupportedOperationException("Array section: encoding " + dtype.getValue() +
					", but only Log64 encoding is supported");
		}
		return new HDTArrayLog64();
	}
}
