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
import java.util.Iterator;

/**
 * HDT TriplesSection part.
 * 
 * @author Bart Hanssens
 */
abstract class HDTTriplesSection extends HDTPart {
	private int triples;

	/**
	 * Parse triples section
	 * 
	 * @param is    input stream
	 * @param order
	 * @throws IOException
	 */
	protected abstract void parse(InputStream is, HDTTriples.Order order) throws IOException;

	/**
	 * Get triple iterator, returning the triple parts in the correct S,P,O order.
	 * 
	 * @return
	 */
	protected abstract Iterator<int[]> getIterator();

	/**
	 * Set triple iterator, providing the triple parts in the correct S,P,O order.
	 * 
	 * @param iter
	 */
	protected abstract void setIterator(Iterator<int[]> iter);

	/**
	 * Get number of triples in this section
	 * 
	 * @return positive integer value
	 */
	protected int size() {
		return this.triples;
	}

	/**
	 * Set number of triples in this section
	 * 
	 * @param entries positive integer value
	 */
	protected void size(int triples) {
		this.triples = triples;
	}

	/**
	 * Write triples section in a specific order.
	 * 
	 * @param os    output stream
	 * @param order
	 * @throws IOException
	 */
	protected abstract void write(OutputStream is, HDTTriples.Order order) throws IOException;
}
