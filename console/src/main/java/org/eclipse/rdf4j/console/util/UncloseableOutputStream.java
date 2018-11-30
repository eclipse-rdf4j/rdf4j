/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console.util;

import java.io.FilterOutputStream;
import java.io.OutputStream;

/**
 * Uncloseable output stream, useful for keeping console output open
 * 
 * @author Bart Hanssens
 */
public class UncloseableOutputStream extends FilterOutputStream {

	/**
	 * Constructor
	 * 
	 * @param os
	 */
	public UncloseableOutputStream(OutputStream os) {
		super(os);
	}
	
	@Override
	public void close() {
		// ignore closing the stream
	}
}
