/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.hdt;

import java.io.IOException;
import java.util.Iterator;

/**
 * HDT DictionarySection part. Various encodings exist.
 * 
 * @author Bart Hanssens
 */
abstract class HDTDictionarySection extends HDTPart {
	protected enum Type {
		PLAIN(1),
		FRONT(2),
		HTFC(3),
		FMINDEX(4),
		REPAIRDAC(5),
		HASHHUFF(6);
		private final int value;

		protected int getValue() {
			return value;
		}

		private Type(int value) {
			this.value = value;
		}
	}

	/**
	 * Get the size
	 * 
	 * @return size
	 */
	protected abstract int size();

	/**
	 * Set the size
	 * 
	 * @param size
	 */
	protected abstract void setSize(int size);

	/**
	 * Get the entry
	 * 
	 * @param i zero-based index
	 * @return
	 */
	protected abstract byte[] get(int i) throws IOException;

	/**
	 * Set from an iterator, allowing to remove strings from memory while processing
	 * 
	 * @param iter
	 */
	protected abstract void set(Iterator<String> iter) throws IOException;

	/**
	 * Constructor
	 * 
	 * @param pos  position
	 * @param name name
	 */
	protected HDTDictionarySection(String name, long pos) {
		super(name, pos);
	}
}
