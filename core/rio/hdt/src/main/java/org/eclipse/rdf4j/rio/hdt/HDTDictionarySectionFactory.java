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
 * HDT DictionarySection factory.
 * 
 * @author Bart Hanssens
 */
class HDTDictionarySectionFactory {
	/**
	 * Create a dictionary section from input stream. The name an starting position are provided for debugging purposes.
	 * 
	 * @param is   input stream
	 * @param name name
	 * @param pos  starting position
	 * @return dictionary section
	 * @throws IOException
	 */
	protected static HDTDictionarySection parse(InputStream is, String name, long pos) throws IOException {
		int dtype = is.read();
		if (dtype != HDTDictionarySection.Type.FRONT.getValue()) {
			throw new UnsupportedOperationException("Dictionary " + name + ": encoding "
					+ Long.toHexString(dtype) + ", but only front encoding is supported");
		}
		return new HDTDictionarySectionPFC(name, pos);
	}
	
	/**
	 * Create a dictionary section for output stream. The name an starting position are provided for debugging purposes.
	 * 
	 * @param is   output stream
	 * @param name name
	 * @param pos  starting position
	 * @param dtype type
	 * @return dictionary section
	 * @throws IOException
	 */
	protected static HDTDictionarySection write(OutputStream os, String name, long pos, HDTDictionarySection.Type dtype) throws IOException {
		if (dtype != HDTDictionarySection.Type.FRONT) {
			throw new UnsupportedOperationException("Dictionary " + name + ": encoding "
					+ Long.toHexString(dtype.getValue()) + ", but only front encoding is supported");
		}
		os.write(HDTDictionarySection.Type.FRONT.getValue());
		return new HDTDictionarySectionPFC(name, pos);
	}
}
